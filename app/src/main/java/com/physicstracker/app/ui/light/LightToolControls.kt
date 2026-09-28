package com.physicstracker.app.ui.light

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.physicstracker.app.ui.LightChannel
import com.physicstracker.app.ui.LightGeometry

internal fun lightRegionControlLabel(slot: String, geometry: LightGeometry?): String =
    "$slot · ${geometry?.tool?.name ?: "POINT"}"

internal fun lightRegionColour(slot: String): Color {
    val index = slot.fold(0) { value, letter -> value * 26 + (letter - 'A' + 1) } - 1
    val hue = (180f + index.coerceAtLeast(0) * 137.508f) % 360f
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.75f, 1f)))
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun LightVisibleTools(state: LightWorkspaceState, time: Boolean, onTime: (Boolean) -> Unit,
    onChannel: (LightChannel) -> Unit) {
    Column {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(!time || !state.video, { onTime(false) }, enabled = !state.busy, label = { Text("Current frame") })
            if (state.video) FilterChip(time, { onTime(true) }, enabled = !state.busy, label = { Text("Over time") })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LightChannel.entries.forEach { channel -> FilterChip(state.channel == channel, { onChannel(channel) },
                enabled = !state.busy, label = { Text(channel.displayName()) }) }
        }
        Text("${state.regionCount} ${if (state.regionCount == 1) "region" else "regions"}")
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun LightProcessingTools(baseline: Boolean, onBaseline: (Boolean) -> Unit, smooth: Boolean,
    onSmooth: (Boolean) -> Unit, peaks: Boolean, onPeaks: (Boolean) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(baseline, { onBaseline(!baseline) }, label = { Text("Subtract minimum") })
        FilterChip(smooth, { onSmooth(!smooth) }, label = { Text("Smooth (3 samples)") })
        FilterChip(peaks, { onPeaks(!peaks) }, label = { Text("Find peaks") })
    }
}
