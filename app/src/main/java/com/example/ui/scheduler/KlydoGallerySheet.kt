package com.example.ui.scheduler

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.model.KlydoArtCatalog
import com.example.data.model.KlydoArtCategory
import com.example.data.model.KlydoArtMeta
import com.example.data.model.PendulumStyle
import com.example.data.model.WatchFaceLibraryState
import com.example.ui.clock.KlydoArtCanvas
import com.example.ui.clock.KlydoCyan
import com.example.ui.clock.KlydoViolet
import com.example.ui.clock.parseHexColorOrDefault
import com.example.ui.theme.CreamDial
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.PlyCharcoal
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.WarmSlate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class GalleryTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    FACES("Faces", Icons.Rounded.Palette, "gallery_tab_faces"),
    FAVORITES("Saved", Icons.Rounded.Favorite, "gallery_tab_favorites"),
    CUSTOM("Videos", Icons.Rounded.VideoLibrary, "gallery_tab_custom"),
    STYLE("Style", Icons.Rounded.Tune, "gallery_tab_style")
}

private enum class GalleryCategoryFilter(
    val key: String,
    val label: String,
    val category: KlydoArtCategory? = null
) {
    ALL("all", "All"),
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
    onRestoreSinglePrebuilt: (String) -> Unit = {},
    onDismiss: () -> Unit,
    initialTab: GalleryTab = GalleryTab.FACES,
    animatePreviews: Boolean = true
) {
    val previewPhaseState = remember { mutableFloatStateOf(0f) }
    val phaseProvider: () -> Float = remember { { previewPhaseState.floatValue } }
    val staticMinuteProvider: () -> Float = remember { { 540f } }
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var selectedFilter by remember { mutableStateOf(GalleryCategoryFilter.ALL) }
    var confirmDeleteFace by remember { mutableStateOf<KlydoArtMeta?>(null) }
    var recentlyDeletedPrebuilt by remember { mutableStateOf<KlydoArtMeta?>(null) }

    val availableFaces = remember(libraryState) { libraryState.availableArtMetas() }
    val activePrebuiltFaces = remember(availableFaces) {
        availableFaces.filter { it.category != KlydoArtCategory.CUSTOM }
    }
    val favoriteFaces = remember(availableFaces, settings.favoriteKlydoIds) {
        availableFaces.filter { settings.favoriteKlydoIds.contains(it.id) }
    }
    val customFaces = remember(availableFaces) {
        availableFaces.filter { it.category == KlydoArtCategory.CUSTOM }
    }
    val filteredFaces = remember(availableFaces, selectedFilter) {
        if (selectedFilter == GalleryCategoryFilter.ALL) {
            availableFaces
        } else {
            availableFaces.filter { it.category == selectedFilter.category }
        }
    }
    val activeArt = remember(availableFaces, settings.activeKlydoId) {
        availableFaces.firstOrNull { it.id == settings.activeKlydoId }
            ?: KlydoArtCatalog.findById(settings.activeKlydoId)
    }
    val activeCustomFace = remember(libraryState, activeArt.id) {
        libraryState.findCustomFace(activeArt.id)
    }
    val canDelete = availableFaces.size > 1

    LaunchedEffect(recentlyDeletedPrebuilt) {
        if (recentlyDeletedPrebuilt != null) {
            delay(4000L)
            recentlyDeletedPrebuilt = null
        }
    }

    if (animatePreviews) {
        LaunchedEffect(Unit) {
            val startNanos = withFrameNanos { it }
            while (true) {
                withFrameNanos { nowNanos ->
                    previewPhaseState.floatValue = ((nowNanos - startNanos) / 1_000_000_000.0).toFloat()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .heightIn(max = 720.dp)
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
                            text = "${availableFaces.size} faces · Active: ${activeArt.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSand,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_gallery_button")
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close gallery",
                            tint = MutedSand
                        )
                    }
                }

                GalleryTabBar(
                    selectedTab = selectedTab,
                    favoriteCount = favoriteFaces.size,
                    customCount = customFaces.size,
                    onSelectTab = { selectedTab = it }
                )

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180)) togetherWith
                            fadeOut(animationSpec = tween(120))
                    },
                    label = "gallery_tab_transition"
                ) { tab ->
                    when (tab) {
                        GalleryTab.FACES -> FacesArtTab(
                            availableFaces = availableFaces,
                            filteredFaces = filteredFaces,
                            selectedFilter = selectedFilter,
                            onSelectFilter = { selectedFilter = it },
                            settings = settings,
                            libraryState = libraryState,
                            libraryStore = libraryStore,
                            canDelete = canDelete,
                            phaseProvider = phaseProvider,
                            minuteOfDayProvider = staticMinuteProvider,
                            onSelectKlydo = onSelectKlydo,
                            onToggleFavorite = onToggleFavorite,
                            onDeleteRequest = { art, _ ->
                                confirmDeleteFace = art
                            }
                        )

                        GalleryTab.FAVORITES -> FavoritesArtTab(
                            favoriteFaces = favoriteFaces,
                            settings = settings,
                            libraryState = libraryState,
                            libraryStore = libraryStore,
                            canDelete = canDelete,
                            phaseProvider = phaseProvider,
                            minuteOfDayProvider = staticMinuteProvider,
                            onBrowseFaces = { selectedTab = GalleryTab.FACES },
                            onSelectKlydo = onSelectKlydo,
                            onToggleFavorite = onToggleFavorite,
                            onDeleteRequest = { art, _ ->
                                confirmDeleteFace = art
                            }
                        )

                        GalleryTab.CUSTOM -> CustomVideoFacesTab(
                            customFaces = customFaces,
                            settings = settings,
                            libraryState = libraryState,
                            libraryStore = libraryStore,
                            canDelete = canDelete,
                            phaseProvider = phaseProvider,
                            minuteOfDayProvider = staticMinuteProvider,
                            onOpenAddVideoFace = onOpenAddVideoFace,
                            onSelectKlydo = onSelectKlydo,
                            onToggleFavorite = onToggleFavorite,
                            onDeleteRequest = { art, _ ->
                                confirmDeleteFace = art
                            }
                        )

                        GalleryTab.STYLE -> CabinetStyleTab(
                            settings = settings,
                            activeArt = activeArt,
                            activeCustomFace = activeCustomFace,
                            activePrebuiltFaces = activePrebuiltFaces,
                            canDeleteFace = canDelete,
                            libraryState = libraryState,
                            libraryStore = libraryStore,
                            phaseProvider = phaseProvider,
                            minuteOfDayProvider = staticMinuteProvider,
                            onToggleFrame = onToggleFrame,
                            onSelectPendulum = onSelectPendulum,
                            onDeletePrebuiltFace = { face ->
                                confirmDeleteFace = face
                            },
                            onRestoreSinglePrebuilt = onRestoreSinglePrebuilt,
                            onRestorePrebuilt = onRestorePrebuilt
                        )
                    }
                }

                AnimatedVisibility(
                    visible = recentlyDeletedPrebuilt != null,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(180))
                ) {
                    val deleted = recentlyDeletedPrebuilt
                    if (deleted != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianChassis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SaffronAmber.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Removed \"${deleted.title}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CreamDial,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        onRestoreSinglePrebuilt(deleted.id)
                                        recentlyDeletedPrebuilt = null
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Undo", color = SaffronAmber)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val pendingDelete = confirmDeleteFace
    if (pendingDelete != null) {
        val isCustom = WatchFaceLibraryStore.isCustomVideoId(pendingDelete.id)
        AlertDialog(
            onDismissRequest = { confirmDeleteFace = null },
            containerColor = PlyCharcoal,
            titleContentColor = CreamDial,
            textContentColor = MutedSand,
            title = {
                Text(
                    if (isCustom) "Delete custom video face?" else "Delete inbuilt watch face?"
                )
            },
            text = {
                Text(
                    if (isCustom) {
                        "Remove \"${pendingDelete.title}\" and its saved video clip from KlydoDay?"
                    } else {
                        "Remove \"${pendingDelete.title}\" from your watch faces? You can restore removed inbuilt faces anytime from the Style tab."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteFace = null
                        onDeleteFace(pendingDelete)
                        if (!isCustom) {
                            recentlyDeletedPrebuilt = pendingDelete
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_face_button")
                ) {
                    Text("Delete", color = TerracottaAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFace = null }) {
                    Text("Cancel", color = CreamDial)
                }
            }
        )
    }
}

@Composable
private fun GalleryTabBar(
    selectedTab: GalleryTab,
    favoriteCount: Int,
    customCount: Int,
    onSelectTab: (GalleryTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianChassis)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GalleryTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            val labelText = when (tab) {
                GalleryTab.FAVORITES -> if (favoriteCount > 0) "${tab.label} ($favoriteCount)" else tab.label
                GalleryTab.CUSTOM -> if (customCount > 0) "${tab.label} ($customCount)" else tab.label
                else -> tab.label
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) TerracottaAccent else Color.Transparent)
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .testTag(tab.testTag),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) CreamDial else MutedSand,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) CreamDial else MutedSand,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FacesArtTab(
    availableFaces: List<KlydoArtMeta>,
    filteredFaces: List<KlydoArtMeta>,
    selectedFilter: GalleryCategoryFilter,
    onSelectFilter: (GalleryCategoryFilter) -> Unit,
    settings: DisplaySettings,
    libraryState: WatchFaceLibraryState,
    libraryStore: WatchFaceLibraryStore,
    canDelete: Boolean,
    phaseProvider: () -> Float,
    minuteOfDayProvider: () -> Float,
    onSelectKlydo: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteRequest: (KlydoArtMeta, CustomWatchFace?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GalleryCategoryFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                val count = if (filter == GalleryCategoryFilter.ALL) {
                    availableFaces.size
                } else {
                    availableFaces.count { it.category == filter.category }
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
                        .clickable { onSelectFilter(filter) }
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

        if (filteredFaces.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No watch faces in this category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSand
                )
            }
        } else {
            WatchFaceGrid(
                faces = filteredFaces,
                settings = settings,
                libraryState = libraryState,
                libraryStore = libraryStore,
                canDelete = canDelete,
                phaseProvider = phaseProvider,
                minuteOfDayProvider = minuteOfDayProvider,
                onSelectKlydo = onSelectKlydo,
                onToggleFavorite = onToggleFavorite,
                onDeleteRequest = onDeleteRequest
            )
        }
    }
}

@Composable
private fun FavoritesArtTab(
    favoriteFaces: List<KlydoArtMeta>,
    settings: DisplaySettings,
    libraryState: WatchFaceLibraryState,
    libraryStore: WatchFaceLibraryStore,
    canDelete: Boolean,
    phaseProvider: () -> Float,
    minuteOfDayProvider: () -> Float,
    onBrowseFaces: () -> Unit,
    onSelectKlydo: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteRequest: (KlydoArtMeta, CustomWatchFace?) -> Unit
) {
    if (favoriteFaces.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = TerracottaAccent,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "No saved favorites yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = CreamDial
                )
                Text(
                    text = "Tap the heart icon on any clock face to keep quick shortcuts here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSand
                )
                OutlinedButton(
                    onClick = onBrowseFaces,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Browse Faces", color = CreamDial)
                }
            }
        }
    } else {
        WatchFaceGrid(
            faces = favoriteFaces,
            settings = settings,
            libraryState = libraryState,
            libraryStore = libraryStore,
            canDelete = canDelete,
            phaseProvider = phaseProvider,
            minuteOfDayProvider = minuteOfDayProvider,
            onSelectKlydo = onSelectKlydo,
            onToggleFavorite = onToggleFavorite,
            onDeleteRequest = onDeleteRequest
        )
    }
}

@Composable
internal fun WatchFaceGrid(
    faces: List<KlydoArtMeta>,
    settings: DisplaySettings,
    libraryState: WatchFaceLibraryState,
    libraryStore: WatchFaceLibraryStore,
    canDelete: Boolean,
    phaseProvider: () -> Float,
    minuteOfDayProvider: () -> Float,
    onSelectKlydo: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteRequest: (KlydoArtMeta, CustomWatchFace?) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 164.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp, max = 460.dp)
            .testTag("watch_face_grid"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(faces, key = { it.id }) { art ->
            val isActive = settings.activeKlydoId == art.id
            val isFav = settings.favoriteKlydoIds.contains(art.id)
            val customFace = libraryState.findCustomFace(art.id)

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
                            minuteOfDayProvider = minuteOfDayProvider,
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
                            onClick = { onDeleteRequest(art, customFace) },
                            enabled = canDelete,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_face_${art.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete watch face ${art.title}",
                                tint = if (canDelete) CrimsonAlert.copy(alpha = 0.88f) else WarmSlate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CustomFaceThumbnailPreview(
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
