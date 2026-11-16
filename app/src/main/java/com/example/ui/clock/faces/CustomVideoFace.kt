package com.example.ui.clock.faces

import android.graphics.Matrix
import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

@Composable
internal fun CustomVideoLoopFace(
    videoUri: Uri,
    clipStartMs: Long,
    clipEndMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var viewWidth by remember { mutableIntStateOf(0) }
    var viewHeight by remember { mutableIntStateOf(0) }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
        }
    }

    LaunchedEffect(videoUri, clipStartMs, clipEndMs, durationMs) {
        val builder = MediaItem.Builder().setUri(videoUri)
        if (clipEndMs > clipStartMs + 300L && (clipStartMs > 0L || (durationMs > 0L && clipEndMs < durationMs))) {
            builder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clipStartMs.coerceAtLeast(0L))
                    .setEndPositionMs(clipEndMs)
                    .build()
            )
        }
        exoPlayer.setMediaItem(builder.build())
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.volume = 0f
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewWidth = size.width
                viewHeight = size.height
            },
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = false
                exoPlayer.setVideoTextureView(this)
            }
        },
        update = { textureView ->
            if (viewWidth > 0 && viewHeight > 0 && videoWidth > 0 && videoHeight > 0) {
                applyCenterCropTransform(
                    textureView = textureView,
                    viewWidth = viewWidth,
                    viewHeight = viewHeight,
                    videoWidth = videoWidth,
                    videoHeight = videoHeight
                )
            }
        },
        onRelease = { textureView ->
            exoPlayer.clearVideoTextureView(textureView)
        }
    )
}

private fun applyCenterCropTransform(
    textureView: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    videoWidth: Int,
    videoHeight: Int
) {
    val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
    val scaleX: Float
    val scaleY: Float
    if (videoAspect > viewAspect) {
        scaleX = videoAspect / viewAspect
        scaleY = 1f
    } else {
        scaleX = 1f
        scaleY = viewAspect / videoAspect
    }
    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}
