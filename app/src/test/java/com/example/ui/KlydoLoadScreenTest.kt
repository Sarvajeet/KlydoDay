package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.theme.KlydoDayTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KlydoLoadScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `load page shows app name and developer credit`() {
        var dismissed = false

        composeRule.setContent {
            KlydoDayTheme {
                KlydoLoadScreen(
                    onFinished = { dismissed = true },
                    displayDurationMs = 5_000L,
                    animateMotion = false
                )
            }
        }

        composeRule.onNodeWithTag("app_load_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("load_screen_app_name").assertIsDisplayed()
            .assertTextEquals("KlydoDay")
        composeRule.onNodeWithTag("load_screen_developer_credit").assertIsDisplayed()
            .assertTextEquals("developed by Sarvajeet Gada")

        composeRule.onNodeWithTag("load_screen_skip_backdrop").performClick()
        assertTrue(dismissed)
    }
}
