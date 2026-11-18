package com.example.data.local

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.audio.ChimeInterval
import com.example.audio.ChimeTone
import com.example.audio.CustomChimeSound
import com.example.data.model.DisplaySettings
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private val Context.surfaceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "klydo_surface_store"
)

data class ClockSurfaceState(
    val activeKlydoId: String = "bioluminescent_jelly",
    val autoCycleArt: Boolean = false,
    val cycleIntervalMinutes: Long = 60L,
    val pendulumStyle: String = PendulumStyle.OBSIDIAN.key,
    val pendulumEnabled: Boolean = true,
    val hourlyChimeEnabled: Boolean = true,
    val nightModeEnabled: Boolean = false,
    val nightStartMinutes: Long = 1320L,
    val nightEndMinutes: Long = 420L,
    val clockFormat24h: Boolean = false,
    val showFrame: Boolean = false,
    val scheduleBlocks: List<ScheduleBlock> = emptyList(),
    // Surface-specific refinements
    val wallpaperShowFrame: Boolean = false,
    val dreamOledShift: Boolean = true,
    val dreamShowNextRoutine: Boolean = true,
    val wallpaperBatterySaverFps: Boolean = true,
    // Interval chime refinements
    val chimeIntervalKey: String = ChimeInterval.EVERY_1_HOUR.key,
    val chimeToneKey: String = ChimeTone.WESTMINSTER.key,
    val strikeHourCount: Boolean = false,
    val chimeInBackground: Boolean = true,
    val selectedCustomChimeId: String = "",
    val customChimes: List<CustomChimeSound> = emptyList()
) {
    val chimeInterval: ChimeInterval
        get() = ChimeInterval.fromKey(chimeIntervalKey)

    val chimeTone: ChimeTone
        get() = ChimeTone.fromKey(chimeToneKey)

    val activeCustomChime: CustomChimeSound?
        get() = selectedCustomChimeId.takeIf { it.isNotBlank() }
            ?.let { id -> customChimes.firstOrNull { it.id == id } }

    val activeChimeLabel: String
        get() = activeCustomChime?.title ?: chimeTone.label

    fun toDisplaySettingsSnapshot(): DisplaySettings = DisplaySettings(
        id = "surface",
        activeKlydoId = activeKlydoId,
        autoCycleArt = autoCycleArt,
        cycleIntervalMinutes = cycleIntervalMinutes,
        pendulumStyle = pendulumStyle,
        pendulumEnabled = pendulumEnabled,
        hourlyChimeEnabled = hourlyChimeEnabled,
        nightModeEnabled = nightModeEnabled,
        nightStartMinutes = nightStartMinutes,
        nightEndMinutes = nightEndMinutes,
        clockFormat24h = clockFormat24h,
        showFrame = showFrame
    )
}

class ClockSurfaceStore(private val context: Context) {

    private val snapshotJsonKey = stringPreferencesKey("surface_snapshot_json")
    private val blocksJsonKey = stringPreferencesKey("surface_blocks_json")
    private val wallpaperShowFrameKey = booleanPreferencesKey("wallpaper_show_frame")
    private val dreamOledShiftKey = booleanPreferencesKey("dream_oled_shift")
    private val dreamShowNextRoutineKey = booleanPreferencesKey("dream_show_next_routine")
    private val wallpaperBatterySaverFpsKey = booleanPreferencesKey("wallpaper_battery_saver_fps")
    private val chimeIntervalKeyPref = stringPreferencesKey("chime_interval_key")
    private val chimeToneKeyPref = stringPreferencesKey("chime_tone_key")
    private val strikeHourCountKey = booleanPreferencesKey("strike_hour_count")
    private val chimeInBackgroundKey = booleanPreferencesKey("chime_in_background")
    private val selectedCustomChimeIdKey = stringPreferencesKey("selected_custom_chime_id")
    private val customChimesJsonKey = stringPreferencesKey("custom_chimes_json")

    private val chimeDir: File
        get() = File(context.filesDir, CHIME_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

    fun resolveChimeFile(fileName: String): File = File(chimeDir, fileName)

    fun resolveActiveCustomChimeFile(state: ClockSurfaceState): File? =
        state.activeCustomChime?.let { custom ->
            resolveChimeFile(custom.audioFileName).takeIf { it.exists() && it.length() > 0L }
        }

    val surfaceFlow: Flow<ClockSurfaceState> =
        context.surfaceDataStore.data.map { prefs ->
            val base = decodeSnapshot(prefs[snapshotJsonKey])
            val blocks = decodeBlocks(prefs[blocksJsonKey])
            val customList = decodeCustomChimes(prefs[customChimesJsonKey])
            val selectedCustom = prefs[selectedCustomChimeIdKey].orEmpty()
                .takeIf { id -> customList.any { it.id == id } }
                .orEmpty()
            base.copy(
                scheduleBlocks = blocks,
                wallpaperShowFrame = prefs[wallpaperShowFrameKey] ?: false,
                dreamOledShift = prefs[dreamOledShiftKey] ?: true,
                dreamShowNextRoutine = prefs[dreamShowNextRoutineKey] ?: true,
                wallpaperBatterySaverFps = prefs[wallpaperBatterySaverFpsKey] ?: true,
                chimeIntervalKey = prefs[chimeIntervalKeyPref] ?: ChimeInterval.EVERY_1_HOUR.key,
                chimeToneKey = prefs[chimeToneKeyPref] ?: ChimeTone.WESTMINSTER.key,
                strikeHourCount = prefs[strikeHourCountKey] ?: false,
                chimeInBackground = prefs[chimeInBackgroundKey] ?: true,
                selectedCustomChimeId = selectedCustom,
                customChimes = customList
            )
        }

    suspend fun syncFromApp(settings: DisplaySettings, blocks: List<ScheduleBlock>) {
        val encodedSnapshot = encodeSnapshot(settings)
        val encodedBlocks = encodeBlocks(blocks)
        context.surfaceDataStore.edit { prefs ->
            if (prefs[snapshotJsonKey] != encodedSnapshot) {
                prefs[snapshotJsonKey] = encodedSnapshot
            }
            if (prefs[blocksJsonKey] != encodedBlocks) {
                prefs[blocksJsonKey] = encodedBlocks
            }
        }
    }

    suspend fun updateActiveKlydoId(klydoId: String) {
        context.surfaceDataStore.edit { prefs ->
            val current = decodeSnapshot(prefs[snapshotJsonKey]).toDisplaySettingsSnapshot()
            prefs[snapshotJsonKey] = encodeSnapshot(current.copy(activeKlydoId = klydoId))
        }
    }

    suspend fun updateSurfacePreferences(
        wallpaperShowFrame: Boolean? = null,
        dreamOledShift: Boolean? = null,
        dreamShowNextRoutine: Boolean? = null,
        wallpaperBatterySaverFps: Boolean? = null
    ) {
        context.surfaceDataStore.edit { prefs ->
            if (wallpaperShowFrame != null) prefs[wallpaperShowFrameKey] = wallpaperShowFrame
            if (dreamOledShift != null) prefs[dreamOledShiftKey] = dreamOledShift
            if (dreamShowNextRoutine != null) prefs[dreamShowNextRoutineKey] = dreamShowNextRoutine
            if (wallpaperBatterySaverFps != null) prefs[wallpaperBatterySaverFpsKey] = wallpaperBatterySaverFps
        }
    }

    suspend fun updateChimePreferences(
        chimeInterval: ChimeInterval? = null,
        chimeTone: ChimeTone? = null,
        strikeHourCount: Boolean? = null,
        chimeInBackground: Boolean? = null,
        selectedCustomChimeId: String? = null
    ) {
        context.surfaceDataStore.edit { prefs ->
            if (chimeInterval != null) prefs[chimeIntervalKeyPref] = chimeInterval.key
            if (chimeTone != null) prefs[chimeToneKeyPref] = chimeTone.key
            if (strikeHourCount != null) prefs[strikeHourCountKey] = strikeHourCount
            if (chimeInBackground != null) prefs[chimeInBackgroundKey] = chimeInBackground
            if (selectedCustomChimeId != null) prefs[selectedCustomChimeIdKey] = selectedCustomChimeId
        }
    }

    suspend fun importCustomChime(
        uri: Uri,
        overrideTitle: String? = null
    ): Result<CustomChimeSound> = withContext(Dispatchers.IO) {
        val rawDisplayName = queryDisplayName(uri)
        val ext = rawDisplayName?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it in SUPPORTED_AUDIO_EXTENSIONS }
            ?: "mp3"
        val suggestedTitle = sanitizeChimeTitle(
            overrideTitle ?: rawDisplayName?.substringBeforeLast('.') ?: "Custom Chime"
        )
        val chimeId = "$CUSTOM_CHIME_PREFIX${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val fileName = "$chimeId.$ext"
        val destFile = File(chimeDir, fileName)

        try {
            val copiedBytes = context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: 0L

            if (copiedBytes <= 0L || !destFile.exists()) {
                destFile.delete()
                return@withContext Result.failure(
                    IllegalStateException("Could not read selected audio file.")
                )
            }

            val retriever = MediaMetadataRetriever()
            val durationMs: Long
            val hasAudio: Boolean
            try {
                retriever.setDataSource(destFile.absolutePath)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val hasAudioFlag = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                hasAudio = hasAudioFlag == null || hasAudioFlag.equals("yes", ignoreCase = true)
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }

            if (!hasAudio || durationMs < MIN_CHIME_DURATION_MS) {
                destFile.delete()
                return@withContext Result.failure(
                    IllegalArgumentException("Please select a valid audio clip (MP3, WAV, M4A, or OGG).")
                )
            }

            val customChime = CustomChimeSound(
                id = chimeId,
                title = suggestedTitle,
                audioFileName = fileName,
                durationMs = durationMs
            )

            context.surfaceDataStore.edit { prefs ->
                val list = decodeCustomChimes(prefs[customChimesJsonKey]).toMutableList()
                list.add(0, customChime)
                prefs[customChimesJsonKey] = encodeCustomChimes(list)
                prefs[selectedCustomChimeIdKey] = customChime.id
            }

            Result.success(customChime)
        } catch (e: Exception) {
            destFile.delete()
            Result.failure(
                IllegalStateException("Unable to import custom chime sound. Try MP3, WAV, or M4A.", e)
            )
        }
    }

    suspend fun deleteCustomChime(chimeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            var removed: CustomChimeSound? = null
            context.surfaceDataStore.edit { prefs ->
                val list = decodeCustomChimes(prefs[customChimesJsonKey])
                removed = list.firstOrNull { it.id == chimeId }
                val updated = list.filterNot { it.id == chimeId }
                prefs[customChimesJsonKey] = encodeCustomChimes(updated)
                if (prefs[selectedCustomChimeIdKey] == chimeId) {
                    prefs[selectedCustomChimeIdKey] = ""
                }
            }
            removed?.let { chime ->
                File(chimeDir, chime.audioFileName).delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    companion object {
        const val CHIME_DIR_NAME = "custom_chimes"
        const val CUSTOM_CHIME_PREFIX = "custom_chime_"
        private const val MIN_CHIME_DURATION_MS = 120L
        private val SUPPORTED_AUDIO_EXTENSIONS = setOf("mp3", "wav", "m4a", "ogg", "aac", "flac")

        fun sanitizeChimeTitle(raw: String): String {
            val cleaned = raw.replace('_', ' ').replace('-', ' ').replace(Regex("\\s+"), " ").trim()
            return cleaned.ifBlank { "Custom Chime" }.take(28).trimEnd()
        }

        fun encodeCustomChimes(chimes: List<CustomChimeSound>): String {
            val arr = JSONArray()
            chimes.forEach { chime ->
                arr.put(
                    JSONObject().apply {
                        put("id", chime.id)
                        put("title", chime.title)
                        put("audioFileName", chime.audioFileName)
                        put("durationMs", chime.durationMs)
                        put("createdAtMillis", chime.createdAtMillis)
                    }
                )
            }
            return arr.toString()
        }

        fun decodeCustomChimes(raw: String?): List<CustomChimeSound> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("id")
                        val fileName = obj.optString("audioFileName")
                        if (id.isBlank() || fileName.isBlank()) continue
                        add(
                            CustomChimeSound(
                                id = id,
                                title = obj.optString("title", "Custom Chime"),
                                audioFileName = fileName,
                                durationMs = obj.optLong("durationMs", 2000L),
                                createdAtMillis = obj.optLong("createdAtMillis", 0L)
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
        fun encodeSnapshot(s: DisplaySettings): String = JSONObject().apply {
            put("activeKlydoId", s.activeKlydoId)
            put("autoCycleArt", s.autoCycleArt)
            put("cycleIntervalMinutes", s.cycleIntervalMinutes)
            put("pendulumStyle", PendulumStyle.fromKey(s.pendulumStyle).key)
            put("pendulumEnabled", s.pendulumEnabled)
            put("hourlyChimeEnabled", s.hourlyChimeEnabled)
            put("nightModeEnabled", s.nightModeEnabled)
            put("nightStartMinutes", s.nightStartMinutes)
            put("nightEndMinutes", s.nightEndMinutes)
            put("clockFormat24h", s.clockFormat24h)
            put("showFrame", s.showFrame)
        }.toString()

        fun decodeSnapshot(raw: String?): ClockSurfaceState {
            if (raw.isNullOrBlank()) return ClockSurfaceState()
            return try {
                val obj = JSONObject(raw)
                ClockSurfaceState(
                    activeKlydoId = obj.optString("activeKlydoId", "bioluminescent_jelly"),
                    autoCycleArt = obj.optBoolean("autoCycleArt", false),
                    cycleIntervalMinutes = obj.optLong("cycleIntervalMinutes", 60L).coerceIn(15L, 1440L),
                    pendulumStyle = obj.optString("pendulumStyle", PendulumStyle.OBSIDIAN.key),
                    pendulumEnabled = obj.optBoolean("pendulumEnabled", true),
                    hourlyChimeEnabled = obj.optBoolean("hourlyChimeEnabled", true),
                    nightModeEnabled = obj.optBoolean("nightModeEnabled", false),
                    nightStartMinutes = obj.optLong("nightStartMinutes", 1320L).coerceIn(0L, 1439L),
                    nightEndMinutes = obj.optLong("nightEndMinutes", 420L).coerceIn(0L, 1439L),
                    clockFormat24h = obj.optBoolean("clockFormat24h", false),
                    showFrame = obj.optBoolean("showFrame", false)
                )
            } catch (_: Exception) {
                ClockSurfaceState()
            }
        }

        fun encodeBlocks(blocks: List<ScheduleBlock>): String {
            val arr = JSONArray()
            for (b in blocks) {
                arr.put(
                    JSONObject().apply {
                        put("id", b.id)
                        put("title", b.title)
                        put("startMinutes", b.startMinutes)
                        put("durationMinutes", b.durationMinutes)
                        put("category", b.category)
                        put("colorHex", b.colorHex)
                        put("daysOfWeek", JSONArray(b.daysOfWeek))
                    }
                )
            }
            return arr.toString()
        }

        fun decodeBlocks(raw: String?): List<ScheduleBlock> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val daysArr = obj.optJSONArray("daysOfWeek")
                        val days = buildList {
                            if (daysArr != null) {
                                for (d in 0 until daysArr.length()) {
                                    val code = daysArr.optString(d)
                                    if (code.isNotBlank()) add(code)
                                }
                            }
                        }
                        add(
                            ScheduleBlock(
                                id = obj.optString("id"),
                                title = obj.optString("title"),
                                startMinutes = obj.optLong("startMinutes", 540L),
                                durationMinutes = obj.optLong("durationMinutes", 60L),
                                category = obj.optString("category", ScheduleCategory.FOCUS.key),
                                colorHex = obj.optString("colorHex", ScheduleCategory.FOCUS.defaultColorHex),
                                daysOfWeek = days
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
