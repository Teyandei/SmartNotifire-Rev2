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
    /**
     * ルール（Rules）に関するデータアクセスを集約する Repository。
     *
     * ViewModel からは本 Repository を介して DAO を利用し、
     * データ取得・更新の窓口を一本化する。
     */
    val dao = db.ruleDao() // ViewModel から利用可能にする

    /**
     * ルールを新規追加する。
     */
    suspend fun insert(rule: RuleEntity) = dao.insert(rule)

    /**
     * ルールを更新する。
     */
    suspend fun update(rule: RuleEntity) = dao.update(rule)

    /**
     * ルールを削除する。
     */
    suspend fun delete(rule: RuleEntity) = dao.delete(rule)

    /**
     * ルール一覧を「新しい順（ID 降順相当）」で監視する。
     */
    fun observeAllRulesOrderByNewest(): Flow<List<RuleEntity>> = dao.getAllRulesDesc()

    /**
     * ルール一覧を「PackageName 昇順、同一 PackageName 内で ID 昇順」で監視する。
     */
    fun observeRulesOrderByPackageAsc(): Flow<List<RuleEntity>> = dao.getRulesOrderByPackageAsc()

    /**
     * 指定 ID のルールを複製する。
     *
     * DAO 側のトランザクション処理を呼び出し、複製の一貫性を担保する。
     */
    suspend fun duplicateRule(id: Int) = dao.duplicateRuleTransaction(id)
}
