package com.example.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DisplaySettings
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class ClockSurfaceStoreTest {

    @Test
    fun `store mirrors app settings and blocks and preserves surface preferences`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = ClockSurfaceStore(context)

        val settings = DisplaySettings(
            activeKlydoId = "celestial_observatory",
            pendulumStyle = PendulumStyle.BRASS.key,
            pendulumEnabled = true,
            nightModeEnabled = true,
            clockFormat24h = true,
            showFrame = false
        )
        val blocks = listOf(
            ScheduleBlock(
                id = "b1",
                title = "Deep Focus",
                startMinutes = 540L,
                durationMinutes = 90L,
                category = "focus",
                colorHex = "#E06B3E",
                daysOfWeek = listOf("MON", "TUE")
            )
        )

        store.syncFromApp(settings, blocks)
        store.updateSurfacePreferences(
            wallpaperShowFrame = true,
            dreamOledShift = false,
            dreamShowNextRoutine = false,
            wallpaperBatterySaverFps = false
        )

        val loaded = store.surfaceFlow.first()
        assertEquals("celestial_observatory", loaded.activeKlydoId)
        assertEquals(PendulumStyle.BRASS.key, loaded.pendulumStyle)
        assertTrue(loaded.nightModeEnabled)
        assertTrue(loaded.clockFormat24h)
        assertFalse(loaded.showFrame)
        assertEquals(1, loaded.scheduleBlocks.size)
        assertEquals("Deep Focus", loaded.scheduleBlocks.first().title)

        assertTrue(loaded.wallpaperShowFrame)
        assertFalse(loaded.dreamOledShift)
        assertFalse(loaded.dreamShowNextRoutine)
        assertFalse(loaded.wallpaperBatterySaverFps)

        store.updateActiveKlydoId("zen_ripple")
        store.updateChimePreferences(
            chimeInterval = com.example.audio.ChimeInterval.EVERY_30_MIN,
            chimeTone = com.example.audio.ChimeTone.CRYSTAL_BELL,
            strikeHourCount = true,
            chimeInBackground = true
        )

        val updated = store.surfaceFlow.first()
        assertEquals("zen_ripple", updated.activeKlydoId)
        assertEquals(com.example.audio.ChimeInterval.EVERY_30_MIN, updated.chimeInterval)
        assertEquals(com.example.audio.ChimeTone.CRYSTAL_BELL, updated.chimeTone)
        assertTrue(updated.strikeHourCount)
        assertTrue(updated.chimeInBackground)
    }

    @Test
    fun `custom chimes json round trip and title sanitization`() {
        assertEquals("Grandfather Ding", ClockSurfaceStore.sanitizeChimeTitle("  Grandfather_Ding-  "))
        assertEquals("Custom Chime", ClockSurfaceStore.sanitizeChimeTitle("   "))

        val items = listOf(
            com.example.audio.CustomChimeSound(
                id = "custom_chime_1",
                title = "Brass Bell",
                audioFileName = "custom_chime_1.mp3",
                durationMs = 3400L,
                createdAtMillis = 123456L
            ),
            com.example.audio.CustomChimeSound(
                id = "custom_chime_2",
                title = "LoFi Ping",
                audioFileName = "custom_chime_2.wav",
                durationMs = 1500L,
                createdAtMillis = 654321L
            )
        )

        val encoded = ClockSurfaceStore.encodeCustomChimes(items)
        val decoded = ClockSurfaceStore.decodeCustomChimes(encoded)
        assertEquals(items, decoded)
        assertTrue(ClockSurfaceStore.decodeCustomChimes("not-json").isEmpty())
    }

    @Test
    fun `activeChimeLabel falls back to built-in tone when selected custom id missing`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = ClockSurfaceStore(context)

        store.updateChimePreferences(
            chimeTone = com.example.audio.ChimeTone.TEMPLE_BOWL,
            selectedCustomChimeId = "non_existent_chime"
        )

        val state = store.surfaceFlow.first()
        assertEquals("", state.selectedCustomChimeId)
        assertEquals("Temple Bowl", state.activeChimeLabel)
    }
}
