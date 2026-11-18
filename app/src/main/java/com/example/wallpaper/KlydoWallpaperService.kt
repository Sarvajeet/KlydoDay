package com.example.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import com.example.audio.ChimeInterval
import com.example.audio.ChimePlayer
import com.example.audio.ChimeScheduleEvaluator
import com.example.audio.ChimeTriggerKind
import com.example.data.local.ClockSurfaceState
import com.example.data.local.ClockSurfaceStore
import com.example.data.local.WatchFaceLibraryStore
import com.example.data.model.CustomWatchFace
import com.example.data.model.KlydoArtCatalog
import com.example.data.model.KlydoArtMeta
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import com.example.data.model.WatchFaceLibraryState
import com.example.ui.viewmodel.KlydoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class KlydoWallpaperService : WallpaperService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timeTickRegistered = false
    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != Intent.ACTION_TIME_TICK) return
            val appCtx = applicationContext
            serviceScope.launch {
                val store = ClockSurfaceStore(appCtx)
                val state = store.surfaceFlow.first()
                if (!state.chimeInBackground) return@launch
                val interval = ChimeScheduleEvaluator.effectiveInterval(
                    hourlyChimeEnabled = state.hourlyChimeEnabled,
                    configuredInterval = state.chimeInterval
                )
                if (interval == ChimeInterval.OFF) return@launch
                val now = System.currentTimeMillis()
                val decision = ChimeScheduleEvaluator.evaluate(
                    nowMillis = now,
                    interval = interval,
                    settings = state.toDisplaySettingsSnapshot(),
                    lastChimeKey = ChimePlayer.lastPlayedDedupKey(appCtx)
                ) ?: return@launch
                if (ChimePlayer.tryAcquireChimeSlot(appCtx, decision.dedupKey)) {
                    val customFile = store.resolveActiveCustomChimeFile(state)
                    when (decision.kind) {
                        ChimeTriggerKind.FULL_HOUR -> ChimePlayer.playHourlyChime(
                            tone = state.chimeTone,
                            strikeHourCount = if (state.strikeHourCount) decision.hour12 else 0,
                            customFile = customFile,
                            context = appCtx
                        )
                        ChimeTriggerKind.SUB_INTERVAL -> ChimePlayer.playSubIntervalChime(
                            tone = state.chimeTone,
                            customFile = customFile,
                            context = appCtx
                        )
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        runCatching {
            ContextCompat.registerReceiver(
                this,
                timeTickReceiver,
                IntentFilter(Intent.ACTION_TIME_TICK),
                ContextCompat.RECEIVER_EXPORTED
            )
            timeTickRegistered = true
        }
    }

    override fun onDestroy() {
        if (timeTickRegistered) {
            runCatching { unregisterReceiver(timeTickReceiver) }
            timeTickRegistered = false
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onCreateEngine(): Engine = KlydoWallpaperEngine()

    inner class KlydoWallpaperEngine : Engine() {

        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private lateinit var surfaceStore: ClockSurfaceStore
        private lateinit var libraryStore: WatchFaceLibraryStore
        private val artPainter = KlydoWallpaperArtPainter()

        private var surfaceState: ClockSurfaceState = ClockSurfaceState()
        private var libraryState: WatchFaceLibraryState = WatchFaceLibraryState()
        private var cachedVideoFaceId: String? = null
        private var cachedVideoFrames: List<Bitmap> = emptyList()
        private var videoLoadJob: Job? = null

        private var isVisibleFlag = false
        private var hasSurface = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var xOffsetFraction = 0.5f

        private val choreographer = Choreographer.getInstance()
        private var lastDrawNanos = 0L
        private var startWallNanos = System.nanoTime()
        private val reusableCal = Calendar.getInstance()

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val clipPath = Path()
        private val rectF = RectF()

        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isVisibleFlag || !hasSurface) return
                val minIntervalNanos = if (surfaceState.wallpaperBatterySaverFps) {
                    21_000_000L // ~47fps battery-conscious smooth pacing
                } else {
                    0L // native display refresh (60-90fps)
                }
                if (minIntervalNanos == 0L || frameTimeNanos - lastDrawNanos >= minIntervalNanos) {
                    lastDrawNanos = frameTimeNanos
                    drawFrame(frameTimeNanos)
                }
                if (isVisibleFlag && hasSurface) {
                    choreographer.postFrameCallback(this)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            surfaceStore = ClockSurfaceStore(applicationContext)
            libraryStore = WatchFaceLibraryStore(applicationContext)

            engineScope.launch {
                combine(surfaceStore.surfaceFlow, libraryStore.libraryFlow) { surface, library ->
                    surface to library
                }.collect { (surface, library) ->
                    surfaceState = surface
                    libraryState = library
                    ensureVideoFramesLoadedIfNeeded()
                    if (isVisibleFlag && hasSurface) {
                        drawFrame(System.nanoTime())
                    }
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            hasSurface = true
            startWallNanos = System.nanoTime()
            if (isVisibleFlag) {
                scheduleNextFrame()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            if (isVisibleFlag && hasSurface) {
                drawFrame(System.nanoTime())
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleFlag = visible
            if (visible && hasSurface) {
                lastDrawNanos = 0L
                drawFrame(System.nanoTime())
                scheduleNextFrame()
            } else {
                stopFrames()
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            xOffsetFraction = xOffset
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            hasSurface = false
            stopFrames()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            stopFrames()
            videoLoadJob?.cancel()
            clearCachedVideoFrames()
            engineScope.cancel()
            super.onDestroy()
        }

        private fun scheduleNextFrame() {
            choreographer.removeFrameCallback(frameCallback)
            choreographer.postFrameCallback(frameCallback)
        }

        private fun stopFrames() {
            choreographer.removeFrameCallback(frameCallback)
        }

        private fun clearCachedVideoFrames() {
            val toRecycle = cachedVideoFrames
            cachedVideoFrames = emptyList()
            cachedVideoFaceId = null
            toRecycle.forEach { bmp ->
                if (!bmp.isRecycled) bmp.recycle()
            }
        }

        private fun ensureVideoFramesLoadedIfNeeded() {
            val activeCustom = libraryState.findCustomFace(surfaceState.activeKlydoId)
            if (activeCustom == null) {
                videoLoadJob?.cancel()
                if (cachedVideoFrames.isNotEmpty()) clearCachedVideoFrames()
                return
            }
            val cacheKey = "${activeCustom.id}_${activeCustom.clipStartMs}_${activeCustom.clipEndMs}"
            if (cachedVideoFaceId == cacheKey) return

            videoLoadJob?.cancel()
            clearCachedVideoFrames()
            cachedVideoFaceId = cacheKey

            val videoFile = libraryStore.resolveMediaFile(activeCustom.videoFileName)
            val thumbFile = activeCustom.thumbnailFileName.takeIf { it.isNotBlank() }
                ?.let { libraryStore.resolveMediaFile(it) }

            // Show saved thumbnail immediately so there is no blank flicker
            if (thumbFile != null && thumbFile.exists()) {
                val thumb = BitmapFactory.decodeFile(thumbFile.absolutePath)
                if (thumb != null) {
                    cachedVideoFrames = listOf(thumb)
                }
            }

            videoLoadJob = engineScope.launch {
                val extracted = withContext(Dispatchers.IO) {
                    extractLoopFrames(videoFile, activeCustom)
                }
                if (extracted.isNotEmpty() && cachedVideoFaceId == cacheKey) {
                    val previous = cachedVideoFrames
                    cachedVideoFrames = extracted
                    previous.forEach { bmp ->
                        if (!bmp.isRecycled) bmp.recycle()
                    }
                    if (isVisibleFlag && hasSurface) {
                        drawFrame(System.nanoTime())
                    }
                } else {
                    extracted.forEach { if (!it.isRecycled) it.recycle() }
                }
            }
        }

        private fun extractLoopFrames(videoFile: File, face: CustomWatchFace): List<Bitmap> {
            if (!videoFile.exists()) return emptyList()
            val retriever = MediaMetadataRetriever()
            val result = mutableListOf<Bitmap>()
            return try {
                retriever.setDataSource(videoFile.absolutePath)
                val startMs = face.clipStartMs.coerceAtLeast(0L)
                val endMs = face.clipEndMs.coerceAtLeast(startMs + 1000L)
                val spanMs = (endMs - startMs).coerceAtLeast(1000L)
                val frameCount = 20
                for (i in 0 until frameCount) {
                    val atUs = (startMs + (spanMs * i) / frameCount) * 1000L
                    val raw = retriever.getFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: continue
                    val square = cropSquareScaled(raw, 280)
                    if (raw !== square) raw.recycle()
                    result.add(square)
                }
                result
            } catch (_: Exception) {
                result
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }

        private fun cropSquareScaled(source: Bitmap, targetPx: Int): Bitmap {
            val minDim = min(source.width, source.height).coerceAtLeast(1)
            val cropX = (source.width - minDim) / 2
            val cropY = (source.height - minDim) / 2
            val square = Bitmap.createBitmap(source, cropX, cropY, minDim, minDim)
            return if (minDim > targetPx) {
                val scaled = Bitmap.createScaledBitmap(square, targetPx, targetPx, true)
                if (scaled !== square) square.recycle()
                scaled
            } else {
                square
            }
        }

        private fun drawFrame(frameTimeNanos: Long) {
            val holder = surfaceHolder ?: return
            if (!holder.surface.isValid || surfaceWidth <= 0 || surfaceHeight <= 0) return

            var canvas: Canvas? = null
            try {
                canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
                if (canvas != null) {
                    renderScene(canvas, frameTimeNanos)
                }
            } catch (_: IllegalStateException) {
            } catch (_: IllegalArgumentException) {
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: IllegalStateException) {
                    } catch (_: IllegalArgumentException) {
                    }
                }
            }
        }

        private fun renderScene(canvas: Canvas, frameTimeNanos: Long) {
            val w = surfaceWidth.toFloat()
            val h = surfaceHeight.toFloat()

            val nowMillis = System.currentTimeMillis()
            reusableCal.timeInMillis = nowMillis
            val hour24 = reusableCal.get(Calendar.HOUR_OF_DAY)
            val minute = reusableCal.get(Calendar.MINUTE)
            val second = reusableCal.get(Calendar.SECOND)
            val millis = reusableCal.get(Calendar.MILLISECOND)
            val minuteOfDay = hour24 * 60 + minute
            val exactMinuteOfDay = minuteOfDay + (second + millis / 1000f) / 60f
            val dayCode = KlydoViewModel.dayCodeFromCalendar(reusableCal)

            val settingsSnapshot = surfaceState.toDisplaySettingsSnapshot()
            val isNightMode = KlydoViewModel.isMinuteInNightWindow(minuteOfDay, settingsSnapshot)

            val availableFaces = libraryState.availableArtMetas()
            val activeArtMeta = availableFaces.firstOrNull { it.id == surfaceState.activeKlydoId }
                ?: availableFaces.firstOrNull()
                ?: KlydoArtCatalog.findById(surfaceState.activeKlydoId)
            val activeCustom = libraryState.findCustomFace(activeArtMeta.id)

            val blocksForDay = surfaceState.scheduleBlocks.filter { it.isActiveForDay(dayCode) }

            val elapsedSec = ((frameTimeNanos - startWallNanos).coerceAtLeast(0L)) / 1_000_000_000.0
            val phaseOffsetSec = (nowMillis % 2000L) / 1000.0
            val phaseSec = (elapsedSec + phaseOffsetSec).toFloat()

            // Pure obsidian background with gentle depth aura behind the clock
            canvas.drawColor(if (isNightMode) Color.BLACK else "#050609".toColorInt())
            val parallaxX = (xOffsetFraction - 0.5f) * (w * 0.03f)

            val showFrame = surfaceState.wallpaperShowFrame
            val topSafeOffset = if (h > w) h * 0.08f else 0f // Keep clear of lock-screen top clock widget
            val usableW = w * 0.84f
            val usableH = (h - topSafeOffset * 1.4f) * 0.84f

            if (showFrame) {
                val aspect = 0.575f
                val cabW = min(usableW, usableH * aspect)
                val cabH = cabW / aspect
                val left = (w - cabW) / 2f + parallaxX
                val top = (h - cabH) / 2f + topSafeOffset * 0.45f

                drawAtmosphereGlow(canvas, left + cabW / 2f, top + cabH * 0.44f, cabW * 0.95f, activeArtMeta, isNightMode)
                drawCabinetShell(canvas, left, top, cabW, cabH)

                val dialCenterX = left + cabW * 0.5f
                val dialRadius = (cabW * 0.78f) * 0.5f
                val dialCenterY = top + cabH * 0.080f + dialRadius

                drawUpperDial(
                    canvas = canvas,
                    cx = dialCenterX,
                    cy = dialCenterY,
                    radius = dialRadius,
                    artMeta = activeArtMeta,
                    customFace = activeCustom,
                    phaseSec = phaseSec,
                    exactMinuteOfDay = exactMinuteOfDay,
                    hour24 = hour24,
                    minute = minute,
                    second = second,
                    millis = millis,
                    blocksForDay = blocksForDay,
                    isNightMode = isNightMode
                )

                val pendRadius = (cabW * 0.41f) * 0.5f
                val pendCenterY = top + cabH * 0.080f + dialRadius * 2f + cabH * 0.115f + pendRadius
                drawPendulumAperture(
                    canvas = canvas,
                    cx = dialCenterX,
                    cy = pendCenterY,
                    radius = pendRadius,
                    tSeconds = phaseSec,
                    artMeta = activeArtMeta,
                    pendulumStyle = PendulumStyle.fromKey(surfaceState.pendulumStyle),
                    pendulumEnabled = surfaceState.pendulumEnabled,
                    isNightMode = isNightMode
                )
            } else {
                // Sleek frameless circular watch dial + optional pendulum for lock/home wallpaper
                val pendulumRatio = 0.52f
                val gapRatio = 0.10f
                val dialSize = if (surfaceState.pendulumEnabled) {
                    min(usableW * 0.88f, usableH / (1f + gapRatio + pendulumRatio))
                } else {
                    min(usableW, usableH) * 0.84f
                }
                val dialRadius = dialSize * 0.5f
                val pendRadius = if (surfaceState.pendulumEnabled) (dialSize * pendulumRatio) * 0.5f else 0f
                val gapPx = if (surfaceState.pendulumEnabled) dialSize * gapRatio else 0f
                val totalHeight = dialRadius * 2f + gapPx + pendRadius * 2f

                val cx = w * 0.5f + parallaxX
                val startTop = (h - totalHeight) * 0.5f + topSafeOffset * 0.45f
                val dialCy = startTop + dialRadius

                drawAtmosphereGlow(canvas, cx, dialCy, dialRadius * 1.65f, activeArtMeta, isNightMode)

                drawUpperDial(
                    canvas = canvas,
                    cx = cx,
                    cy = dialCy,
                    radius = dialRadius,
                    artMeta = activeArtMeta,
                    customFace = activeCustom,
                    phaseSec = phaseSec,
                    exactMinuteOfDay = exactMinuteOfDay,
                    hour24 = hour24,
                    minute = minute,
                    second = second,
                    millis = millis,
                    blocksForDay = blocksForDay,
                    isNightMode = isNightMode
                )

                if (surfaceState.pendulumEnabled) {
                    val pendCy = startTop + dialRadius * 2f + gapPx + pendRadius
                    drawPendulumAperture(
                        canvas = canvas,
                        cx = cx,
                        cy = pendCy,
                        radius = pendRadius,
                        tSeconds = phaseSec,
                        artMeta = activeArtMeta,
                        pendulumStyle = PendulumStyle.fromKey(surfaceState.pendulumStyle),
                        pendulumEnabled = true,
                        isNightMode = isNightMode
                    )
                }
            }
        }

        private fun drawAtmosphereGlow(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            artMeta: KlydoArtMeta,
            isNightMode: Boolean
        ) {
            val auraColor = if (isNightMode) {
                Color.argb(26, 255, 168, 76)
            } else {
                val base = parseHexOrDefault(artMeta.pendulumBobColorHex, "#28F7E6".toColorInt())
                Color.argb(46, Color.red(base), Color.green(base), Color.blue(base))
            }
            bgPaint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(auraColor, Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius, bgPaint)
            bgPaint.shader = null
        }

        private fun drawCabinetShell(canvas: Canvas, left: Float, top: Float, w: Float, h: Float) {
            val outerCorner = w * 0.115f
            rectF.set(left, top, left + w, top + h)

            fillPaint.shader = LinearGradient(
                left, top, left + w, top + h,
                intArrayOf("#E7CA9F".toColorInt(), "#CBA36F".toColorInt(), "#DEC096".toColorInt(), "#BC925F".toColorInt()),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(rectF, outerCorner, outerCorner, fillPaint)
            fillPaint.shader = null

            val seam1 = w * 0.011f
            rectF.set(left + seam1, top + seam1, left + w - seam1, top + h - seam1)
            strokePaint.color = "#5D4023".toColorInt()
            strokePaint.strokeWidth = w * 0.006f
            canvas.drawRoundRect(rectF, outerCorner - seam1, outerCorner - seam1, strokePaint)

            val ply2 = w * 0.018f
            rectF.set(left + ply2, top + ply2, left + w - ply2, top + h - ply2)
            strokePaint.color = "#D5B180".toColorInt()
            strokePaint.strokeWidth = w * 0.011f
            canvas.drawRoundRect(rectF, outerCorner - ply2, outerCorner - ply2, strokePaint)

            val faceInset = w * 0.047f
            rectF.set(left + faceInset, top + faceInset, left + w - faceInset, top + h - faceInset)
            fillPaint.shader = LinearGradient(
                left, top, left + w, top + h,
                intArrayOf("#1A1B20".toColorInt(), "#131418".toColorInt(), "#101114".toColorInt()),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(rectF, outerCorner - faceInset, outerCorner - faceInset, fillPaint)
            fillPaint.shader = null
        }

        private fun drawUpperDial(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            artMeta: KlydoArtMeta,
            customFace: CustomWatchFace?,
            phaseSec: Float,
            exactMinuteOfDay: Float,
            hour24: Int,
            minute: Int,
            second: Int,
            millis: Int,
            blocksForDay: List<ScheduleBlock>,
            isNightMode: Boolean
        ) {
            clipPath.reset()
            clipPath.addCircle(cx, cy, radius, Path.Direction.CW)

            canvas.withClip(clipPath) {
                if (customFace != null) {
                    val durationSec = ((customFace.clipEndMs - customFace.clipStartMs) / 1000f)
                        .coerceIn(1f, 10f)
                    artPainter.drawCustomVideoFrames(
                        canvas = canvas,
                        cx = cx,
                        cy = cy,
                        radius = radius,
                        frames = cachedVideoFrames,
                        phaseSec = phaseSec,
                        clipDurationSec = durationSec
                    )
                } else {
                    artPainter.drawPrebuiltArt(
                        canvas = canvas,
                        artMeta = artMeta,
                        cx = cx,
                        cy = cy,
                        radius = radius,
                        phaseSec = phaseSec,
                        minuteOfDay = exactMinuteOfDay
                    )
                }

                if (isNightMode) {
                    fillPaint.color = Color.argb(136, 0, 0, 0)
                    canvas.drawCircle(cx, cy, radius, fillPaint)
                }

                // Schedule arcs along inner glass rim
                val isPm = hour24 >= 12
                val windowStart = if (isPm) 720 else 0
                val windowEnd = if (isPm) 1440 else 720
                val rimRadius = radius * 0.955f
                val rimStroke = radius * 0.036f
                rectF.set(cx - rimRadius, cy - rimRadius, cx + rimRadius, cy + rimRadius)

                for (block in blocksForDay) {
                    val bStart = block.startMinutes.toInt()
                    val bEnd = bStart + block.durationMinutes.toInt()
                    val overlapStart = maxOf(bStart, windowStart)
                    val overlapEnd = minOf(bEnd, windowEnd)
                    if (overlapEnd > overlapStart) {
                        val startDeg = ((overlapStart - windowStart) / 720f) * 360f - 90f
                        val sweepDeg = (((overlapEnd - overlapStart) / 720f) * 360f).coerceAtLeast(3f)
                        val parsed = parseHexOrDefault(block.colorHex, "#E06B3E".toColorInt())
                        val isActiveNow = exactMinuteOfDay >= bStart && exactMinuteOfDay < bEnd
                        val alpha = if (isNightMode) {
                            if (isActiveNow) 165 else 52
                        } else {
                            if (isActiveNow) 235 else 115
                        }
                        strokePaint.color = if (isNightMode) {
                            Color.argb(alpha, 255, 168, 76)
                        } else {
                            Color.argb(alpha, Color.red(parsed), Color.green(parsed), Color.blue(parsed))
                        }
                        strokePaint.strokeWidth = if (isActiveNow) rimStroke * 1.3f else rimStroke
                        canvas.drawArc(rectF, startDeg, sweepDeg, false, strokePaint)
                    }
                }

                // Recessed circular glass bezel
                strokePaint.color = "#66333842".toColorInt()
                strokePaint.strokeWidth = radius * 0.030f
                canvas.drawCircle(cx, cy, radius * 0.985f, strokePaint)

                // Synced signature Klydoclock rounded hands
                val secValue = second + millis / 1000f
                val minValue = minute + secValue / 60f
                val hourValue = (hour24 % 12) + minValue / 60f
                val hourAngle = (hourValue * 30f - 90f) * PI.toFloat() / 180f
                val minAngle = (minValue * 6f - 90f) * PI.toFloat() / 180f

                val handRadius = radius * 0.88f
                val nightAmber = "#FFFFA84C".toColorInt()
                val handColor = if (isNightMode) {
                    nightAmber
                } else {
                    parseHexOrDefault(artMeta.handsColorHex, "#28F7E6".toColorInt())
                }
                val shadowColor = if (!isNightMode && !artMeta.isDarkFace) {
                    Color.argb(64, 255, 255, 255)
                } else {
                    Color.argb(128, 0, 0, 0)
                }

                val hStartX = cx - handRadius * 0.09f * cos(hourAngle)
                val hStartY = cy - handRadius * 0.09f * sin(hourAngle)
                val hEndX = cx + handRadius * 0.49f * cos(hourAngle)
                val hEndY = cy + handRadius * 0.49f * sin(hourAngle)

                val mStartX = cx - handRadius * 0.11f * cos(minAngle)
                val mStartY = cy - handRadius * 0.11f * sin(minAngle)
                val mEndX = cx + handRadius * 0.78f * cos(minAngle)
                val mEndY = cy + handRadius * 0.78f * sin(minAngle)

                handPaint.color = shadowColor
                handPaint.strokeWidth = radius * 0.136f
                canvas.drawLine(hStartX, hStartY, hEndX, hEndY, handPaint)
                handPaint.strokeWidth = radius * 0.112f
                canvas.drawLine(mStartX, mStartY, mEndX, mEndY, handPaint)

                handPaint.color = handColor
                handPaint.strokeWidth = radius * 0.112f
                canvas.drawLine(hStartX, hStartY, hEndX, hEndY, handPaint)
                handPaint.strokeWidth = radius * 0.088f
                canvas.drawLine(mStartX, mStartY, mEndX, mEndY, handPaint)

                fillPaint.color = shadowColor
                canvas.drawCircle(cx, cy, radius * 0.096f, fillPaint)
                fillPaint.color = handColor
                canvas.drawCircle(cx, cy, radius * 0.084f, fillPaint)
            }
        }

        private fun drawPendulumAperture(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            tSeconds: Float,
            artMeta: KlydoArtMeta,
            pendulumStyle: PendulumStyle,
            pendulumEnabled: Boolean,
            isNightMode: Boolean
        ) {
            clipPath.reset()
            clipPath.addCircle(cx, cy, radius, Path.Direction.CW)

            canvas.withClip(clipPath) {
                fillPaint.color = Color.BLACK
                canvas.drawCircle(cx, cy, radius, fillPaint)

                if (pendulumEnabled) {
                    val nightAmber = "#FFFFA84C".toColorInt()
                    val syncedRod = parseHexOrDefault(artMeta.pendulumRodColorHex, "#28F7E6".toColorInt())
                    val syncedBob = parseHexOrDefault(artMeta.pendulumBobColorHex, "#9D00DF".toColorInt())

                    val rodColor = when {
                        isNightMode -> Color.argb(184, 255, 168, 76)
                        pendulumStyle == PendulumStyle.OBSIDIAN -> syncedRod
                        pendulumStyle == PendulumStyle.BRASS -> "#FFD873".toColorInt()
                        pendulumStyle == PendulumStyle.TERRACOTTA -> "#FF9B73".toColorInt()
                        else -> syncedRod
                    }
                    val bobColor = when {
                        isNightMode -> nightAmber
                        pendulumStyle == PendulumStyle.OBSIDIAN -> syncedBob
                        pendulumStyle == PendulumStyle.BRASS -> "#F5A623".toColorInt()
                        pendulumStyle == PendulumStyle.TERRACOTTA -> "#E06B3E".toColorInt()
                        else -> syncedBob
                    }

                    val swingAngleDeg = sin(tSeconds * PI.toFloat()) * 22f
                    val diameter = radius * 2f
                    val pivotX = cx
                    val pivotY = (cy - radius) - diameter * 0.36f
                    val rodLength = diameter * 0.87f
                    val rad = swingAngleDeg * PI.toFloat() / 180f
                    val bobX = pivotX + rodLength * sin(rad)
                    val bobY = pivotY + rodLength * cos(rad)
                    val bobRadius = radius * 0.63f

                    fillPaint.shader = RadialGradient(
                        bobX, bobY, bobRadius * 1.45f,
                        intArrayOf(
                            Color.argb(68, Color.red(bobColor), Color.green(bobColor), Color.blue(bobColor)),
                            Color.TRANSPARENT
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(bobX, bobY, bobRadius * 1.45f, fillPaint)
                    fillPaint.shader = null

                    handPaint.color = rodColor
                    handPaint.strokeWidth = diameter * 0.055f
                    canvas.drawLine(pivotX, pivotY, bobX, bobY, handPaint)

                    if (pendulumStyle == PendulumStyle.MINIMAL && !isNightMode) {
                        strokePaint.color = bobColor
                        strokePaint.strokeWidth = diameter * 0.048f
                        canvas.drawCircle(bobX, bobY, bobRadius * 0.90f, strokePaint)
                    } else {
                        fillPaint.color = bobColor
                        canvas.drawCircle(bobX, bobY, bobRadius, fillPaint)
                    }
                }

                strokePaint.color = "#662F333C".toColorInt()
                strokePaint.strokeWidth = radius * 0.044f
                canvas.drawCircle(cx, cy, radius * 0.985f, strokePaint)
            }
        }

        private fun parseHexOrDefault(hex: String, fallback: Int): Int = try {
            hex.toColorInt()
        } catch (_: Exception) {
            fallback
        }
    }
}
