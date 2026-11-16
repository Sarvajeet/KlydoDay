package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CustomWatchFace
import com.example.data.model.KlydoArtCatalog
import com.example.data.model.VideoFacePalettePreset
import com.example.data.model.WatchFaceLibraryState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private val Context.faceLibraryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "klydo_face_library"
)

data class InspectedVideoInfo(
    val uri: Uri,
    val suggestedTitle: String,
    val durationMs: Long,
    val recommendedStartMs: Long,
    val recommendedEndMs: Long,
    val previewThumbnail: Bitmap?
)

class WatchFaceLibraryStore(private val context: Context) {

    private val customFacesKey = stringPreferencesKey("custom_faces_json")
    private val deletedPrebuiltKey = stringPreferencesKey("deleted_prebuilt_ids_json")

    private val mediaDir: File
        get() = File(context.filesDir, MEDIA_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

    val libraryFlow: Flow<WatchFaceLibraryState> =
        context.faceLibraryDataStore.data.map { prefs ->
            WatchFaceLibraryState(
                customFaces = decodeCustomFaces(prefs[customFacesKey]),
                deletedPrebuiltIds = decodeDeletedIds(prefs[deletedPrebuiltKey])
            )
        }

    fun resolveMediaFile(fileName: String): File = File(mediaDir, fileName)

    suspend fun inspectVideoUri(uri: Uri): Result<InspectedVideoInfo> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            val rawName = queryDisplayName(uri)
            val suggestedTitle = sanitizeTitle(rawName?.substringBeforeLast('.') ?: "My Video Face")

            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            if (durationMs < MIN_VALID_DURATION_MS) {
                return@withContext Result.failure(
                    IllegalArgumentException("Please select a video clip at least 1 second long (5–10s recommended).")
                )
            }

            val (recStart, recEnd) = clampClipWindow(durationMs, 0L, minOf(durationMs, MAX_CLIP_DURATION_MS))
            val frameAtUs = (recStart + (recEnd - recStart) / 3L) * 1000L
            val rawFrame = retriever.getFrameAtTime(frameAtUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val scaledThumb = rawFrame?.let { scaleSquareCenterCrop(it, THUMB_SIZE_PX) }
            if (rawFrame != null && rawFrame !== scaledThumb) {
                rawFrame.recycle()
            }

            Result.success(
                InspectedVideoInfo(
                    uri = uri,
                    suggestedTitle = suggestedTitle,
                    durationMs = durationMs,
                    recommendedStartMs = recStart,
                    recommendedEndMs = recEnd,
                    previewThumbnail = scaledThumb
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("Could not read this video file. Try an MP4 or MOV clip.", e)
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun importVideoFace(
        uri: Uri,
        title: String,
        durationMs: Long,
        clipStartMs: Long,
        clipEndMs: Long,
        palette: VideoFacePalettePreset
    ): Result<CustomWatchFace> = withContext(Dispatchers.IO) {
        val shortHex = UUID.randomUUID().toString().replace("-", "").take(12)
        val faceId = "${CUSTOM_ID_PREFIX}$shortHex"
        val videoFileName = "$faceId.mp4"
        val thumbFileName = "$faceId.jpg"
        val destVideoFile = File(mediaDir, videoFileName)
        val destThumbFile = File(mediaDir, thumbFileName)

        try {
            val copiedBytes = context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destVideoFile).use { output ->
                    input.copyTo(output)
                }
            } ?: 0L

            if (copiedBytes <= 0L || !destVideoFile.exists()) {
                destVideoFile.delete()
                return@withContext Result.failure(
                    IllegalStateException("Unable to import video clip from shared source.")
                )
            }

            val retriever = MediaMetadataRetriever()
            val actualDurationMs: Long
            try {
                retriever.setDataSource(destVideoFile.absolutePath)
                actualDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: durationMs
                val safeWindow = clampClipWindow(actualDurationMs, clipStartMs, clipEndMs)
                val frame = retriever.getFrameAtTime(
                    safeWindow.first * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (frame != null) {
                    val thumb = scaleSquareCenterCrop(frame, THUMB_SIZE_PX)
                    FileOutputStream(destThumbFile).use { out ->
                        thumb.compress(Bitmap.CompressFormat.JPEG, 88, out)
                    }
                    if (thumb !== frame) thumb.recycle()
                    frame.recycle()
                }
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }

            val (safeStart, safeEnd) = clampClipWindow(actualDurationMs, clipStartMs, clipEndMs)
            val customFace = CustomWatchFace(
                id = faceId,
                title = sanitizeTitle(title),
                videoFileName = videoFileName,
                thumbnailFileName = if (destThumbFile.exists()) thumbFileName else "",
                durationMs = actualDurationMs,
                clipStartMs = safeStart,
                clipEndMs = safeEnd,
                isDarkFace = palette.isDarkFace,
                handsColorHex = palette.handsColorHex,
                pendulumRodColorHex = palette.pendulumRodColorHex,
                pendulumBobColorHex = palette.pendulumBobColorHex
            )

            context.faceLibraryDataStore.edit { prefs ->
                val list = decodeCustomFaces(prefs[customFacesKey]).toMutableList()
                list.add(0, customFace)
                prefs[customFacesKey] = encodeCustomFaces(list)
            }
            Result.success(customFace)
        } catch (e: Exception) {
            destVideoFile.delete()
            destThumbFile.delete()
            Result.failure(e)
        }
    }

    suspend fun deleteCustomFace(faceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            var removed: CustomWatchFace? = null
            context.faceLibraryDataStore.edit { prefs ->
                val list = decodeCustomFaces(prefs[customFacesKey])
                removed = list.firstOrNull { it.id == faceId }
                val updated = list.filterNot { it.id == faceId }
                prefs[customFacesKey] = encodeCustomFaces(updated)
            }
            removed?.let { face ->
                File(mediaDir, face.videoFileName).delete()
                if (face.thumbnailFileName.isNotBlank()) {
                    File(mediaDir, face.thumbnailFileName).delete()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePrebuiltFace(faceId: String): Result<Unit> {
        return try {
            if (KlydoArtCatalog.all.none { it.id == faceId }) {
                return Result.failure(IllegalArgumentException("Unknown prebuilt watch face"))
            }
            context.faceLibraryDataStore.edit { prefs ->
                val set = decodeDeletedIds(prefs[deletedPrebuiltKey]).toMutableSet()
                set.add(faceId)
                prefs[deletedPrebuiltKey] = encodeDeletedIds(set)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restorePrebuiltFace(faceId: String): Result<Unit> {
        return try {
            context.faceLibraryDataStore.edit { prefs ->
                val set = decodeDeletedIds(prefs[deletedPrebuiltKey]).toMutableSet()
                set.remove(faceId)
                prefs[deletedPrebuiltKey] = encodeDeletedIds(set)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreAllPrebuiltFaces(): Result<Unit> {
        return try {
            context.faceLibraryDataStore.edit { prefs ->
                prefs.remove(deletedPrebuiltKey)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    companion object {
        const val CUSTOM_ID_PREFIX = "custom_vid_"
        const val MEDIA_DIR_NAME = "custom_video_faces"
        const val MIN_VALID_DURATION_MS = 1000L
        const val MIN_CLIP_DURATION_MS = 3000L
        const val MAX_CLIP_DURATION_MS = 10_000L
        private const val THUMB_SIZE_PX = 320

        fun isCustomVideoId(id: String): Boolean = id.startsWith(CUSTOM_ID_PREFIX)

        fun sanitizeTitle(raw: String): String {
            val cleaned = raw.trim().replace('_', ' ').replace('-', ' ').replace(Regex("\\s+"), " ")
            return cleaned.ifBlank { "My Video Face" }.take(32)
        }

        fun clampClipWindow(durationMs: Long, startMs: Long, endMs: Long): Pair<Long, Long> {
            val safeDuration = durationMs.coerceAtLeast(MIN_VALID_DURATION_MS)
            if (safeDuration <= MAX_CLIP_DURATION_MS) {
                return 0L to safeDuration
            }
            val safeStart = startMs.coerceIn(0L, safeDuration - MIN_CLIP_DURATION_MS)
            val desiredLen = (endMs - safeStart).coerceIn(MIN_CLIP_DURATION_MS, MAX_CLIP_DURATION_MS)
            val safeEnd = (safeStart + desiredLen).coerceAtMost(safeDuration)
            return safeStart to safeEnd
        }

        private fun scaleSquareCenterCrop(source: Bitmap, targetPx: Int): Bitmap {
            val minDim = minOf(source.width, source.height).coerceAtLeast(1)
            val cropX = (source.width - minDim) / 2
            val cropY = (source.height - minDim) / 2
            val square = Bitmap.createBitmap(source, cropX, cropY, minDim, minDim)
            return if (minDim > targetPx) {
                val scaled = Bitmap.createScaledBitmap(square, targetPx, targetPx, true)
                if (scaled !== square) square.recycle()
                scaled
            } else {
                square
            }
        }

        fun encodeCustomFaces(faces: List<CustomWatchFace>): String {
            val arr = JSONArray()
            faces.forEach { face ->
                arr.put(
                    JSONObject().apply {
                        put("id", face.id)
                        put("title", face.title)
                        put("videoFileName", face.videoFileName)
                        put("thumbnailFileName", face.thumbnailFileName)
                        put("durationMs", face.durationMs)
                        put("clipStartMs", face.clipStartMs)
                        put("clipEndMs", face.clipEndMs)
                        put("isDarkFace", face.isDarkFace)
                        put("handsColorHex", face.handsColorHex)
                        put("pendulumRodColorHex", face.pendulumRodColorHex)
                        put("pendulumBobColorHex", face.pendulumBobColorHex)
                        put("createdAtMillis", face.createdAtMillis)
                    }
                )
            }
            return arr.toString()
        }

        fun decodeCustomFaces(raw: String?): List<CustomWatchFace> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("id")
                        val videoFileName = obj.optString("videoFileName")
                        if (id.isBlank() || videoFileName.isBlank()) continue
                        add(
                            CustomWatchFace(
                                id = id,
                                title = obj.optString("title", "My Video Face"),
                                videoFileName = videoFileName,
                                thumbnailFileName = obj.optString("thumbnailFileName", ""),
                                durationMs = obj.optLong("durationMs", 8000L),
                                clipStartMs = obj.optLong("clipStartMs", 0L),
                                clipEndMs = obj.optLong("clipEndMs", 8000L),
                                isDarkFace = obj.optBoolean("isDarkFace", true),
                                handsColorHex = obj.optString("handsColorHex", "#28F7E6"),
                                pendulumRodColorHex = obj.optString("pendulumRodColorHex", "#28F7E6"),
                                pendulumBobColorHex = obj.optString("pendulumBobColorHex", "#9D00DF"),
                                createdAtMillis = obj.optLong("createdAtMillis", 0L)
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun encodeDeletedIds(ids: Set<String>): String = JSONArray(ids.toList()).toString()

        fun decodeDeletedIds(raw: String?): Set<String> {
            if (raw.isNullOrBlank()) return emptySet()
            return try {
                val arr = JSONArray(raw)
                buildSet {
                    for (i in 0 until arr.length()) {
                        val id = arr.optString(i)
                        if (id.isNotBlank()) add(id)
                    }
                }
            } catch (_: Exception) {
                emptySet()
            }
        }
    }
}
