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

/**
 * 表示用通知ログリスト
 *
 * @param id 通知ログのID
 * @param packageName 通知ログの対象パッケージ名
 * @param channelId 通知ログの対象チャンネルID
 * @param appLabel 通知ログの対象アプリ名
 * @param importance 通知ログの重要度
 * @param channelName 通知ログのチャンネル名
 * @param lastReceived 通知ログの最終受信時刻
 * @param created 通知ログの登録時刻
 * @param receivedCount 通知ログの受信回数
 * @param hasRule 通知ログのルール有無
 */
 data class NotificationLogListItem(
    val id: Long,
    val packageName: String,
    val channelId: String,
    val appLabel: String,
    val importance: Int,
    val channelName: String,
    val lastReceived: Long,
    val created: Long,
    val receivedCount: Int,
    val hasRule: Boolean
)
