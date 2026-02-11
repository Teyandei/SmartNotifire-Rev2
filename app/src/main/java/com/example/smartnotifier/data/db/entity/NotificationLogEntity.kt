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
