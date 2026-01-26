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
import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.smartnotifier.R
import com.example.smartnotifier.core.tts.TtsManager
import com.example.smartnotifier.data.db.DatabaseProvider
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import com.example.smartnotifier.data.repository.NotificationLogRepository
import com.example.smartnotifier.data.repository.RulesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class SmartNotificationListenerService : NotificationListenerService() {

    private lateinit var logRepo: NotificationLogRepository
    private lateinit var ruleRepo: RulesRepository

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

        private const val THIS_CLASS :String = "SmartNotificationListenerService"
    }

    override fun onCreate() {
        super.onCreate()
        val db = DatabaseProvider.get(applicationContext)
        logRepo = NotificationLogRepository(db)
        ruleRepo = RulesRepository(db)
        ttsManager = TtsManager(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        serviceScope.cancel()
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
        val pm = this.packageManager
        val packageName = sbn.packageName
        var appLabel: String
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appLabel = pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.w(THIS_CLASS, "onNotificationPosted： $packageName. ", e)
            appLabel = ""
        }
        val log = NotificationLogEntity(
            packageName = packageName,
            appLabel = appLabel,
            channelId = sbn.notification.channelId,
            title = sbn
                .notification.extras
                .getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
                .orEmpty()
        )
        serviceScope.launch {
            if (!log.appLabel.isEmpty()) saveToLog(log)  // アプリ名がある時のみ通知ログに保存

            // 音を慣らして良いときのみTTS
            if (canSpeakNow(applicationContext)) checkRulesAndSpeak(log)
            else Log.w(THIS_CLASS, "tts disabled")
        }
    }

    /**
     * 通知ログに保存する
     *
     * @param log 通知ログ
     */
    private suspend fun saveToLog(log: NotificationLogEntity) {
        try {
            val logCount = logRepo.getLogCount(log.packageName, log.channelId, log.title)
            if (logCount == 0) logRepo.insert(log)  // 新規のみ保存
        } catch (e: Exception) {
            Log.e(THIS_CLASS, "saveToLog: $log", e)
        }
        finally {
            logRepo.trimLogs(100)
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

    /**
     * 検索タイトルのヒット確認とTTS読み上げ
     *
     * @param log 通知ログ
     */
    private suspend fun checkRulesAndSpeak(log: NotificationLogEntity) {
        val rules = ruleRepo.getRulesByPackageAndChannel(log.packageName, log.channelId).first()

        val matchedRule = rules.find { rule ->
            rule.enabled &&
            rule.packageName == log.packageName &&
            rule.channelId == log.channelId &&
            (rule.srhTitle.isBlank() || log.title.contains(rule.srhTitle, ignoreCase = true))
        }

        matchedRule?.let { rules ->
            var message = rules.voiceMsg
            if (message.isBlank()) {
                message = applicationContext.getString(R.string.spk_msg_default, rules.appLabel)
            }
            processVoiceQueue(message)
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
