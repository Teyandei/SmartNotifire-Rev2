package com.example.smartnotifier.core.service

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

import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.smartnotifier.core.tts.TtsManager
import com.example.smartnotifier.data.db.DatabaseProvider
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class SmartNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var ttsManager: TtsManager
    
    // 最近発声したメッセージを保持するセット（重複回避用）
    private val recentlySpokenMessages =
        java.util.Collections.newSetFromMap(
            java.util.concurrent.ConcurrentHashMap<String, Boolean>()
        )

    private val speakMutex = Mutex()


    companion object {
        /**
         * 通知アクセスの権限が許可されているか確認する
         */
        fun isPermissionGranted(context: Context): Boolean {
            return NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ttsManager = TtsManager(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        serviceScope.cancel()
    }

    private fun shouldLogNotification(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName
        val pm = this.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            // ここまで来たらアプリ名/アイコン取れる → OK
            true
        } catch (_: PackageManager.NameNotFoundException) {
            Log.w("NotificationListener", "Skip logging: Package not visible - $packageName")
            false
        } catch (e: Exception) {
            Log.e("NotificationListener", "Unexpected error checking package", e)
            false
        }
    }

    /**
     * 通知がステータスバーに表示されたときに呼ばれるコールバック。
     *
     * この時点では通知はまだユーザーに表示されておらず、
     * 内容の解析やログ保存に適している。
     *
     * @param sbn システムから渡される通知情報
     */
    override fun onNotificationPosted(sbn : StatusBarNotification?) {
        sbn ?: return
        if (!shouldLogNotification(sbn)) return

        val packageName = sbn.packageName
        val channelId = sbn.notification.channelId
        val extras = sbn.notification.extras
        val title = extras
            .getCharSequence(android.app.Notification.EXTRA_TITLE)
            ?.toString()
            .orEmpty()
        
        saveToLog(packageName, channelId, title)

        serviceScope.launch {
            checkRulesAndSpeak(packageName, channelId, title)
        }
    }

    private fun saveToLog(packageName: String, channelId: String, title: String) {
        serviceScope.launch {
            val db = DatabaseProvider.get(this@SmartNotificationListenerService)
            val logDao = db.notificationLogDao()

            val logSameCount = logDao.getLogCount(packageName, channelId, title)
            if (logSameCount == 0) {
                val logEntry = NotificationLogEntity(
                    packageName = packageName,
                    channelId = channelId,
                    title = title
                )
                logDao.insert(logEntry)
            }
            logDao.trimLogs(100)
        }
    }

    private suspend fun checkRulesAndSpeak(packageName: String, channelId: String, title: String) {
        val db = DatabaseProvider.get(this@SmartNotificationListenerService)
        val ruleDao = db.ruleDao()
        
        val rules = ruleDao.getAllRulesDesc().first()
        
        val matchedRule = rules.find { rule ->
            rule.enabled && 
            rule.packageName == packageName &&
            rule.channelId == channelId &&
            (rule.srhTitle.isBlank() || title.contains(rule.srhTitle, ignoreCase = true))
        }

        matchedRule?.voiceMsg?.let { msg ->
            if (msg.isNotBlank()) {
                processVoiceQueue(msg)
            }
        }
    }

    private suspend fun processVoiceQueue(message: String) {
        val isNew = recentlySpokenMessages.add(message)
        if (!isNew) return

        try {
            recentlySpokenMessages.add(message)
            delay(3000)

            speakMutex.withLock {
                withContext(Dispatchers.Main) {
                    ttsManager.speak(message)
                }
            }
        } finally {
            serviceScope.launch {
                delay(5000)
                recentlySpokenMessages.remove(message)
            }
        }
    }
}
