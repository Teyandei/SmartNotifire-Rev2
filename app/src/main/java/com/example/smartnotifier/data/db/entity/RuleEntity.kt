package com.example.smartnotifier.data.db.entity

import androidx.room.*

/**
 * 通知検出ルール
 *
 * @property id 主キー
 * @property packageName パッケージ名
 * @property appLabel アプリ名
 * @property channelId チャンネルID
 * @property srhTitle 検索タイトル
 * @property voiceMsg 音声メッセージ
 * @property enabled 許可
 * @property channelName チャンネル名
 */
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
    val appLabel: String,          // AppLabel
    val channelId: String,         // ChannelID
    val srhTitle: String = "",     // SrhTitle
    val voiceMsg: String = "",     // VoiceMsg
    val enabled: Boolean = false,  // Enabled
    val channelName:String = ""    // ChannelName
)
