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

import android.util.Log
import com.example.smartnotifier.data.db.AppDatabase
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val insertOrUpdateMutex = Mutex()

    /**
     * データベース内のログ件数を制限内に保つために、古いログを削除します。
     *
     * @param limit 保持するログの最大件数。
     */
    private suspend fun trimLogs(limit: Int = 100) = dao.trimLogs(limit)

    /**
     * 最新の通知ログを監視するための[kotlinx.coroutines.flow.Flow]を返します。
     * UI層はこのFlowを購読することで、データベースの変更をリアクティブに受け取ることができます。
     *
     * @param limit 取得するログの最大件数。
     * @return 通知ログのリストを放出する[kotlinx.coroutines.flow.Flow]。
     */
    fun observeLatestLogs(limit: Int = 100) = dao.getLatestLogs(limit)

    /**
     *  同一の条件（パッケージ名、チャンネルID、タイトル）に一致するログの件数を取得します。
     *
     * @param packageName 検索対象のパッケージ名。
     * @param channelId 検索対象のチャンネルID。
     * @return 条件に一致したログの件数。
     */
    suspend fun getAppLabel(packageName: String, channelId: String): String? = dao.getAppLabel(packageName, channelId)

    /**
     * ログの追加又は更新
     *
     * @param log ログの内容　※追加時はappLabelにアプリ名を設定しておくこと。
     */
    suspend fun insertOrCount(log: NotificationLogEntity) {
        insertOrUpdateMutex.withLock {
            val update = dao.incrementReceivedCount(log.packageName, log.channelId, log.lastReceived)
            if (update == 0) {
                try {
                    dao.insert(
                        NotificationLogEntity(
                            packageName = log.packageName,
                            channelId = log.channelId,
                            appLabel = log.appLabel,
                            receivedCount = 1,
                            importance = log.importance,
                            channelName = log.channelName,
                            created = log.created,
                            lastReceived = log.lastReceived
                        )
                    )
                    trimLogs(100)   // 100行に制限
                } catch (_: android.database.sqlite.SQLiteConstraintException) {
                    dao.incrementReceivedCount(log.packageName, log.channelId, log.lastReceived)
                } catch (e: Exception) {
                    Log.e(THIS_CLASS, """
                        |insertOrCount: 
                        |packageName = "${log.packageName}",
                        |channelId = "${log.channelId}",
                        |appLabel = "${log.appLabel}"
                        """.trimMargin(), e)
                }
            }
        }
    }

    /**
     * 通知ログの追加・更新
     *
     * @param log ログの内容　
     *            # 以下の内容は呼び出し前に設定しておくこと
     *            packageName,
     *            channelId,
     *            appLabel,
     *            importance,
     *            channelName,
     *            lastReceived,
     *
     */
    suspend fun upsertNotificationLog(log: NotificationLogEntity) = dao.upsertNotificationLog(log)

    companion object {
        private const val THIS_CLASS = "NotificationLogRepository"
    }
}
