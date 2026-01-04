package com.example.smartnotifier.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.smartnotifier.data.db.entity.RuleEntity

@Dao
interface RuleDao {

    /**
     * 設計書 ⑨ 並び順 False: Rules.ID の降順
     */
    @Query("SELECT * FROM rules ORDER BY id DESC")
    fun getAllRulesDesc(): Flow<List<RuleEntity>>

    /**
     * 設計書 ⑨ 並び順 True: 1:PackageName, 2:Rules.ID の昇順
     */
    @Query("SELECT * FROM rules ORDER BY packageName ASC, id ASC")
    fun getRulesOrderByPackageAsc(): Flow<List<RuleEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rule: RuleEntity): Long

    @Update
    suspend fun update(rule: RuleEntity)

    @Delete
    suspend fun delete(rule: RuleEntity)

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Int): RuleEntity?

    /**
     * コピー機能や通知ログからの追加時、一意な srhTitle を作成するために
     * 同一パッケージ・チャンネル内での類似タイトル数をカウントします。
     */
    @Query("SELECT COUNT(*) FROM rules WHERE packageName = :packageName AND channelId = :channelId AND srhTitle LIKE :srhTitle || '%'")
    suspend fun countSimilarTitles(packageName: String, channelId: String, srhTitle: String): Int

    /**
     * 設計書 ⑤ コピー
     * トランザクション内で一意なタイトルを生成して挿入する
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
