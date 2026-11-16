package com.example.ui.scheduler

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WatchFaceLibraryStore
import com.example.data.model.DisplaySettings
import com.example.data.model.KlydoArtCatalog
import com.example.data.model.WatchFaceLibraryState
import com.example.ui.theme.KlydoDayTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KlydoGalleryDialogTabTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `switching gallery sub tabs displays faces favorites videos and style cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = WatchFaceLibraryStore(context)
        val hiddenPrebuilt = (KlydoArtCatalog.all.map { it.id } - setOf("bauhaus_orbit", "zen_ripple")).toSet()
        var deletedId: String? = null

        composeRule.setContent {
            KlydoDayTheme {
                KlydoGalleryDialog(
                    settings = DisplaySettings(favoriteKlydoIds = listOf("bauhaus_orbit")),
                    libraryState = WatchFaceLibraryState(
                        deletedPrebuiltIds = hiddenPrebuilt
                    ),
                    libraryStore = store,
                    onSelectKlydo = {},
                    onToggleFavorite = {},
                    onSelectPendulum = {},
                    onToggleFrame = {},
                    onOpenAddVideoFace = {},
                    onDeleteFace = { deletedId = it.id },
                    onRestorePrebuilt = {},
                    onDismiss = {},
                    animatePreviews = false
                )
            }
        }

        // Faces tab is active initially with category filter bar
        composeRule.onNodeWithTag("gallery_filter_all").assertIsDisplayed()
        composeRule.onNodeWithTag("gallery_frame_toggle").assertDoesNotExist()

        // Delete an inbuilt face via confirmation dialog
        composeRule.onNodeWithTag("watch_face_grid")
            .performScrollToNode(hasTestTag("gallery_face_zen_ripple"))
        composeRule.onNodeWithTag("delete_face_zen_ripple").performScrollTo().performClick()
        composeRule.onNodeWithTag("confirm_delete_face_button").performClick()
        org.junit.Assert.assertEquals("zen_ripple", deletedId)

        // Switch to Saved (Favorites) tab
        composeRule.onNodeWithTag("gallery_tab_favorites").performClick()
        composeRule.onNodeWithTag("gallery_face_bauhaus_orbit").assertIsDisplayed()
        composeRule.onNodeWithTag("gallery_filter_all").assertDoesNotExist()

        // Switch to Videos tab
        composeRule.onNodeWithTag("gallery_tab_custom").performClick()
        composeRule.onNodeWithTag("add_video_face_button").assertIsDisplayed()

        // Switch to Style tab
        composeRule.onNodeWithTag("gallery_tab_style").performClick()
        composeRule.onNodeWithTag("gallery_frame_toggle").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("pendulum_style_brass").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("restore_prebuilt_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("manage_inbuilt_faces_button").performScrollTo().assertIsDisplayed()
    }
}
