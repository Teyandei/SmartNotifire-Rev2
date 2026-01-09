package com.example.smartnotifier.core.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.smartnotifier.core.tts.TtsManager
import com.example.smartnotifier.data.db.DatabaseProvider
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class SmartNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var ttsManager: TtsManager
    
    // 最近発声したメッセージを保持するセット（重複回避用）
    private val recentlySpokenMessages = mutableSetOf<String>()

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

    /**
     * 通知がステータスバーに表示されたときに呼ばれるコールバック。
     *
     * この時点では通知はまだユーザーに表示されておらず、
     * 内容の解析やログ保存に適している。
     *
     * @param sbn システムから渡される通知情報
     */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
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
        if (recentlySpokenMessages.contains(message)) {
            Log.d("SmartService", "Skip duplicate message: $message")
            return
        }

        recentlySpokenMessages.add(message)
        delay(3000)

        withContext(Dispatchers.Main) {
            ttsManager.speak(message)
        }

        serviceScope.launch {
            delay(5000)
            recentlySpokenMessages.remove(message)
        }
    }
}
