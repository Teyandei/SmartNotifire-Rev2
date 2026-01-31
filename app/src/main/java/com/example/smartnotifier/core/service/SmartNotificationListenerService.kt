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
import android.content.Intent
import android.content.pm.PackageManager
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
        private const val DO_NOT_SELECT: String = "ERR: Don't select this."
    }

    override fun onCreate() {
        super.onCreate()
        val db = DatabaseProvider.get(applicationContext)
        logRepo = NotificationLogRepository(db)
        ruleRepo = RulesRepository(db)
        ttsManager = TtsManager(this)
    }

    override fun onDestroy() {
        Log.w(THIS_CLASS, "onDestroy")
        serviceScope.cancel()
        ttsManager.shutdown()
        super.onDestroy()
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.w(THIS_CLASS, "onTaskRemoved")
        super.onTaskRemoved(rootIntent)
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
        val pm = applicationContext.packageManager
        val packageName = sbn.packageName   // パッケージ名
        val channelId = sbn.notification.channelId?: return
        var appLabel = DO_NOT_SELECT        // アプリ名 ※途中エラーではこの初期値を生かすこと
        /**
         * 通知タイトル
         */
        val title = sbn
            .notification.extras
            .getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
            .orEmpty()

        serviceScope.launch {
            try {
                val label: String = logRepo.getAppLabel(packageName, channelId)?: ""
                if (label.isEmpty()) {
                    /*
                     * 既存レコードが無い可能性がある場合はアプリ名を取得する
                     */
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    appLabel = pm.getApplicationLabel(appInfo).toString()
                } else {
                    appLabel = label
                }
            } catch (_: PackageManager.NameNotFoundException) {
                Log.w(THIS_CLASS, "onNotificationPosted NameNotFoundException： $packageName. ")
            } catch (e: Exception) {
                Log.e(THIS_CLASS, "onNotificationPosted： $packageName. ", e)
            }


            /**
             * 通知ログ
             */
            val log = NotificationLogEntity(
                packageName = packageName,
                channelId = channelId,
                appLabel = appLabel,
            )

            if (appLabel != DO_NOT_SELECT && appLabel.isNotBlank()) {
                logRepo.insertOrCount(log)  // ログの追加又はカウント
                if (canSpeakNow(applicationContext)) checkRulesAndSpeak(log, title)
                else Log.w(THIS_CLASS, "tts disabled")
            }
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
     * @param title 受信した通知タイトル
     */
    private suspend fun checkRulesAndSpeak(log: NotificationLogEntity, title: String) {
        val rules = ruleRepo.getRulesByPackageAndChannel(log.packageName, log.channelId).first()

        val matchedRule = rules.find { rule ->
            rule.enabled &&
            rule.packageName == log.packageName &&
            rule.channelId == log.channelId &&
            (rule.srhTitle.isBlank() || title.contains(rule.srhTitle, ignoreCase = true))
        }

        matchedRule?.let { rules ->
            var message = rules.voiceMsg
            if (message.isBlank()) {
                message = applicationContext.getString(R.string.spk_msg_default, rules.appLabel)
            }
            processVoiceQueue(message)
        }
    }

    /**
     * TTSによる読み上げ
     *
     * キュー形式のバッファを持ち、後続に同じ文字列があった場合はTTSを使わずリターンする。。
     * 文字列の内容は呼び出し側で操作すること。
     * この関数の呼び出し->3秒待->TTS発声->5病後メッセージ削除
     *
     * @param message 読み上げる文字列
     */
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
            // serviceScope が cancel 済みでも、ここだけは確実に走らせる
            withContext(NonCancellable) {
                delay(5000)
                recentlySpokenMessages.remove(message)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(THIS_CLASS, "onListenerConnected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(THIS_CLASS, "onListenerDisconnected")
    }

}
