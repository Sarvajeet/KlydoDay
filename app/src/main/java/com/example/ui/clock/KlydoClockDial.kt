package com.example.ui.clock

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.data.model.CustomWatchFace
import com.example.data.model.KlydoArtCatalog
import com.example.data.model.KlydoArtMeta
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import com.example.ui.theme.NightAmber
import com.example.ui.theme.TerracottaAccent
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val CABINET_ASPECT_RATIO = 0.575f

fun parseHexColorOrDefault(hex: String, fallback: Color): Color = try {
    Color(hex.toColorInt())
} catch (_: Exception) {
    fallback
}

@Composable
fun KlydoClockCabinet(
    klydoId: String,
    pendulumStyle: PendulumStyle,
    pendulumEnabled: Boolean,
    showFrame: Boolean = true,
    blocksForDay: List<ScheduleBlock>,
    scrubOffsetMinutes: Float,
    isNightMode: Boolean,
    onCycleKlydo: () -> Unit,
    onScrubDelta: (Float) -> Unit,
    onSnapToNow: () -> Unit,
    modifier: Modifier = Modifier,
    artMeta: KlydoArtMeta = KlydoArtCatalog.findById(klydoId),
    customVideoFace: CustomWatchFace? = null,
    customVideoUri: Uri? = null
) {
    val baseWallMillis = remember { System.currentTimeMillis() }
    val phaseOffsetSec = remember(baseWallMillis) { (baseWallMillis % 2000L) / 1000.0 }
    val elapsedSecondsState = remember { mutableDoubleStateOf(0.0) }
    val reusableCal = remember { Calendar.getInstance() }

    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { nowNanos ->
                elapsedSecondsState.doubleValue = (nowNanos - startNanos) / 1_000_000_000.0
            }
        }
    }

    val useDarkHands = !isNightMode && !artMeta.isDarkFace
    val targetHandColor = if (isNightMode) {
        NightAmber
    } else {
        parseHexColorOrDefault(artMeta.handsColorHex, KlydoCyan)
    }
    val animatedHandColor by animateColorAsState(
        targetValue = targetHandColor,
        animationSpec = tween(durationMillis = 420),
        label = "clock_hands_color"
    )
    val syncedRodColor = parseHexColorOrDefault(artMeta.pendulumRodColorHex, KlydoCyan)
    val syncedBobColor = parseHexColorOrDefault(artMeta.pendulumBobColorHex, KlydoViolet)

    val elapsedSecondsProvider: () -> Double = remember {
        { elapsedSecondsState.doubleValue }
    }
    val phaseProvider: () -> Float = remember(phaseOffsetSec, scrubOffsetMinutes) {
        { (elapsedSecondsState.doubleValue + phaseOffsetSec).toFloat() + (scrubOffsetMinutes * 0.15f) }
    }
    val pendulumSecondsProvider: () -> Float = remember(phaseOffsetSec) {
        { (elapsedSecondsState.doubleValue + phaseOffsetSec).toFloat() }
    }
    val minuteOfDayProvider: () -> Float = remember(baseWallMillis, scrubOffsetMinutes) {
        {
            val effectiveMillis = baseWallMillis +
                (elapsedSecondsState.doubleValue * 1000.0).toLong() +
                (scrubOffsetMinutes * 60_000.0).toLong()
            reusableCal.timeInMillis = effectiveMillis
            val h = reusableCal.get(Calendar.HOUR_OF_DAY)
            val m = reusableCal.get(Calendar.MINUTE)
            val s = reusableCal.get(Calendar.SECOND)
            h * 60f + m + s / 60f
        }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (showFrame) {
            val shadowRoom = 14.dp
            val availableH = (maxHeight - shadowRoom).coerceAtLeast(140.dp)
            val cabW = minOf(maxWidth, availableH * CABINET_ASPECT_RATIO)
            val cabH = cabW / CABINET_ASPECT_RATIO

            val dialSize = cabW * 0.78f
            val pendulumSize = cabW * 0.41f

            Box(
                modifier = Modifier.size(width = cabW, height = cabH + shadowRoom),
                contentAlignment = Alignment.TopCenter
            ) {
                // Soft realistic grounding shadow directly beneath the standing clock cabinet
                Canvas(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(width = cabW * 0.94f, height = 22.dp)
                ) {
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xB8000000), Color(0x45000000), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width * 0.52f
                        )
                    )
                }

                // Layered Baltic-Birch Plywood & Matte Charcoal Cabinet Shell
                Box(
                    modifier = Modifier.size(width = cabW, height = cabH),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val outerCorner = w * 0.115f

                        // Layer 1: Outer warm Baltic birch ply veneer
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE7CA9F),
                                    Color(0xFFCBA36F),
                                    Color(0xFFDEC096),
                                    Color(0xFFBC925F)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(w, h)
                            ),
                            cornerRadius = CornerRadius(outerCorner, outerCorner)
                        )

                        // Layer 2: Dark wood lamination glue seam
                        val seam1 = w * 0.011f
                        drawRoundRect(
                            color = Color(0xFF5D4023),
                            topLeft = Offset(seam1, seam1),
                            size = Size(w - seam1 * 2f, h - seam1 * 2f),
                            cornerRadius = CornerRadius(outerCorner - seam1, outerCorner - seam1),
                            style = Stroke(width = w * 0.006f)
                        )

                        // Layer 3: Mid birch ply layer
                        val ply2 = w * 0.018f
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFDFC197), Color(0xFFC59B69)),
                                start = Offset(0f, 0f),
                                end = Offset(w, h)
                            ),
                            topLeft = Offset(ply2, ply2),
                            size = Size(w - ply2 * 2f, h - ply2 * 2f),
                            cornerRadius = CornerRadius(outerCorner - ply2, outerCorner - ply2),
                            style = Stroke(width = w * 0.011f)
                        )

                        // Layer 4: Inner lamination glue seam
                        val seam2 = w * 0.029f
                        drawRoundRect(
                            color = Color(0xFF473019),
                            topLeft = Offset(seam2, seam2),
                            size = Size(w - seam2 * 2f, h - seam2 * 2f),
                            cornerRadius = CornerRadius(outerCorner - seam2, outerCorner - seam2),
                            style = Stroke(width = w * 0.0055f)
                        )

                        // Layer 5: Inner birch trim
                        val ply3 = w * 0.036f
                        drawRoundRect(
                            color = Color(0xFFD8B78B),
                            topLeft = Offset(ply3, ply3),
                            size = Size(w - ply3 * 2f, h - ply3 * 2f),
                            cornerRadius = CornerRadius(outerCorner - ply3, outerCorner - ply3),
                            style = Stroke(width = w * 0.007f)
                        )

                        // Layer 6: Deep inner shadow groove
                        val bezelInset = w * 0.043f
                        drawRoundRect(
                            color = Color(0xFF07080A),
                            topLeft = Offset(bezelInset, bezelInset),
                            size = Size(w - bezelInset * 2f, h - bezelInset * 2f),
                            cornerRadius = CornerRadius(outerCorner - bezelInset, outerCorner - bezelInset)
                        )

                        // Matte Charcoal-Black Textured Faceplate
                        val faceInset = w * 0.047f
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1A1B20),
                                    Color(0xFF131418),
                                    Color(0xFF101114)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(w, h)
                            ),
                            topLeft = Offset(faceInset, faceInset),
                            size = Size(w - faceInset * 2f, h - faceInset * 2f),
                            cornerRadius = CornerRadius(outerCorner - faceInset, outerCorner - faceInset)
                        )
                    }

                    // Upper Circular Clock Screen + Lower Circular Pendulum Screen
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(cabH * 0.080f))

                        KlydoUpperDial(
                            klydoId = klydoId,
                            phaseProvider = phaseProvider,
                            minuteOfDayProvider = minuteOfDayProvider,
                            isNightMode = isNightMode,
                            blocksForDay = blocksForDay,
                            baseWallMillis = baseWallMillis,
                            elapsedSecondsProvider = elapsedSecondsProvider,
                            scrubOffsetMinutes = scrubOffsetMinutes,
                            reusableCal = reusableCal,
                            animatedHandColor = animatedHandColor,
                            useDarkHands = useDarkHands,
                            onCycleKlydo = onCycleKlydo,
                            customVideoFace = customVideoFace,
                            customVideoUri = customVideoUri,
                            modifier = Modifier.size(dialSize)
                        )

                        Spacer(modifier = Modifier.height(cabH * 0.115f))

                        KlydoPendulumAperture(
                            tSecondsProvider = pendulumSecondsProvider,
                            pendulumStyle = pendulumStyle,
                            syncedRodColor = syncedRodColor,
                            syncedBobColor = syncedBobColor,
                            pendulumEnabled = pendulumEnabled,
                            isNightMode = isNightMode,
                            scrubOffsetMinutes = scrubOffsetMinutes,
                            onScrubDelta = onScrubDelta,
                            onSnapToNow = onSnapToNow,
                            modifier = Modifier
                                .size(pendulumSize)
                                .offset(y = 0.dp)
                        )
                    }
                }
            }
        } else {
            // Frameless floating clock dial + optional pendulum
            val pendulumRatio = 0.525f
            val gapRatio = 0.09f
            val dialSize = if (pendulumEnabled) {
                minOf(maxWidth * 0.92f, maxHeight / (1f + gapRatio + pendulumRatio))
            } else {
                minOf(maxWidth, maxHeight) * 0.94f
            }.coerceAtLeast(100.dp)
            val pendulumSize = dialSize * pendulumRatio
            val gapSize = dialSize * gapRatio

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KlydoUpperDial(
                    klydoId = klydoId,
                    phaseProvider = phaseProvider,
                    minuteOfDayProvider = minuteOfDayProvider,
                    isNightMode = isNightMode,
                    blocksForDay = blocksForDay,
                    baseWallMillis = baseWallMillis,
                    elapsedSecondsProvider = elapsedSecondsProvider,
                    scrubOffsetMinutes = scrubOffsetMinutes,
                    reusableCal = reusableCal,
                    animatedHandColor = animatedHandColor,
                    useDarkHands = useDarkHands,
                    onCycleKlydo = onCycleKlydo,
                    customVideoFace = customVideoFace,
                    customVideoUri = customVideoUri,
                    modifier = Modifier.size(dialSize)
                )

                if (pendulumEnabled) {
                    Spacer(modifier = Modifier.height(gapSize))

                    KlydoPendulumAperture(
                        tSecondsProvider = pendulumSecondsProvider,
                        pendulumStyle = pendulumStyle,
                        syncedRodColor = syncedRodColor,
                        syncedBobColor = syncedBobColor,
                        pendulumEnabled = true,
                        isNightMode = isNightMode,
                        scrubOffsetMinutes = scrubOffsetMinutes,
                        onScrubDelta = onScrubDelta,
                        onSnapToNow = onSnapToNow,
                        modifier = Modifier.size(pendulumSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun KlydoUpperDial(
    klydoId: String,
    phaseProvider: () -> Float,
    minuteOfDayProvider: () -> Float,
    isNightMode: Boolean,
    blocksForDay: List<ScheduleBlock>,
    baseWallMillis: Long,
    elapsedSecondsProvider: () -> Double,
    scrubOffsetMinutes: Float,
    reusableCal: Calendar,
    animatedHandColor: Color,
    useDarkHands: Boolean,
    onCycleKlydo: () -> Unit,
    customVideoFace: CustomWatchFace?,
    customVideoUri: Uri?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onCycleKlydo() }
            .testTag("klydo_clock_dial"),
        contentAlignment = Alignment.Center
    ) {
        KlydoArtCanvas(
            klydoId = klydoId,
            phaseProvider = phaseProvider,
            minuteOfDayProvider = minuteOfDayProvider,
            isNightMode = isNightMode,
            customVideoFace = customVideoFace,
            customVideoUri = customVideoUri,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val effectiveMillis = baseWallMillis +
                (elapsedSecondsProvider() * 1000.0).toLong() +
                (scrubOffsetMinutes * 60_000.0).toLong()
            reusableCal.timeInMillis = effectiveMillis
            val hour24 = reusableCal.get(Calendar.HOUR_OF_DAY)
            val minute = reusableCal.get(Calendar.MINUTE)
            val second = reusableCal.get(Calendar.SECOND)
            val millis = reusableCal.get(Calendar.MILLISECOND)

            val exactMinuteOfDay = hour24 * 60f + minute + (second + millis / 1000f) / 60f
            val isPm = hour24 >= 12
            val windowStart = if (isPm) 720 else 0
            val windowEnd = if (isPm) 1440 else 720

            val cx = size.width / 2f
            val cy = size.height / 2f
            val rimRadius = size.minDimension * 0.478f
            val rimStroke = size.minDimension * 0.018f

            // Whisper-thin schedule arc indicators along the outer edge of the glass
            for (block in blocksForDay) {
                val bStart = block.startMinutes.toInt()
                val bEnd = bStart + block.durationMinutes.toInt()
                val overlapStart = maxOf(bStart, windowStart)
                val overlapEnd = minOf(bEnd, windowEnd)
                if (overlapEnd > overlapStart) {
                    val startDeg = ((overlapStart - windowStart) / 720f) * 360f - 90f
                    val sweepDeg = ((overlapEnd - overlapStart) / 720f) * 360f
                    val parsedColor = parseHexColorOrDefault(block.colorHex, TerracottaAccent)
                    val isActiveNow = exactMinuteOfDay >= bStart && exactMinuteOfDay < bEnd
                    val arcColor = if (isNightMode) {
                        NightAmber.copy(alpha = if (isActiveNow) 0.65f else 0.20f)
                    } else {
                        parsedColor.copy(alpha = if (isActiveNow) 0.92f else 0.45f)
                    }
                    drawArc(
                        color = arcColor,
                        startAngle = startDeg,
                        sweepAngle = sweepDeg.coerceAtLeast(3f),
                        useCenter = false,
                        topLeft = Offset(cx - rimRadius, cy - rimRadius),
                        size = Size(rimRadius * 2f, rimRadius * 2f),
                        style = Stroke(
                            width = if (isActiveNow) rimStroke * 1.3f else rimStroke,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // Subtle recessed circular glass bezel rim
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF060709),
                        Color(0xFF333842),
                        Color(0xFF14161B)
                    )
                ),
                radius = size.minDimension * 0.495f,
                center = Offset(cx, cy),
                style = Stroke(width = size.minDimension * 0.015f)
            )

            // Signature Klydoclock bold rounded hands synced to active face color
            val secValue = second + millis / 1000f
            val minValue = minute + secValue / 60f
            val hourValue = (hour24 % 12) + minValue / 60f

            val hourAngle = (hourValue * 30f - 90f) * PI.toFloat() / 180f
            val minAngle = (minValue * 6f - 90f) * PI.toFloat() / 180f

            val handRadius = size.minDimension * 0.44f
            val shadowColor = if (useDarkHands) {
                Color.White.copy(alpha = 0.25f)
            } else {
                Color.Black.copy(alpha = 0.50f)
            }

            val hourStart = Offset(
                cx - handRadius * 0.09f * cos(hourAngle),
                cy - handRadius * 0.09f * sin(hourAngle)
            )
            val hourEnd = Offset(
                cx + handRadius * 0.49f * cos(hourAngle),
                cy + handRadius * 0.49f * sin(hourAngle)
            )
            val minStart = Offset(
                cx - handRadius * 0.11f * cos(minAngle),
                cy - handRadius * 0.11f * sin(minAngle)
            )
            val minEnd = Offset(
                cx + handRadius * 0.78f * cos(minAngle),
                cy + handRadius * 0.78f * sin(minAngle)
            )

            drawLine(
                color = shadowColor,
                start = hourStart,
                end = hourEnd,
                strokeWidth = size.minDimension * 0.068f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = shadowColor,
                start = minStart,
                end = minEnd,
                strokeWidth = size.minDimension * 0.056f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = animatedHandColor,
                start = hourStart,
                end = hourEnd,
                strokeWidth = size.minDimension * 0.056f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = animatedHandColor,
                start = minStart,
                end = minEnd,
                strokeWidth = size.minDimension * 0.044f,
                cap = StrokeCap.Round
            )

            drawCircle(
                color = shadowColor,
                radius = size.minDimension * 0.048f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = animatedHandColor,
                radius = size.minDimension * 0.042f,
                center = Offset(cx, cy)
            )
        }
    }
}
