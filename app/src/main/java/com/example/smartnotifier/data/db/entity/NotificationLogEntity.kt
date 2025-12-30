package com.example.smartnotifier.data.db.entity

import android.net.Uri
import androidx.room.*

@Entity(
    tableName = "notification_log"
)

data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val packageName: String,
    val channelId: String,
    val notificationIcon: Uri?,
    val title: String
)
