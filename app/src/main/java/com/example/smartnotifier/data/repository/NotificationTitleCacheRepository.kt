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

import com.example.smartnotifier.data.db.DbConstants.NOTIFICATION_TITLE_CACHE_LIMIT
import com.example.smartnotifier.data.db.NotificationTitleCacheDao
import com.example.smartnotifier.data.db.entity.NotificationTitleCacheEntity

class NotificationTitleCacheRepository(
    private val dao : NotificationTitleCacheDao
) {
    suspend fun saveTitle(
        packageName: String,
        channelId: String,
        title: String
    ) {
        if (title.trim().isEmpty()) return
        val now = System.currentTimeMillis()
        val old = dao.findByKey(packageName, channelId, title)

        dao.upsert(
            NotificationTitleCacheEntity(
                id = old?.id ?: 0,
                packageName = packageName,
                channelId = channelId,
                title = title,
                lastUsed = now
            )
        )

        dao.trimRecentTitles(packageName, channelId, NOTIFICATION_TITLE_CACHE_LIMIT)
    }

    suspend fun getRecentTitleStrings(
        packageName: String,
        channelId: String
    ): List<String> {
        return dao.getRecentTitles(
            packageName = packageName,
            channelId = channelId,
            limit = NOTIFICATION_TITLE_CACHE_LIMIT
        ).map { it.title }
    }
}
