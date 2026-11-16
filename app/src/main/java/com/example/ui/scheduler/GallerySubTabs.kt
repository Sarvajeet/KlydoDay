package com.example.ui.scheduler

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.WatchFaceLibraryStore
import com.example.data.model.CustomWatchFace
import com.example.data.model.DisplaySettings
import com.example.data.model.KlydoArtCatalog
import com.example.data.model.KlydoArtMeta
import com.example.data.model.PendulumStyle
import com.example.data.model.WatchFaceLibraryState
import com.example.ui.clock.KlydoArtCanvas
import com.example.ui.clock.KlydoCyan
import com.example.ui.clock.KlydoViolet
import com.example.ui.clock.parseHexColorOrDefault
import com.example.ui.theme.CelestialTeal
import com.example.ui.theme.CreamDial
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.WarmSlate

@Composable
internal fun CustomVideoFacesTab(
    customFaces: List<KlydoArtMeta>,
    settings: DisplaySettings,
    libraryState: WatchFaceLibraryState,
    libraryStore: WatchFaceLibraryStore,
    canDelete: Boolean,
    phaseProvider: () -> Float,
    minuteOfDayProvider: () -> Float,
    onOpenAddVideoFace: () -> Unit,
    onSelectKlydo: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteRequest: (KlydoArtMeta, CustomWatchFace?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ObsidianChassis,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CUSTOM 5–10s VIDEO FACES",
                        style = MaterialTheme.typography.labelSmall,
                        color = SaffronAmber
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Import any 5–10s looping clip from Photos or share a video directly to KlydoDay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSand
                    )
                }
                Button(
                    onClick = onOpenAddVideoFace,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerracottaAccent,
                        contentColor = CreamDial
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("add_video_face_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Video")
                }
            }
        }

        if (customFaces.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VideoLibrary,
                        contentDescription = null,
                        tint = SaffronAmber,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "No custom video watch faces yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = CreamDial
                    )
                    Text(
                        text = "Tap Add Video above to turn your favorite animated clip or nature loop into a live clock face.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSand
                    )
                }
            }
        } else {
            WatchFaceGrid(
                faces = customFaces,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CabinetStyleTab(
    settings: DisplaySettings,
    activeArt: KlydoArtMeta,
    activeCustomFace: CustomWatchFace?,
    activePrebuiltFaces: List<KlydoArtMeta>,
    canDeleteFace: Boolean,
    libraryState: WatchFaceLibraryState,
    libraryStore: WatchFaceLibraryStore,
    phaseProvider: () -> Float,
    minuteOfDayProvider: () -> Float,
    onToggleFrame: (Boolean) -> Unit,
    onSelectPendulum: (PendulumStyle) -> Unit,
    onDeletePrebuiltFace: (KlydoArtMeta) -> Unit,
    onRestoreSinglePrebuilt: (String) -> Unit,
    onRestorePrebuilt: () -> Unit
) {
    var expandInbuiltManager by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionCard(
            eyebrow = "ACTIVE DIAL HARMONY",
            trailingBadge = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CelestialTeal.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "AUTO-SYNCED",
                        style = MaterialTheme.typography.labelSmall,
                        color = CelestialTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (activeCustomFace != null) {
                    CustomFaceThumbnailPreview(
                        customFace = activeCustomFace,
                        libraryStore = libraryStore,
                        modifier = Modifier.size(64.dp)
                    )
                } else {
                    KlydoArtCanvas(
                        klydoId = activeArt.id,
                        phaseProvider = phaseProvider,
                        minuteOfDayProvider = minuteOfDayProvider,
                        isNightMode = false,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = activeArt.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = CreamDial,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Hands & pendulum accents adapt automatically to match ${activeArt.title}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSand
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColorOrDefault(activeArt.handsColorHex, KlydoCyan))
                            )
                            Text("Hands", style = MaterialTheme.typography.labelSmall, color = MutedSand)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColorOrDefault(activeArt.pendulumBobColorHex, KlydoViolet))
                            )
                            Text("Pendulum", style = MaterialTheme.typography.labelSmall, color = MutedSand)
                        }
                    }
                }
            }
        }

        SettingsSectionCard(eyebrow = "CABINET HOUSING & PENDULUM") {
            SettingSwitchRow(
                title = "Clock Cabinet Frame",
                subtitle = "Show Baltic-birch & charcoal housing around the dial",
                checked = settings.showFrame,
                onCheckedChange = onToggleFrame,
                switchTestTag = "gallery_frame_toggle"
            )

            Text(
                text = "Pendulum Finish",
                style = MaterialTheme.typography.labelMedium,
                color = CreamDial
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PendulumStyle.entries.forEach { style ->
                    val isSelected = settings.pendulumStyle == style.key
                    Row(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) TerracottaAccent else WarmSlate)
                            .border(
                                1.dp,
                                if (isSelected) CreamDial.copy(alpha = 0.5f) else HairlineBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectPendulum(style) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("pendulum_style_${style.key}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = CreamDial,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = style.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) CreamDial else MutedSand,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        SettingsSectionCard(
            eyebrow = "INBUILT WATCH FACES",
            trailingBadge = {
                Text(
                    text = "${activePrebuiltFaces.size} active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSand
                )
            }
        ) {
            Text(
                text = "Tap the delete icon on any watch face card in Faces—or manage inbuilt watch faces right here.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSand
            )

            if (libraryState.deletedPrebuiltIds.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    libraryState.deletedPrebuiltIds.forEach { deletedId ->
                        val meta = KlydoArtCatalog.findById(deletedId)
                        Row(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(WarmSlate)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(50))
                                .clickable { onRestoreSinglePrebuilt(deletedId) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Restore ${meta.title}",
                                tint = SaffronAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = meta.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = CreamDial,
                                maxLines = 1
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onRestorePrebuilt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("restore_prebuilt_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restore,
                        contentDescription = null,
                        tint = SaffronAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Restore All ${libraryState.deletedPrebuiltIds.size} Inbuilt Face(s)",
                        color = CreamDial
                    )
                }
            }

            HorizontalDivider(color = HairlineBorder)

            TextButton(
                onClick = { expandInbuiltManager = !expandInbuiltManager },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("manage_inbuilt_faces_button")
            ) {
                Text(
                    text = if (expandInbuiltManager) {
                        "Hide Inbuilt Watch Faces"
                    } else {
                        "Manage & Delete Inbuilt Faces (${activePrebuiltFaces.size})"
                    },
                    color = SaffronAmber
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expandInbuiltManager) {
                        Icons.Rounded.ExpandLess
                    } else {
                        Icons.Rounded.ExpandMore
                    },
                    contentDescription = null,
                    tint = SaffronAmber
                )
            }

            AnimatedVisibility(visible = expandInbuiltManager) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    activePrebuiltFaces.forEach { face ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmSlate.copy(alpha = 0.65f))
                                .padding(start = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = face.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CreamDial,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = face.category.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSand
                                )
                            }
                            IconButton(
                                onClick = { onDeletePrebuiltFace(face) },
                                enabled = canDeleteFace,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("style_delete_inbuilt_${face.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete inbuilt face ${face.title}",
                                    tint = if (canDeleteFace) CrimsonAlert else MutedSand
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
