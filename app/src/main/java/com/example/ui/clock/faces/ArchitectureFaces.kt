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
import com.example.ui.theme.CreamDial
import com.example.ui.theme.SaffronAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun CelestialObservatoryFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = Color.Black)
        val framePhase = phaseProvider()
        val w = size.width
        val cx = w / 2f
        val cy = size.height / 2f
        val beat = framePhase * PI.toFloat()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF14214D), Color(0xFF070B1C), Color.Black),
                center = Offset(cx, cy),
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = Offset(cx, cy)
        )

        for (i in 0 until 24) {
            val angle = i * 2.39996f + 0.4f
            val dist = w * (0.14f + (i % 6) * 0.056f)
            val pulse = 0.30f + 0.70f * ((sin(beat * 1.1f + i * 1.7f) + 1f) * 0.5f)
            drawCircle(
                color = (if (i % 5 == 0) SaffronAmber else CreamDial).copy(alpha = pulse),
                radius = w * (0.004f + (i % 3) * 0.0025f),
                center = Offset(cx + dist * cos(angle), cy + dist * sin(angle))
            )
        }

        val orbitR1 = w * 0.39f
        rotate(degrees = (framePhase * 15f) % 360f, pivot = Offset(cx, cy)) {
            drawCircle(
                color = SaffronAmber.copy(alpha = 0.25f),
                radius = orbitR1,
                center = Offset(cx, cy),
                style = Stroke(width = w * 0.006f)
            )
            drawArc(
                color = SaffronAmber,
                startAngle = 0f,
                sweepAngle = 85f,
                useCenter = false,
                topLeft = Offset(cx - orbitR1, cy - orbitR1),
                size = Size(orbitR1 * 2f, orbitR1 * 2f),
                style = Stroke(width = w * 0.016f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = SaffronAmber,
                radius = w * 0.025f,
                center = Offset(cx + orbitR1, cy)
            )
        }

        val orbitR2 = w * 0.26f
        rotate(degrees = (-framePhase * 28f) % 360f, pivot = Offset(cx, cy)) {
            drawCircle(
                color = Color(0xFF3E6BFF).copy(alpha = 0.35f),
                radius = orbitR2,
                center = Offset(cx, cy),
                style = Stroke(width = w * 0.006f)
            )
            drawArc(
                color = Color(0xFF5D87FF),
                startAngle = 140f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(cx - orbitR2, cy - orbitR2),
                size = Size(orbitR2 * 2f, orbitR2 * 2f),
                style = Stroke(width = w * 0.014f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = Color(0xFF5D87FF),
                radius = w * 0.028f,
                center = Offset(cx - orbitR2, cy)
            )
        }
    }
}

@Composable
internal fun KineticMonolithFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0D1119), Color(0xFF080A0F)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        drawCircle(
            color = Color(0x1A8FAEC4),
            radius = d * 0.39f,
            center = Offset(cx, cy),
            style = Stroke(width = d * 0.0045f)
        )
        drawCircle(
            color = Color(0x14F2E9DC),
            radius = d * 0.22f,
            center = Offset(cx, cy),
            style = Stroke(width = d * 0.0045f)
        )

        val angle1 = (t * 0.38f + 0.45f * sin(t * 0.618f))
        val span1 = d * 0.34f
        val p1A = Offset(cx + span1 * cos(angle1), cy + span1 * sin(angle1))
        val p1B = Offset(cx - span1 * 0.62f * cos(angle1), cy - span1 * 0.62f * sin(angle1))

        drawLine(
            color = Color(0x708FAEC4),
            start = p1A,
            end = p1B,
            strokeWidth = d * 0.0055f,
            cap = StrokeCap.Round
        )
        drawCircle(color = Color(0xFFD98A4E), radius = d * 0.044f, center = p1A)
        drawCircle(color = Color(0xFFF2E9DC), radius = d * 0.022f, center = p1B)

        val angle2 = -t * 0.55f + 0.6f * cos(t * 0.85f)
        val span2 = d * 0.16f
        val p2A = Offset(p1B.x + span2 * cos(angle2), p1B.y + span2 * sin(angle2))
        val p2B = Offset(p1B.x - span2 * 0.85f * cos(angle2), p1B.y - span2 * 0.85f * sin(angle2))
        drawLine(
            color = Color(0x55F2E9DC),
            start = p2A,
            end = p2B,
            strokeWidth = d * 0.0045f,
            cap = StrokeCap.Round
        )
        drawCircle(color = Color(0xFF8FAEC4), radius = d * 0.026f, center = p2A)
        drawCircle(color = Color(0xAAFFFFFF), radius = d * 0.011f, center = p2B)
    }
}

@Composable
internal fun GothicRoseFace(phaseProvider: () -> Float) {
    val lancetPath = remember { Path() }
    val beamPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(color = Color(0xFF0A0B10))

        // Luminous cathedral stained-glass ambient glow shifting softly
        val glowPulse = 0.78f + 0.22f * sin(t * 0.9f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE8B84E).copy(alpha = 0.42f * glowPulse),
                    Color(0xFFD93B58).copy(alpha = 0.28f * glowPulse),
                    Color(0xFF2855C8).copy(alpha = 0.22f * glowPulse),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = d * 0.48f
            ),
            radius = d * 0.48f,
            center = Offset(cx, cy)
        )

        // 12 stained glass radial lancet petals
        val jewelColors = listOf(
            Color(0xFFD93B58), // Ruby
            Color(0xFF2E66E8), // Chartres Sapphire
            Color(0xFFE8B84E), // Cathedral Amber
            Color(0xFF239E7C)  // Emerald
        )
        val slowRot = (t * 4f) % 360f
        rotate(degrees = slowRot, pivot = Offset(cx, cy)) {
            val petalCount = 12
            val rIn = d * 0.16f
            val rMid = d * 0.34f
            val rOut = d * 0.435f
            for (i in 0 until petalCount) {
                val a0 = (i * 30f - 11f) * PI.toFloat() / 180f
                val aCenter = (i * 30f) * PI.toFloat() / 180f
                val a1 = (i * 30f + 11f) * PI.toFloat() / 180f

                lancetPath.reset()
                lancetPath.moveTo(cx + rIn * cos(aCenter), cy + rIn * sin(aCenter))
                lancetPath.lineTo(cx + rMid * cos(a0), cy + rMid * sin(a0))
                lancetPath.quadraticTo(
                    cx + rOut * cos(aCenter),
                    cy + rOut * sin(aCenter),
                    cx + rMid * cos(a1),
                    cy + rMid * sin(a1)
                )
                lancetPath.close()

                val jewel = jewelColors[i % jewelColors.size]
                val shimmer = 0.68f + 0.32f * ((sin(t * 1.4f + i * 0.55f) + 1f) * 0.5f)
                drawPath(
                    path = lancetPath,
                    color = jewel.copy(alpha = shimmer * 0.78f)
                )
                drawPath(
                    path = lancetPath,
                    color = Color(0xFF15171F),
                    style = Stroke(width = d * 0.011f)
                )

                // Outer trefoil oculus jewel
                val trefoilC = Offset(cx + d * 0.405f * cos(aCenter), cy + d * 0.405f * sin(aCenter))
                drawCircle(
                    color = jewelColors[(i + 2) % jewelColors.size].copy(alpha = shimmer * 0.85f),
                    radius = d * 0.032f,
                    center = trefoilC
                )
                drawCircle(
                    color = Color(0xFF14161E),
                    radius = d * 0.032f,
                    center = trefoilC,
                    style = Stroke(width = d * 0.008f)
                )
            }
        }

        // Stone tracery rings
        drawCircle(
            color = Color(0xFF161820),
            radius = d * 0.445f,
            center = Offset(cx, cy),
            style = Stroke(width = d * 0.020f)
        )
        drawCircle(
            color = Color(0xFF181A22),
            radius = d * 0.165f,
            center = Offset(cx, cy),
            style = Stroke(width = d * 0.014f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF1D0), Color(0xFFE8B84E), Color(0xAA2E66E8)),
                center = Offset(cx, cy),
                radius = d * 0.14f
            ),
            radius = d * 0.14f,
            center = Offset(cx, cy)
        )

        // Volumetric sunbeam sweeping gracefully across the stained glass
        val beamAngle = (t * 12f) % 360f
        rotate(degrees = beamAngle, pivot = Offset(cx, cy)) {
            beamPath.reset()
            beamPath.moveTo(cx, cy)
            beamPath.lineTo(cx - d * 0.14f, cy - d * 0.52f)
            beamPath.lineTo(cx + d * 0.14f, cy - d * 0.52f)
            beamPath.close()
            drawPath(
                path = beamPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x28FFF1D0), Color.Transparent),
                    startY = cy - d * 0.52f,
                    endY = cy
                )
            )
        }
    }
}

@Composable
internal fun ChryslerBrassFace(phaseProvider: () -> Float) {
    val archPath = remember { Path() }
    val beamPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0A0D16), Color(0xFF101522), Color(0xFF07080C))
            )
        )

        // Sweeping twin Art Deco searchlight beams behind the spire
        for (b in 0..1) {
            val dir = if (b == 0) 1f else -1f
            val sweep = sin(t * 0.65f + b * 1.4f) * 32f * dir
            rotate(degrees = sweep, pivot = Offset(cx, h * 0.92f)) {
                beamPath.reset()
                beamPath.moveTo(cx, h * 0.92f)
                beamPath.lineTo(cx - d * 0.15f, h * 0.02f)
                beamPath.lineTo(cx + d * 0.15f, h * 0.02f)
                beamPath.close()
                drawPath(
                    path = beamPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x04FBE3A4), Color(0x28FBE3A4)),
                        startY = h * 0.02f,
                        endY = h * 0.92f
                    )
                )
            }
        }

        // Gilded Art Deco sunburst fan rays
        for (i in 0 until 13) {
            val angle = (-180f + i * 15f) * PI.toFloat() / 180f
            val rIn = d * 0.16f
            val rOut = d * 0.45f
            val shimmer = 0.25f + 0.35f * ((sin(t * 1.5f + i * 0.5f) + 1f) * 0.5f)
            drawLine(
                color = Color(0xFFD4A853).copy(alpha = shimmer),
                start = Offset(cx + rIn * cos(angle), cy + d * 0.14f + rIn * sin(angle)),
                end = Offset(cx + rOut * cos(angle), cy + d * 0.14f + rOut * sin(angle)),
                strokeWidth = d * 0.006f
            )
        }

        // Stepped Chrysler terraced brass arches
        val tiers = 5
        for (tier in 0 until tiers) {
            val archR = d * (0.37f - tier * 0.058f)
            val archBaseY = cy + d * (0.24f - tier * 0.036f)
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFBE3A4), Color(0xFFC59338), Color(0xFFFBE3A4))
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - archR, archBaseY - archR),
                size = Size(archR * 2f, archR * 2f),
                style = Stroke(width = d * 0.015f)
            )
            // Triangular sunburst window niches along each brass arch
            val niches = 7 - tier
            for (n in 1 until niches) {
                val frac = n.toFloat() / niches
                val angle = (180f + frac * 180f) * PI.toFloat() / 180f
                val nx = cx + archR * 0.88f * cos(angle)
                val ny = archBaseY + archR * 0.88f * sin(angle)
                drawCircle(
                    color = if ((n + tier) % 2 == 0) Color(0xFF45B8AC) else Color(0xFFFBE3A4),
                    radius = d * 0.010f,
                    center = Offset(nx, ny)
                )
            }
        }

        // Central Art Deco needle spire
        archPath.reset()
        archPath.moveTo(cx, cy - d * 0.34f)
        archPath.lineTo(cx - d * 0.038f, cy + d * 0.25f)
        archPath.lineTo(cx + d * 0.038f, cy + d * 0.25f)
        archPath.close()
        drawPath(
            path = archPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFBE3A4), Color(0xFFB6842D)),
                start = Offset(cx - d * 0.04f, cy),
                end = Offset(cx + d * 0.04f, cy)
            )
        )
    }
}

@Composable
internal fun IsometricAtriumFace(phaseProvider: () -> Float) {
    val facePath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.56f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF10151E), Color(0xFF080B10)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Precision architectural drafting grid rings
        drawCircle(
            color = Color(0x1C7CA1BD),
            radius = d * 0.41f,
            center = Offset(cx, h * 0.5f),
            style = Stroke(width = d * 0.004f)
        )

        // 7 kinetic limestone isometric monolith pillars breathing smoothly in elevation
        val coords = listOf(
            -1.2f to -1.1f,
            0.0f to -1.4f,
            1.2f to -1.1f,
            -1.0f to 0.0f,
            1.0f to 0.0f,
            0.0f to 0.2f,
            0.0f to 1.25f
        )
        val isoX = d * 0.135f
        val isoY = d * 0.072f
        val pillarRadius = d * 0.072f

        for (i in coords.indices) {
            val (gx, gy) = coords[i]
            val px = cx + (gx - gy) * isoX
            val py = cy + (gx + gy) * isoY
            val lift = d * (0.14f + 0.065f * sin(t * 1.15f + i * 0.85f))
            drawIsoPillar(
                path = facePath,
                baseCenter = Offset(px, py),
                halfW = pillarRadius,
                halfD = pillarRadius * 0.56f,
                height = lift,
                topColor = Color(0xFFFAF3E8),
                leftColor = Color(0xFF7CA1BD),
                rightColor = Color(0xFF394B5E),
                accentRim = Color(0xFFE57A54)
            )
        }
    }
}

private fun DrawScope.drawIsoPillar(
    path: Path,
    baseCenter: Offset,
    halfW: Float,
    halfD: Float,
    height: Float,
    topColor: Color,
    leftColor: Color,
    rightColor: Color,
    accentRim: Color
) {
    val topCenter = Offset(baseCenter.x, baseCenter.y - height)
    // Left shaded face
    path.reset()
    path.moveTo(topCenter.x - halfW, topCenter.y)
    path.lineTo(topCenter.x, topCenter.y + halfD)
    path.lineTo(baseCenter.x, baseCenter.y + halfD)
    path.lineTo(baseCenter.x - halfW, baseCenter.y)
    path.close()
    drawPath(path = path, color = leftColor)

    // Right deep face
    path.reset()
    path.moveTo(topCenter.x, topCenter.y + halfD)
    path.lineTo(topCenter.x + halfW, topCenter.y)
    path.lineTo(baseCenter.x + halfW, baseCenter.y)
    path.lineTo(baseCenter.x, baseCenter.y + halfD)
    path.close()
    drawPath(path = path, color = rightColor)

    // Illuminated top diamond plane
    path.reset()
    path.moveTo(topCenter.x, topCenter.y - halfD)
    path.lineTo(topCenter.x + halfW, topCenter.y)
    path.lineTo(topCenter.x, topCenter.y + halfD)
    path.lineTo(topCenter.x - halfW, topCenter.y)
    path.close()
    drawPath(path = path, color = topColor)
    drawPath(path = path, color = accentRim.copy(alpha = 0.45f), style = Stroke(width = halfW * 0.055f))
}
