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

@Dao
interface NotificationLogDao {

    @Query("SELECT * FROM notification_log ORDER BY id DESC LIMIT :limit")
    fun getLatestLogs(limit: Int = 100): Flow<List<NotificationLogEntity>>

    @Query("SELECT Count(*) FROM notification_log WHERE packageName = :packageName AND channelId = :channelId AND title = :title")
    suspend fun getLogCount(packageName: String, channelId: String, title: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: NotificationLogEntity)

    @Query("DELETE FROM notification_log WHERE id NOT IN (" +
            "SELECT id FROM notification_log ORDER BY id DESC LIMIT :limit)")
    suspend fun trimLogs(limit: Int = 100)
}