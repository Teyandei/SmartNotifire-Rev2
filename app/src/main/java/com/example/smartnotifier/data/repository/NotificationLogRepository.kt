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
import com.example.smartnotifier.data.db.DbConstants
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
     * 表示用ログリストの取得(Idの降順)
     *
     * @param limit 取得するログの最大件数。デフォルト=[DbConstants.NOTIFICATION_LOG_LIMIT]。
     * @return 通知ログのリストを放出する[kotlinx.coroutines.flow.Flow]。
     */
    fun getLogItemsByLatest(limit: Int = DbConstants.NOTIFICATION_LOG_LIMIT) = dao.getLatestLogsForList(limit)

    /**
     * 表示用ログリストの取得(アプリ名の昇順)
     *
     * @param limit 取得するログの最大件数。デフォルト=[DbConstants.NOTIFICATION_LOG_LIMIT]。
     * @return 通知ログのリストを放出する[kotlinx.coroutines.flow.Flow]。
     */
    fun getLogItemsByAppLabel(limit: Int = DbConstants.NOTIFICATION_LOG_LIMIT) = dao.getLLogByAppLabelForList(limit)

    /**
     * 表示用ログリストの取得(受信件数の降順)
     *
     * @param limit 取得するログの最大件数。デフォルト=[DbConstants.NOTIFICATION_LOG_LIMIT]。
     * @return 通知ログのリストを放出する[kotlinx.coroutines.flow.Flow]。
     */
    fun getLogItemsByReceivedCount(limit: Int = DbConstants.NOTIFICATION_LOG_LIMIT) = dao.getLLogByReceivedCountForList(limit)

    /**
     *  同一の条件（パッケージ名、チャンネルID、タイトル）に一致するログの件数を取得します。
     *
     * @param packageName 検索対象のパッケージ名。
     * @param channelId 検索対象のチャンネルID。
     * @return 条件に一致したログの件数。
     */
    suspend fun getAppLabel(packageName: String, channelId: String): String? = dao.getAppLabel(packageName, channelId)

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
}
