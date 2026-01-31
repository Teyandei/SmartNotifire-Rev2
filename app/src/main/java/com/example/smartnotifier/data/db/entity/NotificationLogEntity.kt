package com.example.smartnotifier.data.db.entity

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

    val packageName: String,    // PackageName
    val channelId: String,      // ChannelID
    val appLabel: String = "",  // AppLabel
    val receivedCount: Long = 0 // Title
)
