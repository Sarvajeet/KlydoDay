package com.example.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import androidx.core.graphics.withRotation
import androidx.core.graphics.withSave
import com.example.data.model.KlydoArtMeta
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal class KlydoWallpaperArtPainter {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val reusablePath = Path()
    private val reusableRect = RectF()
    private val srcRect = Rect()
    private val dstRect = RectF()

    fun drawCustomVideoFrames(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        frames: List<Bitmap>,
        phaseSec: Float,
        clipDurationSec: Float
    ) {
        if (frames.isEmpty()) {
            fillPaint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf("#1D2330".toColorInt(), "#090B0F".toColorInt()),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius, fillPaint)
            fillPaint.shader = null
            return
        }

        val duration = clipDurationSec.coerceIn(1f, 10f)
        val norm = (phaseSec % duration) / duration
        val exactIndex = norm * frames.size
        val idx0 = exactIndex.toInt().coerceIn(0, frames.lastIndex)
        val idx1 = (idx0 + 1) % frames.size
        val blend = exactIndex - idx0

        dstRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        val bmp0 = frames[idx0]
        srcRect.set(0, 0, bmp0.width, bmp0.height)
        bitmapPaint.alpha = 255
        canvas.drawBitmap(bmp0, srcRect, dstRect, bitmapPaint)

        if (idx1 != idx0 && blend > 0.01f) {
            val bmp1 = frames[idx1]
            srcRect.set(0, 0, bmp1.width, bmp1.height)
            bitmapPaint.alpha = (blend * 255f).toInt().coerceIn(0, 255)
            canvas.drawBitmap(bmp1, srcRect, dstRect, bitmapPaint)
            bitmapPaint.alpha = 255
        }
    }

    fun drawPrebuiltArt(
        canvas: Canvas,
        artMeta: KlydoArtMeta,
        cx: Float,
        cy: Float,
        radius: Float,
        phaseSec: Float,
        minuteOfDay: Float
    ) {
        when (artMeta.id) {
            // ABSTRACT
            "bauhaus_orbit" -> drawBauhausOrbit(canvas, cx, cy, radius, phaseSec)
            "retro_grid" -> drawRetroGrid(canvas, cx, cy, radius, phaseSec)
            "silk_lissajous" -> drawSilkLissajous(canvas, cx, cy, radius, phaseSec)
            "fluid_aurora" -> drawFluidAurora(canvas, cx, cy, radius, phaseSec)
            "chromatic_prism" -> drawChromaticPrism(canvas, cx, cy, radius, phaseSec)
            "kinetic_moire" -> drawKineticMoire(canvas, cx, cy, radius, phaseSec)

            // NATURE
            "bioluminescent_jelly" -> drawBioluminescentJelly(canvas, cx, cy, radius, phaseSec)
            "sunburst_horizon" -> drawSunburstHorizon(canvas, cx, cy, radius, phaseSec, minuteOfDay)
            "botanical_pulse" -> drawBotanicalPulse(canvas, cx, cy, radius, phaseSec)
            "koi_lotus_pond" -> drawKoiLotusPond(canvas, cx, cy, radius, phaseSec)
            "firefly_forest" -> drawFireflyForest(canvas, cx, cy, radius, phaseSec)
            "tidal_nocturne" -> drawTidalNocturne(canvas, cx, cy, radius, phaseSec)

            // ARCHITECTURE
            "celestial_observatory" -> drawCelestialObservatory(canvas, cx, cy, radius, phaseSec)
            "kinetic_monolith" -> drawKineticMonolith(canvas, cx, cy, radius, phaseSec)
            "rose_cathedral" -> drawRoseCathedral(canvas, cx, cy, radius, phaseSec)
            "art_deco_spire" -> drawArtDecoSpire(canvas, cx, cy, radius, phaseSec)
            "escher_atrium" -> drawEscherAtrium(canvas, cx, cy, radius, phaseSec)

            // MANDALA
            "sacred_lotus_mandala" -> drawSacredLotusMandala(canvas, cx, cy, radius, phaseSec)
            "kaleidoscope_bloom" -> drawKaleidoscopeBloom(canvas, cx, cy, radius, phaseSec)
            "celestial_astrolabe" -> drawCelestialAstrolabe(canvas, cx, cy, radius, phaseSec)
            "paper_cut_mandala" -> drawPaperCutMandala(canvas, cx, cy, radius, phaseSec)

            // CARTOONS
            "sleepy_neko" -> drawSleepyNeko(canvas, cx, cy, radius, phaseSec)
            "astro_pup" -> drawAstroPup(canvas, cx, cy, radius, phaseSec)
            "retro_bot" -> drawRetroBot(canvas, cx, cy, radius, phaseSec)
            "tiny_dino_cloud" -> drawTinyDinoCloud(canvas, cx, cy, radius, phaseSec)

            // FUN
            "rubber_duck_float" -> drawRubberDuckFloat(canvas, cx, cy, radius, phaseSec)
            "disco_vinyl" -> drawDiscoVinyl(canvas, cx, cy, radius, phaseSec)
            "cosmic_boba" -> drawCosmicBoba(canvas, cx, cy, radius, phaseSec)
            "paper_plane_sky" -> drawPaperPlaneSky(canvas, cx, cy, radius, phaseSec)

            // MINIMAL
            "minimal_atelier" -> drawMinimalAtelier(canvas, cx, cy, radius, phaseSec)
            "zen_ripple" -> drawZenRipple(canvas, cx, cy, radius, phaseSec)
            "nordic_pendule" -> drawNordicPendule(canvas, cx, cy, radius, phaseSec)
            "eclipse_halo" -> drawEclipseHalo(canvas, cx, cy, radius, phaseSec)

            else -> drawBioluminescentJelly(canvas, cx, cy, radius, phaseSec)
        }
    }

    private fun drawBioluminescentJelly(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = Color.BLACK
        canvas.drawCircle(cx, cy, r, fillPaint)

        val bobY = cy - r * 0.14f + sin(t * 1.15f) * r * 0.055f
        val bellW = r * (0.48f + 0.045f * cos(t * 1.15f))
        val bellH = r * 0.34f

        // Bioluminescent aura
        fillPaint.shader = RadialGradient(
            cx, bobY, r * 0.85f,
            intArrayOf("#669D00DF".toColorInt(), "#3328F7E6".toColorInt(), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, bobY, r * 0.85f, fillPaint)
        fillPaint.shader = null

        // Flowing translucent tentacles
        val tentacleColors = intArrayOf(
            "#AA9D00DF".toColorInt(),
            "#CC28F7E6".toColorInt(),
            "#BBE038B8".toColorInt(),
            "#996E44FF".toColorInt(),
            "#BB28F7E6".toColorInt()
        )
        for (i in -3..3) {
            reusablePath.reset()
            val startX = cx + i * bellW * 0.21f
            val startY = bobY + bellH * 0.35f
            reusablePath.moveTo(startX, startY)
            val len = r * (0.64f - kotlin.math.abs(i) * 0.06f)
            for (seg in 1..6) {
                val frac = seg / 6f
                val wave = sin(t * 2.1f - frac * 3.4f + i * 0.8f) * r * 0.085f * frac
                reusablePath.lineTo(startX + wave + i * r * 0.02f * frac, startY + len * frac)
            }
            strokePaint.color = tentacleColors[(i + 3) % tentacleColors.size]
            strokePaint.strokeWidth = r * (0.032f - kotlin.math.abs(i) * 0.003f)
            canvas.drawPath(reusablePath, strokePaint)
        }

        // Translucent glowing bell
        reusableRect.set(cx - bellW, bobY - bellH, cx + bellW, bobY + bellH * 0.85f)
        fillPaint.shader = LinearGradient(
            cx - bellW, bobY - bellH, cx + bellW, bobY + bellH,
            intArrayOf("#E6FF48C4".toColorInt(), "#E69D00DF".toColorInt(), "#DD28F7E6".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawArc(reusableRect, 180f, 180f, true, fillPaint)
        fillPaint.shader = null

        // Internal luminous ribs
        strokePaint.color = "#88FFFFFF".toColorInt()
        strokePaint.strokeWidth = r * 0.015f
        for (k in -2..2) {
            canvas.drawLine(
                cx + k * bellW * 0.14f, bobY - bellH * 0.82f,
                cx + k * bellW * 0.32f, bobY,
                strokePaint
            )
        }
    }

    private fun drawBauhausOrbit(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#131419".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)

        // Asymmetric primary arcs
        strokePaint.strokeWidth = r * 0.13f
        strokePaint.color = "#E06B3E".toColorInt()
        reusableRect.set(cx - r * 0.64f, cy - r * 0.64f, cx + r * 0.64f, cy + r * 0.64f)
        canvas.drawArc(reusableRect, t * 18f, 130f, false, strokePaint)

        strokePaint.color = "#E8A838".toColorInt()
        strokePaint.strokeWidth = r * 0.09f
        reusableRect.set(cx - r * 0.44f, cy - r * 0.44f, cx + r * 0.44f, cy + r * 0.44f)
        canvas.drawArc(reusableRect, -t * 24f + 160f, 150f, false, strokePaint)

        strokePaint.color = "#3E6BFF".toColorInt()
        strokePaint.strokeWidth = r * 0.055f
        reusableRect.set(cx - r * 0.82f, cy - r * 0.82f, cx + r * 0.82f, cy + r * 0.82f)
        canvas.drawArc(reusableRect, t * 12f + 220f, 95f, false, strokePaint)

        // Orbiting Bauhaus sphere
        val ang = t * 0.9f
        fillPaint.color = "#F5EFE4".toColorInt()
        canvas.drawCircle(cx + r * 0.64f * cos(ang), cy + r * 0.64f * sin(ang), r * 0.075f, fillPaint)
    }

    private fun drawRetroGrid(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.shader = LinearGradient(
            cx, cy - r, cx, cy + r,
            intArrayOf("#090418".toColorInt(), "#1D0C3A".toColorInt(), "#090418".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        // Neon sun disc
        fillPaint.shader = LinearGradient(
            cx, cy - r * 0.55f, cx, cy + r * 0.1f,
            intArrayOf("#FFE066".toColorInt(), "#FF47B8".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy - r * 0.15f, r * 0.42f, fillPaint)
        fillPaint.shader = null

        // Perspective horizon grid
        strokePaint.color = "#8828F7E6".toColorInt()
        strokePaint.strokeWidth = r * 0.014f
        val horizonY = cy + r * 0.05f
        for (i in -5..5) {
            canvas.drawLine(cx + i * r * 0.08f, horizonY, cx + i * r * 0.34f, cy + r, strokePaint)
        }
        val scroll = (t * 0.45f) % 1f
        for (row in 0..6) {
            val f = ((row + scroll) / 7f)
            val y = horizonY + (r * 0.95f) * f * f
            canvas.drawLine(cx - r, y, cx + r, y, strokePaint)
        }
    }

    private fun drawSilkLissajous(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#0C0E14".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)

        for (layer in 0..2) {
            reusablePath.reset()
            val phaseShift = layer * 0.4f + t * 0.35f
            val steps = 72
            for (s in 0..steps) {
                val u = (s / steps.toFloat()) * 2f * PI.toFloat()
                val px = cx + r * 0.72f * sin(3f * u + phaseShift)
                val py = cy + r * 0.72f * sin(4f * u + t * 0.2f)
                if (s == 0) reusablePath.moveTo(px, py) else reusablePath.lineTo(px, py)
            }
            strokePaint.color = when (layer) {
                0 -> "#99F4EDE2".toColorInt()
                1 -> "#66D6C3A5".toColorInt()
                else -> "#55E2A898".toColorInt()
            }
            strokePaint.strokeWidth = r * (0.024f - layer * 0.005f)
            canvas.drawPath(reusablePath, strokePaint)
        }
        val nodeU = t * 0.85f
        fillPaint.color = "#FFE2A898".toColorInt()
        canvas.drawCircle(
            cx + r * 0.72f * sin(3f * nodeU + t * 0.35f),
            cy + r * 0.72f * sin(4f * nodeU + t * 0.2f),
            r * 0.05f,
            fillPaint
        )
    }

    private fun drawFluidAurora(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.shader = LinearGradient(
            cx, cy - r, cx, cy + r,
            intArrayOf("#030A14".toColorInt(), "#091B2C".toColorInt(), "#04080E".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        val ribbons = intArrayOf("#66A4FFEE".toColorInt(), "#5570E1F5".toColorInt(), "#559D6BFF".toColorInt())
        for (band in 0..2) {
            reusablePath.reset()
            val baseY = cy - r * 0.25f + band * r * 0.22f
            for (xStep in -10..10) {
                val fx = xStep / 10f
                val px = cx + fx * r * 0.95f
                val py = baseY + sin(fx * 3.2f + t * (0.7f + band * 0.2f) + band) * r * 0.16f
                if (xStep == -10) reusablePath.moveTo(px, py) else reusablePath.lineTo(px, py)
            }
            strokePaint.color = ribbons[band]
            strokePaint.strokeWidth = r * 0.24f
            canvas.drawPath(reusablePath, strokePaint)
        }
    }

    private fun drawChromaticPrism(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#08090C".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)

        val spectral = intArrayOf(
            "#AAFF4D6D".toColorInt(), "#AAFFB703".toColorInt(), "#AA06D6A0".toColorInt(),
            "#AA28F7E6".toColorInt(), "#AA8338EC".toColorInt()
        )
        canvas.withRotation(t * 14f, cx, cy) {
            for (i in spectral.indices) {
                val ang = (i - 2) * 0.18f
                strokePaint.color = spectral[i]
                strokePaint.strokeWidth = r * 0.08f
                canvas.drawLine(cx, cy, cx + r * cos(ang), cy + r * sin(ang), strokePaint)
            }
            reusablePath.reset()
            val triR = r * 0.42f
            for (k in 0..2) {
                val a = (k * 120f - 90f) * PI.toFloat() / 180f
                val px = cx + triR * cos(a)
                val py = cy + triR * sin(a)
                if (k == 0) reusablePath.moveTo(px, py) else reusablePath.lineTo(px, py)
            }
            reusablePath.close()
            fillPaint.color = "#33FFFFFF".toColorInt()
            canvas.drawPath(reusablePath, fillPaint)
            strokePaint.color = Color.WHITE
            strokePaint.strokeWidth = r * 0.025f
            canvas.drawPath(reusablePath, strokePaint)
        }
    }

    private fun drawKineticMoire(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#0C0E16".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)

        val ox1 = cx + sin(t * 0.7f) * r * 0.14f
        val oy1 = cy + cos(t * 0.5f) * r * 0.10f
        val ox2 = cx - sin(t * 0.6f) * r * 0.14f
        val oy2 = cy - cos(t * 0.75f) * r * 0.10f
        strokePaint.strokeWidth = r * 0.015f
        for (i in 1..10) {
            val rr = r * (i / 10f) * 0.92f
            strokePaint.color = "#669BB7FF".toColorInt()
            canvas.drawCircle(ox1, oy1, rr, strokePaint)
            strokePaint.color = "#66FF7E6B".toColorInt()
            canvas.drawCircle(ox2, oy2, rr, strokePaint)
        }
    }

    private fun drawSunburstHorizon(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float, minuteOfDay: Float) {
        val dayFrac = sin(((minuteOfDay / 1440f) * PI).toFloat()).coerceIn(0.15f, 1f)
        fillPaint.shader = LinearGradient(
            cx, cy - r, cx, cy + r,
            intArrayOf("#1D2448".toColorInt(), "#D96A3B".toColorInt(), "#FFB238".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        val sunY = cy + r * (0.28f - dayFrac * 0.35f) + sin(t * 0.6f) * r * 0.02f
        fillPaint.color = "#FFF3D6".toColorInt()
        canvas.drawCircle(cx, sunY, r * 0.30f, fillPaint)

        fillPaint.color = "#D8161924".toColorInt()
        canvas.drawRect(cx - r, cy + r * 0.18f, cx + r, cy + r, fillPaint)
    }

    private fun drawBotanicalPulse(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#0C1814".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        val breath = 0.90f + 0.10f * sin(t * 1.4f)
        canvas.withRotation(t * 8f, cx, cy) {
            for (i in 0 until 8) {
                canvas.withRotation(i * 45f, cx, cy) {
                    reusableRect.set(
                        cx - r * 0.16f * breath,
                        cy - r * 0.78f * breath,
                        cx + r * 0.16f * breath,
                        cy - r * 0.10f
                    )
                    fillPaint.color = if (i % 2 == 0) "#AA2E9F72".toColorInt() else "#99A8F0C6".toColorInt()
                    canvas.drawOval(reusableRect, fillPaint)
                }
            }
        }
        fillPaint.color = "#FFE8A838".toColorInt()
        canvas.drawCircle(cx, cy, r * 0.12f, fillPaint)
    }

    private fun drawKoiLotusPond(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf("#0E3238".toColorInt(), "#06161B".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        // Ripples
        strokePaint.color = "#446FD4B8".toColorInt()
        strokePaint.strokeWidth = r * 0.015f
        val rip = (t * 0.35f) % 1f
        canvas.drawCircle(cx, cy, r * (0.2f + rip * 0.7f), strokePaint)

        // Two orbiting koi silhouettes
        for (k in 0..1) {
            val baseA = t * 0.65f + k * PI.toFloat()
            val kx = cx + r * 0.45f * cos(baseA)
            val ky = cy + r * 0.45f * sin(baseA)
            val heading = Math.toDegrees(baseA.toDouble()).toFloat() + 90f
            canvas.withRotation(heading, kx, ky) {
                fillPaint.color = if (k == 0) "#FF653D".toColorInt() else "#FFB703".toColorInt()
                reusableRect.set(kx - r * 0.08f, ky - r * 0.22f, kx + r * 0.08f, ky + r * 0.22f)
                canvas.drawOval(reusableRect, fillPaint)
            }
        }
        // Lotus pad
        fillPaint.color = "#AA2A7B5B".toColorInt()
        canvas.drawCircle(cx - r * 0.36f, cy - r * 0.28f, r * 0.22f, fillPaint)
    }

    private fun drawFireflyForest(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.shader = LinearGradient(
            cx, cy - r, cx, cy + r,
            intArrayOf("#08111E".toColorInt(), "#102826".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        for (i in 0 until 14) {
            val a = i * 1.9f + t * (0.35f + (i % 3) * 0.12f)
            val rad = r * (0.22f + ((i * 37) % 62) / 100f)
            val fx = cx + rad * cos(a)
            val fy = cy + rad * sin(a * 0.8f)
            val pulse = 0.4f + 0.6f * ((sin(t * 2.5f + i) + 1f) * 0.5f)
            fillPaint.color = Color.argb((pulse * 220).toInt(), 255, 224, 120)
            canvas.drawCircle(fx, fy, r * 0.036f * (0.7f + pulse * 0.6f), fillPaint)
        }
    }

    private fun drawTidalNocturne(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.shader = LinearGradient(
            cx, cy - r, cx, cy + r,
            intArrayOf("#081426".toColorInt(), "#0D2B3E".toColorInt(), "#05101B".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        fillPaint.color = "#FFF2D59B".toColorInt()
        canvas.drawCircle(cx - r * 0.25f, cy - r * 0.35f, r * 0.18f, fillPaint)

        for (w in 0..3) {
            reusablePath.reset()
            val yBase = cy + r * (0.05f + w * 0.18f)
            for (step in -8..8) {
                val fx = step / 8f
                val px = cx + fx * r
                val py = yBase + sin(fx * 3.5f + t * (1.1f + w * 0.2f) + w) * r * 0.06f
                if (step == -8) reusablePath.moveTo(px, py) else reusablePath.lineTo(px, py)
            }
            strokePaint.color = if (w == 0) "#AA78C2D4".toColorInt() else "#773A89A8".toColorInt()
            strokePaint.strokeWidth = r * 0.04f
            canvas.drawPath(reusablePath, strokePaint)
        }
    }

    private fun drawCelestialObservatory(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf("#13204E".toColorInt(), "#060B1E".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        strokePaint.color = "#66F5C451".toColorInt()
        strokePaint.strokeWidth = r * 0.018f
        canvas.drawCircle(cx, cy, r * 0.78f, strokePaint)
        canvas.drawCircle(cx, cy, r * 0.54f, strokePaint)
        canvas.drawCircle(cx, cy, r * 0.30f, strokePaint)

        fillPaint.color = "#FFF5C451".toColorInt()
        canvas.drawCircle(cx + r * 0.78f * cos(t * 0.4f), cy + r * 0.78f * sin(t * 0.4f), r * 0.05f, fillPaint)
        fillPaint.color = "#FF3E6BFF".toColorInt()
        canvas.drawCircle(cx + r * 0.54f * cos(-t * 0.75f), cy + r * 0.54f * sin(-t * 0.75f), r * 0.065f, fillPaint)
    }

    private fun drawKineticMonolith(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#12151C".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        val tilt = sin(t * 0.9f) * 18f
        canvas.withRotation(tilt, cx, cy - r * 0.25f) {
            strokePaint.color = "#8FAEC4".toColorInt()
            strokePaint.strokeWidth = r * 0.02f
            canvas.drawLine(cx - r * 0.58f, cy - r * 0.1f, cx + r * 0.58f, cy - r * 0.1f, strokePaint)
            fillPaint.color = "#D98A4E".toColorInt()
            canvas.drawCircle(cx - r * 0.58f, cy + r * 0.18f, r * 0.15f, fillPaint)
            fillPaint.color = "#F2E9DC".toColorInt()
            canvas.drawCircle(cx + r * 0.58f, cy + r * 0.10f, r * 0.11f, fillPaint)
        }
    }

    private fun drawRoseCathedral(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#0C0910".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        val jewelColors = intArrayOf(
            "#AAD93B58".toColorInt(), "#AAE8B84E".toColorInt(), "#AA3D7EFF".toColorInt(), "#AA2EC4B6".toColorInt()
        )
        canvas.withRotation(t * 7f, cx, cy) {
            for (i in 0 until 12) {
                canvas.withRotation(i * 30f, cx, cy) {
                    fillPaint.color = jewelColors[i % jewelColors.size]
                    reusableRect.set(cx - r * 0.10f, cy - r * 0.80f, cx + r * 0.10f, cy - r * 0.22f)
                    canvas.drawOval(reusableRect, fillPaint)
                }
            }
        }
        strokePaint.color = "#FFE8B84E".toColorInt()
        strokePaint.strokeWidth = r * 0.025f
        canvas.drawCircle(cx, cy, r * 0.82f, strokePaint)
    }

    private fun drawArtDecoSpire(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#0E1016".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        strokePaint.color = "#D4A853".toColorInt()
        strokePaint.strokeWidth = r * 0.024f
        for (i in 1..4) {
            val archR = r * (i * 0.21f)
            reusableRect.set(cx - archR, cy + r * 0.35f - archR * 1.3f, cx + archR, cy + r * 0.9f)
            canvas.drawArc(reusableRect, 180f, 180f, false, strokePaint)
        }
        val sweep = sin(t * 0.8f) * 28f
        canvas.withRotation(sweep, cx, cy + r * 0.7f) {
            fillPaint.color = "#2245B8AC".toColorInt()
            canvas.drawRect(cx - r * 0.08f, cy - r, cx + r * 0.08f, cy + r * 0.7f, fillPaint)
        }
    }

    private fun drawEscherAtrium(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#131722".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        val lift = sin(t * 1.1f) * r * 0.06f
        for (row in -1..1) {
            val bx = cx + row * r * 0.36f
            val by = cy + (if (row == 0) -lift else lift)
            fillPaint.color = "#E57A54".toColorInt()
            canvas.drawRect(bx - r * 0.14f, by - r * 0.24f, bx, by + r * 0.24f, fillPaint)
            fillPaint.color = "#7CA1BD".toColorInt()
            canvas.drawRect(bx, by - r * 0.24f, bx + r * 0.14f, by + r * 0.24f, fillPaint)
        }
    }

    private fun drawSacredLotusMandala(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf("#281024".toColorInt(), "#0D0611".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        canvas.withRotation(t * 10f, cx, cy) {
            strokePaint.color = "#FFE5A93C".toColorInt()
            strokePaint.strokeWidth = r * 0.022f
            for (i in 0 until 12) {
                canvas.withRotation(i * 30f, cx, cy) {
                    reusableRect.set(cx - r * 0.13f, cy - r * 0.80f, cx + r * 0.13f, cy)
                    canvas.drawOval(reusableRect, strokePaint)
                }
            }
        }
        canvas.withRotation(-t * 14f, cx, cy) {
            fillPaint.color = "#88E85D75".toColorInt()
            for (i in 0 until 8) {
                canvas.withRotation(i * 45f, cx, cy) {
                    reusableRect.set(cx - r * 0.10f, cy - r * 0.46f, cx + r * 0.10f, cy)
                    canvas.drawOval(reusableRect, fillPaint)
                }
            }
        }
    }

    private fun drawKaleidoscopeBloom(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#080B12".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        val colors = intArrayOf("#BB5EEAD4".toColorInt(), "#BBF472B6".toColorInt(), "#BBFACC15".toColorInt())
        canvas.withRotation(t * 16f, cx, cy) {
            val scale = 0.85f + 0.15f * sin(t * 1.4f)
            for (i in 0 until 12) {
                canvas.withRotation(i * 30f, cx, cy) {
                    fillPaint.color = colors[i % colors.size]
                    reusablePath.reset()
                    reusablePath.moveTo(cx, cy)
                    reusablePath.lineTo(cx + r * 0.18f * scale, cy - r * 0.72f * scale)
                    reusablePath.lineTo(cx - r * 0.18f * scale, cy - r * 0.72f * scale)
                    reusablePath.close()
                    canvas.drawPath(reusablePath, fillPaint)
                }
            }
        }
    }

    private fun drawCelestialAstrolabe(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#08142C".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        strokePaint.color = "#C99D44".toColorInt()
        strokePaint.strokeWidth = r * 0.025f
        canvas.drawCircle(cx, cy, r * 0.84f, strokePaint)
        canvas.withRotation(t * 9f, cx, cy) {
            canvas.drawCircle(cx + r * 0.14f, cy - r * 0.10f, r * 0.56f, strokePaint)
            canvas.drawLine(cx - r * 0.84f, cy, cx + r * 0.84f, cy, strokePaint)
        }
        fillPaint.color = "#3D7EFF".toColorInt()
        canvas.drawCircle(cx + r * 0.65f * cos(t * 0.5f), cy + r * 0.65f * sin(t * 0.5f), r * 0.08f, fillPaint)
    }

    private fun drawPaperCutMandala(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#F4ECE0".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        val rings = intArrayOf("#D8C3A5".toColorInt(), "#8E9AAF".toColorInt(), "#D64933".toColorInt())
        for (layer in 0..2) {
            canvas.withRotation((if (layer % 2 == 0) 1f else -1f) * t * (6f + layer * 3f), cx, cy) {
                fillPaint.color = rings[layer]
                val rad = r * (0.76f - layer * 0.22f)
                for (i in 0 until 8) {
                    canvas.withRotation(i * 45f, cx, cy) {
                        canvas.drawCircle(cx, cy - rad * 0.55f, rad * 0.36f, fillPaint)
                    }
                }
            }
        }
    }

    private fun drawSleepyNeko(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#1B1526".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        val bob = sin(t * 1.4f) * r * 0.04f
        // Ears
        fillPaint.color = "#FFF4EC".toColorInt()
        reusablePath.reset()
        reusablePath.moveTo(cx - r * 0.42f, cy - r * 0.12f + bob)
        reusablePath.lineTo(cx - r * 0.50f, cy - r * 0.56f + bob)
        reusablePath.lineTo(cx - r * 0.12f, cy - r * 0.36f + bob)
        reusablePath.close()
        canvas.drawPath(reusablePath, fillPaint)

        reusablePath.reset()
        reusablePath.moveTo(cx + r * 0.42f, cy - r * 0.12f + bob)
        reusablePath.lineTo(cx + r * 0.50f, cy - r * 0.56f + bob)
        reusablePath.lineTo(cx + r * 0.12f, cy - r * 0.36f + bob)
        reusablePath.close()
        canvas.drawPath(reusablePath, fillPaint)

        // Head
        canvas.drawCircle(cx, cy + bob, r * 0.46f, fillPaint)
        fillPaint.color = "#FF8FAB".toColorInt()
        canvas.drawCircle(cx - r * 0.22f, cy + r * 0.08f + bob, r * 0.07f, fillPaint)
        canvas.drawCircle(cx + r * 0.22f, cy + r * 0.08f + bob, r * 0.07f, fillPaint)

        // Happy closed eyes
        strokePaint.color = "#2D1E2F".toColorInt()
        strokePaint.strokeWidth = r * 0.03f
        reusableRect.set(cx - r * 0.26f, cy - r * 0.06f + bob, cx - r * 0.10f, cy + r * 0.04f + bob)
        canvas.drawArc(reusableRect, 180f, 180f, false, strokePaint)
        reusableRect.set(cx + r * 0.10f, cy - r * 0.06f + bob, cx + r * 0.26f, cy + r * 0.04f + bob)
        canvas.drawArc(reusableRect, 180f, 180f, false, strokePaint)
    }

    private fun drawAstroPup(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#081226".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        val floatY = sin(t * 1.2f) * r * 0.06f
        // Helmet ring
        strokePaint.color = "#5CE1E6".toColorInt()
        strokePaint.strokeWidth = r * 0.035f
        canvas.drawCircle(cx, cy + floatY, r * 0.55f, strokePaint)
        fillPaint.color = "#225CE1E6".toColorInt()
        canvas.drawCircle(cx, cy + floatY, r * 0.55f, fillPaint)
        // Shiba head
        fillPaint.color = "#F4A261".toColorInt()
        canvas.drawCircle(cx, cy + floatY, r * 0.34f, fillPaint)
        fillPaint.color = "#FFF8EE".toColorInt()
        canvas.drawCircle(cx, cy + r * 0.08f + floatY, r * 0.22f, fillPaint)
        // Orbiting bone dot
        val a = t * 0.9f
        fillPaint.color = "#FFD166".toColorInt()
        canvas.drawCircle(cx + r * 0.74f * cos(a), cy + r * 0.74f * sin(a), r * 0.065f, fillPaint)
    }

    private fun drawRetroBot(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#0E1624".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        val bob = sin(t * 2.2f) * r * 0.04f
        fillPaint.color = "#60A5FA".toColorInt()
        reusableRect.set(cx - r * 0.44f, cy - r * 0.36f + bob, cx + r * 0.44f, cy + r * 0.34f + bob)
        canvas.drawRoundRect(reusableRect, r * 0.14f, r * 0.14f, fillPaint)

        fillPaint.color = "#0B1220".toColorInt()
        reusableRect.set(cx - r * 0.32f, cy - r * 0.18f + bob, cx + r * 0.32f, cy + r * 0.08f + bob)
        canvas.drawRoundRect(reusableRect, r * 0.08f, r * 0.08f, fillPaint)

        fillPaint.color = "#52FFB8".toColorInt()
        canvas.drawCircle(cx - r * 0.14f, cy - r * 0.05f + bob, r * 0.06f, fillPaint)
        canvas.drawCircle(cx + r * 0.14f, cy - r * 0.05f + bob, r * 0.06f, fillPaint)

        // Glowing antenna
        fillPaint.color = "#FF5964".toColorInt()
        canvas.drawCircle(cx, cy - r * 0.56f + bob, r * 0.08f, fillPaint)
    }

    private fun drawTinyDinoCloud(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#DCEEFB".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        val bounce = kotlin.math.abs(sin(t * 2.2f)) * r * 0.10f
        fillPaint.color = "#34D399".toColorInt()
        canvas.drawCircle(cx, cy + r * 0.12f - bounce, r * 0.34f, fillPaint)
        canvas.drawCircle(cx + r * 0.22f, cy - r * 0.10f - bounce, r * 0.22f, fillPaint)
        fillPaint.color = Color.WHITE
        canvas.drawCircle(cx - r * 0.40f + sin(t * 0.4f) * r * 0.08f, cy - r * 0.45f, r * 0.18f, fillPaint)
        canvas.drawCircle(cx - r * 0.22f + sin(t * 0.4f) * r * 0.08f, cy - r * 0.48f, r * 0.22f, fillPaint)
    }

    private fun drawRubberDuckFloat(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.shader = LinearGradient(
            cx, cy - r, cx, cy + r,
            intArrayOf("#0E3B56".toColorInt(), "#1A759F".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        val bob = sin(t * 1.9f) * r * 0.06f
        fillPaint.color = "#FFE066".toColorInt()
        canvas.drawCircle(cx, cy + r * 0.12f + bob, r * 0.34f, fillPaint)
        canvas.drawCircle(cx + r * 0.18f, cy - r * 0.18f + bob, r * 0.20f, fillPaint)
        fillPaint.color = "#FF6B35".toColorInt()
        canvas.drawCircle(cx + r * 0.36f, cy - r * 0.15f + bob, r * 0.08f, fillPaint)
    }

    private fun drawDiscoVinyl(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#0C0914".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.color = "#1A1B24".toColorInt()
        canvas.drawCircle(cx, cy, r * 0.84f, fillPaint)
        strokePaint.color = "#4470D6FF".toColorInt()
        strokePaint.strokeWidth = r * 0.015f
        canvas.drawCircle(cx, cy, r * 0.68f, strokePaint)
        canvas.drawCircle(cx, cy, r * 0.52f, strokePaint)
        canvas.withRotation(t * 45f, cx, cy) {
            fillPaint.color = "#FF70A6".toColorInt()
            canvas.drawCircle(cx, cy, r * 0.30f, fillPaint)
            fillPaint.color = "#FFD670".toColorInt()
            canvas.drawCircle(cx + r * 0.12f, cy, r * 0.05f, fillPaint)
        }
    }

    private fun drawCosmicBoba(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#1B1230".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        val bob = sin(t * 1.5f) * r * 0.04f
        fillPaint.color = "#C4B5FD".toColorInt()
        reusableRect.set(cx - r * 0.34f, cy - r * 0.34f + bob, cx + r * 0.34f, cy + r * 0.44f + bob)
        canvas.drawRoundRect(reusableRect, r * 0.12f, r * 0.12f, fillPaint)
        fillPaint.color = "#2D1E4F".toColorInt()
        for (i in -2..2) {
            canvas.drawCircle(cx + i * r * 0.10f, cy + r * 0.28f + bob + sin(t * 3f + i) * r * 0.02f, r * 0.05f, fillPaint)
        }
        fillPaint.color = "#F472B6".toColorInt()
        canvas.drawRect(cx + r * 0.05f, cy - r * 0.62f + bob, cx + r * 0.14f, cy - r * 0.30f + bob, fillPaint)
    }

    private fun drawPaperPlaneSky(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.shader = LinearGradient(
            cx, cy - r, cx, cy + r,
            intArrayOf("#A5D8FF".toColorInt(), "#EBF5FF".toColorInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        val u = t * 0.8f
        val px = cx + r * 0.50f * sin(u)
        val py = cy + r * 0.26f * sin(2f * u)
        canvas.withRotation(cos(u) * 28f, px, py) {
            fillPaint.color = "#EF4444".toColorInt()
            reusablePath.reset()
            reusablePath.moveTo(px + r * 0.24f, py)
            reusablePath.lineTo(px - r * 0.18f, py - r * 0.14f)
            reusablePath.lineTo(px - r * 0.10f, py)
            reusablePath.lineTo(px - r * 0.18f, py + r * 0.14f)
            reusablePath.close()
            canvas.drawPath(reusablePath, fillPaint)
        }
    }

    private fun drawMinimalAtelier(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#F4EFE6".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        strokePaint.color = "#343842".toColorInt()
        strokePaint.strokeWidth = r * 0.016f
        for (i in 0 until 12) {
            val a = i * 30f * PI.toFloat() / 180f
            val inner = if (i % 3 == 0) r * 0.78f else r * 0.84f
            canvas.drawLine(cx + inner * cos(a), cy + inner * sin(a), cx + r * 0.90f * cos(a), cy + r * 0.90f * sin(a), strokePaint)
        }
        val dotA = t * 0.5f
        fillPaint.color = "#E06B3E".toColorInt()
        canvas.drawCircle(cx + r * 0.66f * cos(dotA), cy + r * 0.66f * sin(dotA), r * 0.045f, fillPaint)
    }

    private fun drawZenRipple(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#0D1210".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        strokePaint.strokeWidth = r * 0.016f
        for (i in 0..3) {
            val phase = ((t * 0.22f + i * 0.25f) % 1f)
            val alpha = ((1f - phase) * 180f).toInt().coerceIn(0, 255)
            strokePaint.color = Color.argb(alpha, 143, 168, 155)
            canvas.drawCircle(cx, cy, r * (0.15f + phase * 0.78f), strokePaint)
        }
    }

    private fun drawNordicPendule(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = "#EFECE6".toColorInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        strokePaint.color = "#4A4E58".toColorInt()
        strokePaint.strokeWidth = r * 0.02f
        reusableRect.set(cx - r * 0.66f, cy - r * 0.66f, cx + r * 0.66f, cy + r * 0.66f)
        canvas.drawArc(reusableRect, sin(t * 0.8f) * 35f + 40f, 110f, false, strokePaint)
        fillPaint.color = "#D94E34".toColorInt()
        canvas.drawCircle(cx, cy + r * 0.50f, r * 0.06f, fillPaint)
    }

    private fun drawEclipseHalo(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        fillPaint.color = Color.BLACK
        canvas.drawCircle(cx, cy, r, fillPaint)
        val haloPulse = 0.68f + 0.05f * sin(t * 1.1f)
        fillPaint.shader = RadialGradient(
            cx, cy, r * 0.90f,
            intArrayOf(Color.BLACK, "#DDE5B869".toColorInt(), "#44E5B869".toColorInt(), Color.TRANSPARENT),
            floatArrayOf(0.55f, haloPulse, 0.82f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 0.90f, fillPaint)
        fillPaint.shader = null
        fillPaint.color = Color.BLACK
        canvas.drawCircle(cx, cy, r * 0.48f, fillPaint)
    }
}
