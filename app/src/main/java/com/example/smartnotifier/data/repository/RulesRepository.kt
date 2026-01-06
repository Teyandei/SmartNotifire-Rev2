package com.example.smartnotifier.data.repository

import com.example.smartnotifier.data.db.AppDatabase
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import com.example.smartnotifier.data.db.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

class RulesRepository(private val db: AppDatabase) {
    val dao = db.ruleDao() // ViewModel から利用可能にする

    suspend fun insert(rule: RuleEntity) = dao.insert(rule)
    suspend fun update(rule: RuleEntity) = dao.update(rule)
    suspend fun delete(rule: RuleEntity) = dao.delete(rule)

    /**
     * 設計書 ⑨ 並び順 False: Rules.ID の降順
     */
    fun observeAllRulesOrderByNewest(): Flow<List<RuleEntity>> = dao.getAllRulesDesc()

    /**
     * 設計書 ⑨ 並び順 True: 1:PackageName, 2:Rules.ID の昇順
     */
    fun observeRulesOrderByPackageAsc(): Flow<List<RuleEntity>> = dao.getRulesOrderByPackageAsc()

    /**
     * トランザクションを利用したコピー処理
     */
    suspend fun duplicateRule(id: Int) = dao.duplicateRuleTransaction(id)
}
