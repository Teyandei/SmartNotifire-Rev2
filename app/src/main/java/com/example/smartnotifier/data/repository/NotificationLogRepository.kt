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
import com.example.smartnotifier.data.db.entity.NotificationLogEntity

/**
 * 通知ログ([NotificationLogEntity])のデータ操作を抽象化するリポジトリ。
 *
 * 設計書の責務分離（MVVM）に基づき、ViewModelとデータソース（DAO）の間に位置します。
 * このクラスは、通知ログの保存や取得に関するビジネスロジックをカプセル化し、
 * UI層（ViewModel）がデータベースの実装詳細を意識することなくデータ操作を行えるようにします。
 *
 * @property db アプリケーションのデータベースインスタンス([AppDatabase])。
 */
class NotificationLogRepository(
    private val db: AppDatabase
) {
    private val dao = db.notificationLogDao()

    /**
     * 新しい通知ログをデータベースに挿入します。
     *
     * @param log 挿入する[NotificationLogEntity]インスタンス。
     */
    suspend fun insert(log: NotificationLogEntity) = dao.insert(log)

    /**
     * データベース内のログ件数を制限内に保つために、古いログを削除します。
     *
     * @param limit 保持するログの最大件数。
     */
    suspend fun trimLogs(limit: Int = 100) = dao.trimLogs(limit)

    /**
     * 最新の通知ログを監視するための[kotlinx.coroutines.flow.Flow]を返します。
     * UI層はこのFlowを購読することで、データベースの変更をリアクティブに受け取ることができます。
     *
     * @param limit 取得するログの最大件数。
     * @return 通知ログのリストを放出する[kotlinx.coroutines.flow.Flow]。
     */
    fun observeLatestLogs(limit: Int = 100) = dao.getLatestLogs(limit)
}
