package com.example.smartnotifier.data.repository

import com.example.smartnotifier.data.db.AppDatabase
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import androidx.room.withTransaction

class NotificationLogRepository(
    private val db: AppDatabase
) {
    private val dao = db.notificationLogDao()

    suspend fun insert(log: NotificationLogEntity) = dao.insert(log)
    suspend fun trimLogs(limit: Int = 100) = dao.trimLogs(limit)

    fun observeLatestLogs(limit: Int = 100) = dao.getLatestLogs(limit)

    suspend fun insertAndTrim(log: NotificationLogEntity, limit: Int = 100) {
        db.withTransaction {
            dao.insert(log)
            dao.trimLogs(limit)
        }
    }
}
