package com.example.data.firestore

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.DisplaySettings
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class KlydoRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveScheduleBlock_owner_succeedsAndStreamsUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = KlydoRepository(firestore, auth)

        val block = ScheduleBlock(
            title = "Morning Studio Focus",
            subtitle = "Architecture sketching",
            startMinutes = 510L,
            durationMinutes = 90L,
            category = ScheduleCategory.FOCUS.key,
            colorHex = "#E06B3E",
            daysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI"),
            chimeEnabled = true,
            completedDateKey = ""
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createScheduleBlock(block) }
        assertTrue("Expected createScheduleBlock to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val createdId = createResult.getOrThrow()
        assertTrue(createdId.isNotBlank())

        val blocks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeScheduleBlocks(uid).first { list -> list.any { it.id == createdId } }
        }
        val fetched = blocks.first { it.id == createdId }
        assertEquals("Morning Studio Focus", fetched.title)
        assertEquals(uid, fetched.userId)

        val toggleResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleBlockCompleted(createdId, "2026-11-13")
        }
        assertTrue(toggleResult.isSuccess)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteScheduleBlock(createdId)
        }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun saveAndObserveDisplaySettings_owner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = KlydoRepository(firestore, auth)

        val settings = DisplaySettings(
            activeKlydoId = "celestial_observatory",
            autoCycleArt = true,
            cycleIntervalMinutes = 60L,
            pendulumStyle = PendulumStyle.TERRACOTTA.key,
            pendulumEnabled = true,
            hourlyChimeEnabled = true,
            blockChimeEnabled = true,
            keepScreenOn = true,
            nightModeEnabled = true,
            nightStartMinutes = 1320L,
            nightEndMinutes = 420L,
            clockFormat24h = true,
            showFrame = false,
            favoriteKlydoIds = listOf("celestial_observatory", "bauhaus_orbit")
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveDisplaySettings(settings) }
        assertTrue("Expected saveDisplaySettings to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDisplaySettings(uid).first { it.activeKlydoId == "celestial_observatory" }
        }
        assertEquals(PendulumStyle.TERRACOTTA.key, observed.pendulumStyle)
        assertTrue(observed.clockFormat24h)
        assertFalse(observed.showFrame)
    }

    @Test
    fun getScheduleBlock_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = KlydoRepository(firestore, auth)
        val blockId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createScheduleBlock(
                ScheduleBlock(
                    title = "Private Routine",
                    startMinutes = 480L,
                    durationMinutes = 30L
                )
            ).getOrThrow()
        }

        val aliceUid = checkNotNull(auth.currentUser?.uid)
        signInTestUser(BOB_EMAIL)

        val bobBlocks = withTimeout(FLOW_TIMEOUT_MS) {
            try {
                KlydoRepository(firestore, auth).observeScheduleBlocks(aliceUid).first()
                fail("Expected PERMISSION_DENIED for cross-user observeScheduleBlocks")
            } catch (e: Exception) {
                val fsException = e.unwrapFirestoreException()
                assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsException)
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException!!.code)
            }
        }
        assertNotNull(bobBlocks)
        assertTrue(blockId.isNotBlank())
    }

    @Test
    fun observeScheduleBlocks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = KlydoRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeScheduleBlocks(uid).first()
            }
            fail("Expected PERMISSION_DENIED when signed out")
        } catch (e: Exception) {
            val fsException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException!!.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@klydoday.test"
        const val BOB_EMAIL = "bob@klydoday.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
