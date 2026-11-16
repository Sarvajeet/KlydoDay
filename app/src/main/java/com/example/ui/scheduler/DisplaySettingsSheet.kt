package com.example.ui.scheduler

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.audio.ChimeInterval
import com.example.audio.ChimeTone
import com.example.audio.CustomChimeSound
import com.example.data.local.ClockSurfaceState
import com.example.data.model.DisplaySettings
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.system.SystemClockSurfacesHelper
import com.example.ui.theme.CelestialTeal
import com.example.ui.theme.CreamDial
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.PlyCharcoal
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.WarmSlate
import kotlinx.coroutines.launch
import java.io.File

enum class SettingsTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    CLOCK("Clock", Icons.Rounded.Schedule, "settings_tab_clock"),
    CHIMES("Chimes", Icons.Rounded.MusicNote, "settings_tab_chimes"),
    SURFACES("Surfaces", Icons.Rounded.Wallpaper, "settings_tab_surfaces"),
    SYNC("Sync", Icons.Rounded.AccountCircle, "settings_tab_sync")
}

@Composable
fun DisplaySettingsDialog(
    settings: DisplaySettings,
    surfaceState: ClockSurfaceState = ClockSurfaceState(),
    isGuest: Boolean,
    userEmail: String?,
    onUpdateSettings: (DisplaySettings) -> Unit,
    onUpdateSurfacePreferences: (
        wallpaperShowFrame: Boolean?,
        dreamOledShift: Boolean?,
        dreamShowNextRoutine: Boolean?,
        wallpaperBatterySaverFps: Boolean?
    ) -> Unit = { _, _, _, _ -> },
    onUpdateChimePreferences: (
        interval: ChimeInterval?,
        tone: ChimeTone?,
        strikeHourCount: Boolean?,
        chimeInBackground: Boolean?,
        selectedCustomChimeId: String?
    ) -> Unit = { _, _, _, _, _ -> },
    onImportCustomChime: suspend (Uri) -> Result<CustomChimeSound> = {
        Result.failure(UnsupportedOperationException())
    },
    onDeleteCustomChime: (String) -> Unit = {},
    onTestChime: (ChimeTone, Int, File?) -> Unit = { _, _, _ -> },
    resolveChimeFile: (String) -> File = { File(it) },
    onSignInSuccess: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
    initialTab: SettingsTab = SettingsTab.CLOCK
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var systemLaunchMessage by remember { mutableStateOf<String?>(null) }
    var isImportingChime by remember { mutableStateOf(false) }
    var chimeImportError by remember { mutableStateOf<String?>(null) }
    var isWallpaperActive by remember {
        mutableStateOf(SystemClockSurfacesHelper.isKlydoLiveWallpaperActive(context))
    }

    val tabScrollState = rememberScrollState()
    LaunchedEffect(selectedTab) {
        tabScrollState.scrollTo(0)
    }

    val chimePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImportingChime = true
                chimeImportError = null
                onImportCustomChime(uri)
                    .onSuccess { imported ->
                        if (!settings.hourlyChimeEnabled) {
                            onUpdateSettings(settings.copy(hourlyChimeEnabled = true))
                        }
                        if (surfaceState.chimeInterval == ChimeInterval.OFF) {
                            onUpdateChimePreferences(
                                ChimeInterval.EVERY_1_HOUR,
                                null,
                                null,
                                null,
                                imported.id
                            )
                        }
                        onTestChime(
                            surfaceState.chimeTone,
                            0,
                            resolveChimeFile(imported.audioFileName)
                        )
                    }
                    .onFailure { err ->
                        chimeImportError = err.message ?: "Could not import audio clip."
                    }
                isImportingChime = false
            }
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isWallpaperActive = SystemClockSurfacesHelper.isKlydoLiveWallpaperActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 580.dp)
                .heightIn(max = 680.dp)
                .fillMaxWidth()
                .border(1.dp, HairlineBorder, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = PlyCharcoal
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.display_settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = CreamDial
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_settings_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close settings", tint = MutedSand)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SettingsTabBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(tabScrollState)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith
                                fadeOut(animationSpec = tween(120))
                        },
                        label = "settings_tab_transition"
                    ) { tab ->
                        when (tab) {
                            SettingsTab.CLOCK -> ClockSettingsTab(
                                settings = settings,
                                onUpdateSettings = onUpdateSettings
                            )

                            SettingsTab.CHIMES -> ChimesSettingsTab(
                                settings = settings,
                                surfaceState = surfaceState,
                                isImportingChime = isImportingChime,
                                chimeImportError = chimeImportError,
                                onUpdateSettings = onUpdateSettings,
                                onUpdateChimePreferences = onUpdateChimePreferences,
                                onPickCustomChime = {
                                    chimeImportError = null
                                    chimePickerLauncher.launch(arrayOf("audio/*"))
                                },
                                onDeleteCustomChime = onDeleteCustomChime,
                                onTestChime = onTestChime,
                                resolveChimeFile = resolveChimeFile
                            )

                            SettingsTab.SURFACES -> SurfacesSettingsTab(
                                surfaceState = surfaceState,
                                isWallpaperActive = isWallpaperActive,
                                systemLaunchMessage = systemLaunchMessage,
                                onLaunchWallpaper = {
                                    systemLaunchMessage = null
                                    val launched = SystemClockSurfacesHelper.launchLiveWallpaperPicker(context)
                                    if (!launched) {
                                        systemLaunchMessage =
                                            "Open Android Settings → Wallpaper & style → Live wallpapers → KlydoDay Live Clock."
                                    }
                                },
                                onOpenDreamSettings = {
                                    systemLaunchMessage = null
                                    val opened = SystemClockSurfacesHelper.openDreamSettings(context)
                                    if (!opened) {
                                        systemLaunchMessage =
                                            "Open Android Settings → Display → Screen saver → KlydoDay Clock."
                                    }
                                },
                                onUpdateSurfacePreferences = onUpdateSurfacePreferences
                            )

                            SettingsTab.SYNC -> SyncSettingsTab(
                                isGuest = isGuest,
                                userEmail = userEmail,
                                isSigningIn = isSigningIn,
                                authError = authError,
                                onStartGoogleSignIn = {
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
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTabBar(
    selectedTab: SettingsTab,
    onSelectTab: (SettingsTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianChassis)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SettingsTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) TerracottaAccent else Color.Transparent)
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .testTag(tab.testTag),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) CreamDial else MutedSand,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) CreamDial else MutedSand,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ClockSettingsTab(
    settings: DisplaySettings,
    onUpdateSettings: (DisplaySettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionCard(eyebrow = "CLOCK FACE & FRAME") {
            SettingSwitchRow(
                title = "Always-On Home Display",
                subtitle = "Keep screen awake continuously",
                checked = settings.keepScreenOn,
                onCheckedChange = { onUpdateSettings(settings.copy(keepScreenOn = it)) }
            )

            SettingSwitchRow(
                title = "Clock Cabinet Frame",
                subtitle = "Show Baltic-birch plywood & charcoal housing",
                checked = settings.showFrame,
                onCheckedChange = { onUpdateSettings(settings.copy(showFrame = it)) },
                switchTestTag = "show_frame_switch"
            )

            SettingSwitchRow(
                title = "Swinging Digital Pendulum",
                subtitle = "Show classic lower pendulum bay",
                checked = settings.pendulumEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(pendulumEnabled = it)) }
            )

            SettingSwitchRow(
                title = "Auto-Cycle Klydo Art",
                subtitle = "Rotate through gallery faces every hour",
                checked = settings.autoCycleArt,
                onCheckedChange = { onUpdateSettings(settings.copy(autoCycleArt = it)) }
            )

            SettingSwitchRow(
                title = "24-Hour Digital Readout",
                subtitle = "Display 14:30 instead of 2:30 PM",
                checked = settings.clockFormat24h,
                onCheckedChange = { onUpdateSettings(settings.copy(clockFormat24h = it)) }
            )
        }

        SettingsSectionCard(eyebrow = "QUIET HOURS") {
            SettingSwitchRow(
                title = "Night Mode Dimming (10 PM – 7 AM)",
                subtitle = "Dim watch face softly & pause chimes overnight",
                checked = settings.nightModeEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(nightModeEnabled = it)) }
            )
        }
    }
}

@Composable
private fun SurfacesSettingsTab(
    surfaceState: ClockSurfaceState,
    isWallpaperActive: Boolean,
    systemLaunchMessage: String?,
    onLaunchWallpaper: () -> Unit,
    onOpenDreamSettings: () -> Unit,
    onUpdateSurfacePreferences: (
        wallpaperShowFrame: Boolean?,
        dreamOledShift: Boolean?,
        dreamShowNextRoutine: Boolean?,
        wallpaperBatterySaverFps: Boolean?
    ) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionCard(
            eyebrow = "LOCK SCREEN & DOCK SAVER",
            trailingBadge = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isWallpaperActive) CelestialTeal.copy(alpha = 0.2f) else WarmSlate
                ) {
                    Text(
                        text = if (isWallpaperActive) "WALLPAPER ACTIVE" else "OPTIONAL SETUP",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isWallpaperActive) CelestialTeal else MutedSand,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        ) {
            Text(
                text = "Activate KlydoDay as your animated Lock & Home Screen wallpaper, or enable Charging Screen Saver for automatic standby clock mode.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSand
            )

            Button(
                onClick = onLaunchWallpaper,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaAccent,
                    contentColor = CreamDial
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("activate_live_wallpaper_button")
            ) {
                Icon(Icons.Rounded.Wallpaper, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.set_live_wallpaper_cta))
            }

            OutlinedButton(
                onClick = onOpenDreamSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("activate_screen_saver_button")
            ) {
                Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = SaffronAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.open_dream_settings_cta), color = CreamDial)
            }

            Text(
                text = "Tip: Under Screen saver, select KlydoDay Clock and set When to start → While charging or docked.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSand.copy(alpha = 0.85f)
            )

            if (systemLaunchMessage != null) {
                Text(
                    text = systemLaunchMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = SaffronAmber
                )
            }
        }

        SettingsSectionCard(eyebrow = "SURFACE APPEARANCE & POWER") {
            SettingSwitchRow(
                title = "Cabinet Frame on Wallpaper",
                subtitle = "Off keeps a clean floating dial for lock & home screens",
                checked = surfaceState.wallpaperShowFrame,
                onCheckedChange = { onUpdateSurfacePreferences(it, null, null, null) },
                switchTestTag = "wallpaper_show_frame_switch"
            )

            SettingSwitchRow(
                title = "OLED Burn-In Orbit (Screen Saver)",
                subtitle = "Gently shifts clock position during long standby",
                checked = surfaceState.dreamOledShift,
                onCheckedChange = { onUpdateSurfacePreferences(null, it, null, null) },
                switchTestTag = "dream_oled_shift_switch"
            )

            SettingSwitchRow(
                title = "Routine Bar on Screen Saver",
                subtitle = "Show time, date & next scheduled block",
                checked = surfaceState.dreamShowNextRoutine,
                onCheckedChange = { onUpdateSurfacePreferences(null, null, it, null) },
                switchTestTag = "dream_next_routine_switch"
            )

            SettingSwitchRow(
                title = "Battery-Smart Wallpaper Pacing",
                subtitle = "Smooth power-saving refresh (~47fps) on wallpaper",
                checked = surfaceState.wallpaperBatterySaverFps,
                onCheckedChange = { onUpdateSurfacePreferences(null, null, null, it) },
                switchTestTag = "wallpaper_battery_saver_switch"
            )
        }
    }
}

@Composable
private fun SyncSettingsTab(
    isGuest: Boolean,
    userEmail: String?,
    isSigningIn: Boolean,
    authError: String?,
    onStartGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionCard(eyebrow = "CLOUD BACKUP & SYNC") {
            if (isGuest) {
                Text(
                    text = "Guest Mode (Stored on This Device)",
                    style = MaterialTheme.typography.titleMedium,
                    color = CreamDial
                )
                Text(
                    text = "Sign in with Google to back up your routine schedule and display preferences across phones and tablets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSand
                )
                if (authError != null) {
                    Text(
                        text = authError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFB4AB)
                    )
                }
                Button(
                    onClick = onStartGoogleSignIn,
                    enabled = !isSigningIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerracottaAccent,
                        contentColor = CreamDial
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CreamDial,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.auth_google_button))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Synced Google Account",
                            style = MaterialTheme.typography.labelSmall,
                            color = CelestialTeal
                        )
                        Text(
                            text = userEmail ?: "Connected",
                            style = MaterialTheme.typography.bodyLarge,
                            color = CreamDial,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = CrimsonAlert
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.sign_out), color = CrimsonAlert)
                    }
                }
            }
        }

        SettingsSectionCard(eyebrow = "DEVICE STORAGE") {
            Text(
                text = "Custom video watch faces and custom chime audio clips stay stored locally on this device for fast offline and screen-saver playback.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSand
            )
        }
    }
}

@Composable
internal fun SettingsSectionCard(
    eyebrow: String,
    trailingBadge: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianChassis,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    color = SaffronAmber
                )
                trailingBadge?.invoke()
            }
            content()
        }
    }
}

@Composable
internal fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = CreamDial)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MutedSand)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CreamDial,
                checkedTrackColor = TerracottaAccent
            )
        )
    }
}
