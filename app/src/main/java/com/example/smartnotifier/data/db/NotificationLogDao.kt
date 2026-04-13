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
import com.example.smartnotifier.data.db.entity.NotificationLogEntity

/**
 * [NotificationLogEntity] テーブルへのデータアクセスを提供するDAO(Data Access Object)です。
 *
 * 設計書「２．通知ログ(NotificationLog)」および「機能：通知ログ機能」に基づき、
 * 通知履歴の永続化、取得、および件数管理に関連するデータベース操作を定義します。
 * Roomライブラリによって実装が自動生成されます。
 */
@Dao
interface NotificationLogDao {

    /**
     * 最新の通知ログを指定された件数だけ、IDの降順で取得します。
     *
     * UIがデータベースの変更をリアクティブに監視できるよう、結果を[Flow]でラップして返します。
     *
     * @param limit 取得するログの最大件数。デフォルトは[DbConstants.NOTIFICATION_LOG_LIMIT]件です。
     * @return 通知ログのリストを放出する[Flow]。
     */
    @Query(
        """
            SELECT
                nl.id,
                nl.packageName,
                nl.channelId,
                nl.appLabel,
                nl.importance,
                nl.channelName,
                nl.lastReceived,
                nl.created,
                nl.receivedCount,
                EXISTS (
                    SELECT 1
                    FROM Rules AS r
                    WHERE r.packageName = nl.packageName
                        AND r.channelId = nl.channelId
                ) AS hasRule
            FROM notification_log AS nl
            ORDER BY nl.id DESC 
            LIMIT :limit
        """
    )
    fun getLatestLogsForList(limit: Int = DbConstants.NOTIFICATION_LOG_LIMIT): Flow<List<NotificationLogListItem>>

    /**
     * 最新の通知ログを指定された件数だけ、アプリ名の昇順で取得します。
     *
     * UIがデータベースの変更をリアクティブに監視できるよう、結果を[Flow]でラップして返します。
     *
     * @param limit 取得するログの最大件数。デフォルトは[DbConstants.NOTIFICATION_LOG_LIMIT]件です。
     * @return 通知ログのリストを放出する[Flow]。
     */
    @Query(
        """
            SELECT
                nl.id,
                nl.packageName,
                nl.channelId,
                nl.appLabel,
                nl.importance,
                nl.channelName,
                nl.lastReceived,
                nl.created,
                nl.receivedCount,
                EXISTS (
                    SELECT 1
                    FROM Rules AS r
                    WHERE r.packageName = nl.packageName
                        AND r.channelId = nl.channelId
                ) AS hasRule
            FROM notification_log AS nl
            ORDER BY nl.appLabel ASC 
            LIMIT :limit
        """
    )
    fun getLLogByAppLabelForList(limit: Int = DbConstants.NOTIFICATION_LOG_LIMIT): Flow<List<NotificationLogListItem>>

    /**
     * 最新の通知ログを指定された件数だけ、受信件数の降順で取得します。
     *
     * UIがデータベースの変更をリアクティブに監視できるよう、結果を[Flow]でラップして返します。
     *
     * @param limit 取得するログの最大件数。デフォルトは[DbConstants.NOTIFICATION_LOG_LIMIT]件です。
     * @return 通知ログのリストを放出する[Flow]。
     */
    @Query(
        """
            SELECT
                nl.id,
                nl.packageName,
                nl.channelId,
                nl.appLabel,
                nl.importance,
                nl.channelName,
                nl.lastReceived,
                nl.created,
                nl.receivedCount,
                EXISTS (
                    SELECT 1
                    FROM Rules AS r
                    WHERE r.packageName = nl.packageName
                        AND r.channelId = nl.channelId
                ) AS hasRule
            FROM notification_log AS nl
            ORDER BY nl.receivedCount DESC 
            LIMIT :limit
        """
    )
    fun getLLogByReceivedCountForList(limit: Int = DbConstants.NOTIFICATION_LOG_LIMIT): Flow<List<NotificationLogListItem>>

    @Query(
        """
            SELECT * 
            FROM notification_log AS nl
            WHERE 
                NOT EXISTS (SELECT 1
                    FROM rules AS r
                    WHERE r.packageName = nl.packageName
                        AND r.channelId = nl.channelId
                )
            ORDER BY nl.id DESC 
            LIMIT :limit
        """
    )
    fun getLatestLogs(limit: Int = DbConstants.NOTIFICATION_LOG_LIMIT): Flow<List<NotificationLogEntity>>

    @Query(
        """
            SELECT * 
            FROM notification_log AS nl
            WHERE 
                NOT EXISTS (SELECT 1
                    FROM rules AS r
                    WHERE r.packageName = nl.packageName
                        AND r.channelId = nl.channelId
                )
            ORDER BY nl.appLabel ASC
        """
    )
    fun getLogsByAppLabel(): Flow<List<NotificationLogEntity>>

    @Query(
        """
            SELECT * 
            FROM notification_log AS nl
            WHERE 
                NOT EXISTS (SELECT 1
                    FROM rules AS r
                    WHERE r.packageName = nl.packageName
                        AND r.channelId = nl.channelId
                )
            ORDER BY nl.receivedCount DESC
         """
    )
    fun getLogsByReceivedCount(): Flow<List<NotificationLogEntity>>

    /**
     * 指定された条件（パッケージ名、チャンネルIDに一致するアプリ名を取得します。
     *
     * 主に、重複した内容のログをデータベースに挿入するのを防ぐために使用されます。
     *
     * @param packageName 検索対象のパッケージ名。
     * @param channelId 検索対象のチャンネルID。
     * @return 条件に一致したアプリ名。
     */
    @Query("SELECT appLabel FROM notification_log WHERE packageName = :packageName AND channelId = :channelId")
    suspend fun getAppLabel(packageName: String, channelId: String): String?

    /**
     * 新しい通知ログをデータベースに挿入します。
     *
     * @param log 挿入する [NotificationLogEntity] インスタンス。
     */
    @Insert
    suspend fun insert(log: NotificationLogEntity)

    /**
     * データベース内のログ件数を制限内に保つために、古いログを削除します。
     *
     * 最新の[limit]件を残し、それ以外の古いエントリ（IDが小さいもの）をすべて削除することで、
     * FIFO（先入れ先出し）のログ管理を実現します。
     *
     * @param limit 保持するログの最大件数。デフォルトは100件です。
     */
    @Query("DELETE FROM notification_log WHERE id NOT IN (" +
            "SELECT id FROM notification_log ORDER BY id DESC LIMIT :limit)")
    suspend fun trimLogs(limit: Int = DbConstants.NOTIFICATION_LOG_LIMIT)

    /**
     * 指定されたパッケージ名の通知ログを削除します。
     *
     * @param packageName 削除するログの対象となるパッケージ名。
     */
    @Query("DELETE FROM notification_log WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    /**
     * クリーンアップ
     *
     * 最終受信日時が指定日時以前のログを削除する。
     * 削除対象は、Rulesに登録されていない通知
     *
     * @param limit 指定日時
     *
     */
    @Query(
        """
            DELETE 
            FROM notification_log
            WHERE 
                NOT EXISTS (
                    SELECT 1
                    FROM rules AS r
                    WHERE r.packageName = packageName
                        AND r.channelId = channelId
                )
                AND
                    lastReceived < :limit
        """
    )
    suspend fun cleanupLog(limit: Long)

    /**
     * 受信カウントをインクリメントする
     *
     * @param packageName 受信カウントをインクリメントするログの対象となるパッケージ名。
     * @param channelId 受信カウントをインクリメントするログの対象となるチャンネルID。
     * @return 受信カウントの更新件数。(一意なので0 or 1)
     */
    @Query("""
       UPDATE notification_log
       SET receivedCount = receivedCount + 1,
           lastReceived = :lastReceived
       WHERE packageName = :packageName AND channelId = :channelId
    """)
    suspend fun incrementReceivedCount(packageName: String, channelId: String, lastReceived: Long) : Int

    @Query("SELECT * FROM notification_log WHERE packageName = :packageName AND channelId = :channelId")
    fun getIndex(packageName: String, channelId: String): NotificationLogEntity?

    @Upsert
    suspend fun upsert(notificationLog: NotificationLogEntity)

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
    @Transaction
    suspend fun upsertNotificationLog(log: NotificationLogEntity) {
        val existing = getIndex(log.packageName, log.channelId)
        val saveToLog =  if (existing == null) {
            log.copy(
                created = log.lastReceived,
                receivedCount = 1L
            )
        } else {
            existing.copy(
                receivedCount = existing.receivedCount + 1L,
                lastReceived = log.lastReceived
            )
        }
        upsert(saveToLog)
    }

    @Query("""
        UPDATE notification_log
        SET lastReceived = :lastReceived
        WHERE packageName = :packageName AND channelId = :channelId
        """
    )
    suspend fun updateLastReceivedForLog(packageName: String, channelId: String, lastReceived: Long)
}
