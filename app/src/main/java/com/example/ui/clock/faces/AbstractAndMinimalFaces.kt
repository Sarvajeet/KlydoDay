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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ui.theme.CelestialTeal
import com.example.ui.theme.CreamDial
import com.example.ui.theme.IndigoOrbit
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun BauhausKineticFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = Color.Black)
        val framePhase = phaseProvider()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val beat = framePhase * PI.toFloat()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1D2230), Color(0xFF0A0B0F), Color.Black),
                center = Offset(cx, cy),
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = Offset(cx, cy)
        )

        val rOuter = w * 0.38f
        rotate(degrees = (framePhase * 18f) % 360f, pivot = Offset(cx, cy)) {
            drawArc(
                color = TerracottaAccent,
                startAngle = -35f,
                sweepAngle = 145f,
                useCenter = false,
                topLeft = Offset(cx - rOuter, cy - rOuter),
                size = Size(rOuter * 2f, rOuter * 2f),
                style = Stroke(width = w * 0.065f, cap = StrokeCap.Butt)
            )
            drawArc(
                color = SaffronAmber,
                startAngle = 155f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(cx - rOuter, cy - rOuter),
                size = Size(rOuter * 2f, rOuter * 2f),
                style = Stroke(width = w * 0.065f, cap = StrokeCap.Butt)
            )
        }

        val rMid = w * 0.25f
        rotate(degrees = (-framePhase * 27f) % 360f, pivot = Offset(cx, cy)) {
            drawArc(
                color = Color(0xFF3E7BFA),
                startAngle = 15f,
                sweepAngle = 165f,
                useCenter = false,
                topLeft = Offset(cx - rMid, cy - rMid),
                size = Size(rMid * 2f, rMid * 2f),
                style = Stroke(width = w * 0.048f, cap = StrokeCap.Round)
            )
            drawArc(
                color = CreamDial.copy(alpha = 0.88f),
                startAngle = 205f,
                sweepAngle = 125f,
                useCenter = false,
                topLeft = Offset(cx - rMid, cy - rMid),
                size = Size(rMid * 2f, rMid * 2f),
                style = Stroke(width = w * 0.032f, cap = StrokeCap.Round)
            )
        }

        val orb1Angle = beat * 0.5f
        val orb2Angle = -beat * 0.75f + PI.toFloat()
        drawCircle(
            color = SaffronAmber,
            radius = w * (0.046f + 0.006f * sin(beat)),
            center = Offset(cx + rOuter * cos(orb1Angle), cy + rOuter * sin(orb1Angle))
        )
        drawCircle(
            color = TerracottaAccent,
            radius = w * 0.034f,
            center = Offset(cx + rMid * cos(orb2Angle), cy + rMid * sin(orb2Angle))
        )

        drawLine(
            color = CreamDial.copy(alpha = 0.16f),
            start = Offset(cx, h * 0.08f),
            end = Offset(cx, h * 0.92f),
            strokeWidth = w * 0.004f
        )
        drawLine(
            color = CreamDial.copy(alpha = 0.16f),
            start = Offset(w * 0.08f, cy),
            end = Offset(w * 0.92f, cy),
            strokeWidth = w * 0.004f
        )
    }
}

@Composable
internal fun RetroTelemetryFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF090B16), Color(0xFF1A1D3B), Color.Black)
            )
        )
        val framePhase = phaseProvider()
        val horizonY = size.height * 0.56f
        val cx = size.width / 2f
        val beat = framePhase * PI.toFloat()
        val pulseR = size.minDimension * (0.22f + 0.012f * sin(beat))

        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF47B8), SaffronAmber, Color(0x00E040FB))
            ),
            radius = pulseR,
            center = Offset(cx, horizonY - size.minDimension * 0.04f)
        )

        val lines = 10
        for (i in -lines..lines) {
            val topX = cx + i * (size.width * 0.03f)
            val botX = cx + i * (size.width * 0.14f)
            drawLine(
                color = IndigoOrbit.copy(alpha = 0.62f),
                start = Offset(topX, horizonY),
                end = Offset(botX, size.height),
                strokeWidth = size.minDimension * 0.007f
            )
        }
        val phaseShift = (framePhase * 0.5f) % 1f
        for (row in 0 until 7) {
            val t = (row + phaseShift) / 7f
            val y = horizonY + (t * t) * (size.height - horizonY)
            drawLine(
                color = CelestialTeal.copy(alpha = 0.55f * t + 0.18f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = size.minDimension * 0.008f
            )
        }
    }
}

@Composable
internal fun HarmonicSilkFace(phaseProvider: () -> Float) {
    val curvePath = remember { Path() }
    val secondaryPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF14151B), Color(0xFF08090C), Color.Black),
                center = Offset(cx, cy),
                radius = d * 0.55f
            )
        )

        for (i in 0 until 4) {
            val angle = i * PI.toFloat() * 0.5f
            val rIn = d * 0.43f
            val rOut = d * 0.468f
            drawLine(
                color = Color(0x44F4EDE2),
                start = Offset(cx + rIn * cos(angle), cy + rIn * sin(angle)),
                end = Offset(cx + rOut * cos(angle), cy + rOut * sin(angle)),
                strokeWidth = d * 0.005f,
                cap = StrokeCap.Round
            )
        }

        drawCircle(
            color = Color(0x1AD6C3A5),
            radius = d * 0.415f,
            center = Offset(cx, cy),
            style = Stroke(width = d * 0.004f)
        )

        val delta = t * 0.34f
        val amp = d * (0.31f + 0.015f * sin(t * 0.7f))
        val steps = 96
        curvePath.reset()
        secondaryPath.reset()
        for (i in 0..steps) {
            val u = (i.toFloat() / steps) * 2f * PI.toFloat()
            val x1 = cx + amp * sin(3f * u + delta)
            val y1 = cy + amp * sin(2f * u)
            val x2 = cx + amp * 0.84f * sin(2f * u - delta * 0.7f)
            val y2 = cy + amp * 0.84f * sin(3f * u + 0.6f)
            if (i == 0) {
                curvePath.moveTo(x1, y1)
                secondaryPath.moveTo(x2, y2)
            } else {
                curvePath.lineTo(x1, y1)
                secondaryPath.lineTo(x2, y2)
            }
        }
        curvePath.close()
        secondaryPath.close()

        drawPath(
            path = secondaryPath,
            color = Color(0x28D6C3A5),
            style = Stroke(width = d * 0.006f, cap = StrokeCap.Round)
        )
        drawPath(
            path = curvePath,
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0x38E2A898),
                    Color(0x48F4EDE2),
                    Color(0x38D6C3A5),
                    Color(0x38E2A898)
                ),
                center = Offset(cx, cy)
            ),
            style = Stroke(width = d * 0.022f, cap = StrokeCap.Round)
        )
        drawPath(
            path = curvePath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xCCF4EDE2), Color(0xCCE2A898), Color(0xCCD6C3A5))
            ),
            style = Stroke(width = d * 0.007f, cap = StrokeCap.Round)
        )

        val nodeU = (t * 0.55f) % (2f * PI.toFloat())
        val nodeX = cx + amp * sin(3f * nodeU + delta)
        val nodeY = cy + amp * sin(2f * nodeU)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x88E2A898), Color.Transparent),
                center = Offset(nodeX, nodeY),
                radius = d * 0.065f
            ),
            radius = d * 0.065f,
            center = Offset(nodeX, nodeY)
        )
        drawCircle(
            color = Color(0xFFF4EDE2),
            radius = d * 0.015f,
            center = Offset(nodeX, nodeY)
        )
    }
}

@Composable
internal fun AuroraBorealisFace(phaseProvider: () -> Float) {
    val ribbonPath = remember { Path() }
    val ridgePath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF040714), Color(0xFF0A1428), Color(0xFF0D2330))
            )
        )

        // Sub-arctic stars
        for (i in 0 until 22) {
            val sx = w * (0.10f + ((i * 37) % 80) / 100f)
            val sy = h * (0.08f + ((i * 53) % 52) / 100f)
            val twinkle = 0.25f + 0.75f * ((sin(t * 1.4f + i * 1.9f) + 1f) * 0.5f)
            drawCircle(
                color = Color(0xFFE6FAFF).copy(alpha = twinkle * 0.78f),
                radius = d * (0.0032f + (i % 3) * 0.0018f),
                center = Offset(sx, sy)
            )
        }

        // Three undulating luminous aurora curtains
        val curtainConfigs = 3
        for (c in 0 until curtainConfigs) {
            val baseY = h * (0.34f + c * 0.095f)
            val ribbonH = h * (0.24f - c * 0.025f)
            val steps = 28
            ribbonPath.reset()
            for (s in 0..steps) {
                val frac = s.toFloat() / steps
                val x = frac * w
                val wave = sin(frac * 5.2f + t * (0.75f + c * 0.22f) + c * 1.8f) * h * 0.055f +
                    cos(frac * 8.4f - t * 0.5f + c) * h * 0.022f
                val yTop = baseY + wave - ribbonH
                if (s == 0) ribbonPath.moveTo(x, yTop) else ribbonPath.lineTo(x, yTop)
            }
            for (s in steps downTo 0) {
                val frac = s.toFloat() / steps
                val x = frac * w
                val wave = sin(frac * 5.2f + t * (0.75f + c * 0.22f) + c * 1.8f) * h * 0.055f +
                    cos(frac * 8.4f - t * 0.5f + c) * h * 0.022f
                val yBot = baseY + wave + h * 0.04f
                ribbonPath.lineTo(x, yBot)
            }
            ribbonPath.close()

            val curtainColors = when (c) {
                0 -> listOf(Color.Transparent, Color(0x559D6BFF), Color(0x8838F8C6), Color(0x2270E1F5))
                1 -> listOf(Color.Transparent, Color(0x5570E1F5), Color(0x992AF598), Color(0x182AF598))
                else -> listOf(Color.Transparent, Color(0x44A855F7), Color(0x885EEAD4), Color(0x18FFFFFF))
            }
            drawPath(
                path = ribbonPath,
                brush = Brush.verticalGradient(
                    colors = curtainColors,
                    startY = baseY - ribbonH,
                    endY = baseY + h * 0.05f
                )
            )
        }

        // Sharp dark Nordic mountain silhouette grounding the horizon
        ridgePath.reset()
        ridgePath.moveTo(0f, h)
        ridgePath.lineTo(0f, h * 0.78f)
        ridgePath.lineTo(w * 0.16f, h * 0.69f)
        ridgePath.lineTo(w * 0.30f, h * 0.76f)
        ridgePath.lineTo(w * 0.48f, h * 0.65f)
        ridgePath.lineTo(w * 0.68f, h * 0.75f)
        ridgePath.lineTo(w * 0.84f, h * 0.68f)
        ridgePath.lineTo(w, h * 0.77f)
        ridgePath.lineTo(w, h)
        ridgePath.close()
        drawPath(path = ridgePath, color = Color(0xFF04060C))
    }
}

@Composable
internal fun PrismRefractionFace(phaseProvider: () -> Float) {
    val prismPath = remember { Path() }
    val beamPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF141824), Color(0xFF07080D), Color.Black),
                center = Offset(cx, cy),
                radius = d * 0.55f
            )
        )

        val prismRot = (t * 8f) % 360f
        val prismR = d * 0.21f
        val breathAngle = 0.08f * sin(t * 1.1f)

        // Incoming crisp white light ray
        val entryPoint = Offset(cx - prismR * 0.36f, cy - prismR * 0.14f)
        val sourcePoint = Offset(0f, cy - d * 0.22f + d * 0.04f * sin(t * 0.8f))
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0x11FFFFFF), Color(0xEEFFFFFF)),
                start = sourcePoint,
                end = entryPoint
            ),
            start = sourcePoint,
            end = entryPoint,
            strokeWidth = d * 0.018f,
            cap = StrokeCap.Round
        )

        // Dispersed spectral rainbow rays fanning from right prism face
        val spectrum = listOf(
            Color(0xFFFF4D6D),
            Color(0xFFFF9E3D),
            Color(0xFFFFE066),
            Color(0xFF38F2A6),
            Color(0xFF38D4FF),
            Color(0xFFA855F7)
        )
        val exitOrigin = Offset(cx + prismR * 0.32f, cy + prismR * 0.06f)
        for (i in spectrum.indices) {
            val spread = (i - 2.5f) * (0.095f + breathAngle * 0.35f) + 0.16f
            val endX = exitOrigin.x + d * 0.65f * cos(spread)
            val endY = exitOrigin.y + d * 0.65f * sin(spread)
            val endTopY = endY - d * 0.028f
            val endBotY = endY + d * 0.028f

            beamPath.reset()
            beamPath.moveTo(exitOrigin.x, exitOrigin.y - d * 0.006f)
            beamPath.lineTo(endX, endTopY)
            beamPath.lineTo(endX, endBotY)
            beamPath.lineTo(exitOrigin.x, exitOrigin.y + d * 0.006f)
            beamPath.close()

            drawPath(
                path = beamPath,
                brush = Brush.linearGradient(
                    colors = listOf(spectrum[i].copy(alpha = 0.78f), spectrum[i].copy(alpha = 0.04f)),
                    start = exitOrigin,
                    end = Offset(endX, endY)
                )
            )
        }

        // Rotating faceted crystal prism
        rotate(degrees = prismRot, pivot = Offset(cx, cy)) {
            prismPath.reset()
            for (v in 0 until 3) {
                val a = (v * 120f - 90f) * PI.toFloat() / 180f
                val px = cx + prismR * cos(a)
                val py = cy + prismR * sin(a)
                if (v == 0) prismPath.moveTo(px, py) else prismPath.lineTo(px, py)
            }
            prismPath.close()

            drawPath(
                path = prismPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x40FFFFFF),
                        Color(0x18A5F3FC),
                        Color(0x34E879F9)
                    )
                )
            )
            drawPath(
                path = prismPath,
                color = Color(0xCCFFFFFF),
                style = Stroke(width = d * 0.009f, cap = StrokeCap.Round)
            )
            // Internal refraction caustic line
            drawLine(
                color = Color(0x66FFFFFF),
                start = Offset(cx - prismR * 0.4f, cy),
                end = Offset(cx + prismR * 0.4f, cy + prismR * 0.12f),
                strokeWidth = d * 0.008f
            )
        }
    }
}

@Composable
internal fun MoireVelvetFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(color = Color(0xFF06070B))

        // Lattice A: slowly precessing champagne-gold concentric rings
        val offsetA = Offset(
            cx + d * 0.055f * cos(t * 0.45f),
            cy + d * 0.055f * sin(t * 0.45f)
        )
        val ringCount = 22
        for (i in 1..ringCount) {
            val r = d * (0.04f + i * 0.022f)
            drawCircle(
                color = Color(0x52FFE39F),
                radius = r,
                center = offsetA,
                style = Stroke(width = d * 0.0038f)
            )
        }

        // Lattice B: counter-orbiting electric indigo rings generating velvet moiré interference
        val offsetB = Offset(
            cx - d * 0.055f * cos(t * 0.37f),
            cy + d * 0.055f * sin(t * 0.52f)
        )
        for (i in 1..ringCount) {
            val r = d * (0.04f + i * 0.022f)
            drawCircle(
                color = Color(0x589BB7FF),
                radius = r,
                center = offsetB,
                style = Stroke(width = d * 0.0038f)
            )
        }

        // Subtle warm coral nodal glow at center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x40FF7E6B), Color.Transparent),
                center = Offset(cx, cy),
                radius = d * 0.22f
            ),
            radius = d * 0.22f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
internal fun MinimalAtelierFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = CreamDial)
        val framePhase = phaseProvider()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val ring1 = size.minDimension * 0.36f
        val ring2 = size.minDimension * 0.23f
        drawCircle(
            color = Color(0xFFD5CCB9),
            radius = ring1,
            center = Offset(cx, cy),
            style = Stroke(width = size.minDimension * 0.008f)
        )
        drawCircle(
            color = Color(0xFFE0D8C7),
            radius = ring2,
            center = Offset(cx, cy),
            style = Stroke(width = size.minDimension * 0.008f)
        )
        val dotRad = framePhase * PI.toFloat() * 0.5f
        drawCircle(
            color = TerracottaAccent,
            radius = size.minDimension * 0.025f,
            center = Offset(cx + ring2 * cos(dotRad), cy + ring2 * sin(dotRad))
        )
        drawCircle(
            color = ObsidianChassis.copy(alpha = 0.65f),
            radius = size.minDimension * 0.016f,
            center = Offset(cx + ring1 * cos(-dotRad * 0.5f), cy + ring1 * sin(-dotRad * 0.5f))
        )
    }
}

@Composable
internal fun ZenRippleFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF121818), Color(0xFF080A0C), Color.Black),
                center = Offset(cx, cy),
                radius = d * 0.54f
            )
        )

        val dropX = cx + d * 0.055f * cos(t * 0.35f)
        val dropY = cy + d * 0.055f * sin(t * 0.28f)

        val ringCount = 5
        for (i in 0 until ringCount) {
            val progress = ((t * 0.20f) + i.toFloat() / ringCount) % 1f
            val alpha = sin(progress * PI.toFloat()).coerceIn(0f, 1f) * 0.55f
            val radius = d * (0.06f + progress * 0.41f)
            drawCircle(
                color = Color(0xFF8FA89B).copy(alpha = alpha),
                radius = radius,
                center = Offset(dropX, dropY),
                style = Stroke(width = d * (0.008f * (1f - progress * 0.55f)))
            )
        }

        val arcRadius = d * 0.42f
        val arcStart = (t * 14f) % 360f
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xAA8FA89B),
                    Color(0xFFEAE6DF),
                    Color.Transparent
                ),
                center = Offset(cx, cy)
            ),
            startAngle = arcStart,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(cx - arcRadius, cy - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = d * 0.009f, cap = StrokeCap.Round)
        )

        val beadAngle = (arcStart + 108f) * PI.toFloat() / 180f
        drawCircle(
            color = Color(0xFF5D8A7B),
            radius = d * 0.017f,
            center = Offset(cx + arcRadius * cos(beadAngle), cy + arcRadius * sin(beadAngle))
        )
    }
}

@Composable
internal fun NordicPenduleFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF6F1E7), Color(0xFFECE4D5)),
                center = Offset(cx, cy),
                radius = d * 0.55f
            )
        )

        for (i in 0 until 12) {
            val angle = (i * 30f - 90f) * PI.toFloat() / 180f
            val isCardinal = i % 3 == 0
            val rIn = d * (if (isCardinal) 0.405f else 0.425f)
            val rOut = d * 0.455f
            drawLine(
                color = Color(if (isCardinal) 0x99181A1F else 0x45181A1F),
                start = Offset(cx + rIn * cos(angle), cy + rIn * sin(angle)),
                end = Offset(cx + rOut * cos(angle), cy + rOut * sin(angle)),
                strokeWidth = d * (if (isCardinal) 0.008f else 0.0045f),
                cap = StrokeCap.Round
            )
        }

        val innerR = d * 0.305f
        drawCircle(
            color = Color(0x1E181A1F),
            radius = innerR,
            center = Offset(cx, cy),
            style = Stroke(width = d * 0.0045f)
        )

        val balanceAngle = (t * 22f + 12f * sin(t * 0.9f)) % 360f
        drawArc(
            color = Color(0xFF4A4E58),
            startAngle = balanceAngle,
            sweepAngle = 78f,
            useCenter = false,
            topLeft = Offset(cx - innerR, cy - innerR),
            size = Size(innerR * 2f, innerR * 2f),
            style = Stroke(width = d * 0.011f, cap = StrokeCap.Round)
        )

        val jewelR = d * (0.205f + 0.015f * sin(t * 1.1f))
        val jewelAngle = (-t * 0.65f)
        val jx = cx + jewelR * cos(jewelAngle)
        val jy = cy + jewelR * sin(jewelAngle)
        drawCircle(
            color = Color(0x28D94E34),
            radius = d * 0.036f,
            center = Offset(jx, jy)
        )
        drawCircle(
            color = Color(0xFFD94E34),
            radius = d * 0.019f,
            center = Offset(jx, jy)
        )
    }
}

@Composable
internal fun EclipseHaloFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(color = Color.Black)

        val orbitAngle = t * 0.42f
        val offsetMag = d * 0.042f
        val coronaCenter = Offset(
            cx + offsetMag * cos(orbitAngle),
            cy + offsetMag * sin(orbitAngle)
        )
        val coronaRadius = d * (0.37f + 0.012f * sin(t * 1.15f))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xAAE5B869),
                    Color(0x55F7F3EA),
                    Color(0x18B8C0CC),
                    Color.Transparent
                ),
                center = coronaCenter,
                radius = coronaRadius * 1.28f
            ),
            radius = coronaRadius * 1.28f,
            center = coronaCenter
        )

        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFE5B869),
                    Color(0xFFF7F3EA),
                    Color(0x22B8C0CC),
                    Color(0xFFE5B869)
                ),
                center = coronaCenter
            ),
            radius = coronaRadius * 0.80f,
            center = coronaCenter,
            style = Stroke(width = d * 0.012f)
        )

        drawCircle(
            color = Color(0xFF030406),
            radius = d * 0.285f,
            center = Offset(cx, cy)
        )

        val outerR = d * 0.435f
        drawArc(
            color = Color(0x66B8C0CC),
            startAngle = (orbitAngle * 180f / PI.toFloat()) - 35f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(cx - outerR, cy - outerR),
            size = Size(outerR * 2f, outerR * 2f),
            style = Stroke(width = d * 0.005f, cap = StrokeCap.Round)
        )
    }
}
