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
import com.example.smartnotifier.data.db.entity.NotificationsEntity

/**
 *  NotificationDAOを扱うRepository
 *
 */
class NotificationsRepository(private val db: AppDatabase
) {
    private val dao = db.notificationsDao()


    fun getAll() = dao.getAll()
    fun getAllSortByAppLabel() = dao.getAllSortByAppLabel()
    fun getAllSortByReceivedCount() = dao.getAllSortByReceivedCount()

    /**
     * 通知情報の追加と更新
     *
     * @param notification 通知情報。StatusBarNotificationで取得した情報及びその情報を元に取得したものは全てセットしておくこと。
     */
    suspend fun upsertNotification(notification: NotificationsEntity) = dao.upsertNotification(notification)
}
