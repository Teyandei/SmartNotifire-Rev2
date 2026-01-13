package com.example.smartnotifier.core.notification

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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.smartnotifier.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_CHECK = "check"
        private const val NOTIFICATION_ID_TEST = 1001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * 設計書に従い、テスト通知用のチャンネルを作成する
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Test Notification"
            val descriptionText = "Channel for checking notification functionality"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_CHECK, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 設計書 ⑬ に基づき通知を発行する
     */
    fun sendTestNotification(title: String) {
        // チャンネルがなければ作成
        createNotificationChannels()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_CHECK)
            .setSmallIcon(R.drawable.ic_launcher_small)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.msg_text_message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID_TEST, builder.build())
    }
}
