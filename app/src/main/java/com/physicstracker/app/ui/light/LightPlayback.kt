package com.physicstracker.app.ui.light

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@androidx.media3.common.util.UnstableApi
@Composable
internal fun rememberLightPlayer(state: LightWorkspaceState): ExoPlayer {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    LaunchedEffect(state.uri, state.video) {
        player.stop(); player.clearMediaItems()
        if (state.video && state.uri != null) { player.setMediaItem(MediaItem.fromUri(state.uri)); player.prepare() }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

@androidx.media3.common.util.UnstableApi
@Composable
internal fun LightVideoSurface(player: ExoPlayer, modifier: Modifier) {
    AndroidView(factory = { PlayerView(it).apply { useController = false; this.player = player } }, modifier = modifier,
        update = { it.player = player })
}

@androidx.media3.common.util.UnstableApi
@Composable
internal fun LightPlaybackControls(player: ExoPlayer, enabled: Boolean, onPreview: (Long) -> Unit) {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seek by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(player) { while (true) { position = player.currentPosition; duration = player.duration.coerceAtLeast(0); delay(150) } }
    Column {
        Text("${position / 1000.0} / ${duration / 1000.0} s")
        Slider(value = seek ?: position.toFloat().coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(), enabled = enabled && duration > 0,
            onValueChange = { seek = it }, onValueChangeFinished = {
                val target = seek?.toLong() ?: position
                player.pause(); player.seekTo(target); onPreview(target * 1000); seek = null
            })
    }
}

@androidx.media3.common.util.UnstableApi
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LightSpeedControls(rate: Float, enabled: Boolean, onRate: (Float) -> Unit) {
    Column {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(0.1f, 0.25f, 0.5f, 1f, 2f, 4f).forEach { choice ->
                com.physicstracker.app.ui.BottomDeckButton("${choice}×", selected = kotlin.math.abs(rate - choice) < 0.01f,
                    enabled = enabled) { onRate(choice) }
            }
        }
        Slider(value = rate, onValueChange = onRate, valueRange = 0.1f..4f, enabled = enabled)
    }
}
