package com.example.ui.scheduler

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.local.ClockSurfaceState
import com.example.data.model.DisplaySettings
import com.example.ui.theme.KlydoDayTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DisplaySettingsDialogTabTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `switching settings sub tabs reveals each category cleanly`() {
        composeRule.setContent {
            KlydoDayTheme {
                DisplaySettingsDialog(
                    settings = DisplaySettings(),
                    surfaceState = ClockSurfaceState(),
                    isGuest = true,
                    userEmail = null,
                    onUpdateSettings = {},
                    onSignInSuccess = {},
                    onSignOut = {},
                    onDismiss = {}
                )
            }
        }

        // Clock tab is visible initially
        composeRule.onNodeWithTag("show_frame_switch").assertIsDisplayed()
        composeRule.onNodeWithTag("test_chime_button").assertDoesNotExist()

        // Switch to Chimes tab
        composeRule.onNodeWithTag("settings_tab_chimes").performClick()
        composeRule.onNodeWithTag("add_custom_chime_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("test_chime_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("show_frame_switch").assertDoesNotExist()

        // Switch to Surfaces tab
        composeRule.onNodeWithTag("settings_tab_surfaces").performClick()
        composeRule.onNodeWithTag("activate_live_wallpaper_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("activate_screen_saver_button").performScrollTo().assertIsDisplayed()

        // Switch to Sync tab
        composeRule.onNodeWithTag("settings_tab_sync").performClick()
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
    }
}
