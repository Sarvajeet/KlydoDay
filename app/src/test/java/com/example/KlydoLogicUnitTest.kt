package com.example

import com.example.data.model.DisplaySettings
import com.example.data.model.KlydoArtCatalog
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import com.example.ui.viewmodel.KlydoViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlydoLogicUnitTest {
    @Test
    fun formatMinutesOfDay_handles12hAnd24h() {
        assertEquals("12:00 AM", KlydoViewModel.formatMinutesOfDay(0, use24h = false))
        assertEquals("9:30 AM", KlydoViewModel.formatMinutesOfDay(570, use24h = false))
        assertEquals("12:15 PM", KlydoViewModel.formatMinutesOfDay(735, use24h = false))
        assertEquals("7:45 PM", KlydoViewModel.formatMinutesOfDay(1185, use24h = false))
        assertEquals("00:00", KlydoViewModel.formatMinutesOfDay(0, use24h = true))
        assertEquals("19:45", KlydoViewModel.formatMinutesOfDay(1185, use24h = true))
    }

    @Test
    fun nightWindow_handlesOvernightWrap() {
        val settings = DisplaySettings(
            nightModeEnabled = true,
            nightStartMinutes = 1320L, // 22:00
            nightEndMinutes = 420L // 07:00
        )
        assertTrue(KlydoViewModel.isMinuteInNightWindow(1350, settings)) // 22:30
        assertTrue(KlydoViewModel.isMinuteInNightWindow(120, settings)) // 02:00
        assertFalse(KlydoViewModel.isMinuteInNightWindow(420, settings)) // 07:00
        assertFalse(KlydoViewModel.isMinuteInNightWindow(900, settings)) // 15:00
    }

    @Test
    fun scheduleBlock_weekdayFiltering() {
        val daily = ScheduleBlock(title = "Daily", daysOfWeek = emptyList())
        assertTrue(daily.isActiveForDay("MON"))
        assertTrue(daily.isActiveForDay("SUN"))

        val weekdays = ScheduleBlock(title = "Work", daysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI"))
        assertTrue(weekdays.isActiveForDay("WED"))
        assertFalse(weekdays.isActiveForDay("SAT"))
    }

    @Test
    fun artCatalog_providesDistinctSyncedHandAndPendulumColors() {
        val hexRegex = Regex("^#[0-9A-Fa-f]{6}$")
        assertTrue(KlydoArtCatalog.all.size >= 33)

        val uniqueIds = KlydoArtCatalog.all.map { it.id }.toSet()
        assertEquals("Every watch face ID must be unique", KlydoArtCatalog.all.size, uniqueIds.size)

        com.example.data.model.KlydoArtCategory.entries
            .filter { it != com.example.data.model.KlydoArtCategory.CUSTOM }
            .forEach { category ->
                val count = KlydoArtCatalog.all.count { it.category == category }
                assertTrue("Category ${category.label} should have at least 4 faces (found $count)", count >= 4)
            }

        KlydoArtCatalog.all.forEach { art ->
            assertTrue("${art.id} id exceeds Firestore limit", art.id.length in 1..48)
            assertTrue("${art.id} hands hex invalid", hexRegex.matches(art.handsColorHex))
            assertTrue("${art.id} rod hex invalid", hexRegex.matches(art.pendulumRodColorHex))
            assertTrue("${art.id} bob hex invalid", hexRegex.matches(art.pendulumBobColorHex))
        }

        val uniqueHandColors = KlydoArtCatalog.all.map { it.handsColorHex }.toSet()
        val uniqueBobColors = KlydoArtCatalog.all.map { it.pendulumBobColorHex }.toSet()
        assertTrue("Hands colors should vary with selected face", uniqueHandColors.size >= 16)
        assertTrue("Pendulum bob colors should vary with selected face", uniqueBobColors.size >= 16)

        val defaultSettings = DisplaySettings()
        assertEquals(PendulumStyle.OBSIDIAN.key, defaultSettings.pendulumStyle)
        assertEquals("bioluminescent_jelly", defaultSettings.activeKlydoId)
        org.junit.Assert.assertFalse(defaultSettings.showFrame)
        org.junit.Assert.assertFalse(com.example.data.local.ClockSurfaceState().showFrame)
    }

    @Test
    fun customVideoClipWindow_clampsToFiveToTenSecondLoop() {
        val shortClip = com.example.data.local.WatchFaceLibraryStore.clampClipWindow(6500L, 0L, 6500L)
        assertEquals(0L to 6500L, shortClip)

        val longClip = com.example.data.local.WatchFaceLibraryStore.clampClipWindow(28_000L, 4_000L, 20_000L)
        assertEquals(4_000L to 14_000L, longClip)
    }

    @Test
    fun watchFaceLibraryState_mergesCustomAndExcludesDeletedPrebuilt() {
        val custom = com.example.data.model.CustomWatchFace(
            id = "custom_vid_abc123",
            title = "Ocean Wave",
            videoFileName = "custom_vid_abc123.mp4",
            thumbnailFileName = "custom_vid_abc123.jpg",
            durationMs = 8000L,
            clipStartMs = 0L,
            clipEndMs = 8000L
        )
        val state = com.example.data.model.WatchFaceLibraryState(
            customFaces = listOf(custom),
            deletedPrebuiltIds = setOf("bauhaus_orbit", "retro_grid")
        )
        val merged = state.availableArtMetas()
        assertEquals("custom_vid_abc123", merged.first().id)
        assertFalse(merged.any { it.id == "bauhaus_orbit" })
        assertFalse(merged.any { it.id == "retro_grid" })
        assertEquals(KlydoArtCatalog.all.size - 1, merged.size)
    }
}
