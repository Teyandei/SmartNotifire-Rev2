package com.example.smartnotifier.data.db.entity

import androidx.room.*

@Entity(
    tableName = "notification_log",
    indices = [
        Index(
            value = ["packageName", "channelId", "title"],
            unique = true
        )
    ]
)

data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val packageName: String,    // PackageName
    val appLabel: String,       // AppLabel
    val channelId: String,      // ChannelID
    val title: String = ""      // Title
)
