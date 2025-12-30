package com.example.smartnotifier.data.repository

import com.example.smartnotifier.data.db.AppDatabase
import com.example.smartnotifier.data.db.entity.RuleEntity

class RulesRepository(
    private val db: AppDatabase
) {
    private val dao = db.ruleDao()

    suspend fun insert(rule: RuleEntity) = dao.insert(rule)
    suspend fun update(rule: RuleEntity) = dao.update(rule)
    suspend fun delete(rule: RuleEntity) = dao.delete(rule)

    fun observeAllRulesOrderByNewest() = dao.getAllRulesDesc()
    fun observeRulesOrderByPackageThenNewest() = dao.getRulesOrderByPackageThenNewest()

}
