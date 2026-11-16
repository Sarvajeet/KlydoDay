package com.example.ui.scheduler

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.InspectedVideoInfo
import com.example.data.local.WatchFaceLibraryStore
import com.example.data.model.VideoFacePalettePreset
import com.example.data.model.VideoFacePalettes
import com.example.ui.clock.KlydoCyan
import com.example.ui.clock.KlydoViolet
import com.example.ui.clock.faces.CustomVideoLoopFace
import com.example.ui.clock.parseHexColorOrDefault
import com.example.ui.theme.CreamDial
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianChassis
import com.example.ui.theme.PlyCharcoal
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.WarmSlate
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CustomVideoFaceDialog(
    initialUri: Uri?,
    libraryStore: WatchFaceLibraryStore,
    onSavedAndActivate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var pickedUri by remember(initialUri) { mutableStateOf(initialUri) }
    var inspected by remember { mutableStateOf<InspectedVideoInfo?>(null) }
    var isInspecting by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var title by remember { mutableStateOf("My Video Face") }
    var selectedPalette by remember { mutableStateOf(VideoFacePalettes.all.first()) }
    var clipStartMs by remember { mutableFloatStateOf(0f) }
    var clipLengthMs by remember { mutableFloatStateOf(8000f) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
        }
    }

    LaunchedEffect(pickedUri) {
        val uri = pickedUri ?: return@LaunchedEffect
        isInspecting = true
        errorMessage = null
        val result = libraryStore.inspectVideoUri(uri)
        isInspecting = false
        if (result.isSuccess) {
            val info = result.getOrThrow()
            inspected = info
            title = info.suggestedTitle
            clipStartMs = info.recommendedStartMs.toFloat()
            clipLengthMs = (info.recommendedEndMs - info.recommendedStartMs).toFloat()
        } else {
            inspected = null
            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unsupported video clip."
        }
    }

    val info = inspected
    val durationMs = info?.durationMs ?: 0L
    val clipEndMs = if (durationMs > 0L) {
        (clipStartMs.toLong() + clipLengthMs.toLong()).coerceAtMost(durationMs)
    } else {
        clipStartMs.toLong() + clipLengthMs.toLong()
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom Video Watch Face",
                            style = MaterialTheme.typography.headlineSmall,
                            color = CreamDial
                        )
                        Text(
                            text = "Pick or share any 5–10s video to loop behind the hands",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSand
                        )
                    }
                    IconButton(onClick = { if (!isSaving) onDismiss() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MutedSand)
                    }
                }

                // Circular live preview dial
                Box(
                    modifier = Modifier
                        .size(188.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(ObsidianChassis)
                        .border(2.dp, HairlineBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isInspecting) {
                        CircularProgressIndicator(color = SaffronAmber)
                    } else if (info != null && pickedUri != null) {
                        CustomVideoLoopFace(
                            videoUri = info.uri,
                            clipStartMs = clipStartMs.toLong(),
                            clipEndMs = clipEndMs,
                            durationMs = durationMs
                        )
                        PreviewHandsOverlay(palette = selectedPalette)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VideoLibrary,
                                contentDescription = "Choose video",
                                tint = SaffronAmber,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to Choose Video",
                                style = MaterialTheme.typography.labelLarge,
                                color = CreamDial
                            )
                            Text(
                                text = "5–10 sec MP4 / MOV",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSand
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TerracottaAccent
                    )
                }

                if (info != null) {
                    val clipSec = ((clipEndMs - clipStartMs.toLong()).coerceAtLeast(1000L)) / 1000f
                    val totalSec = durationMs / 1000f
                    Text(
                        text = String.format(
                            Locale.US,
                            "Loop: %.1fs (%.1fs – %.1fs of %.1fs)",
                            clipSec,
                            clipStartMs / 1000f,
                            clipEndMs / 1000f,
                            totalSec
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = SaffronAmber,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    if (durationMs > WatchFaceLibraryStore.MAX_CLIP_DURATION_MS) {
                        val maxStart = (durationMs - WatchFaceLibraryStore.MIN_CLIP_DURATION_MS).toFloat()
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Trim Start Window (up to 10s loop)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSand
                            )
                            Slider(
                                value = clipStartMs.coerceIn(0f, maxStart),
                                onValueChange = { clipStartMs = it },
                                valueRange = 0f..maxStart,
                                colors = SliderDefaults.colors(
                                    thumbColor = TerracottaAccent,
                                    activeTrackColor = TerracottaAccent
                                )
                            )
                        }
                    }

                    if (durationMs > WatchFaceLibraryStore.MIN_CLIP_DURATION_MS) {
                        val maxLen = minOf(
                            WatchFaceLibraryStore.MAX_CLIP_DURATION_MS.toFloat(),
                            (durationMs - clipStartMs.toLong()).toFloat()
                        ).coerceAtLeast(WatchFaceLibraryStore.MIN_CLIP_DURATION_MS.toFloat())
                        if (maxLen > WatchFaceLibraryStore.MIN_CLIP_DURATION_MS + 500f) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Loop Duration (3–10s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSand
                                )
                                Slider(
                                    value = clipLengthMs.coerceIn(
                                        WatchFaceLibraryStore.MIN_CLIP_DURATION_MS.toFloat(),
                                        maxLen
                                    ),
                                    onValueChange = { clipLengthMs = it },
                                    valueRange = WatchFaceLibraryStore.MIN_CLIP_DURATION_MS.toFloat()..maxLen,
                                    colors = SliderDefaults.colors(
                                        thumbColor = SaffronAmber,
                                        activeTrackColor = SaffronAmber
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(32) },
                        label = { Text("Watch Face Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CreamDial,
                            unfocusedTextColor = CreamDial,
                            focusedBorderColor = SaffronAmber,
                            unfocusedBorderColor = HairlineBorder,
                            focusedLabelColor = SaffronAmber,
                            unfocusedLabelColor = MutedSand
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_video_title_input")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Synced Hands & Pendulum Palette",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSand
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VideoFacePalettes.all.forEach { preset ->
                                val selected = preset.key == selectedPalette.key
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) WarmSlate else ObsidianChassis,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) SaffronAmber else HairlineBorder,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectedPalette = preset }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColorOrDefault(preset.handsColorHex, KlydoCyan))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColorOrDefault(preset.pendulumBobColorHex, KlydoViolet))
                                        )
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CreamDial
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_video_button")
                    ) {
                        Text(if (info == null) "Choose Video" else "Change Video", color = CreamDial)
                    }

                    Button(
                        onClick = {
                            val currentInfo = info ?: return@Button
                            scope.launch {
                                isSaving = true
                                errorMessage = null
                                val saved = libraryStore.importVideoFace(
                                    uri = currentInfo.uri,
                                    title = title,
                                    durationMs = currentInfo.durationMs,
                                    clipStartMs = clipStartMs.toLong(),
                                    clipEndMs = clipEndMs,
                                    palette = selectedPalette
                                )
                                isSaving = false
                                if (saved.isSuccess) {
                                    onSavedAndActivate(saved.getOrThrow().id)
                                } else {
                                    errorMessage = saved.exceptionOrNull()?.localizedMessage
                                        ?: "Failed to save video watch face."
                                }
                            }
                        },
                        enabled = info != null && !isSaving && !isInspecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerracottaAccent,
                            contentColor = CreamDial
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_video_face_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = CreamDial,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text("Use Watch Face")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewHandsOverlay(palette: VideoFacePalettePreset) {
    val handColor = parseHexColorOrDefault(palette.handsColorHex, KlydoCyan)
    val shadowColor = if (palette.isDarkFace) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.35f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.42f
        val hourAngle = (-150f) * PI.toFloat() / 180f
        val minAngle = (-35f) * PI.toFloat() / 180f
        val hourEnd = Offset(cx + r * 0.52f * cos(hourAngle), cy + r * 0.52f * sin(hourAngle))
        val minEnd = Offset(cx + r * 0.80f * cos(minAngle), cy + r * 0.80f * sin(minAngle))

        drawLine(shadowColor, Offset(cx, cy), hourEnd, strokeWidth = size.minDimension * 0.068f, cap = StrokeCap.Round)
        drawLine(shadowColor, Offset(cx, cy), minEnd, strokeWidth = size.minDimension * 0.056f, cap = StrokeCap.Round)
        drawLine(handColor, Offset(cx, cy), hourEnd, strokeWidth = size.minDimension * 0.054f, cap = StrokeCap.Round)
        drawLine(handColor, Offset(cx, cy), minEnd, strokeWidth = size.minDimension * 0.042f, cap = StrokeCap.Round)
        drawCircle(color = handColor, radius = size.minDimension * 0.044f, center = Offset(cx, cy))
    }
}
