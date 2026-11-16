package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object ChimePlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private var activePlaybackJob: Job? = null
    private var activeMediaPlayer: MediaPlayer? = null
    private var activeAudioTrack: AudioTrack? = null

    private const val MAX_CUSTOM_FULL_MS = 12_000L
    private const val MAX_CUSTOM_SUB_MS = 2_800L

    fun playHourlyChime(
        tone: ChimeTone = ChimeTone.WESTMINSTER,
        strikeHourCount: Int = 0,
        customFile: File? = null
    ) {
        launchExclusive {
            playChimeSync(
                tone = tone,
                subInterval = false,
                strikeHourCount = strikeHourCount,
                customFile = customFile
            )
        }
    }

    fun playSubIntervalChime(
        tone: ChimeTone = ChimeTone.WESTMINSTER,
        customFile: File? = null
    ) {
        launchExclusive {
            playChimeSync(
                tone = tone,
                subInterval = true,
                strikeHourCount = 0,
                customFile = customFile
            )
        }
    }

    fun playBlockStartChime() {
        launchExclusive {
            playNotes(
                notes = listOf(523.25 to 0.25, 659.25 to 0.25, 783.99 to 0.65),
                volumeScale = 0.30,
                decayRate = 3.8
            )
        }
    }

    fun stopCurrentPlayback() {
        synchronized(lock) {
            activePlaybackJob?.cancel()
            activePlaybackJob = null
            releaseActiveAudioLocked()
        }
    }

    private fun launchExclusive(block: () -> Unit) {
        synchronized(lock) {
            activePlaybackJob?.cancel()
            releaseActiveAudioLocked()
            activePlaybackJob = scope.launch {
                block()
            }
        }
    }

    private fun releaseActiveAudioLocked() {
        try {
            activeMediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {
        }
        activeMediaPlayer = null

        try {
            activeAudioTrack?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
        }
        activeAudioTrack = null
    }

    fun playChimeSync(
        tone: ChimeTone,
        subInterval: Boolean,
        strikeHourCount: Int = 0,
        customFile: File? = null
    ) {
        val playedCustom = if (customFile != null && customFile.exists() && customFile.length() > 0L) {
            playCustomFileSync(
                file = customFile,
                maxDurationMs = if (subInterval) MAX_CUSTOM_SUB_MS else MAX_CUSTOM_FULL_MS,
                volume = if (subInterval) 0.58f else 0.92f
            )
        } else {
            false
        }

        if (!playedCustom) {
            when (tone) {
                ChimeTone.WESTMINSTER -> {
                    val notes = if (subInterval) {
                        listOf(659.25 to 0.30, 523.25 to 0.55)
                    } else {
                        listOf(659.25 to 0.35, 523.25 to 0.35, 587.33 to 0.35, 392.00 to 0.85)
                    }
                    playNotes(notes, volumeScale = if (subInterval) 0.24 else 0.32, decayRate = 3.6)
                }

                ChimeTone.CRYSTAL_BELL -> {
                    val notes = if (subInterval) {
                        listOf(1046.50 to 0.30, 1318.51 to 0.55)
                    } else {
                        listOf(1046.50 to 0.32, 1318.51 to 0.32, 1567.98 to 0.80)
                    }
                    playNotes(notes, volumeScale = if (subInterval) 0.20 else 0.28, decayRate = 4.2)
                }

                ChimeTone.WARM_MARIMBA -> {
                    val notes = if (subInterval) {
                        listOf(392.00 to 0.24, 587.33 to 0.42)
                    } else {
                        listOf(392.00 to 0.22, 493.88 to 0.22, 587.33 to 0.22, 783.99 to 0.60)
                    }
                    playNotes(notes, volumeScale = if (subInterval) 0.26 else 0.34, decayRate = 5.4)
                }

                ChimeTone.TEMPLE_BOWL -> {
                    val notes = if (subInterval) {
                        listOf(329.63 to 0.85)
                    } else {
                        listOf(220.00 to 0.90, 329.63 to 1.05)
                    }
                    playNotes(notes, volumeScale = if (subInterval) 0.25 else 0.34, decayRate = 2.4)
                }

                ChimeTone.CUCKOO_CHIRP -> {
                    val notes = if (subInterval) {
                        listOf(783.99 to 0.18, 659.25 to 0.28)
                    } else {
                        listOf(783.99 to 0.20, 659.25 to 0.32, 0.0 to 0.14, 783.99 to 0.20, 659.25 to 0.36)
                    }
                    playNotes(notes, volumeScale = if (subInterval) 0.24 else 0.30, decayRate = 6.2)
                }
            }
        }

        if (!subInterval && strikeHourCount in 1..12) {
            val strikeNotes = buildList {
                add(0.0 to 0.22)
                repeat(strikeHourCount) {
                    add(440.00 to 0.34)
                    add(0.0 to 0.12)
                }
            }
            playNotes(strikeNotes, volumeScale = 0.24, decayRate = 4.6)
        }
    }

    private fun playCustomFileSync(
        file: File,
        maxDurationMs: Long,
        volume: Float
    ): Boolean {
        var player: MediaPlayer? = null
        return try {
            val doneLatch = CountDownLatch(1)
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(file.absolutePath)
                setVolume(volume, volume)
                setOnCompletionListener { doneLatch.countDown() }
                setOnErrorListener { _, _, _ ->
                    doneLatch.countDown()
                    true
                }
                prepare()
            }
            synchronized(lock) {
                activeMediaPlayer = player
            }
            val waitMs = player.duration.toLong().let { d ->
                if (d > 0L) minOf(d, maxDurationMs) + 180L else maxDurationMs
            }
            player.start()
            doneLatch.await(waitMs, TimeUnit.MILLISECONDS)
            true
        } catch (_: Exception) {
            false
        } finally {
            synchronized(lock) {
                if (activeMediaPlayer === player) {
                    activeMediaPlayer = null
                }
            }
            try {
                player?.apply {
                    if (isPlaying) stop()
                    release()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun playNotes(
        notes: List<Pair<Double, Double>>,
        volumeScale: Double = 0.32,
        decayRate: Double = 3.8
    ) {
        val sampleRate = 22050
        val totalSamples = notes.sumOf { (it.second * sampleRate).toInt() }
        if (totalSamples <= 0) return

        val pcm = ShortArray(totalSamples)
        var offset = 0
        for ((freq, durationSec) in notes) {
            val count = (durationSec * sampleRate).toInt()
            for (i in 0 until count) {
                if (freq <= 0.0) {
                    pcm[offset + i] = 0
                    continue
                }
                val t = i.toDouble() / sampleRate
                val attack = (t / 0.015).coerceIn(0.0, 1.0)
                val decay = exp(-t * decayRate)
                val fundamental = sin(2.0 * PI * freq * t)
                val overtone = 0.30 * sin(2.0 * PI * (freq * 2.0) * t)
                val shimmer = 0.10 * sin(2.0 * PI * (freq * 3.0) * t)
                val sample = ((fundamental + overtone + shimmer) * attack * decay * volumeScale * Short.MAX_VALUE)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcm[offset + i] = sample.toShort()
            }
            offset += count
        }

        var track: AudioTrack? = null
        try {
            val minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferBytes = maxOf(minBuffer, pcm.size * 2)
            track = AudioTrack.Builder()
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

            synchronized(lock) {
                activeAudioTrack = track
            }
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep((notes.sumOf { it.second } * 1000).toLong() + 150L)
        } catch (_: Exception) {
            // Audio hardware unavailable or interrupted cleanly
        } finally {
            synchronized(lock) {
                if (activeAudioTrack === track) {
                    activeAudioTrack = null
                }
            }
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {
            }
        }
    }
}
