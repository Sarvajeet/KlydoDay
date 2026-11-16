package com.example.ui.scheduler

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.R
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
import com.example.ui.theme.CreamDial
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.PlyCharcoal
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.WarmSlate
import com.example.ui.viewmodel.KlydoViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun ScheduleTimelinePane(
    blocksState: UiState<List<ScheduleBlock>>,
    selectedDayCode: String,
    todayDateKey: String,
    effectiveMinuteOfDay: Int,
    clockFormat24h: Boolean,
    onSelectDay: (String) -> Unit,
    onAddBlock: () -> Unit,
    onEditBlock: (ScheduleBlock) -> Unit,
    onToggleComplete: (ScheduleBlock) -> Unit,
    onLoadStarterRoutine: () -> Unit,
    listBottomPadding: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = PlyCharcoal
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Weekday selector strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KlydoViewModel.WEEKDAYS.forEach { day ->
                    val isSelected = day == selectedDayCode
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) TerracottaAccent else WarmSlate)
                            .clickable { onSelectDay(day) }
                            .testTag("day_chip_$day"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) CreamDial else MutedSand,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            when (blocksState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TerracottaAccent)
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = blocksState.message,
                            color = Color(0xFFFFB4AB),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is UiState.Success -> {
                    val allBlocks = blocksState.data
                    val dayBlocks = allBlocks.filter { it.isActiveForDay(selectedDayCode) }
                        .sortedBy { it.startMinutes }

                    if (dayBlocks.isEmpty()) {
                        EmptyScheduleState(
                            onAddBlock = onAddBlock,
                            onLoadStarterRoutine = onLoadStarterRoutine,
                            hasAnyBlocks = allBlocks.isNotEmpty(),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val activeBlock = dayBlocks.firstOrNull { block ->
                            val start = block.startMinutes.toInt()
                            val end = start + block.durationMinutes.toInt()
                            effectiveMinuteOfDay in start until end
                        }

                        if (activeBlock != null) {
                            ActiveBlockHeroCard(
                                block = activeBlock,
                                effectiveMinuteOfDay = effectiveMinuteOfDay,
                                clockFormat24h = clockFormat24h,
                                isCompleted = activeBlock.isCompletedOn(todayDateKey),
                                onToggleComplete = { onToggleComplete(activeBlock) },
                                onEdit = { onEditBlock(activeBlock) }
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = listBottomPadding)
                        ) {
                            items(dayBlocks, key = { it.id }) { block ->
                                val isActive = block.id == activeBlock?.id
                                val isPast = (block.startMinutes + block.durationMinutes) <= effectiveMinuteOfDay
                                ScheduleBlockCard(
                                    block = block,
                                    isActive = isActive,
                                    isPast = isPast,
                                    isCompleted = block.isCompletedOn(todayDateKey),
                                    clockFormat24h = clockFormat24h,
                                    onToggleComplete = { onToggleComplete(block) },
                                    onClick = { onEditBlock(block) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBlockHeroCard(
    block: ScheduleBlock,
    effectiveMinuteOfDay: Int,
    clockFormat24h: Boolean,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit
) {
    val accent = parseBlockColor(block.colorHex)
    val elapsed = (effectiveMinuteOfDay - block.startMinutes).coerceAtLeast(0L)
    val progress = (elapsed.toFloat() / block.durationMinutes.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val remaining = (block.startMinutes + block.durationMinutes - effectiveMinuteOfDay).coerceAtLeast(0L)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ObsidianChassis,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            .clickable { onEdit() }
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Text(
                        text = "ACTIVE BLOCK • ${remaining}M LEFT",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "Mark active block complete",
                        tint = if (isCompleted) SaffronAmber else MutedSand
                    )
                }
            }

            Text(
                text = block.title,
                style = MaterialTheme.typography.headlineMedium,
                color = CreamDial,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (block.subtitle.isNotBlank()) {
                Text(
                    text = block.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSand,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = WarmSlate,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = KlydoViewModel.formatMinutesOfDay(block.startMinutes.toInt(), clockFormat24h),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSand
                )
                Text(
                    text = KlydoViewModel.formatMinutesOfDay(block.endMinutes, clockFormat24h),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSand
                )
            }
        }
    }
}

@Composable
private fun ScheduleBlockCard(
    block: ScheduleBlock,
    isActive: Boolean,
    isPast: Boolean,
    isCompleted: Boolean,
    clockFormat24h: Boolean,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
) {
    val accent = parseBlockColor(block.colorHex)
    val category = ScheduleCategory.fromKey(block.category)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isActive) WarmSlate else ObsidianChassis.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) accent else HairlineBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("schedule_block_card_${block.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Color pill bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isPast && !isActive) accent.copy(alpha = 0.45f) else accent)
            )

            // Time column
            Column(modifier = Modifier.width(74.dp)) {
                Text(
                    text = KlydoViewModel.formatMinutesOfDay(block.startMinutes.toInt(), clockFormat24h),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isPast && !isActive) MutedSand else CreamDial,
                    maxLines = 1
                )
                Text(
                    text = "${block.durationMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSand
                )
            }

            // Category Icon
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = category.label,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Title & subtitle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) MutedSand else CreamDial,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (block.subtitle.isNotBlank()) {
                    Text(
                        text = block.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (block.chimeEnabled) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = "Chime active",
                    tint = MutedSand.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = "Toggle completion",
                    tint = if (isCompleted) SaffronAmber else MutedSand
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyScheduleState(
    onAddBlock: () -> Unit,
    onLoadStarterRoutine: () -> Unit,
    hasAnyBlocks: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = SaffronAmber,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.empty_schedule_title),
            style = MaterialTheme.typography.headlineMedium,
            color = CreamDial,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_schedule_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MutedSand,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(22.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAddBlock,
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaAccent, contentColor = CreamDial),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Block")
            }
            if (!hasAnyBlocks) {
                OutlinedButton(
                    onClick = onLoadStarterRoutine,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.starter_routine_cta), color = SaffronAmber)
                }
            }
        }
    }
}

fun parseBlockColor(hex: String): Color = try {
    Color(hex.toColorInt())
} catch (_: Exception) {
    TerracottaAccent
}

fun categoryIcon(category: ScheduleCategory): ImageVector = when (category) {
    ScheduleCategory.FOCUS -> Icons.Rounded.Bolt
    ScheduleCategory.WELLNESS -> Icons.Rounded.SelfImprovement
    ScheduleCategory.FAMILY -> Icons.Rounded.Groups
    ScheduleCategory.MEAL -> Icons.Rounded.Restaurant
    ScheduleCategory.CREATIVE -> Icons.Rounded.Brush
    ScheduleCategory.REST -> Icons.Rounded.Bedtime
    ScheduleCategory.ERRAND -> Icons.Rounded.ShoppingBag
}
