package com.example.smartnotifier.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * システムデフォルトの TTS を管理するクラス
 * 設計書：TTS(Text-to-Speech) / 発声機能 に基づく
 */
class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsManager", "Language not supported")
            } else {
                isInitialized = true
                // 初期化待ちのテキストがあれば再生
                pendingText?.let {
                    speak(it)
                    pendingText = null
                }
            }
        } else {
            Log.e("TtsManager", "Initialization failed")
        }
    }

    /**
     * テキストを発声する
     * @param text 読み上げる文字列
     * @param queueMode TextToSpeech.QUEUE_FLUSH (即時) または QUEUE_ADD (キュー追加)
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isInitialized) {
            tts?.speak(text, queueMode, null, null)
        } else {
            pendingText = text
        }
    }

    /**
     * リソースを解放する (メモリリーク防止)
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
