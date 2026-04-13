package com.example.smartnotifier.data.db.entity

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

@Entity(
    tableName = "notification_log",
    indices = [
        Index(
            value = ["packageName", "channelId"],
            unique = true
        )
    ]
)

data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val packageName: String,        // PackageName
    val channelId: String,          // ChannelID
    val appLabel: String = "",      // AppLabel
    val receivedCount: Long = 0,    // Title
    @ColumnInfo(name = "importance" , defaultValue = "3")
    val importance: Int = 3,        // Importance
    val channelName: String = "",   // ChannelName
    val created: Long = 0,          // Created
    val lastReceived: Long = 0      // LastReceived
)
