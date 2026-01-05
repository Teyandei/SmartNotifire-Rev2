package com.example.smartnotifier.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Collections

/**
 * TTS のライフサイクルとキュー管理を担うシングルトン。
 * - 3 秒遅延後に順次再生
 * - キュー内に同一メッセージがある場合はスキップ
 */
object SpeechQueue : TextToSpeech.OnInitListener {

    private const val DELAY_MILLIS = 3_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val messageChannel = Channel<Pair<String, Long>>(Channel.UNLIMITED)
    private val pendingMessages = Collections.synchronizedSet(mutableSetOf<String>())

    @Volatile
    private var isReady: Boolean = false

    private var textToSpeech: TextToSpeech? = null
    private var processorStarted = false

    fun initialize(context: Context) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        }
        if (!processorStarted) {
            processorStarted = true
            scope.launch {
                for ((message, delayMillis) in messageChannel) {
                    var waitCount = 0
                    while (!isReady && waitCount < 60) { // 最大3秒待機
                        delay(50)
                        waitCount++
                    }
                    if (!isReady) {
                        pendingMessages.remove(message)
                        continue
                    }
                    if (delayMillis > 0) delay(delayMillis)
                    speakInternal(message)
                    pendingMessages.remove(message)
                }
            }
        }
    }

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        if (isReady) {
            textToSpeech?.language = Locale.getDefault()
        }
    }

    /**
     * 通知検出用: 3 秒遅延・重複スキップあり
     */
    fun enqueue(context: Context, message: String) {
        enqueueInternal(context, message, DELAY_MILLIS, deduplicate = true)
    }

    /**
     * 手動再生用: 遅延なし、キューに入っていなければ即再生
     */
    fun speakImmediately(context: Context, message: String) {
        enqueueInternal(context, message, 0, deduplicate = true)
    }

    private fun enqueueInternal(
        context: Context,
        message: String,
        delayMillis: Long,
        deduplicate: Boolean
    ) {
        if (message.isBlank()) return
        initialize(context)
        if (deduplicate && pendingMessages.contains(message)) return
        pendingMessages.add(message)
        messageChannel.trySend(message to delayMillis)
    }

    private fun speakInternal(message: String) {
        if (!isReady) return
        textToSpeech?.speak(
            message,
            TextToSpeech.QUEUE_ADD,
            null,
            message.hashCode().toString()
        )
    }

    fun shutdown() {
        scope.cancel()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
