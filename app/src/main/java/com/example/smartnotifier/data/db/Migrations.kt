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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE notification_log
                    ADD COLUMN importance INTEGER NOT NULL DEFAULT 3
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. notification_log テーブルに channelName カラムを追加
            // デフォルト値として空文字 "" を設定します
            db.execSQL("ALTER TABLE notification_log ADD COLUMN channelName TEXT NOT NULL DEFAULT ''")

            // 2. notifications テーブルから値をコピー
            db.execSQL(
                """
                UPDATE notification_log
                SET channelName = (
                    SELECT n.channelName
                    FROM notifications AS n
                    WHERE n.packageName = notification_log.packageName 
                      AND n.channelId = notification_log.channelId
                )
                WHERE EXISTS (
                    SELECT 1 
                    FROM notifications AS n
                    WHERE n.packageName = notification_log.packageName 
                      AND n.channelId = notification_log.channelId
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE notification_log ADD COLUMN created INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE notification_log ADD COLUMN lastReceived INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE notification_log
                SET created = (
                    SELECT n.created
                    FROM notifications AS n
                    WHERE n.packageName = notification_log.packageName 
                      AND n.channelId = notification_log.channelId
                ),
                lastReceived = (
                    SELECT n.lastReceived
                    FROM notifications AS n
                    WHERE n.packageName = notification_log.packageName
                      AND n.channelId = notification_log.channelId
                )
                WHERE EXISTS (
                    SELECT 1 
                    FROM notifications AS n
                    WHERE n.packageName = notification_log.packageName 
                      AND n.channelId = notification_log.channelId
                )
                """.trimIndent()
            )
        }
    }
}