package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object ChimePlayer {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playHourlyChime() {
        scope.launch {
            playNotes(listOf(659.25 to 0.35, 523.25 to 0.35, 587.33 to 0.35, 392.00 to 0.85))
        }
    }

    fun playBlockStartChime() {
        scope.launch {
            playNotes(listOf(523.25 to 0.25, 659.25 to 0.25, 783.99 to 0.65))
        }
    }

    private fun playNotes(notes: List<Pair<Double, Double>>) {
        val sampleRate = 22050
        val totalSamples = notes.sumOf { (it.second * sampleRate).toInt() }
        if (totalSamples <= 0) return

        val pcm = ShortArray(totalSamples)
        var offset = 0
        for ((freq, durationSec) in notes) {
            val count = (durationSec * sampleRate).toInt()
            for (i in 0 until count) {
                val t = i.toDouble() / sampleRate
                val attack = (t / 0.015).coerceIn(0.0, 1.0)
                val decay = exp(-t * 3.8)
                val fundamental = sin(2.0 * PI * freq * t)
                val overtone = 0.32 * sin(2.0 * PI * (freq * 2.0) * t)
                val shimmer = 0.10 * sin(2.0 * PI * (freq * 3.0) * t)
                val sample = ((fundamental + overtone + shimmer) * attack * decay * 0.32 * Short.MAX_VALUE)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcm[offset + i] = sample.toShort()
            }
            offset += count
        }

        try {
            val minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferBytes = maxOf(minBuffer, pcm.size * 2)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep((notes.sumOf { it.second } * 1000).toLong() + 150L)
            track.stop()
            track.release()
        } catch (_: Exception) {
            // Audio hardware unavailable in headless test environments
        }
    }
}
