package com.example.ui.scheduler

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.WatchFaceLibraryStore
import com.example.data.model.CustomWatchFace
import com.example.data.model.DisplaySettings
import com.example.data.model.KlydoArtCategory
import com.example.data.model.KlydoArtMeta
import com.example.data.model.PendulumStyle
import com.example.data.model.WatchFaceLibraryState
import com.example.ui.clock.KlydoArtCanvas
import com.example.ui.clock.KlydoCyan
import com.example.ui.clock.KlydoViolet
import com.example.ui.clock.parseHexColorOrDefault
import com.example.ui.theme.CreamDial
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.PlyCharcoal
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.WarmSlate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class GalleryFilter(val key: String, val label: String, val category: KlydoArtCategory? = null) {
    ALL("all", "All"),
    CUSTOM("custom", KlydoArtCategory.CUSTOM.label, KlydoArtCategory.CUSTOM),
    FAVORITES("favorites", "Favorites"),
    ABSTRACT("abstract", KlydoArtCategory.ABSTRACT.label, KlydoArtCategory.ABSTRACT),
    NATURE("nature", KlydoArtCategory.NATURE.label, KlydoArtCategory.NATURE),
    ARCHITECTURE("architecture", KlydoArtCategory.ARCHITECTURE.label, KlydoArtCategory.ARCHITECTURE),
    MANDALA("mandala", KlydoArtCategory.MANDALA.label, KlydoArtCategory.MANDALA),
    CARTOONS("cartoons", KlydoArtCategory.CARTOONS.label, KlydoArtCategory.CARTOONS),
    FUN("fun", KlydoArtCategory.FUN.label, KlydoArtCategory.FUN),
    MINIMAL("minimal", KlydoArtCategory.MINIMAL.label, KlydoArtCategory.MINIMAL)
}

@Composable
fun KlydoGalleryDialog(
    settings: DisplaySettings,
    libraryState: WatchFaceLibraryState,
    libraryStore: WatchFaceLibraryStore,
    onSelectKlydo: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectPendulum: (PendulumStyle) -> Unit,
    onToggleFrame: (Boolean) -> Unit,
    onOpenAddVideoFace: () -> Unit,
    onDeleteFace: (KlydoArtMeta) -> Unit,
    onRestorePrebuilt: () -> Unit,
    onDismiss: () -> Unit
) {
    val previewPhaseState = remember { mutableFloatStateOf(0f) }
    val phaseProvider: () -> Float = remember { { previewPhaseState.floatValue } }
    val staticMinuteProvider: () -> Float = remember { { 540f } }
    var selectedFilter by remember { mutableStateOf(GalleryFilter.ALL) }
    var confirmCustomDelete by remember { mutableStateOf<KlydoArtMeta?>(null) }

    val availableFaces = remember(libraryState) { libraryState.availableArtMetas() }
    val filteredCatalog = remember(availableFaces, selectedFilter, settings.favoriteKlydoIds) {
        when (selectedFilter) {
            GalleryFilter.ALL -> availableFaces
            GalleryFilter.FAVORITES -> availableFaces.filter { settings.favoriteKlydoIds.contains(it.id) }
            else -> availableFaces.filter { it.category == selectedFilter.category }
        }
    }

    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { nowNanos ->
                previewPhaseState.floatValue = ((nowNanos - startNanos) / 1_000_000_000.0).toFloat()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .heightIn(max = 740.dp)
                .border(1.dp, HairlineBorder, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = PlyCharcoal
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.klydo_gallery_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = CreamDial
                        )
                        Text(
                            text = "${availableFaces.size} faces · Add 5–10s videos or delete prebuilt faces",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSand
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (libraryState.deletedPrebuiltIds.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WarmSlate,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onRestorePrebuilt() }
                                    .testTag("restore_prebuilt_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Restore,
                                        contentDescription = "Restore deleted prebuilt faces",
                                        tint = SaffronAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Restore (${libraryState.deletedPrebuiltIds.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CreamDial
                                    )
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = onOpenAddVideoFace,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = TerracottaAccent,
                                contentColor = CreamDial
                            ),
                            modifier = Modifier.testTag("add_video_face_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Video Face",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close gallery", tint = MutedSand)
                        }
                    }
                }

                // Frame & Pendulum Material Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pendulum & Frame", style = MaterialTheme.typography.labelSmall, color = MutedSand)
                        Text(
                            text = "Hands & pendulum auto-sync to face",
                            style = MaterialTheme.typography.labelSmall,
                            color = SaffronAmber
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (settings.showFrame) TerracottaAccent else WarmSlate,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleFrame(!settings.showFrame) }
                                .testTag("gallery_frame_toggle")
                        ) {
                            Text(
                                text = if (settings.showFrame) "Frame: On" else "Frame: Off",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (settings.showFrame) CreamDial else MutedSand,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }

                        PendulumStyle.entries.forEach { style ->
                            val isSelected = settings.pendulumStyle == style.key
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) TerracottaAccent else WarmSlate,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectPendulum(style) }
                            ) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) CreamDial else MutedSand,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // Category Filter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GalleryFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        val count = when (filter) {
                            GalleryFilter.ALL -> availableFaces.size
                            GalleryFilter.FAVORITES -> availableFaces.count { settings.favoriteKlydoIds.contains(it.id) }
                            else -> availableFaces.count { it.category == filter.category }
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) SaffronAmber else ObsidianChassis,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) SaffronAmber else HairlineBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedFilter = filter }
                                .testTag("gallery_filter_${filter.key}")
                        ) {
                            Text(
                                text = "${filter.label} ($count)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) ObsidianChassis else CreamDial,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                if (filteredCatalog.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = when (selectedFilter) {
                                    GalleryFilter.CUSTOM -> "No custom video faces yet. Add or share a 5–10s clip!"
                                    GalleryFilter.FAVORITES -> "Tap the heart icon on any watch face to save favorites here."
                                    else -> "No watch faces in this category."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSand
                            )
                            if (selectedFilter == GalleryFilter.CUSTOM) {
                                FilledTonalButton(
                                    onClick = onOpenAddVideoFace,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = TerracottaAccent,
                                        contentColor = CreamDial
                                    )
                                ) {
                                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Add 5–10s Video Face", modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 164.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(min = 220.dp, max = 430.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(filteredCatalog, key = { it.id }) { art ->
                            val isActive = settings.activeKlydoId == art.id
                            val isFav = settings.favoriteKlydoIds.contains(art.id)
                            val customFace = libraryState.findCustomFace(art.id)
                            val canDelete = availableFaces.size > 1

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ObsidianChassis,
                                modifier = Modifier
                                    .border(
                                        width = if (isActive) 2.dp else 1.dp,
                                        color = if (isActive) TerracottaAccent else HairlineBorder,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onSelectKlydo(art.id) }
                                    .testTag("gallery_face_${art.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (customFace != null) {
                                        CustomFaceThumbnailPreview(
                                            customFace = customFace,
                                            libraryStore = libraryStore,
                                            modifier = Modifier.size(104.dp)
                                        )
                                    } else {
                                        KlydoArtCanvas(
                                            klydoId = art.id,
                                            phaseProvider = phaseProvider,
                                            minuteOfDayProvider = staticMinuteProvider,
                                            isNightMode = false,
                                            modifier = Modifier.size(104.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = art.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = CreamDial,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(parseHexColorOrDefault(art.handsColorHex, KlydoCyan))
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(parseHexColorOrDefault(art.pendulumBobColorHex, KlydoViolet))
                                            )
                                            Text(
                                                text = "${art.category.label} · ${art.studioCredit}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isActive) SaffronAmber else MutedSand,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onToggleFavorite(art.id) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                contentDescription = "Toggle favorite",
                                                tint = if (isFav) TerracottaAccent else MutedSand
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (customFace != null) {
                                                    confirmCustomDelete = art
                                                } else {
                                                    onDeleteFace(art)
                                                }
                                            },
                                            enabled = canDelete,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("delete_face_${art.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Delete watch face ${art.title}",
                                                tint = if (canDelete) MutedSand else WarmSlate
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val pendingDelete = confirmCustomDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmCustomDelete = null },
            containerColor = PlyCharcoal,
            titleContentColor = CreamDial,
            textContentColor = MutedSand,
            title = { Text("Delete custom video face?") },
            text = { Text("Remove \"${pendingDelete.title}\" and its saved video clip from KlydoDay?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmCustomDelete = null
                        onDeleteFace(pendingDelete)
                    }
                ) {
                    Text("Delete", color = TerracottaAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCustomDelete = null }) {
                    Text("Cancel", color = CreamDial)
                }
            }
        )
    }
}

@Composable
private fun CustomFaceThumbnailPreview(
    customFace: CustomWatchFace,
    libraryStore: WatchFaceLibraryStore,
    modifier: Modifier = Modifier
) {
    val thumbFileName = customFace.thumbnailFileName
    val bitmap by produceState<Bitmap?>(initialValue = null, thumbFileName) {
        value = if (thumbFileName.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                val file = libraryStore.resolveMediaFile(thumbFileName)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF10131C)),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = customFace.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x88000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Looping video face",
                tint = CreamDial,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
