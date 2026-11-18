package com.example.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DisplaySettings
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
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
class GuestKlydoRepositoryTest {

    @Test
    fun `guest repository persists blocks and settings locally without authentication`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GuestKlydoRepository(context)
        repo.clearGuestBlocks()

        val createdId = repo.createScheduleBlock(
            ScheduleBlock(
                title = "Morning Focus",
                subtitle = "Studio desk",
                startMinutes = 540L,
                durationMinutes = 90L,
                category = ScheduleCategory.FOCUS.key
            )
        ).getOrThrow()

        assertTrue(createdId.startsWith("guest_"))

        val blocksAfterCreate = repo.observeScheduleBlocks(GuestKlydoRepository.GUEST_USER_ID).first()
        assertEquals(1, blocksAfterCreate.size)
        assertEquals("Morning Focus", blocksAfterCreate.first().title)
        assertEquals(GuestKlydoRepository.GUEST_USER_ID, blocksAfterCreate.first().userId)

        repo.toggleBlockCompleted(createdId, "2026-11-13").getOrThrow()
        val blocksAfterComplete = repo.observeScheduleBlocks(GuestKlydoRepository.GUEST_USER_ID).first()
        assertTrue(blocksAfterComplete.first().isCompletedOn("2026-11-13"))

        repo.updateScheduleBlock(
            blocksAfterComplete.first().copy(title = "Deep Focus", durationMinutes = 120L)
        ).getOrThrow()
        val blocksAfterUpdate = repo.observeScheduleBlocks(GuestKlydoRepository.GUEST_USER_ID).first()
        assertEquals("Deep Focus", blocksAfterUpdate.first().title)
        assertEquals(120L, blocksAfterUpdate.first().durationMinutes)

        val snapshot = repo.getGuestBlocksSnapshot()
        assertEquals(1, snapshot.size)

        repo.saveDisplaySettings(
            DisplaySettings(
                activeKlydoId = "celestial_tide",
                pendulumStyle = PendulumStyle.TERRACOTTA.key,
                clockFormat24h = true,
                showFrame = false
            )
        ).getOrThrow()

        val savedSettings = repo.observeDisplaySettings(GuestKlydoRepository.GUEST_USER_ID).first()
        assertEquals("celestial_tide", savedSettings.activeKlydoId)
        assertEquals(PendulumStyle.TERRACOTTA.key, savedSettings.pendulumStyle)
        assertTrue(savedSettings.clockFormat24h)
        assertFalse(savedSettings.showFrame)

        repo.deleteScheduleBlock(createdId).getOrThrow()
        val blocksAfterDelete = repo.observeScheduleBlocks(GuestKlydoRepository.GUEST_USER_ID).first()
        assertTrue(blocksAfterDelete.isEmpty())
    }

    @Test
    fun `explicitly cleared favoriteKlydoIds remains empty instead of restoring default`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GuestKlydoRepository(context)

        repo.saveDisplaySettings(
            DisplaySettings(favoriteKlydoIds = emptyList())
        ).getOrThrow()

        val loaded = repo.observeDisplaySettings(GuestKlydoRepository.GUEST_USER_ID).first()
        assertTrue(loaded.favoriteKlydoIds.isEmpty())
    }
}
