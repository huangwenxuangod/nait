package com.nailit.app.core.preview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

class PcmAudioRecorder(
    private val context: Context,
    private val onChunk: (ByteArray) -> Unit,
) {
    private val sampleRate = 16_000
    private val channelMask = AudioFormat.CHANNEL_IN_MONO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT
    private val isRecording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null

    fun canRecord(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun start() {
        if (isRecording.get()) return
        if (!canRecord()) return

        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding)
        val bufferSize = maxOf(minBuffer, 3200)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelMask,
            encoding,
            bufferSize,
        )

        audioRecord = recorder
        isRecording.set(true)
        recorder.startRecording()

        worker = Thread {
            val buffer = ByteArray(3200)
            while (isRecording.get()) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    onChunk(buffer.copyOf(read))
                }
            }
        }.also {
            it.name = "NailItPcmAudioRecorder"
            it.start()
        }
    }

    fun stop() {
        isRecording.set(false)
        runCatching { worker?.join(300) }
        worker = null
        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null
    }
}
