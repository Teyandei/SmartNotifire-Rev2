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
     * @param limit 取得するログの最大件数。デフォルトは100件です。
     * @return 通知ログのリストを放出する[Flow]。
     */
    @Query("SELECT * FROM notification_log ORDER BY id DESC LIMIT :limit")
    fun getLatestLogs(limit: Int = 100): Flow<List<NotificationLogEntity>>

    /**
     * 指定された条件（パッケージ名、チャンネルID、タイトル）に一致するログの件数を取得します。
     *
     * 主に、重複した内容のログをデータベースに挿入するのを防ぐために使用されます。
     *
     * @param packageName 検索対象のパッケージ名。
     * @param channelId 検索対象のチャンネルID。
     * @param title 検索対象の通知タイトル。
     * @return 条件に一致したログの件数。
     */
    @Query("SELECT Count(*) FROM notification_log WHERE packageName = :packageName AND channelId = :channelId AND title = :title")
    suspend fun getLogCount(packageName: String, channelId: String, title: String): Int

    /**
     * 新しい通知ログをデータベースに挿入します。
     *
     * 既に同じ内容のログが存在する場合（主キー以外での重複はここでは考慮されない）でも、
     * [OnConflictStrategy.IGNORE] により競合を無視し、エラーを発生させません。
     *
     * @param log 挿入する [NotificationLogEntity] インスタンス。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
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
    suspend fun trimLogs(limit: Int = 100)
}