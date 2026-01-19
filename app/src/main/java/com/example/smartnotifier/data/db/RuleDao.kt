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
    @Query("SELECT * FROM rules ORDER BY packageName ASC, id ASC")
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
     * 指定されたルールをトランザクション内で複製する。
     *
     * 元ルールを取得し、タイトルの重複を回避した上で
     * 新しいルールとして挿入する。
     */
    @Transaction
    suspend fun duplicateRuleTransaction(id: Int) {
        val original = getRuleById(id) ?: return
        val count = countSimilarTitles(original.packageName, original.channelId, original.srhTitle)
        
        val newTitle = if (count > 0) {
            "${original.srhTitle}($count)"
        } else {
            original.srhTitle
        }
        
        val newRule = original.copy(
            id = 0,
            srhTitle = newTitle,
            enabled = false
        )
        insert(newRule)
    }
}
