package com.physicstracker.app.ui.analysis

import com.physicstracker.app.ui.positionAverageWindowForStrength
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.physicstracker.app.ui.GraphProfileMode
import com.physicstracker.app.ui.PhysicsColorBackground
import com.physicstracker.app.ui.PhysicsColorDivider
import com.physicstracker.app.ui.PhysicsColorPrimary
import com.physicstracker.app.ui.PhysicsColorSurface
import com.physicstracker.app.ui.PhysicsColorSurfaceVariant
import com.physicstracker.app.ui.PhysicsColorTrackA
import com.physicstracker.app.ui.PhysicsColorTrackB
import java.util.Locale

@Composable
fun AnalysisWorkspace(
    state: AnalysisWorkspaceState,
    onSelectedModuleChange: (AnalysisModuleId) -> Unit,
    fullGraphVisible: Boolean,
    onFullGraphVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    graphContent: @Composable () -> Unit
) {
    var panelWidthFraction by rememberSaveable { mutableFloatStateOf(0.32f) }
    var tableHeightFraction by rememberSaveable { mutableFloatStateOf(0.26f) }
    val modules = remember { DefaultAnalysisModules }
    val statistics = remember(state.rows) { buildAnalysisStatistics(state.rows) }
    val motion = remember(state.rows) { buildAnalysisMotionSummary(state.rows) }
    val spectrum = remember(state.rows) { buildAnalysisSpectrumSummary(state.rows) }

    Surface(
        color = PhysicsColorBackground,
        modifier = modifier.fillMaxSize()
    ) {
        if (fullGraphVisible) {
            Box(modifier = Modifier.fillMaxSize()) {
                graphContent()
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .zIndex(20f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onFullGraphVisibleChange(false) },
                        border = BorderStroke(1.dp, PhysicsColorDivider),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.78f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text("BACK", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = state.onExitFullScreen,
                        border = BorderStroke(1.dp, PhysicsColorPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.82f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text("EXIT FULL SCREEN", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            return@Surface
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val totalHeightDp = maxHeight
                    val panelWidth = maxWidth * panelWidthFraction
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .border(1.dp, PhysicsColorDivider, RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                ) {
                                    graphContent()
                                }
                                AnalysisModeBar(
                                    modules = modules,
                                    selectedModule = state.selectedModule,
                                    onSelectedModuleChange = onSelectedModuleChange,
                                    onShowFullGraph = { onFullGraphVisibleChange(true) }
                                )
                            }
                            AnalysisVerticalSplitter(totalWidthPx) {
                                panelWidthFraction = (panelWidthFraction - it).coerceIn(0.22f, 0.58f)
                            }
                            Surface(
                                color = PhysicsColorSurface.copy(alpha = 0.98f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, PhysicsColorDivider),
                                modifier = Modifier
                                    .width(panelWidth)
                                    .fillMaxHeight()
                            ) {
                                AnalysisInspector(
                                    state = state,
                                    statistics = statistics,
                                    motion = motion,
                                    spectrum = spectrum
                                )
                            }
                        }
                        if (state.tableVisible) {
                            AnalysisHorizontalSplitter(totalHeightDp.value) {
                                tableHeightFraction = (tableHeightFraction - it).coerceIn(0.16f, 0.44f)
                            }
                            AnalysisDataTable(
                                rows = state.rows,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((totalHeightDp * tableHeightFraction).coerceIn(108.dp, 300.dp))
                            )
                        }
                    }
                }
            }
            }
            OutlinedButton(
                onClick = state.onExitFullScreen,
                border = BorderStroke(1.dp, PhysicsColorPrimary),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.82f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .zIndex(20f)
            ) {
                Text("EXIT FULL SCREEN", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AnalysisModeBar(
    modules: List<AnalysisModuleSpec>,
    selectedModule: AnalysisModuleId,
    onSelectedModuleChange: (AnalysisModuleId) -> Unit,
    onShowFullGraph: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .horizontalScroll(rememberScrollState())
            .background(PhysicsColorSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modules.forEach { module ->
            TextButton(
                onClick = { onSelectedModuleChange(module.id) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (module.id == selectedModule) PhysicsColorPrimary.copy(alpha = 0.22f) else Color.Transparent,
                    contentColor = if (module.id == selectedModule) Color.White else Color.White.copy(alpha = 0.72f)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(module.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
        OutlinedButton(
            onClick = onShowFullGraph,
            border = BorderStroke(1.dp, PhysicsColorPrimary.copy(alpha = 0.72f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PhysicsColorPrimary.copy(alpha = 0.14f),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("Full Graph", style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun AnalysisVerticalSplitter(totalWidthPx: Float, onDeltaFraction: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .width(10.dp)
            .fillMaxHeight()
            .pointerInput(totalWidthPx) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDeltaFraction(dragAmount.x / totalWidthPx)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(56.dp)
                .background(PhysicsColorDivider, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun AnalysisHorizontalSplitter(totalHeightDp: Float, onDeltaFraction: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .pointerInput(totalHeightDp) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDeltaFraction(dragAmount.y / totalHeightDp.coerceAtLeast(1f))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(4.dp)
                .background(PhysicsColorDivider, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun AnalysisInspector(
    state: AnalysisWorkspaceState,
    statistics: AnalysisStatistics,
    motion: AnalysisMotionSummary,
    spectrum: AnalysisSpectrumSummary
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = state.selectedModule.label,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Divider(color = PhysicsColorDivider)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                when (state.selectedModule) {
                    AnalysisModuleId.DATA -> AnalysisCard("Curation Results") {
                        MetricRow("Rows", state.rows.size.toString())
                        MetricRow("Y series", state.selectedSeriesLabel)
                        MetricRow("X series", state.selectedXAxisLabel)
                        MetricRow(
                            "Position average",
                            if (state.smoothingEnabled) {
                                "${positionAverageWindowForStrength(state.filterStrength)} samples"
                            } else {
                                "Off"
                            }
                        )
                        MetricRow("Outlier filter", if (state.outlierFilterEnabled) "On" else "Off")
                        MetricRow("Signal filter", activeSignalFilterLabel(state))
                        MetricRow("Baseline", if (state.baselineCorrectionEnabled) "Corrected" else "Original")
                        MetricRow("Normalization", if (state.normalizationEnabled) "0-1" else "Off")
                    }
                    AnalysisModuleId.MOTION -> AnalysisCard("Motion Summary") {
                        MetricRow("Distance", motion.distance.format())
                        MetricRow("Displacement", motion.displacement.format())
                        MetricRow("Direction", motion.directionDegrees.format("deg"))
                        MetricRow("Average Speed", motion.averageSpeed.format())
                        MetricRow("Average Acceleration", motion.averageAcceleration.format())
                        MetricRow("Average Jerk", motion.averageJerk.format())
                        MetricRow("Average Curvature", motion.averageCurvature.format())
                    }
                    AnalysisModuleId.GRAPH -> AnalysisCard("Visible Series") {
                        SectionLabel("Y Variable")
                        OptionGrid(state.ySeriesOptions)
                        Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                        SectionLabel("X Variable")
                        OptionGrid(state.xSeriesOptions)
                        Divider(color = PhysicsColorDivider.copy(alpha = 0.6f))
                        ToggleAction("Track A", state.showTrackA) { state.onShowTrackAChange(!state.showTrackA) }
                        ToggleAction("Track B", state.showTrackB) { state.onShowTrackBChange(!state.showTrackB) }
                        ToggleAction("Delta", state.showDelta) { state.onShowDeltaChange(!state.showDelta) }
                        Divider(color = PhysicsColorDivider.copy(alpha = 0.6f))
                        ToggleAction("Box Zoom", state.boxZoomEnabled) { state.onBoxZoomEnabledChange(!state.boxZoomEnabled) }
                        ToggleAction("Coordinate Details", state.showGraphDetails) { state.onShowGraphDetailsChange(!state.showGraphDetails) }
                        ExportButton("Auto Fit View", Modifier.fillMaxWidth(), state.onResetView)
                    }
                    AnalysisModuleId.FITTING -> AnalysisCard("Curve Fit") {
                        val realFitOptions = state.fitOptions.filter { it.mode != GraphProfileMode.CUSTOM }
                        realFitOptions.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { option ->
                                    OutlinedButton(
                                        onClick = option.onSelect,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (option.selected) PhysicsColorPrimary.copy(alpha = 0.28f) else PhysicsColorSurfaceVariant,
                                            contentColor = Color.White
                                        ),
                                        border = BorderStroke(1.dp, if (option.selected) PhysicsColorPrimary else PhysicsColorDivider),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(option.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        Divider(color = PhysicsColorDivider.copy(alpha = 0.6f))
                        val fit = state.selectedFit
                        FitDetails(fit)
                    }
                    AnalysisModuleId.STATISTICS -> AnalysisCard("Statistics") {
                        MetricRow("Count", statistics.count.toString())
                        MetricRow("Minimum", statistics.min.format())
                        MetricRow("Maximum", statistics.max.format())
                        MetricRow("Mean", statistics.mean.format())
                        MetricRow("Median", statistics.median.format())
                        MetricRow("Variance", statistics.variance.format())
                        MetricRow("Std Dev", statistics.standardDeviation.format())
                        MetricRow("RMS", statistics.rms.format())
                        Divider(color = PhysicsColorDivider.copy(alpha = 0.6f))
                        Text("Motion Physics", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                        MetricRow("Distance", motion.distance.format())
                        MetricRow("Displacement", motion.displacement.format())
                        MetricRow("Direction", motion.directionDegrees.format("deg"))
                        MetricRow("Mean Speed", motion.averageSpeed.format())
                        MetricRow("Mean Acceleration", motion.averageAcceleration.format())
                        MetricRow("Mean Jerk", motion.averageJerk.format())
                        MetricRow("Mean Curvature", motion.averageCurvature.format())
                        FormulaRow("v", "dx/dt, dy/dt, |v|")
                        FormulaRow("a", "dv/dt")
                        FormulaRow("jerk", "da/dt")
                        FormulaRow("curvature", "|vx*ay - vy*ax| / |v|^3")
                        Divider(color = PhysicsColorDivider.copy(alpha = 0.6f))
                        FitDetails(state.selectedFit)
                        Divider(color = PhysicsColorDivider.copy(alpha = 0.6f))
                        ExportButton("Export Statistics PDF", Modifier.fillMaxWidth(), state.onExportPdf)
                    }
                    AnalysisModuleId.SIGNAL -> AnalysisCard("Frequency Summary") {
                        MetricRow("FFT bins", spectrum.binCount.toString())
                        MetricRow("Dominant f", spectrum.dominantFrequencyHz.format("Hz"))
                        MetricRow("Amplitude", spectrum.dominantAmplitude.format())
                    }
                    AnalysisModuleId.MEASUREMENTS -> AnalysisCard("Selected Range") {
                        MetricRow("Rows", state.rows.size.toString())
                        MetricRow("First frame", state.rows.firstOrNull()?.frameNumber?.toString() ?: "-")
                        MetricRow("Last frame", state.rows.lastOrNull()?.frameNumber?.toString() ?: "-")
                        ToggleAction("Coordinate Details", state.showGraphDetails) { state.onShowGraphDetailsChange(!state.showGraphDetails) }
                        ToggleAction("Box Zoom", state.boxZoomEnabled) { state.onBoxZoomEnabledChange(!state.boxZoomEnabled) }
                    }
                    AnalysisModuleId.EXPORT -> AnalysisCard("Export") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExportButton("CSV", Modifier.weight(1f), state.onExportCsv)
                            ExportButton("XLSX", Modifier.weight(1f), state.onExportXlsx)
                            ExportButton("PDF", Modifier.weight(1f), state.onExportPdf)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisContextControls(
    state: AnalysisWorkspaceState,
    statistics: AnalysisStatistics,
    motion: AnalysisMotionSummary,
    spectrum: AnalysisSpectrumSummary
) {
    Surface(
        color = PhysicsColorSurface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state.selectedModule) {
                AnalysisModuleId.DATA -> {
                    ToggleAction("Pos avg", state.smoothingEnabled, Modifier.width(96.dp)) {
                        state.onSmoothingEnabledChange(!state.smoothingEnabled)
                    }
                    ToggleAction("Outliers", state.outlierFilterEnabled, Modifier.width(100.dp)) {
                        state.onOutlierFilterEnabledChange(!state.outlierFilterEnabled)
                    }
                    ToggleAction("Low-pass", state.lowPassFilterEnabled, Modifier.width(102.dp)) {
                        state.onLowPassFilterEnabledChange(!state.lowPassFilterEnabled)
                    }
                    ToggleAction("High-pass", state.highPassFilterEnabled, Modifier.width(104.dp)) {
                        state.onHighPassFilterEnabledChange(!state.highPassFilterEnabled)
                    }
                    ToggleAction("Band-pass", state.bandPassFilterEnabled, Modifier.width(104.dp)) {
                        state.onBandPassFilterEnabledChange(!state.bandPassFilterEnabled)
                    }
                    ToggleAction("Baseline", state.baselineCorrectionEnabled, Modifier.width(100.dp)) {
                        state.onBaselineCorrectionEnabledChange(!state.baselineCorrectionEnabled)
                    }
                    ToggleAction("Normalize", state.normalizationEnabled, Modifier.width(104.dp)) {
                        state.onNormalizationEnabledChange(!state.normalizationEnabled)
                    }
                    ToolbarSlider(
                        label = "Filter strength",
                        value = state.filterStrength,
                        onValueChange = state.onFilterStrengthChange
                    )
                    ToggleAction(if (state.tableVisible) "Hide Table" else "Show Table", state.tableVisible, Modifier.width(112.dp)) {
                        state.onTableVisibleChange(!state.tableVisible)
                    }
                    if (state.canUndo) ExportButton("Undo", Modifier.width(82.dp), state.onUndo)
                    if (state.canRedo) ExportButton("Redo", Modifier.width(82.dp), state.onRedo)
                    state.onDeleteSelectedPoint?.let { deleteSelectedPoint ->
                        ExportButton("Delete Point", Modifier.width(118.dp), deleteSelectedPoint)
                    }
                    ExportButton("Reset Curation", Modifier.width(136.dp), state.onResetCuration)
                }
                AnalysisModuleId.GRAPH -> {
                    ToggleAction("Coordinate Details", state.showGraphDetails, Modifier.width(154.dp)) {
                        state.onShowGraphDetailsChange(!state.showGraphDetails)
                    }
                    ToggleAction("Box Zoom", state.boxZoomEnabled, Modifier.width(104.dp)) {
                        state.onBoxZoomEnabledChange(!state.boxZoomEnabled)
                    }
                    ToggleAction("Track A", state.showTrackA, Modifier.width(92.dp)) { state.onShowTrackAChange(!state.showTrackA) }
                    ToggleAction("Track B", state.showTrackB, Modifier.width(92.dp)) { state.onShowTrackBChange(!state.showTrackB) }
                    ToggleAction("Delta", state.showDelta, Modifier.width(88.dp)) { state.onShowDeltaChange(!state.showDelta) }
                    ToggleAction(if (state.tableVisible) "Hide Table" else "Show Table", state.tableVisible, Modifier.width(112.dp)) {
                        state.onTableVisibleChange(!state.tableVisible)
                    }
                    ExportButton("Auto Fit View", Modifier.width(126.dp), state.onResetView)
                }
                AnalysisModuleId.FITTING -> {
                    val realFitOptions = state.fitOptions.filter { it.mode != GraphProfileMode.CUSTOM }
                    realFitOptions.forEach { option ->
                        ToggleAction(option.label, option.selected, Modifier.width(112.dp), option.onSelect)
                    }
                }
                AnalysisModuleId.SIGNAL -> {
                    ToggleAction("Low-pass", state.lowPassFilterEnabled, Modifier.width(104.dp)) {
                        state.onLowPassFilterEnabledChange(!state.lowPassFilterEnabled)
                    }
                    ToggleAction("High-pass", state.highPassFilterEnabled, Modifier.width(106.dp)) {
                        state.onHighPassFilterEnabledChange(!state.highPassFilterEnabled)
                    }
                    ToggleAction("Band-pass", state.bandPassFilterEnabled, Modifier.width(106.dp)) {
                        state.onBandPassFilterEnabledChange(!state.bandPassFilterEnabled)
                    }
                    ToggleAction("Pos avg", state.smoothingEnabled, Modifier.width(96.dp)) {
                        state.onSmoothingEnabledChange(!state.smoothingEnabled)
                    }
                    ToolbarMetric("FFT", "${spectrum.binCount} bins")
                    ToolbarMetric("Dominant", spectrum.dominantFrequencyHz.format("Hz"))
                }
                AnalysisModuleId.EXPORT -> {
                    ExportButton("Share CSV", Modifier.width(108.dp), state.onExportCsv)
                    ExportButton("Share XLSX", Modifier.width(112.dp), state.onExportXlsx)
                    ExportButton("Share PDF", Modifier.width(108.dp), state.onExportPdf)
                }
                AnalysisModuleId.MOTION -> {
                    ToolbarMetric("Distance", motion.distance.format())
                    ToolbarMetric("Mean Speed", motion.averageSpeed.format())
                    ToolbarMetric("Mean Accel", motion.averageAcceleration.format())
                    ToolbarMetric("Jerk", motion.averageJerk.format())
                    ToolbarMetric("Curvature", motion.averageCurvature.format())
                }
                AnalysisModuleId.STATISTICS -> {
                    ToolbarMetric("Mean", statistics.mean.format())
                    ToolbarMetric("Median", statistics.median.format())
                    ToolbarMetric("Std Dev", statistics.standardDeviation.format())
                    ToolbarMetric("RMS", statistics.rms.format())
                    ToolbarMetric("Min", statistics.min.format())
                    ToolbarMetric("Max", statistics.max.format())
                }
                AnalysisModuleId.MEASUREMENTS -> {
                    ToggleAction("Coordinate Details", state.showGraphDetails, Modifier.width(154.dp)) {
                        state.onShowGraphDetailsChange(!state.showGraphDetails)
                    }
                    ToggleAction("Box Zoom", state.boxZoomEnabled, Modifier.width(104.dp)) {
                        state.onBoxZoomEnabledChange(!state.boxZoomEnabled)
                    }
                    ExportButton("Auto Fit View", Modifier.width(126.dp), state.onResetView)
                }
            }
        }
    }
}

@Composable
private fun AnalysisCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF07130E), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        label,
        color = Color.White.copy(alpha = 0.74f),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

@Composable
private fun ToolbarMetric(label: String, value: String, modifier: Modifier = Modifier.width(146.dp)) {
    Column(
        modifier = modifier
            .height(42.dp)
            .background(PhysicsColorSurfaceVariant, RoundedCornerShape(6.dp))
            .border(1.dp, PhysicsColorDivider, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.58f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            value,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ToolbarSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .width(240.dp)
            .height(42.dp)
            .background(PhysicsColorSurfaceVariant, RoundedCornerShape(6.dp))
            .border(1.dp, PhysicsColorDivider, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.68f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = PhysicsColorPrimary,
                activeTrackColor = PhysicsColorPrimary,
                inactiveTrackColor = PhysicsColorDivider
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            String.format(Locale.US, "%.2f", value),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
private fun ToggleAction(label: String, selected: Boolean, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) PhysicsColorPrimary.copy(alpha = 0.24f) else PhysicsColorSurfaceVariant,
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, if (selected) PhysicsColorPrimary else PhysicsColorDivider),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
        modifier = modifier
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun OptionGrid(options: List<AnalysisSeriesOption>) {
    options.chunked(2).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { option ->
                OutlinedButton(
                    onClick = option.onSelect,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (option.selected) PhysicsColorPrimary.copy(alpha = 0.24f) else PhysicsColorSurfaceVariant,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, if (option.selected) PhysicsColorPrimary else PhysicsColorDivider),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(option.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlotPresetGrid(options: List<AnalysisPlotPreset>) {
    options.chunked(2).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { option ->
                OutlinedButton(
                    onClick = option.onSelect,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (option.selected) PhysicsColorPrimary.copy(alpha = 0.24f) else PhysicsColorSurfaceVariant,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, if (option.selected) PhysicsColorPrimary else PhysicsColorDivider),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(option.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun FitDetails(fit: com.physicstracker.app.ui.ProfileResult?) {
    if (fit == null) {
        MetricRow("Fit Model", "None")
        return
    }
    MetricRow("Fit Model", fit.label)
    Text(
        text = fit.equation,
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace
    )
    MetricRow("R2", fit.rSquared.format())
    MetricRow("RMSE", fit.rmse.format())
    MetricRow("MAE", fit.mae.format())
    MetricRow("Reduced Chi2", fit.reducedChiSq.format())
    fitDerivedRows(fit).forEach { (label, value) ->
        MetricRow(label, value)
    }
    if (fit.parameters.isNotEmpty()) {
        Divider(color = PhysicsColorDivider.copy(alpha = 0.45f))
        Text("Parameters", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
        fit.parameters.forEach { (name, value) ->
            val uncertainty = fit.parameterUncertainties.firstOrNull { it.first == name }?.second
            val display = if (uncertainty != null && uncertainty > 0.0) {
                "${value.format()} +/- ${uncertainty.format()}"
            } else {
                value.format()
            }
            MetricRow(name, display)
        }
    }
}

@Composable
private fun FormulaRow(label: String, formula: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        Text(formula, color = Color.White, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End)
    }
}

@Composable
private fun ExportButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = PhysicsColorSurfaceVariant,
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, PhysicsColorDivider),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
        modifier = modifier
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MetricRow(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

private fun activeSignalFilterLabel(state: AnalysisWorkspaceState): String = when {
    state.bandPassFilterEnabled -> "Band-pass"
    state.highPassFilterEnabled -> "High-pass"
    state.lowPassFilterEnabled -> "Low-pass"
    else -> "Off"
}

@Composable
private fun AnalysisDataTable(rows: List<AnalysisTableRow>, modifier: Modifier = Modifier) {
    Surface(
        color = PhysicsColorSurface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Synchronized Data Table", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("${rows.size} rows", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.labelSmall)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PhysicsColorSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeaderCell("Frame", 0.8f)
                HeaderCell("Time", 1f)
                HeaderCell("X", 1f)
                HeaderCell("Y", 1f)
                HeaderCell("Value", 1f, TextAlign.End)
                HeaderCell("Speed", 1f, TextAlign.End)
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows, key = { "${it.trackLabel}:${it.frameNumber}:${it.timestampUs}" }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BodyCell(row.frameNumber.toString(), 0.8f)
                        BodyCell(String.format(Locale.US, "%.4f", row.timeSeconds), 1f)
                        BodyCell(row.xValue.format(), 1f)
                        BodyCell(row.yValue.format(), 1f)
                        BodyCell(row.value.format(), 1f, TextAlign.End)
                        BodyCell(row.speed.format(), 1f, TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(label: String, weight: Float, textAlign: TextAlign = TextAlign.Start) {
    Text(label, color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(weight), textAlign = textAlign)
}

@Composable
private fun RowScope.BodyCell(value: String, weight: Float, textAlign: TextAlign = TextAlign.Start) {
    Text(
        value,
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.weight(weight),
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun Double?.format(suffix: String = ""): String {
    val value = this ?: return "-"
    val formatted = if (kotlin.math.abs(value) >= 10000.0 || kotlin.math.abs(value) < 0.001 && value != 0.0) {
        String.format(Locale.US, "%.4e", value)
    } else {
        String.format(Locale.US, "%.5f", value)
    }
    return if (suffix.isBlank()) formatted else "$formatted $suffix"
}

private fun fitDerivedRows(fit: com.physicstracker.app.ui.ProfileResult): List<Pair<String, String>> {
    val params = fit.parameters.toMap()
    val rows = mutableListOf<Pair<String, String>>()
    val omega = params["omega"] ?: params["omega_d"] ?: params["omega_1"]
    val amplitude = params["amplitude"] ?: params["A"] ?: params["amp"]
    val offset = params["offset"] ?: params["c"]
    val phase = params["phase"] ?: params["phi"]
    val damping = params["lambda"] ?: params["damping_beta"]
    amplitude?.let { rows += "Amplitude" to it.format() }
    offset?.let { rows += "Offset / Equilibrium" to it.format() }
    phase?.let { rows += "Phase" to it.format("rad") }
    omega?.takeIf { it > 0.0 }?.let {
        rows += "Angular Frequency" to it.format("rad/s")
        rows += "Frequency" to (it / (2.0 * Math.PI)).format("Hz")
        rows += "Period" to ((2.0 * Math.PI) / it).format("s")
    }
    damping?.takeIf { it > 0.0 }?.let {
        rows += "Damping" to it.format("1/s")
        rows += "Time Constant" to (1.0 / it).format("s")
        rows += "Half Life" to (kotlin.math.ln(2.0) / it).format("s")
        omega?.takeIf { value -> value > 0.0 }?.let { w ->
            rows += "Q Factor" to (w / (2.0 * it)).format()
        }
    }
    return rows
}
