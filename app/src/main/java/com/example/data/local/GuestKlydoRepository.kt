package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.KlydoDataSource
import com.example.data.model.DisplaySettings
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.guestDataStore: DataStore<Preferences> by preferencesDataStore(name = "klydo_guest_store")

class GuestKlydoRepository(private val context: Context) : KlydoDataSource {

    private val blocksKey = stringPreferencesKey("guest_blocks_json")
    private val settingsKey = stringPreferencesKey("guest_settings_json")

    override fun observeScheduleBlocks(userId: String): Flow<List<ScheduleBlock>> =
        context.guestDataStore.data.map { prefs ->
            decodeBlocks(prefs[blocksKey]).sortedBy { it.startMinutes }
        }

    override suspend fun createScheduleBlock(block: ScheduleBlock): Result<String> {
        return try {
            val newId = "guest_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val sanitized = block.copy(
                id = newId,
                userId = GUEST_USER_ID,
                title = block.title.trim().take(80),
                subtitle = block.subtitle.trim().take(120),
                startMinutes = block.startMinutes.coerceIn(0L, 1439L),
                durationMinutes = block.durationMinutes.coerceIn(5L, 720L),
                category = ScheduleCategory.fromKey(block.category).key,
                daysOfWeek = block.daysOfWeek.take(7),
                completedDateKey = block.completedDateKey.take(16)
            )
            context.guestDataStore.edit { prefs ->
                val list = decodeBlocks(prefs[blocksKey]).toMutableList()
                list.add(sanitized)
                prefs[blocksKey] = encodeBlocks(list)
            }
            Result.success(newId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateScheduleBlock(block: ScheduleBlock): Result<Unit> {
        return try {
            require(block.id.isNotBlank()) { "ScheduleBlock ID must not be blank" }
            context.guestDataStore.edit { prefs ->
                val list = decodeBlocks(prefs[blocksKey]).map { existing ->
                    if (existing.id == block.id) {
                        block.copy(
                            userId = GUEST_USER_ID,
                            title = block.title.trim().take(80),
                            subtitle = block.subtitle.trim().take(120),
                            startMinutes = block.startMinutes.coerceIn(0L, 1439L),
                            durationMinutes = block.durationMinutes.coerceIn(5L, 720L),
                            category = ScheduleCategory.fromKey(block.category).key,
                            daysOfWeek = block.daysOfWeek.take(7),
                            completedDateKey = block.completedDateKey.take(16)
                        )
                    } else {
                        existing
                    }
                }
                prefs[blocksKey] = encodeBlocks(list)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleBlockCompleted(blockId: String, completedDateKey: String): Result<Unit> {
        return try {
            context.guestDataStore.edit { prefs ->
                val list = decodeBlocks(prefs[blocksKey]).map { existing ->
                    if (existing.id == blockId) {
                        existing.copy(completedDateKey = completedDateKey.take(16))
                    } else {
                        existing
                    }
                }
                prefs[blocksKey] = encodeBlocks(list)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteScheduleBlock(blockId: String): Result<Unit> {
        return try {
            context.guestDataStore.edit { prefs ->
                val list = decodeBlocks(prefs[blocksKey]).filterNot { it.id == blockId }
                prefs[blocksKey] = encodeBlocks(list)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeDisplaySettings(userId: String): Flow<DisplaySettings> =
        context.guestDataStore.data.map { prefs ->
            decodeSettings(prefs[settingsKey])
        }

    override suspend fun saveDisplaySettings(settings: DisplaySettings): Result<Unit> {
        return try {
            val sanitized = settings.copy(
                id = "default",
                userId = GUEST_USER_ID,
                activeKlydoId = settings.activeKlydoId.take(48),
                cycleIntervalMinutes = settings.cycleIntervalMinutes.coerceIn(15L, 1440L),
                pendulumStyle = PendulumStyle.fromKey(settings.pendulumStyle).key,
                nightStartMinutes = settings.nightStartMinutes.coerceIn(0L, 1439L),
                nightEndMinutes = settings.nightEndMinutes.coerceIn(0L, 1439L),
                favoriteKlydoIds = settings.favoriteKlydoIds.take(20)
            )
            context.guestDataStore.edit { prefs ->
                prefs[settingsKey] = encodeSettings(sanitized)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGuestBlocksSnapshot(): List<ScheduleBlock> {
        val prefs = context.guestDataStore.data.first()
        return decodeBlocks(prefs[blocksKey])
    }

    suspend fun clearGuestBlocks() {
        context.guestDataStore.edit { prefs ->
            prefs.remove(blocksKey)
        }
    }

    private fun decodeBlocks(raw: String?): List<ScheduleBlock> {
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
                            userId = GUEST_USER_ID,
                            title = obj.optString("title"),
                            subtitle = obj.optString("subtitle"),
                            startMinutes = obj.optLong("startMinutes", 540L),
                            durationMinutes = obj.optLong("durationMinutes", 60L),
                            category = obj.optString("category", ScheduleCategory.FOCUS.key),
                            colorHex = obj.optString("colorHex", ScheduleCategory.FOCUS.defaultColorHex),
                            daysOfWeek = days,
                            chimeEnabled = obj.optBoolean("chimeEnabled", true),
                            completedDateKey = obj.optString("completedDateKey", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeBlocks(blocks: List<ScheduleBlock>): String {
        val arr = JSONArray()
        for (b in blocks) {
            arr.put(
                JSONObject().apply {
                    put("id", b.id)
                    put("title", b.title)
                    put("subtitle", b.subtitle)
                    put("startMinutes", b.startMinutes)
                    put("durationMinutes", b.durationMinutes)
                    put("category", b.category)
                    put("colorHex", b.colorHex)
                    put("daysOfWeek", JSONArray(b.daysOfWeek))
                    put("chimeEnabled", b.chimeEnabled)
                    put("completedDateKey", b.completedDateKey)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeSettings(raw: String?): DisplaySettings {
        if (raw.isNullOrBlank()) return DisplaySettings(id = "default", userId = GUEST_USER_ID)
        return try {
            val obj = JSONObject(raw)
            val favArr = obj.optJSONArray("favoriteKlydoIds")
            val favs = buildList {
                if (favArr != null) {
                    for (i in 0 until favArr.length()) {
                        val id = favArr.optString(i)
                        if (id.isNotBlank()) add(id)
                    }
                }
            }.ifEmpty { listOf("bioluminescent_jelly") }
            DisplaySettings(
                id = "default",
                userId = GUEST_USER_ID,
                activeKlydoId = obj.optString("activeKlydoId", "bioluminescent_jelly"),
                autoCycleArt = obj.optBoolean("autoCycleArt", false),
                cycleIntervalMinutes = obj.optLong("cycleIntervalMinutes", 60L),
                pendulumStyle = obj.optString("pendulumStyle", PendulumStyle.OBSIDIAN.key),
                pendulumEnabled = obj.optBoolean("pendulumEnabled", true),
                hourlyChimeEnabled = obj.optBoolean("hourlyChimeEnabled", true),
                blockChimeEnabled = obj.optBoolean("blockChimeEnabled", true),
                keepScreenOn = obj.optBoolean("keepScreenOn", true),
                nightModeEnabled = obj.optBoolean("nightModeEnabled", false),
                nightStartMinutes = obj.optLong("nightStartMinutes", 1320L),
                nightEndMinutes = obj.optLong("nightEndMinutes", 420L),
                clockFormat24h = obj.optBoolean("clockFormat24h", false),
                showFrame = obj.optBoolean("showFrame", true),
                favoriteKlydoIds = favs
            )
        } catch (_: Exception) {
            DisplaySettings(id = "default", userId = GUEST_USER_ID)
        }
    }

    private fun encodeSettings(s: DisplaySettings): String {
        return JSONObject().apply {
            put("activeKlydoId", s.activeKlydoId)
            put("autoCycleArt", s.autoCycleArt)
            put("cycleIntervalMinutes", s.cycleIntervalMinutes)
            put("pendulumStyle", s.pendulumStyle)
            put("pendulumEnabled", s.pendulumEnabled)
            put("hourlyChimeEnabled", s.hourlyChimeEnabled)
            put("blockChimeEnabled", s.blockChimeEnabled)
            put("keepScreenOn", s.keepScreenOn)
            put("nightModeEnabled", s.nightModeEnabled)
            put("nightStartMinutes", s.nightStartMinutes)
            put("nightEndMinutes", s.nightEndMinutes)
            put("clockFormat24h", s.clockFormat24h)
            put("showFrame", s.showFrame)
            put("favoriteKlydoIds", JSONArray(s.favoriteKlydoIds))
        }.toString()
    }

    companion object {
        const val GUEST_USER_ID = "guest"
    }
}
