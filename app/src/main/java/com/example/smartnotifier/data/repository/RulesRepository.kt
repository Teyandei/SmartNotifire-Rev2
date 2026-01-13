package com.example.smartnotifier.data.repository

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

import com.example.smartnotifier.data.db.AppDatabase
import com.example.smartnotifier.data.db.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

class RulesRepository(db: AppDatabase) {
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
