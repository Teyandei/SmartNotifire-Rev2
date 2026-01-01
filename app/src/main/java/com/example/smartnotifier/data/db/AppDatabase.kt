package com.example.smartnotifier.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smartnotifier.data.db.entity.RuleEntity
import com.example.smartnotifier.data.db.entity.NotificationLogEntity

@Database(
    entities = [
        RuleEntity::class,
        NotificationLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun notificationLogDao(): NotificationLogDao
}