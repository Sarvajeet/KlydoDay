package com.example.ui.scheduler

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.ChimeInterval
import com.example.audio.ChimeScheduleEvaluator
import com.example.audio.ChimeTone
import com.example.data.local.ClockSurfaceState
import com.example.data.model.DisplaySettings
import com.example.ui.theme.CelestialTeal
import com.example.ui.theme.CreamDial
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSand
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.WarmSlate
import java.io.File
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChimesSettingsTab(
    settings: DisplaySettings,
    surfaceState: ClockSurfaceState,
    isImportingChime: Boolean,
    chimeImportError: String?,
    onTransformSettings: ((DisplaySettings) -> DisplaySettings) -> Unit,
    onUpdateChimePreferences: (
        interval: ChimeInterval?,
        tone: ChimeTone?,
        strikeHourCount: Boolean?,
        chimeInBackground: Boolean?,
        selectedCustomChimeId: String?
    ) -> Unit,
    onPickCustomChime: () -> Unit,
    onDeleteCustomChime: (String) -> Unit,
    onTestChime: (ChimeTone, Int, File?) -> Unit,
    resolveChimeFile: (String) -> File
) {
    val effectiveInterval = ChimeScheduleEvaluator.effectiveInterval(
        hourlyChimeEnabled = settings.hourlyChimeEnabled,
        configuredInterval = surfaceState.chimeInterval
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionCard(
            eyebrow = "CHIME SCHEDULE",
            trailingBadge = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (effectiveInterval != ChimeInterval.OFF) {
                        CelestialTeal.copy(alpha = 0.2f)
                    } else {
                        WarmSlate
                    },
                    modifier = Modifier.widthIn(max = 210.dp)
                ) {
                    Text(
                        text = if (effectiveInterval == ChimeInterval.OFF) {
                            "CHIMES OFF"
                        } else {
                            "${effectiveInterval.label.uppercase()} • ${surfaceState.activeChimeLabel.uppercase()}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (effectiveInterval != ChimeInterval.OFF) CelestialTeal else MutedSand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        ) {
            Text(
                text = "Choose how often the clock chimes. Chimes pause automatically overnight when Night Mode is active.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSand
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ChimeInterval.entries.forEach { interval ->
                    val selected = effectiveInterval == interval
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) TerracottaAccent else WarmSlate)
                            .border(
                                1.dp,
                                if (selected) CreamDial.copy(alpha = 0.5f) else HairlineBorder,
                                RoundedCornerShape(50)
                            )
                            .clickable {
                                val enabled = interval != ChimeInterval.OFF
                                onTransformSettings { it.copy(hourlyChimeEnabled = enabled) }
                                onUpdateChimePreferences(interval, null, null, null, null)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("chime_interval_${interval.key}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = interval.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) CreamDial else MutedSand
                        )
                    }
                }
            }
        }

        SettingsSectionCard(eyebrow = "SOUND VOICE & CUSTOM AUDIO") {
            Text(
                text = "Built-in Chime Voices",
                style = MaterialTheme.typography.labelMedium,
                color = CreamDial
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ChimeTone.entries.forEach { tone ->
                    val selected = surfaceState.selectedCustomChimeId.isBlank() &&
                        surfaceState.chimeTone == tone
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) SaffronAmber.copy(alpha = 0.22f) else WarmSlate)
                            .border(
                                1.dp,
                                if (selected) SaffronAmber else HairlineBorder,
                                RoundedCornerShape(50)
                            )
                            .clickable {
                                onUpdateChimePreferences(null, tone, null, null, "")
                                onTestChime(tone, 0, null)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("chime_tone_${tone.key}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tone.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) SaffronAmber else MutedSand
                        )
                    }
                }
            }

            HorizontalDivider(color = HairlineBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Chime Sounds",
                    style = MaterialTheme.typography.labelMedium,
                    color = CreamDial
                )
                OutlinedButton(
                    onClick = onPickCustomChime,
                    enabled = !isImportingChime,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("add_custom_chime_button")
                ) {
                    if (isImportingChime) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = SaffronAmber,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = SaffronAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isImportingChime) "Importing..." else "Add Sound",
                        color = CreamDial
                    )
                }
            }

            if (chimeImportError != null) {
                Text(
                    text = chimeImportError,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFB4AB)
                )
            }

            if (surfaceState.customChimes.isEmpty()) {
                Text(
                    text = "Import an MP3, WAV, M4A, or OGG clip — or share any audio file directly to KlydoDay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSand.copy(alpha = 0.85f)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    surfaceState.customChimes.forEach { chime ->
                        val isSelected = surfaceState.selectedCustomChimeId == chime.id
                        val durationLabel = String.format(
                            Locale.US,
                            "%.1fs",
                            (chime.durationMs.coerceAtLeast(100L)) / 1000.0
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) {
                                        SaffronAmber.copy(alpha = 0.16f)
                                    } else {
                                        WarmSlate.copy(alpha = 0.7f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) SaffronAmber else HairlineBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    onUpdateChimePreferences(null, null, null, null, chime.id)
                                    onTestChime(
                                        surfaceState.chimeTone,
                                        0,
                                        resolveChimeFile(chime.audioFileName)
                                    )
                                }
                                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                                .testTag("custom_chime_item_${chime.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) {
                                    Icons.Rounded.CheckCircle
                                } else {
                                    Icons.Rounded.GraphicEq
                                },
                                contentDescription = null,
                                tint = if (isSelected) SaffronAmber else MutedSand,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chime.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) CreamDial else MutedSand,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Custom audio • $durationLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) SaffronAmber else MutedSand.copy(alpha = 0.75f)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteCustomChime(chime.id) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_custom_chime_${chime.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete ${chime.title}",
                                    tint = CrimsonAlert
                                )
                            }
                        }
                    }
                }
            }
        }

        SettingsSectionCard(eyebrow = "CHIME BEHAVIOR") {
            SettingSwitchRow(
                title = "Strike the Hour Count",
                subtitle = "Ring gentle hour bells (1–12) after top-of-hour chimes",
                checked = surfaceState.strikeHourCount,
                onCheckedChange = { onUpdateChimePreferences(null, null, it, null, null) },
                switchTestTag = "strike_hour_switch"
            )

            SettingSwitchRow(
                title = "Background Chimes (Screen Off)",
                subtitle = "Keep hourly & interval chimes active when app is closed",
                checked = surfaceState.chimeInBackground,
                onCheckedChange = { onUpdateChimePreferences(null, null, null, it, null) },
                switchTestTag = "background_chime_switch"
            )

            SettingSwitchRow(
                title = "Routine Block Chimes",
                subtitle = "Sound a cue when scheduled blocks begin",
                checked = settings.blockChimeEnabled,
                onCheckedChange = { checked -> onTransformSettings { it.copy(blockChimeEnabled = checked) } }
            )

            OutlinedButton(
                onClick = {
                    val customFile = surfaceState.activeCustomChime?.let {
                        resolveChimeFile(it.audioFileName)
                    }
                    onTestChime(
                        surfaceState.chimeTone,
                        if (surfaceState.strikeHourCount) 2 else 0,
                        customFile
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("test_chime_button")
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = SaffronAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Test ${surfaceState.activeChimeLabel} Chime",
                    color = CreamDial,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
