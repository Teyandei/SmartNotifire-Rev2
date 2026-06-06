package com.example.smartnotifier.core.tts

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
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * システムデフォルトの TTS を管理するクラス
 * 設計書：TTS(Text-to-Speech) / 発声機能 に基づく
 */
class TtsManager(
    context: Context,
    private val debugLogger: ((String) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val utteranceCounter = AtomicInteger()
    private val activeUtterances = ConcurrentHashMap<String, SpeechRequest>()
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initializationFailed = false
    private var pendingSpeech: SpeechRequest? = null
    @Volatile
    private var isShutdown = false

    private data class SpeechRequest(
        val text: String,
        val queueMode: Int,
        val canRetry: Boolean
    )

    init {
        createTts("init")
    }

    @Synchronized
    override fun onInit(status: Int) {
        if (isShutdown) return

        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                logError("Language not supported result=$result")
            } else {
                isInitialized = true
                initializationFailed = false
                logDebug("Initialized language=${Locale.getDefault()}")
                pendingSpeech?.let {
                    pendingSpeech = null
                    speak(it.text, it.queueMode, it.canRetry)
                }
            }
        } else {
            isInitialized = false
            initializationFailed = true
            logError("Initialization failed status=$status")
        }
    }

    /**
     * テキストを発声する
     * @param text 読み上げる文字列
     * @param queueMode TextToSpeech.QUEUE_FLUSH (即時) または QUEUE_ADD (キュー追加)
     */
    @Synchronized
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        speak(text, queueMode, canRetry = true)
    }

    @Synchronized
    private fun speak(text: String, queueMode: Int, canRetry: Boolean) {
        if (isShutdown) return

        val request = SpeechRequest(text, queueMode, canRetry)
        val currentTts = tts
        if (initializationFailed) {
            logError("speak requested after initialization failure")
            resetTts("init_failed_on_speak", request.takeIf { it.canRetry }?.copy(canRetry = false))
            return
        }

        if (!isInitialized || currentTts == null) {
            pendingSpeech = request
            logDebug("Pending speak initialized=$isInitialized hasTts=${currentTts != null}")
            return
        }

        val utteranceId = "smart-notifier-${System.currentTimeMillis()}-${utteranceCounter.incrementAndGet()}"
        activeUtterances[utteranceId] = request
        val result = currentTts.speak(text, queueMode, null, utteranceId)
        logDebug("speak result=$result utteranceId=$utteranceId queueMode=$queueMode canRetry=$canRetry")

        if (result == TextToSpeech.ERROR) {
            activeUtterances.remove(utteranceId)
            logError("speak returned ERROR utteranceId=$utteranceId")
            resetTts("speak_return_error", request.takeIf { it.canRetry }?.copy(canRetry = false))
        }
    }

    private fun createTts(reason: String) {
        logDebug("Create TTS reason=$reason")
        tts = TextToSpeech(appContext, this).also {
            it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    logDebug("onStart utteranceId=$utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    utteranceId?.let { activeUtterances.remove(it) }
                    logDebug("onDone utteranceId=$utteranceId")
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    handleUtteranceError(utteranceId, null)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    handleUtteranceError(utteranceId, errorCode)
                }
            })
        }
    }

    private fun handleUtteranceError(utteranceId: String?, errorCode: Int?) {
        val request = utteranceId?.let { activeUtterances.remove(it) }
        logError("onError utteranceId=$utteranceId errorCode=$errorCode canRetry=${request?.canRetry}")

        if (utteranceId != null && request == null) return

        val retryRequest = request
            ?.takeIf { it.canRetry }
            ?.copy(canRetry = false)
        resetTts("utterance_error", retryRequest)
    }

    private fun resetTts(reason: String, retryRequest: SpeechRequest?) {
        mainHandler.post {
            resetTtsOnMain(reason, retryRequest)
        }
    }

    @Synchronized
    private fun resetTtsOnMain(reason: String, retryRequest: SpeechRequest?) {
        if (isShutdown) return

        logError("Reset TTS reason=$reason retry=${retryRequest != null}")
        pendingSpeech = retryRequest
        activeUtterances.clear()
        isInitialized = false
        initializationFailed = false

        tts?.stop()
        tts?.shutdown()
        tts = null
        createTts(reason)
    }

    private fun logDebug(message: String) {
        Log.d(TAG, message)
        debugLogger?.invoke("TTS DEBUG $message")
    }

    private fun logError(message: String) {
        Log.e(TAG, message)
        debugLogger?.invoke("TTS ERROR $message")
    }

    companion object {
        private const val TAG = "TtsManager"
    }

    /**
     * リソースを解放する (メモリリーク防止)
     */
    @Synchronized
    fun shutdown() {
        isShutdown = true
        pendingSpeech = null
        activeUtterances.clear()
        isInitialized = false
        initializationFailed = false

        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
