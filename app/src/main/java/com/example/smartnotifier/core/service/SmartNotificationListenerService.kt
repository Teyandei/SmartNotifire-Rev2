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

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.smartnotifier.BuildConfig
import com.example.smartnotifier.R
import com.example.smartnotifier.core.tts.TtsManager
import com.example.smartnotifier.data.db.DatabaseProvider
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import com.example.smartnotifier.data.db.entity.NotificationsEntity
import com.example.smartnotifier.data.repository.NotificationLogRepository
import com.example.smartnotifier.data.repository.NotificationTitleCacheRepository
import com.example.smartnotifier.data.repository.NotificationsRepository
import com.example.smartnotifier.data.repository.RulesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class SmartNotificationListenerService : NotificationListenerService() {

    private lateinit var logRepo: NotificationLogRepository
    private lateinit var ruleRepo: RulesRepository
    private lateinit var notificationsRepo: NotificationsRepository
    private lateinit var titleCacheRepo : NotificationTitleCacheRepository


    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var ttsManager: TtsManager
    
    // 最近発声したメッセージを保持するセット（重複回避用）
    private val recentlySpokenMessages =
        java.util.Collections.newSetFromMap(
            java.util.concurrent.ConcurrentHashMap<String, Boolean>()
        )

    private val speakMutex = Mutex()

    private val recentNotificationKeys = mutableMapOf<String, Long>()

    private enum class SpeakDecision {
        SPEAK,
        SKIP_BLOCKED,
        SKIP_CANNOT_SPEAK_NOW,
        SKIP_LOW_IMPORTANCE,
        SKIP_ONGOING,
        SKIP_GROUP_SUMMARY,
        SKIP_DUPLICATE_KEY,
        SKIP_IS_AMBIENT
    }

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
        private const val DUPLICATE_WINDOW_MS = 10_000L
    }

    override fun onCreate() {
        super.onCreate()
        val db = DatabaseProvider.get(applicationContext)
        logRepo = NotificationLogRepository(db)
        ruleRepo = RulesRepository(db)
        notificationsRepo = NotificationsRepository(db)
        ttsManager = TtsManager(this)
        titleCacheRepo = NotificationTitleCacheRepository(db.notificationTitleCacheDao())
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
    override fun onNotificationPosted(sbn : StatusBarNotification?, rankingMap: RankingMap) {
        sbn ?: return
        val pm = applicationContext.packageManager
        val packageName = sbn.packageName   // パッケージ名
        val channelId = sbn.notification.channelId?: return
        var appLabel = DO_NOT_SELECT        // アプリ名 ※途中エラーではこの初期値を生かすこと
        val ranking = Ranking()
        val rankingOk = rankingMap.getRanking(sbn.key, ranking)
        val isBlocked = if (rankingOk) !ranking.matchesInterruptionFilter() else true   // おやすみモードなどのブロック
        val importance = if (rankingOk) ranking.importance else -1  // 重要度
        val channel = if (rankingOk) ranking.channel else null
        val isAmbient = if (rankingOk) ranking.isAmbient else false
        val channelName = channel?.name?.toString() ?: ""           // チャンネル名
        val groupKey = sbn.groupKey         // グループKey
        val isGoing = sbn.isOngoing         // 連続的な通知(進捗など)
        val now = System.currentTimeMillis()


        /**
         * 通知タイトル
         */
        val title = sbn
            .notification.extras
            .getCharSequence(Notification.EXTRA_TITLE)?.toString()
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
                importance = importance,
                channelName = channelName,
                lastReceived = now
            )

            if (BuildConfig.DEBUG) {
                /**
                 * 通知内容
                 */
                val ntf = NotificationsEntity(
                    packageName = packageName,
                    channelId = channelId,
                    appLabel = appLabel,
                    groupKey = groupKey,
                    isBlocked = isBlocked,
                    importance = importance,
                    channelName = channelName,
                    isGoing = isGoing,
                    lastReceived = now
                )
                notificationsRepo.upsertNotification(ntf)
            }

            val decision = shouldSpeakNotification(sbn, isBlocked, importance, isAmbient)

            if (appLabel != DO_NOT_SELECT && appLabel.isNotBlank() && !isGoing) {
                logRepo.upsertNotificationLog(log)  // ログの追加又はカウント
                titleCacheRepo.saveTitle(packageName, channelId, title) // 検索タイトル入力補助用キャッシュ

                when (decision) {
                    SpeakDecision.SPEAK -> {
                        if (checkRulesAndSpeak(log, title)) {
                            rememberSpokenNotification(sbn)
                            writeDecisionLog("SPEAK", "RULE_MATCH", sbn, title)
                        } else {
                            writeDecisionLog("SKIP", "NO_RULE_MATCH", sbn, title)
                        }
                    }
                    else -> {
                        writeDecisionLog("SKIP", decision.name, sbn, title)
                    }
                }
            } else {
                writeDecisionLog("IGNORE", decision.name, sbn, title)
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
     * @return true:ルールにヒットしてTTS, false: ルールにヒットしなかった
     */
    private suspend fun checkRulesAndSpeak(log: NotificationLogEntity, title: String): Boolean {
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
        return matchedRule != null
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
                    speakAt(message)
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

    private fun formatTime(millis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    private fun speakAt(text: String) {
        val file = File(filesDir, "notif_log.txt")
        if (BuildConfig.DEBUG) {
            file.appendText("🟩Speak：${text} at ${formatTime(System.currentTimeMillis())}\n")
        }
    }

    private fun isRecentDuplicate(key: String, now: Long): Boolean {
        val lastTime = recentNotificationKeys[key] ?: return false
        return (now - lastTime) < DUPLICATE_WINDOW_MS
    }

    private fun rememberSpokenNotification(sbn: StatusBarNotification) {
        val now = System.currentTimeMillis()
        recentNotificationKeys[sbn.key] = now

        // 古いキーを軽く掃除
        recentNotificationKeys.entries.removeAll { (_, time) ->
            now - time > DUPLICATE_WINDOW_MS
        }
    }

    private fun shouldSpeakNotification(
        sbn: StatusBarNotification,
        isBlocked: Boolean,
        importance: Int,
        isAmbient: Boolean
    ): SpeakDecision {
        if (isBlocked) return SpeakDecision.SKIP_BLOCKED
        if (!canSpeakNow(applicationContext)) return SpeakDecision.SKIP_CANNOT_SPEAK_NOW
        if (importance < NotificationManager.IMPORTANCE_DEFAULT) return SpeakDecision.SKIP_LOW_IMPORTANCE
        if (sbn.isOngoing) return SpeakDecision.SKIP_ONGOING

        val isGroupSummary =
            (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary) return SpeakDecision.SKIP_GROUP_SUMMARY

        val now = System.currentTimeMillis()
        if (isRecentDuplicate(sbn.key, now)) return SpeakDecision.SKIP_DUPLICATE_KEY
        if (isAmbient) return SpeakDecision.SKIP_IS_AMBIENT
        return SpeakDecision.SPEAK
    }

    private fun writeDecisionLog(
        action: String,
        reason: String,
        sbn: StatusBarNotification,
        title: String
    ) {
        if (!BuildConfig.DEBUG) return

        val text = "[${formatTime(System.currentTimeMillis())}] $action $reason pkg=${sbn.packageName} title=$title key=${sbn.key}\n"
        File(filesDir, "notif_log.txt").appendText(text)
    }

}
