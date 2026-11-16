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
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun MochiNekoFace(phaseProvider: () -> Float) {
    val earPath = remember { Path() }
    val tailPath = remember { Path() }
    val mouthPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        // Cozy twilight lavender-indigo nursery sky
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF191F38), Color(0xFF282E4F), Color(0xFF1C2038))
            )
        )

        // Twinkling pastel stars
        for (i in 0 until 12) {
            val sx = w * (0.12f + ((i * 43) % 76) / 100f)
            val sy = h * (0.10f + ((i * 29) % 34) / 100f)
            val alpha = 0.35f + 0.65f * ((sin(t * 2.1f + i * 1.4f) + 1f) * 0.5f)
            drawCircle(
                color = Color(0xFFFFE5B4).copy(alpha = alpha),
                radius = d * (0.006f + (i % 2) * 0.003f),
                center = Offset(sx, sy)
            )
        }

        // Gentle breathing head bob
        val bob = d * 0.015f * sin(t * 1.8f)
        val headC = Offset(cx, h * 0.63f + bob)
        val headRx = d * 0.27f
        val headRy = d * 0.22f

        // Playfully swinging cat tail peeking on right
        val tailSwing = sin(t * 2.2f) * d * 0.06f
        tailPath.reset()
        tailPath.moveTo(cx + headRx * 0.85f, h * 0.80f)
        tailPath.cubicTo(
            cx + headRx * 1.35f + tailSwing, h * 0.72f,
            cx + headRx * 1.25f + tailSwing, h * 0.54f,
            cx + headRx * 1.05f + tailSwing * 0.6f, h * 0.50f
        )
        drawPath(
            path = tailPath,
            color = Color(0xFFFFD8B5),
            style = Stroke(width = d * 0.052f, cap = StrokeCap.Round)
        )

        // Twitching triangular cat ears
        for (side in listOf(-1f, 1f)) {
            val earTwitch = if (side > 0f) sin(t * 3.1f) * 5f else -sin(t * 2.3f) * 3f
            val earPivot = Offset(headC.x + side * headRx * 0.56f, headC.y - headRy * 0.58f)
            rotate(degrees = side * 16f + earTwitch, pivot = earPivot) {
                earPath.reset()
                earPath.moveTo(earPivot.x - d * 0.075f, earPivot.y + d * 0.045f)
                earPath.lineTo(earPivot.x, earPivot.y - d * 0.115f)
                earPath.lineTo(earPivot.x + d * 0.075f, earPivot.y + d * 0.045f)
                earPath.close()
                drawPath(
                    path = earPath,
                    color = if (side < 0f) Color(0xFFFFCFA8) else Color(0xFFFFF8F0)
                )
                // Soft inner pink ear
                earPath.reset()
                earPath.moveTo(earPivot.x - d * 0.040f, earPivot.y + d * 0.025f)
                earPath.lineTo(earPivot.x, earPivot.y - d * 0.068f)
                earPath.lineTo(earPivot.x + d * 0.040f, earPivot.y + d * 0.025f)
                earPath.close()
                drawPath(path = earPath, color = Color(0xFFFFA5BD))
            }
        }

        // Plump cream mochi head + round body base
        drawOval(
            color = Color(0xFFFFF8F0),
            topLeft = Offset(headC.x - headRx, headC.y - headRy),
            size = Size(headRx * 2f, headRy * 2f)
        )
        // Calico warm peach & mocha ear patches
        drawArc(
            color = Color(0xFFFFB997),
            startAngle = 195f,
            sweepAngle = 70f,
            useCenter = true,
            topLeft = Offset(headC.x - headRx, headC.y - headRy),
            size = Size(headRx * 1.4f, headRy * 1.4f)
        )
        drawArc(
            color = Color(0xFF9C7A68).copy(alpha = 0.85f),
            startAngle = 290f,
            sweepAngle = 55f,
            useCenter = true,
            topLeft = Offset(headC.x - headRx * 0.4f, headC.y - headRy),
            size = Size(headRx * 1.4f, headRy * 1.3f)
        )

        // Rosy blush cheeks
        for (side in listOf(-1f, 1f)) {
            drawOval(
                color = Color(0x66FF8FAB),
                topLeft = Offset(headC.x + side * headRx * 0.56f - d * 0.045f, headC.y + d * 0.01f),
                size = Size(d * 0.090f, d * 0.046f)
            )
        }

        // Expressive blinking / happy crescent eyes (`^ ^`)
        val blinkCycle = (t % 4.2f)
        val isHappyArc = blinkCycle > 3.55f && blinkCycle < 4.05f
        for (side in listOf(-1f, 1f)) {
            val ex = headC.x + side * headRx * 0.40f
            val ey = headC.y - d * 0.018f
            if (isHappyArc) {
                drawArc(
                    color = Color(0xFF2D232E),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(ex - d * 0.035f, ey - d * 0.022f),
                    size = Size(d * 0.070f, d * 0.048f),
                    style = Stroke(width = d * 0.011f, cap = StrokeCap.Round)
                )
            } else {
                drawCircle(color = Color(0xFF2D232E), radius = d * 0.028f, center = Offset(ex, ey))
                drawCircle(
                    color = Color.White,
                    radius = d * 0.009f,
                    center = Offset(ex - d * 0.008f, ey - d * 0.009f)
                )
            }
        }

        // Tiny pink nose + `:3` w-mouth
        val noseY = headC.y + d * 0.014f
        drawOval(
            color = Color(0xFFFF7A9E),
            topLeft = Offset(headC.x - d * 0.014f, noseY - d * 0.008f),
            size = Size(d * 0.028f, d * 0.018f)
        )
        mouthPath.reset()
        mouthPath.moveTo(headC.x - d * 0.036f, noseY + d * 0.022f)
        mouthPath.quadraticTo(headC.x - d * 0.018f, noseY + d * 0.042f, headC.x, noseY + d * 0.020f)
        mouthPath.quadraticTo(headC.x + d * 0.018f, noseY + d * 0.042f, headC.x + d * 0.036f, noseY + d * 0.022f)
        drawPath(
            path = mouthPath,
            color = Color(0xFF3E303D),
            style = Stroke(width = d * 0.0075f, cap = StrokeCap.Round)
        )

        // Gently wiggling whiskers
        val wWiggle = sin(t * 2.8f) * d * 0.008f
        for (side in listOf(-1f, 1f)) {
            for (row in -1..1) {
                val wy = headC.y + d * 0.012f + row * d * 0.018f
                drawLine(
                    color = Color(0xAA73626E),
                    start = Offset(headC.x + side * headRx * 0.66f, wy),
                    end = Offset(headC.x + side * headRx * 1.06f, wy + row * d * 0.012f + wWiggle),
                    strokeWidth = d * 0.005f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Cute rolling pink yarn ball bouncing in upper left
        val yarnAngle = (t * 40f) % 360f
        val yarnPos = Offset(w * 0.24f + d * 0.025f * sin(t * 1.1f), h * 0.29f + d * 0.018f * cos(t * 1.6f))
        rotate(degrees = yarnAngle, pivot = yarnPos) {
            drawCircle(color = Color(0xFFFF8FAB), radius = d * 0.055f, center = yarnPos)
            drawArc(
                color = Color(0xFFFFC2D1),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(yarnPos.x - d * 0.040f, yarnPos.y - d * 0.040f),
                size = Size(d * 0.080f, d * 0.080f),
                style = Stroke(width = d * 0.007f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
internal fun AstroPupFace(phaseProvider: () -> Float) {
    val bonePath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.52f
        val d = size.minDimension
        val t = phaseProvider()

        // Deep cosmic cartoon sky
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A2248), Color(0xFF0C1026), Color(0xFF050712)),
                center = Offset(cx, cy),
                radius = d * 0.56f
            )
        )

        // Pastel ringed planet in upper-left background
        val planetC = Offset(w * 0.22f, h * 0.24f)
        drawCircle(color = Color(0xFFFF9E7D), radius = d * 0.058f, center = planetC)
        rotate(degrees = -22f, pivot = planetC) {
            drawOval(
                color = Color(0xAAFFD166),
                topLeft = Offset(planetC.x - d * 0.095f, planetC.y - d * 0.020f),
                size = Size(d * 0.190f, d * 0.040f),
                style = Stroke(width = d * 0.008f)
            )
        }

        // Floating zero-gravity golden dog bone orbiting the pup
        val boneOrbA = t * 0.7f
        val boneC = Offset(cx + d * 0.34f * cos(boneOrbA), cy + d * 0.30f * sin(boneOrbA))
        rotate(degrees = (t * 65f) % 360f, pivot = boneC) {
            val bw = d * 0.044f
            val bh = d * 0.015f
            drawLine(
                color = Color(0xFFFFD166),
                start = Offset(boneC.x - bw * 0.6f, boneC.y),
                end = Offset(boneC.x + bw * 0.6f, boneC.y),
                strokeWidth = bh * 1.4f,
                cap = StrokeCap.Round
            )
            for (sx in listOf(-1f, 1f)) {
                for (sy in listOf(-1f, 1f)) {
                    drawCircle(
                        color = Color(0xFFFFD166),
                        radius = bh * 0.85f,
                        center = Offset(boneC.x + sx * bw * 0.62f, boneC.y + sy * bh * 0.62f)
                    )
                }
            }
        }

        // Zero-G bobbing Shiba pup inside glossy helmet
        val bobY = d * 0.025f * sin(t * 1.6f)
        val tilt = sin(t * 0.9f) * 6f
        val pupCenter = Offset(cx, cy + bobY)
        rotate(degrees = tilt, pivot = pupCenter) {
            // Suit collar ring
            drawRoundRect(
                color = Color(0xFFFF6B57),
                topLeft = Offset(pupCenter.x - d * 0.16f, pupCenter.y + d * 0.16f),
                size = Size(d * 0.32f, d * 0.065f),
                cornerRadius = CornerRadius(d * 0.03f, d * 0.03f)
            )

            // Shiba tan-caramel ears wagging gently
            for (side in listOf(-1f, 1f)) {
                val earC = Offset(pupCenter.x + side * d * 0.105f, pupCenter.y - d * 0.115f)
                bonePath.reset()
                bonePath.moveTo(earC.x - d * 0.045f, earC.y + d * 0.03f)
                bonePath.lineTo(earC.x + side * d * 0.015f, earC.y - d * 0.075f)
                bonePath.lineTo(earC.x + d * 0.045f, earC.y + d * 0.03f)
                bonePath.close()
                drawPath(path = bonePath, color = Color(0xFFE8934A))
            }

            // Shiba round caramel & cream face
            drawCircle(color = Color(0xFFE8934A), radius = d * 0.155f, center = pupCenter)
            drawOval(
                color = Color(0xFFFFF4E3),
                topLeft = Offset(pupCenter.x - d * 0.135f, pupCenter.y - d * 0.015f),
                size = Size(d * 0.270f, d * 0.168f)
            )
            // Cream Shiba eyebrow dots
            for (side in listOf(-1f, 1f)) {
                drawCircle(
                    color = Color(0xFFFFF4E3),
                    radius = d * 0.020f,
                    center = Offset(pupCenter.x + side * d * 0.058f, pupCenter.y - d * 0.058f)
                )
            }

            // Happy blinking cartoon eyes
            val isBlink = (t % 3.6f) > 3.42f
            for (side in listOf(-1f, 1f)) {
                val ex = pupCenter.x + side * d * 0.058f
                val ey = pupCenter.y - d * 0.015f
                if (isBlink) {
                    drawLine(
                        color = Color(0xFF231F20),
                        start = Offset(ex - d * 0.020f, ey),
                        end = Offset(ex + d * 0.020f, ey),
                        strokeWidth = d * 0.009f,
                        cap = StrokeCap.Round
                    )
                } else {
                    drawCircle(color = Color(0xFF231F20), radius = d * 0.022f, center = Offset(ex, ey))
                    drawCircle(
                        color = Color.White,
                        radius = d * 0.008f,
                        center = Offset(ex - d * 0.007f, ey - d * 0.007f)
                    )
                }
            }

            // Shiny black nose & happy pink tongue
            drawOval(
                color = Color(0xFF231F20),
                topLeft = Offset(pupCenter.x - d * 0.022f, pupCenter.y + d * 0.022f),
                size = Size(d * 0.044f, d * 0.028f)
            )
            drawArc(
                color = Color(0xFFFF758F),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(pupCenter.x - d * 0.022f, pupCenter.y + d * 0.048f),
                size = Size(d * 0.044f, d * 0.042f)
            )

            // Glossy translucent bubble space helmet
            val helmR = d * 0.255f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0x185CE1E6), Color(0x455CE1E6)),
                    center = pupCenter,
                    radius = helmR
                ),
                radius = helmR,
                center = pupCenter
            )
            drawCircle(
                color = Color(0xAA9EF7FF),
                radius = helmR,
                center = pupCenter,
                style = Stroke(width = d * 0.010f)
            )
            // Curved glass specular highlight arc
            drawArc(
                color = Color(0xCCFFFFFF),
                startAngle = 212f,
                sweepAngle = 52f,
                useCenter = false,
                topLeft = Offset(pupCenter.x - helmR * 0.84f, pupCenter.y - helmR * 0.84f),
                size = Size(helmR * 1.68f, helmR * 1.68f),
                style = Stroke(width = d * 0.013f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
internal fun BeepBoopBotFace(phaseProvider: () -> Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.55f
        val d = size.minDimension
        val t = phaseProvider()

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A2842), Color(0xFF0C1220), Color(0xFF050810)),
                center = Offset(cx, cy),
                radius = d * 0.56f
            )
        )

        val headBob = d * 0.015f * sin(t * 2.2f)
        val botCenter = Offset(cx, cy + headBob)
        val boxW = d * 0.52f
        val boxH = d * 0.40f

        // Bouncing coiled spring antenna & glowing beacon bulb
        val antennaTopY = botCenter.y - boxH * 0.5f - d * (0.14f + 0.018f * sin(t * 4.5f))
        val antennaSwayX = botCenter.x + d * 0.025f * sin(t * 2.2f)
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(botCenter.x, botCenter.y - boxH * 0.5f),
            end = Offset(antennaSwayX, antennaTopY),
            strokeWidth = d * 0.014f,
            cap = StrokeCap.Round
        )
        val beaconGlow = 0.5f + 0.5f * sin(t * 3.8f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF5964).copy(alpha = 0.65f * beaconGlow), Color.Transparent),
                center = Offset(antennaSwayX, antennaTopY),
                radius = d * 0.085f
            ),
            radius = d * 0.085f,
            center = Offset(antennaSwayX, antennaTopY)
        )
        drawCircle(
            color = Color(0xFFFF5964),
            radius = d * 0.028f,
            center = Offset(antennaSwayX, antennaTopY)
        )

        // Side ear bolts
        for (side in listOf(-1f, 1f)) {
            drawRoundRect(
                color = Color(0xFF60A5FA),
                topLeft = Offset(botCenter.x + side * boxW * 0.52f - d * 0.03f, botCenter.y - d * 0.055f),
                size = Size(d * 0.06f, d * 0.11f),
                cornerRadius = CornerRadius(d * 0.02f, d * 0.02f)
            )
        }

        // Brushed cobalt-steel rounded robot head chassis
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF60A5FA), Color(0xFF2563EB))
            ),
            topLeft = Offset(botCenter.x - boxW * 0.5f, botCenter.y - boxH * 0.5f),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(d * 0.09f, d * 0.09f)
        )

        // Dark glossy OLED visor screen
        val visorW = boxW * 0.80f
        val visorH = boxH * 0.66f
        drawRoundRect(
            color = Color(0xFF0A111D),
            topLeft = Offset(botCenter.x - visorW * 0.5f, botCenter.y - visorH * 0.5f),
            size = Size(visorW, visorH),
            cornerRadius = CornerRadius(d * 0.055f, d * 0.055f)
        )

        // Animated expressive cyan eyes (glancing & blinking happily)
        val glanceX = sin(t * 0.9f) * d * 0.016f
        val blink = (t % 3.8f) > 3.58f
        for (side in listOf(-1f, 1f)) {
            val ex = botCenter.x + side * visorW * 0.25f + glanceX
            val ey = botCenter.y - visorH * 0.12f
            if (blink) {
                drawLine(
                    color = Color(0xFF52FFB8),
                    start = Offset(ex - d * 0.035f, ey),
                    end = Offset(ex + d * 0.035f, ey),
                    strokeWidth = d * 0.014f,
                    cap = StrokeCap.Round
                )
            } else {
                drawRoundRect(
                    color = Color(0xFF52FFB8),
                    topLeft = Offset(ex - d * 0.032f, ey - d * 0.044f),
                    size = Size(d * 0.064f, d * 0.088f),
                    cornerRadius = CornerRadius(d * 0.025f, d * 0.025f)
                )
                drawCircle(
                    color = Color.White,
                    radius = d * 0.011f,
                    center = Offset(ex - d * 0.010f, ey - d * 0.018f)
                )
            }
        }

        // Playful animated equalizer smile bars
        val bars = 5
        val barStartX = botCenter.x - d * 0.09f
        for (b in 0 until bars) {
            val bx = barStartX + b * d * 0.045f
            val bh = d * (0.018f + 0.022f * ((sin(t * 5.2f + b * 0.9f) + 1f) * 0.5f))
            val by = botCenter.y + visorH * 0.22f - bh * 0.5f
            drawRoundRect(
                color = Color(0xFF5CE1E6),
                topLeft = Offset(bx - d * 0.010f, by),
                size = Size(d * 0.020f, bh),
                cornerRadius = CornerRadius(d * 0.008f, d * 0.008f)
            )
        }
    }
}

@Composable
internal fun CloudDinoFace(phaseProvider: () -> Float) {
    val crestPath = remember { Path() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val d = size.minDimension
        val t = phaseProvider()

        // Cheerful pastel storybook sky
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFCDEFFD), Color(0xFFE6F6FF), Color(0xFFD8F3DC))
            )
        )

        // Drifting fluffy cartoon clouds
        for (c in 0..1) {
            val cloudX = ((w * 0.25f + c * w * 0.55f + t * d * 0.04f) % (w * 1.3f)) - w * 0.15f
            val cloudY = h * (0.24f + c * 0.10f)
            drawCircle(color = Color.White, radius = d * 0.068f, center = Offset(cloudX, cloudY))
            drawCircle(color = Color.White, radius = d * 0.052f, center = Offset(cloudX - d * 0.052f, cloudY + d * 0.012f))
            drawCircle(color = Color.White, radius = d * 0.054f, center = Offset(cloudX + d * 0.055f, cloudY + d * 0.010f))
        }

        // Bouncing cheerful mint-green chibi dinosaur
        val bounce = kotlin.math.abs(sin(t * 2.6f)) * d * 0.032f
        val dinoC = Offset(cx, h * 0.67f - bounce)

        // Rounded coral dinosaur spine plates along upper left curve
        for (p in 0 until 4) {
            val ang = (-165f + p * 22f) * PI.toFloat() / 180f
            val px = dinoC.x + d * 0.22f * cos(ang)
            val py = dinoC.y + d * 0.21f * sin(ang)
            crestPath.reset()
            crestPath.addOval(
                androidx.compose.ui.geometry.Rect(
                    px - d * 0.032f,
                    py - d * 0.032f,
                    px + d * 0.032f,
                    py + d * 0.032f
                )
            )
            drawPath(path = crestPath, color = Color(0xFFFB7185))
        }

        // Chubby mint-green body & head silhouette
        drawCircle(color = Color(0xFF34D399), radius = d * 0.21f, center = dinoC)
        drawOval(
            color = Color(0xFFA7F3D0),
            topLeft = Offset(dinoC.x - d * 0.13f, dinoC.y + d * 0.04f),
            size = Size(d * 0.26f, d * 0.16f)
        )

        // Happy blinking big cartoon dino eyes & rosy cheeks
        val blink = (t % 3.5f) > 3.34f
        for (side in listOf(-1f, 1f)) {
            val ex = dinoC.x + side * d * 0.082f
            val ey = dinoC.y - d * 0.045f
            if (blink) {
                drawLine(
                    color = Color(0xFF1F2937),
                    start = Offset(ex - d * 0.024f, ey),
                    end = Offset(ex + d * 0.024f, ey),
                    strokeWidth = d * 0.010f,
                    cap = StrokeCap.Round
                )
            } else {
                drawCircle(color = Color(0xFF1F2937), radius = d * 0.026f, center = Offset(ex, ey))
                drawCircle(
                    color = Color.White,
                    radius = d * 0.009f,
                    center = Offset(ex - d * 0.008f, ey - d * 0.008f)
                )
            }
            drawOval(
                color = Color(0x88FB7185),
                topLeft = Offset(ex + side * d * 0.012f - d * 0.026f, ey + d * 0.038f),
                size = Size(d * 0.052f, d * 0.026f)
            )
        }

        // Cheerful wide smile
        drawArc(
            color = Color(0xFF1F2937),
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(dinoC.x - d * 0.055f, dinoC.y - d * 0.025f),
            size = Size(d * 0.110f, d * 0.065f),
            style = Stroke(width = d * 0.010f, cap = StrokeCap.Round)
        )
    }
}
