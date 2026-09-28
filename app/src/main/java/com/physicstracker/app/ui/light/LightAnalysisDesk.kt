package com.physicstracker.app.ui.light

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.physicstracker.app.ui.BottomDeckButton
import com.physicstracker.app.ui.PhysicsColorBackground
import java.util.Locale

internal enum class LightDeskTab { GRAPH, TABLE, COMPARE, STATS }

/** Full-window results desk. It reads the same datasets as the floating panels and exports. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LightAnalysisDesk(
    state: LightWorkspaceState,
    tab: LightDeskTab,
    comparisonSlots: List<String>,
    showOptions: Boolean,
    showFit: Boolean,
    onBack: () -> Unit,
    onTab: (LightDeskTab) -> Unit,
    onToggleCompareSlot: (String) -> Unit,
    onFirstSlot: (String) -> Unit,
    onAddRegion: () -> Unit,
    onMeasureRegion: (String) -> Unit,
    onOptions: () -> Unit,
    onFit: () -> Unit,
    onBaseline: (Boolean) -> Unit,
    onSmooth: (Boolean) -> Unit,
    onPeaks: (Boolean) -> Unit,
    onRow: (Int) -> Unit,
    onShowFrame: () -> Unit,
    onExport: () -> Unit,
    onFloat: () -> Unit
) {
    val first = state.datasets[state.active]?.let { processedLightData(it, state.baseline, state.smooth) }
    val compared = comparisonSlots.mapNotNull { state.datasets[it] }
        .map { processedLightData(it, state.baseline, state.smooth) }
    Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = PhysicsColorBackground) {
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (tab) {
                LightDeskTab.GRAPH -> {
                    if (first == null) {
                        LightMissingRegion(state.active) { onMeasureRegion(state.active) }
                    } else BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val plotHeight = (maxHeight - 110.dp).coerceAtLeast(120.dp)
                        Column {
                            LightResultPlot(first, state.selectedRow, height = plotHeight,
                                fit = if (showFit) lightStatistics(first.observations) else null, onSelect = onRow)
                            first.observations.getOrNull(state.selectedRow)?.let { selected ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${lightTableNumber(selected.axis)} · ${lightTableNumber(selected.value)}",
                                        Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.labelMedium)
                                    if (selected.timestampUs != null) TextButton(onClick = onShowFrame,
                                        enabled = !state.busy) { Text("View frame") }
                                }
                            }
                        }
                    }
                    LightDeskRegionPicker("Region", state.active, lightRegionLabels(state.regionCount), state.geometries, onFirstSlot)
                }
                LightDeskTab.TABLE -> {
                    if (first == null) LightMissingRegion(state.active) { onMeasureRegion(state.active) }
                    else LightResultsTable(first, state.selectedRow, onRow, onShowFrame, Modifier.weight(1f))
                    LightDeskRegionPicker("Region", state.active, lightRegionLabels(state.regionCount), state.geometries, onFirstSlot)
                }
                LightDeskTab.COMPARE -> {
                    val sameMode = compared.filter { it.timeSeries == compared.firstOrNull()?.timeSeries }
                    LightComparisonPlot(sameMode, Modifier.weight(1f).fillMaxWidth())
                    if (compared.size > sameMode.size) Text("Compare regions measured in the same mode", style = MaterialTheme.typography.labelSmall)
                    Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        lightRegionLabels(state.regionCount).forEach { slot ->
                            BottomDeckButton(lightRegionControlLabel(slot, state.geometries[slot]),
                                selected = slot in comparisonSlots, accent = lightRegionColour(slot)) {
                                onToggleCompareSlot(slot)
                            }
                        }
                        BottomDeckButton("+ REGION", enabled = state.regionCount < MAX_LIGHT_REGIONS) { onAddRegion() }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        comparisonSlots.filter { it !in state.datasets }.forEach { slot ->
                            LightMissingRegion(slot) { onMeasureRegion(slot) }
                        }
                    }
                }
                LightDeskTab.STATS -> {
                    if (first == null) LightMissingRegion(state.active) { onMeasureRegion(state.active) }
                    else LightStatisticsPanel(first, Modifier.weight(1f))
                    LightDeskRegionPicker("Region", state.active, lightRegionLabels(state.regionCount), state.geometries, onFirstSlot)
                }
            }
            if (showOptions) {
                LightProcessingTools(state.baseline, onBaseline, state.smooth, onSmooth, state.peaks, onPeaks)
                BottomDeckButton("LINEAR FIT", selected = showFit, onClick = onFit)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                BottomDeckButton("‹ BACK", onClick = onBack)
                LightDeskTab.entries.forEach { choice ->
                    BottomDeckButton(choice.name, selected = tab == choice) { onTab(choice) }
                }
                BottomDeckButton("OPTIONS", selected = showOptions, onClick = onOptions)
                BottomDeckButton("FLOAT", onClick = onFloat)
                BottomDeckButton("EXPORT", enabled = if (tab == LightDeskTab.COMPARE) compared.isNotEmpty() else first != null,
                    onClick = onExport)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LightDeskRegionPicker(label: String, selected: String, slots: List<String>,
    geometries: Map<String, com.physicstracker.app.ui.LightGeometry>, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        slots.forEach { slot -> BottomDeckButton(lightRegionControlLabel(slot, geometries[slot]), selected = selected == slot,
            accent = lightRegionColour(slot)) { onSelect(slot) } }
    }
}

@Composable
private fun LightComparisonPlot(data: List<LightDataset>, modifier: Modifier = Modifier) {
    val samples = data.flatMap { it.observations }
    val bounds = LightPlotBounds.of(samples)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (data.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Measure two points to compare")
        } else {
            Canvas(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF101820))) {
                fun point(sample: LightObservation) = bounds.normalized(sample).let {
                    Offset(12f + it.x * (size.width - 24f), 12f + it.y * (size.height - 24f))
                }
                (0..4).forEach { step ->
                    val y = 12f + step * (size.height - 24f) / 4f
                    drawLine(Color.Gray.copy(alpha = 0.25f), Offset(12f, y), Offset(size.width - 12f, y))
                }
                data.forEach { dataset ->
                    dataset.observations.zipWithNext().forEach { (a, b) ->
                        if (!b.gapBefore) drawLine(lightRegionColour(dataset.slot), point(a), point(b), strokeWidth = 2.5f)
                    }
                    if (dataset.observations.size == 1) drawCircle(lightRegionColour(dataset.slot), 5f, point(dataset.observations.first()))
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                data.forEach { dataset -> Text("● ${dataset.slot}", color = lightRegionColour(dataset.slot), style = MaterialTheme.typography.labelMedium) }
                Text(if (data.first().timeSeries) "Time (s)" else "Position (px)", style = MaterialTheme.typography.labelSmall)
                Text("Intensity 0–255", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LightStatisticsPanel(data: LightDataset, modifier: Modifier = Modifier) {
    val stats = lightStatistics(data.observations)
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (stats == null) Text("No samples") else {
            Text("${data.slot} · ${data.channel.displayName()}", style = MaterialTheme.typography.titleMedium)
            Text("Samples ${stats.count}    Mean ${lightTableNumber(stats.mean)}")
            Text("Min ${lightTableNumber(stats.minimum)}    Max ${lightTableNumber(stats.maximum)}")
            Text("Std dev ${lightTableNumber(stats.standardDeviation)}")
            if (stats.slope != null && stats.intercept != null) {
                val axis = if (data.timeSeries) "s" else "px"
                Text("Linear fit: y = ${lightTableNumber(stats.slope)}x + ${lightTableNumber(stats.intercept)}")
                Text("Slope ${lightTableNumber(stats.slope)} intensity/$axis    R² ${stats.rSquared?.let(::lightTableNumber) ?: "—"}")
            } else Text("Linear fit needs at least two distinct positions")
        }
    }
}

@Composable
private fun LightMissingRegion(slot: String, onMeasure: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("No result for $slot")
        TextButton(onClick = onMeasure) { Text("Measure $slot") }
    }
}

@Composable
internal fun LightResultsTable(data: LightDataset, selected: Int, onSelect: (Int) -> Unit,
    onShowFrame: () -> Unit, modifier: Modifier = Modifier) {
    val processed = data.processing != null
    val axis = if (data.timeSeries) "Time (s)" else "Position (px)"
    Row(Modifier.fillMaxWidth().background(Color(0xFF263743)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text("#", Modifier.weight(0.45f))
        Text(axis, Modifier.weight(1.2f))
        Text("Value (0–255)", Modifier.weight(1f))
        if (processed) Text("Raw (0–255)", Modifier.weight(1f))
    }
    LazyColumn(modifier) {
        itemsIndexed(data.observations) { index, sample ->
            TextButton(onClick = { onSelect(index) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().background(if (index == selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)) {
                    Text("${index + 1}", Modifier.weight(0.45f))
                    Text(lightTableNumber(sample.axis), Modifier.weight(1.2f))
                    Text(lightTableNumber(sample.value), Modifier.weight(1f))
                    if (processed) Text(lightTableNumber(sample.rawValue ?: sample.value), Modifier.weight(1f))
                }
            }
        }
    }
    if (data.observations.getOrNull(selected)?.timestampUs != null) {
        TextButton(onClick = onShowFrame) { Text("View selected video frame") }
    }
}

private fun lightTableNumber(value: Double) = String.format(Locale.US, "%.2f", value)
