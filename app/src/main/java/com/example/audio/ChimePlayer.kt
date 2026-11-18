package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
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

    @Volatile
    private var lastPlayedDedupKeyMem: String = ""

    private const val DEDUP_PREFS_NAME = "klydo_chime_dedup_prefs"
    private const val DEDUP_KEY_PREF = "last_played_chime_key"
    private const val MAX_CUSTOM_FULL_MS = 12_000L
    private const val MAX_CUSTOM_SUB_MS = 2_800L

    fun lastPlayedDedupKey(context: Context? = null): String {
        synchronized(lock) {
            if (lastPlayedDedupKeyMem.isNotBlank()) return lastPlayedDedupKeyMem
            val persisted = context?.applicationContext
                ?.getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
                ?.getString(DEDUP_KEY_PREF, "")
                .orEmpty()
            if (persisted.isNotBlank()) {
                lastPlayedDedupKeyMem = persisted
            }
            return lastPlayedDedupKeyMem
        }
    }

    fun tryAcquireChimeSlot(context: Context? = null, dedupKey: String): Boolean {
        if (dedupKey.isBlank()) return false
        synchronized(lock) {
            val prefs = context?.applicationContext
                ?.getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
            val current = lastPlayedDedupKeyMem.ifBlank {
                prefs?.getString(DEDUP_KEY_PREF, "").orEmpty()
            }
            if (current == dedupKey) {
                lastPlayedDedupKeyMem = current
                return false
            }
            lastPlayedDedupKeyMem = dedupKey
            prefs?.edit()?.putString(DEDUP_KEY_PREF, dedupKey)?.apply()
            return true
        }
    }

    fun playHourlyChime(
        tone: ChimeTone = ChimeTone.WESTMINSTER,
        strikeHourCount: Int = 0,
        customFile: File? = null,
        context: Context? = null
    ) {
        launchExclusive {
            playChimeSync(
                tone = tone,
                subInterval = false,
                strikeHourCount = strikeHourCount,
                customFile = customFile,
                context = context
            )
        }
    }

    fun playSubIntervalChime(
        tone: ChimeTone = ChimeTone.WESTMINSTER,
        customFile: File? = null,
        context: Context? = null
    ) {
        launchExclusive {
            playChimeSync(
                tone = tone,
                subInterval = true,
                strikeHourCount = 0,
                customFile = customFile,
                context = context
            )
        }
    }

    fun playBlockStartChime(context: Context? = null) {
        launchExclusive {
            withTransientAudioFocus(context) {
                playNotes(
                    notes = listOf(523.25 to 0.25, 659.25 to 0.25, 783.99 to 0.65),
                    volumeScale = 0.60,
                    decayRate = 3.8
                )
            }
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
        customFile: File? = null,
        context: Context? = null,
        maxTotalDurationMs: Long = Long.MAX_VALUE
    ) {
        val startedAtMs = System.currentTimeMillis()
        withTransientAudioFocus(context) {
            val maxCustomLimit = if (subInterval) {
                minOf(MAX_CUSTOM_SUB_MS, maxTotalDurationMs)
            } else {
                minOf(MAX_CUSTOM_FULL_MS, maxTotalDurationMs)
            }
            val playedCustom = if (customFile != null && customFile.exists() && customFile.length() > 0L) {
                playCustomFileSync(
                    file = customFile,
                    maxDurationMs = maxCustomLimit.coerceAtLeast(1000L),
                    volume = if (subInterval) 0.72f else 1.0f
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
                        playNotes(notes, volumeScale = if (subInterval) 0.50 else 0.66, decayRate = 3.6)
                    }

                    ChimeTone.CRYSTAL_BELL -> {
                        val notes = if (subInterval) {
                            listOf(1046.50 to 0.30, 1318.51 to 0.55)
                        } else {
                            listOf(1046.50 to 0.32, 1318.51 to 0.32, 1567.98 to 0.80)
                        }
                        playNotes(notes, volumeScale = if (subInterval) 0.45 else 0.60, decayRate = 4.2)
                    }

                    ChimeTone.WARM_MARIMBA -> {
                        val notes = if (subInterval) {
                            listOf(392.00 to 0.24, 587.33 to 0.42)
                        } else {
                            listOf(392.00 to 0.22, 493.88 to 0.22, 587.33 to 0.22, 783.99 to 0.60)
                        }
                        playNotes(notes, volumeScale = if (subInterval) 0.52 else 0.68, decayRate = 5.4)
                    }

                    ChimeTone.TEMPLE_BOWL -> {
                        val notes = if (subInterval) {
                            listOf(329.63 to 0.85)
                        } else {
                            listOf(220.00 to 0.90, 329.63 to 1.05)
                        }
                        playNotes(notes, volumeScale = if (subInterval) 0.52 else 0.68, decayRate = 2.4)
                    }

                    ChimeTone.CUCKOO_CHIRP -> {
                        val notes = if (subInterval) {
                            listOf(783.99 to 0.18, 659.25 to 0.28)
                        } else {
                            listOf(783.99 to 0.20, 659.25 to 0.32, 0.0 to 0.14, 783.99 to 0.20, 659.25 to 0.36)
                        }
                        playNotes(notes, volumeScale = if (subInterval) 0.50 else 0.64, decayRate = 6.2)
                    }
                }
            }

            if (!subInterval && strikeHourCount in 1..12) {
                val elapsed = System.currentTimeMillis() - startedAtMs
                val remainingBudget = if (maxTotalDurationMs == Long.MAX_VALUE) {
                    Long.MAX_VALUE
                } else {
                    (maxTotalDurationMs - elapsed).coerceAtLeast(0L)
                }
                val maxAffordableStrikes = if (remainingBudget == Long.MAX_VALUE) {
                    strikeHourCount
                } else {
                    ((remainingBudget - 250L) / 460L).toInt().coerceIn(0, strikeHourCount)
                }
                if (maxAffordableStrikes > 0) {
                    val strikeNotes = buildList {
                        add(0.0 to 0.22)
                        repeat(maxAffordableStrikes) {
                            add(440.00 to 0.34)
                            add(0.0 to 0.12)
                        }
                    }
                    playNotes(strikeNotes, volumeScale = 0.56, decayRate = 4.6)
                }
            }
        }
    }

    private fun chimeAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

    private inline fun withTransientAudioFocus(context: Context?, block: () -> Unit) {
        val audioManager = context?.applicationContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            block()
            return
        }
        val focusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(chimeAudioAttributes())
                .setAcceptsDelayedFocusGain(false)
                .build()
                .also { req ->
                    runCatching { audioManager.requestAudioFocus(req) }
                }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
            null
        }
        try {
            block()
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { req -> runCatching { audioManager.abandonAudioFocusRequest(req) } }
            } else {
                @Suppress("DEPRECATION")
                runCatching { audioManager.abandonAudioFocus(null) }
            }
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
                setAudioAttributes(chimeAudioAttributes())
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
        volumeScale: Double = 0.62,
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
            ).coerceAtLeast(sampleRate / 4)
            val bufferBytes = maxOf(minBuffer, pcm.size * 2)
            track = AudioTrack.Builder()
                .setAudioAttributes(chimeAudioAttributes())
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

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                return
            }

            synchronized(lock) {
                activeAudioTrack = track
            }
            val written = track.write(pcm, 0, pcm.size)
            if (written <= 0) {
                return
            }
            track.play()
            Thread.sleep((notes.sumOf { it.second } * 1000).toLong() + 180L)
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
