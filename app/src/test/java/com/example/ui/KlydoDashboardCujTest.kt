package com.example.ui

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ChimeInterval
import com.example.audio.ChimeTone
import com.example.data.local.ClockSurfaceStore
import com.example.data.local.GuestKlydoRepository
import com.example.data.local.WatchFaceLibraryStore
import com.example.data.model.DisplaySettings
import com.example.ui.theme.KlydoDayTheme
import com.example.ui.viewmodel.KlydoViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KlydoDashboardCujTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var repository: GuestKlydoRepository
    private lateinit var surfaceStore: ClockSurfaceStore
    private lateinit var libraryStore: WatchFaceLibraryStore
    private lateinit var viewModel: KlydoViewModel
    private lateinit var vmStore: ViewModelStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = GuestKlydoRepository(context)
        surfaceStore = ClockSurfaceStore(context)
        libraryStore = WatchFaceLibraryStore(context)

        runBlocking {
            repository.clearGuestBlocks()
            repository.saveDisplaySettings(DisplaySettings(id = "default", userId = "guest"))
            libraryStore.restoreAllPrebuiltFaces()
            surfaceStore.updateChimePreferences(
                chimeInterval = ChimeInterval.EVERY_1_HOUR,
                chimeTone = ChimeTone.WESTMINSTER,
                strikeHourCount = false,
                chimeInBackground = true,
                selectedCustomChimeId = ""
            )
        }

        vmStore = ViewModelStore()
        viewModel = KlydoViewModel(
            repository = repository,
            currentUserId = GuestKlydoRepository.GUEST_USER_ID,
            appContext = context,
            enableTicker = false
        )
        vmStore.put("cuj_vm", viewModel)
    }

    @After
    fun tearDown() {
        vmStore.clear()
    }

    private fun launchDashboardAndAwaitLoaded() {
        composeRule.setContent {
            KlydoDayTheme {
                KlydoDashboardScreen(
                    viewModel = viewModel,
                    isGuest = true,
                    userEmail = null,
                    animateClock = false
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            shadowOf(Looper.getMainLooper()).idle()
            viewModel.hasLoadedSettings.value && viewModel.scheduleBlocksState.value is UiState.Success
        }
        composeRule.waitForIdle()
    }

    @Test
    fun dashboard_scheduleCrud_andCompletionCuj() {
        launchDashboardAndAwaitLoaded()

        composeRule.onNodeWithTag("klydo_clock_dial").assertIsDisplayed()
        composeRule.onNodeWithTag("klydo_scrub_knob").assertIsDisplayed()

        // Switch to Schedule tab on phone layout and create a block
        composeRule.onNodeWithTag("mobile_tab_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mobile_add_block_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("block_title_input").performScrollTo().performTextInput("Evening Reading")
        composeRule.onNodeWithTag("save_block_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            shadowOf(Looper.getMainLooper()).idle()
            val list = (viewModel.scheduleBlocksState.value as? UiState.Success)?.data.orEmpty()
            list.any { it.title == "Evening Reading" }
        }
        composeRule.waitForIdle()

        val created = (viewModel.scheduleBlocksState.value as UiState.Success).data
            .first { it.title == "Evening Reading" }
        composeRule.onNodeWithTag("schedule_timeline_list")
            .performScrollToNode(hasTestTag("schedule_block_card_${created.id}"))
        composeRule.onNodeWithTag("schedule_block_card_${created.id}").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("delete_block_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            shadowOf(Looper.getMainLooper()).idle()
            val list = (viewModel.scheduleBlocksState.value as? UiState.Success)?.data.orEmpty()
            list.none { it.id == created.id }
        }
        val snapshot = runBlocking { repository.getGuestBlocksSnapshot() }
        assertTrue(snapshot.isEmpty())
    }

    @Test
    fun dashboard_ambientMode_cycleFace_andScrubSnap() {
        launchDashboardAndAwaitLoaded()

        val initialFace = viewModel.displaySettingsState.value.activeKlydoId

        composeRule.onNodeWithTag("ambient_mode_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("exit_ambient_button").assertIsDisplayed()

        composeRule.onNodeWithTag("klydo_clock_dial").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            shadowOf(Looper.getMainLooper()).idle()
            viewModel.displaySettingsState.value.activeKlydoId != initialFace
        }
        assertNotEquals(initialFace, viewModel.displaySettingsState.value.activeKlydoId)

        // Adjust scrub offset and verify snap pill resets it
        viewModel.adjustScrubMinutes(45f)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("snap_to_now_pill").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(0f, viewModel.scrubOffsetMinutes.value, 0.01f)

        composeRule.onNodeWithTag("exit_ambient_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("klydo_scrub_knob").assertIsDisplayed()
    }

    @Test
    fun dashboard_settingsTabs_clockAndChimesPersistWithoutClobbering() {
        launchDashboardAndAwaitLoaded()

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.waitForIdle()

        // Toggle Clock Cabinet Frame ON
        composeRule.onNodeWithTag("show_frame_switch").assertIsOff().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("show_frame_switch").assertIsOn()

        // Switch to Chimes tab and pick 30m interval + Crystal tone + Strike hour
        composeRule.onNodeWithTag("settings_tab_chimes").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chime_interval_30m").performScrollTo().performClick()
        composeRule.onNodeWithTag("chime_tone_crystal").performScrollTo().performClick()
        composeRule.onNodeWithTag("strike_hour_switch").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("close_settings_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            shadowOf(Looper.getMainLooper()).idle()
            val snap = runBlocking { surfaceStore.surfaceFlow.first() }
            snap.showFrame &&
                snap.chimeInterval == ChimeInterval.EVERY_30_MIN &&
                snap.chimeTone == ChimeTone.CRYSTAL_BELL &&
                snap.strikeHourCount
        }

        val finalSettings = runBlocking {
            repository.observeDisplaySettings(GuestKlydoRepository.GUEST_USER_ID).first()
        }
        val finalSurface = runBlocking { surfaceStore.surfaceFlow.first() }
        assertTrue(finalSettings.showFrame)
        assertTrue(finalSettings.hourlyChimeEnabled)
        assertTrue(finalSurface.showFrame)
        assertEquals(ChimeInterval.EVERY_30_MIN, finalSurface.chimeInterval)
        assertEquals(ChimeTone.CRYSTAL_BELL, finalSurface.chimeTone)
        assertTrue(finalSurface.strikeHourCount)
    }

    @Test
    fun dashboard_galleryDeleteInbuiltFace_andUndoRestores() {
        launchDashboardAndAwaitLoaded()

        composeRule.onNodeWithTag("gallery_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("watch_face_grid")
            .performScrollToNode(hasTestTag("gallery_face_zen_ripple"))
        composeRule.onNodeWithTag("delete_face_zen_ripple").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_delete_face_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            shadowOf(Looper.getMainLooper()).idle()
            runBlocking { libraryStore.libraryFlow.first().deletedPrebuiltIds.contains("zen_ripple") }
        }
        assertTrue(runBlocking { libraryStore.libraryFlow.first().deletedPrebuiltIds.contains("zen_ripple") })

        // Undo right inside the gallery banner
        composeRule.onNodeWithText("Undo").assertIsDisplayed().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            shadowOf(Looper.getMainLooper()).idle()
            runBlocking { !libraryStore.libraryFlow.first().deletedPrebuiltIds.contains("zen_ripple") }
        }
        assertFalse(runBlocking { libraryStore.libraryFlow.first().deletedPrebuiltIds.contains("zen_ripple") })
    }
}
