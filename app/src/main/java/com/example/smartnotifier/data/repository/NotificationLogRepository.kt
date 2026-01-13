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

class NotificationLogRepository(
    private val db: AppDatabase
) {
    private val dao = db.notificationLogDao()

    suspend fun insert(log: NotificationLogEntity) = dao.insert(log)
    suspend fun trimLogs(limit: Int = 100) = dao.trimLogs(limit)

    fun observeLatestLogs(limit: Int = 100) = dao.getLatestLogs(limit)
}