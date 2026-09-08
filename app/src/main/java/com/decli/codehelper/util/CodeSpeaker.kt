package com.decli.codehelper.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.decli.codehelper.model.PickupCodeItem
import java.util.Locale

/**
 * 取件码语音播报。逐字朗读、语速 0.75×，连字符读「杠」。
 *
 * 本机没有可用语音引擎时 [isAvailable] 保持 false，界面按钮变灰但不隐藏，
 * 点按给出「本机没有语音引擎」的提示而不是静默失败。
 */
class CodeSpeaker(
    context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var onDone: (() -> Unit)? = null
    private var engineReady = false
    private var pendingRelease = false

    /** 引擎是否可用，Compose 可直接读取 */
    var isAvailable by mutableStateOf(false)
        private set

    private val tts: TextToSpeech? = runCatching {
        TextToSpeech(context.applicationContext) { status ->
            mainHandler.post {
                engineReady = status == TextToSpeech.SUCCESS
                if (engineReady) {
                    applyEngineConfig()
                }
                isAvailable = engineReady
                if (pendingRelease) {
                    release()
                }
            }
        }
    }.getOrNull()

    private fun applyEngineConfig() {
        val engine = tts ?: return
        runCatching {
            val languageResult = engine.setLanguage(Locale.CHINA)
            if (
                languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                engine.setLanguage(Locale.getDefault())
            }
            engine.setSpeechRate(SPEECH_RATE)
            engine.setOnUtteranceProgressListener(
                @Suppress("OVERRIDE_DEPRECATION")
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { notifyFinished() }
                    }

                    override fun onError(utteranceId: String?) {
                        mainHandler.post { notifyFinished() }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        mainHandler.post { notifyFinished() }
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        mainHandler.post { notifyFinished() }
                    }
                },
            )
        }
    }

    fun textFor(item: PickupCodeItem): String =
        CodeSpeech.speechText(senderShort = item.senderShort, codes = item.codes)

    /** 开始朗读；同一时刻只朗读一条，重复调用会打断上一条 */
    fun speak(item: PickupCodeItem, onFinished: () -> Unit) {
        val engine = tts
        if (engine == null || !engineReady) {
            onFinished()
            return
        }
        onDone = onFinished
        runCatching {
            engine.stop()
            engine.speak(textFor(item), TextToSpeech.QUEUE_FLUSH, null, item.uniqueKey)
        }.onFailure { notifyFinished() }
    }

    /** 停止朗读；不会触发完成回调之外的副作用 */
    fun stop() {
        runCatching { tts?.stop() }
        notifyFinished()
    }

    fun release() {
        if (!engineReady && tts != null) {
            // 引擎还在初始化，等初始化回调再释放，避免 shutdown 竞态
            pendingRelease = true
            return
        }
        pendingRelease = false
        onDone = null
        runCatching { tts?.shutdown() }
        isAvailable = false
    }

    private fun notifyFinished() {
        val callback = onDone
        onDone = null
        callback?.invoke()
    }

    private companion object {
        const val SPEECH_RATE = 0.75f
    }
}
