package com.example.smartnotifier.data.db

import android.content.Context
import androidx.room.Room
import kotlin.jvm.Volatile

object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "smart_notifier.db"
            ).build().also { instance = it }
        }
}
