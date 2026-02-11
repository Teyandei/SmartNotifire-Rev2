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

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.smartnotifier.data.db.entity.NotificationsEntity
import kotlinx.coroutines.flow.Flow

/**
 * NotificationsEntityに対するDAO
 */
@Dao
interface NotificationsDao {
    /**
     * 指定したアプリのインデックスを取得条件としてレコードを検索する
     */
    @Query("SELECT * FROM notifications WHERE packageName = :packageName AND channelId = :channelId")
    suspend fun getByIndex(packageName: String, channelId: String) : NotificationsEntity?

    /**
     * 指定したアプリの最大チャンネルインデックスを取得する
     */
    @Query("SELECT Max(channelIndex) FROM notifications WHERE packageName = :packageName")
    suspend fun getMaxChannelIndex(packageName: String) : Int?

    /**
     * 追加更新
     */
    @Upsert
    suspend fun upsert(notification: NotificationsEntity)

    /**
     * 全ての通知を取得する
     */
    @Query ("SELECT * FROM notifications ORDER BY id DESC")
    fun getAll(): Flow<List<NotificationsEntity>>

    /**
     * アプリ名昇順で並べ替えた通知一覧を取得する
     */
    @Query("SELECT * FROM notifications ORDER BY appLabel ASC, channelIndex ASC")
    fun getAllSortByAppLabel(): Flow<List<NotificationsEntity>>

    /**
     * 受信回数が多い順で並べ替えた通知一覧を取得する
     *
     */
    @Query("SELECT * FROM notifications ORDER BY receivedCount DESC, appLabel ASC, channelIndex ASC")
    fun getAllSortByReceivedCount(): Flow<List<NotificationsEntity>>

    /**
     * NotificationsのUpsert
     *
     * notificationの内容により以下の処理を行う
     * - notificationのIndex(packageName. channelId)のレコードが無い場合は新規作成
     *    1. notification.createdに現在日時を設定
     *    2. notification.channelIndexに新しいindexとしMax(notifications.channelIndex)+1を設定
     *    3. notification.receivedCountを1に設定
     *    4. notification.lastUpdatedに現在日時を設定
     * - notificationのIndex(packageName. channelId)のレコードが既に存在する場合はそのidで更新
     *    1. notification.receivedCountに1を加算
     *    2. notification.lastUpdatedに現在日時を設定
     *
     * @param notification 通知内容
     */
    @Transaction
    suspend fun upsertNotification(notification: NotificationsEntity) {
        val existing = getByIndex(notification.packageName, notification.channelId)

        val entityToSave = if (existing == null) {
            // 新規作成
            val maxChannelIndex = (getMaxChannelIndex(notification.packageName)?: 0) + 1
            notification.copy(  // copyは変更するメンバーだけを設定できる。
                created = notification.lastReceived,
                channelIndex = maxChannelIndex,
                firstImportance = notification.importance,
                receivedCount = 1L,
                lastReceived = notification.lastReceived
            )
        } else {
            // 更新
            existing.copy(
                receivedCount = existing.receivedCount + 1L,
                lastReceived = notification.lastReceived
            )
        }
        upsert(entityToSave)
    }
}

