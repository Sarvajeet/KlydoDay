package com.example.ui.clock.faces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ui.theme.BotanicalSage
import com.example.ui.theme.CreamDial
import com.example.ui.theme.SaffronAmber
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
internal fun PrismJellyfishFace(phaseProvider: () -> Float) {
    val sharedPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = Color.Black)
        val framePhase = phaseProvider()
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val beat = framePhase * PI.toFloat()
        val propulsion = sin(beat)
        val bobY = h * 0.022f * propulsion
        val cy = h * 0.47f + bobY
        val bellW = w * (0.63f + 0.032f * cos(beat))
        val bellH = h * (0.28f - 0.018f * propulsion)

        rotate(degrees = -7f + 2.2f * sin(beat * 0.5f), pivot = Offset(cx, cy)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66B028FF), Color(0x3036C8FF), Color.Transparent),
                    center = Offset(cx, cy + bellH * 0.08f),
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = Offset(cx, cy + bellH * 0.08f)
            )

            for (u in 0 until 7) {
                val uNorm = (u - 3f) / 3f
                val startX = cx + uNorm * bellW * 0.25f
                val startY = cy - bellH * 0.42f
                val sway = sin(beat * 0.75f + u * 0.9f) * w * 0.045f
                val topLen = h * (0.27f + ((u * 3) % 4) * 0.045f)
                sharedPath.reset()
                sharedPath.moveTo(startX, startY)
                sharedPath.cubicTo(
                    startX + sway * 0.6f, startY - topLen * 0.35f,
                    startX - sway * 0.8f, startY - topLen * 0.7f,
                    startX + sway * 0.45f + uNorm * w * 0.04f, startY - topLen
                )
                drawPath(
                    path = sharedPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x88C77DFF), Color(0xCCFF7AD9)),
                        startY = startY - topLen,
                        endY = startY
                    ),
                    style = Stroke(width = w * 0.006f, cap = StrokeCap.Round)
                )
            }

            for (t in 0 until 16) {
                val frac = (t - 7.5f) / 7.5f
                val anchorX = cx + frac * bellW * 0.43f
                val anchorY = cy + bellH * 0.28f
                val tentacleLen = h * (0.39f + (1f - abs(frac)) * 0.13f)
                val phase = beat * 0.9f + t * 0.52f
                val wave1 = sin(phase) * w * 0.042f
                val wave2 = cos(phase * 0.85f) * w * 0.058f
                val tentacleColor = when (t % 4) {
                    0 -> Color(0xBB9D4EDD)
                    1 -> Color(0xAAE056FD)
                    2 -> Color(0x9948BFE3)
                    else -> Color(0xAAFF6EC7)
                }
                sharedPath.reset()
                sharedPath.moveTo(anchorX, anchorY)
                sharedPath.cubicTo(
                    anchorX + wave1, anchorY + tentacleLen * 0.33f,
                    anchorX + wave2 - frac * w * 0.02f, anchorY + tentacleLen * 0.68f,
                    anchorX + wave1 * 0.65f - frac * w * 0.05f, anchorY + tentacleLen
                )
                drawPath(
                    path = sharedPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(tentacleColor, tentacleColor.copy(alpha = 0.38f), Color.Transparent),
                        startY = anchorY,
                        endY = anchorY + tentacleLen
                    ),
                    style = Stroke(
                        width = w * (if (t % 3 == 0) 0.017f else 0.010f),
                        cap = StrokeCap.Round
                    )
                )
            }

            sharedPath.reset()
            sharedPath.moveTo(cx - bellW * 0.52f, cy + bellH * 0.24f)
            sharedPath.cubicTo(
                cx - bellW * 0.54f, cy - bellH * 0.45f,
                cx - bellW * 0.22f, cy - bellH * 0.64f,
                cx, cy - bellH * 0.62f
            )
            sharedPath.cubicTo(
                cx + bellW * 0.24f, cy - bellH * 0.64f,
                cx + bellW * 0.55f, cy - bellH * 0.42f,
                cx + bellW * 0.52f, cy + bellH * 0.24f
            )
            sharedPath.cubicTo(
                cx + bellW * 0.30f, cy + bellH * 0.42f,
                cx + bellW * 0.12f, cy + bellH * 0.22f,
                cx, cy + bellH * 0.36f
            )
            sharedPath.cubicTo(
                cx - bellW * 0.14f, cy + bellH * 0.22f,
                cx - bellW * 0.32f, cy + bellH * 0.42f,
                cx - bellW * 0.52f, cy + bellH * 0.24f
            )
            sharedPath.close()

            drawPath(
                path = sharedPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xF2FF8AD4),
                        Color(0xE0A832F5),
                        Color(0xD05B2EE5),
                        Color(0xB829A8FF)
                    ),
                    center = Offset(cx - bellW * 0.08f, cy - bellH * 0.14f),
                    radius = bellW * 0.62f
                )
            )
            drawPath(
                path = sharedPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x803CF6FF),
                        Color(0x28FF4EDB),
                        Color(0x707B2CBF),
                        Color(0x663CF6FF)
                    ),
                    start = Offset(cx - bellW * 0.45f, cy - bellH * 0.5f),
                    end = Offset(cx + bellW * 0.45f, cy + bellH * 0.3f)
                )
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xD8FFE6FF), Color(0x99E040FB), Color.Transparent),
                    center = Offset(cx, cy + bellH * 0.04f),
                    radius = bellW * 0.30f
                ),
                topLeft = Offset(cx - bellW * 0.24f, cy - bellH * 0.18f),
                size = Size(bellW * 0.48f, bellH * 0.50f)
            )

            for (s in 0 until 28) {
                val angle = s * 2.39996f
                val radial = sqrt((s + 1f) / 28f)
                val sx = cx + cos(angle) * bellW * 0.40f * radial
                val sy = cy - bellH * 0.10f + sin(angle) * bellH * 0.34f * radial
                val twinkle = 0.35f + 0.65f * ((sin(beat * 1.4f + s * 1.3f) + 1f) * 0.5f)
                val sparkColor = when (s % 4) {
                    0 -> Color(0xFF9DFBFF)
                    1 -> Color(0xFFFFB7F2)
                    2 -> Color(0xFFD580FF)
                    else -> Color.White
                }
                drawCircle(
                    color = sparkColor.copy(alpha = twinkle * 0.90f),
                    radius = w * (0.0055f + (s % 3) * 0.003f),
                    center = Offset(sx, sy)
                )
            }
            drawPath(
                path = sharedPath,
                color = Color(0x78B8F4FF),
                style = Stroke(width = w * 0.005f)
            )
        }
    }
}

@Composable
internal fun SunburstHorizonFace(
    phaseProvider: () -> Float,
    minuteOfDayProvider: () -> Float
) {
    val wavePath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val framePhase = phaseProvider()
        val minuteOfDay = minuteOfDayProvider()
        val dayFraction = (minuteOfDay / 1440f).coerceIn(0f, 1f)
        val beat = framePhase * PI.toFloat()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0D1426), Color(0xFF3A2D56), Color(0xFFD96F43), SaffronAmber)
            )
        )

        val cx = size.width / 2f
        val horizonY = size.height * 0.56f
        val sunOffset = sin(beat * 0.25f) * size.width * 0.07f
        val sunY = horizonY - size.height * (0.14f + 0.05f * cos(dayFraction * 2f * PI.toFloat()))
        val sunRadius = size.minDimension * (0.16f + 0.01f * sin(beat))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SaffronAmber.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(cx + sunOffset, sunY),
                radius = sunRadius * 2.1f
            ),
            radius = sunRadius * 2.1f,
            center = Offset(cx + sunOffset, sunY)
        )
        drawCircle(
            color = SaffronAmber,
            radius = sunRadius,
            center = Offset(cx + sunOffset, sunY)
        )

        for (i in 0 until 12) {
            val a = (i * 30f + framePhase * 16f) * PI.toFloat() / 180f
            val inner = sunRadius * 1.18f
            val outer = sunRadius * 1.62f
            drawLine(
                color = CreamDial.copy(alpha = 0.45f),
                start = Offset(cx + sunOffset + inner * cos(a), sunY + inner * sin(a)),
                end = Offset(cx + sunOffset + outer * cos(a), sunY + outer * sin(a)),
                strokeWidth = size.minDimension * 0.012f,
                cap = StrokeCap.Round
            )
        }

        for (layer in 0 until 4) {
            val baseY = horizonY + layer * size.height * 0.10f
            val waveColor = when (layer) {
                0 -> Color(0xFF1F4258)
                1 -> Color(0xFF183347)
                2 -> Color(0xFF112333)
                else -> Color.Black
            }
            wavePath.reset()
            wavePath.moveTo(0f, size.height)
            wavePath.lineTo(0f, baseY)
            val steps = 28
            for (s in 0..steps) {
                val x = (s.toFloat() / steps) * size.width
                val y = baseY + sin(s * 0.55f + beat * (0.6f + layer * 0.18f) + layer) * (size.height * 0.025f)
                wavePath.lineTo(x, y)
            }
            wavePath.lineTo(size.width, size.height)
            wavePath.close()
            drawPath(path = wavePath, color = waveColor)
        }
    }
}

@Composable
internal fun BotanicalPulseFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = Color.Black)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A352A), Color(0xFF0A1410), Color.Black),
                radius = size.minDimension * 0.6f
            )
        )
        val framePhase = phaseProvider()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val beat = framePhase * PI.toFloat()
        val baseRotation = (framePhase * 14f) % 360f
        val breathe = 1f + 0.08f * sin(beat)

        rotate(degrees = baseRotation, pivot = Offset(cx, cy)) {
            val petalCount = 12
            val orbit = size.minDimension * 0.25f * breathe
            for (i in 0 until petalCount) {
                val rad = (i * (360f / petalCount)) * PI.toFloat() / 180f
                val px = cx + orbit * cos(rad)
                val py = cy + orbit * sin(rad)
                val color = when (i % 3) {
                    0 -> BotanicalSage.copy(alpha = 0.80f)
                    1 -> Color(0xFF2E9F72).copy(alpha = 0.72f)
                    else -> Color(0xFFA8F0C6).copy(alpha = 0.55f)
                }
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.145f,
                    center = Offset(px, py)
                )
            }
        }
        drawCircle(
            color = Color(0xFFA8F0C6).copy(alpha = 0.25f),
            radius = size.minDimension * 0.18f * breathe,
            center = Offset(cx, cy)
        )
    }
}

@Composable
internal fun KoiSanctuaryFace(phaseProvider: () -> Float) {
    val petalPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        // Translucent deep jade pond gradient
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0D3835), Color(0xFF061E1E), Color(0xFF030E10)),
                center = Offset(cx, cy),
                radius = d * 0.58f
            )
        )

        // Subtle water ripple rings radiating softly
        for (i in 0 until 3) {
            val prog = ((t * 0.24f) + i * 0.333f) % 1f
            val alpha = sin(prog * PI.toFloat()).coerceIn(0f, 1f) * 0.26f
            drawCircle(
                color = Color(0xFF6FD4B8).copy(alpha = alpha),
                radius = d * (0.08f + prog * 0.38f),
                center = Offset(cx - d * 0.12f, cy + d * 0.08f),
                style = Stroke(width = d * 0.0045f)
            )
        }

        // Articulated swimming Koi 1: Kohaku (vermilion & white)
        drawArticulatedKoi(
            cx = cx,
            cy = cy,
            d = d,
            u = t * 0.42f,
            orbitScale = 0.26f,
            headColor = Color(0xFFFF653D),
            accentColor = Color(0xFFFFF3E2),
            finColor = Color(0xAAFFB59E),
            swimPhase = t * 5.2f
        )

        // Articulated swimming Koi 2: Yamabuki Ogon (warm gold) swimming opposite phase
        drawArticulatedKoi(
            cx = cx,
            cy = cy,
            d = d,
            u = t * 0.42f + PI.toFloat(),
            orbitScale = 0.22f,
            headColor = Color(0xFFFFBE44),
            accentColor = Color(0xFFFFEEB8),
            finColor = Color(0xAAFFE394),
            swimPhase = t * 5.6f + 1.5f
        )

        // Floating notched lily pads
        drawLilyPad(Offset(cx - d * 0.24f, cy - d * 0.20f), d * 0.105f, 35f, Color(0xFF1F6B50))
        drawLilyPad(Offset(cx + d * 0.25f, cy + d * 0.21f), d * 0.092f, 210f, Color(0xFF287D5F))
        drawLilyPad(Offset(cx - d * 0.18f, cy + d * 0.26f), d * 0.072f, 120f, Color(0xFF1A5A43))

        // Floating pale-pink lotus flower gently breathing
        val lotusCenter = Offset(cx + d * 0.23f, cy + d * 0.19f)
        val bloomR = d * (0.052f + 0.004f * sin(t * 1.4f))
        rotate(degrees = sin(t * 0.5f) * 6f, pivot = lotusCenter) {
            for (p in 0 until 8) {
                val a = p * 45f * PI.toFloat() / 180f
                val px = lotusCenter.x + bloomR * 0.55f * cos(a)
                val py = lotusCenter.y + bloomR * 0.55f * sin(a)
                petalPath.reset()
                petalPath.addOval(
                    androidx.compose.ui.geometry.Rect(
                        px - bloomR * 0.35f,
                        py - bloomR * 0.35f,
                        px + bloomR * 0.35f,
                        py + bloomR * 0.35f
                    )
                )
                drawPath(
                    path = petalPath,
                    color = if (p % 2 == 0) Color(0xFFF9B8C9) else Color(0xFFFDE2E8)
                )
            }
            drawCircle(color = Color(0xFFFFD166), radius = bloomR * 0.28f, center = lotusCenter)
        }
    }
}

private fun DrawScope.drawArticulatedKoi(
    cx: Float,
    cy: Float,
    d: Float,
    u: Float,
    orbitScale: Float,
    headColor: Color,
    accentColor: Color,
    finColor: Color,
    swimPhase: Float
) {
    val segCount = 9
    for (s in segCount - 1 downTo 0) {
        val lag = s * 0.145f
        val pathU = u - lag
        val px = cx + d * orbitScale * sin(pathU)
        val py = cy + d * (orbitScale * 0.72f) * sin(2f * pathU)
        val dx = cos(pathU)
        val dy = 1.44f * cos(2f * pathU)
        val heading = atan2(dy, dx)
        val sway = sin(swimPhase - s * 0.55f) * d * 0.008f * (s / segCount.toFloat())
        val perpX = -sin(heading) * sway
        val perpY = cos(heading) * sway

        val frac = s / (segCount - 1f)
        val segRadius = d * (0.036f * (1f - frac * 0.74f))
        val segPos = Offset(px + perpX, py + perpY)

        if (s == 2) {
            // Pectoral fins
            val finLen = d * 0.042f
            val finFlap = 0.25f * sin(swimPhase)
            drawLine(
                color = finColor,
                start = segPos,
                end = Offset(
                    segPos.x + finLen * cos(heading + 1.9f + finFlap),
                    segPos.y + finLen * sin(heading + 1.9f + finFlap)
                ),
                strokeWidth = d * 0.014f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = finColor,
                start = segPos,
                end = Offset(
                    segPos.x + finLen * cos(heading - 1.9f - finFlap),
                    segPos.y + finLen * sin(heading - 1.9f - finFlap)
                ),
                strokeWidth = d * 0.014f,
                cap = StrokeCap.Round
            )
        }
        val segColor = when {
            s == 0 -> headColor
            s % 3 == 1 -> accentColor
            else -> headColor
        }
        drawCircle(
            color = segColor,
            radius = if (s == segCount - 1) segRadius * 1.4f else segRadius,
            center = segPos
        )
    }
}

private fun DrawScope.drawLilyPad(center: Offset, radius: Float, notchAngle: Float, color: Color) {
    drawArc(
        color = color.copy(alpha = 0.88f),
        startAngle = notchAngle,
        sweepAngle = 326f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f)
    )
    drawArc(
        color = Color(0x44A8F0C6),
        startAngle = notchAngle,
        sweepAngle = 326f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.82f, center.y - radius * 0.82f),
        size = Size(radius * 1.64f, radius * 1.64f),
        style = Stroke(width = radius * 0.05f)
    )
}

@Composable
internal fun FireflyGladeFace(phaseProvider: () -> Float) {
    val treePath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0A1424), Color(0xFF102430), Color(0xFF091416))
            )
        )

        // Glowing warm crescent moon in upper right
        val moonCenter = Offset(w * 0.72f, h * 0.26f)
        val moonR = d * 0.085f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x40FFE89E), Color.Transparent),
                center = moonCenter,
                radius = moonR * 2.8f
            ),
            radius = moonR * 2.8f,
            center = moonCenter
        )
        drawCircle(color = Color(0xFFFFEFC4), radius = moonR, center = moonCenter)
        drawCircle(
            color = Color(0xFF0C1828),
            radius = moonR * 0.88f,
            center = Offset(moonCenter.x - moonR * 0.34f, moonCenter.y - moonR * 0.14f)
        )

        // Distant & near pine tree silhouettes
        treePath.reset()
        treePath.moveTo(0f, h)
        val pinePeaks = 7
        for (i in 0..pinePeaks) {
            val px = (i.toFloat() / pinePeaks) * w
            val peakH = h * (0.64f + (i % 3) * 0.055f)
            treePath.lineTo(px, peakH)
            if (i < pinePeaks) {
                treePath.lineTo(px + w / (pinePeaks * 2f), h * 0.80f)
            }
        }
        treePath.lineTo(w, h)
        treePath.close()
        drawPath(path = treePath, color = Color(0xFF050B0D))

        // 18 drifting bioluminescent fireflies with gentle glow cycles
        for (i in 0 until 18) {
            val baseX = w * (0.14f + ((i * 41) % 72) / 100f)
            val baseY = h * (0.24f + ((i * 59) % 58) / 100f)
            val fx = baseX + d * 0.055f * sin(t * (0.55f + (i % 4) * 0.12f) + i * 1.1f)
            val fy = baseY + d * 0.045f * cos(t * (0.48f + (i % 3) * 0.15f) + i * 1.7f)
            val glow = 0.20f + 0.80f * ((sin(t * 1.8f + i * 2.1f) + 1f) * 0.5f)
            val haloR = d * (0.036f + (i % 3) * 0.010f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD15C).copy(alpha = glow * 0.65f), Color.Transparent),
                    center = Offset(fx, fy),
                    radius = haloR
                ),
                radius = haloR,
                center = Offset(fx, fy)
            )
            drawCircle(
                color = Color(0xFFFFF4C2).copy(alpha = 0.4f + glow * 0.6f),
                radius = d * 0.0075f,
                center = Offset(fx, fy)
            )
        }
    }
}

@Composable
internal fun TidalNocturneFace(phaseProvider: () -> Float) {
    val wavePath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF060E22), Color(0xFF0E1F40), Color(0xFF081426))
            )
        )

        // Full pearl moon high over the sea
        val moonY = h * 0.25f
        val moonR = d * 0.095f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x50F2D59B), Color(0x1878C2D4), Color.Transparent),
                center = Offset(cx, moonY),
                radius = moonR * 3.0f
            ),
            radius = moonR * 3.0f,
            center = Offset(cx, moonY)
        )
        drawCircle(color = Color(0xFFFAF0D7), radius = moonR, center = Offset(cx, moonY))

        val horizonY = h * 0.49f
        // Shimmering broken moonlight ribbons on the water
        for (r in 0 until 8) {
            val y = horizonY + d * (0.025f + r * 0.042f)
            val halfSpan = d * (0.07f - r * 0.005f) * (0.75f + 0.35f * sin(t * 2.2f + r * 1.3f))
            val drift = d * 0.015f * sin(t * 1.4f + r)
            drawLine(
                color = Color(0x88F2D59B),
                start = Offset(cx - halfSpan + drift, y),
                end = Offset(cx + halfSpan + drift, y),
                strokeWidth = d * 0.006f,
                cap = StrokeCap.Round
            )
        }

        // Five rolling twilight ocean swells with bioluminescent foam crests
        for (layer in 0 until 5) {
            val baseY = horizonY + layer * h * 0.088f
            val waveColor = when (layer) {
                0 -> Color(0xCC112A48)
                1 -> Color(0xDD0D223A)
                2 -> Color(0xEE091A2E)
                3 -> Color(0xFF061222)
                else -> Color(0xFF030912)
            }
            wavePath.reset()
            wavePath.moveTo(0f, h)
            val steps = 32
            for (s in 0..steps) {
                val frac = s.toFloat() / steps
                val x = frac * w
                val crest = sin(frac * 7.5f + t * (0.85f + layer * 0.16f) + layer * 1.4f) * (h * 0.024f)
                val y = baseY + crest
                if (s == 0) wavePath.lineTo(0f, y) else wavePath.lineTo(x, y)
            }
            wavePath.lineTo(w, h)
            wavePath.close()
            drawPath(path = wavePath, color = waveColor)

            // Bioluminescent cyan crest glow along the top edge of mid swells
            if (layer in 1..3) {
                for (s in 0..steps step 2) {
                    val frac = s.toFloat() / steps
                    val x = frac * w
                    val y = baseY + sin(frac * 7.5f + t * (0.85f + layer * 0.16f) + layer * 1.4f) * (h * 0.024f)
                    val sparkle = 0.25f + 0.75f * ((sin(t * 2.6f + s + layer) + 1f) * 0.5f)
                    drawCircle(
                        color = Color(0xFF78E2F4).copy(alpha = sparkle * 0.55f),
                        radius = d * 0.0055f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
