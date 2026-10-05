package com.hanooot.notes.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Live speech-to-text using the on-device recogniser, running alongside the
 * recorder so one pass produces both audio and text.
 *
 * Note this is Android's built-in recogniser, not a large speech model — it is
 * fast and free but less accurate than a server-side model on long or noisy
 * audio.
 */
class Transcriber(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var finalText = StringBuilder()
    private var listening = false

    var onUpdate: ((finalText: String, partial: String) -> Unit)? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /** @param languageTag a BCP-47 tag such as `ar-IQ` or `en-US`. */
    fun start(languageTag: String) {
        if (!isAvailable()) return
        stop()
        finalText = StringBuilder()
        listening = true

        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            // Some recognisers honour only the preferred-language extra.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) finalText.append(text).append(' ')
                onUpdate?.invoke(finalText.toString(), "")
                // The recogniser stops after each pause; restart to keep going
                if (listening) restart(intent)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                onUpdate?.invoke(finalText.toString(), partial)
            }

            override fun onError(error: Int) {
                // Timeouts are normal during silence — just start listening again
                if (listening &&
                    (error == SpeechRecognizer.ERROR_NO_MATCH ||
                     error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                ) restart(intent)
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        runCatching { sr.startListening(intent) }
    }

    private fun restart(intent: Intent) {
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.startListening(intent) }
    }

    fun stop(): String {
        listening = false
        runCatching { recognizer?.stopListening() }
        runCatching { recognizer?.destroy() }
        recognizer = null
        return finalText.toString().trim()
    }
}
