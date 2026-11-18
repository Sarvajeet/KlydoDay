package com.example.ui.scheduler

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.data.model.ScheduleBlock
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
class BlockEditorSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `validates blank title and saves trimmed block`() {
        var savedBlock: ScheduleBlock? = null

        composeRule.setContent {
            KlydoDayTheme {
                BlockEditorDialog(
                    initialBlock = null,
                    clockFormat24h = false,
                    onDismiss = {},
                    onSave = { savedBlock = it },
                    onDelete = null
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("save_block_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Title is required").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("block_title_input").performScrollTo().performTextInput("Studio Planning")
        composeRule.onNodeWithTag("save_block_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("Studio Planning", savedBlock?.title)
        assertTrue(savedBlock?.daysOfWeek?.isEmpty() == true)
    }

    @Test
    fun `deselecting the last selected weekday resets to Every Day`() {
        var savedBlock: ScheduleBlock? = null
        val singleDayBlock = ScheduleBlock(
            id = "b1",
            title = "Monday Review",
            startMinutes = 540L,
            durationMinutes = 60L,
            daysOfWeek = listOf("MON")
        )

        composeRule.setContent {
            KlydoDayTheme {
                BlockEditorDialog(
                    initialBlock = singleDayBlock,
                    clockFormat24h = false,
                    onDismiss = {},
                    onSave = { savedBlock = it },
                    onDelete = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Repeat: Selected Days").performScrollTo().assertIsDisplayed()

        // Deselect the only active day ("MO")
        composeRule.onNodeWithText("MO").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Repeat: Every Day").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("save_block_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), savedBlock?.daysOfWeek)
    }

    @Test
    fun `editing existing block can delete cleanly`() {
        var deletedId: String? = null
        val block = ScheduleBlock(
            id = "block_42",
            title = "Focus Sprint",
            startMinutes = 600L,
            durationMinutes = 45L
        )

        composeRule.setContent {
            KlydoDayTheme {
                BlockEditorDialog(
                    initialBlock = block,
                    clockFormat24h = true,
                    onDismiss = {},
                    onSave = {},
                    onDelete = { deletedId = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("block_title_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("delete_block_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("block_42", deletedId)
    }
}
