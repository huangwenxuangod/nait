package com.nailit.app.core.preview

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

class PcmAudioPlayer {
    private val sampleRate = 24_000
    private val channelMask = AudioFormat.CHANNEL_OUT_MONO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT
    private var track: AudioTrack? = null

    fun start() {
        if (track != null) return

        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding)
        track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(encoding)
                .setChannelMask(channelMask)
                .build(),
            maxOf(minBuffer, 4096),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        ).apply {
            play()
        }
    }

    fun write(bytes: ByteArray) {
        if (track == null) start()
        track?.write(bytes, 0, bytes.size)
    }

    fun stop() {
        runCatching {
            track?.stop()
            track?.release()
        }
        track = null
    }
}
