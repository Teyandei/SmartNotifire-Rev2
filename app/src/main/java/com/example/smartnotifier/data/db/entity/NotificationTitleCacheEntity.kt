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

/**
 * **通知タイトル保持キャッシュ**
 * - 検索タイトル追加・編集時に表示する既存通知タイトルを保持する
 *
 * @property id 主キー
 * @property packageName 通知パッケージ名（検索用）
 * @property channelId 通知チャンネルID（検索用）
 * @property title 通知タイトル（リスト表示用）
 * @property lastUsed 最終通知日時（削除判定用）
 */
@Entity(
    tableName = "notification_title_cache",
    indices = [
        Index(value = ["packageName", "channelId", "title"], unique = true)
    ]
)
data class NotificationTitleCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val packageName: String,
    val channelId: String,
    val title: String,
    val lastUsed: Long
)
