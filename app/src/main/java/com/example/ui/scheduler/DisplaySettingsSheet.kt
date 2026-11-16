package com.example.ui.scheduler

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.DisplaySettings
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.theme.CreamDial
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.PlyCharcoal
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent

@Composable
fun DisplaySettingsDialog(
    settings: DisplaySettings,
    isGuest: Boolean,
    userEmail: String?,
    onUpdateSettings: (DisplaySettings) -> Unit,
    onTestChime: () -> Unit,
    onSignInSuccess: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .border(1.dp, HairlineBorder, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = PlyCharcoal
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close settings", tint = MutedSand)
                    }
                }

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

                HorizontalDivider(color = HairlineBorder)

                SettingSwitchRow(
                    title = "Hourly Westminster Chime",
                    subtitle = "Gentle harmonic bell at the top of each hour",
                    checked = settings.hourlyChimeEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(hourlyChimeEnabled = it)) }
                )

                SettingSwitchRow(
                    title = "Routine Block Chimes",
                    subtitle = "Sound a cue when scheduled blocks begin",
                    checked = settings.blockChimeEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(blockChimeEnabled = it)) }
                )

                OutlinedButton(
                    onClick = onTestChime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = SaffronAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Harmonic Chime", color = CreamDial)
                }

                HorizontalDivider(color = HairlineBorder)

                SettingSwitchRow(
                    title = "Night Mode Dimming (10 PM – 7 AM)",
                    subtitle = "Dim watch face softly & pause chimes overnight",
                    checked = settings.nightModeEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(nightModeEnabled = it)) }
                )

                HorizontalDivider(color = HairlineBorder)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianChassis,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGuest) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Guest Mode (Stored on This Device)", style = MaterialTheme.typography.labelSmall, color = SaffronAmber)
                            Text(
                                text = "Sign in with Google to back up your schedule and sync across tablets and phones.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSand
                            )
                            if (authError != null) {
                                Text(
                                    text = authError ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFFB4AB)
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
                                    .height(48.dp)
                                    .testTag("google_sign_in_button")
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.width(20.dp).height(20.dp),
                                        color = CreamDial,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.auth_google_button))
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Synced Google Account", style = MaterialTheme.typography.labelSmall, color = MutedSand)
                                Text(
                                    text = userEmail ?: "Connected",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = CreamDial
                                )
                            }
                            TextButton(onClick = onSignOut) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = CrimsonAlert)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.sign_out), color = CrimsonAlert)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
