package com.example.smartnotifier.data.db.entity

import androidx.room.*

@Entity(
    tableName = "rules",
    indices = [
        Index(
                value = ["packageName", "channelId", "srhTitle"],
                unique = true
        )
    ]
)

data class RuleEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,

        val packageName: String,       // PackageName
        val channelId: String,         // ChannelID
        val srhTitle: String,          // SrhTitle
        val voiceMsg: String?,         // VoiceMsg
        val enabled: Boolean = false   // Enabled
)