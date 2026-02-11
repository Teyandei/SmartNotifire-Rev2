package com.example.smartnotifier.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 通知の永続化、取得、および件数管理に関連するデータベース操作を定義します。
 * Roomライブラリによって実装が自動生成されます。
 *
 * @param id 主キー。自動生成されます。
 * @param packageName 通知が送信されたアプリのパッケージ名。
 * @param channelId 通知のチャンネルID
 * @param appLabel 通知が送信されたアプリのラベル。
 * @param created 通知が受信された時刻
 * @param channelIndex 通知のチャンネルのインデックス。
 * @param groupKey 通知のグループキー。
 * @param firstImportance 通知の最初の重要度。
 * @param importance 通知の最終的な重要度。
 * @param receivedCount 通知の受信回数。
 * @param lastReceived 通知の最終受信時刻。
 * @see [Roomライブラリ](https://developer.android.com/training/data-storage/room)
 * @see [設計書](https://teyandei.github.io/SmartNotifire-Rev2/design/SmartNotifire-Rev2.html)
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(
            value = ["packageName", "channelId"],
            unique = true
        )
    ]
)
data class NotificationsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val channelId: String,
    val appLabel: String,
    val created: Long = 0L,
    val channelIndex: Int = 0,
    val groupKey: String,
    val firstImportance: Int = -1,
    val isBlocked: Boolean = true,
    val importance: Int = 0,
    val channelName: String = "",
    val isGoing: Boolean = false,
    val receivedCount: Long = 0L,
    val lastReceived: Long = 0L
)
