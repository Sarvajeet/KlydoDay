package com.example.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CustomWatchFace
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
class WatchFaceLibraryStoreTest {

    @Test
    fun `store persists deleted prebuilt faces and restores cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = WatchFaceLibraryStore(context)
        store.restoreAllPrebuiltFaces().getOrThrow()

        store.deletePrebuiltFace("bauhaus_orbit").getOrThrow()
        store.deletePrebuiltFace("sleepy_neko").getOrThrow()

        val afterDelete = store.libraryFlow.first()
        assertTrue(afterDelete.deletedPrebuiltIds.contains("bauhaus_orbit"))
        assertTrue(afterDelete.deletedPrebuiltIds.contains("sleepy_neko"))
        assertFalse(afterDelete.availableArtMetas().any { it.id == "bauhaus_orbit" })

        store.restorePrebuiltFace("bauhaus_orbit").getOrThrow()
        val afterPartialRestore = store.libraryFlow.first()
        assertFalse(afterPartialRestore.deletedPrebuiltIds.contains("bauhaus_orbit"))
        assertTrue(afterPartialRestore.deletedPrebuiltIds.contains("sleepy_neko"))

        store.restoreAllPrebuiltFaces().getOrThrow()
        val afterFullRestore = store.libraryFlow.first()
        assertTrue(afterFullRestore.deletedPrebuiltIds.isEmpty())
    }

    @Test
    fun `custom watch face json serialization round trips cleanly`() {
        val sample = listOf(
            CustomWatchFace(
                id = "custom_vid_001",
                title = "Sunset Waves",
                videoFileName = "custom_vid_001.mp4",
                thumbnailFileName = "custom_vid_001.jpg",
                durationMs = 9500L,
                clipStartMs = 500L,
                clipEndMs = 9000L,
                isDarkFace = false,
                handsColorHex = "#14161A",
                pendulumRodColorHex = "#4A4E58",
                pendulumBobColorHex = "#D94E34",
                createdAtMillis = 123456789L
            )
        )
        val encoded = WatchFaceLibraryStore.encodeCustomFaces(sample)
        val decoded = WatchFaceLibraryStore.decodeCustomFaces(encoded)
        assertEquals(sample, decoded)
    }
}
