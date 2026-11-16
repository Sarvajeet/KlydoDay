package com.example.ui.clock.faces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun SplashDuckieFace(phaseProvider: () -> Float) {
    val wavePath = remember { Path() }
    val billPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        // Bright Mediterranean turquoise pool backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0B7285), Color(0xFF0A4E6E), Color(0xFF052B42))
            )
        )

        // Floating iridescent soap bubbles rising smoothly
        for (i in 0 until 9) {
            val prog = ((t * 0.22f) + i * 0.11f) % 1f
            val bx = w * (0.16f + ((i * 37) % 68) / 100f) + d * 0.025f * sin(t * 2f + i)
            val by = h * (0.92f - prog * 0.85f)
            val br = d * (0.020f + (i % 3) * 0.011f)
            val alpha = sin(prog * PI.toFloat()).coerceIn(0f, 1f) * 0.75f
            drawCircle(
                color = Color(0xFFA5F3FC).copy(alpha = alpha * 0.30f),
                radius = br,
                center = Offset(bx, by)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.72f),
                radius = br,
                center = Offset(bx, by),
                style = Stroke(width = d * 0.0045f)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = br * 0.24f,
                center = Offset(bx - br * 0.35f, by - br * 0.35f)
            )
        }

        // Bobbing sunny yellow rubber ducky
        val duckBob = d * 0.025f * sin(t * 2.4f)
        val duckTilt = sin(t * 1.9f) * 7f
        val duckC = Offset(cx, h * 0.57f + duckBob)
        rotate(degrees = duckTilt, pivot = duckC) {
            // Plump yellow body
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFEC80), Color(0xFFFFD43B), Color(0xFFF59F00)),
                    center = Offset(duckC.x - d * 0.04f, duckC.y),
                    radius = d * 0.22f
                ),
                topLeft = Offset(duckC.x - d * 0.17f, duckC.y - d * 0.02f),
                size = Size(d * 0.34f, d * 0.20f)
            )
            // Wing highlight
            drawOval(
                color = Color(0xFFFFF3BF),
                topLeft = Offset(duckC.x - d * 0.10f, duckC.y + d * 0.035f),
                size = Size(d * 0.15f, d * 0.09f)
            )
            // Head
            val headC = Offset(duckC.x + d * 0.075f, duckC.y - d * 0.075f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF099), Color(0xFFFFD43B)),
                    center = Offset(headC.x - d * 0.02f, headC.y - d * 0.02f),
                    radius = d * 0.12f
                ),
                radius = d * 0.105f,
                center = headC
            )
            // Bright orange bill
            billPath.reset()
            billPath.addOval(
                androidx.compose.ui.geometry.Rect(
                    headC.x + d * 0.065f,
                    headC.y - d * 0.012f,
                    headC.x + d * 0.165f,
                    headC.y + d * 0.042f
                )
            )
            drawPath(path = billPath, color = Color(0xFFFF6B35))

            // Blinking cheerful eye
            val eyeC = Offset(headC.x + d * 0.036f, headC.y - d * 0.025f)
            if ((t % 3.5f) > 3.34f) {
                drawLine(
                    color = Color(0xFF1F2937),
                    start = Offset(eyeC.x - d * 0.015f, eyeC.y),
                    end = Offset(eyeC.x + d * 0.015f, eyeC.y),
                    strokeWidth = d * 0.008f,
                    cap = StrokeCap.Round
                )
            } else {
                drawCircle(color = Color(0xFF1F2937), radius = d * 0.018f, center = eyeC)
                drawCircle(
                    color = Color.White,
                    radius = d * 0.006f,
                    center = Offset(eyeC.x - d * 0.005f, eyeC.y - d * 0.005f)
                )
            }
        }

        // Foreground undulating translucent turquoise water waves
        for (layer in 0..1) {
            val baseY = h * (0.69f + layer * 0.09f)
            wavePath.reset()
            wavePath.moveTo(0f, h)
            val steps = 28
            for (s in 0..steps) {
                val frac = s.toFloat() / steps
                val x = frac * w
                val y = baseY + sin(frac * 8f + t * (2.2f + layer * 0.7f) + layer * 1.5f) * d * 0.025f
                if (s == 0) wavePath.lineTo(0f, y) else wavePath.lineTo(x, y)
            }
            wavePath.lineTo(w, h)
            wavePath.close()
            drawPath(
                path = wavePath,
                color = if (layer == 0) Color(0xBB15AABF) else Color(0xEE0B7285)
            )
        }
    }
}

@Composable
internal fun MidnightGrooveFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(color = Color(0xFF0A0B0F))

        // Pulsing circular neon equalizer bars behind the LP edge
        val eqCount = 36
        for (i in 0 until eqCount) {
            val a = (i * (360f / eqCount)) * PI.toFloat() / 180f
            val amp = 0.012f + 0.032f * ((sin(t * 4.5f + i * 0.65f) + 1f) * 0.5f)
            val rStart = d * 0.42f
            val rEnd = d * (0.42f + amp)
            val eqColor = when (i % 3) {
                0 -> Color(0xFFFF70A6)
                1 -> Color(0xFF70D6FF)
                else -> Color(0xFFFFD670)
            }
            drawLine(
                color = eqColor.copy(alpha = 0.78f),
                start = Offset(cx + rStart * cos(a), cy + rStart * sin(a)),
                end = Offset(cx + rEnd * cos(a), cy + rEnd * sin(a)),
                strokeWidth = d * 0.009f,
                cap = StrokeCap.Round
            )
        }

        // Spinning 33 1/3 RPM vinyl LP
        val lpRot = (t * 68f) % 360f
        val lpR = d * 0.405f
        rotate(degrees = lpRot, pivot = Offset(cx, cy)) {
            drawCircle(color = Color(0xFF14151A), radius = lpR, center = Offset(cx, cy))
            // Iridescent specular light wedge rotating on grooves
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0x2870D6FF),
                        Color(0x04FFFFFF),
                        Color(0x2EFF70A6),
                        Color(0x04FFFFFF),
                        Color(0x2870D6FF)
                    ),
                    center = Offset(cx, cy)
                ),
                radius = lpR * 0.96f,
                center = Offset(cx, cy)
            )
            // Concentric vinyl micro-grooves
            for (g in 1..7) {
                drawCircle(
                    color = Color(0x22FFFFFF),
                    radius = lpR * (0.40f + g * 0.08f),
                    center = Offset(cx, cy),
                    style = Stroke(width = d * 0.0028f)
                )
            }
            // Spinning vibrant coral-amber record label
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFF70A6), Color(0xFFFF9770), Color(0xFFFFD670))
                ),
                radius = lpR * 0.34f,
                center = Offset(cx, cy)
            )
            drawLine(
                color = Color(0x8814151A),
                start = Offset(cx - lpR * 0.22f, cy),
                end = Offset(cx + lpR * 0.22f, cy),
                strokeWidth = d * 0.006f,
                cap = StrokeCap.Round
            )
            drawCircle(color = Color(0xFF0A0B0F), radius = lpR * 0.055f, center = Offset(cx, cy))
        }

        // Brushed aluminum turntable tonearm tracking gracefully
        val armPivot = Offset(cx + d * 0.33f, cy - d * 0.27f)
        val armSwing = sin(t * 0.6f) * 2.5f
        rotate(degrees = armSwing, pivot = armPivot) {
            drawCircle(color = Color(0xFF94A3B8), radius = d * 0.038f, center = armPivot)
            val elbow = Offset(armPivot.x - d * 0.07f, armPivot.y + d * 0.26f)
            val stylus = Offset(elbow.x - d * 0.06f, elbow.y + d * 0.09f)
            drawLine(
                color = Color(0xFFE2E8F0),
                start = armPivot,
                end = elbow,
                strokeWidth = d * 0.015f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFE2E8F0),
                start = elbow,
                end = stylus,
                strokeWidth = d * 0.013f,
                cap = StrokeCap.Round
            )
            drawRoundRect(
                color = Color(0xFFFF70A6),
                topLeft = Offset(stylus.x - d * 0.018f, stylus.y - d * 0.014f),
                size = Size(d * 0.036f, d * 0.028f),
                cornerRadius = CornerRadius(d * 0.008f, d * 0.008f)
            )
        }
    }
}

@Composable
internal fun GalaxyBobaFace(phaseProvider: () -> Float) {
    val cupPath = remember { Path() }
    val teaPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.53f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF281746), Color(0xFF150B29), Color(0xFF090414)),
                center = Offset(cx, cy),
                radius = d * 0.56f
            )
        )

        // Orbiting pastel star sprinkles & floating pearls
        for (i in 0 until 12) {
            val a = t * 0.35f + i * (2f * PI.toFloat() / 12f)
            val rad = d * (0.36f + (i % 3) * 0.035f)
            val sx = cx + rad * cos(a)
            val sy = cy + rad * sin(a)
            val col = when (i % 3) {
                0 -> Color(0xFFF472B6)
                1 -> Color(0xFFC4B5FD)
                else -> Color(0xFFA7F3D0)
            }
            drawCircle(color = col.copy(alpha = 0.85f), radius = d * (0.009f + (i % 2) * 0.005f), center = Offset(sx, sy))
        }

        val bob = d * 0.016f * sin(t * 2.0f)
        val tilt = sin(t * 1.4f) * 4f
        val cupC = Offset(cx, cy + bob)
        rotate(degrees = tilt, pivot = cupC) {
            // Wide pastel striped boba straw
            drawLine(
                color = Color(0xFFF472B6),
                start = Offset(cupC.x + d * 0.035f, cupC.y - d * 0.08f),
                end = Offset(cupC.x + d * 0.078f, cupC.y - d * 0.29f),
                strokeWidth = d * 0.034f,
                cap = StrokeCap.Round
            )

            // Cup dimensions
            val topHalfW = d * 0.145f
            val botHalfW = d * 0.105f
            val topY = cupC.y - d * 0.135f
            val botY = cupC.y + d * 0.195f

            // Sloshing lavender-taro milk tea wave inside
            val liquidTop = cupC.y - d * 0.025f
            teaPath.reset()
            teaPath.moveTo(cupC.x - botHalfW, botY)
            teaPath.lineTo(cupC.x - topHalfW * 0.90f, liquidTop + d * 0.016f * sin(t * 3.0f))
            teaPath.quadraticTo(
                cupC.x,
                liquidTop - d * 0.020f * sin(t * 3.0f),
                cupC.x + topHalfW * 0.90f,
                liquidTop - d * 0.016f * sin(t * 3.0f)
            )
            teaPath.lineTo(cupC.x + botHalfW, botY)
            teaPath.close()
            drawPath(
                path = teaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFC4B5FD), Color(0xFF9370DB)),
                    startY = liquidTop,
                    endY = botY
                )
            )

            // Bouncy glossy tapioca boba pearls at the bottom
            for (p in 0 until 7) {
                val px = cupC.x - botHalfW * 0.65f + p * (botHalfW * 1.3f / 6f)
                val py = botY - d * (0.025f + (p % 2) * 0.022f + 0.008f * sin(t * 3.4f + p))
                drawCircle(color = Color(0xFF231636), radius = d * 0.019f, center = Offset(px, py))
                drawCircle(
                    color = Color(0xAAFFFFFF),
                    radius = d * 0.005f,
                    center = Offset(px - d * 0.005f, py - d * 0.005f)
                )
            }

            // Clear cup outline & dome lid
            cupPath.reset()
            cupPath.moveTo(cupC.x - topHalfW, topY)
            cupPath.lineTo(cupC.x - botHalfW, botY)
            cupPath.lineTo(cupC.x + botHalfW, botY)
            cupPath.lineTo(cupC.x + topHalfW, topY)
            cupPath.close()
            drawPath(
                path = cupPath,
                color = Color(0xCCFFF0F5),
                style = Stroke(width = d * 0.010f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0x44FFFFFF),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cupC.x - topHalfW, topY - topHalfW * 0.65f),
                size = Size(topHalfW * 2f, topHalfW * 1.3f),
                style = Stroke(width = d * 0.009f)
            )

            // Cute kawaii face on cup
            for (side in listOf(-1f, 1f)) {
                drawCircle(
                    color = Color(0xFF231636),
                    radius = d * 0.013f,
                    center = Offset(cupC.x + side * d * 0.045f, cupC.y + d * 0.035f)
                )
            }
            drawArc(
                color = Color(0xFF231636),
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(cupC.x - d * 0.022f, cupC.y + d * 0.040f),
                size = Size(d * 0.044f, d * 0.028f),
                style = Stroke(width = d * 0.0075f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
internal fun OrigamiSoarFace(phaseProvider: () -> Float) {
    val planeWingPath = remember { Path() }
    val foldShadowPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        // Crisp paper-sky gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFCDE6FF), Color(0xFFE8F4FF), Color(0xFFF6FAFF))
            )
        )

        // Warm playful paper sun
        drawCircle(
            color = Color(0x40FDE047),
            radius = d * 0.14f,
            center = Offset(w * 0.76f, h * 0.24f)
        )
        drawCircle(
            color = Color(0xFFFACC15),
            radius = d * 0.075f,
            center = Offset(w * 0.76f, h * 0.24f)
        )

        // Soft drifting clouds
        for (c in 0..1) {
            val clX = ((w * 0.20f + c * w * 0.50f + t * d * 0.035f) % (w * 1.25f)) - w * 0.12f
            val clY = h * (0.28f + c * 0.46f)
            drawCircle(color = Color.White, radius = d * 0.055f, center = Offset(clX, clY))
            drawCircle(color = Color.White, radius = d * 0.042f, center = Offset(clX - d * 0.045f, clY + d * 0.010f))
            drawCircle(color = Color.White, radius = d * 0.044f, center = Offset(clX + d * 0.045f, clY + d * 0.008f))
        }

        // Dotted fading contrail along figure-8 flight path
        val u = t * 0.85f
        for (dot in 1..14) {
            val trailU = u - dot * 0.14f
            val tx = cx + d * 0.30f * sin(trailU)
            val ty = cy + d * 0.18f * sin(2f * trailU)
            val alpha = (1f - dot / 15f) * 0.60f
            drawCircle(
                color = Color(0xFF3B82F6).copy(alpha = alpha),
                radius = d * 0.0065f,
                center = Offset(tx, ty)
            )
        }

        // Sharp faceted crimson-and-white origami paper airplane banking along heading
        val px = cx + d * 0.30f * sin(u)
        val py = cy + d * 0.18f * sin(2f * u)
        val dx = d * 0.30f * cos(u)
        val dy = d * 0.36f * cos(2f * u)
        val headingDeg = atan2(dy, dx) * 180f / PI.toFloat()

        rotate(degrees = headingDeg, pivot = Offset(px, py)) {
            val nose = Offset(px + d * 0.085f, py)
            val topTail = Offset(px - d * 0.065f, py - d * 0.058f)
            val botTail = Offset(px - d * 0.065f, py + d * 0.058f)
            val keelTail = Offset(px - d * 0.050f, py + d * 0.015f)

            // Upper crimson wing
            planeWingPath.reset()
            planeWingPath.moveTo(nose.x, nose.y)
            planeWingPath.lineTo(topTail.x, topTail.y)
            planeWingPath.lineTo(keelTail.x, keelTail.y)
            planeWingPath.close()
            drawPath(path = planeWingPath, color = Color(0xFFEF4444))

            // Shaded lower inner fuselage fold
            foldShadowPath.reset()
            foldShadowPath.moveTo(nose.x, nose.y)
            foldShadowPath.lineTo(keelTail.x, keelTail.y)
            foldShadowPath.lineTo(botTail.x, botTail.y)
            foldShadowPath.close()
            drawPath(path = foldShadowPath, color = Color(0xFFB91C1C))

            // Crisp white origami spine highlight
            drawLine(
                color = Color.White,
                start = nose,
                end = keelTail,
                strokeWidth = d * 0.006f,
                cap = StrokeCap.Round
            )
        }
    }
}
