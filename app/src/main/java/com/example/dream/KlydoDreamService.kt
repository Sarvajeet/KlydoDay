package com.example.dream

import android.net.Uri
import android.service.dreams.DreamService
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.R
import com.example.audio.ChimePlayer
import com.example.audio.ChimeScheduleEvaluator
import com.example.audio.ChimeTriggerKind
import com.example.data.local.ClockSurfaceState
import com.example.data.local.ClockSurfaceStore
import com.example.data.local.WatchFaceLibraryStore
import com.example.data.model.KlydoArtCatalog
import com.example.data.model.PendulumStyle
import com.example.data.model.WatchFaceLibraryState
import com.example.ui.clock.KlydoClockCabinet
import com.example.ui.clock.KlydoCyan
import com.example.ui.clock.parseHexColorOrDefault
import com.example.ui.theme.CreamDial
import com.example.ui.theme.KlydoDayTheme
import com.example.ui.theme.MutedSand
import com.example.ui.theme.NightAmber
import com.example.ui.viewmodel.KlydoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class KlydoDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isFullscreen = true
        isScreenBright = true

        window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }

        val surfaceStore = ClockSurfaceStore(applicationContext)
        val libraryStore = WatchFaceLibraryStore(applicationContext)

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                KlydoDayTheme {
                    KlydoDreamContent(
                        surfaceStore = surfaceStore,
                        libraryStore = libraryStore,
                        onBrightnessPreference = { isNight ->
                            isScreenBright = !isNight
                        },
                        onWake = { finish() }
                    )
                }
            }
        }
        setContentView(composeView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    override fun onDreamingStopped() {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        moveToDestroyed()
        super.onDetachedFromWindow()
    }

    private fun moveToDestroyed() {
        val state = lifecycleRegistry.currentState
        if (state == Lifecycle.State.DESTROYED) return
        if (state.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }
}

@Composable
private fun KlydoDreamContent(
    surfaceStore: ClockSurfaceStore,
    libraryStore: WatchFaceLibraryStore,
    onBrightnessPreference: (isNight: Boolean) -> Unit,
    onWake: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val surfaceState by surfaceStore.surfaceFlow.collectAsStateWithLifecycle(
        initialValue = ClockSurfaceState()
    )
    val libraryState by libraryStore.libraryFlow.collectAsStateWithLifecycle(
        initialValue = WatchFaceLibraryState()
    )

    val appContext = LocalContext.current.applicationContext
    val latestSurfaceState by rememberUpdatedState(surfaceState)
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var oledPhase by remember { mutableFloatStateOf(0f) }
    var showWakeHint by remember { mutableStateOf(true) }
    var lastDreamChimeKey by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            nowMillis = now
            // Complete gentle 2-minute anti-burn-in orbit
            oledPhase = ((now % 120_000L) / 120_000f) * (2f * PI.toFloat())
            val state = latestSurfaceState
            val settingsSnapshot = state.toDisplaySettingsSnapshot()
            val effectiveInterval = ChimeScheduleEvaluator.effectiveInterval(
                hourlyChimeEnabled = state.hourlyChimeEnabled,
                configuredInterval = state.chimeInterval
            )
            val knownLastKey = lastDreamChimeKey.ifBlank { ChimePlayer.lastPlayedDedupKey(appContext) }
            val trigger = ChimeScheduleEvaluator.evaluate(
                nowMillis = now,
                interval = effectiveInterval,
                settings = settingsSnapshot,
                lastChimeKey = knownLastKey
            )
            if (trigger != null && ChimePlayer.tryAcquireChimeSlot(appContext, trigger.dedupKey)) {
                lastDreamChimeKey = trigger.dedupKey
                val customFile = surfaceStore.resolveActiveCustomChimeFile(state)
                when (trigger.kind) {
                    ChimeTriggerKind.FULL_HOUR -> ChimePlayer.playHourlyChime(
                        tone = state.chimeTone,
                        strikeHourCount = if (state.strikeHourCount) trigger.hour12 else 0,
                        customFile = customFile,
                        context = appContext
                    )
                    ChimeTriggerKind.SUB_INTERVAL -> ChimePlayer.playSubIntervalChime(
                        tone = state.chimeTone,
                        customFile = customFile,
                        context = appContext
                    )
                }
            }
            val sleepMs = (1000L - (now % 1000L) + 15L).coerceIn(200L, 1020L)
            delay(sleepMs)
        }
    }

    LaunchedEffect(Unit) {
        delay(6500L)
        showWakeHint = false
    }

    val cal = remember(nowMillis) { Calendar.getInstance().apply { timeInMillis = nowMillis } }
    val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val dayCode = KlydoViewModel.dayCodeFromCalendar(cal)
    val settingsSnapshot = remember(surfaceState) { surfaceState.toDisplaySettingsSnapshot() }
    val isNightMode = KlydoViewModel.isMinuteInNightWindow(minuteOfDay, settingsSnapshot)

    LaunchedEffect(isNightMode) {
        onBrightnessPreference(isNightMode)
    }

    val availableFaces = remember(libraryState) { libraryState.availableArtMetas() }
    val activeArtMeta = remember(surfaceState.activeKlydoId, availableFaces) {
        availableFaces.firstOrNull { it.id == surfaceState.activeKlydoId }
            ?: availableFaces.firstOrNull()
            ?: KlydoArtCatalog.findById(surfaceState.activeKlydoId)
    }

    val activeCustomVideoFace = remember(activeArtMeta.id, libraryState.customFaces) {
        libraryState.findCustomFace(activeArtMeta.id)
    }
    val activeCustomVideoUri = remember(activeCustomVideoFace) {
        activeCustomVideoFace?.let { face ->
            val file = libraryStore.resolveMediaFile(face.videoFileName)
            if (file.exists()) Uri.fromFile(file) else null
        }
    }

    val blocksForToday = remember(surfaceState.scheduleBlocks, dayCode) {
        surfaceState.scheduleBlocks.filter { it.isActiveForDay(dayCode) }.sortedBy { it.startMinutes }
    }
    val activeBlock = remember(blocksForToday, minuteOfDay) {
        blocksForToday.firstOrNull { b ->
            val start = b.startMinutes.toInt()
            minuteOfDay in start until (start + b.durationMinutes.toInt())
        }
    }
    val nextBlock = remember(blocksForToday, minuteOfDay) {
        blocksForToday.firstOrNull { it.startMinutes > minuteOfDay }
    }

    val cycleNextFace: () -> Unit = {
        val catalog = availableFaces.ifEmpty { KlydoArtCatalog.all }
        val idx = catalog.indexOfFirst { it.id == activeArtMeta.id }
        val nextId = catalog[(idx + 1) % catalog.size].id
        scope.launch {
            surfaceStore.updateActiveKlydoId(nextId)
        }
    }

    val driftX = if (surfaceState.dreamOledShift) sin(oledPhase) * 10f else 0f
    val driftY = if (surfaceState.dreamOledShift) sin(oledPhase * 2f) * 7f else 0f
    val dateLabel = remember(nowMillis) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(nowMillis))
    }
    val timeLabel = remember(minuteOfDay, surfaceState.clockFormat24h) {
        KlydoViewModel.formatMinutesOfDay(minuteOfDay, surfaceState.clockFormat24h)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onWake() }
            .safeDrawingPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(driftX.dp.roundToPx(), driftY.dp.roundToPx()) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                KlydoClockCabinet(
                    klydoId = activeArtMeta.id,
                    pendulumStyle = PendulumStyle.fromKey(surfaceState.pendulumStyle),
                    pendulumEnabled = surfaceState.pendulumEnabled,
                    showFrame = surfaceState.showFrame,
                    blocksForDay = blocksForToday,
                    scrubOffsetMinutes = 0f,
                    isNightMode = isNightMode,
                    onCycleKlydo = cycleNextFace,
                    onScrubDelta = {},
                    onSnapToNow = {},
                    artMeta = activeArtMeta,
                    customVideoFace = activeCustomVideoFace,
                    customVideoUri = activeCustomVideoUri,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                )
            }

            if (surfaceState.dreamShowNextRoutine) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF0E1015).copy(alpha = if (isNightMode) 0.55f else 0.78f),
                    modifier = Modifier.widthIn(max = 420.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "$timeLabel • $dateLabel",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isNightMode) NightAmber else CreamDial.copy(alpha = 0.88f),
                            maxLines = 1
                        )
                        val routineBlock = activeBlock ?: nextBlock
                        if (routineBlock != null) {
                            val dotColor = if (isNightMode) {
                                NightAmber
                            } else {
                                parseHexColorOrDefault(routineBlock.colorHex, KlydoCyan)
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(dotColor, CircleShape)
                            )
                            val prefix = if (activeBlock != null) "NOW" else "NEXT"
                            Text(
                                text = "$prefix · ${routineBlock.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isNightMode) NightAmber.copy(alpha = 0.82f) else MutedSand,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showWakeHint,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = stringResource(R.string.dream_tap_wake_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSand.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .alpha(0.75f)
                )
            }
        }
    }
}
