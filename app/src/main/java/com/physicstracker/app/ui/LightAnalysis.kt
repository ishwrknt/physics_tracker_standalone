package com.physicstracker.app.ui

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.physicstracker.app.data.CalibrationEntity
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class LightPanelMode { PROFILE, TIME, COMPARE }
enum class LightWorkspaceTab { SELECT, GRAPH, TABLE, COMPARE, MORE }
enum class LightTool { POINT, LINE, BOX }
enum class LightChannel { GRAY, R, G, B }
enum class LightSlot { A, B, C, D }
enum class LightDragHandle { START, END, BODY }
enum class LightCompareSource { PROFILE, TIME }

data class LightGeometry(
    val tool: LightTool,
    val start: Offset,
    val end: Offset
)

data class LightSamplePoint(
    val axisValue: Double,
    val intensity: Double
)

data class LightTimePoint(
    val timestampUs: Long,
    val intensity: Double
)

data class LightStats(
    val sampleCount: Int,
    val min: Double?,
    val max: Double?,
    val mean: Double?,
    val peakPositions: List<Double>,
    val peakSpacing: Double?,
    val contrast: Double?,
    val fwhm: Double?
)

data class LightSeries(
    val label: String,
    val samples: List<LightSamplePoint>,
    val color: Color
)

data class LightAnalysisExport(
    val modeLabel: String,
    val channelLabel: String,
    val unitLabel: String,
    val geometrySummary: String,
    val focusedSeries: List<GraphSample>,
    val compareSeries: List<LightExportSeries>,
    val statsLines: List<String>,
    val tableRows: List<String>
)

data class LightExportSeries(
    val label: String,
    val samples: List<GraphSample>,
    val color: Int
)

fun lightDefaultGeometry(tool: LightTool, canvasSize: IntSize, slot: LightSlot): LightGeometry {
    val width = canvasSize.width.toFloat().coerceAtLeast(480f)
    val height = canvasSize.height.toFloat().coerceAtLeast(480f)
    val centerX = width * when (slot) {
        LightSlot.A -> 0.35f
        LightSlot.B -> 0.45f
        LightSlot.C -> 0.55f
        LightSlot.D -> 0.65f
    }
    val centerY = height * 0.45f
    return when (tool) {
        LightTool.POINT -> LightGeometry(tool, Offset(centerX, centerY), Offset(centerX, centerY))
        LightTool.LINE -> LightGeometry(tool, Offset(centerX - 80f, centerY), Offset(centerX + 80f, centerY))
        LightTool.BOX -> LightGeometry(tool, Offset(centerX - 70f, centerY - 50f), Offset(centerX + 70f, centerY + 50f))
    }
}

fun lightGeometrySummary(geometry: LightGeometry?, calibration: CalibrationEntity?): String {
    geometry ?: return "No light measurement geometry"
    val lengthPx = hypot((geometry.end.x - geometry.start.x).toDouble(), (geometry.end.y - geometry.start.y).toDouble())
    val metrics = calibration?.let(::calibrationMetrics)
    val lengthLabel = if (metrics != null) {
        val unit = calibration.calibrationUnit
        val value = (lengthPx * metrics.metersPerPixel) / calibrationUnitFactor(unit)
        "${formatCalibratedValue(value)} $unit"
    } else {
        "${formatCalibratedValue(lengthPx)} px"
    }
    return when (geometry.tool) {
        LightTool.POINT -> "Point at (${geometry.start.x.roundToInt()}, ${geometry.start.y.roundToInt()})"
        LightTool.LINE -> "Line ${lengthLabel} from (${geometry.start.x.roundToInt()}, ${geometry.start.y.roundToInt()}) to (${geometry.end.x.roundToInt()}, ${geometry.end.y.roundToInt()})"
        LightTool.BOX -> "Box from (${geometry.start.x.roundToInt()}, ${geometry.start.y.roundToInt()}) to (${geometry.end.x.roundToInt()}, ${geometry.end.y.roundToInt()})"
    }
}

private fun lightVideoDisplayRect(videoWidth: Int, videoHeight: Int, canvasSize: IntSize): Rect {
    val canvasWidth = canvasSize.width.toFloat()
    val canvasHeight = canvasSize.height.toFloat()
    val scale = min(canvasWidth / videoWidth.toFloat(), canvasHeight / videoHeight.toFloat())
    val contentWidth = videoWidth * scale
    val contentHeight = videoHeight * scale
    val left = ((canvasWidth - contentWidth) / 2f).toInt()
    val top = ((canvasHeight - contentHeight) / 2f).toInt()
    return Rect(left, top, (left + contentWidth).toInt(), (top + contentHeight).toInt())
}

private fun lightScreenToVideoOffset(offset: Offset, videoWidth: Int, videoHeight: Int, canvasSize: IntSize): Offset? {
    val rect = lightVideoDisplayRect(videoWidth, videoHeight, canvasSize)
    if (offset.x < rect.left || offset.x > rect.right || offset.y < rect.top || offset.y > rect.bottom) return null
    val x = ((offset.x - rect.left) / rect.width().coerceAtLeast(1)) * videoWidth
    val y = ((offset.y - rect.top) / rect.height().coerceAtLeast(1)) * videoHeight
    return Offset(x, y)
}

private fun sampleChannel(bitmap: Bitmap, x: Int, y: Int, channel: LightChannel): Double {
    val safeX = x.coerceIn(0, bitmap.width - 1)
    val safeY = y.coerceIn(0, bitmap.height - 1)
    val pixel = bitmap.getPixel(safeX, safeY)
    val r = android.graphics.Color.red(pixel).toDouble()
    val g = android.graphics.Color.green(pixel).toDouble()
    val b = android.graphics.Color.blue(pixel).toDouble()
    return when (channel) {
        LightChannel.GRAY -> 0.299 * r + 0.587 * g + 0.114 * b
        LightChannel.R -> r
        LightChannel.G -> g
        LightChannel.B -> b
    }
}

private fun smoothLightValues(values: List<Double>): List<Double> {
    if (values.size < 3) return values
    return values.mapIndexed { index, _ ->
        val start = (index - 1).coerceAtLeast(0)
        val end = (index + 1).coerceAtMost(values.lastIndex)
        values.subList(start, end + 1).average()
    }
}

private fun backgroundLevel(values: List<Double>): Double = values.minOrNull() ?: 0.0

fun lightDistanceToSegment(point: Offset, start: Offset, end: Offset): Float {
    val line = end - start
    val lengthSquared = line.x * line.x + line.y * line.y
    if (lengthSquared == 0f) return hypot((point.x - start.x).toDouble(), (point.y - start.y).toDouble()).toFloat()
    val t = (((point.x - start.x) * line.x) + ((point.y - start.y) * line.y)) / lengthSquared
    val clamped = t.coerceIn(0f, 1f)
    val projection = Offset(start.x + line.x * clamped, start.y + line.y * clamped)
    return hypot((point.x - projection.x).toDouble(), (point.y - projection.y).toDouble()).toFloat()
}

fun buildLightProfile(
    bitmap: Bitmap,
    geometry: LightGeometry?,
    channel: LightChannel,
    canvasSize: IntSize,
    calibration: CalibrationEntity?,
    backgroundSubtract: Boolean,
    smoothing: Boolean
): List<LightSamplePoint> {
    geometry ?: return emptyList()
    if (bitmap.width <= 1 || bitmap.height <= 1) return emptyList()
    val metrics = calibration?.let(::calibrationMetrics)
    val unitFactor = calibration?.let { calibrationUnitFactor(it.calibrationUnit) } ?: 1.0
    fun axisValue(pixelDistance: Double): Double =
        if (metrics != null && calibration != null) (pixelDistance * metrics.metersPerPixel) / unitFactor else pixelDistance

    val raw = when (geometry.tool) {
        LightTool.POINT -> {
            val point = lightScreenToVideoOffset(geometry.start, bitmap.width, bitmap.height, canvasSize) ?: return emptyList()
            listOf(sampleChannel(bitmap, point.x.roundToInt(), point.y.roundToInt(), channel))
        }
        LightTool.LINE -> {
            val start = lightScreenToVideoOffset(geometry.start, bitmap.width, bitmap.height, canvasSize) ?: return emptyList()
            val end = lightScreenToVideoOffset(geometry.end, bitmap.width, bitmap.height, canvasSize) ?: return emptyList()
            val steps = max(24, hypot((end.x - start.x).toDouble(), (end.y - start.y).toDouble()).roundToInt())
            (0..steps).map { step ->
                val t = step.toFloat() / steps.toFloat().coerceAtLeast(1f)
                val x = start.x + (end.x - start.x) * t
                val y = start.y + (end.y - start.y) * t
                sampleChannel(bitmap, x.roundToInt(), y.roundToInt(), channel)
            }
        }
        LightTool.BOX -> {
            val start = lightScreenToVideoOffset(geometry.start, bitmap.width, bitmap.height, canvasSize) ?: return emptyList()
            val end = lightScreenToVideoOffset(geometry.end, bitmap.width, bitmap.height, canvasSize) ?: return emptyList()
            val left = min(start.x, end.x).roundToInt().coerceIn(0, bitmap.width - 1)
            val right = max(start.x, end.x).roundToInt().coerceIn(0, bitmap.width - 1)
            val top = min(start.y, end.y).roundToInt().coerceIn(0, bitmap.height - 1)
            val bottom = max(start.y, end.y).roundToInt().coerceIn(0, bitmap.height - 1)
            (left..right).map { x ->
                val values = (top..bottom).map { y -> sampleChannel(bitmap, x, y, channel) }
                values.average()
            }
        }
    }
    val adjusted = if (backgroundSubtract) {
        val background = backgroundLevel(raw)
        raw.map { (it - background).coerceAtLeast(0.0) }
    } else raw
    val values = if (smoothing) smoothLightValues(adjusted) else adjusted
    return values.mapIndexed { index, intensity ->
        LightSamplePoint(
            axisValue = when (geometry.tool) {
                LightTool.POINT -> 0.0
                LightTool.LINE -> {
                    val total = max(values.lastIndex, 1)
                    val lengthPx = hypot((geometry.end.x - geometry.start.x).toDouble(), (geometry.end.y - geometry.start.y).toDouble())
                    axisValue((lengthPx * index) / total)
                }
                LightTool.BOX -> axisValue(index.toDouble())
            },
            intensity = intensity
        )
    }
}

fun sampleLightIntensityValue(
    bitmap: Bitmap,
    geometry: LightGeometry?,
    channel: LightChannel,
    canvasSize: IntSize,
    backgroundSubtract: Boolean
): Double? {
    geometry ?: return null
    val values = buildLightProfile(
        bitmap = bitmap,
        geometry = geometry,
        channel = channel,
        canvasSize = canvasSize,
        calibration = null,
        backgroundSubtract = backgroundSubtract,
        smoothing = false
    ).map { it.intensity }
    return values.takeIf { it.isNotEmpty() }?.average()
}

fun buildLightStats(samples: List<LightSamplePoint>, detectPeaks: Boolean): LightStats {
    val values = samples.map { it.intensity }
    val peaks = if (detectPeaks && values.size >= 3) {
        (1 until values.lastIndex).mapNotNull { index ->
            val current = values[index]
            if (current > values[index - 1] && current >= values[index + 1]) samples[index].axisValue else null
        }
    } else {
        emptyList()
    }
    val peakSpacing = peaks.zipWithNext().map { (left, right) -> right - left }.takeIf { it.isNotEmpty() }?.average()
    val minValue = values.minOrNull()
    val maxValue = values.maxOrNull()
    val halfMax = if (minValue != null && maxValue != null) minValue + (maxValue - minValue) * 0.5 else null
    val aboveHalf = if (halfMax != null) samples.filter { it.intensity >= halfMax } else emptyList()
    val fwhm = if (aboveHalf.size >= 2) aboveHalf.last().axisValue - aboveHalf.first().axisValue else null
    val contrast = if (minValue != null && maxValue != null && (maxValue + minValue) > 1e-6) {
        (maxValue - minValue) / (maxValue + minValue)
    } else {
        null
    }
    return LightStats(
        sampleCount = samples.size,
        min = minValue,
        max = maxValue,
        mean = values.takeIf { it.isNotEmpty() }?.average(),
        peakPositions = peaks,
        peakSpacing = peakSpacing,
        contrast = contrast,
        fwhm = fwhm
    )
}

fun updateLightTimeSeries(
    target: MutableList<LightTimePoint>,
    timestampUs: Long,
    intensity: Double
) {
    val existingIndex = target.indexOfFirst { abs(it.timestampUs - timestampUs) <= 1_000L }
    if (existingIndex >= 0) {
        target[existingIndex] = LightTimePoint(timestampUs, intensity)
    } else {
        target += LightTimePoint(timestampUs, intensity)
        target.sortBy { it.timestampUs }
    }
}

fun lightGeometryHitHandle(
    geometry: LightGeometry?,
    touch: Offset,
    density: Float
): LightDragHandle? {
    geometry ?: return null
    val startRadius = 40f * density
    val endRadius = 40f * density
    val startDistance = hypot((touch.x - geometry.start.x).toDouble(), (touch.y - geometry.start.y).toDouble()).toFloat()
    val endDistance = hypot((touch.x - geometry.end.x).toDouble(), (touch.y - geometry.end.y).toDouble()).toFloat()
    if (startDistance <= startRadius && startDistance <= endDistance) return LightDragHandle.START
    if (endDistance <= endRadius) return LightDragHandle.END
    return when (geometry.tool) {
        LightTool.POINT -> if (startDistance <= 52f * density) LightDragHandle.BODY else null
        LightTool.LINE, LightTool.BOX -> {
            val left = min(geometry.start.x, geometry.end.x)
            val right = max(geometry.start.x, geometry.end.x)
            val top = min(geometry.start.y, geometry.end.y)
            val bottom = max(geometry.start.y, geometry.end.y)
            if (geometry.tool == LightTool.BOX && touch.x in left..right && touch.y in top..bottom) {
                LightDragHandle.BODY
            } else {
                val distance = lightDistanceToSegment(touch, geometry.start, geometry.end)
                if (distance <= 36f * density) LightDragHandle.BODY else null
            }
        }
    }
}

fun moveLightGeometry(
    geometry: LightGeometry,
    handle: LightDragHandle,
    delta: Offset,
    canvasSize: IntSize
): LightGeometry {
    val maxX = canvasSize.width.toFloat().coerceAtLeast(1f)
    val maxY = canvasSize.height.toFloat().coerceAtLeast(1f)
    fun clamp(point: Offset): Offset = Offset(point.x.coerceIn(0f, maxX), point.y.coerceIn(0f, maxY))
    return when (handle) {
        LightDragHandle.START -> geometry.copy(start = clamp(geometry.start + delta))
        LightDragHandle.END -> geometry.copy(end = clamp(geometry.end + delta))
        LightDragHandle.BODY -> {
            val minAllowedDx = -min(geometry.start.x, geometry.end.x)
            val maxAllowedDx = maxX - max(geometry.start.x, geometry.end.x)
            val minAllowedDy = -min(geometry.start.y, geometry.end.y)
            val maxAllowedDy = maxY - max(geometry.start.y, geometry.end.y)
            val translated = Offset(
                x = delta.x.coerceIn(minAllowedDx, maxAllowedDx),
                y = delta.y.coerceIn(minAllowedDy, maxAllowedDy)
            )
            geometry.copy(
                start = geometry.start + translated,
                end = geometry.end + translated
            )
        }
    }
}

fun lightSeriesToGraphSamples(series: List<LightSamplePoint>): List<GraphSample> =
    series.map { GraphSample(it.axisValue, it.intensity) }

private fun lightAxisLabel(mode: LightPanelMode, unitLabel: String): String =
    when (mode) {
        LightPanelMode.PROFILE -> "Position ($unitLabel)"
        LightPanelMode.TIME -> "Time (s)"
        LightPanelMode.COMPARE -> "Compare"
    }

@Composable
fun FloatingLightPanel(
    workspaceTab: LightWorkspaceTab,
    onWorkspaceTabSelected: (LightWorkspaceTab) -> Unit,
    mode: LightPanelMode,
    onModeSelected: (LightPanelMode) -> Unit,
    compareSource: LightCompareSource,
    onCompareSourceSelected: (LightCompareSource) -> Unit,
    tool: LightTool,
    onToolSelected: (LightTool) -> Unit,
    channel: LightChannel,
    onChannelSelected: (LightChannel) -> Unit,
    dualMode: Boolean,
    onDualModeChange: (Boolean) -> Unit,
    activeSlot: LightSlot,
    onActiveSlotSelected: (LightSlot) -> Unit,
    showGraphA: Boolean,
    showGraphB: Boolean,
    showGraphC: Boolean,
    showGraphD: Boolean,
    onShowGraphAChange: (Boolean) -> Unit,
    onShowGraphBChange: (Boolean) -> Unit,
    onShowGraphCChange: (Boolean) -> Unit,
    onShowGraphDChange: (Boolean) -> Unit,
    showTableA: Boolean,
    showTableB: Boolean,
    showTableC: Boolean,
    showTableD: Boolean,
    onShowTableAChange: (Boolean) -> Unit,
    onShowTableBChange: (Boolean) -> Unit,
    onShowTableCChange: (Boolean) -> Unit,
    onShowTableDChange: (Boolean) -> Unit,
    backgroundSubtract: Boolean,
    onBackgroundSubtractChange: (Boolean) -> Unit,
    smoothing: Boolean,
    onSmoothingChange: (Boolean) -> Unit,
    detectPeaks: Boolean,
    onDetectPeaksChange: (Boolean) -> Unit,
    timeRecording: Boolean,
    onStartTimeRecording: () -> Unit,
    onStopTimeRecording: () -> Unit,
    onClearAllTimeSeries: () -> Unit,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit,
    onExportPdf: () -> Unit,
    showOptions: Boolean,
    onShowOptionsChange: (Boolean) -> Unit,
    slotAColor: Color,
    slotBColor: Color,
    slotCColor: Color,
    slotDColor: Color,
    unitLabel: String,
    geometryA: LightGeometry?,
    geometryB: LightGeometry?,
    geometryC: LightGeometry?,
    geometryD: LightGeometry?,
    profileA: List<LightSamplePoint>,
    profileB: List<LightSamplePoint>,
    profileC: List<LightSamplePoint>,
    profileD: List<LightSamplePoint>,
    timeA: List<LightTimePoint>,
    timeB: List<LightTimePoint>,
    timeC: List<LightTimePoint>,
    timeD: List<LightTimePoint>,
    onClearTimeSeries: (LightSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val detailsScroll = rememberScrollState()
    val chartSeries = remember(workspaceTab, mode, compareSource, activeSlot, profileA, profileB, profileC, profileD, timeA, timeB, timeC, timeD, showGraphA, showGraphB, showGraphC, showGraphD) {
        when (mode) {
            LightPanelMode.PROFILE -> {
                buildList {
                    if (showGraphA) add(LightSeries("A", profileA, slotAColor))
                    if (dualMode && showGraphB) add(LightSeries("B", profileB, slotBColor))
                    if (dualMode && showGraphC) add(LightSeries("C", profileC, slotCColor))
                    if (dualMode && showGraphD) add(LightSeries("D", profileD, slotDColor))
                }.ifEmpty {
                    listOf(
                        LightSeries(
                            label = "Profile ${activeSlot.name}",
                            samples = when (activeSlot) {
                                LightSlot.A -> profileA
                                LightSlot.B -> profileB
                                LightSlot.C -> profileC
                                LightSlot.D -> profileD
                            },
                            color = when (activeSlot) {
                                LightSlot.A -> slotAColor
                                LightSlot.B -> slotBColor
                                LightSlot.C -> slotCColor
                                LightSlot.D -> slotDColor
                            }
                        )
                    )
                }
            }
            LightPanelMode.TIME -> {
                buildList {
                    if (showGraphA) add(LightSeries("A", timeA.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }, slotAColor))
                    if (dualMode && showGraphB) add(LightSeries("B", timeB.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }, slotBColor))
                    if (dualMode && showGraphC) add(LightSeries("C", timeC.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }, slotCColor))
                    if (dualMode && showGraphD) add(LightSeries("D", timeD.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }, slotDColor))
                }.ifEmpty {
                    val slot = activeSlot
                    listOf(
                        LightSeries(
                            label = "Time ${slot.name}",
                            samples = when (slot) {
                                LightSlot.A -> timeA
                                LightSlot.B -> timeB
                                LightSlot.C -> timeC
                                LightSlot.D -> timeD
                            }.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) },
                            color = when (slot) {
                                LightSlot.A -> slotAColor
                                LightSlot.B -> slotBColor
                                LightSlot.C -> slotCColor
                                LightSlot.D -> slotDColor
                            }
                        )
                    )
                }
            }
            LightPanelMode.COMPARE -> {
                if (compareSource == LightCompareSource.PROFILE) {
                    buildList {
                        if (showGraphA) add(LightSeries("A", profileA, slotAColor))
                        if (dualMode && showGraphB) add(LightSeries("B", profileB, slotBColor))
                        if (dualMode && showGraphC) add(LightSeries("C", profileC, slotCColor))
                        if (dualMode && showGraphD) add(LightSeries("D", profileD, slotDColor))
                    }
                } else {
                    buildList {
                        if (showGraphA) add(LightSeries("A", timeA.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }, slotAColor))
                        if (dualMode && showGraphB) add(LightSeries("B", timeB.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }, slotBColor))
                        if (dualMode && showGraphC) add(LightSeries("C", timeC.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }, slotCColor))
                        if (dualMode && showGraphD) add(LightSeries("D", timeD.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }, slotDColor))
                    }
                }
            }
        }.filter { it.samples.isNotEmpty() }
    }
    val focusedStats = remember(mode, compareSource, activeSlot, profileA, profileB, profileC, profileD, timeA, timeB, timeC, timeD, detectPeaks) {
        val source = when (mode) {
            LightPanelMode.PROFILE -> when (activeSlot) {
                LightSlot.A -> profileA
                LightSlot.B -> profileB
                LightSlot.C -> profileC
                LightSlot.D -> profileD
            }
            LightPanelMode.TIME -> when (activeSlot) {
                LightSlot.A -> timeA
                LightSlot.B -> timeB
                LightSlot.C -> timeC
                LightSlot.D -> timeD
            }.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }
            LightPanelMode.COMPARE -> if (compareSource == LightCompareSource.PROFILE) profileA else timeA.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }
        }
        buildLightStats(source, detectPeaks)
    }
    Surface(
        color = Color.Black.copy(alpha = 0.86f),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 920.dp)
            .heightIn(min = 300.dp, max = 620.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Light ${workspaceTab.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    color = slotAColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace
                )
                if (workspaceTab != LightWorkspaceTab.MORE) {
                    LightDeckButton("MORE", accent = slotBColor) {
                        onWorkspaceTabSelected(LightWorkspaceTab.MORE)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LightWorkspaceTab.values().forEach { item ->
                    LightDeckButton(
                        item.name,
                        selected = workspaceTab == item,
                        accent = when (item) {
                            LightWorkspaceTab.SELECT -> slotAColor
                            LightWorkspaceTab.GRAPH -> Color(0xFF00E5FF)
                            LightWorkspaceTab.TABLE -> Color(0xFF83D49A)
                            LightWorkspaceTab.COMPARE -> slotBColor
                            LightWorkspaceTab.MORE -> Color(0xFFFFC107)
                        }
                    ) {
                        onWorkspaceTabSelected(item)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (workspaceTab == LightWorkspaceTab.GRAPH || workspaceTab == LightWorkspaceTab.COMPARE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(248.dp)
                        .background(Color.White.copy(alpha = 0.03f))
                ) {
                    if (chartSeries.any { it.samples.size >= 1 }) {
                        LightChart(
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            series = chartSeries,
                            axisLabel = lightAxisLabel(if (mode == LightPanelMode.COMPARE && compareSource == LightCompareSource.TIME) LightPanelMode.TIME else mode, unitLabel),
                            intensityLabel = "Intensity"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(detailsScroll),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (workspaceTab) {
                    LightWorkspaceTab.SELECT -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LightTool.values().forEach { item ->
                                LightDeckButton(item.name, selected = tool == item, accent = Color(0xFF00E5FF)) {
                                    onToolSelected(item)
                                }
                            }
                            LightDeckButton("A", selected = activeSlot == LightSlot.A, accent = slotAColor) { onActiveSlotSelected(LightSlot.A) }
                            if (dualMode) {
                                LightDeckButton("B", selected = activeSlot == LightSlot.B, accent = slotBColor) { onActiveSlotSelected(LightSlot.B) }
                                LightDeckButton("C", selected = activeSlot == LightSlot.C, accent = slotCColor) { onActiveSlotSelected(LightSlot.C) }
                                LightDeckButton("D", selected = activeSlot == LightSlot.D, accent = slotDColor) { onActiveSlotSelected(LightSlot.D) }
                            }
                        }
                        LightGeometryBlock("A", slotAColor, geometryA)
                        if (dualMode) {
                            LightGeometryBlock("B", slotBColor, geometryB)
                            LightGeometryBlock("C", slotCColor, geometryC)
                            LightGeometryBlock("D", slotDColor, geometryD)
                        }
                    }
                    LightWorkspaceTab.GRAPH -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LightDeckButton("PROFILE", selected = mode == LightPanelMode.PROFILE, accent = slotAColor) {
                                onModeSelected(LightPanelMode.PROFILE)
                            }
                            LightDeckButton("TIME", selected = mode == LightPanelMode.TIME, accent = slotBColor) {
                                onModeSelected(LightPanelMode.TIME)
                            }
                            LightDeckButton("A", selected = activeSlot == LightSlot.A, accent = slotAColor) { onActiveSlotSelected(LightSlot.A) }
                            if (dualMode) {
                                LightDeckButton("B", selected = activeSlot == LightSlot.B, accent = slotBColor) { onActiveSlotSelected(LightSlot.B) }
                                LightDeckButton("C", selected = activeSlot == LightSlot.C, accent = slotCColor) { onActiveSlotSelected(LightSlot.C) }
                                LightDeckButton("D", selected = activeSlot == LightSlot.D, accent = slotDColor) { onActiveSlotSelected(LightSlot.D) }
                            }
                            // Slot visibility toggles
                            LightDeckButton("A\u2714", selected = showGraphA, accent = slotAColor) { onShowGraphAChange(!showGraphA) }
                            if (dualMode) {
                                LightDeckButton("B\u2714", selected = showGraphB, accent = slotBColor) { onShowGraphBChange(!showGraphB) }
                                LightDeckButton("C\u2714", selected = showGraphC, accent = slotCColor) { onShowGraphCChange(!showGraphC) }
                                LightDeckButton("D\u2714", selected = showGraphD, accent = slotDColor) { onShowGraphDChange(!showGraphD) }
                            }
                            if (mode == LightPanelMode.TIME) {
                                LightDeckButton(if (timeRecording) "STOP" else "START", selected = timeRecording, accent = if (timeRecording) Color(0xFFE98D85) else Color(0xFF83D49A)) {
                                    if (timeRecording) onStopTimeRecording() else onStartTimeRecording()
                                }
                                LightDeckButton("CLEAR", accent = Color(0xFFE98D85)) { onClearAllTimeSeries() }
                            }
                            LightDeckButton("CSV", accent = Color(0xFF83D49A)) { onExportCsv() }
                            LightDeckButton("XLSX", accent = Color(0xFF83D49A)) { onExportXlsx() }
                            LightDeckButton("PDF", accent = Color(0xFF83D49A)) { onExportPdf() }
                        }
                        LightStatsBlock(
                            title = "Stats",
                            stats = focusedStats,
                            unitLabel = if (mode == LightPanelMode.TIME) "s" else unitLabel
                        )
                    }
                    LightWorkspaceTab.TABLE -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LightDeckButton("PROFILE", selected = mode == LightPanelMode.PROFILE, accent = slotAColor) {
                                onModeSelected(LightPanelMode.PROFILE)
                            }
                            LightDeckButton("TIME", selected = mode == LightPanelMode.TIME, accent = slotBColor) {
                                onModeSelected(LightPanelMode.TIME)
                            }
                            LightDeckButton("A", selected = activeSlot == LightSlot.A, accent = slotAColor) { onActiveSlotSelected(LightSlot.A) }
                            if (dualMode) {
                                LightDeckButton("B", selected = activeSlot == LightSlot.B, accent = slotBColor) { onActiveSlotSelected(LightSlot.B) }
                                LightDeckButton("C", selected = activeSlot == LightSlot.C, accent = slotCColor) { onActiveSlotSelected(LightSlot.C) }
                                LightDeckButton("D", selected = activeSlot == LightSlot.D, accent = slotDColor) { onActiveSlotSelected(LightSlot.D) }
                            }
                            // Table visibility toggles
                            LightDeckButton("A\u2714", selected = showTableA, accent = slotAColor) { onShowTableAChange(!showTableA) }
                            if (dualMode) {
                                LightDeckButton("B\u2714", selected = showTableB, accent = slotBColor) { onShowTableBChange(!showTableB) }
                                LightDeckButton("C\u2714", selected = showTableC, accent = slotCColor) { onShowTableCChange(!showTableC) }
                                LightDeckButton("D\u2714", selected = showTableD, accent = slotDColor) { onShowTableDChange(!showTableD) }
                            }
                            if (mode == LightPanelMode.TIME) {
                                LightDeckButton(if (timeRecording) "STOP" else "START", selected = timeRecording, accent = if (timeRecording) Color(0xFFE98D85) else Color(0xFF83D49A)) {
                                    if (timeRecording) onStopTimeRecording() else onStartTimeRecording()
                                }
                                LightDeckButton("CLEAR", accent = Color(0xFFE98D85)) { onClearAllTimeSeries() }
                            }
                            LightDeckButton("CSV", accent = Color(0xFF83D49A)) { onExportCsv() }
                            LightDeckButton("XLSX", accent = Color(0xFF83D49A)) { onExportXlsx() }
                            LightDeckButton("PDF", accent = Color(0xFF83D49A)) { onExportPdf() }
                        }
                        val isTime = mode == LightPanelMode.TIME
                        LightTableBlock(
                            "Samples",
                            buildList {
                                if (showTableA) add(LightSeries("A", if (isTime) timeA.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) } else profileA, slotAColor))
                                if (dualMode && showTableB) add(LightSeries("B", if (isTime) timeB.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) } else profileB, slotBColor))
                                if (dualMode && showTableC) add(LightSeries("C", if (isTime) timeC.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) } else profileC, slotCColor))
                                if (dualMode && showTableD) add(LightSeries("D", if (isTime) timeD.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) } else profileD, slotDColor))
                            }.ifEmpty {
                                listOf(LightSeries("A", if (isTime) timeA.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) } else profileA, slotAColor))
                            }
                        )
                    }
                    LightWorkspaceTab.COMPARE -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LightDeckButton("PROFILE", selected = compareSource == LightCompareSource.PROFILE, accent = slotAColor) {
                                onCompareSourceSelected(LightCompareSource.PROFILE)
                                onModeSelected(LightPanelMode.COMPARE)
                            }
                            LightDeckButton("TIME", selected = compareSource == LightCompareSource.TIME, accent = slotBColor) {
                                onCompareSourceSelected(LightCompareSource.TIME)
                                onModeSelected(LightPanelMode.COMPARE)
                            }
                        }
                        LightStatsBlock(
                            title = "Compare Stats",
                            stats = focusedStats,
                            unitLabel = if (compareSource == LightCompareSource.TIME) "s" else unitLabel
                        )
                        LightTableBlock("Compare Samples", chartSeries)
                    }
                    LightWorkspaceTab.MORE -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LightChannel.values().forEach { item ->
                                LightDeckButton(item.name, selected = channel == item, accent = Color.White.copy(alpha = 0.78f)) {
                                    onChannelSelected(item)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val slotCount = if (!dualMode) 1 else 4
                            LightDeckButton(when (slotCount) { 1 -> "1 SLOT"; 4 -> "4 SLOTS"; else -> "MULTI" }, selected = dualMode, accent = Color(0xFF00E5FF)) {
                                onDualModeChange(!dualMode)
                            }
                            LightDeckButton("BG SUB", selected = backgroundSubtract, accent = Color(0xFFFFC107)) {
                                onBackgroundSubtractChange(!backgroundSubtract)
                            }
                            LightDeckButton("SMOOTH", selected = smoothing, accent = Color(0xFF83D49A)) {
                                onSmoothingChange(!smoothing)
                            }
                            LightDeckButton("PEAKS", selected = detectPeaks, accent = Color(0xFFE98D85)) {
                                onDetectPeaksChange(!detectPeaks)
                            }
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LightDeckButton("CLEAR A", accent = slotAColor) { onClearTimeSeries(LightSlot.A) }
                            if (dualMode) {
                                LightDeckButton("CLEAR B", accent = slotBColor) { onClearTimeSeries(LightSlot.B) }
                                LightDeckButton("CLEAR C", accent = slotCColor) { onClearTimeSeries(LightSlot.C) }
                                LightDeckButton("CLEAR D", accent = slotDColor) { onClearTimeSeries(LightSlot.D) }
                            }
                            LightDeckButton("CSV", accent = Color(0xFF83D49A)) { onExportCsv() }
                            LightDeckButton("XLSX", accent = Color(0xFF83D49A)) { onExportXlsx() }
                            LightDeckButton("PDF", accent = Color(0xFF83D49A)) { onExportPdf() }
                            LightDeckButton("BACK", accent = Color.White.copy(alpha = 0.7f)) {
                                onWorkspaceTabSelected(LightWorkspaceTab.GRAPH)
                            }
                        }
                        LightGeometryBlock("A", slotAColor, geometryA)
                        if (dualMode) {
                            LightGeometryBlock("B", slotBColor, geometryB)
                            LightGeometryBlock("C", slotCColor, geometryC)
                            LightGeometryBlock("D", slotDColor, geometryD)
                        }
                    }
                }
                if (showOptions && false) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LightTool.values().forEach { item ->
                            LightDeckButton(item.name, selected = tool == item, accent = Color(0xFF00E5FF)) {
                                onToolSelected(item)
                            }
                        }
                        LightDeckButton("A", selected = activeSlot == LightSlot.A, accent = slotAColor) { onActiveSlotSelected(LightSlot.A) }
                        LightDeckButton("B", selected = activeSlot == LightSlot.B, accent = slotBColor) { onActiveSlotSelected(LightSlot.B) }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LightChannel.values().forEach { item ->
                            LightDeckButton(item.name, selected = channel == item, accent = Color.White.copy(alpha = 0.78f)) {
                                onChannelSelected(item)
                            }
                        }
                    }
                    if (mode == LightPanelMode.COMPARE) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LightDeckButton("PROFILE", selected = compareSource == LightCompareSource.PROFILE, accent = slotAColor) {
                                onCompareSourceSelected(LightCompareSource.PROFILE)
                            }
                            LightDeckButton("TIME", selected = compareSource == LightCompareSource.TIME, accent = slotBColor) {
                                onCompareSourceSelected(LightCompareSource.TIME)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LightDeckButton("BG SUB", selected = backgroundSubtract, accent = Color(0xFFFFC107)) {
                            onBackgroundSubtractChange(!backgroundSubtract)
                        }
                        LightDeckButton("SMOOTH", selected = smoothing, accent = Color(0xFF83D49A)) {
                            onSmoothingChange(!smoothing)
                        }
                        LightDeckButton("PEAKS", selected = detectPeaks, accent = Color(0xFFE98D85)) {
                            onDetectPeaksChange(!detectPeaks)
                        }
                        LightDeckButton("CLEAR A", accent = slotAColor) { onClearTimeSeries(LightSlot.A) }
                        LightDeckButton("CLEAR B", accent = slotBColor) { onClearTimeSeries(LightSlot.B) }
                    }
                }
            }
        }
    }
}

@Composable
fun LightTopTray(
    workspaceTab: LightWorkspaceTab,
    onWorkspaceTabSelected: (LightWorkspaceTab) -> Unit,
    lightTool: LightTool,
    onLightToolSelected: (LightTool) -> Unit,
    lightDualMode: Boolean,
    lightActiveSlot: LightSlot,
    onLightActiveSlotSelected: (LightSlot) -> Unit,
    lightPanelMode: LightPanelMode,
    timeRecording: Boolean,
    onTimeRecordingToggle: () -> Unit,
    onClearTimeSeries: () -> Unit,
    slotAColor: Color,
    slotBColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LightWorkspaceTab.values().forEach { tab ->
                LightDeckButton(
                    tab.name,
                    selected = workspaceTab == tab,
                    accent = when (tab) {
                        LightWorkspaceTab.SELECT -> slotAColor
                        LightWorkspaceTab.GRAPH -> Color(0xFF58D5F7)
                        LightWorkspaceTab.TABLE -> Color(0xFF83D49A)
                        LightWorkspaceTab.COMPARE -> slotBColor
                        LightWorkspaceTab.MORE -> Color(0xFFF2C66D)
                    }
                ) {
                    onWorkspaceTabSelected(tab)
                }
            }
        }
        if (workspaceTab == LightWorkspaceTab.SELECT) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LightTool.values().forEach { tool ->
                    LightDeckButton(tool.name, selected = lightTool == tool, accent = Color(0xFFF2C66D)) {
                        onLightToolSelected(tool)
                    }
                }
                LightDeckButton("A", selected = lightActiveSlot == LightSlot.A, accent = slotAColor) {
                    onLightActiveSlotSelected(LightSlot.A)
                }
                if (lightDualMode) {
                    LightDeckButton("B", selected = lightActiveSlot == LightSlot.B, accent = slotBColor) {
                        onLightActiveSlotSelected(LightSlot.B)
                    }
                }
                if (lightPanelMode == LightPanelMode.TIME) {
                    LightDeckButton(
                        if (timeRecording) "STOP" else "START",
                        selected = timeRecording,
                        accent = if (timeRecording) Color(0xFFE98D85) else Color(0xFF83D49A)
                    ) {
                        onTimeRecordingToggle()
                    }
                    LightDeckButton("CLEAR", accent = Color(0xFFE98D85)) {
                        onClearTimeSeries()
                    }
                }
            }
        }
    }
}

@Composable
private fun LightDeckButton(
    label: String,
    selected: Boolean = false,
    accent: Color = Color(0xFF58D5F7),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = if (selected) 0.9f else 0.3f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.05f),
            contentColor = if (selected) accent else Color.White.copy(alpha = 0.82f)
        )
    ) {
        Text(label, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LightChart(
    series: List<LightSeries>,
    axisLabel: String,
    intensityLabel: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val plotLeft = 52f
        val plotTop = 14f
        val plotRight = size.width - 14f
        val plotBottom = size.height - 26f
        val plotWidth = (plotRight - plotLeft).coerceAtLeast(1f)
        val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)
        val allSamples = series.flatMap { it.samples }
        val minX = allSamples.minOfOrNull { it.axisValue } ?: 0.0
        val maxX = allSamples.maxOfOrNull { it.axisValue } ?: 1.0
        val minY = allSamples.minOfOrNull { it.intensity } ?: 0.0
        val maxY = allSamples.maxOfOrNull { it.intensity } ?: 1.0
        val xRange = (maxX - minX).takeIf { it > 1e-6 } ?: 1.0
        val yRange = (maxY - minY).takeIf { it > 1e-6 } ?: 1.0
        drawLine(Color.White.copy(alpha = 0.35f), Offset(plotLeft, plotBottom), Offset(plotRight, plotBottom), 1.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.35f), Offset(plotLeft, plotTop), Offset(plotLeft, plotBottom), 1.dp.toPx())
        repeat(4) { index ->
            val y = plotTop + plotHeight * (index / 4f)
            drawLine(Color.White.copy(alpha = 0.08f), Offset(plotLeft, y), Offset(plotRight, y), 1.dp.toPx())
        }
        series.forEach { item ->
            if (item.samples.isEmpty()) return@forEach
            val path = Path()
            item.samples.forEachIndexed { index, sample ->
                val x = plotLeft + (((sample.axisValue - minX) / xRange) * plotWidth).toFloat()
                val y = plotBottom - (((sample.intensity - minY) / yRange) * plotHeight).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(item.color.copy(alpha = 0.9f), 2.4.dp.toPx(), Offset(x, y))
            }
            drawPath(path, item.color, style = Stroke(width = 1.5.dp.toPx()))
        }
        drawIntoCanvas { canvas ->
            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 22f
                color = android.graphics.Color.WHITE
            }
            canvas.nativeCanvas.drawText(axisLabel, plotLeft, size.height - 4f, labelPaint)
            canvas.nativeCanvas.drawText(intensityLabel, 4f, 24f, labelPaint)
            series.forEachIndexed { index, item ->
                labelPaint.color = item.color.toArgb()
                canvas.nativeCanvas.drawText(item.label, plotLeft + index * 150f, 18f, labelPaint)
            }
        }
    }
}

@Composable
private fun LightStatsBlock(title: String, stats: LightStats, unitLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Color(0xFF00E5FF), style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LightMetric("N", stats.sampleCount.toString(), Color.White)
            LightMetric("Min", stats.min?.let(::formatCalibratedValue) ?: "--", Color.White)
            LightMetric("Max", stats.max?.let(::formatCalibratedValue) ?: "--", Color.White)
            LightMetric("Mean", stats.mean?.let(::formatCalibratedValue) ?: "--", Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LightMetric("Peaks", stats.peakPositions.size.toString(), Color(0xFFFFC107))
            LightMetric("Spacing", stats.peakSpacing?.let { "${formatCalibratedValue(it)} $unitLabel" } ?: "--", Color(0xFFFFC107))
            LightMetric("Contrast", stats.contrast?.let(::formatCalibratedValue) ?: "--", Color(0xFF83D49A))
            LightMetric("FWHM", stats.fwhm?.let { "${formatCalibratedValue(it)} $unitLabel" } ?: "--", Color(0xFFE98D85))
        }
    }
}

@Composable
private fun LightMetric(label: String, value: String, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.66f), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
        Text(value, color = tint, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun LightGeometryBlock(label: String, color: Color, geometry: LightGeometry?) {
    val summary = geometry?.let { lightGeometrySummary(it, null) } ?: "No geometry"
    Text(
        text = "$label  $summary",
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun LightTableBlock(title: String, series: List<LightSeries>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Color(0xFF00E5FF), style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
        series.forEach { item ->
            Text(item.label, color = item.color, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            item.samples.take(18).forEach { sample ->
                Text(
                    text = "${formatCalibratedValue(sample.axisValue)}   ${formatCalibratedValue(sample.intensity)}",
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
