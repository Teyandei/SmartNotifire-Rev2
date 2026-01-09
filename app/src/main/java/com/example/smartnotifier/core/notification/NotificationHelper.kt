package com.example.smartnotifier.core.notification

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
