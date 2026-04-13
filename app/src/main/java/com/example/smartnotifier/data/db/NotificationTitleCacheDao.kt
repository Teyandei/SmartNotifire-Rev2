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
import com.example.smartnotifier.data.db.DbConstants.NOTIFICATION_TITLE_CACHE_LIMIT
import com.example.smartnotifier.data.db.entity.NotificationTitleCacheEntity

/**
 * NotificationTitleCacheテーブルのDAO
 */
@Dao
interface NotificationTitleCacheDao {

    @Upsert
    suspend fun upsert(entity: NotificationTitleCacheEntity)

    /**
     * 通知ごとのタイトル保持件数を保つ
     * - 削除は通知を受けた日時が古いものから行う
     *
     * @param packageName 検索用パッケージ名
     * @param channelId 検索用チャンネルID
     * @param limit 保持件数
     */
    @Query(
        """
        DELETE FROM notification_title_cache
        WHERE id NOT IN (
            SELECT id
            FROM notification_title_cache
            WHERE packageName = :packageName
              AND channelId = :channelId
            ORDER BY lastUsed DESC
            LIMIT :limit
        )
        AND packageName = :packageName
        AND channelId = :channelId
        """
    )
    suspend fun trimRecentTitles(
        packageName: String,
        channelId: String,
        limit: Int = NOTIFICATION_TITLE_CACHE_LIMIT
    )

    /**
     * 通知で受け取ったタイトルの取得用クエリ
     */
    @Query(
        """
        SELECT *
        FROM notification_title_cache
        WHERE packageName = :packageName
          AND channelId = :channelId
        ORDER BY lastUsed DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentTitles(
        packageName: String,
        channelId: String,
        limit: Int = NOTIFICATION_TITLE_CACHE_LIMIT
    ): List<NotificationTitleCacheEntity>

    /**
     * 追加時の重複回避用クエリ
     */
    @Query(
        """
        SELECT *
        FROM notification_title_cache
        WHERE packageName = :packageName
          AND channelId = :channelId
          AND title = :title
        LIMIT 1
        """
    )
    suspend fun findByKey(
        packageName: String,
        channelId: String,
        title: String
    ): NotificationTitleCacheEntity?
}
