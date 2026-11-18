package com.example.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.ui.theme.KlydoDayTheme
import org.junit.Assert.assertEquals
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
            .assertTextEquals("Developed by Sarvajeet Gada")

        composeRule.onNodeWithTag("load_screen_skip_backdrop").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun `theme configures white starting window and transparent system splash icon`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val white = context.getColor(R.color.white)
        val attrs = context.obtainStyledAttributes(
            R.style.Theme_MyApplication,
            intArrayOf(
                android.R.attr.windowBackground,
                android.R.attr.windowSplashScreenBackground,
                android.R.attr.windowSplashScreenAnimatedIcon
            )
        )
        try {
            assertEquals(white, attrs.getColor(0, 0))
            assertEquals(white, attrs.getColor(1, 0))
            assertEquals(R.drawable.splash_transparent_icon, attrs.getResourceId(2, 0))
        } finally {
            attrs.recycle()
        }
    }
}
