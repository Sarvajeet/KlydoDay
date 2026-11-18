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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
import com.example.ui.theme.CreamDial
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.PlyCharcoal
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.WarmSlate
import com.example.ui.viewmodel.KlydoViewModel

private val PALETTE_SWATCHES = listOf(
    "#E06B3E",
    "#E8A838",
    "#5E9E7A",
    "#3EA89C",
    "#6B7FE3",
    "#C85E7D",
    "#A078D8"
)

private val DURATION_PRESETS = listOf(15, 30, 45, 60, 90, 120, 180)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockEditorDialog(
    initialBlock: ScheduleBlock?,
    clockFormat24h: Boolean,
    onDismiss: () -> Unit,
    onSave: (ScheduleBlock) -> Unit,
    onDelete: ((String) -> Unit)?
) {
    val isEditing = initialBlock != null && initialBlock.id.isNotBlank()
    var title by remember { mutableStateOf(initialBlock?.title ?: "") }
    var subtitle by remember { mutableStateOf(initialBlock?.subtitle ?: "") }
    var startMinutes by remember { mutableIntStateOf((initialBlock?.startMinutes ?: 540L).toInt()) }
    var durationMinutes by remember { mutableIntStateOf((initialBlock?.durationMinutes ?: 60L).toInt()) }
    var selectedCategory by remember { mutableStateOf(ScheduleCategory.fromKey(initialBlock?.category ?: ScheduleCategory.FOCUS.key)) }
    var colorHex by remember { mutableStateOf(initialBlock?.colorHex ?: selectedCategory.defaultColorHex) }
    var daysOfWeek by remember { mutableStateOf(initialBlock?.daysOfWeek ?: emptyList()) }
    var chimeEnabled by remember { mutableStateOf(initialBlock?.chimeEnabled ?: true) }
    var titleError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(14.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .border(1.dp, HairlineBorder, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = PlyCharcoal
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) stringResource(R.string.edit_block_title) else stringResource(R.string.add_block_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = CreamDial
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = MutedSand)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(80)
                        titleError = false
                    },
                    label = { Text("Routine Title") },
                    placeholder = { Text("e.g., Morning Studio Focus") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Title is required", color = CrimsonAlert) }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamDial,
                        unfocusedTextColor = CreamDial,
                        focusedBorderColor = TerracottaAccent,
                        unfocusedBorderColor = HairlineBorder,
                        errorBorderColor = CrimsonAlert,
                        focusedLabelColor = TerracottaAccent,
                        unfocusedLabelColor = MutedSand,
                        errorLabelColor = CrimsonAlert
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("block_title_input")
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it.take(120) },
                    label = { Text("Context / Room (Optional)") },
                    placeholder = { Text("e.g., Studio desk • Silence alerts") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamDial,
                        unfocusedTextColor = CreamDial,
                        focusedBorderColor = TerracottaAccent,
                        unfocusedBorderColor = HairlineBorder,
                        focusedLabelColor = TerracottaAccent,
                        unfocusedLabelColor = MutedSand
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Start Time Stepper
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianChassis,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Start Time", style = MaterialTheme.typography.labelSmall, color = MutedSand)
                            Text(
                                text = KlydoViewModel.formatMinutesOfDay(startMinutes, clockFormat24h),
                                style = MaterialTheme.typography.headlineMedium,
                                color = CreamDial
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { startMinutes = ((startMinutes - 15) + 1440) % 1440 },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(WarmSlate)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Earlier 15m", tint = CreamDial)
                            }
                            IconButton(
                                onClick = { startMinutes = (startMinutes + 15) % 1440 },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(WarmSlate)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Later 15m", tint = CreamDial)
                            }
                        }
                    }
                }

                // Duration Presets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Duration", style = MaterialTheme.typography.labelSmall, color = MutedSand)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DURATION_PRESETS.forEach { mins ->
                            val selected = durationMinutes == mins
                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) TerracottaAccent else WarmSlate)
                                    .clickable { durationMinutes = mins },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (mins >= 60 && mins % 60 == 0) "${mins / 60}h" else "${mins}m",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) CreamDial else MutedSand,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelSmall, color = MutedSand)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScheduleCategory.entries.forEach { cat ->
                            val selected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) parseBlockColor(cat.defaultColorHex).copy(alpha = 0.25f) else WarmSlate)
                                    .border(
                                        1.dp,
                                        if (selected) parseBlockColor(cat.defaultColorHex) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedCategory = cat
                                        colorHex = cat.defaultColorHex
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(cat),
                                        contentDescription = null,
                                        tint = parseBlockColor(cat.defaultColorHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = cat.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CreamDial
                                    )
                                }
                            }
                        }
                    }
                }

                // Dial Arc Color Swatches
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dial Arc Color", style = MaterialTheme.typography.labelSmall, color = MutedSand)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PALETTE_SWATCHES.forEach { hex ->
                            val isSelected = colorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { colorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(parseBlockColor(hex))
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) CreamDial else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

                // Active Days (Empty = Every Day)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (daysOfWeek.isEmpty()) "Repeat: Every Day" else "Repeat: Selected Days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSand
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KlydoViewModel.WEEKDAYS.forEach { day ->
                            val active = daysOfWeek.isEmpty() || daysOfWeek.contains(day)
                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) WarmSlate else ObsidianChassis)
                                    .border(
                                        1.dp,
                                        if (active) TerracottaAccent else HairlineBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        val base = if (daysOfWeek.isEmpty()) KlydoViewModel.WEEKDAYS else daysOfWeek
                                        daysOfWeek = if (base.contains(day)) {
                                            val next = base - day
                                            if (next.isEmpty()) emptyList() else next
                                        } else {
                                            val next = base + day
                                            if (next.size == 7) emptyList() else next
                                        }
                                    }
                                    .testTag("weekday_chip_$day"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.take(2),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) CreamDial else MutedSand,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Block Start Chime Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Play Chime at Start", style = MaterialTheme.typography.titleMedium, color = CreamDial)
                        Text("Gentle harmonic tone when block begins", style = MaterialTheme.typography.bodyMedium, color = MutedSand)
                    }
                    Switch(
                        checked = chimeEnabled,
                        onCheckedChange = { chimeEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CreamDial, checkedTrackColor = TerracottaAccent)
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing && onDelete != null) {
                        TextButton(
                            onClick = { onDelete(initialBlock!!.id) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("delete_block_button")
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = CrimsonAlert)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = CrimsonAlert, maxLines = 1)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Cancel", color = MutedSand, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    titleError = true
                                } else {
                                    val saved = (initialBlock ?: ScheduleBlock()).copy(
                                        title = title.trim(),
                                        subtitle = subtitle.trim(),
                                        startMinutes = startMinutes.toLong(),
                                        durationMinutes = durationMinutes.toLong(),
                                        category = selectedCategory.key,
                                        colorHex = colorHex,
                                        daysOfWeek = daysOfWeek,
                                        chimeEnabled = chimeEnabled
                                    )
                                    onSave(saved)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaAccent, contentColor = CreamDial),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("save_block_button")
                        ) {
                            Text("Save", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
