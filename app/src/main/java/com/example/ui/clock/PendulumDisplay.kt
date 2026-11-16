package com.example.ui.clock

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import com.example.ui.theme.CreamDial
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSand
import com.example.ui.theme.NightAmber
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.PlyCharcoal
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent
import com.example.ui.viewmodel.KlydoViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val KlydoCyan = Color(0xFF28F7E6)
val KlydoViolet = Color(0xFF9D00DF)

@Composable
fun KlydoPendulumAperture(
    tSecondsProvider: () -> Float,
    pendulumStyle: PendulumStyle,
    syncedRodColor: Color,
    syncedBobColor: Color,
    pendulumEnabled: Boolean,
    isNightMode: Boolean,
    scrubOffsetMinutes: Float,
    onScrubDelta: (Float) -> Unit,
    onSnapToNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetRodColor = when {
        isNightMode -> NightAmber.copy(alpha = 0.72f)
        pendulumStyle == PendulumStyle.OBSIDIAN -> syncedRodColor
        pendulumStyle == PendulumStyle.BRASS -> Color(0xFFFFD873)
        pendulumStyle == PendulumStyle.TERRACOTTA -> Color(0xFFFF9B73)
        else -> syncedRodColor.copy(alpha = 0.90f)
    }
    val targetBobColor = when {
        isNightMode -> NightAmber
        pendulumStyle == PendulumStyle.OBSIDIAN -> syncedBobColor
        pendulumStyle == PendulumStyle.BRASS -> SaffronAmber
        pendulumStyle == PendulumStyle.TERRACOTTA -> TerracottaAccent
        else -> syncedBobColor
    }

    val animatedRodColor by animateColorAsState(
        targetValue = targetRodColor,
        animationSpec = tween(durationMillis = 420),
        label = "pendulum_rod_color"
    )
    val animatedBobColor by animateColorAsState(
        targetValue = targetBobColor,
        animationSpec = tween(durationMillis = 420),
        label = "pendulum_bob_color"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onScrubDelta((dragAmount.x - dragAmount.y) * 0.45f)
                }
            }
            .clickable { onSnapToNow() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black)

            if (pendulumEnabled) {
                val tSeconds = tSecondsProvider()
                val swingAngleDeg = sin((tSeconds * PI.toFloat()) + (scrubOffsetMinutes * 0.25f)) * 22f

                // Pivot sits above the upper lip of the circular LCD window so the rod
                // enters from the top rim and the large circular bob clips cleanly like the Klydoclock.
                val pivot = Offset(size.width * 0.5f, -size.height * 0.36f)
                val rodLength = size.height * 0.87f
                val rad = swingAngleDeg * PI.toFloat() / 180f
                val bobCenter = Offset(
                    pivot.x + rodLength * sin(rad),
                    pivot.y + rodLength * cos(rad)
                )
                val bobRadius = size.minDimension * 0.315f

                // Soft ambient glow behind the bob for smooth 90fps depth
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animatedBobColor.copy(alpha = 0.26f), Color.Transparent),
                        center = bobCenter,
                        radius = bobRadius * 1.45f
                    ),
                    radius = bobRadius * 1.45f,
                    center = bobCenter
                )

                drawLine(
                    color = animatedRodColor,
                    start = pivot,
                    end = bobCenter,
                    strokeWidth = size.minDimension * 0.055f,
                    cap = StrokeCap.Round
                )

                when {
                    isNightMode -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(NightAmber, Color(0xFF5A3310)),
                                center = Offset(bobCenter.x - bobRadius * 0.25f, bobCenter.y - bobRadius * 0.25f),
                                radius = bobRadius * 1.3f
                            ),
                            radius = bobRadius,
                            center = bobCenter
                        )
                    }

                    pendulumStyle == PendulumStyle.OBSIDIAN -> {
                        drawCircle(
                            color = animatedBobColor,
                            radius = bobRadius,
                            center = bobCenter
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.Transparent
                                ),
                                center = Offset(bobCenter.x - bobRadius * 0.30f, bobCenter.y - bobRadius * 0.30f),
                                radius = bobRadius * 0.95f
                            ),
                            radius = bobRadius,
                            center = bobCenter
                        )
                    }

                    pendulumStyle == PendulumStyle.BRASS -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFE598), animatedBobColor, Color(0xFF8C5B12)),
                                center = Offset(bobCenter.x - bobRadius * 0.28f, bobCenter.y - bobRadius * 0.28f),
                                radius = bobRadius * 1.35f
                            ),
                            radius = bobRadius,
                            center = bobCenter
                        )
                    }

                    pendulumStyle == PendulumStyle.TERRACOTTA -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFF9E78), animatedBobColor, Color(0xFF7D2E14)),
                                center = Offset(bobCenter.x - bobRadius * 0.28f, bobCenter.y - bobRadius * 0.28f),
                                radius = bobRadius * 1.35f
                            ),
                            radius = bobRadius,
                            center = bobCenter
                        )
                    }

                    else -> {
                        drawCircle(
                            color = animatedBobColor,
                            radius = bobRadius * 0.90f,
                            center = bobCenter,
                            style = Stroke(width = size.minDimension * 0.048f)
                        )
                    }
                }
            }

            // Crisp recessed glass bezel ring around the circular screen
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF050608),
                        Color(0xFF2F333C),
                        Color(0xFF14161B)
                    )
                ),
                radius = size.minDimension * 0.494f,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(width = size.minDimension * 0.022f)
            )
        }
    }
}

@Composable
fun RoutineStatusStrip(
    activeBlock: ScheduleBlock?,
    nextBlock: ScheduleBlock?,
    effectiveMinuteOfDay: Int,
    clockFormat24h: Boolean,
    artTitle: String,
    accentColor: Color,
    isNightMode: Boolean,
    scrubOffsetMinutes: Float,
    onScrubDelta: (Float) -> Unit,
    onSnapToNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HairlineBorder, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = PlyCharcoal
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val isCompactWidth = maxWidth < 360.dp
            val knobSize = if (isCompactWidth) 54.dp else 62.dp
            val isScrubbing = scrubOffsetMinutes != 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = KlydoViewModel.formatMinutesOfDay(effectiveMinuteOfDay, clockFormat24h),
                            style = if (isCompactWidth) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            color = if (isNightMode) NightAmber else CreamDial,
                            maxLines = 1
                        )
                        if (isScrubbing) {
                            val sign = if (scrubOffsetMinutes > 0) "+" else ""
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = TerracottaAccent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onSnapToNow() }
                                    .testTag("snap_to_now_pill")
                            ) {
                                Text(
                                    text = "$sign${scrubOffsetMinutes.toInt()}m • SNAP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CreamDial,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            Text(
                                text = artTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSand,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (activeBlock != null) {
                        val remaining = (activeBlock.startMinutes + activeBlock.durationMinutes - effectiveMinuteOfDay).coerceAtLeast(0L)
                        Text(
                            text = "NOW • ${activeBlock.title} (${remaining}m left)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isNightMode) NightAmber else accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (nextBlock != null) {
                        val minsUntil = ((nextBlock.startMinutes - effectiveMinuteOfDay + 1440L) % 1440L)
                        Text(
                            text = "UP NEXT • ${nextBlock.title} (in ${minsUntil}m)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SaffronAmber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Tap face to switch art • Drag knob to scrub",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSand,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                KlydoScrubKnob(
                    isScrubbing = isScrubbing,
                    accentColor = accentColor,
                    knobSize = knobSize,
                    onScrubDelta = onScrubDelta,
                    onCenterTap = onSnapToNow
                )
            }
        }
    }
}

@Composable
fun KlydoScrubKnob(
    isScrubbing: Boolean,
    accentColor: Color = KlydoCyan,
    knobSize: Dp = 62.dp,
    onScrubDelta: (Float) -> Unit,
    onCenterTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var knobAngle by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(knobSize)
                .clip(CircleShape)
                .background(ObsidianChassis)
                .border(2.dp, if (isScrubbing) TerracottaAccent else HairlineBorder, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = (dragAmount.x - dragAmount.y) * 0.55f
                        knobAngle = (knobAngle + delta * 3f) % 360f
                        onScrubDelta(delta)
                    }
                }
                .clickable { onCenterTap() }
                .testTag("klydo_scrub_knob"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension * 0.34f
                for (i in 0 until 12) {
                    val a = (i * 30f + knobAngle) * PI.toFloat() / 180f
                    val p1 = Offset(cx + r * 0.85f * cos(a), cy + r * 0.85f * sin(a))
                    val p2 = Offset(cx + r * 1.1f * cos(a), cy + r * 1.1f * sin(a))
                    drawLine(
                        color = MutedSand.copy(alpha = 0.4f),
                        start = p1,
                        end = p2,
                        strokeWidth = 2.0f,
                        cap = StrokeCap.Round
                    )
                }
                val markerRad = (knobAngle - 90f) * PI.toFloat() / 180f
                drawCircle(
                    color = if (isScrubbing) TerracottaAccent else accentColor,
                    radius = 4.0f,
                    center = Offset(cx + r * 0.65f * cos(markerRad), cy + r * 0.65f * sin(markerRad))
                )
            }

            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.snap_to_now),
                tint = if (isScrubbing) CreamDial else MutedSand,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = if (isScrubbing) "SNAP" else "SCRUB",
            style = MaterialTheme.typography.labelSmall,
            color = if (isScrubbing) TerracottaAccent else MutedSand
        )
    }
}
