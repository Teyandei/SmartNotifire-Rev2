package com.example.smartnotifier.core.notification

import android.app.Notification
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.smartnotifier.data.db.DatabaseProvider
import com.example.smartnotifier.data.db.entity.NotificationLogEntity
import com.example.smartnotifier.data.repository.NotificationLogRepository
import com.example.smartnotifier.data.repository.RulesRepository
import com.example.smartnotifier.core.tts.SpeechQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SmartNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database by lazy { DatabaseProvider.get(applicationContext) }
    private val rulesRepository by lazy { RulesRepository(database) }
    private val logRepository by lazy { NotificationLogRepository(database) }

    override fun onCreate() {
        super.onCreate()
        SpeechQueue.initialize(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val notification = sbn.notification ?: return
        val packageName = sbn.packageName ?: return
        val channelId = notification.channelId.orEmpty()
        val title = notification.extras
            ?.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            .orEmpty()

        serviceScope.launch {
            runCatching {
                val iconUri = resolveIconUri(packageName)
                val log = NotificationLogEntity(
                    packageName = packageName,
                    channelId = channelId,
                    notificationIcon = iconUri,
                    title = title
                )
                logRepository.insertAndTrim(log)

                val matchedRules = rulesRepository.findMatchingRules(packageName, channelId, title)
                matchedRules.forEach { rule ->
                    val message = rule.voiceMsg?.takeIf { it.isNotBlank() }
                        ?: rule.srhTitle.ifBlank { title }
                    if (!message.isNullOrBlank()) {
                        SpeechQueue.enqueue(applicationContext, message)
                    }
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to handle notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun resolveIconUri(packageName: String): Uri? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            if (appInfo.icon != 0) {
                Uri.parse("android.resource://$packageName/${appInfo.icon}")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "SmartNotifire"
    }
}
