package com.physicstracker.app.live.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.physicstracker.app.live.LivePhysicsPoint
import com.physicstracker.app.live.LiveTrackingViewModel
import com.physicstracker.app.ui.GraphPlotPoint
import com.physicstracker.app.ui.GraphProfileMode
import com.physicstracker.app.ui.GraphSample
import com.physicstracker.app.ui.GraphSeries
import com.physicstracker.app.ui.ProfileResult
import com.physicstracker.app.ui.PhysicsColorTrackA
import com.physicstracker.app.ui.fitProfile
import com.physicstracker.app.ui.analysis.AnalysisFitOption
import com.physicstracker.app.ui.analysis.AnalysisModuleId
import com.physicstracker.app.ui.analysis.AnalysisPlotPreset
import com.physicstracker.app.ui.analysis.AnalysisSeriesOption
import com.physicstracker.app.ui.analysis.AnalysisTableRow
import com.physicstracker.app.ui.analysis.AnalysisWorkspace
import com.physicstracker.app.ui.analysis.AnalysisWorkspaceState
import com.physicstracker.app.ui.GraphCanvasWidget
import com.physicstracker.app.ui.GraphDisplayMode

/**
 * Live ANALYSIS tab: the same analysis workspace used by the video Experiment feature
 * ([AnalysisWorkspace] module bar + inspector + data table) with the same graph widget
 * ([GraphCanvasWidget]). Only the data source differs: live physics series.
 */
@Composable
fun LiveAnalysisTab(
    points: List<LivePhysicsPoint>,
    tick: Int,
    calibrated: Boolean,
    viewModel: LiveTrackingViewModel,
    onBackToCamera: () -> Unit
) {
    // ---- series selection (mirrors the experiment workspace presets) ----
    var selectedSeries by remember { mutableStateOf(GraphSeries.Y) }
    var selectedSeriesX by remember { mutableStateOf(GraphSeries.TIME) }
    var selectedModule by remember { mutableStateOf(AnalysisModuleId.GRAPH) }
    var fullGraphVisible by remember { mutableStateOf(false) }
    var tableVisible by remember { mutableStateOf(false) }
    var boxZoomEnabled by remember { mutableStateOf(false) }
    var showGraphDetails by remember { mutableStateOf(false) }
    var smoothingEnabled by remember { mutableStateOf(false) }
    var graphDisplayMode by remember { mutableStateOf(GraphDisplayMode.ORIGINAL) }
    var selectedFitMode by remember { mutableStateOf(GraphProfileMode.NONE) }

    // ---- local viewport state for the shared graph widget ----
    var graphScaleX by remember { mutableFloatStateOf(1f) }
    var graphScaleY by remember { mutableFloatStateOf(1f) }
    var viewportCenterX by remember { mutableFloatStateOf(0f) }
    var viewportCenterY by remember { mutableFloatStateOf(0f) }
    var manualXMin by remember { mutableStateOf<Float?>(null) }
    var manualXMax by remember { mutableStateOf<Float?>(null) }
    var manualYMin by remember { mutableStateOf<Float?>(null) }
    var manualYMax by remember { mutableStateOf<Float?>(null) }

    fun seriesValue(p: LivePhysicsPoint, series: GraphSeries): Double? = when (series) {
        GraphSeries.X -> p.xMeters
        GraphSeries.Y -> p.yMeters
        GraphSeries.VX -> p.vxMetersPerSec
        GraphSeries.VY -> p.vyMetersPerSec
        GraphSeries.SPEED -> p.speedMetersPerSec
        GraphSeries.AX -> p.axMetersPerSec2
        GraphSeries.AY -> p.ayMetersPerSec2
        GraphSeries.ACCEL -> p.accelerationMetersPerSec2
        else -> null
    }

    fun seriesUnit(series: GraphSeries): String = when (series) {
        GraphSeries.X, GraphSeries.Y -> "m"
        GraphSeries.VX, GraphSeries.VY, GraphSeries.SPEED -> "m/s"
        GraphSeries.AX, GraphSeries.AY, GraphSeries.ACCEL -> "m/s²"
        else -> "s"
    }

    fun seriesName(series: GraphSeries): String = when (series) {
        GraphSeries.TIME -> "t"
        GraphSeries.X -> "x"
        GraphSeries.Y -> "y"
        GraphSeries.VX -> "vx"
        GraphSeries.VY -> "vy"
        GraphSeries.SPEED -> "speed"
        GraphSeries.AX -> "ax"
        GraphSeries.AY -> "ay"
        GraphSeries.ACCEL -> "a"
        else -> series.name.lowercase()
    }

    // ---- graph plot data from the live dataset ----
    val plotPoints: List<GraphPlotPoint> = remember(points, selectedSeries, selectedSeriesX, smoothingEnabled, tick) {
        val base = points.mapIndexedNotNull { index, p ->
            val y = seriesValue(p, selectedSeries) ?: return@mapIndexedNotNull null
            val xv = if (selectedSeriesX == GraphSeries.TIME) p.elapsedUs / 1e6f
                     else (seriesValue(p, selectedSeriesX)?.toFloat() ?: return@mapIndexedNotNull null)
            GraphPlotPoint(
                frameNumber = index + 1,
                timestampUs = p.elapsedUs,
                timeSeconds = p.elapsedUs / 1e6f,
                value = y.toFloat(),
                xValue = xv
            )
        }
        if (smoothingEnabled) com.physicstracker.app.ui.smoothGraphPlotPoints(base) else base
    }
    val rows: List<AnalysisTableRow> = remember(points, tick) {
        points.mapIndexed { index, p ->
            AnalysisTableRow(
                frameNumber = index + 1,
                timestampUs = p.elapsedUs,
                timeSeconds = p.elapsedUs / 1e6f,
                xValue = p.xMeters,
                yValue = p.yMeters,
                value = p.speedMetersPerSec ?: 0.0,
                velocityX = p.vxMetersPerSec,
                velocityY = p.vyMetersPerSec,
                speed = p.speedMetersPerSec,
                accelerationX = p.axMetersPerSec2,
                accelerationY = p.ayMetersPerSec2,
                acceleration = p.accelerationMetersPerSec2,
                trackLabel = "A"
            )
        }
    }
    val selectedFit: ProfileResult? = remember(plotPoints, selectedFitMode) {
        if (selectedFitMode == GraphProfileMode.NONE || plotPoints.size < 3) null
        else fitProfile(plotPoints.map { GraphSample(it.xValue.toDouble(), it.value.toDouble()) }, selectedFitMode)
    }

    // ---- viewport extents derived from data + user zoom/pan ----
    val xs = plotPoints.map { it.xValue }
    val ys = plotPoints.map { it.value }
    val dataMinX = xs.minOrNull() ?: 0f
    val dataMaxX = xs.maxOrNull() ?: 1f
    val dataMinY = ys.minOrNull() ?: 0f
    val dataMaxY = ys.maxOrNull() ?: 1f
    val baseTimeRange = (dataMaxX - dataMinX).let { if (it <= 0f) 1f else it }
    val baseValueRange = ((dataMaxY - dataMinY) * 1.2f).let { if (it <= 0f) 1f else it }
    val visibleTimeRange = baseTimeRange / graphScaleX
    val visibleValueRange = baseValueRange / graphScaleY
    if (viewportCenterX == 0f && plotPoints.isNotEmpty()) {
        viewportCenterX = dataMinX + baseTimeRange / 2
        viewportCenterY = dataMinY + baseValueRange / 2
    }
    val visibleStartTime = manualXMin ?: (viewportCenterX - visibleTimeRange / 2)
    val visibleEndTime = manualXMax ?: (viewportCenterX + visibleTimeRange / 2)
    val visibleMinValue = manualYMin ?: (viewportCenterY - visibleValueRange / 2)
    val visibleMaxValue = manualYMax ?: (viewportCenterY + visibleValueRange / 2)

    val seriesOptions = listOf(
        GraphSeries.X, GraphSeries.Y, GraphSeries.VX, GraphSeries.VY,
        GraphSeries.SPEED, GraphSeries.AX, GraphSeries.AY, GraphSeries.ACCEL
    ).map { s ->
        AnalysisSeriesOption(
            label = seriesName(s),
            selected = s == selectedSeries,
            onSelect = { selectedSeries = s }
        )
    }

    val fitModes = listOf(
        GraphProfileMode.NONE, GraphProfileMode.AUTO, GraphProfileMode.LINEAR,
        GraphProfileMode.POLY_2, GraphProfileMode.POLY_3, GraphProfileMode.POLY_4,
        GraphProfileMode.SINE, GraphProfileMode.DAMPED_SINE
    )
    val fitOptions = fitModes.map { mode ->
        AnalysisFitOption(
            mode = mode,
            label = mode.name.replace('_', ' ').lowercase().replace("none", "off"),
            selected = mode == selectedFitMode,
            onSelect = { selectedFitMode = mode }
        )
    }

    val presets = listOf(
        Triple("x vs t", GraphSeries.X, GraphSeries.TIME),
        Triple("y vs t", GraphSeries.Y, GraphSeries.TIME),
        Triple("speed vs t", GraphSeries.SPEED, GraphSeries.TIME),
        Triple("accel vs t", GraphSeries.ACCEL, GraphSeries.TIME),
        Triple("trajectory y vs x", GraphSeries.Y, GraphSeries.X)
    ).map { (label, ySer, xSer) ->
        AnalysisPlotPreset(
            label = label,
            selected = selectedSeries == ySer && selectedSeriesX == xSer,
            onSelect = { selectedSeries = ySer; selectedSeriesX = xSer }
        )
    }

    val state = AnalysisWorkspaceState(
        selectedModule = selectedModule,
        rows = rows,
        selectedSeriesLabel = seriesName(selectedSeries),
        selectedXAxisLabel = seriesName(selectedSeriesX),
        selectedFit = selectedFit,
        ySeriesOptions = seriesOptions,
        xSeriesOptions = listOf(
            AnalysisSeriesOption("t", selectedSeriesX == GraphSeries.TIME) { selectedSeriesX = GraphSeries.TIME },
            AnalysisSeriesOption("x", selectedSeriesX == GraphSeries.X) { selectedSeriesX = GraphSeries.X }
        ),
        plotPresets = presets,
        fitOptions = fitOptions,
        showTrackA = true,
        showTrackB = false,
        showDelta = false,
        tableVisible = tableVisible,
        boxZoomEnabled = boxZoomEnabled,
        showGraphDetails = showGraphDetails,
        smoothingEnabled = smoothingEnabled,
        outlierFilterEnabled = false,
        lowPassFilterEnabled = false,
        highPassFilterEnabled = false,
        bandPassFilterEnabled = false,
        baselineCorrectionEnabled = false,
        normalizationEnabled = false,
        filterStrength = 0.5f,
        onShowTrackAChange = {},
        onShowTrackBChange = {},
        onShowDeltaChange = {},
        onTableVisibleChange = { tableVisible = it },
        onBoxZoomEnabledChange = { boxZoomEnabled = it },
        onShowGraphDetailsChange = { showGraphDetails = it },
        onSmoothingEnabledChange = { smoothingEnabled = it },
        onOutlierFilterEnabledChange = {},
        onLowPassFilterEnabledChange = {},
        onHighPassFilterEnabledChange = {},
        onBandPassFilterEnabledChange = {},
        onBaselineCorrectionEnabledChange = {},
        onNormalizationEnabledChange = {},
        onFilterStrengthChange = {},
        onResetCuration = {
            smoothingEnabled = false
            graphScaleX = 1f; graphScaleY = 1f
            manualXMin = null; manualXMax = null; manualYMin = null; manualYMax = null
        },
        onDeleteSelectedPoint = null,
        canUndo = false,
        canRedo = false,
        onUndo = {},
        onRedo = {},
        onResetView = {
            graphScaleX = 1f; graphScaleY = 1f
            manualXMin = null; manualXMax = null; manualYMin = null; manualYMax = null
        },
        onExitFullScreen = onBackToCamera,
        onExportCsv = { viewModel.shareCurrentRunCsv() },
        onExportXlsx = { viewModel.shareCurrentRunCsv() },
        onExportPdf = { viewModel.shareCurrentRunCsv() }
    )

    if (plotPoints.size < 2) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Track and record an object to analyze it.",
                color = Color.White.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    val unit = seriesUnit(selectedSeries)
    val xUnit = if (selectedSeriesX == GraphSeries.TIME) "s" else seriesUnit(selectedSeriesX)

    AnalysisWorkspace(
        state = state,
        onSelectedModuleChange = { selectedModule = it },
        fullGraphVisible = fullGraphVisible,
        onFullGraphVisibleChange = { fullGraphVisible = it },
        modifier = Modifier.fillMaxSize()
    ) {
        GraphCanvasWidget(
            modifier = Modifier.fillMaxSize(),
            label = seriesName(selectedSeries),
            points = plotPoints,
            color = PhysicsColorTrackA,
            isPrimary = true,
            isBottomSubplot = true,
            visibleStartTime = visibleStartTime,
            visibleEndTime = visibleEndTime,
            visibleMinValue = visibleMinValue,
            visibleMaxValue = visibleMaxValue,
            dataMinX = dataMinX,
            dataMaxX = dataMaxX,
            dataMinY = dataMinY,
            dataMaxY = dataMaxY,
            baseTimeRange = baseTimeRange,
            baseValueRange = baseValueRange,
            visibleTimeRange = visibleTimeRange,
            visibleValueRange = visibleValueRange,
            graphScaleX = graphScaleX,
            onGraphScaleXChange = { graphScaleX = it },
            graphScaleY = graphScaleY,
            onGraphScaleYChange = { graphScaleY = it },
            viewportCenterX = viewportCenterX,
            onViewportCenterXChange = { viewportCenterX = it },
            viewportCenterY = viewportCenterY,
            onViewportCenterYChange = { viewportCenterY = it },
            manualXMin = manualXMin,
            onManualXMinChange = { manualXMin = it },
            manualXMax = manualXMax,
            onManualXMaxChange = { manualXMax = it },
            manualYMin = manualYMin,
            onManualYMinChange = { manualYMin = it },
            manualYMax = manualYMax,
            onManualYMaxChange = { manualYMax = it },
            analysisMode = true,
            showAnalysisGuides = false,
            photogateAFraction = 0f,
            onPhotogateAFractionChange = {},
            photogateBFraction = 0f,
            onPhotogateBFractionChange = {},
            useAnalysisRange = false,
            analysisRangeStartFraction = 0f,
            onAnalysisRangeStartFractionChange = {},
            analysisRangeEndFraction = 1f,
            onAnalysisRangeEndFractionChange = {},
            selectedSampleIndex = -1,
            onSampleSelected = {},
            primarySeriesPoints = plotPoints,
            graphDisplayMode = graphDisplayMode,
            curatedSeriesPoints = emptyList(),
            blendedSeriesPoints = emptyList(),
            profileResult = selectedFit,
            selectedSample = null,
            selectedSamplePrediction = null,
            selectedSampleResidual = null,
            closestApproachGraphPoint = null,
            onFocusClosestApproach = {},
            showScaleBars = false,
            showGraphDetails = showGraphDetails,
            boxZoomModeEnabled = boxZoomEnabled,
            onBoxZoomModeEnabledChange = { boxZoomEnabled = it },
            graphFullScreen = fullGraphVisible,
            onGraphFullScreenChange = { fullGraphVisible = it },
            displayedUnit = unit,
            selectedSeries = selectedSeries,
            selectedSeriesX = selectedSeriesX,
            xAxisTitle = seriesName(selectedSeriesX),
            yAxisTitle = seriesName(selectedSeries),
            xAxisUnits = xUnit,
            yAxisUnits = unit,
            xAxisLogScale = false,
            yAxisLogScale = false,
            showMinorGrid = true,
            useScientificNotation = true,
            axisDecimalPrecision = 3,
            tickInterval = null,
            peakDetectionEnabled = false,
            measurementCursorEnabled = showGraphDetails,
            derivativeEnabled = false,
            integrationEnabled = false,
            lowPassFilterEnabled = false,
            highPassFilterEnabled = false,
            bandPassFilterEnabled = false,
            fftEnabled = selectedModule == AnalysisModuleId.SIGNAL,
            baselineCorrectionEnabled = false,
            roiSelectionEnabled = false,
            forceShowCoordinates = showGraphDetails,
            showCompareTrackA = false,
            compareSeriesA = emptyList(),
            trackAColor = PhysicsColorTrackA,
            showCompareTrackB = false,
            compareSeriesB = emptyList(),
            trackBColor = PhysicsColorTrackA,
            showCompareDelta = false,
            deltaSeries = emptyList()
        )
    }
}
