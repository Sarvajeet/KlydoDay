package com.example.ui

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.audio.ChimeInterval
import com.example.audio.ChimeTone
import com.example.audio.KlydoChimeScheduler
import com.example.data.firestore.KlydoRepository
import com.example.data.local.ClockSurfaceState
import com.example.data.local.ClockSurfaceStore
import com.example.data.local.GuestKlydoRepository
import com.example.data.local.WatchFaceLibraryStore
import com.example.data.model.KlydoArtCatalog
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import com.example.data.model.WatchFaceLibraryState
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.auth.signOut
import com.example.ui.clock.KlydoClockCabinet
import com.example.ui.clock.KlydoCyan
import com.example.ui.clock.RoutineStatusStrip
import com.example.ui.clock.parseHexColorOrDefault
import com.example.ui.scheduler.BlockEditorDialog
import com.example.ui.scheduler.CustomVideoFaceDialog
import com.example.ui.scheduler.DisplaySettingsDialog
import com.example.ui.scheduler.KlydoGalleryDialog
import com.example.ui.scheduler.ScheduleTimelinePane
import com.example.ui.theme.CelestialTeal
import com.example.ui.theme.CreamDial
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.PlyCharcoal
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.WarmSlate
import com.example.ui.viewmodel.KlydoViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun KlydoDashboardRoute(
    sessionKey: String,
    isGuest: Boolean,
    userEmail: String?,
    sharedVideoUri: Uri? = null,
    onConsumeSharedVideo: () -> Unit = {},
    sharedAudioUri: Uri? = null,
    onConsumeSharedAudio: () -> Unit = {}
) {
    val viewModel: KlydoViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dataSource = if (isGuest) {
                    GuestKlydoRepository(app)
                } else {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    KlydoRepository(db)
                }
                KlydoViewModel(
                    repository = dataSource,
                    currentUserId = sessionKey,
                    appContext = app.applicationContext
                )
            }
        }
    )

    KlydoDashboardScreen(
        viewModel = viewModel,
        isGuest = isGuest,
        userEmail = userEmail,
        sharedVideoUri = sharedVideoUri,
        onConsumeSharedVideo = onConsumeSharedVideo,
        sharedAudioUri = sharedAudioUri,
        onConsumeSharedAudio = onConsumeSharedAudio
    )
}

@Composable
fun KlydoDashboardScreen(
    viewModel: KlydoViewModel,
    isGuest: Boolean,
    userEmail: String?,
    sharedVideoUri: Uri? = null,
    onConsumeSharedVideo: () -> Unit = {},
    sharedAudioUri: Uri? = null,
    onConsumeSharedAudio: () -> Unit = {},
    animateClock: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val libraryStore = remember(context) { WatchFaceLibraryStore(context.applicationContext) }
    val libraryState by libraryStore.libraryFlow.collectAsStateWithLifecycle(
        initialValue = WatchFaceLibraryState()
    )
    val surfaceStore = remember(context) { ClockSurfaceStore(context.applicationContext) }
    val surfaceState by surfaceStore.surfaceFlow.collectAsStateWithLifecycle(
        initialValue = ClockSurfaceState()
    )

    val blocksState by viewModel.scheduleBlocksState.collectAsStateWithLifecycle()
    val settings by viewModel.displaySettingsState.collectAsStateWithLifecycle()
    val hasLoadedSettings by viewModel.hasLoadedSettings.collectAsStateWithLifecycle()
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val scrubOffsetMinutes by viewModel.scrubOffsetMinutes.collectAsStateWithLifecycle()
    val selectedDayCode by viewModel.selectedDayCode.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var showGallery by remember { mutableStateOf(false) }
    var showCustomVideoDialog by remember { mutableStateOf(false) }
    var pendingSharedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }
    var editingBlock by remember { mutableStateOf<ScheduleBlock?>(null) }
    var isEditorOpen by remember { mutableStateOf(false) }
    var isAmbientFullClock by remember { mutableStateOf(false) }
    var mobileTabIndex by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    LaunchedEffect(sharedVideoUri) {
        if (sharedVideoUri != null) {
            pendingSharedVideoUri = sharedVideoUri
            showGallery = false
            showCustomVideoDialog = true
            onConsumeSharedVideo()
        }
    }

    LaunchedEffect(sharedAudioUri) {
        if (sharedAudioUri != null) {
            onConsumeSharedAudio()
            val res = surfaceStore.importCustomChime(sharedAudioUri)
            res.onSuccess { imported ->
                viewModel.updateDisplaySettings { current ->
                    if (current.hourlyChimeEnabled) current else current.copy(hourlyChimeEnabled = true)
                }
                if (surfaceState.chimeInterval == ChimeInterval.OFF) {
                    surfaceStore.updateChimePreferences(chimeInterval = ChimeInterval.EVERY_1_HOUR)
                }
                val file = surfaceStore.resolveChimeFile(imported.audioFileName)
                viewModel.testChime(tone = surfaceState.chimeTone, strikeHourExample = 0, customFile = file)
                snackbarHostState.showSnackbar("Custom chime \"${imported.title}\" added & activated")
            }.onFailure { err ->
                snackbarHostState.showSnackbar(err.localizedMessage ?: "Could not import chime audio")
            }
        }
    }

    DisposableEffect(settings.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val hasFlag = (window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
            if (settings.keepScreenOn && !hasFlag) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else if (!settings.keepScreenOn && hasFlag) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler(enabled = isAmbientFullClock) {
        isAmbientFullClock = false
    }

    val effectiveMillis = nowMillis + (scrubOffsetMinutes * 60_000f).toLong()
    val effectiveCal = remember(effectiveMillis) {
        Calendar.getInstance().apply { timeInMillis = effectiveMillis }
    }
    val effectiveMinuteOfDay = effectiveCal.get(Calendar.HOUR_OF_DAY) * 60 + effectiveCal.get(Calendar.MINUTE)
    val effectiveDayCode = KlydoViewModel.dayCodeFromCalendar(effectiveCal)
    val todayDateKey = remember(nowMillis) { KlydoViewModel.currentDateKey(nowMillis) }
    val dateHeader = remember(nowMillis) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(nowMillis))
    }

    val isNightMode = KlydoViewModel.isMinuteInNightWindow(effectiveMinuteOfDay, settings)
    val allBlocks = (blocksState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(hasLoadedSettings, blocksState, settings) {
        val loadedBlocks = (blocksState as? UiState.Success)?.data ?: return@LaunchedEffect
        if (!hasLoadedSettings) return@LaunchedEffect
        surfaceStore.syncFromApp(settings, loadedBlocks)
    }
    val activeCustomChimeFile = remember(
        surfaceState.selectedCustomChimeId,
        surfaceState.customChimes
    ) {
        surfaceStore.resolveActiveCustomChimeFile(surfaceState)
    }
    LaunchedEffect(
        settings.hourlyChimeEnabled,
        surfaceState.chimeInterval,
        surfaceState.chimeTone,
        surfaceState.strikeHourCount,
        surfaceState.chimeInBackground,
        activeCustomChimeFile
    ) {
        viewModel.updateChimeConfig(
            interval = surfaceState.chimeInterval,
            tone = surfaceState.chimeTone,
            strikeHourCount = surfaceState.strikeHourCount,
            customChimeFile = activeCustomChimeFile
        )
        val effectiveInterval = if (settings.hourlyChimeEnabled) {
            surfaceState.chimeInterval.takeIf { it != ChimeInterval.OFF } ?: ChimeInterval.EVERY_1_HOUR
        } else {
            ChimeInterval.OFF
        }
        KlydoChimeScheduler.syncSchedule(
            context = context.applicationContext,
            enabled = surfaceState.chimeInBackground && effectiveInterval != ChimeInterval.OFF,
            interval = effectiveInterval
        )
    }
    val dialDayCode = if (scrubOffsetMinutes != 0f) effectiveDayCode else selectedDayCode
    val blocksForDial = remember(allBlocks, dialDayCode) {
        allBlocks.filter { it.isActiveForDay(dialDayCode) }.sortedBy { it.startMinutes }
    }

    val activeBlock = remember(blocksForDial, effectiveMinuteOfDay) {
        blocksForDial.firstOrNull { block ->
            val start = block.startMinutes.toInt()
            val end = start + block.durationMinutes.toInt()
            effectiveMinuteOfDay in start until end
        }
    }
    val nextBlock = remember(blocksForDial, effectiveMinuteOfDay) {
        blocksForDial.firstOrNull { it.startMinutes > effectiveMinuteOfDay } ?: blocksForDial.firstOrNull()
    }

    val availableFaces = remember(libraryState) {
        libraryState.availableArtMetas()
    }
    LaunchedEffect(availableFaces) {
        viewModel.updateAvailableFaceIds(availableFaces.map { it.id })
    }

    val activeArtMeta = remember(settings.activeKlydoId, availableFaces) {
        availableFaces.firstOrNull { it.id == settings.activeKlydoId }
            ?: availableFaces.firstOrNull()
            ?: KlydoArtCatalog.findById(settings.activeKlydoId)
    }
    LaunchedEffect(settings.activeKlydoId, availableFaces) {
        if (availableFaces.isNotEmpty() && availableFaces.none { it.id == settings.activeKlydoId }) {
            viewModel.selectKlydo(availableFaces.first().id)
        }
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

    val stripAccentColor = remember(activeArtMeta) {
        parseHexColorOrDefault(
            if (activeArtMeta.isDarkFace) activeArtMeta.handsColorHex else activeArtMeta.pendulumBobColorHex,
            KlydoCyan
        )
    }

    val cycleNextKlydo: () -> Unit = {
        val catalog = availableFaces.ifEmpty { KlydoArtCatalog.all }
        val idx = catalog.indexOfFirst { it.id == activeArtMeta.id }
        val next = catalog[(idx + 1) % catalog.size].id
        viewModel.selectKlydo(next)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletSplit = maxWidth >= 700.dp && maxHeight >= 520.dp
        val isPhoneLandscape = maxWidth >= 560.dp && maxHeight < 520.dp
        val isLandscapeAspect = maxWidth > maxHeight
        val showMobileFab = !isAmbientFullClock && !isTabletSplit && mobileTabIndex == 1

        Scaffold(
            containerColor = ObsidianChassis,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (showMobileFab) {
                    FloatingActionButton(
                        onClick = {
                            editingBlock = null
                            isEditorOpen = true
                        },
                        containerColor = TerracottaAccent,
                        contentColor = CreamDial,
                        modifier = Modifier.testTag("mobile_add_block_fab")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add schedule block")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .safeDrawingPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {

            if (isAmbientFullClock) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    KlydoClockCabinet(
                        klydoId = activeArtMeta.id,
                        pendulumStyle = PendulumStyle.fromKey(settings.pendulumStyle),
                        pendulumEnabled = settings.pendulumEnabled,
                        showFrame = settings.showFrame,
                        blocksForDay = blocksForDial,
                        scrubOffsetMinutes = scrubOffsetMinutes,
                        isNightMode = isNightMode,
                        onCycleKlydo = cycleNextKlydo,
                        onScrubDelta = { viewModel.adjustScrubMinutes(it) },
                        onSnapToNow = { viewModel.snapToLiveTime() },
                        artMeta = activeArtMeta,
                        customVideoFace = activeCustomVideoFace,
                        customVideoUri = activeCustomVideoUri,
                        animateClock = animateClock,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp)
                    )

                    if (scrubOffsetMinutes != 0f) {
                        val sign = if (scrubOffsetMinutes > 0) "+" else ""
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = TerracottaAccent,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { viewModel.snapToLiveTime() }
                                .testTag("snap_to_now_pill")
                        ) {
                            Text(
                                text = "PREVIEW $sign${scrubOffsetMinutes.toInt()}m • TAP TO SNAP",
                                style = MaterialTheme.typography.labelLarge,
                                color = CreamDial,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PlyCharcoal.copy(alpha = 0.88f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable { isAmbientFullClock = false }
                            .testTag("exit_ambient_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloseFullscreen,
                                contentDescription = "Exit full display",
                                tint = CreamDial,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Show Schedule", style = MaterialTheme.typography.labelLarge, color = CreamDial)
                        }
                    }
                }
            } else if (isTabletSplit) {
                // Tablet Home Display Split Console
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TopConsoleHeader(
                        dateHeader = dateHeader,
                        isNightMode = isNightMode,
                        isGuest = isGuest,
                        compact = false,
                        onGuestSyncClick = {
                            if (isGuest) showSignInDialog = true else showSettings = true
                        },
                        onOpenGallery = { showGallery = true },
                        onOpenSettings = { showSettings = true },
                        onEnterAmbient = { isAmbientFullClock = true },
                        onAddBlock = {
                            editingBlock = null
                            isEditorOpen = true
                        }
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KlydoClockCabinet(
                                klydoId = activeArtMeta.id,
                                pendulumStyle = PendulumStyle.fromKey(settings.pendulumStyle),
                                pendulumEnabled = settings.pendulumEnabled,
                                showFrame = settings.showFrame,
                                blocksForDay = blocksForDial,
                                scrubOffsetMinutes = scrubOffsetMinutes,
                                isNightMode = isNightMode,
                                onCycleKlydo = cycleNextKlydo,
                                onScrubDelta = { viewModel.adjustScrubMinutes(it) },
                                onSnapToNow = { viewModel.snapToLiveTime() },
                                artMeta = activeArtMeta,
                                customVideoFace = activeCustomVideoFace,
                                customVideoUri = activeCustomVideoUri,
                                animateClock = animateClock,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )

                            RoutineStatusStrip(
                                activeBlock = activeBlock,
                                nextBlock = nextBlock,
                                effectiveMinuteOfDay = effectiveMinuteOfDay,
                                clockFormat24h = settings.clockFormat24h,
                                artTitle = activeArtMeta.title,
                                accentColor = stripAccentColor,
                                isNightMode = isNightMode,
                                scrubOffsetMinutes = scrubOffsetMinutes,
                                onScrubDelta = { viewModel.adjustScrubMinutes(it) },
                                onSnapToNow = { viewModel.snapToLiveTime() }
                            )
                        }

                        ScheduleTimelinePane(
                            blocksState = blocksState,
                            selectedDayCode = selectedDayCode,
                            todayDateKey = todayDateKey,
                            effectiveMinuteOfDay = effectiveMinuteOfDay,
                            clockFormat24h = settings.clockFormat24h,
                            onSelectDay = { viewModel.selectDayCode(it) },
                            onAddBlock = {
                                editingBlock = null
                                isEditorOpen = true
                            },
                            onEditBlock = { block ->
                                editingBlock = block
                                isEditorOpen = true
                            },
                            onToggleComplete = { viewModel.toggleBlockCompletedToday(it) },
                            onLoadStarterRoutine = { viewModel.addStarterRoutine() },
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                        )
                    }
                }
            } else if (isPhoneLandscape) {
                // Dedicated Phone Landscape Bedside / Desk Layout
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KlydoClockCabinet(
                        klydoId = activeArtMeta.id,
                        pendulumStyle = PendulumStyle.fromKey(settings.pendulumStyle),
                        pendulumEnabled = settings.pendulumEnabled,
                        showFrame = settings.showFrame,
                        blocksForDay = blocksForDial,
                        scrubOffsetMinutes = scrubOffsetMinutes,
                        isNightMode = isNightMode,
                        onCycleKlydo = cycleNextKlydo,
                        onScrubDelta = { viewModel.adjustScrubMinutes(it) },
                        onSnapToNow = { viewModel.snapToLiveTime() },
                        artMeta = activeArtMeta,
                        customVideoFace = activeCustomVideoFace,
                        customVideoUri = activeCustomVideoUri,
                        animateClock = animateClock,
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight()
                    )

                    Column(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TopConsoleHeader(
                            dateHeader = dateHeader,
                            isNightMode = isNightMode,
                            isGuest = isGuest,
                            compact = true,
                            onGuestSyncClick = {
                                if (isGuest) showSignInDialog = true else showSettings = true
                            },
                            onOpenGallery = { showGallery = true },
                            onOpenSettings = { showSettings = true },
                            onEnterAmbient = { isAmbientFullClock = true },
                            onAddBlock = {
                                editingBlock = null
                                isEditorOpen = true
                            }
                        )

                        MobileTabBar(
                            selectedIndex = mobileTabIndex,
                            leftLabel = "Now & Scrub",
                            rightLabel = "Schedule",
                            onSelect = { mobileTabIndex = it }
                        )

                        if (mobileTabIndex == 0) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                RoutineStatusStrip(
                                    activeBlock = activeBlock,
                                    nextBlock = nextBlock,
                                    effectiveMinuteOfDay = effectiveMinuteOfDay,
                                    clockFormat24h = settings.clockFormat24h,
                                    artTitle = activeArtMeta.title,
                                    accentColor = stripAccentColor,
                                    isNightMode = isNightMode,
                                    scrubOffsetMinutes = scrubOffsetMinutes,
                                    onScrubDelta = { viewModel.adjustScrubMinutes(it) },
                                    onSnapToNow = { viewModel.snapToLiveTime() }
                                )
                            }
                        } else {
                            ScheduleTimelinePane(
                                blocksState = blocksState,
                                selectedDayCode = selectedDayCode,
                                todayDateKey = todayDateKey,
                                effectiveMinuteOfDay = effectiveMinuteOfDay,
                                clockFormat24h = settings.clockFormat24h,
                                onSelectDay = { viewModel.selectDayCode(it) },
                                onAddBlock = {
                                    editingBlock = null
                                    isEditorOpen = true
                                },
                                onEditBlock = { block ->
                                    editingBlock = block
                                    isEditorOpen = true
                                },
                                onToggleComplete = { viewModel.toggleBlockCompletedToday(it) },
                                onLoadStarterRoutine = { viewModel.addStarterRoutine() },
                                listBottomPadding = 84.dp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // Phone Portrait Layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TopConsoleHeader(
                        dateHeader = dateHeader,
                        isNightMode = isNightMode,
                        isGuest = isGuest,
                        compact = true,
                        onGuestSyncClick = {
                            if (isGuest) showSignInDialog = true else showSettings = true
                        },
                        onOpenGallery = { showGallery = true },
                        onOpenSettings = { showSettings = true },
                        onEnterAmbient = { isAmbientFullClock = true },
                        onAddBlock = {
                            editingBlock = null
                            isEditorOpen = true
                        }
                    )

                    MobileTabBar(
                        selectedIndex = mobileTabIndex,
                        leftLabel = "Clock",
                        rightLabel = "Schedule",
                        onSelect = { mobileTabIndex = it }
                    )

                    if (mobileTabIndex == 0) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KlydoClockCabinet(
                                klydoId = activeArtMeta.id,
                                pendulumStyle = PendulumStyle.fromKey(settings.pendulumStyle),
                                pendulumEnabled = settings.pendulumEnabled,
                                showFrame = settings.showFrame,
                                blocksForDay = blocksForDial,
                                scrubOffsetMinutes = scrubOffsetMinutes,
                                isNightMode = isNightMode,
                                onCycleKlydo = cycleNextKlydo,
                                onScrubDelta = { viewModel.adjustScrubMinutes(it) },
                                onSnapToNow = { viewModel.snapToLiveTime() },
                                artMeta = activeArtMeta,
                                customVideoFace = activeCustomVideoFace,
                                customVideoUri = activeCustomVideoUri,
                                animateClock = animateClock,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                            RoutineStatusStrip(
                                activeBlock = activeBlock,
                                nextBlock = nextBlock,
                                effectiveMinuteOfDay = effectiveMinuteOfDay,
                                clockFormat24h = settings.clockFormat24h,
                                artTitle = activeArtMeta.title,
                                accentColor = stripAccentColor,
                                isNightMode = isNightMode,
                                scrubOffsetMinutes = scrubOffsetMinutes,
                                onScrubDelta = { viewModel.adjustScrubMinutes(it) },
                                onSnapToNow = { viewModel.snapToLiveTime() }
                            )
                        }
                    } else {
                        ScheduleTimelinePane(
                            blocksState = blocksState,
                            selectedDayCode = selectedDayCode,
                            todayDateKey = todayDateKey,
                            effectiveMinuteOfDay = effectiveMinuteOfDay,
                            clockFormat24h = settings.clockFormat24h,
                            onSelectDay = { viewModel.selectDayCode(it) },
                            onAddBlock = {
                                editingBlock = null
                                isEditorOpen = true
                            },
                            onEditBlock = { block ->
                                editingBlock = block
                                isEditorOpen = true
                            },
                            onToggleComplete = { viewModel.toggleBlockCompletedToday(it) },
                            onLoadStarterRoutine = { viewModel.addStarterRoutine() },
                            listBottomPadding = 84.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            }
        }
    }

    if (showGallery) {
        KlydoGalleryDialog(
            settings = settings,
            libraryState = libraryState,
            libraryStore = libraryStore,
            onSelectKlydo = { viewModel.selectKlydo(it) },
            onToggleFavorite = { viewModel.toggleFavoriteKlydo(it) },
            onSelectPendulum = { viewModel.selectPendulumStyle(it) },
            onToggleFrame = { viewModel.setShowFrame(it) },
            onOpenAddVideoFace = {
                showGallery = false
                pendingSharedVideoUri = null
                showCustomVideoDialog = true
            },
            onDeleteFace = { art ->
                scope.launch {
                    if (availableFaces.size <= 1) {
                        snackbarHostState.showSnackbar("Keep at least one watch face in your library.")
                        return@launch
                    }
                    val remaining = availableFaces.filterNot { it.id == art.id }
                    if (settings.activeKlydoId == art.id && remaining.isNotEmpty()) {
                        viewModel.selectKlydo(remaining.first().id)
                    }
                    if (WatchFaceLibraryStore.isCustomVideoId(art.id)) {
                        libraryStore.deleteCustomFace(art.id)
                        snackbarHostState.showSnackbar("Deleted custom face \"${art.title}\"")
                    } else {
                        libraryStore.deletePrebuiltFace(art.id)
                    }
                }
            },
            onRestorePrebuilt = {
                scope.launch {
                    libraryStore.restoreAllPrebuiltFaces()
                    snackbarHostState.showSnackbar("Restored all prebuilt watch faces.")
                }
            },
            onRestoreSinglePrebuilt = { faceId ->
                scope.launch {
                    libraryStore.restorePrebuiltFace(faceId)
                }
            },
            onDismiss = { showGallery = false },
            animatePreviews = animateClock
        )
    }

    if (showCustomVideoDialog) {
        CustomVideoFaceDialog(
            initialUri = pendingSharedVideoUri,
            libraryStore = libraryStore,
            onSavedAndActivate = { newFaceId ->
                showCustomVideoDialog = false
                pendingSharedVideoUri = null
                viewModel.selectKlydo(newFaceId)
                scope.launch {
                    snackbarHostState.showSnackbar("Custom video watch face activated!")
                }
            },
            onDismiss = {
                showCustomVideoDialog = false
                pendingSharedVideoUri = null
            }
        )
    }

    if (showSettings) {
        DisplaySettingsDialog(
            settings = settings,
            surfaceState = surfaceState,
            isGuest = isGuest,
            userEmail = userEmail,
            onUpdateSettings = { viewModel.saveDisplaySettings(it) },
            onUpdateSurfacePreferences = { wallpaperShowFrame, dreamOledShift, dreamShowNextRoutine, wallpaperBatterySaverFps ->
                scope.launch {
                    surfaceStore.updateSurfacePreferences(
                        wallpaperShowFrame = wallpaperShowFrame,
                        dreamOledShift = dreamOledShift,
                        dreamShowNextRoutine = dreamShowNextRoutine,
                        wallpaperBatterySaverFps = wallpaperBatterySaverFps
                    )
                }
            },
            onUpdateChimePreferences = { interval, tone, strikeHourCount, chimeInBackground, selectedCustomChimeId ->
                scope.launch {
                    surfaceStore.updateChimePreferences(
                        chimeInterval = interval,
                        chimeTone = tone,
                        strikeHourCount = strikeHourCount,
                        chimeInBackground = chimeInBackground,
                        selectedCustomChimeId = selectedCustomChimeId
                    )
                }
            },
            onImportCustomChime = { audioUri ->
                surfaceStore.importCustomChime(audioUri)
            },
            onDeleteCustomChime = { chimeId ->
                scope.launch {
                    surfaceStore.deleteCustomChime(chimeId)
                }
            },
            onTestChime = { tone, strikeExample, customFile ->
                viewModel.testChime(
                    tone = tone,
                    strikeHourExample = strikeExample,
                    customFile = customFile
                )
            },
            resolveChimeFile = { fileName -> surfaceStore.resolveChimeFile(fileName) },
            onSignInSuccess = { showSettings = false },
            onSignOut = {
                showSettings = false
                signOut(credentialManager = credentialManager, scope = scope)
            },
            onDismiss = { showSettings = false },
            onTransformSettings = { viewModel.updateDisplaySettings(it) }
        )
    }

    if (showSignInDialog) {
        GuestSyncDialog(
            onDismiss = { showSignInDialog = false },
            onSignInSuccess = { showSignInDialog = false }
        )
    }

    if (isEditorOpen) {
        BlockEditorDialog(
            initialBlock = editingBlock,
            clockFormat24h = settings.clockFormat24h,
            onDismiss = { isEditorOpen = false },
            onSave = { block ->
                viewModel.saveBlock(block)
                isEditorOpen = false
            },
            onDelete = if (editingBlock != null) { id ->
                viewModel.deleteBlock(id)
                isEditorOpen = false
            } else null
        )
    }
}

@Composable
private fun MobileTabBar(
    selectedIndex: Int,
    leftLabel: String,
    rightLabel: String,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PlyCharcoal)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(leftLabel, rightLabel).forEachIndexed { idx, label ->
            val selected = selectedIndex == idx
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) TerracottaAccent else PlyCharcoal)
                    .clickable { onSelect(idx) }
                    .testTag("mobile_tab_$idx"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) CreamDial else MutedSand,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun TopConsoleHeader(
    dateHeader: String,
    isNightMode: Boolean,
    isGuest: Boolean,
    compact: Boolean,
    onGuestSyncClick: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    onEnterAmbient: () -> Unit,
    onAddBlock: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            if (compact) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = CreamDial,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dateHeader,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    GuestSyncPill(isGuest = isGuest, onClick = onGuestSyncClick)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = CreamDial,
                        maxLines = 1
                    )
                    GuestSyncPill(isGuest = isGuest, onClick = onGuestSyncClick)
                    AnimatedVisibility(visible = isNightMode, enter = fadeIn(), exit = fadeOut()) {
                        Surface(shape = RoundedCornerShape(50), color = WarmSlate) {
                            Text(
                                text = "NIGHT",
                                style = MaterialTheme.typography.labelSmall,
                                color = SaffronAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = dateHeader,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSand,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenGallery,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PlyCharcoal)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                    .testTag("gallery_button")
            ) {
                Icon(Icons.Rounded.Palette, contentDescription = "Klydo Art Gallery", tint = SaffronAmber)
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PlyCharcoal)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                    .testTag("settings_button")
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = "Display & Chimes Settings", tint = CreamDial)
            }

            IconButton(
                onClick = onEnterAmbient,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PlyCharcoal)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                    .testTag("ambient_mode_button")
            ) {
                Icon(Icons.Rounded.Fullscreen, contentDescription = "Full Display Mode", tint = CreamDial)
            }

            if (!compact) {
                Button(
                    onClick = onAddBlock,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaAccent, contentColor = CreamDial),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("add_block_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Block")
                }
            }
        }
    }
}

@Composable
private fun GuestSyncPill(
    isGuest: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
            .testTag("guest_sync_pill"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = WarmSlate
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isGuest) Icons.Rounded.AccountCircle else Icons.Rounded.CloudDone,
                    contentDescription = if (isGuest) "Guest mode" else "Cloud synced",
                    tint = if (isGuest) SaffronAmber else CelestialTeal,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (isGuest) "GUEST" else "SYNCED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isGuest) SaffronAmber else CelestialTeal
                )
            }
        }
    }
}

@Composable
private fun GuestSyncDialog(
    onDismiss: () -> Unit,
    onSignInSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .border(1.dp, HairlineBorder, RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = PlyCharcoal
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = null,
                    tint = SaffronAmber,
                    modifier = Modifier.size(42.dp)
                )
                Text(
                    text = "Using Guest Mode",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CreamDial,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Your schedule and Klydo settings are saved locally on this device. Sign in with Google anytime to back up and sync across your home tablet and phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSand,
                    textAlign = TextAlign.Center
                )
                if (authError != null) {
                    Text(
                        text = authError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFB4AB),
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = {
                        authError = null
                        isSigningIn = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isSigningIn = false
                                onSignInSuccess()
                            },
                            onAuthError = { msg ->
                                isSigningIn = false
                                authError = msg
                            },
                            scope = scope,
                            onAuthCancelled = { isSigningIn = false }
                        )
                    },
                    enabled = !isSigningIn,
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaAccent, contentColor = CreamDial),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CreamDial,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.auth_google_button))
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Continue as Guest", color = MutedSand)
                }
            }
        }
    }
}
