package com.physicstracker.app.ui.analysis

import com.physicstracker.app.ui.GraphProfileMode
import com.physicstracker.app.ui.ProfileResult
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

enum class AnalysisModuleId(val label: String) {
    DATA("Data"),
    MOTION("Motion"),
    GRAPH("Graph"),
    FITTING("Fitting"),
    STATISTICS("Statistics"),
    SIGNAL("Signal"),
    MEASUREMENTS("Measurements"),
    EXPORT("Export")
}

data class AnalysisModuleSpec(
    val id: AnalysisModuleId,
    val title: String = id.label
)

data class AnalysisTableRow(
    val frameNumber: Int,
    val timestampUs: Long,
    val timeSeconds: Float,
    val xValue: Double?,
    val yValue: Double?,
    val value: Double,
    val velocityX: Double?,
    val velocityY: Double?,
    val speed: Double?,
    val accelerationX: Double?,
    val accelerationY: Double?,
    val acceleration: Double?,
    val trackLabel: String?
)

data class AnalysisFitOption(
    val mode: GraphProfileMode,
    val label: String,
    val selected: Boolean,
    val onSelect: () -> Unit
)

data class AnalysisSeriesOption(
    val label: String,
    val selected: Boolean,
    val onSelect: () -> Unit
)

data class AnalysisPlotPreset(
    val label: String,
    val selected: Boolean,
    val onSelect: () -> Unit
)

data class AnalysisWorkspaceState(
    val selectedModule: AnalysisModuleId,
    val rows: List<AnalysisTableRow>,
    val selectedSeriesLabel: String,
    val selectedXAxisLabel: String,
    val selectedFit: ProfileResult?,
    val ySeriesOptions: List<AnalysisSeriesOption>,
    val xSeriesOptions: List<AnalysisSeriesOption>,
    val plotPresets: List<AnalysisPlotPreset>,
    val fitOptions: List<AnalysisFitOption>,
    val showTrackA: Boolean,
    val showTrackB: Boolean,
    val showDelta: Boolean,
    val tableVisible: Boolean,
    val boxZoomEnabled: Boolean,
    val showGraphDetails: Boolean,
    val smoothingEnabled: Boolean,
    val outlierFilterEnabled: Boolean,
    val lowPassFilterEnabled: Boolean,
    val highPassFilterEnabled: Boolean,
    val bandPassFilterEnabled: Boolean,
    val baselineCorrectionEnabled: Boolean,
    val normalizationEnabled: Boolean,
    val filterStrength: Float,
    val onShowTrackAChange: (Boolean) -> Unit,
    val onShowTrackBChange: (Boolean) -> Unit,
    val onShowDeltaChange: (Boolean) -> Unit,
    val onTableVisibleChange: (Boolean) -> Unit,
    val onBoxZoomEnabledChange: (Boolean) -> Unit,
    val onShowGraphDetailsChange: (Boolean) -> Unit,
    val onSmoothingEnabledChange: (Boolean) -> Unit,
    val onOutlierFilterEnabledChange: (Boolean) -> Unit,
    val onLowPassFilterEnabledChange: (Boolean) -> Unit,
    val onHighPassFilterEnabledChange: (Boolean) -> Unit,
    val onBandPassFilterEnabledChange: (Boolean) -> Unit,
    val onBaselineCorrectionEnabledChange: (Boolean) -> Unit,
    val onNormalizationEnabledChange: (Boolean) -> Unit,
    val onFilterStrengthChange: (Float) -> Unit,
    val onResetCuration: () -> Unit,
    val onDeleteSelectedPoint: (() -> Unit)?,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onResetView: () -> Unit,
    val onExitFullScreen: () -> Unit,
    val onExportCsv: () -> Unit,
    val onExportXlsx: () -> Unit,
    val onExportPdf: () -> Unit
)

data class AnalysisStatistics(
    val count: Int,
    val min: Double?,
    val max: Double?,
    val mean: Double?,
    val median: Double?,
    val variance: Double?,
    val standardDeviation: Double?,
    val rms: Double?
)

data class AnalysisMotionSummary(
    val distance: Double?,
    val displacement: Double?,
    val directionDegrees: Double?,
    val averageSpeed: Double?,
    val averageAcceleration: Double?,
    val averageJerk: Double?,
    val averageCurvature: Double?
)

data class AnalysisSpectrumSummary(
    val binCount: Int,
    val dominantFrequencyHz: Double?,
    val dominantAmplitude: Double?
)

val DefaultAnalysisModules = listOf(
    AnalysisModuleSpec(AnalysisModuleId.DATA),
    AnalysisModuleSpec(AnalysisModuleId.MOTION),
    AnalysisModuleSpec(AnalysisModuleId.GRAPH),
    AnalysisModuleSpec(AnalysisModuleId.FITTING),
    AnalysisModuleSpec(AnalysisModuleId.STATISTICS),
    AnalysisModuleSpec(AnalysisModuleId.SIGNAL),
    AnalysisModuleSpec(AnalysisModuleId.MEASUREMENTS),
    AnalysisModuleSpec(AnalysisModuleId.EXPORT)
)

fun buildAnalysisStatistics(rows: List<AnalysisTableRow>): AnalysisStatistics {
    val values = rows.map { it.value }
    if (values.isEmpty()) {
        return AnalysisStatistics(0, null, null, null, null, null, null, null)
    }
    val mean = values.average()
    val sorted = values.sorted()
    val median = if (sorted.size % 2 == 1) {
        sorted[sorted.size / 2]
    } else {
        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
    }
    val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
    val rms = sqrt(values.sumOf { it * it } / values.size)
    return AnalysisStatistics(
        count = values.size,
        min = values.minOrNull(),
        max = values.maxOrNull(),
        mean = mean,
        median = median,
        variance = variance,
        standardDeviation = sqrt(variance),
        rms = rms
    )
}

fun buildAnalysisMotionSummary(rows: List<AnalysisTableRow>): AnalysisMotionSummary {
    val xyRows = rows.filter { it.xValue != null && it.yValue != null }
    val distance = xyRows.zipWithNext { left, right ->
        val dx = (right.xValue ?: 0.0) - (left.xValue ?: 0.0)
        val dy = (right.yValue ?: 0.0) - (left.yValue ?: 0.0)
        sqrt(dx * dx + dy * dy)
    }.sum().takeIf { xyRows.size >= 2 }
    val displacement = if (xyRows.size >= 2) {
        val first = xyRows.first()
        val last = xyRows.last()
        val dx = (last.xValue ?: 0.0) - (first.xValue ?: 0.0)
        val dy = (last.yValue ?: 0.0) - (first.yValue ?: 0.0)
        sqrt(dx * dx + dy * dy)
    } else null
    val direction = if (xyRows.size >= 2) {
        val first = xyRows.first()
        val last = xyRows.last()
        Math.toDegrees(atan2((last.yValue ?: 0.0) - (first.yValue ?: 0.0), (last.xValue ?: 0.0) - (first.xValue ?: 0.0)))
    } else null
    val speeds = rows.mapNotNull { it.speed }
    val accelerations = rows.mapNotNull { it.acceleration }
    val jerks = rows.zipWithNext().mapNotNull { (left, right) ->
        val a0 = left.acceleration ?: return@mapNotNull null
        val a1 = right.acceleration ?: return@mapNotNull null
        val dt = (right.timestampUs - left.timestampUs) / 1_000_000.0
        if (dt > 0.0) (a1 - a0) / dt else null
    }
    val curvatures = rows.mapNotNull { row ->
        val vx = row.velocityX ?: return@mapNotNull null
        val vy = row.velocityY ?: return@mapNotNull null
        val ax = row.accelerationX ?: return@mapNotNull null
        val ay = row.accelerationY ?: return@mapNotNull null
        val denom = (vx * vx + vy * vy).pow(1.5)
        if (denom <= 1e-12) null else kotlin.math.abs(vx * ay - vy * ax) / denom
    }
    return AnalysisMotionSummary(
        distance = distance,
        displacement = displacement,
        directionDegrees = direction,
        averageSpeed = speeds.takeIf { it.isNotEmpty() }?.average(),
        averageAcceleration = accelerations.takeIf { it.isNotEmpty() }?.average(),
        averageJerk = jerks.takeIf { it.isNotEmpty() }?.average(),
        averageCurvature = curvatures.takeIf { it.isNotEmpty() }?.average()
    )
}

fun buildAnalysisSpectrumSummary(rows: List<AnalysisTableRow>, maxBins: Int = 48): AnalysisSpectrumSummary {
    if (rows.size < 4) return AnalysisSpectrumSummary(0, null, null)
    val values = rows.map { it.value }
    val mean = values.average()
    val centered = values.map { it - mean }
    val duration = (rows.last().timeSeconds - rows.first().timeSeconds).toDouble()
    if (duration <= 0.0) return AnalysisSpectrumSummary(0, null, null)
    val n = centered.size
    val binLimit = minOf(maxBins, n / 2)
    var bestFrequency: Double? = null
    var bestAmplitude: Double? = null
    for (k in 1..binLimit) {
        var real = 0.0
        var imag = 0.0
        for (i in centered.indices) {
            val angle = -2.0 * Math.PI * k * i / n
            real += centered[i] * kotlin.math.cos(angle)
            imag += centered[i] * kotlin.math.sin(angle)
        }
        val amplitude = sqrt(real * real + imag * imag) / n
        if (bestAmplitude == null || amplitude > bestAmplitude) {
            bestAmplitude = amplitude
            bestFrequency = k / duration
        }
    }
    return AnalysisSpectrumSummary(binLimit, bestFrequency, bestAmplitude)
}
