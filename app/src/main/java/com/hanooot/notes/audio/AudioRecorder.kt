package com.hanooot.notes.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Records to an m4a file inside app storage. Unlike the browser version the
 * file persists, so memos are still there after a restart.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    fun start(dir: File): File {
        stop()
        val file = File(dir, "memo_${System.currentTimeMillis()}.m4a")

        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        recorder = rec
        outputFile = file
        startedAt = System.currentTimeMillis()
        return file
    }

    /** Elapsed recording time in milliseconds. */
    fun elapsedMs(): Long = if (startedAt == 0L) 0 else System.currentTimeMillis() - startedAt

    /** 0f..1f input level, for the live meter. */
    fun amplitude(): Float {
        val r = recorder ?: return 0f
        val amp = runCatching { r.maxAmplitude }.getOrDefault(0)
        return (amp / 32767f).coerceIn(0f, 1f)
    }

    /** Returns the finished file and its duration, or null if nothing was captured. */
    fun stop(): Pair<File, Long>? {
        val rec = recorder ?: return null
        val file = outputFile
        val duration = elapsedMs()

        // stop() throws if it's called before any audio was captured
        runCatching { rec.stop() }
        runCatching { rec.reset() }
        runCatching { rec.release() }

        recorder = null
        outputFile = null
        startedAt = 0L

        return if (file != null && file.exists() && file.length() > 0) {
            file to duration
        } else {
            file?.delete()
            null
        }
    }

    fun cancel() {
        val file = outputFile
        stop()
        runCatching { file?.delete() }
    }
}
