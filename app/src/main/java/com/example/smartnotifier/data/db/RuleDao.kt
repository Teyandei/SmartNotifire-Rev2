package com.example.smartnotifier.data.db

/*
 * SmartNotifier-Rev2
 * Copyright (C) 2026  Takeaki Yoshizawa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.smartnotifier.data.db.entity.RuleEntity

@Dao
interface RuleDao {
    /**
     * ルール一覧を新しい順（ID 降順相当）で取得する。
     *
     * Flow として公開し、ルールの追加・更新・削除をリアルタイムに反映する。
     */
    @Query("SELECT * FROM rules ORDER BY id DESC")
    fun getAllRulesDesc(): Flow<List<RuleEntity>>

    /**
     * ルール一覧を PackageName 昇順で取得する。
     *
     * 同一 PackageName 内では ID 昇順で並ぶ。
     */
    @Query("SELECT * FROM rules ORDER BY appLabel ASC, id ASC")
    fun getRulesOrderByPackageAsc(): Flow<List<RuleEntity>>

    /**
     * ルールを新規追加する。
     *
     * 一意制約に違反した場合は例外を送出する。
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rule: RuleEntity): Long

    /**
     * ルールを新規追加する。
     *
     * @return 一意制約に違反した場合は-1を返す。例外は発生しない。
     * */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(rule: RuleEntity): Long

    /**
     * 既存のルールを更新する。
     */
    @Update
    suspend fun update(rule: RuleEntity)

    /**
     * 許可の更新
     * @param id ルールID
     * @param enabled 許可状態
     */
    @Query("UPDATE rules SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: Int, enabled: Boolean)

    /**
     * 指定されたルールを削除する。
     */
    @Delete
    suspend fun delete(rule: RuleEntity)

    /**
     * ID を指定してルールを取得する。
     *
     * 対象が存在しない場合は null を返す。
     */
    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Int): RuleEntity?

    /**
     * 同一パッケージ・チャンネル内で、指定タイトルに類似するルール数を取得する。
     *
     * コピー機能や通知ログからのルール生成時に、
     * 一意な srhTitle を作成する目的で使用される。
     */
    @Query("SELECT COUNT(*) FROM rules WHERE packageName = :packageName AND channelId = :channelId AND srhTitle LIKE :srhTitle || '%'")
    suspend fun countSimilarTitles(packageName: String, channelId: String, srhTitle: String): Int

    /**
     * 通知の該当チャンネルの一覧を検索タイトルの降順で選択する。
     *
     * - Emptyが一番最後にヒットするように
     */
    @Query("SELECT * FROM rules WHERE packageName = :packageName AND channelId = :channelId ORDER BY SrhTitle DESC")
    fun getRulesByPackageAndChannel(packageName: String, channelId: String): Flow<List<RuleEntity>>

    /**
     * 指定されたルールをトランザクション内で複製する。
     *
     * 元ルールを取得し、タイトルの重複を回避した上で新しいルールとして挿入する。
     *
     * @param id 複製元のルールID
     * @return 実行結果
     *         ```
     *         Success: {
     *             rowId,           // 追加したId
     *             adoptedTitle     // 一意制約化したタイトル
     *         }
     *         TooManySameNames: maxTryに達してタイトルを作成出来なかった
     *         ```
     */
    @Transaction
    suspend fun duplicateRuleTransaction(id: Int): InsertWithAutoNumberResult {
        val original = getRuleById(id)
            ?: return InsertWithAutoNumberResult.TooManySameNames

        val ruleBase = original.copy(
            id = 0,
            srhTitle = original.srhTitle, // baseTitle として使用
            enabled = false
        )

        return insertWithAutoNumber(
            ruleBase = ruleBase,
            baseTitle = original.srhTitle
        )
    }

    /**
     * Rulesの一意制約インデックスを保証する Insert 実行
     *
     * @param ruleBase 元となる情報を設定したルール
     * @param baseTitle 一意制約にする元タイトル
     * @param maxTry 一意制約タイトルにするための最大試行回数
     * @return 実行結果
     * - Success(rowId, adoptedTitle): 追加成功。採用タイトルを返す
     * - TooManySameNames: maxTry に達して作成不可
     */
    @Transaction
    suspend fun insertWithAutoNumber(
        ruleBase: RuleEntity,
        baseTitle: String,
        maxTry: Int = 50
    ): InsertWithAutoNumberResult {
        for (i in 0..maxTry) {
            val title = if (i == 0) {
                baseTitle
            } else {
                "$baseTitle-#${i.toString().padStart(2, '0')}"
            }

            val rowId = insertIgnore(ruleBase.copy(srhTitle = title))
            if (rowId != -1L) {
                return InsertWithAutoNumberResult.Success(
                    rowId = rowId,
                    adoptedTitle = title
                )
            }
        }
        return InsertWithAutoNumberResult.TooManySameNames
    }

    sealed interface InsertWithAutoNumberResult {
        data class Success(
            val rowId: Long,
            val adoptedTitle: String
        ) : InsertWithAutoNumberResult

        data object TooManySameNames : InsertWithAutoNumberResult
    }
}
