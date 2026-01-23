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

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
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
            // “表示できる”ことをここで検証する
            val label = pm.getApplicationLabel(appInfo).toString()
            pm.getApplicationIcon(appInfo) // 取れなければ例外になる

            label.isNotBlank()
        } catch (_: PackageManager.NameNotFoundException) {
            Log.w("NotificationListener", "Skip logging: Package not visible - $packageName")
            false
        } catch (_: SecurityException) {
            // Work profile/他ユーザー等で起きうる
            Log.w("NotificationListener", "Skip logging: security exception - $packageName")
            false
        } catch (e: Exception) {
            Log.e("NotificationListener", "Skip logging: unexpected error - $packageName", e)
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

        // 音を慣らしてはいけない時はリターン
        if (!canSpeakNow(this)) return

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

    /**
     * TTSを使えるかを判断する。
     * @return TTSが使え以下の場合はfalseを返す。
     * 1.マナーモード時
     * 2.おやすみモード時
     */
    private fun canSpeakNow(context: Context): Boolean {
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager

        // マナーモード（サイレント / バイブ）は即NG
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            return false
        }

        // おやすみモード（DND）も安全側に倒す
        val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        return nm.currentInterruptionFilter ==
                NotificationManager.INTERRUPTION_FILTER_ALL
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
