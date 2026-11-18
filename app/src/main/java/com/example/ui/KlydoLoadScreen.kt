package com.example.ui

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.R
import com.example.ui.theme.CelestialTeal
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val LightLoadRingBorder = Color(0xFFE0DBD1)
private val LightLoadTickMuted = Color(0xFF8A847A)
private val LightLoadTagPill = Color(0xFFF5F1E9)
private val LightLoadTagBorder = Color(0xFFE5DFD3)
private val LightLoadTagText = Color(0xFFAA4E28)
private val LightLoadTrack = Color(0xFFE8E4DC)

@Composable
fun KlydoLoadScreen(
    onFinished: () -> Unit,
    displayDurationMs: Long = 1650L,
    animateMotion: Boolean = true
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevLightStatus = controller?.isAppearanceLightStatusBars ?: false
        val prevLightNav = controller?.isAppearanceLightNavigationBars ?: false
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            controller?.isAppearanceLightStatusBars = prevLightStatus
            controller?.isAppearanceLightNavigationBars = prevLightNav
        }
    }

    var revealed by remember { mutableStateOf(!animateMotion) }
    val loadProgress = remember { Animatable(if (animateMotion) 0f else 1f) }

    val emblemScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.86f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_emblem_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "splash_content_alpha"
    )

    val sweepAngle = if (animateMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "splash_clock_spin")
        val animValue by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "splash_hand_angle"
        )
        animValue
    } else {
        48f
    }

    LaunchedEffect(displayDurationMs) {
        revealed = true
        if (animateMotion) {
            loadProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = displayDurationMs.toInt().coerceAtLeast(300),
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            delay(displayDurationMs)
        }
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp, vertical = 28.dp)
            .testTag("app_load_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFinished
                )
                .testTag("load_screen_skip_backdrop")
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Kinetic KlydoDay Emblem
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .scale(emblemScale),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.44f

                    // Soft warm halo on white
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                TerracottaAccent.copy(alpha = 0.14f),
                                SaffronAmber.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.52f
                        ),
                        center = center,
                        radius = size.minDimension * 0.52f
                    )

                    // Precision outer ring
                    drawCircle(
                        color = LightLoadRingBorder,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Inner accent arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                TerracottaAccent,
                                SaffronAmber,
                                CelestialTeal,
                                TerracottaAccent
                            ),
                            center = center
                        ),
                        startAngle = sweepAngle - 90f,
                        sweepAngle = 220f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 12 precision hour pips
                    for (i in 0 until 12) {
                        val angleRad = (i * 30.0 - 90.0) * PI / 180.0
                        val tickInner = radius * 0.80f
                        val tickOuter = radius * 0.89f
                        drawLine(
                            color = if (i % 3 == 0) TerracottaAccent else LightLoadTickMuted.copy(alpha = 0.65f),
                            start = Offset(
                                center.x + (cos(angleRad) * tickInner).toFloat(),
                                center.y + (sin(angleRad) * tickInner).toFloat()
                            ),
                            end = Offset(
                                center.x + (cos(angleRad) * tickOuter).toFloat(),
                                center.y + (sin(angleRad) * tickOuter).toFloat()
                            ),
                            strokeWidth = if (i % 3 == 0) 2.2.dp.toPx() else 1.3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Hour & kinetic minute hand
                    val hourRad = (-60.0) * PI / 180.0
                    drawLine(
                        color = ObsidianChassis,
                        start = center,
                        end = Offset(
                            center.x + (cos(hourRad) * radius * 0.44f).toFloat(),
                            center.y + (sin(hourRad) * radius * 0.44f).toFloat()
                        ),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    val minRad = ((sweepAngle - 90f).toDouble()) * PI / 180.0
                    drawLine(
                        color = TerracottaAccent,
                        start = center,
                        end = Offset(
                            center.x + (cos(minRad) * radius * 0.68f).toFloat(),
                            center.y + (sin(minRad) * radius * 0.68f).toFloat()
                        ),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    drawCircle(
                        color = ObsidianChassis,
                        radius = 4.5.dp.toPx(),
                        center = center
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = ObsidianChassis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("load_screen_app_name")
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = LightLoadTagPill,
                    modifier = Modifier.border(1.dp, LightLoadTagBorder, RoundedCornerShape(50))
                ) {
                    Text(
                        text = stringResource(R.string.splash_tagline),
                        style = MaterialTheme.typography.labelMedium,
                        color = LightLoadTagText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .alpha(contentAlpha)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LightLoadTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(loadProgress.value.coerceIn(0.08f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TerracottaAccent, SaffronAmber)
                            )
                        )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TerracottaAccent)
                )
                Text(
                    text = stringResource(R.string.developed_by_credit),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    ),
                    color = ObsidianChassis.copy(alpha = 0.86f),
                    modifier = Modifier.testTag("load_screen_developer_credit")
                )
            }
        }
    }
}
