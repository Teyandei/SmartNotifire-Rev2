package com.example.smartnotifier.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.smartnotifier.data.db.entity.NotificationLogEntity

@Dao
interface NotificationLogDao {

    @Query("SELECT * FROM notification_log ORDER BY id DESC LIMIT :limit")
    fun getLatestLogs(limit: Int = 100): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: NotificationLogEntity)

    @Query("DELETE FROM notification_log WHERE id NOT IN (" +
            "SELECT id FROM notification_log ORDER BY id DESC LIMIT :limit)")
    suspend fun trimLogs(limit: Int = 100)
}