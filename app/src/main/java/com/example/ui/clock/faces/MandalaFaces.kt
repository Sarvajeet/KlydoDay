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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun LotusYantraFace(phaseProvider: () -> Float) {
    val petalPath = remember { Path() }
    val triPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF220A14), Color(0xFF0D0408), Color(0xFF030103)),
                center = Offset(cx, cy),
                radius = d * 0.55f
            )
        )

        val breathe = 1f + 0.035f * sin(t * 1.1f)

        // Outer 16-petal delicate gold-rose filigree ring
        rotate(degrees = (t * 5f) % 360f, pivot = Offset(cx, cy)) {
            val count = 16
            val rBase = d * 0.25f * breathe
            val rTip = d * 0.44f * breathe
            for (i in 0 until count) {
                val step = 360f / count
                val a0 = (i * step - step * 0.42f) * PI.toFloat() / 180f
                val aMid = (i * step) * PI.toFloat() / 180f
                val a1 = (i * step + step * 0.42f) * PI.toFloat() / 180f
                petalPath.reset()
                petalPath.moveTo(cx + rBase * cos(a0), cy + rBase * sin(a0))
                petalPath.quadraticTo(
                    cx + rTip * 0.96f * cos(a0),
                    cy + rTip * 0.96f * sin(a0),
                    cx + rTip * cos(aMid),
                    cy + rTip * sin(aMid)
                )
                petalPath.quadraticTo(
                    cx + rTip * 0.96f * cos(a1),
                    cy + rTip * 0.96f * sin(a1),
                    cx + rBase * cos(a1),
                    cy + rBase * sin(a1)
                )
                petalPath.close()

                drawPath(
                    path = petalPath,
                    color = if (i % 2 == 0) Color(0x30E85D75) else Color(0x28E5A93C)
                )
                drawPath(
                    path = petalPath,
                    color = Color(0xAAE5A93C),
                    style = Stroke(width = d * 0.0055f)
                )
            }
        }

        // Middle 12-petal sculpted lotus tier counter-rotating
        rotate(degrees = (-t * 8f) % 360f, pivot = Offset(cx, cy)) {
            val count = 12
            val rBase = d * 0.14f
            val rTip = d * 0.31f * breathe
            for (i in 0 until count) {
                val step = 360f / count
                val a0 = (i * step - step * 0.45f) * PI.toFloat() / 180f
                val aMid = (i * step) * PI.toFloat() / 180f
                val a1 = (i * step + step * 0.45f) * PI.toFloat() / 180f
                petalPath.reset()
                petalPath.moveTo(cx + rBase * cos(a0), cy + rBase * sin(a0))
                petalPath.quadraticTo(
                    cx + rTip * cos(a0),
                    cy + rTip * sin(a0),
                    cx + rTip * cos(aMid),
                    cy + rTip * sin(aMid)
                )
                petalPath.quadraticTo(
                    cx + rTip * cos(a1),
                    cy + rTip * sin(a1),
                    cx + rBase * cos(a1),
                    cy + rBase * sin(a1)
                )
                petalPath.close()
                drawPath(
                    path = petalPath,
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x99E85D75), Color(0x66FFE49E)),
                        center = Offset(cx, cy),
                        radius = rTip
                    )
                )
                drawPath(
                    path = petalPath,
                    color = Color(0xCCFFE49E),
                    style = Stroke(width = d * 0.005f)
                )
            }
        }

        // Interlocking Sri-Yantra golden star triangles
        rotate(degrees = (t * 11f) % 360f, pivot = Offset(cx, cy)) {
            val yantraR = d * 0.18f
            for (flip in 0..1) {
                triPath.reset()
                for (v in 0 until 3) {
                    val ang = (v * 120f + flip * 60f - 90f) * PI.toFloat() / 180f
                    val px = cx + yantraR * cos(ang)
                    val py = cy + yantraR * sin(ang)
                    if (v == 0) triPath.moveTo(px, py) else triPath.lineTo(px, py)
                }
                triPath.close()
                drawPath(
                    path = triPath,
                    color = Color(0xDDE5A93C),
                    style = Stroke(width = d * 0.006f)
                )
            }
        }

        // Central luminous bindu jewel
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFE49E), Color(0xAAE85D75), Color.Transparent),
                center = Offset(cx, cy),
                radius = d * 0.09f
            ),
            radius = d * 0.09f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
internal fun PrismKaleidoscopeFace(phaseProvider: () -> Float) {
    val shardPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF131524), Color(0xFF06070D), Color.Black),
                center = Offset(cx, cy),
                radius = d * 0.56f
            )
        )

        val symmetry = 12
        val bloom = 0.85f + 0.15f * sin(t * 1.2f)
        val outerRot = (t * 10f) % 360f

        rotate(degrees = outerRot, pivot = Offset(cx, cy)) {
            for (i in 0 until symmetry) {
                val baseA = (i * (360f / symmetry)) * PI.toFloat() / 180f
                val spread = (12f + 4f * sin(t * 0.9f)) * PI.toFloat() / 180f
                val r1 = d * 0.13f * bloom
                val r2 = d * 0.28f * bloom
                val r3 = d * 0.43f * bloom

                shardPath.reset()
                shardPath.moveTo(cx + r1 * cos(baseA), cy + r1 * sin(baseA))
                shardPath.lineTo(cx + r2 * cos(baseA - spread), cy + r2 * sin(baseA - spread))
                shardPath.lineTo(cx + r3 * cos(baseA), cy + r3 * sin(baseA))
                shardPath.lineTo(cx + r2 * cos(baseA + spread), cy + r2 * sin(baseA + spread))
                shardPath.close()

                val tint = when (i % 3) {
                    0 -> Color(0xFF5EEAD4)
                    1 -> Color(0xFFF472B6)
                    else -> Color(0xFFFDE68A)
                }
                drawPath(
                    path = shardPath,
                    brush = Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = 0.72f), tint.copy(alpha = 0.14f)),
                        center = Offset(cx, cy),
                        radius = r3
                    )
                )
                drawPath(
                    path = shardPath,
                    color = Color(0xAAFFFFFF),
                    style = Stroke(width = d * 0.004f)
                )
            }
        }

        // Inner counter-rotating gem ring
        rotate(degrees = (-t * 18f) % 360f, pivot = Offset(cx, cy)) {
            for (i in 0 until symmetry) {
                val a = (i * (360f / symmetry)) * PI.toFloat() / 180f
                val rBead = d * (0.18f + 0.02f * cos(t * 1.6f))
                drawCircle(
                    color = if (i % 2 == 0) Color(0xFF5EEAD4) else Color(0xFFF472B6),
                    radius = d * 0.015f,
                    center = Offset(cx + rBead * cos(a), cy + rBead * sin(a))
                )
            }
        }
    }
}

@Composable
internal fun AstrolabeGuildFace(phaseProvider: () -> Float) {
    val starPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF10204A), Color(0xFF070E24), Color(0xFF03050C)),
                center = Offset(cx, cy),
                radius = d * 0.55f
            )
        )

        // Outer engraved antique brass degree ring
        val outerR = d * 0.435f
        drawCircle(
            color = Color(0xAAC99D44),
            radius = outerR,
            center = Offset(cx, cy),
            style = Stroke(width = d * 0.010f)
        )
        for (i in 0 until 72) {
            val a = (i * 5f) * PI.toFloat() / 180f
            val major = i % 6 == 0
            val rIn = outerR - d * (if (major) 0.026f else 0.012f)
            drawLine(
                color = Color(if (major) 0xCCF7DC98 else 0x55C99D44),
                start = Offset(cx + rIn * cos(a), cy + rIn * sin(a)),
                end = Offset(cx + outerR * cos(a), cy + outerR * sin(a)),
                strokeWidth = d * (if (major) 0.005f else 0.0028f)
            )
        }

        // Rotating 12-point astrolabe geometric star polygon filigree
        rotate(degrees = (t * 6f) % 360f, pivot = Offset(cx, cy)) {
            val rStar = d * 0.36f
            starPath.reset()
            var idx = 0
            for (step in 0..12) {
                val a = (idx * 30f) * PI.toFloat() / 180f
                val px = cx + rStar * cos(a)
                val py = cy + rStar * sin(a)
                if (step == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                idx = (idx + 5) % 12
            }
            starPath.close()
            drawPath(
                path = starPath,
                color = Color(0x66C99D44),
                style = Stroke(width = d * 0.0045f)
            )
        }

        // Eccentric counter-rotating astrolabe Rete circle
        rotate(degrees = (-t * 14f) % 360f, pivot = Offset(cx, cy)) {
            val reteCenter = Offset(cx + d * 0.045f, cy - d * 0.03f)
            val reteR = d * 0.255f
            drawCircle(
                color = Color(0xDDF7DC98),
                radius = reteR,
                center = reteCenter,
                style = Stroke(width = d * 0.010f)
            )
            // Ecliptic zodiac jewel pointers on rete
            for (p in 0 until 6) {
                val a = (p * 60f) * PI.toFloat() / 180f
                val starPos = Offset(reteCenter.x + reteR * cos(a), reteCenter.y + reteR * sin(a))
                drawCircle(color = Color(0xFF3D7EFF), radius = d * 0.015f, center = starPos)
                drawCircle(
                    color = Color(0xFFF7DC98),
                    radius = d * 0.015f,
                    center = starPos,
                    style = Stroke(width = d * 0.004f)
                )
            }
        }

        // Center celestial globe ring
        drawCircle(
            color = Color(0x88F7DC98),
            radius = d * 0.11f,
            center = Offset(cx, cy),
            style = Stroke(width = d * 0.006f)
        )
    }
}

@Composable
internal fun KirigamiLaceFace(phaseProvider: () -> Float) {
    val petalPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        // Warm cream washi paper dial
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFAF6EE), Color(0xFFEDE4D4)),
                center = Offset(cx, cy),
                radius = d * 0.55f
            )
        )

        // Outer slate-blue cut-paper 12-lobe rosette with soft drop shadow
        drawPaperRosette(
            petalPath = petalPath,
            cx = cx,
            cy = cy,
            d = d,
            lobes = 12,
            outerR = d * 0.43f,
            innerR = d * 0.29f,
            rotationDeg = (t * 6f) % 360f,
            fillColor = Color(0xFF54687A)
        )

        // Middle terracotta cut-paper 10-lobe rosette
        drawPaperRosette(
            petalPath = petalPath,
            cx = cx,
            cy = cy,
            d = d,
            lobes = 10,
            outerR = d * 0.32f,
            innerR = d * 0.20f,
            rotationDeg = (-t * 10f) % 360f,
            fillColor = Color(0xFFD64933)
        )

        // Inner sage-cream cut-paper 8-lobe rosette
        drawPaperRosette(
            petalPath = petalPath,
            cx = cx,
            cy = cy,
            d = d,
            lobes = 8,
            outerR = d * 0.21f,
            innerR = d * 0.11f,
            rotationDeg = (t * 15f) % 360f,
            fillColor = Color(0xFFE3D8C5)
        )

        drawCircle(
            color = Color(0xFF1B2028),
            radius = d * 0.055f,
            center = Offset(cx, cy),
            style = Stroke(width = d * 0.007f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPaperRosette(
    petalPath: Path,
    cx: Float,
    cy: Float,
    d: Float,
    lobes: Int,
    outerR: Float,
    innerR: Float,
    rotationDeg: Float,
    fillColor: Color
) {
    rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
        petalPath.reset()
        for (i in 0 until lobes) {
            val step = 360f / lobes
            val a0 = (i * step) * PI.toFloat() / 180f
            val aMid = (i * step + step * 0.5f) * PI.toFloat() / 180f
            val a1 = ((i + 1) * step) * PI.toFloat() / 180f
            val p0 = Offset(cx + innerR * cos(a0), cy + innerR * sin(a0))
            val ctrl = Offset(cx + outerR * 1.12f * cos(aMid), cy + outerR * 1.12f * sin(aMid))
            val p1 = Offset(cx + innerR * cos(a1), cy + innerR * sin(a1))
            if (i == 0) petalPath.moveTo(p0.x, p0.y)
            petalPath.quadraticTo(ctrl.x, ctrl.y, p1.x, p1.y)
        }
        petalPath.close()

        // Gentle paper shadow under cut edge
        drawPath(
            path = petalPath,
            color = Color(0x28000000),
            style = Stroke(width = d * 0.014f, cap = StrokeCap.Round)
        )
        drawPath(path = petalPath, color = fillColor)
        drawPath(
            path = petalPath,
            color = Color(0x40FFFFFF),
            style = Stroke(width = d * 0.004f)
        )
    }
}
