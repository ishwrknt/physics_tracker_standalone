package com.physicstracker.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.PlayerView
import android.view.LayoutInflater
import com.physicstracker.app.R
import com.bmad.new_start.shared_c_core.NativeTemplateTracker
import com.physicstracker.app.tracking.ClassicTrackerEngine
import com.physicstracker.app.tracking.ClassicTrackerMode
import com.physicstracker.app.tracking.ClassicTrackingSeed
import com.physicstracker.app.tracking.DeepSegmentationTracker
import com.physicstracker.app.tracking.DeepTrackingState
import com.physicstracker.app.tracking.DeepTrackingSource
import com.physicstracker.app.tracking.DeepTrackingSupport
import com.physicstracker.app.tracking.MediaPipeObjectTracker
import com.physicstracker.app.tracking.MediaPipeTrackingSource
import com.physicstracker.app.tracking.MediaPipeTrackingState
import com.physicstracker.app.tracking.MediaPipeTrackingSupport
import com.physicstracker.app.data.CalibrationEntity
import com.physicstracker.app.data.TrackPointEntity
import com.physicstracker.app.ui.analysis.AnalysisFitOption
import com.physicstracker.app.ui.analysis.AnalysisModuleId
import com.physicstracker.app.ui.analysis.AnalysisPlotPreset
import com.physicstracker.app.ui.analysis.AnalysisSeriesOption
import com.physicstracker.app.ui.analysis.AnalysisTableRow
import com.physicstracker.app.ui.analysis.AnalysisWorkspace
import com.physicstracker.app.ui.analysis.AnalysisWorkspaceState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.coroutines.resume

private val LabSurface = PhysicsColorSurface.copy(alpha = 0.94f)
private val LabSurfaceStrong = PhysicsColorSurfaceVariant.copy(alpha = 0.98f)
private val LabAccent = PhysicsColorPrimary
private val LabAccentSoft = PhysicsColorOnBackground
private val LabGold = PhysicsColorAccent
private val LabSuccess = PhysicsColorPrimary
private val LabDanger = PhysicsColorError

@Composable
private fun LabOverlayCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = LabSurface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun LabInfoPill(
    label: String,
    value: String,
    tint: Color = LabAccent
) {
    Surface(
        color = tint.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = PhysicsColorOnSurface,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                color = tint,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun formatTimestampMs(valueMs: Long): String {
    val minutes = valueMs / 60_000
    val seconds = (valueMs % 60_000) / 1_000
    val millis = valueMs % 1_000
    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}

private enum class CalibrationHandle { START, END, BODY, ORIGIN, ROTATE }
private enum class PhotogateHandle { A, B }
private enum class CalibrationTool { SCALE, AXIS, MEASURE }
private enum class ActiveTrack { A, B }
private enum class MajorMode { TRACK, CAL, EDIT, DATA, LIGHT }
private enum class TopPanelTab { OPTIONS, TRACKERS, SETTINGS }
private enum class TrackerPanelSection { MODES, MODELS }
private enum class DataPanelMode { TABLE, GRAPH, ANALYSIS }
private enum class GraphDrawerPanel { SERIES, PROFILE, ENHANCE, VIEW, STATS }
private enum class AxisInspector { X, Y }
private enum class AnalysisMainTab(val label: String) {
    DATA("Data"),
    AXES("Axes"),
    GRAPH("Graph"),
    MOTION("Motion"),
    FITTING("Fitting"),
    STATS("Statistics"),
    SIGNAL("Signal Processing"),
    MEASURE("Measurements"),
    EXPORT("Export")
}
private enum class AnalysisSubTab(val label: String) {
    X_AXIS("X Axis"),
    Y_AXIS("Y Axis"),
    GRID("Grid"),
    SCALE("Scale"),
    LABELS("Labels"),
    TICKS("Tick Marks"),
    DATASET("Dataset"),
    REGRESSION("Regression"),
    RESIDUALS("Residuals"),
    RANGE("Range"),
    CLEANUP("Cleanup"),
    TRANSFORM("Transform"),
    DISTRIBUTION("Distribution"),
    MOTION("Motion"),
    EVENTS("Events"),
    FFT("FFT"),
    FILTERS("Filters"),
    BASELINE("Baseline"),
    CURSORS("Cursors"),
    ROI("ROI"),
    CALIBRATION("Calibration"),
    GRAPH("Graph"),
    SERIES("Series"),
    FILES("Files"),
    REPORT("Report")
}
private enum class AutoTrackMode { CLASSIC_FAST, CLASSIC_ACCURATE, MEDIAPIPE_OBJECT, DEEP_ACCURATE }
private enum class BottomMainTab { TRACK, CAL, PLAY, DATA, LIGHT, MORE }
private enum class TrackTrayPanel { TARGET, AUTO_RUN, STEP, DATA }
private enum class MoreTrayPanel { EDIT, MAG, VIEW, COLORS, EXPORT }
private enum class TrackingStepOption(val label: String, val frameStep: Int, val previewDelayMs: Long) {
    FRAME_1("1f", 1, 22L),
    FRAME_2("2f", 2, 18L),
    FRAME_5("5f", 5, 12L),
    FRAME_10("10f", 10, 8L),
    FRAME_20("20f", 20, 4L)
}
enum class GraphSeries { TIME, X, Y, VX, VY, SPEED, AX, AY, ACCEL, SEPARATION, RELATIVE_SPEED }

private enum class OverlayColorRole { TRACK_A, TRACK_B, MANUAL_SELECTOR, VECTOR, CALIBRATION }

data class GraphViewportSummary(
    val xMin: Float? = null,
    val xMax: Float? = null,
    val yMin: Float? = null,
    val yMax: Float? = null
)

private data class MeasurementStickDraft(
    val start: Offset,
    val end: Offset
)

private data class AutoTrackSeed(
    val template: Bitmap,
    val originalTemplate: Bitmap? = null,
    val center: Offset,
    val velocity: Offset = Offset.Zero,
    val mediaPipeTrackingState: MediaPipeTrackingState? = null,
    val deepTrackingState: DeepTrackingState? = null,
    val meanError: Double? = null,
    val searchRadiusPx: Int = autoTrackSearchRadiusPx,
    val confidence: Double? = null,
    val estimatedRadiusPx: Float = autoTrackTemplateRadiusPx.toFloat(),
    val trackingSource: String = "seed",
    val objectLuma: Float = 0f,
    val backgroundLuma: Float = 0f,
    val objectIsBrighter: Boolean = true,
    val sourceWidth: Int,
    val sourceHeight: Int,
    // Kalman Filter state
    val kfX: Float = center.x,
    val kfY: Float = center.y,
    val kfVx: Float = velocity.x,
    val kfVy: Float = velocity.y,
    val kfPx00: Float = 10f,
    val kfPx01: Float = 0f,
    val kfPx11: Float = 5f,
    val kfPy00: Float = 10f,
    val kfPy01: Float = 0f,
    val kfPy11: Float = 5f,
    // PSR, scale, keyframe bank
    val psr: Double = 0.0,
    val currentScale: Float = 1.0f,
    val keyframeTemplates: List<Bitmap> = emptyList()
)

private fun AutoTrackSeed.toClassicSeed(): ClassicTrackingSeed = ClassicTrackingSeed(
    template = template,
    center = center,
    velocity = velocity,
    meanError = meanError,
    searchRadiusPx = searchRadiusPx,
    confidence = confidence,
    estimatedRadiusPx = estimatedRadiusPx,
    objectLuma = objectLuma,
    backgroundLuma = backgroundLuma,
    objectIsBrighter = objectIsBrighter,
    sourceWidth = sourceWidth,
    sourceHeight = sourceHeight
)

private data class AutoTrackPreviewPoint(
    val timestampUs: Long,
    val position: Offset,
    val frameNumber: Int,
    val trackLabel: String
)

private data class SeedRefinement(
    val center: Offset,
    val radiusPx: Float,
    val confidence: Double
)

private data class AutoTrackBitmapStepResult(
    val seed: AutoTrackSeed,
    val screenPoint: Offset,
    val statusText: String
)

private enum class SeedLockMode { EXACT_PIXEL, SNAP_OBJECT }

private data class CalibrationUnitOption(
    val symbol: String,
    val metersFactor: Double
)

private val calibrationUnits = listOf(
    CalibrationUnitOption("mm", 0.001),
    CalibrationUnitOption("cm", 0.01),
    CalibrationUnitOption("m", 1.0)
)
private val kinematicsColumnWidth = 108.dp
private const val autoTrackTemplateRadiusPx = 12
private const val autoTrackTemplateSizePx = 32
private const val manualSelectionFrameWindow = 8
private const val manualSelectionRadiusPx = 64.0
private const val manualHandleLiftDp = 56f

private enum class PointHistoryMode { FADE, ALL, CURRENT }

private fun selectEditablePointNearFrame(
    points: List<TrackPointEntity>,
    tap: Offset,
    alternateTap: Offset? = null,
    currentTimeMs: Long,
    frameDurationMs: Long
): TrackPointEntity? {
    if (points.isEmpty()) return null
    val timeWindowMs = (frameDurationMs.coerceAtLeast(1L) * manualSelectionFrameWindow).coerceAtLeast(120L)
    val relevantPoints = points.filter {
        kotlin.math.abs((it.timestampUs / 1000L) - currentTimeMs) <= timeWindowMs
    }.ifEmpty {
        points
    }
    return relevantPoints
        .map { point ->
            val primaryDx = point.pixelX - tap.x
            val primaryDy = point.pixelY - tap.y
            val primaryDistance = sqrt(primaryDx * primaryDx + primaryDy * primaryDy)
            val distance = alternateTap?.let { alternate ->
                val alternateDx = point.pixelX - alternate.x
                val alternateDy = point.pixelY - alternate.y
                minOf(primaryDistance, sqrt(alternateDx * alternateDx + alternateDy * alternateDy))
            } ?: primaryDistance
            val frameBias = kotlin.math.abs((point.timestampUs / 1000L) - currentTimeMs).toDouble() / timeWindowMs.toDouble()
            point to (distance + (frameBias * 12.0))
        }
        .filter { (_, score) -> score < manualSelectionRadiusPx }
        .minByOrNull { (_, score) -> score }
        ?.first
}

private fun selectVisibleEditablePoint(
    points: List<TrackPointEntity>,
    tap: Offset,
    alternateTap: Offset? = null,
    radiusPx: Double = manualSelectionRadiusPx
): TrackPointEntity? {
    if (points.isEmpty()) return null
    return points
        .map { point ->
            val primaryDx = point.pixelX - tap.x
            val primaryDy = point.pixelY - tap.y
            val primaryDistance = sqrt(primaryDx * primaryDx + primaryDy * primaryDy)
            val distance = alternateTap?.let { alternate ->
                val alternateDx = point.pixelX - alternate.x
                val alternateDy = point.pixelY - alternate.y
                minOf(primaryDistance, sqrt(alternateDx * alternateDx + alternateDy * alternateDy))
            } ?: primaryDistance
            point to distance
        }
        .filter { (_, distance) -> distance < radiusPx }
        .minByOrNull { (_, distance) -> distance }
        ?.first
}

private fun pointHistoryAlpha(
    pointTimeMs: Long,
    currentTimeMs: Long,
    isSelected: Boolean,
    isActive: Boolean,
    historyMode: PointHistoryMode
): Float {
    if (isSelected) return 1f
    if (isActive) return 0.96f
    if (historyMode == PointHistoryMode.ALL) return 0.78f
    val ageMs = (currentTimeMs - pointTimeMs).coerceAtLeast(0L)
    if (historyMode == PointHistoryMode.CURRENT) {
        return if (ageMs <= 80L) 0.9f else 0f
    }
    return when {
        ageMs <= 80L -> 0.88f
        ageMs <= 220L -> 0.62f
        ageMs <= 520L -> 0.34f
        ageMs <= 1100L -> 0.15f
        else -> 0f
    }
}

@Composable
private fun TrackerFloatingCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = PhysicsColorSurface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                color = PhysicsColorOnBackground,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace
            )
            content()
        }
    }
}

@Composable
private fun PhyzxLogoMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val ringColor = PhysicsColorPrimary
        val accentColor = PhysicsColorAccent
        drawRoundRect(
            color = PhysicsColorBackground,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.22f, h * 0.22f)
        )
        drawRoundRect(
            color = PhysicsColorDivider,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.2f, h * 0.2f),
            style = Stroke(width = size.minDimension * 0.018f)
        )
        val center = Offset(w * 0.5f, h * 0.42f)
        rotate(-38f, center) {
            drawOval(
                color = ringColor,
                topLeft = Offset(center.x - w * 0.28f, center.y - h * 0.1f),
                size = Size(w * 0.56f, h * 0.2f),
                style = Stroke(width = size.minDimension * 0.018f)
            )
        }
        rotate(38f, center) {
            drawOval(
                color = ringColor,
                topLeft = Offset(center.x - w * 0.28f, center.y - h * 0.1f),
                size = Size(w * 0.56f, h * 0.2f),
                style = Stroke(width = size.minDimension * 0.018f)
            )
        }
        drawCircle(color = accentColor, radius = size.minDimension * 0.05f, center = Offset(w * 0.72f, h * 0.2f))
        drawCircle(color = ringColor, radius = size.minDimension * 0.036f, center = Offset(w * 0.28f, h * 0.2f))
        drawCircle(color = ringColor, radius = size.minDimension * 0.036f, center = Offset(w * 0.28f, h * 0.52f))
        drawCircle(color = ringColor, radius = size.minDimension * 0.036f, center = Offset(w * 0.72f, h * 0.52f))
        drawCircle(color = PhysicsColorBackground, radius = size.minDimension * 0.095f, center = center)
        drawCircle(color = accentColor, radius = size.minDimension * 0.068f, center = center)
        drawCircle(color = PhysicsColorOnBackground, radius = size.minDimension * 0.036f, center = center)
    }
}

@Composable
private fun BottomNavIconButton(
    symbol: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    accent: Color = LabSurfaceStrong,
    prominent: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (selected) accent else PhysicsColorOnSurface.copy(alpha = 0.6f),
            disabledContainerColor = Color.Transparent,
            disabledContentColor = PhysicsColorOnSurface.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size(if (prominent) 56.dp else 48.dp),
        shape = RoundedCornerShape(14.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f)) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = symbol,
                fontFamily = FontFamily.Monospace,
                color = if (selected) accent else PhysicsColorOnSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(3.dp)
                    .background(
                        color = if (selected) accent else Color.Transparent,
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }
    }
}

@Composable
private fun BottomDeckButton(
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    accent: Color = LabSurfaceStrong,
    prominent: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) accent.copy(alpha = 0.22f) else PhysicsColorSurfaceVariant,
            contentColor = if (selected) accent else PhysicsColorOnBackground,
            disabledContainerColor = PhysicsColorSurfaceVariant.copy(alpha = 0.7f),
            disabledContentColor = PhysicsColorOnSurface.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.55f) else PhysicsColorDivider
        ),
        contentPadding = if (prominent) {
            PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        } else {
            PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        },
        modifier = Modifier.heightIn(min = if (prominent) 48.dp else 44.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            style = if (prominent) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun CompactGraphControl(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = PhysicsColorSurfaceVariant.copy(alpha = 0.82f),
            contentColor = accent
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(width = 42.dp, height = 30.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
    }
}
private const val autoTrackSearchRadiusPx = 24
private const val autoTrackMaxSearchRadiusPx = 72
private const val autoTrackMaxMeanError = 30.0
private const val autoSeedHandleLiftDp = 56f

private data class AutoTrackProfile(
    val label: String,
    val maxSearchRadiusPx: Int,
    val maxMeanError: Double,
    val templateRefreshThreshold: Double
)

private fun autoTrackProfile(mode: AutoTrackMode): AutoTrackProfile = when (mode) {
    AutoTrackMode.CLASSIC_FAST -> AutoTrackProfile(
        label = "Template SSD",
        maxSearchRadiusPx = 72,
        maxMeanError = 900.0,
        templateRefreshThreshold = 10.0
    )
    AutoTrackMode.CLASSIC_ACCURATE -> AutoTrackProfile(
        label = "Classic Accurate",
        maxSearchRadiusPx = 88,
        maxMeanError = 28.0,
        templateRefreshThreshold = 14.0
    )
    AutoTrackMode.MEDIAPIPE_OBJECT -> AutoTrackProfile(
        label = "MediaPipe Detect",
        maxSearchRadiusPx = 96,
        maxMeanError = 0.75,
        templateRefreshThreshold = 14.0
    )
    AutoTrackMode.DEEP_ACCURATE -> AutoTrackProfile(
        label = "Deep Accurate",
        maxSearchRadiusPx = 88,
        maxMeanError = 24.0,
        templateRefreshThreshold = 12.0
    )
}

private val overlayColorPresets = listOf(
    PhysicsColorTrackA,
    PhysicsColorAccent,
    PhysicsColorTrackB,
    PhysicsColorPrimaryDim,
    PhysicsColorOnBackground,
    PhysicsColorOnSurface,
    PhysicsColorError
)

private fun overlayColorFromArgb(argb: Int): Color = Color(argb)

private fun overlayColorLabel(color: Color): String = when (color.toArgb()) {
    PhysicsColorTrackA.toArgb() -> "Track A"
    PhysicsColorAccent.toArgb() -> "Accent"
    PhysicsColorTrackB.toArgb() -> "Track B"
    PhysicsColorPrimaryDim.toArgb() -> "Primary Dim"
    PhysicsColorOnBackground.toArgb() -> "Text"
    PhysicsColorOnSurface.toArgb() -> "Muted"
    else -> "Error"
}

private fun roleColor(role: OverlayColorRole, trackAArgb: Int, trackBArgb: Int, selectorArgb: Int, vectorArgb: Int, calibrationArgb: Int): Color =
    when (role) {
        OverlayColorRole.TRACK_A -> overlayColorFromArgb(trackAArgb)
        OverlayColorRole.TRACK_B -> overlayColorFromArgb(trackBArgb)
        OverlayColorRole.MANUAL_SELECTOR -> overlayColorFromArgb(selectorArgb)
        OverlayColorRole.VECTOR -> overlayColorFromArgb(vectorArgb)
        OverlayColorRole.CALIBRATION -> overlayColorFromArgb(calibrationArgb)
    }

private fun trackColor(track: String, trackAArgb: Int, trackBArgb: Int): Color = when (track) {
    "B" -> overlayColorFromArgb(trackBArgb)
    else -> overlayColorFromArgb(trackAArgb)
}

private fun trackColor(track: String): Color = trackColor(
    track = track,
    trackAArgb = PhysicsColorTrackA.toArgb(),
    trackBArgb = PhysicsColorTrackB.toArgb()
)

private fun autoTrackConfidenceLabel(meanError: Double?): String = when {
    meanError == null -> "Seeded"
    meanError < 12.0 -> "High"
    meanError < 22.0 -> "Good"
    meanError < 32.0 -> "Low"
    else -> "Weak"
}

private fun autoTrackConfidenceColor(meanError: Double?): Color = when {
    meanError == null -> Color(0xFF4DD0E1)
    meanError < 12.0 -> Color(0xFF81C784)
    meanError < 22.0 -> Color(0xFFFFF176)
    meanError < 32.0 -> Color(0xFFFFB74D)
    else -> Color(0xFFE57373)
}

private fun autoTrackConfidenceBucket(confidence: Double?): String = when {
    confidence == null -> "Seeded"
    confidence >= 0.85 -> "High"
    confidence >= 0.65 -> "Good"
    confidence >= 0.45 -> "Low"
    else -> "Weak"
}

private fun autoTrackConfidenceTint(confidence: Double?): Color = when {
    confidence == null -> Color(0xFF4DD0E1)
    confidence >= 0.85 -> Color(0xFF81C784)
    confidence >= 0.65 -> Color(0xFFFFF176)
    confidence >= 0.45 -> Color(0xFFFFB74D)
    else -> Color(0xFFE57373)
}

private fun blendOffset(previous: Offset, next: Offset, factor: Float): Offset =
    Offset(
        x = previous.x + (next.x - previous.x) * factor,
        y = previous.y + (next.y - previous.y) * factor
    )

private fun clampOffsetToBitmap(offset: Offset, bitmap: Bitmap, template: Bitmap): Offset {
    val halfWidth = (template.width / 2).toFloat()
    val halfHeight = (template.height / 2).toFloat()
    return Offset(
        x = offset.x.coerceIn(halfWidth, bitmap.width - halfWidth - 1f),
        y = offset.y.coerceIn(halfHeight, bitmap.height - halfHeight - 1f)
    )
}

private fun offsetDelta(from: Offset, to: Offset): Offset = Offset(to.x - from.x, to.y - from.y)

private fun offsetMagnitude(offset: Offset): Float = sqrt((offset.x * offset.x) + (offset.y * offset.y))

private fun refineSeedPoint(bitmap: Bitmap, initialCenter: Offset, radiusPx: Int = 22): SeedRefinement {
    val centerX = initialCenter.x.toInt().coerceIn(0, bitmap.width - 1)
    val centerY = initialCenter.y.toInt().coerceIn(0, bitmap.height - 1)
    val left = (centerX - radiusPx).coerceAtLeast(0)
    val top = (centerY - radiusPx).coerceAtLeast(0)
    val right = (centerX + radiusPx).coerceAtMost(bitmap.width - 1)
    val bottom = (centerY + radiusPx).coerceAtMost(bitmap.height - 1)
    val centerLuma = luma(bitmap.getPixel(centerX, centerY)).toFloat()

    var localMin = centerLuma
    var localMax = centerLuma
    for (y in top..bottom) {
        for (x in left..right) {
            val sample = luma(bitmap.getPixel(x, y)).toFloat()
            if (sample < localMin) localMin = sample
            if (sample > localMax) localMax = sample
        }
    }

    val objectIsBrighter = abs(localMax - centerLuma) >= abs(centerLuma - localMin)
    val threshold = if (objectIsBrighter) {
        centerLuma + (localMax - centerLuma) * 0.22f
    } else {
        centerLuma - (centerLuma - localMin) * 0.22f
    }

    var sumX = 0f
    var sumY = 0f
    var totalWeight = 0f
    var supportPixels = 0

    for (y in top..bottom) {
        for (x in left..right) {
            val sample = luma(bitmap.getPixel(x, y)).toFloat()
            val signedContrast = if (objectIsBrighter) sample - threshold else threshold - sample
            if (signedContrast > 0f) {
                val dx = (x - centerX).toFloat()
                val dy = (y - centerY).toFloat()
                val distancePenalty = 1f - (sqrt(dx * dx + dy * dy) / radiusPx.toFloat()).coerceIn(0f, 1f) * 0.55f
                val weight = signedContrast * signedContrast * distancePenalty
                sumX += x * weight
                sumY += y * weight
                totalWeight += weight
                supportPixels += 1
            }
        }
    }

    if (supportPixels < 10 || totalWeight <= 0f) {
        return SeedRefinement(
            center = initialCenter,
            radiusPx = 10f,
            confidence = 0.25
        )
    }

    val refinedCenter = Offset(sumX / totalWeight, sumY / totalWeight)
    val estimatedRadius = sqrt(supportPixels / PI.toFloat()).coerceIn(6f, radiusPx.toFloat())
    val contrast = (localMax - localMin).coerceAtLeast(1f)
    val confidence = ((supportPixels / 140f).coerceIn(0f, 1f) * 0.6f + (contrast / 80f).coerceIn(0f, 1f) * 0.4f)

    return SeedRefinement(
        center = refinedCenter,
        radiusPx = estimatedRadius,
        confidence = confidence.toDouble().coerceIn(0.0, 1.0)
    )
}

private fun classicTrackerMode(mode: AutoTrackMode): ClassicTrackerMode? = when (mode) {
    AutoTrackMode.CLASSIC_FAST -> ClassicTrackerMode.FAST
    AutoTrackMode.CLASSIC_ACCURATE -> ClassicTrackerMode.ACCURATE
    else -> null
}

private fun videoDisplayRect(videoWidth: Int, videoHeight: Int, canvasSize: IntSize): Rect {
    val canvasWidth = canvasSize.width.toFloat()
    val canvasHeight = canvasSize.height.toFloat()
    val scale = minOf(canvasWidth / videoWidth.toFloat(), canvasHeight / videoHeight.toFloat())
    val contentWidth = videoWidth * scale
    val contentHeight = videoHeight * scale
    val left = ((canvasWidth - contentWidth) / 2f).toInt()
    val top = ((canvasHeight - contentHeight) / 2f).toInt()
    return Rect(left, top, (left + contentWidth).toInt(), (top + contentHeight).toInt())
}

private fun screenToVideoOffset(offset: Offset, videoWidth: Int, videoHeight: Int, canvasSize: IntSize): Offset? {
    val rect = videoDisplayRect(videoWidth, videoHeight, canvasSize)
    if (offset.x < rect.left || offset.x > rect.right || offset.y < rect.top || offset.y > rect.bottom) return null
    val x = ((offset.x - rect.left) / rect.width().coerceAtLeast(1)) * videoWidth
    val y = ((offset.y - rect.top) / rect.height().coerceAtLeast(1)) * videoHeight
    return Offset(x, y)
}

private fun screenToVideoOffsetClamped(offset: Offset, videoWidth: Int, videoHeight: Int, canvasSize: IntSize): Offset {
    val rect = videoDisplayRect(videoWidth, videoHeight, canvasSize)
    val cx = offset.x.coerceIn(rect.left.toFloat(), rect.right.toFloat())
    val cy = offset.y.coerceIn(rect.top.toFloat(), rect.bottom.toFloat())
    val x = ((cx - rect.left) / rect.width().coerceAtLeast(1)) * videoWidth
    val y = ((cy - rect.top) / rect.height().coerceAtLeast(1)) * videoHeight
    return Offset(x, y)
}

private fun videoToScreenOffset(offset: Offset, videoWidth: Int, videoHeight: Int, canvasSize: IntSize): Offset {
    val rect = videoDisplayRect(videoWidth, videoHeight, canvasSize)
    val x = rect.left + (offset.x / videoWidth.coerceAtLeast(1)) * rect.width()
    val y = rect.top + (offset.y / videoHeight.coerceAtLeast(1)) * rect.height()
    return Offset(x, y)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private suspend fun capturePlayerBitmap(activity: Activity, playerView: PlayerView): Bitmap? {
    val textureView = playerView.videoSurfaceView as? android.view.TextureView ?: return null
    return textureView.bitmap
}

private fun buildMagnifierBitmap(source: Bitmap, focus: Offset, radiusPx: Int = 18, outputSizePx: Int = 220): Bitmap? {
    if (source.width <= 1 || source.height <= 1) return null
    val diameter = radiusPx * 2
    val left = (focus.x.toInt() - radiusPx).coerceIn(0, (source.width - diameter).coerceAtLeast(0))
    val top = (focus.y.toInt() - radiusPx).coerceIn(0, (source.height - diameter).coerceAtLeast(0))
    val cropWidth = diameter.coerceAtMost(source.width)
    val cropHeight = diameter.coerceAtMost(source.height)
    if (cropWidth <= 1 || cropHeight <= 1) return null
    val cropped = Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
    return Bitmap.createScaledBitmap(cropped, outputSizePx, outputSizePx, false)
}

private fun selectionPointFromTouch(
    touch: Offset,
    canvasSize: IntSize,
    verticalOffsetPx: Float
): Offset {
    val maxX = canvasSize.width.toFloat().coerceAtLeast(1f)
    val maxY = canvasSize.height.toFloat().coerceAtLeast(1f)
    return Offset(
        touch.x.coerceIn(0f, maxX),
        touch.y.coerceIn(0f, maxY)
    )
}

private fun inverseVideoTransformOffset(
    touch: Offset,
    canvasSize: IntSize,
    scale: Float,
    translationX: Float,
    translationY: Float
): Offset {
    val width = canvasSize.width.toFloat().coerceAtLeast(1f)
    val height = canvasSize.height.toFloat().coerceAtLeast(1f)
    val centerX = width / 2f
    val centerY = height / 2f
    val safeScale = scale.coerceAtLeast(1f)
    return Offset(
        x = (((touch.x - centerX - translationX) / safeScale) + centerX).coerceIn(0f, width),
        y = (((touch.y - centerY - translationY) / safeScale) + centerY).coerceIn(0f, height)
    )
}

private fun manualHandleCenter(
    point: Offset,
    canvasSize: IntSize,
    handleLiftPx: Float
): Offset = autoSeedHandleCenter(point, canvasSize, handleLiftPx)

private fun autoSeedHandleCenter(
    point: Offset,
    canvasSize: IntSize,
    handleLiftPx: Float
): Offset {
    val maxX = canvasSize.width.toFloat().coerceAtLeast(1f)
    val maxY = canvasSize.height.toFloat().coerceAtLeast(1f)
    return Offset(
        point.x.coerceIn(0f, maxX),
        (point.y + handleLiftPx).coerceIn(0f, maxY)
    )
}

private fun cropTemplate(bitmap: Bitmap, center: Offset, radiusPx: Int): Bitmap? {
    val size = radiusPx * 2 + 1
    val left = center.x.toInt() - radiusPx
    val top = center.y.toInt() - radiusPx
    if (left < 0 || top < 0 || left + size > bitmap.width || top + size > bitmap.height) return null
    return Bitmap.createBitmap(bitmap, left, top, size, size)
}

private fun cropTemplateSized(bitmap: Bitmap, center: Offset, sizePx: Int): Bitmap? {
    val left = center.x.toInt() - (sizePx / 2)
    val top = center.y.toInt() - (sizePx / 2)
    if (left < 0 || top < 0 || left + sizePx > bitmap.width || top + sizePx > bitmap.height) return null
    return Bitmap.createBitmap(bitmap, left, top, sizePx, sizePx)
}

private fun luma(color: Int): Int {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    return (r * 30 + g * 59 + b * 11) / 100
}

private fun findTemplateMatch(
    bitmap: Bitmap,
    template: Bitmap,
    predictedCenter: Offset,
    searchRadiusPx: Int
): Pair<Offset, Double>? {
    val templateWidth = template.width
    val templateHeight = template.height
    if (templateWidth <= 0 || templateHeight <= 0) return null
    val halfWidth = templateWidth / 2
    val halfHeight = templateHeight / 2
    val startX = max(halfWidth, predictedCenter.x.toInt() - searchRadiusPx)
    val endX = minOf(bitmap.width - halfWidth - 1, predictedCenter.x.toInt() + searchRadiusPx)
    val startY = max(halfHeight, predictedCenter.y.toInt() - searchRadiusPx)
    val endY = minOf(bitmap.height - halfHeight - 1, predictedCenter.y.toInt() + searchRadiusPx)
    if (startX > endX || startY > endY) return null

    val templatePixels = IntArray(templateWidth * templateHeight)
    template.getPixels(templatePixels, 0, templateWidth, 0, 0, templateWidth, templateHeight)
    var bestScore = Double.MAX_VALUE
    var bestCenter: Offset? = null
    for (centerY in startY..endY step 2) {
        for (centerX in startX..endX step 2) {
            var totalDiff = 0.0
            var samples = 0
            for (y in 0 until templateHeight step 2) {
                for (x in 0 until templateWidth step 2) {
                    val sourceColor = bitmap.getPixel(centerX - halfWidth + x, centerY - halfHeight + y)
                    val templateColor = templatePixels[y * templateWidth + x]
                    totalDiff += abs(luma(sourceColor) - luma(templateColor)).toDouble()
                    samples += 1
                }
            }
            val meanDiff = totalDiff / samples.coerceAtLeast(1)
            if (meanDiff < bestScore) {
                bestScore = meanDiff
                bestCenter = Offset(centerX.toFloat(), centerY.toFloat())
            }
        }
    }
    return bestCenter?.let { it to bestScore }
}

private fun findNativeTemplateMatch(
    bitmap: Bitmap,
    template: Bitmap,
    predictedCenter: Offset,
    searchRadiusPx: Int
): Pair<Offset, Double>? {
    val framePixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(framePixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val templatePixels = IntArray(template.width * template.height)
    template.getPixels(templatePixels, 0, template.width, 0, 0, template.width, template.height)
    val result = NativeTemplateTracker.localizedSsdMatchNative(
        framePixels = framePixels,
        frameWidth = bitmap.width,
        frameHeight = bitmap.height,
        templatePixels = templatePixels,
        templateWidth = template.width,
        templateHeight = template.height,
        predictedCenterX = predictedCenter.x.toInt(),
        predictedCenterY = predictedCenter.y.toInt(),
        searchRadiusPx = searchRadiusPx
    )
    if (result.size < 3) return null
    return Offset(result[0], result[1]) to result[2].toDouble()
}

fun displayCalibrationDistance(calibration: CalibrationEntity): Double {
    val factor = calibrationUnits.firstOrNull { it.symbol == calibration.calibrationUnit }?.metersFactor ?: 1.0
    return calibration.knownDistanceMeters / factor
}

private fun distanceBetween(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun distanceToSegment(point: Offset, start: Offset, end: Offset): Float {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared <= 0f) return distanceBetween(point, start)
    val t = (((point.x - start.x) * dx) + ((point.y - start.y) * dy)) / lengthSquared
    val clamped = t.coerceIn(0f, 1f)
    val projected = Offset(start.x + dx * clamped, start.y + dy * clamped)
    return distanceBetween(point, projected)
}

private fun isInsideCalibrationLabel(point: Offset, start: Offset, end: Offset, densityPx: Float): Boolean {
    val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
    val halfWidth = 54f * densityPx
    val halfHeight = 16f * densityPx
    return point.x in (mid.x - halfWidth)..(mid.x + halfWidth) &&
        point.y in (mid.y - halfHeight)..(mid.y + halfHeight)
}

private fun isInsidePhotogateHandle(point: Offset, gateX: Float, densityPx: Float): Boolean {
    val handleCenterY = 28f * densityPx
    val radius = 26f * densityPx
    return distanceBetween(point, Offset(gateX, handleCenterY)) <= radius
}

private fun axisEndpoint(origin: Offset, rotationDegrees: Double, length: Float): Offset {
    val radians = (rotationDegrees / 180.0) * PI
    return Offset(
        x = origin.x + (cos(radians) * length).toFloat(),
        y = origin.y + (sin(radians) * length).toFloat()
    )
}

private fun perpendicularAxisEndpoint(origin: Offset, rotationDegrees: Double, length: Float): Offset {
    val radians = ((rotationDegrees - 90.0) / 180.0) * PI
    return Offset(
        x = origin.x + (cos(radians) * length).toFloat(),
        y = origin.y + (sin(radians) * length).toFloat()
    )
}

private fun angleDegrees(origin: Offset, point: Offset): Double {
    return Math.toDegrees(atan2((point.y - origin.y).toDouble(), (point.x - origin.x).toDouble()))
}

private fun normalizedDegrees(value: Double): Double {
    var angle = value % 360.0
    if (angle < 0) angle += 360.0
    return angle
}

fun formatCalibratedValue(value: Double): String = String.format("%.3f", value)
fun formatSeconds(timestampUs: Long): String = String.format("%.3f", timestampUs / 1_000_000.0)
fun formatOptionalCalibratedValue(value: Double?): String = value?.let(::formatCalibratedValue) ?: "-"

fun buildCsvContent(
    projectName: String,
    displayedUnit: String,
    points: List<DisplayKinematicsPoint>,
    photogateSummary: ExportPhotogateSummary? = null,
    twoBodySummary: ExportTwoBodySummary? = null
): String {
    val header = listOf(
        "project",
        "track",
        "frame",
        "time_s",
        "gap",
        "x_$displayedUnit",
        "y_$displayedUnit",
        "vx_${displayedUnit}_per_s",
        "vy_${displayedUnit}_per_s",
        "v_${displayedUnit}_per_s",
        "ax_${displayedUnit}_per_s2",
        "ay_${displayedUnit}_per_s2",
        "a_${displayedUnit}_per_s2",
        "gate_a_time_s",
        "gate_b_time_s",
        "gate_delta_t_s",
        "gate_a_frame",
        "gate_b_frame",
        "two_body_current_separation",
        "two_body_min_separation",
        "two_body_max_separation",
        "two_body_avg_separation",
        "two_body_current_relative_speed",
        "two_body_min_relative_speed",
        "two_body_max_relative_speed",
        "two_body_avg_relative_speed",
        "two_body_frame_count",
        "two_body_unit",
        "track_a_mass_kg",
        "track_b_mass_kg",
        "track_a_momentum",
        "track_b_momentum",
        "total_momentum",
        "relative_momentum",
        "closest_approach_time_s",
        "closest_approach_frame",
        "closest_approach_separation",
        "closest_approach_relative_speed",
        "closest_approach_track_a_momentum",
        "closest_approach_track_b_momentum",
        "closest_approach_total_momentum",
        "closest_approach_relative_momentum",
        "interaction_class",
        "pre_interaction_time_s",
        "pre_interaction_frame",
        "pre_interaction_separation",
        "pre_interaction_relative_speed",
        "pre_interaction_track_a_momentum",
        "pre_interaction_track_b_momentum",
        "pre_interaction_total_momentum",
        "pre_interaction_relative_momentum",
        "pre_interaction_delta_s",
        "post_interaction_time_s",
        "post_interaction_frame",
        "post_interaction_separation",
        "post_interaction_relative_speed",
        "post_interaction_track_a_momentum",
        "post_interaction_track_b_momentum",
        "post_interaction_total_momentum",
        "post_interaction_relative_momentum",
        "post_interaction_delta_s",
        "gate_a_to_closest_s",
        "closest_to_gate_b_s",
        "gate_a_to_gate_b_s",
        "momentum_unit"
    ).joinToString(",")
    val rows = points.joinToString("\n") { point ->
        listOf(
            projectName.replace(",", "_"),
            point.trackLabel,
            point.frameNumber.toString(),
            formatSeconds(point.timestampUs),
            point.gapLabel ?: "",
            formatCalibratedValue(point.xValue),
            formatCalibratedValue(point.yValue),
            point.vxValue?.let(::formatCalibratedValue) ?: "",
            point.vyValue?.let(::formatCalibratedValue) ?: "",
            point.speedValue?.let(::formatCalibratedValue) ?: "",
            point.axValue?.let(::formatCalibratedValue) ?: "",
            point.ayValue?.let(::formatCalibratedValue) ?: "",
            point.accelerationValue?.let(::formatCalibratedValue) ?: "",
            photogateSummary?.gateATimestampUs?.let(::formatSeconds) ?: "",
            photogateSummary?.gateBTimestampUs?.let(::formatSeconds) ?: "",
            photogateSummary?.deltaUs?.let(::formatDeltaSeconds) ?: "",
            photogateSummary?.gateAFrame?.toString() ?: "",
            photogateSummary?.gateBFrame?.toString() ?: "",
            twoBodySummary?.currentSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.minimumSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.maximumSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.averageSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.currentRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.minimumRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.maximumRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.averageRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.frameCount?.toString() ?: "",
            twoBodySummary?.unit ?: "",
            twoBodySummary?.trackAMassKg?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.trackBMassKg?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.trackAMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.trackBMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.totalMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.relativeMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachTimeUs?.let(::formatSeconds) ?: "",
            twoBodySummary?.closestApproachFrame?.toString() ?: "",
            twoBodySummary?.closestApproachSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachMomentumA?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachMomentumB?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachTotalMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachRelativeMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.interactionClass ?: "",
            twoBodySummary?.preInteractionTimeUs?.let(::formatSeconds) ?: "",
            twoBodySummary?.preInteractionFrame?.toString() ?: "",
            twoBodySummary?.preInteractionSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionMomentumA?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionMomentumB?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionTotalMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionRelativeMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionDeltaUs?.let(::formatDeltaSeconds) ?: "",
            twoBodySummary?.postInteractionTimeUs?.let(::formatSeconds) ?: "",
            twoBodySummary?.postInteractionFrame?.toString() ?: "",
            twoBodySummary?.postInteractionSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionMomentumA?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionMomentumB?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionTotalMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionRelativeMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionDeltaUs?.let(::formatDeltaSeconds) ?: "",
            twoBodySummary?.gateAToClosestUs?.let(::formatDeltaSeconds) ?: "",
            twoBodySummary?.closestToGateBUs?.let(::formatDeltaSeconds) ?: "",
            twoBodySummary?.gateAToGateBUs?.let(::formatDeltaSeconds) ?: "",
            twoBodySummary?.momentumUnit ?: ""
        ).joinToString(",")
    }
    return if (rows.isEmpty()) "$header\n" else "$header\n$rows\n"
}

fun defaultCsvFileName(projectName: String): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val normalizedName = projectName.replace(Regex("[^A-Za-z0-9_-]"), "_")
    return "${normalizedName.ifBlank { "physics_tracker" }}_$timestamp.csv"
}

private fun formatCheckpointTime(timestampMs: Long?): String {
    if (timestampMs == null) return "Not saved"
    return SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(timestampMs))
}

private fun formatCheckpointElapsed(timestampMs: Long?, nowMs: Long): String {
    if (timestampMs == null) return "SAVE"
    val elapsedMinutes = ((nowMs - timestampMs).coerceAtLeast(0L)) / 60_000L
    return "SAVE (${elapsedMinutes}m)"
}

fun formatDeltaSeconds(deltaUs: Long): String = String.format("%.3f", deltaUs / 1_000_000.0)

private fun deltaSecondsBetween(startUs: Long, endUs: Long): Double? {
    val deltaUs = endUs - startUs
    return if (deltaUs > 0L) deltaUs / 1_000_000.0 else null
}

data class DisplayKinematicsPoint(
    val pointId: Long,
    val trackLabel: String,
    val frameNumber: Int,
    val timestampUs: Long,
    val xValue: Double,
    val yValue: Double,
    val gapLabel: String?,
    val vxValue: Double?,
    val vyValue: Double?,
    val speedValue: Double?,
    val axValue: Double?,
    val ayValue: Double?,
    val accelerationValue: Double?
)

private data class TwoBodySample(
    val frameNumber: Int,
    val timestampUs: Long,
    val separationMeters: Double,
    val relativeSpeedMetersPerSec: Double?
)

private data class InteractionSnapshot(
    val timestampUs: Long,
    val frameNumber: Int,
    val separation: Double?,
    val relativeSpeed: Double?,
    val momentumA: Double?,
    val momentumB: Double?,
    val totalMomentum: Double?,
    val relativeMomentum: Double?,
    val deltaFromClosestUs: Long?
)

private data class GraphPlotPoint(
    val frameNumber: Int,
    val timestampUs: Long,
    val timeSeconds: Float,
    val value: Float,
    val xValue: Float = timeSeconds, // custom X-axis value (defaults to time for backward compat)
    val detailPoint: DisplayKinematicsPoint? = null,
    val twoBodySample: TwoBodySample? = null
)

private fun smoothGraphPlotPoints(points: List<GraphPlotPoint>, radius: Int = 2): List<GraphPlotPoint> {
    if (points.size < 3 || radius <= 0) return points
    return points.mapIndexed { index, point ->
        val start = (index - radius).coerceAtLeast(0)
        val end = (index + radius).coerceAtMost(points.lastIndex)
        val window = points.subList(start, end + 1)
        point.copy(value = window.map { it.value }.average().toFloat())
    }
}

private fun filterOutliersPlotPoints(points: List<GraphPlotPoint>, windowRadius: Int = 3, thresholdFactor: Float = 3.0f): List<GraphPlotPoint> {
    if (points.size < windowRadius * 2 + 1) return points
    
    val values = points.map { it.value }
    val xValues = points.map { it.xValue }
    val valueRange = (values.maxOrNull() ?: 0f) - (values.minOrNull() ?: 0f)
    val xValueRange = (xValues.maxOrNull() ?: 0f) - (xValues.minOrNull() ?: 0f)
    val minValueDeviation = (valueRange * 0.02f).coerceAtLeast(1e-5f)
    val minXValueDeviation = (xValueRange * 0.02f).coerceAtLeast(1e-5f)
    
    val isOutlier = BooleanArray(points.size)
    for (index in points.indices) {
        val start = (index - windowRadius).coerceAtLeast(0)
        val end = (index + windowRadius).coerceAtMost(points.lastIndex)
        val window = points.subList(start, end + 1)
        
        val sortedVals = window.map { it.value }.sorted()
        val medianVal = sortedVals[sortedVals.size / 2]
        
        val absoluteDeviations = sortedVals.map { kotlin.math.abs(it - medianVal) }.sorted()
        val madVal = absoluteDeviations[absoluteDeviations.size / 2].coerceAtLeast(minValueDeviation)
        
        val sortedXVals = window.map { it.xValue }.sorted()
        val medianXVal = sortedXVals[sortedXVals.size / 2]
        
        val absoluteDeviationsX = sortedXVals.map { kotlin.math.abs(it - medianXVal) }.sorted()
        val madXVal = absoluteDeviationsX[absoluteDeviationsX.size / 2].coerceAtLeast(minXValueDeviation)
        
        val point = points[index]
        val valueDiff = kotlin.math.abs(point.value - medianVal)
        val xValueDiff = kotlin.math.abs(point.xValue - medianXVal)
        
        if (valueDiff > thresholdFactor * madVal || xValueDiff > thresholdFactor * madXVal) {
            isOutlier[index] = true
        }
    }
    
    return points.mapIndexed { index, point ->
        if (!isOutlier[index]) {
            point
        } else {
            val start = (index - windowRadius).coerceAtLeast(0)
            val end = (index + windowRadius).coerceAtMost(points.lastIndex)
            val goodNeighbors = (start..end).filter { !isOutlier[it] }
            
            if (goodNeighbors.isNotEmpty()) {
                val avgValue = goodNeighbors.map { points[it].value }.average().toFloat()
                val avgXValue = goodNeighbors.map { points[it].xValue }.average().toFloat()
                point.copy(value = avgValue, xValue = avgXValue)
            } else {
                val startW = (index - windowRadius).coerceAtLeast(0)
                val endW = (index + windowRadius).coerceAtMost(points.lastIndex)
                val windowVals = (startW..endW).map { points[it].value }.sorted()
                val windowXVals = (startW..endW).map { points[it].xValue }.sorted()
                point.copy(
                    value = windowVals[windowVals.size / 2],
                    xValue = windowXVals[windowXVals.size / 2]
                )
            }
        }
    }
}

private fun buildResidualPlotPoints(
    points: List<GraphPlotPoint>,
    profileResult: ProfileResult?
): List<GraphPlotPoint> {
    val profile = profileResult ?: return emptyList()
    if (points.size < 2) return emptyList()
    return points.map { point ->
        val predicted = evaluateProfile(profile, point.timeSeconds.toDouble()).toFloat()
        point.copy(value = point.value - predicted)
    }
}

private fun buildDerivativePlotPoints(points: List<GraphPlotPoint>): List<GraphPlotPoint> {
    if (points.size < 2) return emptyList()
    return points.mapIndexedNotNull { index, point ->
        val left = points.getOrNull(index - 1) ?: point
        val right = points.getOrNull(index + 1) ?: point
        val dx = right.xValue - left.xValue
        if (kotlin.math.abs(dx) < 1e-9f) null else point.copy(value = (right.value - left.value) / dx)
    }
}

private fun buildIntegralPlotPoints(points: List<GraphPlotPoint>): List<GraphPlotPoint> {
    if (points.size < 2) return emptyList()
    var area = 0f
    return points.mapIndexed { index, point ->
        if (index > 0) {
            val previous = points[index - 1]
            val dx = point.xValue - previous.xValue
            area += 0.5f * (point.value + previous.value) * dx
        }
        point.copy(value = area)
    }
}

private fun lowPassPlotPoints(points: List<GraphPlotPoint>, strength: Float): List<GraphPlotPoint> {
    val radius = (1 + (strength.coerceIn(0f, 1f) * 8f).roundToInt()).coerceIn(1, 9)
    return smoothGraphPlotPoints(points, radius)
}

private fun highPassPlotPoints(points: List<GraphPlotPoint>, strength: Float): List<GraphPlotPoint> {
    if (points.size < 3) return points
    val low = lowPassPlotPoints(points, strength)
    return points.zip(low).map { (raw, smoothed) ->
        raw.copy(value = raw.value - smoothed.value)
    }
}

private fun bandPassPlotPoints(points: List<GraphPlotPoint>, strength: Float): List<GraphPlotPoint> {
    if (points.size < 5) return points
    return lowPassPlotPoints(highPassPlotPoints(points, strength), strength * 0.65f)
}

private fun baselineCorrectPlotPoints(points: List<GraphPlotPoint>): List<GraphPlotPoint> {
    if (points.size < 2) return points
    val first = points.first()
    val last = points.last()
    val dx = last.xValue - first.xValue
    if (kotlin.math.abs(dx) < 1e-9f) {
        val offset = points.map { it.value }.average().toFloat()
        return points.map { it.copy(value = it.value - offset) }
    }
    val slope = (last.value - first.value) / dx
    return points.map { point ->
        val baseline = first.value + slope * (point.xValue - first.xValue)
        point.copy(value = point.value - baseline)
    }
}

private fun normalizePlotPoints(points: List<GraphPlotPoint>): List<GraphPlotPoint> {
    if (points.isEmpty()) return points
    val min = points.minOf { it.value }
    val max = points.maxOf { it.value }
    val range = max - min
    if (kotlin.math.abs(range) < 1e-9f) return points.map { it.copy(value = 0f) }
    return points.map { it.copy(value = (it.value - min) / range) }
}

private data class FrequencyBin(
    val frequencyHz: Double,
    val amplitude: Double,
    val power: Double,
    val phaseRadians: Double
)

private fun buildFrequencySpectrum(points: List<GraphPlotPoint>, maxBins: Int = 64): List<FrequencyBin> {
    if (points.size < 4) return emptyList()
    val ordered = points.sortedBy { it.timeSeconds }
    val diffs = ordered.zipWithNext { a, b -> (b.timeSeconds - a.timeSeconds).toDouble() }
        .filter { it > 1e-9 }
    val dt = diffs.takeIf { it.isNotEmpty() }?.average() ?: return emptyList()
    val values = ordered.map { it.value.toDouble() }
    val mean = values.average()
    val n = values.size
    val usableBins = minOf(maxBins, n / 2)
    if (usableBins <= 0) return emptyList()
    return (1..usableBins).map { k ->
        var real = 0.0
        var imaginary = 0.0
        values.forEachIndexed { index, raw ->
            val centered = raw - mean
            val angle = -2.0 * PI * k.toDouble() * index.toDouble() / n.toDouble()
            real += centered * cos(angle)
            imaginary += centered * sin(angle)
        }
        val amplitude = (2.0 / n.toDouble()) * sqrt(real * real + imaginary * imaginary)
        FrequencyBin(
            frequencyHz = k.toDouble() / (n.toDouble() * dt),
            amplitude = amplitude,
            power = amplitude * amplitude,
            phaseRadians = atan2(imaginary, real)
        )
    }
}

private fun adjustedRSquared(rSquared: Double, sampleCount: Int, parameterCount: Int): Double? {
    if (sampleCount <= parameterCount + 1) return null
    return 1.0 - (1.0 - rSquared) * (sampleCount - 1).toDouble() / (sampleCount - parameterCount - 1).toDouble()
}

private fun akaikeInformationCriterion(sampleCount: Int, parameterCount: Int, rmse: Double): Double? {
    if (sampleCount <= 0 || rmse <= 0.0) return null
    val rss = rmse * rmse * sampleCount
    return sampleCount * ln(rss / sampleCount) + 2.0 * parameterCount
}

private fun bayesianInformationCriterion(sampleCount: Int, parameterCount: Int, rmse: Double): Double? {
    if (sampleCount <= 0 || rmse <= 0.0) return null
    val rss = rmse * rmse * sampleCount
    return sampleCount * ln(rss / sampleCount) + parameterCount * ln(sampleCount.toDouble())
}

private fun skewnessValue(values: List<Double>, mean: Double?, stdDev: Double?): Double? {
    if (values.size < 3 || mean == null || stdDev == null || stdDev <= 1e-12) return null
    return values.map { ((it - mean) / stdDev).pow(3.0) }.average()
}

private fun kurtosisValue(values: List<Double>, mean: Double?, stdDev: Double?): Double? {
    if (values.size < 4 || mean == null || stdDev == null || stdDev <= 1e-12) return null
    return values.map { ((it - mean) / stdDev).pow(4.0) }.average() - 3.0
}

private fun covarianceValue(points: List<GraphPlotPoint>): Double? {
    if (points.size < 2) return null
    val xs = points.map { it.xValue.toDouble() }
    val ys = points.map { it.value.toDouble() }
    val xMean = xs.average()
    val yMean = ys.average()
    return xs.indices.sumOf { (xs[it] - xMean) * (ys[it] - yMean) } / (points.size - 1)
}

private fun correlationValue(points: List<GraphPlotPoint>): Double? {
    if (points.size < 2) return null
    val xs = points.map { it.xValue.toDouble() }
    val ys = points.map { it.value.toDouble() }
    val xMean = xs.average()
    val yMean = ys.average()
    val numerator = xs.indices.sumOf { (xs[it] - xMean) * (ys[it] - yMean) }
    val xDenom = sqrt(xs.sumOf { (it - xMean) * (it - xMean) })
    val yDenom = sqrt(ys.sumOf { (it - yMean) * (it - yMean) })
    val denom = xDenom * yDenom
    return if (denom <= 1e-12) null else numerator / denom
}

private fun histogramSummary(values: List<Double>, bins: Int = 10): String {
    if (values.isEmpty()) return "-"
    val min = values.minOrNull() ?: return "-"
    val max = values.maxOrNull() ?: return "-"
    if (kotlin.math.abs(max - min) <= 1e-12) return "1 bin, all ${formatCalibratedValue(min)}"
    val counts = IntArray(bins.coerceAtLeast(1))
    values.forEach { value ->
        val index = (((value - min) / (max - min)) * counts.size).toInt().coerceIn(0, counts.lastIndex)
        counts[index]++
    }
    return "${counts.size} bins, peak count ${counts.maxOrNull() ?: 0}"
}

private data class AnalysisTimelineEvent(
    val label: String,
    val timestampUs: Long,
    val color: Color,
    val actionLabel: String
)

private data class PhotogateMarker(
    val sampleIndex: Int,
    val point: DisplayKinematicsPoint,
    val seriesValue: Float
)

private data class PhotogateCrossing(
    val gateLabel: String,
    val gateX: Float,
    val timestampUs: Long?,
    val frameNumber: Int?
)

private fun interpolateCrossingTimestampUs(
    leftX: Double,
    leftTimestampUs: Long,
    rightX: Double,
    rightTimestampUs: Long,
    gateX: Float
): Long {
    val span = rightX - leftX
    if (kotlin.math.abs(span) < 1e-6) return leftTimestampUs
    val fraction = ((gateX - leftX) / span).coerceIn(0.0, 1.0)
    return (leftTimestampUs + ((rightTimestampUs - leftTimestampUs) * fraction)).toLong()
}

private fun detectPhotogateCrossing(
    gateLabel: String,
    gateX: Float,
    points: List<TrackPointEntity>
): PhotogateCrossing {
    if (points.size < 2) return PhotogateCrossing(gateLabel, gateX, null, null)
    val crossing = points.zipWithNext().firstOrNull { (left, right) ->
        val leftDelta = left.pixelX - gateX
        val rightDelta = right.pixelX - gateX
        leftDelta == 0.0 ||
            rightDelta == 0.0 ||
            (leftDelta < 0.0 && rightDelta > 0.0) ||
            (leftDelta > 0.0 && rightDelta < 0.0)
    }
    crossing ?: return PhotogateCrossing(gateLabel, gateX, null, null)
    val (left, right) = crossing
    val timestampUs = interpolateCrossingTimestampUs(
        leftX = left.pixelX,
        leftTimestampUs = left.timestampUs,
        rightX = right.pixelX,
        rightTimestampUs = right.timestampUs,
        gateX = gateX
    )
    return PhotogateCrossing(
        gateLabel = gateLabel,
        gateX = gateX,
        timestampUs = timestampUs,
        frameNumber = right.frameNumber
    )
}

fun graphSeriesLabel(series: GraphSeries, unit: String): String {
    return when (series) {
        GraphSeries.TIME -> "t (s)"
        GraphSeries.X -> "x ($unit)"
        GraphSeries.Y -> "y ($unit)"
        GraphSeries.VX -> "vx ($unit/s)"
        GraphSeries.VY -> "vy ($unit/s)"
        GraphSeries.SPEED -> "speed ($unit/s)"
        GraphSeries.AX -> "ax ($unit/s²)"
        GraphSeries.AY -> "ay ($unit/s²)"
        GraphSeries.ACCEL -> "a ($unit/s²)"
        GraphSeries.SEPARATION -> "A-B separation ($unit)"
        GraphSeries.RELATIVE_SPEED -> "A-B relative speed ($unit/s)"
    }
}

private fun graphSeriesColor(series: GraphSeries): Color {
    return when (series) {
        GraphSeries.TIME -> Color(0xFF90A4AE)
        GraphSeries.X -> PhysicsColorTrackA
        GraphSeries.Y -> PhysicsColorTrackB
        GraphSeries.VX -> Color(0xFF80CBC4)
        GraphSeries.VY -> Color(0xFFA5D6A7)
        GraphSeries.SPEED -> PhysicsColorPrimary
        GraphSeries.AX -> PhysicsColorAccent
        GraphSeries.AY -> PhysicsColorOnBackground
        GraphSeries.ACCEL -> PhysicsColorError
        GraphSeries.SEPARATION -> PhysicsColorTrackB
        GraphSeries.RELATIVE_SPEED -> PhysicsColorTrackA
    }
}

private fun graphSeriesValue(point: DisplayKinematicsPoint, series: GraphSeries): Double? {
    return when (series) {
        GraphSeries.TIME -> point.timestampUs / 1_000_000.0
        GraphSeries.X -> point.xValue
        GraphSeries.Y -> point.yValue
        GraphSeries.VX -> point.vxValue
        GraphSeries.VY -> point.vyValue
        GraphSeries.SPEED -> point.speedValue
        GraphSeries.AX -> point.axValue
        GraphSeries.AY -> point.ayValue
        GraphSeries.ACCEL -> point.accelerationValue
        GraphSeries.SEPARATION -> null
        GraphSeries.RELATIVE_SPEED -> null
    }
}

private fun buildGraphPlotPoints(
    displayPoints: List<DisplayKinematicsPoint>,
    twoBodySamples: List<TwoBodySample>,
    series: GraphSeries,
    separationUnitFactor: Double,
    xSeries: GraphSeries = GraphSeries.TIME
): List<GraphPlotPoint> {
    return when (series) {
        GraphSeries.SEPARATION -> twoBodySamples.map {
            GraphPlotPoint(
                frameNumber = it.frameNumber,
                timestampUs = it.timestampUs,
                timeSeconds = it.timestampUs / 1_000_000f,
                value = (it.separationMeters / separationUnitFactor).toFloat(),
                xValue = it.timestampUs / 1_000_000f,
                twoBodySample = it
            )
        }
        GraphSeries.RELATIVE_SPEED -> twoBodySamples.mapNotNull {
            it.relativeSpeedMetersPerSec?.let { relativeSpeed ->
                GraphPlotPoint(
                    frameNumber = it.frameNumber,
                    timestampUs = it.timestampUs,
                    timeSeconds = it.timestampUs / 1_000_000f,
                    value = (relativeSpeed / separationUnitFactor).toFloat(),
                    xValue = it.timestampUs / 1_000_000f,
                    twoBodySample = it
                )
            }
        }
        else -> displayPoints.mapNotNull { point ->
            val yVal = graphSeriesValue(point, series) ?: return@mapNotNull null
            val xVal = graphSeriesValue(point, xSeries) ?: (point.timestampUs / 1_000_000.0)
            GraphPlotPoint(
                frameNumber = point.frameNumber,
                timestampUs = point.timestampUs,
                timeSeconds = point.timestampUs / 1_000_000f,
                value = yVal.toFloat(),
                xValue = xVal.toFloat(),
                detailPoint = point
            )
        }
    }
}

private fun buildDeltaGraphPlotPoints(
    trackA: List<GraphPlotPoint>,
    trackB: List<GraphPlotPoint>
): List<GraphPlotPoint> {
    val byFrameA = trackA.associateBy { it.frameNumber }
    return trackB.mapNotNull { pointB ->
        val pointA = byFrameA[pointB.frameNumber] ?: return@mapNotNull null
        pointB.copy(value = pointB.value - pointA.value)
    }
}

private fun formatGraphLimitInput(value: Float): String = String.format(Locale.US, "%.3f", value)

private fun closedRangeLength(start: Float, end: Float): Float = (end - start).coerceAtLeast(1e-6f)

private fun clampViewportCenter(
    center: Float,
    visibleLength: Float,
    domainStart: Float,
    domainEnd: Float
): Float {
    val domainLength = closedRangeLength(domainStart, domainEnd)
    if (visibleLength >= domainLength) return (domainStart + domainEnd) / 2f
    val half = visibleLength / 2f
    return center.coerceIn(domainStart + half, domainEnd - half)
}

private fun viewportBounds(
    center: Float,
    visibleLength: Float,
    domainStart: Float,
    domainEnd: Float
): Pair<Float, Float> {
    val domainLength = closedRangeLength(domainStart, domainEnd)
    if (visibleLength >= domainLength) return domainStart to domainEnd
    val clampedCenter = clampViewportCenter(center, visibleLength, domainStart, domainEnd)
    val half = visibleLength / 2f
    return (clampedCenter - half) to (clampedCenter + half)
}


@Composable
private fun TableCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Column(modifier = modifier.width(kinematicsColumnWidth)) {
        Text(label, color = Color.LightGray, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = valueColor, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun KinematicsRowText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        modifier = modifier.width(kinematicsColumnWidth)
    )
}

@Composable
private fun KinematicsHeaderText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        modifier = modifier.width(kinematicsColumnWidth)
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TrackingScreen(
    videoUri: String,
    projectName: String,
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel
) {
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    var deepTrackingRefreshToken by remember { mutableIntStateOf(0) }
    val deepTrackingSupport = remember(context, deepTrackingRefreshToken) { DeepTrackingSupport.from(context) }
    var mediaPipeTrackingRefreshToken by remember { mutableIntStateOf(0) }
    val mediaPipeTrackingSupport = remember(context, mediaPipeTrackingRefreshToken) {
        MediaPipeTrackingSupport.from(context)
    }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val trackPoints by viewModel.trackPoints.collectAsState()
    val currentPos by viewModel.currentPosition.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playWhenReady by viewModel.playWhenReady.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val loopPlayback by viewModel.loopPlayback.collectAsState()
    val error by viewModel.error.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val frameDurationMs by viewModel.frameDurationMs.collectAsState()
    val trimStart by viewModel.trimStart.collectAsState()
    val calibration by viewModel.calibration.collectAsState()
    val kinematicsTrackPoints by viewModel.kinematicsTrackPoints.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val lastCheckpointSavedAt by viewModel.lastCheckpointSavedAt.collectAsState()
    val nowMs by produceState(initialValue = System.currentTimeMillis(), lastCheckpointSavedAt) {
        while (true) {
            value = System.currentTimeMillis()
            delay(30_000)
        }
    }
    var autoTrackStatus by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val deepModelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            runCatching {
                DeepTrackingSupport.installModelFromUri(context, uri)
                deepTrackingRefreshToken += 1
                autoTrackStatus = "Deep model imported"
            }.onFailure {
                autoTrackStatus = it.message ?: "Deep model import failed"
            }
        }
    }
    val mediaPipeModelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            runCatching {
                MediaPipeTrackingSupport.installModelFromUri(context, uri)
                mediaPipeTrackingRefreshToken += 1
                autoTrackStatus = "MediaPipe model imported"
            }.onFailure {
                autoTrackStatus = it.message ?: "MediaPipe model import failed"
            }
        }
    }

    var selectedPointId by remember { mutableLongStateOf(-1L) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showDataTable by rememberSaveable { mutableStateOf(true) }
    var dataPanelMode by rememberSaveable { mutableStateOf(DataPanelMode.TABLE) }
    var topPanelTab by rememberSaveable { mutableStateOf<TopPanelTab?>(null) }
    var trackerPanelSection by rememberSaveable { mutableStateOf(TrackerPanelSection.MODES) }
    var showTimeline by rememberSaveable { mutableStateOf(true) }
    val orientationPrefs = remember(context) { context.getSharedPreferences("physics_tracker_settings", Context.MODE_PRIVATE) }
    var forceLandscapeMode by rememberSaveable { mutableStateOf(orientationPrefs.getBoolean("force_landscape", true)) }
    var showTransportStepOptions by rememberSaveable { mutableStateOf(false) }
    var transportStepFrames by rememberSaveable { mutableIntStateOf(10) }
    var pointHistoryMode by rememberSaveable { mutableStateOf(PointHistoryMode.FADE) }
    var showAnalysisGuides by rememberSaveable { mutableStateOf(false) }
    var showBottomSettings by rememberSaveable { mutableStateOf(false) }
    var activeBottomTab by rememberSaveable { mutableStateOf<BottomMainTab?>(BottomMainTab.TRACK) }
    var trackTrayPanel by rememberSaveable { mutableStateOf<TrackTrayPanel?>(TrackTrayPanel.TARGET) }
    var moreTrayPanel by rememberSaveable { mutableStateOf<MoreTrayPanel?>(null) }
    var activeOverlayRole by rememberSaveable { mutableStateOf(OverlayColorRole.TRACK_A) }
    var videoFocusMode by rememberSaveable { mutableStateOf(false) }
    var activeTrack by rememberSaveable { mutableStateOf(ActiveTrack.A) }
    var multiTrackEnabled by rememberSaveable { mutableStateOf(false) }
    var showMagnifier by rememberSaveable { mutableStateOf(false) }
    var videoZoomEnabled by rememberSaveable { mutableStateOf(false) }
    var videoScale by rememberSaveable { mutableFloatStateOf(1f) }
    var videoScaleSlider by rememberSaveable { mutableFloatStateOf(1f) }
    var videoOffsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var videoOffsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var autoTrackEnabled by rememberSaveable { mutableStateOf(false) }
    var autoTrackRunning by rememberSaveable { mutableStateOf(false) }
    var autoTrackAwaitingSeed by rememberSaveable { mutableStateOf(false) }
    var autoTrackSelectionMode by rememberSaveable { mutableStateOf(false) }
    var autoTrackMode by remember { mutableStateOf(AutoTrackMode.CLASSIC_FAST) }
    var trackingStepOption by rememberSaveable { mutableStateOf(TrackingStepOption.FRAME_1) }
    val liveAutoTrackPreview = remember { mutableStateListOf<AutoTrackPreviewPoint>() }
    var trackAMassText by rememberSaveable { mutableStateOf("1.0") }
    var trackBMassText by rememberSaveable { mutableStateOf("1.0") }
    var magnifierZoom by rememberSaveable { mutableFloatStateOf(16.0f) }
    var seedLockMode by rememberSaveable { mutableStateOf(SeedLockMode.EXACT_PIXEL) }
    var seedNudgeStepPx by rememberSaveable { mutableFloatStateOf(1f) }
    var majorMode by rememberSaveable { mutableStateOf<MajorMode?>(null) }
    var selectedGraphSeries by rememberSaveable { mutableStateOf(GraphSeries.X) }
    var selectedGraphSeriesX by rememberSaveable { mutableStateOf(GraphSeries.TIME) }
    var selectedGraphSampleIndex by rememberSaveable { mutableIntStateOf(-1) }
    var graphProfileMode by rememberSaveable { mutableStateOf(GraphProfileMode.NONE) }
    var graphDisplayMode by rememberSaveable { mutableStateOf(GraphDisplayMode.ORIGINAL) }
    var blendAlpha by rememberSaveable { mutableFloatStateOf(0.5f) }
    var graphFullScreen by rememberSaveable { mutableStateOf(false) }
    var calibrationTool by rememberSaveable { mutableStateOf(CalibrationTool.SCALE) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var calibrationDistanceText by remember { mutableStateOf("1.0") }
    var calibrationUnit by remember { mutableStateOf("m") }
    var calibrationGuideScale by remember { mutableFloatStateOf(1.0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var draggedCalibrationHandle by remember { mutableStateOf<CalibrationHandle?>(null) }
    var calibrationDraft by remember { mutableStateOf<CalibrationEntity?>(null) }
    var measurementStickDraft by remember { mutableStateOf<MeasurementStickDraft?>(null) }
    var draggedPointDraft by remember { mutableStateOf<Offset?>(null) }
    var draggedPointTouchDelta by remember { mutableStateOf<Offset?>(null) }
    var manualPendingPoint by remember { mutableStateOf<Offset?>(null) }
    var manualPendingTouchDelta by remember { mutableStateOf<Offset?>(null) }
    var manualDragUsesHandle by rememberSaveable { mutableStateOf(false) }
    var disableExistingPointSelection by rememberSaveable { mutableStateOf(false) }
    var disableExistingPointSelectionA by rememberSaveable { mutableStateOf(false) }
    var disableExistingPointSelectionB by rememberSaveable { mutableStateOf(false) }
    var pendingSeedScreenPoint by remember { mutableStateOf<Offset?>(null) }
    var pendingSeedTouchDelta by remember { mutableStateOf<Offset?>(null) }
    var autoSeedDragUsesHandle by rememberSaveable { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var sourceVideoSize by remember { mutableStateOf(IntSize.Zero) }
    var autoTrackSeed by remember { mutableStateOf<AutoTrackSeed?>(null) }
    var autoTrackSeedA by remember { mutableStateOf<AutoTrackSeed?>(null) }
    var autoTrackSeedB by remember { mutableStateOf<AutoTrackSeed?>(null) }
    var pendingSeedScreenPointA by remember { mutableStateOf<Offset?>(null) }
    var pendingSeedScreenPointB by remember { mutableStateOf<Offset?>(null) }
    var autoTrackAwaitingSeedA by rememberSaveable { mutableStateOf(false) }
    var autoTrackAwaitingSeedB by rememberSaveable { mutableStateOf(false) }
    var autoTrackSelectionModeA by rememberSaveable { mutableStateOf(false) }
    var autoTrackSelectionModeB by rememberSaveable { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var pendingSeekPosition by remember { mutableStateOf<Float?>(null) }
    var isTimelineDragging by rememberSaveable { mutableStateOf(false) }
    var liveGraphCutoffMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var wasGraphPlaying by remember { mutableStateOf(false) }
    var photogateAFraction by rememberSaveable { mutableFloatStateOf(0.33f) }
    var photogateBFraction by rememberSaveable { mutableFloatStateOf(0.66f) }
    var draggedPhotogateHandle by remember { mutableStateOf<PhotogateHandle?>(null) }
    var topOverlayHeightPx by remember { mutableIntStateOf(0) }
    var bottomOverlayHeightPx by remember { mutableIntStateOf(0) }
    var trackAColorArgb by rememberSaveable { mutableIntStateOf(PhysicsColorTrackA.toArgb()) }
    var trackBColorArgb by rememberSaveable { mutableIntStateOf(PhysicsColorTrackB.toArgb()) }
    var manualSelectorColorArgb by rememberSaveable { mutableIntStateOf(PhysicsColorOnBackground.toArgb()) }
    var vectorColorArgb by rememberSaveable { mutableIntStateOf(PhysicsColorAccent.toArgb()) }
    var calibrationColorArgb by rememberSaveable { mutableIntStateOf(PhysicsColorPrimary.toArgb()) }
    var graphViewportSummary by remember { mutableStateOf(GraphViewportSummary()) }
    var showGraphOptions by rememberSaveable { mutableStateOf(false) }
    var showGraphTrackA by rememberSaveable { mutableStateOf(true) }
    var showGraphTrackB by rememberSaveable { mutableStateOf(true) }
    var showGraphDelta by rememberSaveable { mutableStateOf(false) }
    var showTableTrackA by rememberSaveable { mutableStateOf(true) }
    var showTableTrackB by rememberSaveable { mutableStateOf(false) }
    var lightWorkspaceTab by rememberSaveable { mutableStateOf(LightWorkspaceTab.SELECT) }
    var lightPanelMode by rememberSaveable { mutableStateOf(LightPanelMode.PROFILE) }
    var lightCompareSource by rememberSaveable { mutableStateOf(LightCompareSource.PROFILE) }
    var lightTool by rememberSaveable { mutableStateOf(LightTool.LINE) }
    var lightChannel by rememberSaveable { mutableStateOf(LightChannel.GRAY) }
    var lightActiveSlot by rememberSaveable { mutableStateOf(LightSlot.A) }
    var lightDualMode by rememberSaveable { mutableStateOf(false) }
    var showLightGraphA by rememberSaveable { mutableStateOf(true) }
    var showLightGraphB by rememberSaveable { mutableStateOf(false) }
    var showLightGraphC by rememberSaveable { mutableStateOf(false) }
    var showLightGraphD by rememberSaveable { mutableStateOf(false) }
    var showLightTableA by rememberSaveable { mutableStateOf(true) }
    var showLightTableB by rememberSaveable { mutableStateOf(false) }
    var showLightTableC by rememberSaveable { mutableStateOf(false) }
    var showLightTableD by rememberSaveable { mutableStateOf(false) }
    var lightBackgroundSubtract by rememberSaveable { mutableStateOf(false) }
    var lightSmooth by rememberSaveable { mutableStateOf(true) }
    var lightDetectPeaks by rememberSaveable { mutableStateOf(false) }
    var lightTimeRecording by rememberSaveable { mutableStateOf(false) }
    var lightGeometryA by remember { mutableStateOf<LightGeometry?>(null) }
    var lightGeometryB by remember { mutableStateOf<LightGeometry?>(null) }
    var lightGeometryC by remember { mutableStateOf<LightGeometry?>(null) }
    var lightGeometryD by remember { mutableStateOf<LightGeometry?>(null) }
    var lightDragHandle by remember { mutableStateOf<LightDragHandle?>(null) }
    var lightDragSlot by remember { mutableStateOf<LightSlot?>(null) }
    var lightProfileA by remember { mutableStateOf<List<LightSamplePoint>>(emptyList()) }
    var lightProfileB by remember { mutableStateOf<List<LightSamplePoint>>(emptyList()) }
    var lightProfileC by remember { mutableStateOf<List<LightSamplePoint>>(emptyList()) }
    var lightProfileD by remember { mutableStateOf<List<LightSamplePoint>>(emptyList()) }
    val lightTimeSeriesA = remember { mutableStateListOf<LightTimePoint>() }
    val lightTimeSeriesB = remember { mutableStateListOf<LightTimePoint>() }
    val lightTimeSeriesC = remember { mutableStateListOf<LightTimePoint>() }
    val lightTimeSeriesD = remember { mutableStateListOf<LightTimePoint>() }
    val absoluteCurrentPos = trimStart + currentPos
    val displayedCalibration = calibrationDraft ?: calibration
    val localDensity = androidx.compose.ui.platform.LocalDensity.current
    val density = localDensity.density
    val autoSelectTouchOffsetPx = with(localDensity) { 58.dp.toPx() }
    val autoSeedHandleLiftPx = with(localDensity) { autoSeedHandleLiftDp.dp.toPx() }
    val manualHandleLiftPx = with(localDensity) { manualHandleLiftDp.dp.toPx() }
    val calibrationMode = majorMode == MajorMode.CAL
    val editMode = majorMode == MajorMode.EDIT
    val dataMode = majorMode == MajorMode.DATA
    val lightMode = majorMode == MajorMode.LIGHT
    val analysisMode = dataMode && dataPanelMode == DataPanelMode.ANALYSIS
    LaunchedEffect(forceLandscapeMode) {
        orientationPrefs.edit().putBoolean("force_landscape", forceLandscapeMode).apply()
        context.findActivity()?.requestedOrientation = if (forceLandscapeMode) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    val trackMode = majorMode == MajorMode.TRACK
    val dataPanelTopPadding = with(localDensity) { topOverlayHeightPx.toDp() } + 6.dp
    val dataPanelBottomPadding = with(localDensity) { bottomOverlayHeightPx.toDp() } + 8.dp
    val activeTrackLabel = activeTrack.name
    val twoTrackMode = multiTrackEnabled
    val hasPreparedAutoTrack = if (twoTrackMode) {
        autoTrackSeedA != null || autoTrackSeedB != null
    } else {
        autoTrackSeed != null
    }
    fun maxVideoPanX(scale: Float = videoScale): Float =
        (canvasSize.width.toFloat().coerceAtLeast(1f) * (scale - 1f) * 0.5f).coerceAtLeast(0f)
    fun maxVideoPanY(scale: Float = videoScale): Float =
        (canvasSize.height.toFloat().coerceAtLeast(1f) * (scale - 1f) * 0.5f).coerceAtLeast(0f)
    fun clampVideoPan(scale: Float = videoScale) {
        val boundedScale = scale.coerceIn(1f, 6f)
        val maxPanX = maxVideoPanX(boundedScale)
        val maxPanY = maxVideoPanY(boundedScale)
        videoScale = boundedScale
        videoScaleSlider = boundedScale
        videoOffsetX = videoOffsetX.coerceIn(-maxPanX, maxPanX)
        videoOffsetY = videoOffsetY.coerceIn(-maxPanY, maxPanY)
        if (boundedScale <= 1.02f) {
            videoScale = 1f
            videoScaleSlider = 1f
            videoOffsetX = 0f
            videoOffsetY = 0f
        }
    }
    val trackAMassKg = trackPoints.firstOrNull { it.trackLabel == "A" }?.massKg ?: (trackAMassText.toDoubleOrNull() ?: 1.0)
    val trackBMassKg = trackPoints.firstOrNull { it.trackLabel == "B" }?.massKg ?: (trackBMassText.toDoubleOrNull() ?: 1.0)
    val filteredTrackPoints = remember(trackPoints, activeTrackLabel) {
        trackPoints.filter { it.trackLabel == activeTrackLabel }
    }
    val visibleOverlayPoints = remember(trackPoints, twoTrackMode) {
        if (twoTrackMode) trackPoints else trackPoints.filter { it.trackLabel == "A" }
    }
    val visibleAutoTrackPreview = remember(liveAutoTrackPreview.toList(), absoluteCurrentPos) {
        liveAutoTrackPreview.filter { (it.timestampUs / 1000L) <= absoluteCurrentPos }
    }
    fun clearTrackingWorkspaceState() {
        autoTrackRunning = false
        autoTrackAwaitingSeed = false
        autoTrackSelectionMode = false
        autoTrackSeed = null
        pendingSeedScreenPoint = null
        pendingSeedTouchDelta = null
        autoSeedDragUsesHandle = false
        selectedPointId = -1L
        selectedGraphSampleIndex = -1
        draggedPointDraft = null
        draggedPointTouchDelta = null
        manualPendingPoint = null
        manualPendingTouchDelta = null
        manualDragUsesHandle = false
        autoTrackStatus = null
        liveAutoTrackPreview.clear()
    }
    fun advanceManualTrackingFrame() {
        if (!trackMode || autoTrackEnabled) return
        viewModel.stepFrames(trackingStepOption.frameStep.coerceAtLeast(1))
    }
    fun activeTrackMassKg(): Double = if (activeTrack == ActiveTrack.A) trackAMassKg else trackBMassKg
    fun currentDisableExistingPointSelection(): Boolean =
        if (activeTrack == ActiveTrack.A) disableExistingPointSelectionA else disableExistingPointSelectionB
    fun setCurrentDisableExistingPointSelection(value: Boolean) {
        if (activeTrack == ActiveTrack.A) disableExistingPointSelectionA = value else disableExistingPointSelectionB = value
        disableExistingPointSelection = value
    }
    fun saveCurrentTrackAutoState() {
        if (!twoTrackMode) return
        when (activeTrack) {
            ActiveTrack.A -> {
                autoTrackSeedA = autoTrackSeed
                pendingSeedScreenPointA = pendingSeedScreenPoint
                autoTrackAwaitingSeedA = autoTrackAwaitingSeed
                autoTrackSelectionModeA = autoTrackSelectionMode
            }
            ActiveTrack.B -> {
                autoTrackSeedB = autoTrackSeed
                pendingSeedScreenPointB = pendingSeedScreenPoint
                autoTrackAwaitingSeedB = autoTrackAwaitingSeed
                autoTrackSelectionModeB = autoTrackSelectionMode
            }
        }
    }
    fun loadCurrentTrackAutoState() {
        if (!twoTrackMode) return
        when (activeTrack) {
            ActiveTrack.A -> {
                autoTrackSeed = autoTrackSeedA
                pendingSeedScreenPoint = pendingSeedScreenPointA
                autoTrackAwaitingSeed = autoTrackAwaitingSeedA
                autoTrackSelectionMode = autoTrackSelectionModeA
                disableExistingPointSelection = disableExistingPointSelectionA
            }
            ActiveTrack.B -> {
                autoTrackSeed = autoTrackSeedB
                pendingSeedScreenPoint = pendingSeedScreenPointB
                autoTrackAwaitingSeed = autoTrackAwaitingSeedB
                autoTrackSelectionMode = autoTrackSelectionModeB
                disableExistingPointSelection = disableExistingPointSelectionB
            }
        }
    }
    fun toggleTrackCountMode() {
        if (twoTrackMode) {
            saveCurrentTrackAutoState()
        } else {
            autoTrackSeedA = autoTrackSeed
            pendingSeedScreenPointA = pendingSeedScreenPoint
            autoTrackAwaitingSeedA = autoTrackAwaitingSeed
            autoTrackSelectionModeA = autoTrackSelectionMode
            disableExistingPointSelectionA = disableExistingPointSelection
        }
        multiTrackEnabled = !multiTrackEnabled
        activeTrack = ActiveTrack.A
        selectedPointId = -1L
        manualPendingPoint = null
        draggedPointDraft = null
        if (multiTrackEnabled) {
            autoTrackSeedB = null
            pendingSeedScreenPointB = null
            autoTrackAwaitingSeedB = false
            autoTrackSelectionModeB = false
            disableExistingPointSelectionB = false
        }
        loadCurrentTrackAutoState()
    }
    fun trackSeedFor(track: ActiveTrack): AutoTrackSeed? = when (track) {
        ActiveTrack.A -> if (twoTrackMode) autoTrackSeedA else autoTrackSeed
        ActiveTrack.B -> autoTrackSeedB
    }
    fun runEligibleAutoTracks(): List<ActiveTrack> =
        if (twoTrackMode) {
            buildList {
                if (autoTrackSeedA != null) add(ActiveTrack.A)
                if (autoTrackSeedB != null) add(ActiveTrack.B)
            }
        } else {
            listOf(activeTrack).filter { trackSeedFor(it) != null }
        }
    fun lightGeometryFor(slot: LightSlot): LightGeometry? = when (slot) {
        LightSlot.A -> lightGeometryA
        LightSlot.B -> lightGeometryB
        LightSlot.C -> lightGeometryC
        LightSlot.D -> lightGeometryD
    }
    fun setLightGeometry(slot: LightSlot, geometry: LightGeometry?) {
        when (slot) {
            LightSlot.A -> lightGeometryA = geometry
            LightSlot.B -> lightGeometryB = geometry
            LightSlot.C -> lightGeometryC = geometry
            LightSlot.D -> lightGeometryD = geometry
        }
    }
    fun ensureLightGeometry(slot: LightSlot): LightGeometry {
        val existing = lightGeometryFor(slot)
        return if (existing != null && existing.tool == lightTool) {
            existing
        } else {
            val created = lightDefaultGeometry(lightTool, canvasSize, slot)
            setLightGeometry(slot, created)
            created
        }
    }
    fun clearLightTimeSeries(slot: LightSlot) {
        when (slot) {
            LightSlot.A -> lightTimeSeriesA.clear()
            LightSlot.B -> lightTimeSeriesB.clear()
            LightSlot.C -> lightTimeSeriesC.clear()
            LightSlot.D -> lightTimeSeriesD.clear()
        }
    }
    fun clearAllLightTimeSeries() {
        lightTimeSeriesA.clear()
        lightTimeSeriesB.clear()
        lightTimeSeriesC.clear()
        lightTimeSeriesD.clear()
    }
    fun measurementLengthInDisplayUnits(measurement: MeasurementStickDraft?, calibrationEntity: CalibrationEntity?): Double? {
        val currentMeasurement = measurement ?: return null
        val currentCalibration = calibrationEntity ?: return null
        val calibrationPixels = distanceBetween(
            Offset(currentCalibration.stickP1x.toFloat(), currentCalibration.stickP1y.toFloat()),
            Offset(currentCalibration.stickP2x.toFloat(), currentCalibration.stickP2y.toFloat())
        ).toDouble()
        if (calibrationPixels <= 0.0) return null
        val measurementPixels = distanceBetween(currentMeasurement.start, currentMeasurement.end).toDouble()
        val calibrationDisplayDistance = displayCalibrationDistance(currentCalibration)
        return (measurementPixels / calibrationPixels) * calibrationDisplayDistance
    }
    fun confirmManualSelection() {
        if (autoTrackEnabled || !trackMode) return
        val activeMass = activeTrackMassKg()
        when {
            selectedPointId != -1L -> {
                val point = trackPoints.firstOrNull { it.id == selectedPointId }
                val draft = draggedPointDraft
                if (point != null && draft != null) {
                    viewModel.updatePoint(point, draft.x.toDouble(), draft.y.toDouble())
                }
                selectedPointId = -1L
                draggedPointDraft = null
                draggedPointTouchDelta = null
            }
            manualPendingPoint != null -> {
                val point = manualPendingPoint ?: return
                viewModel.addPointAt(
                    positionMs = absoluteCurrentPos,
                    x = point.x.toDouble(),
                    y = point.y.toDouble(),
                    trackLabel = activeTrack.name,
                    massKg = activeMass
                )
                manualPendingPoint = null
                manualPendingTouchDelta = null
            }
            else -> return
        }
        advanceManualTrackingFrame()
    }
    val availableGraphSeries = remember(twoTrackMode) {
        buildList {
            add(GraphSeries.X)
            add(GraphSeries.Y)
            add(GraphSeries.VX)
            add(GraphSeries.VY)
            add(GraphSeries.SPEED)
            add(GraphSeries.AX)
            add(GraphSeries.AY)
            add(GraphSeries.ACCEL)
            if (twoTrackMode) {
                add(GraphSeries.SEPARATION)
                add(GraphSeries.RELATIVE_SPEED)
            }
        }
    }
    // X-axis options: all single-body kinematic variables (two-body series can only be Y-axis)
    val availableGraphSeriesX = remember {
        listOf(GraphSeries.TIME, GraphSeries.X, GraphSeries.Y, GraphSeries.VX, GraphSeries.VY, GraphSeries.SPEED, GraphSeries.AX, GraphSeries.AY, GraphSeries.ACCEL)
    }
    val twoBodySamples = remember(trackPoints, calibration) {
        val currentCalibration = calibration ?: return@remember emptyList()
        val trackAPoints = trackPoints.filter { it.trackLabel == "A" }.associateBy { it.frameNumber }
        val trackBPoints = trackPoints.filter { it.trackLabel == "B" }.associateBy { it.frameNumber }
        val matched = trackAPoints.keys.intersect(trackBPoints.keys).sorted().mapNotNull { frameNumber ->
            val pointA = trackAPoints[frameNumber] ?: return@mapNotNull null
            val pointB = trackBPoints[frameNumber] ?: return@mapNotNull null
            val calibratedA = calibratePoint(pointA, currentCalibration) ?: return@mapNotNull null
            val calibratedB = calibratePoint(pointB, currentCalibration) ?: return@mapNotNull null
            val dx = calibratedB.xMeters - calibratedA.xMeters
            val dy = calibratedB.yMeters - calibratedA.yMeters
            TwoBodySample(
                frameNumber = frameNumber,
                timestampUs = maxOf(calibratedA.timestampUs, calibratedB.timestampUs),
                separationMeters = sqrt(dx * dx + dy * dy),
                relativeSpeedMetersPerSec = null
            )
        }
        matched.mapIndexed { index, sample ->
            val relativeSpeed = when (index) {
                0 -> {
                    val next = matched.getOrNull(1)
                    next?.let {
                        val dt = deltaSecondsBetween(sample.timestampUs, it.timestampUs)
                        if (dt != null) kotlin.math.abs(it.separationMeters - sample.separationMeters) / dt else null
                    }
                }
                matched.lastIndex -> {
                    val previous = matched.getOrNull(index - 1)
                    previous?.let {
                        val dt = deltaSecondsBetween(it.timestampUs, sample.timestampUs)
                        if (dt != null) kotlin.math.abs(sample.separationMeters - it.separationMeters) / dt else null
                    }
                }
                else -> {
                    val previous = matched[index - 1]
                    val next = matched[index + 1]
                    val dt = deltaSecondsBetween(previous.timestampUs, next.timestampUs)
                    if (dt != null) kotlin.math.abs(next.separationMeters - previous.separationMeters) / dt else null
                }
            }
            sample.copy(relativeSpeedMetersPerSec = relativeSpeed)
        }
    }
    val displayedSeparationUnitFactor = calibration?.let { calibrationUnitFactor(it.calibrationUnit) } ?: 1.0
    val currentTwoBodySample = remember(twoBodySamples, absoluteCurrentPos) {
        twoBodySamples.minByOrNull { abs(it.timestampUs / 1000 - absoluteCurrentPos) }
            ?.takeIf { abs(it.timestampUs / 1000 - absoluteCurrentPos) < 25L }
    }
    val separationValues = remember(twoBodySamples, displayedSeparationUnitFactor) {
        twoBodySamples.map { it.separationMeters / displayedSeparationUnitFactor }
    }
    val relativeSpeedValues = remember(twoBodySamples, displayedSeparationUnitFactor) {
        twoBodySamples.mapNotNull { it.relativeSpeedMetersPerSec?.div(displayedSeparationUnitFactor) }
    }
    val averageSeparation = separationValues.takeIf { it.isNotEmpty() }?.average()
    val minimumSeparation = separationValues.minOrNull()
    val maximumSeparation = separationValues.maxOrNull()
    val currentRelativeSpeed = currentTwoBodySample?.relativeSpeedMetersPerSec?.div(displayedSeparationUnitFactor)
    val averageRelativeSpeed = relativeSpeedValues.takeIf { it.isNotEmpty() }?.average()
    val minimumRelativeSpeed = relativeSpeedValues.minOrNull()
    val maximumRelativeSpeed = relativeSpeedValues.maxOrNull()
    val trackAKinematics = remember(kinematicsTrackPoints, trackPoints) {
        val ids = trackPoints.filter { it.trackLabel == "A" }.map { it.id }.toSet()
        kinematicsTrackPoints.filter { it.pointId in ids }
    }
    val trackBKinematics = remember(kinematicsTrackPoints, trackPoints) {
        val ids = trackPoints.filter { it.trackLabel == "B" }.map { it.id }.toSet()
        kinematicsTrackPoints.filter { it.pointId in ids }
    }
    val currentTrackAKinematics = remember(trackAKinematics, absoluteCurrentPos) {
        trackAKinematics.minByOrNull { abs(it.timestampUs / 1000 - absoluteCurrentPos) }
            ?.takeIf { abs(it.timestampUs / 1000 - absoluteCurrentPos) < 25L }
    }
    val currentTrackBKinematics = remember(trackBKinematics, absoluteCurrentPos) {
        trackBKinematics.minByOrNull { abs(it.timestampUs / 1000 - absoluteCurrentPos) }
            ?.takeIf { abs(it.timestampUs / 1000 - absoluteCurrentPos) < 25L }
    }
    val closestApproachSample = remember(twoBodySamples) {
        twoBodySamples.minByOrNull { it.separationMeters }
    }
    val closestApproachIndex = remember(twoBodySamples, closestApproachSample) {
        val frameNumber = closestApproachSample?.frameNumber ?: return@remember -1
        twoBodySamples.indexOfFirst { it.frameNumber == frameNumber }
    }
    val preInteractionSample = remember(twoBodySamples, closestApproachIndex) {
        if (closestApproachIndex > 0) twoBodySamples[closestApproachIndex - 1] else null
    }
    val postInteractionSample = remember(twoBodySamples, closestApproachIndex) {
        if (closestApproachIndex >= 0 && closestApproachIndex < twoBodySamples.lastIndex) {
            twoBodySamples[closestApproachIndex + 1]
        } else {
            null
        }
    }
    val closestApproachTrackAKinematics = remember(trackAKinematics, closestApproachSample) {
        val frameNumber = closestApproachSample?.frameNumber ?: return@remember null
        trackAKinematics.firstOrNull { it.frameNumber == frameNumber }
    }
    val closestApproachTrackBKinematics = remember(trackBKinematics, closestApproachSample) {
        val frameNumber = closestApproachSample?.frameNumber ?: return@remember null
        trackBKinematics.firstOrNull { it.frameNumber == frameNumber }
    }
    val preInteractionTrackAKinematics = remember(trackAKinematics, preInteractionSample) {
        val frameNumber = preInteractionSample?.frameNumber ?: return@remember null
        trackAKinematics.firstOrNull { it.frameNumber == frameNumber }
    }
    val preInteractionTrackBKinematics = remember(trackBKinematics, preInteractionSample) {
        val frameNumber = preInteractionSample?.frameNumber ?: return@remember null
        trackBKinematics.firstOrNull { it.frameNumber == frameNumber }
    }
    val postInteractionTrackAKinematics = remember(trackAKinematics, postInteractionSample) {
        val frameNumber = postInteractionSample?.frameNumber ?: return@remember null
        trackAKinematics.firstOrNull { it.frameNumber == frameNumber }
    }
    val postInteractionTrackBKinematics = remember(trackBKinematics, postInteractionSample) {
        val frameNumber = postInteractionSample?.frameNumber ?: return@remember null
        trackBKinematics.firstOrNull { it.frameNumber == frameNumber }
    }
    val momentumUnitLabel = "${calibration?.calibrationUnit ?: "m"}·kg/s"
    val trackAMomentum = currentTrackAKinematics?.speedMetersPerSec?.times(trackAMassKg)?.div(displayedSeparationUnitFactor)
    val trackBMomentum = currentTrackBKinematics?.speedMetersPerSec?.times(trackBMassKg)?.div(displayedSeparationUnitFactor)
    val totalMomentum = listOfNotNull(trackAMomentum, trackBMomentum).sum().takeIf { (trackAMomentum != null) || (trackBMomentum != null) }
    val relativeMomentum = if (trackAMomentum != null && trackBMomentum != null) kotlin.math.abs(trackAMomentum - trackBMomentum) else null
    val closestApproachMomentumA = closestApproachTrackAKinematics?.speedMetersPerSec?.times(trackAMassKg)?.div(displayedSeparationUnitFactor)
    val closestApproachMomentumB = closestApproachTrackBKinematics?.speedMetersPerSec?.times(trackBMassKg)?.div(displayedSeparationUnitFactor)
    val closestApproachTotalMomentum = listOfNotNull(closestApproachMomentumA, closestApproachMomentumB)
        .sum()
        .takeIf { closestApproachMomentumA != null || closestApproachMomentumB != null }
    val closestApproachRelativeMomentum = if (closestApproachMomentumA != null && closestApproachMomentumB != null) {
        kotlin.math.abs(closestApproachMomentumA - closestApproachMomentumB)
    } else {
        null
    }
    fun buildInteractionSnapshot(
        sample: TwoBodySample?,
        trackAKinematicsPoint: KinematicsPoint?,
        trackBKinematicsPoint: KinematicsPoint?
    ): InteractionSnapshot? {
        val snapshotSample = sample ?: return null
        val momentumA = trackAKinematicsPoint?.speedMetersPerSec?.times(trackAMassKg)?.div(displayedSeparationUnitFactor)
        val momentumB = trackBKinematicsPoint?.speedMetersPerSec?.times(trackBMassKg)?.div(displayedSeparationUnitFactor)
        val total = listOfNotNull(momentumA, momentumB).sum().takeIf { momentumA != null || momentumB != null }
        val relative = if (momentumA != null && momentumB != null) kotlin.math.abs(momentumA - momentumB) else null
        return InteractionSnapshot(
            timestampUs = snapshotSample.timestampUs,
            frameNumber = snapshotSample.frameNumber,
            separation = snapshotSample.separationMeters / displayedSeparationUnitFactor,
            relativeSpeed = snapshotSample.relativeSpeedMetersPerSec?.div(displayedSeparationUnitFactor),
            momentumA = momentumA,
            momentumB = momentumB,
            totalMomentum = total,
            relativeMomentum = relative,
            deltaFromClosestUs = closestApproachSample?.timestampUs?.let { kotlin.math.abs(snapshotSample.timestampUs - it) }
        )
    }
    val preInteractionSnapshot = remember(
        preInteractionSample,
        preInteractionTrackAKinematics,
        preInteractionTrackBKinematics,
        trackAMassKg,
        trackBMassKg,
        displayedSeparationUnitFactor,
        closestApproachSample
    ) {
        buildInteractionSnapshot(
            sample = preInteractionSample,
            trackAKinematicsPoint = preInteractionTrackAKinematics,
            trackBKinematicsPoint = preInteractionTrackBKinematics
        )
    }
    val postInteractionSnapshot = remember(
        postInteractionSample,
        postInteractionTrackAKinematics,
        postInteractionTrackBKinematics,
        trackAMassKg,
        trackBMassKg,
        displayedSeparationUnitFactor,
        closestApproachSample
    ) {
        buildInteractionSnapshot(
            sample = postInteractionSample,
            trackAKinematicsPoint = postInteractionTrackAKinematics,
            trackBKinematicsPoint = postInteractionTrackBKinematics
        )
    }
    val interactionClassification = remember(
        closestApproachSample,
        preInteractionSnapshot,
        postInteractionSnapshot
    ) {
        val closestSeparation = closestApproachSample?.separationMeters?.div(displayedSeparationUnitFactor) ?: return@remember null
        val beforeMovesIn = preInteractionSnapshot?.separation?.let { it > closestSeparation + 1e-6 } == true
        val afterMovesOut = postInteractionSnapshot?.separation?.let { it > closestSeparation + 1e-6 } == true
        val slowedAfter = preInteractionSnapshot?.relativeSpeed?.let { before ->
            postInteractionSnapshot?.relativeSpeed?.let { after -> before > 0.0 && after < before * 0.85 }
        } == true
        when {
            beforeMovesIn && afterMovesOut && slowedAfter -> "possible collision-like event"
            beforeMovesIn && afterMovesOut -> "closest approach"
            beforeMovesIn -> "approaching"
            afterMovesOut -> "separating"
            else -> "interaction window"
        }
    }
    LaunchedEffect(trackPoints) {
        trackAMassText = String.format(Locale.US, "%.3f", trackAMassKg)
        trackBMassText = String.format(Locale.US, "%.3f", trackBMassKg)
    }
    val gateAX = remember(canvasSize, photogateAFraction) { canvasSize.width.toFloat() * photogateAFraction.coerceIn(0.05f, 0.95f) }
    val gateBX = remember(canvasSize, photogateBFraction) { canvasSize.width.toFloat() * photogateBFraction.coerceIn(0.05f, 0.95f) }
    val photogateA = remember(filteredTrackPoints, gateAX) { detectPhotogateCrossing("A", gateAX, filteredTrackPoints) }
    val photogateB = remember(filteredTrackPoints, gateBX) { detectPhotogateCrossing("B", gateBX, filteredTrackPoints) }
    val gateAToClosestUs = if (photogateA.timestampUs != null && closestApproachSample?.timestampUs != null) {
        kotlin.math.abs(closestApproachSample.timestampUs - photogateA.timestampUs)
    } else null
    val closestToGateBUs = if (closestApproachSample?.timestampUs != null && photogateB.timestampUs != null) {
        kotlin.math.abs(photogateB.timestampUs - closestApproachSample.timestampUs)
    } else null
    val videoPhotogateDeltaUs = if (photogateA.timestampUs != null && photogateB.timestampUs != null) {
        kotlin.math.abs(photogateB.timestampUs - photogateA.timestampUs)
    } else null
    BackHandler {
        if (graphFullScreen) {
            graphFullScreen = false
        } else if (videoFocusMode) {
            videoFocusMode = false
        } else {
            showExitConfirm = true
        }
    }

    LaunchedEffect(calibration?.knownDistanceMeters, calibration?.calibrationUnit) {
        calibrationDistanceText = calibration?.let { displayCalibrationDistance(it).toString() } ?: "1.0"
        calibrationUnit = calibration?.calibrationUnit ?: "m"
    }

    // BottomMainTab.PLAY is a persistent transport tray — do NOT auto-reset it.

    LaunchedEffect(calibration) {
        if (draggedCalibrationHandle == null) {
            calibrationDraft = calibration
        }
    }

    LaunchedEffect(selectedGraphSeries, dataPanelMode) {
        selectedGraphSampleIndex = -1
    }
    LaunchedEffect(dataMode, dataPanelMode) {
        if (dataMode && (dataPanelMode == DataPanelMode.GRAPH || dataPanelMode == DataPanelMode.ANALYSIS)) {
            if (selectedGraphSeries == GraphSeries.SEPARATION || selectedGraphSeries == GraphSeries.RELATIVE_SPEED) {
                if (!twoTrackMode) {
                    selectedGraphSeries = GraphSeries.X
                }
            }
        }
    }
    LaunchedEffect(dataMode, dataPanelMode, isPlaying, absoluteCurrentPos) {
        val graphModeActive = dataMode && dataPanelMode == DataPanelMode.GRAPH
        when {
            graphModeActive && isPlaying -> {
                liveGraphCutoffMs = absoluteCurrentPos
                wasGraphPlaying = true
            }
            graphModeActive && !isPlaying && wasGraphPlaying -> {
                liveGraphCutoffMs = absoluteCurrentPos
            }
            !graphModeActive -> {
                liveGraphCutoffMs = null
                wasGraphPlaying = false
            }
        }
    }
    LaunchedEffect(twoTrackMode) {
        if (!twoTrackMode) {
            activeTrack = ActiveTrack.A
            if (selectedGraphSeries == GraphSeries.SEPARATION || selectedGraphSeries == GraphSeries.RELATIVE_SPEED) {
                selectedGraphSeries = GraphSeries.SPEED
            }
            disableExistingPointSelection = disableExistingPointSelectionA
        }
    }

    LaunchedEffect(exportStatus) {
        if (exportStatus != null) {
            delay(2200)
            exportStatus = null
        }
    }
    LaunchedEffect(autoTrackStatus, autoTrackRunning) {
        if (autoTrackStatus != null && !autoTrackRunning) {
            delay(2200)
            autoTrackStatus = null
        }
    }
    LaunchedEffect(twoTrackMode) {
        if (twoTrackMode) {
            autoTrackRunning = false
            autoTrackStatus = null
            saveCurrentTrackAutoState()
            activeTrack = ActiveTrack.A
            loadCurrentTrackAutoState()
        }
    }
    LaunchedEffect(activeTrack, twoTrackMode) {
        if (twoTrackMode) {
            loadCurrentTrackAutoState()
        }
        selectedPointId = -1L
        draggedPointDraft = null
        draggedPointTouchDelta = null
        manualPendingPoint = null
        manualPendingTouchDelta = null
    }

    LaunchedEffect(calibrationMode, canvasSize, displayedCalibration) {
        if (calibrationMode && displayedCalibration == null && canvasSize != IntSize.Zero) {
            viewModel.ensureCalibrationDefaults(
                canvasSize.width.toFloat(),
                canvasSize.height.toFloat()
            )
        }
    }
    LaunchedEffect(calibrationMode, displayedCalibration, canvasSize) {
        if (!calibrationMode || displayedCalibration == null || canvasSize == IntSize.Zero) return@LaunchedEffect
        if (measurementStickDraft == null) {
            val start = Offset(displayedCalibration.stickP1x.toFloat(), displayedCalibration.stickP1y.toFloat())
            val end = Offset(displayedCalibration.stickP2x.toFloat(), displayedCalibration.stickP2y.toFloat())
            val offset = Offset(0f, 72.dp.value * density)
            measurementStickDraft = MeasurementStickDraft(
                start = (start + offset).copy(
                    x = (start.x + offset.x).coerceIn(0f, canvasSize.width.toFloat().coerceAtLeast(1f)),
                    y = (start.y + offset.y).coerceIn(0f, canvasSize.height.toFloat().coerceAtLeast(1f))
                ),
                end = (end + offset).copy(
                    x = (end.x + offset.x).coerceIn(0f, canvasSize.width.toFloat().coerceAtLeast(1f)),
                    y = (end.y + offset.y).coerceIn(0f, canvasSize.height.toFloat().coerceAtLeast(1f))
                )
            )
        }
    }
    LaunchedEffect(videoScale) {
        videoScaleSlider = videoScale
    }

    // Logic: Find the single point that most closely matches the current video frame
    val activePoint = remember(filteredTrackPoints, absoluteCurrentPos) {
        filteredTrackPoints.minByOrNull { abs(it.timestampUs / 1000 - absoluteCurrentPos) }
            ?.takeIf { abs(it.timestampUs / 1000 - absoluteCurrentPos) < 25L }
    }
    val displayedUnitFactor = calibration?.let { calibrationUnitFactor(it.calibrationUnit) } ?: 1.0
    val displayedUnit = calibration?.calibrationUnit ?: "px"
    val activeTrackPointIds = remember(filteredTrackPoints) { filteredTrackPoints.map { it.id }.toSet() }
    fun buildDisplayPointsForTrack(trackLabel: String): List<DisplayKinematicsPoint> {
        val trackRawPoints = trackPoints.filter { it.trackLabel == trackLabel }.sortedBy { it.frameNumber }
        val trackPointIds = trackRawPoints.map { it.id }.toSet()
        return if (calibration != null) {
            val activeTrackKinematics = kinematicsTrackPoints.filter { it.pointId in trackPointIds }
            activeTrackKinematics.mapIndexed { index, point ->
                val previousFrame = activeTrackKinematics.getOrNull(index - 1)?.frameNumber
                val frameGap = previousFrame?.let { point.frameNumber - it - 1 } ?: 0
                DisplayKinematicsPoint(
                    pointId = point.pointId,
                    trackLabel = trackLabel,
                    frameNumber = point.frameNumber,
                    timestampUs = point.timestampUs,
                    xValue = point.xMeters / displayedUnitFactor,
                    yValue = point.yMeters / displayedUnitFactor,
                    gapLabel = if (frameGap > 0) "+$frameGap f" else null,
                    vxValue = point.vxMetersPerSec?.div(displayedUnitFactor),
                    vyValue = point.vyMetersPerSec?.div(displayedUnitFactor),
                    speedValue = point.speedMetersPerSec?.div(displayedUnitFactor),
                    axValue = point.axMetersPerSec2?.div(displayedUnitFactor),
                    ayValue = point.ayMetersPerSec2?.div(displayedUnitFactor),
                    accelerationValue = point.accelerationMetersPerSec2?.div(displayedUnitFactor)
                )
            }
        } else {
            trackRawPoints.mapIndexed { index, point ->
                val previous = trackRawPoints.getOrNull(index - 1)
                val previousFrame = previous?.frameNumber
                val frameGap = previousFrame?.let { point.frameNumber - it - 1 } ?: 0
                val deltaT = previous?.let { ((point.timestampUs - it.timestampUs).toDouble() / 1_000_000.0).takeIf { dt -> dt > 0.0 } }
                val dx = previous?.let { point.pixelX - it.pixelX }
                val dy = previous?.let { point.pixelY - it.pixelY }
                val vx = if (deltaT != null && dx != null) dx / deltaT else null
                val vy = if (deltaT != null && dy != null) dy / deltaT else null
                val speed = if (vx != null && vy != null) sqrt(vx * vx + vy * vy) else null
                DisplayKinematicsPoint(
                    pointId = point.id,
                    trackLabel = trackLabel,
                    frameNumber = point.frameNumber,
                    timestampUs = point.timestampUs,
                    xValue = point.pixelX,
                    yValue = point.pixelY,
                    gapLabel = if (frameGap > 0) "+$frameGap f" else null,
                    vxValue = vx,
                    vyValue = vy,
                    speedValue = speed,
                    axValue = null,
                    ayValue = null,
                    accelerationValue = null
                )
            }
        }
    }
    val displayTablePoints = remember(kinematicsTrackPoints, filteredTrackPoints, calibration, displayedUnitFactor, activeTrackPointIds, frameDurationMs) {
        buildDisplayPointsForTrack(activeTrackLabel)
    }
    val trackADisplayPoints = remember(kinematicsTrackPoints, calibration, displayedUnitFactor, frameDurationMs, trackPoints) {
        buildDisplayPointsForTrack("A")
    }
    val trackBDisplayPoints = remember(kinematicsTrackPoints, calibration, displayedUnitFactor, frameDurationMs, trackPoints) {
        buildDisplayPointsForTrack("B")
    }
    val combinedTablePoints = remember(trackADisplayPoints, trackBDisplayPoints) {
        buildList {
            addAll(trackADisplayPoints)
            addAll(trackBDisplayPoints)
        }.sortedWith(compareBy<DisplayKinematicsPoint> { it.timestampUs }.thenBy { it.trackLabel }.thenBy { it.frameNumber })
    }
    val selectedTableSourcePoints = remember(twoTrackMode, showTableTrackA, showTableTrackB, displayTablePoints, trackADisplayPoints, trackBDisplayPoints, combinedTablePoints) {
        when {
            !twoTrackMode -> displayTablePoints
            showTableTrackA && showTableTrackB -> combinedTablePoints
            showTableTrackB -> trackBDisplayPoints
            else -> trackADisplayPoints
        }
    }
    val selectedGraphSourcePoints = remember(
        twoTrackMode,
        activeTrack,
        showGraphTrackA,
        showGraphTrackB,
        displayTablePoints,
        trackADisplayPoints,
        trackBDisplayPoints
    ) {
        when {
            !twoTrackMode -> displayTablePoints
            showGraphTrackA && !showGraphTrackB -> trackADisplayPoints
            showGraphTrackB && !showGraphTrackA -> trackBDisplayPoints
            else -> if (activeTrack == ActiveTrack.B) trackBDisplayPoints else trackADisplayPoints
        }
    }
    val graphRevealCutoffMs = remember(dataMode, dataPanelMode, isPlaying, absoluteCurrentPos, liveGraphCutoffMs) {
        if (dataMode && dataPanelMode == DataPanelMode.GRAPH) {
            if (isPlaying) absoluteCurrentPos else liveGraphCutoffMs
        } else null
    }
    val visibleTablePoints = remember(selectedTableSourcePoints, absoluteCurrentPos, dataMode, dataPanelMode, isPlaying, graphRevealCutoffMs) {
        val revealCutoffMs = graphRevealCutoffMs
        if (!dataMode || (revealCutoffMs == null && !isPlaying)) {
            selectedTableSourcePoints
        } else {
            val cutoffMs = revealCutoffMs ?: absoluteCurrentPos
            val revealed = selectedTableSourcePoints.filter { (it.timestampUs / 1000) <= cutoffMs }
            if (revealed.isNotEmpty()) revealed else selectedTableSourcePoints.take(1)
        }
    }
    val visibleTwoBodySamples = remember(twoBodySamples, absoluteCurrentPos, dataMode, dataPanelMode, isPlaying, graphRevealCutoffMs) {
        val revealCutoffMs = graphRevealCutoffMs
        if (!dataMode || (revealCutoffMs == null && !isPlaying)) {
            twoBodySamples
        } else {
            val cutoffMs = revealCutoffMs ?: absoluteCurrentPos
            val revealed = twoBodySamples.filter { (it.timestampUs / 1000) <= cutoffMs }
            if (revealed.isNotEmpty()) revealed else twoBodySamples.take(1)
        }
    }
    val visibleGraphSourcePoints = remember(selectedGraphSourcePoints, absoluteCurrentPos, dataMode, dataPanelMode, isPlaying, graphRevealCutoffMs) {
        val revealCutoffMs = graphRevealCutoffMs
        if (!dataMode || (revealCutoffMs == null && !isPlaying)) {
            selectedGraphSourcePoints
        } else {
            val cutoffMs = revealCutoffMs ?: absoluteCurrentPos
            val revealed = selectedGraphSourcePoints.filter { (it.timestampUs / 1000) <= cutoffMs }
            if (revealed.isNotEmpty()) revealed else selectedGraphSourcePoints.take(1)
        }
    }
    val selectedGraphPlotPoints = remember(
        visibleGraphSourcePoints,
        visibleTwoBodySamples,
        selectedGraphSeries,
        selectedGraphSeriesX,
        displayedSeparationUnitFactor
    ) {
        buildGraphPlotPoints(
            displayPoints = visibleGraphSourcePoints,
            twoBodySamples = if (twoTrackMode) visibleTwoBodySamples else emptyList(),
            series = selectedGraphSeries,
            separationUnitFactor = displayedSeparationUnitFactor,
            xSeries = selectedGraphSeriesX
        )
    }
    val totalGraphPointCount = remember(
        selectedGraphSourcePoints,
        twoBodySamples,
        selectedGraphSeries,
        selectedGraphSeriesX,
        displayedSeparationUnitFactor
    ) {
        buildGraphPlotPoints(
            displayPoints = selectedGraphSourcePoints,
            twoBodySamples = if (twoTrackMode) twoBodySamples else emptyList(),
            series = selectedGraphSeries,
            separationUnitFactor = displayedSeparationUnitFactor,
            xSeries = selectedGraphSeriesX
        ).size
    }
    val compareTrackAGraphPoints = remember(trackADisplayPoints, selectedGraphSeries, selectedGraphSeriesX, displayedSeparationUnitFactor) {
        buildGraphPlotPoints(
            displayPoints = trackADisplayPoints,
            twoBodySamples = emptyList(),
            series = selectedGraphSeries,
            separationUnitFactor = displayedSeparationUnitFactor,
            xSeries = selectedGraphSeriesX
        )
    }
    val compareTrackBGraphPoints = remember(trackBDisplayPoints, selectedGraphSeries, selectedGraphSeriesX, displayedSeparationUnitFactor) {
        buildGraphPlotPoints(
            displayPoints = trackBDisplayPoints,
            twoBodySamples = emptyList(),
            series = selectedGraphSeries,
            separationUnitFactor = displayedSeparationUnitFactor,
            xSeries = selectedGraphSeriesX
        )
    }
    val showLandscapeLiveGraph = remember(
        isLandscape,
        videoFocusMode,
        trackMode,
        lightMode
    ) {
        isLandscape &&
            !videoFocusMode &&
            trackMode &&
            !lightMode
    }
    val useLandscapeSplit = isLandscape && !videoFocusMode
    val primaryGraphTrackColor = remember(twoTrackMode, activeTrack, showGraphTrackA, showGraphTrackB, trackAColorArgb, trackBColorArgb) {
        when {
            !twoTrackMode -> overlayColorFromArgb(trackAColorArgb)
            showGraphTrackA && !showGraphTrackB -> overlayColorFromArgb(trackAColorArgb)
            showGraphTrackB && !showGraphTrackA -> overlayColorFromArgb(trackBColorArgb)
            activeTrack == ActiveTrack.B -> overlayColorFromArgb(trackBColorArgb)
            else -> overlayColorFromArgb(trackAColorArgb)
        }
    }
    val selectedGraphSamples = remember(selectedGraphPlotPoints) {
        selectedGraphPlotPoints.map {
            GraphSample(
                timeSeconds = it.timeSeconds.toDouble(),
                value = it.value.toDouble()
            )
        }
    }
    val selectedGraphPointId = remember(selectedGraphPlotPoints, selectedGraphSampleIndex) {
        selectedGraphPlotPoints.getOrNull(selectedGraphSampleIndex)?.detailPoint?.pointId
    }
    val closestApproachGraphSampleIndex = remember(selectedGraphPlotPoints, closestApproachSample, selectedGraphSeries) {
        if (selectedGraphSeries != GraphSeries.SEPARATION && selectedGraphSeries != GraphSeries.RELATIVE_SPEED) {
            -1
        } else {
            val frameNumber = closestApproachSample?.frameNumber ?: return@remember -1
            selectedGraphPlotPoints.indexOfFirst { it.frameNumber == frameNumber }
        }
    }
    val selectedProfileResult = remember(selectedGraphSamples, graphProfileMode) {
        fitProfile(selectedGraphSamples, graphProfileMode)
    }
    val lightProfileStatsA = remember(lightProfileA, lightDetectPeaks) { buildLightStats(lightProfileA, lightDetectPeaks) }
    val lightProfileStatsB = remember(lightProfileB, lightDetectPeaks) { buildLightStats(lightProfileB, lightDetectPeaks) }
    val lightTimeStatsA = remember(lightTimeSeriesA.toList(), lightDetectPeaks) {
        buildLightStats(lightTimeSeriesA.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }, lightDetectPeaks)
    }
    val lightTimeStatsB = remember(lightTimeSeriesB.toList(), lightDetectPeaks) {
        buildLightStats(lightTimeSeriesB.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }, lightDetectPeaks)
    }
    val activeLightExport = remember(
        lightMode,
        lightWorkspaceTab,
        lightPanelMode,
        lightCompareSource,
        lightChannel,
        lightActiveSlot,
        lightDualMode,
        showLightGraphA,
        showLightGraphB,
        showLightTableA,
        showLightTableB,
        lightGeometryA,
        lightGeometryB,
        lightProfileA,
        lightProfileB,
        lightTimeSeriesA.toList(),
        lightTimeSeriesB.toList(),
        lightDetectPeaks,
        trackAColorArgb,
        trackBColorArgb,
        calibration
    ) {
        val exportMode = when (lightPanelMode) {
            LightPanelMode.PROFILE -> "Profile"
            LightPanelMode.TIME -> "Time"
            LightPanelMode.COMPARE -> "Compare ${lightCompareSource.name.lowercase().replaceFirstChar { it.uppercase() }}"
        }
        val profileSeriesA = lightProfileA
        val profileSeriesB = lightProfileB
        val timeSeriesA = lightTimeSeriesA.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }
        val timeSeriesB = lightTimeSeriesB.map { LightSamplePoint(it.timestampUs / 1_000_000.0, it.intensity) }
        val modeSeriesA = when (lightPanelMode) {
            LightPanelMode.PROFILE -> profileSeriesA
            LightPanelMode.TIME -> timeSeriesA
            LightPanelMode.COMPARE -> if (lightCompareSource == LightCompareSource.PROFILE) profileSeriesA else timeSeriesA
        }
        val modeSeriesB = when (lightPanelMode) {
            LightPanelMode.PROFILE -> profileSeriesB
            LightPanelMode.TIME -> timeSeriesB
            LightPanelMode.COMPARE -> if (lightCompareSource == LightCompareSource.PROFILE) profileSeriesB else timeSeriesB
        }
        val focusedSeries = when {
            lightPanelMode == LightPanelMode.COMPARE -> if (showLightGraphA || showLightTableA || !lightDualMode) modeSeriesA else modeSeriesB
            lightActiveSlot == LightSlot.A -> modeSeriesA
            else -> modeSeriesB
        }
        val compareSeries = buildList {
            val exportShowA = if (lightWorkspaceTab == LightWorkspaceTab.TABLE) showLightTableA else showLightGraphA
            val exportShowB = if (lightWorkspaceTab == LightWorkspaceTab.TABLE) showLightTableB else showLightGraphB
            val first = modeSeriesA
            val second = modeSeriesB
            if (lightPanelMode == LightPanelMode.COMPARE || (lightDualMode && (exportShowA || exportShowB))) {
                if (exportShowA && first.isNotEmpty()) add(LightExportSeries("A", lightSeriesToGraphSamples(first), trackAColorArgb))
                if (lightDualMode && exportShowB && second.isNotEmpty()) add(LightExportSeries("B", lightSeriesToGraphSamples(second), trackBColorArgb))
            }
        }
        val stats = buildList {
            add("Mode: $exportMode")
            add("Channel: ${lightChannel.name}")
            add("Geometry A: ${lightGeometrySummary(lightGeometryA, calibration)}")
            if (lightDualMode) add("Geometry B: ${lightGeometrySummary(lightGeometryB, calibration)}")
            when (lightPanelMode) {
                LightPanelMode.PROFILE -> {
                    add("A profile mean ${lightProfileStatsA.mean?.let(::formatCalibratedValue) ?: "--"}")
                    if (lightDualMode) add("B profile mean ${lightProfileStatsB.mean?.let(::formatCalibratedValue) ?: "--"}")
                }
                LightPanelMode.TIME -> {
                    add("A time mean ${lightTimeStatsA.mean?.let(::formatCalibratedValue) ?: "--"}")
                    if (lightDualMode) add("B time mean ${lightTimeStatsB.mean?.let(::formatCalibratedValue) ?: "--"}")
                }
                LightPanelMode.COMPARE -> {
                    if (lightCompareSource == LightCompareSource.PROFILE) {
                        add("A profile mean ${lightProfileStatsA.mean?.let(::formatCalibratedValue) ?: "--"}")
                        if (lightDualMode) add("B profile mean ${lightProfileStatsB.mean?.let(::formatCalibratedValue) ?: "--"}")
                    } else {
                        add("A time mean ${lightTimeStatsA.mean?.let(::formatCalibratedValue) ?: "--"}")
                        if (lightDualMode) add("B time mean ${lightTimeStatsB.mean?.let(::formatCalibratedValue) ?: "--"}")
                    }
                }
            }
        }
        val tableRows = buildList {
            val exportShowA = if (lightWorkspaceTab == LightWorkspaceTab.TABLE) showLightTableA else showLightGraphA
            val exportShowB = if (lightWorkspaceTab == LightWorkspaceTab.TABLE) showLightTableB else showLightGraphB
            if (!lightDualMode || exportShowA) {
                modeSeriesA.take(96).forEach { add("A,${exportMode.uppercase(Locale.US)},${formatCalibratedValue(it.axisValue)},${formatCalibratedValue(it.intensity)}") }
            }
            if (lightDualMode && exportShowB) {
                modeSeriesB.take(96).forEach { add("B,${exportMode.uppercase(Locale.US)},${formatCalibratedValue(it.axisValue)},${formatCalibratedValue(it.intensity)}") }
            }
        }
        LightAnalysisExport(
            modeLabel = exportMode,
            channelLabel = lightChannel.name,
            unitLabel = calibration?.calibrationUnit ?: "px",
            geometrySummary = buildString {
                append(lightGeometrySummary(lightGeometryA, calibration))
                if (lightDualMode) {
                    append(" | ")
                    append(lightGeometrySummary(lightGeometryB, calibration))
                }
            },
            focusedSeries = lightSeriesToGraphSamples(focusedSeries),
            compareSeries = compareSeries,
            statsLines = stats,
            tableRows = tableRows
        )
    }
    // lightProfileA/B and lightTimeSeriesA/B are passed directly to FloatingLightPanel
    fun focusClosestApproach() {
        val sample = closestApproachSample ?: return
        val sampleIndex = closestApproachGraphSampleIndex
        if (sampleIndex >= 0) {
            selectedGraphSampleIndex = sampleIndex
        }
        val target = (sample.timestampUs / 1000L - trimStart).coerceAtLeast(0L)
        if (kotlin.math.abs(currentPos - target) > 8L) {
            viewModel.pausePlayback()
            viewModel.seekTo(target)
        }
    }
    fun focusTimestampUs(timestampUs: Long) {
        val target = (timestampUs / 1000L - trimStart).coerceAtLeast(0L)
        if (kotlin.math.abs(currentPos - target) > 8L) {
            viewModel.pausePlayback()
            viewModel.seekTo(target)
        }
    }
    LaunchedEffect(selectedGraphSampleIndex, dataMode, selectedGraphPlotPoints) {
        if (!dataMode) return@LaunchedEffect
        val sample = selectedGraphPlotPoints.getOrNull(selectedGraphSampleIndex) ?: return@LaunchedEffect
        focusTimestampUs(sample.timestampUs)
    }
    LaunchedEffect(analysisMode, selectedGraphSeries, selectedGraphPlotPoints, absoluteCurrentPos) {
        if (!analysisMode) return@LaunchedEffect
        if (selectedGraphSeries != GraphSeries.SEPARATION && selectedGraphSeries != GraphSeries.RELATIVE_SPEED) return@LaunchedEffect
        if (selectedGraphPlotPoints.isEmpty()) return@LaunchedEffect
        val nearestIndex = selectedGraphPlotPoints.indices.minByOrNull { index ->
            kotlin.math.abs(selectedGraphPlotPoints[index].timestampUs / 1000L - absoluteCurrentPos)
        } ?: return@LaunchedEffect
        if (nearestIndex != selectedGraphSampleIndex) {
            selectedGraphSampleIndex = nearestIndex
        }
    }
    val runExportAction: ((ExportArtifacts) -> String) -> Unit = { onArtifactsReady ->
        val exportPoints = when {
            !twoTrackMode -> displayTablePoints
            dataPanelMode == DataPanelMode.TABLE -> selectedTableSourcePoints
            showGraphTrackA && showGraphTrackB -> combinedTablePoints
            showGraphTrackB -> trackBDisplayPoints
            else -> trackADisplayPoints
        }
        val exportTwoBodySummary = ExportTwoBodySummary(
            currentSeparation = currentTwoBodySample?.separationMeters?.div(displayedSeparationUnitFactor),
            minimumSeparation = minimumSeparation,
            maximumSeparation = maximumSeparation,
            averageSeparation = averageSeparation,
            currentRelativeSpeed = currentRelativeSpeed,
            minimumRelativeSpeed = minimumRelativeSpeed,
            maximumRelativeSpeed = maximumRelativeSpeed,
            averageRelativeSpeed = averageRelativeSpeed,
            frameCount = twoBodySamples.size,
            unit = calibration?.calibrationUnit ?: "m",
            trackAMassKg = trackAMassKg,
            trackBMassKg = trackBMassKg,
            trackAMomentum = trackAMomentum,
            trackBMomentum = trackBMomentum,
            totalMomentum = totalMomentum,
            relativeMomentum = relativeMomentum,
            closestApproachTimeUs = closestApproachSample?.timestampUs,
            closestApproachFrame = closestApproachSample?.frameNumber,
            closestApproachSeparation = closestApproachSample?.separationMeters?.div(displayedSeparationUnitFactor),
            closestApproachRelativeSpeed = closestApproachSample?.relativeSpeedMetersPerSec?.div(displayedSeparationUnitFactor),
            closestApproachMomentumA = closestApproachMomentumA,
            closestApproachMomentumB = closestApproachMomentumB,
            closestApproachTotalMomentum = closestApproachTotalMomentum,
            closestApproachRelativeMomentum = closestApproachRelativeMomentum,
            interactionClass = interactionClassification,
            preInteractionTimeUs = preInteractionSnapshot?.timestampUs,
            preInteractionFrame = preInteractionSnapshot?.frameNumber,
            preInteractionSeparation = preInteractionSnapshot?.separation,
            preInteractionRelativeSpeed = preInteractionSnapshot?.relativeSpeed,
            preInteractionMomentumA = preInteractionSnapshot?.momentumA,
            preInteractionMomentumB = preInteractionSnapshot?.momentumB,
            preInteractionTotalMomentum = preInteractionSnapshot?.totalMomentum,
            preInteractionRelativeMomentum = preInteractionSnapshot?.relativeMomentum,
            preInteractionDeltaUs = preInteractionSnapshot?.deltaFromClosestUs,
            postInteractionTimeUs = postInteractionSnapshot?.timestampUs,
            postInteractionFrame = postInteractionSnapshot?.frameNumber,
            postInteractionSeparation = postInteractionSnapshot?.separation,
            postInteractionRelativeSpeed = postInteractionSnapshot?.relativeSpeed,
            postInteractionMomentumA = postInteractionSnapshot?.momentumA,
            postInteractionMomentumB = postInteractionSnapshot?.momentumB,
            postInteractionTotalMomentum = postInteractionSnapshot?.totalMomentum,
            postInteractionRelativeMomentum = postInteractionSnapshot?.relativeMomentum,
            postInteractionDeltaUs = postInteractionSnapshot?.deltaFromClosestUs,
            gateAToClosestUs = gateAToClosestUs,
            closestToGateBUs = closestToGateBUs,
            gateAToGateBUs = videoPhotogateDeltaUs,
            momentumUnit = momentumUnitLabel
        ).takeIf { twoTrackMode && exportPoints.any { it.trackLabel == "A" } && exportPoints.any { it.trackLabel == "B" } }
        runCatching {
            val exportTrackAPoints = exportPoints.filter { it.trackLabel == "A" }
            val exportTrackBPoints = exportPoints.filter { it.trackLabel == "B" }
            val exportViewLabel = if (lightMode) {
                "Light ${lightPanelMode.name}"
            } else when (dataPanelMode) {
                DataPanelMode.TABLE -> "Table"
                DataPanelMode.GRAPH -> when {
                    twoTrackMode && showGraphTrackA && showGraphTrackB -> "Graph: A + B"
                    twoTrackMode && showGraphTrackB -> "Graph: B"
                    else -> "Graph: A"
                }
                DataPanelMode.ANALYSIS -> when {
                    twoTrackMode && showGraphTrackA && showGraphTrackB -> "Analysis: A + B"
                    twoTrackMode && showGraphTrackB -> "Analysis: B"
                    else -> "Analysis: A"
                }
            }
            exportExperimentArtifacts(
                context = context,
                projectName = projectName,
                displayedUnit = displayedUnit,
                points = exportPoints,
                trackAPoints = exportTrackAPoints,
                trackBPoints = exportTrackBPoints,
                calibration = calibration,
                selectedSeries = selectedGraphSeries,
                graphViewport = graphViewportSummary,
                showTrackA = showGraphTrackA,
                showTrackB = showGraphTrackB,
                showDelta = showGraphDelta,
                activeViewLabel = exportViewLabel,
                trackAColor = trackAColorArgb,
                trackBColor = trackBColorArgb,
                profileResult = selectedProfileResult,
                photogateSummary = ExportPhotogateSummary(
                    gateATimestampUs = photogateA.timestampUs,
                    gateAFrame = photogateA.frameNumber,
                    gateBTimestampUs = photogateB.timestampUs,
                    gateBFrame = photogateB.frameNumber,
                    deltaUs = videoPhotogateDeltaUs
                ),
                twoBodySummary = exportTwoBodySummary,
                lightAnalysis = activeLightExport.takeIf { lightMode || lightProfileA.isNotEmpty() || lightProfileB.isNotEmpty() || lightTimeSeriesA.isNotEmpty() || lightTimeSeriesB.isNotEmpty() },
                trimStartMs = trimStart,
                lastCheckpointSavedAt = lastCheckpointSavedAt
            )
        }.onSuccess { artifacts ->
            exportStatus = onArtifactsReady(artifacts)
        }.onFailure { throwable ->
            exportStatus = throwable.message ?: "Export failed"
        }
    }
    val selectedTrackPointPosition = remember(trackPoints, selectedPointId, draggedPointDraft) {
        if (selectedPointId == -1L) null
        else {
            val point = trackPoints.firstOrNull { it.id == selectedPointId } ?: return@remember null
            draggedPointDraft ?: Offset(point.pixelX.toFloat(), point.pixelY.toFloat())
        }
    }
    val selectedTrackPoint = remember(trackPoints, selectedPointId) {
        trackPoints.firstOrNull { it.id == selectedPointId }
    }
    val precisionTargetPosition = remember(pendingSeedScreenPoint, manualPendingPoint, selectedTrackPointPosition) {
        pendingSeedScreenPoint ?: manualPendingPoint ?: selectedTrackPointPosition
    }
    val precisionTargetLabel = remember(pendingSeedScreenPoint, manualPendingPoint, selectedPointId) {
        when {
            pendingSeedScreenPoint != null -> "AUTO SEED"
            manualPendingPoint != null -> "PENDING"
            selectedPointId != -1L -> "SELECTED"
            else -> "NO TARGET"
        }
    }
    val canConfirmPrecisionTarget = pendingSeedScreenPoint != null || manualPendingPoint != null || selectedPointId != -1L
    val precisionTargetPixelPosition = remember(precisionTargetPosition, sourceVideoSize, canvasSize) {
        precisionTargetPosition?.let { target ->
            if (sourceVideoSize.width > 0 && sourceVideoSize.height > 0 && canvasSize != IntSize.Zero) {
                screenToVideoOffsetClamped(
                    target,
                    sourceVideoSize.width,
                    sourceVideoSize.height,
                    canvasSize
                )
            } else {
                target
            }
        }
    }
    suspend fun seedAutoTracker(target: Offset) {
        if (canvasSize == IntSize.Zero) {
            autoTrackStatus = "Video not ready"
            return
        }
        val frameTimeUs = absoluteCurrentPos * 1000L
        val seedResult = withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, Uri.parse(videoUri))
                    val bitmap = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: return@runCatching null
                    val tappedVideoTarget = screenToVideoOffset(target, bitmap.width, bitmap.height, canvasSize) ?: return@runCatching null
                    val refinement = refineSeedPoint(bitmap, tappedVideoTarget)
                    val videoTarget = if (seedLockMode == SeedLockMode.SNAP_OBJECT) refinement.center else tappedVideoTarget
                    val customRadius = refinement.radiusPx.toInt().coerceIn(8, 48)
                    val classicSeed = classicTrackerMode(autoTrackMode)?.let { mode ->
                        ClassicTrackerEngine(mode).seed(bitmap, videoTarget)
                    }
                    val template = classicSeed?.template ?: when (autoTrackMode) {
                        AutoTrackMode.CLASSIC_FAST -> cropTemplateSized(bitmap, videoTarget, autoTrackTemplateSizePx)
                        else -> cropTemplate(bitmap, videoTarget, autoTrackTemplateRadiusPx)
                    } ?: return@runCatching null
                    val deepState = if (autoTrackMode == AutoTrackMode.DEEP_ACCURATE && deepTrackingSupport.isModelReady) {
                        DeepSegmentationTracker(context, deepTrackingSupport).use { tracker ->
                            tracker.seed(bitmap, videoTarget)
                        }
                    } else {
                        null
                    }
                    val mediaPipeState =
                        if (autoTrackMode == AutoTrackMode.MEDIAPIPE_OBJECT && mediaPipeTrackingSupport.isModelReady) {
                            MediaPipeObjectTracker(context, mediaPipeTrackingSupport).use { tracker ->
                                tracker.seed(bitmap, videoTarget, frameTimeUs / 1000L)
                            }
                        } else {
                            null
                        }
                    AutoTrackSeed(
                        template = template,
                        originalTemplate = template,
                        center = mediaPipeState?.center ?: deepState?.center ?: classicSeed?.center ?: videoTarget,
                        velocity = classicSeed?.velocity ?: Offset.Zero,
                        mediaPipeTrackingState = mediaPipeState,
                        deepTrackingState = deepState,
                        meanError = classicSeed?.meanError,
                        searchRadiusPx = classicSeed?.searchRadiusPx ?: autoTrackSearchRadiusPx,
                        confidence = classicSeed?.confidence ?: refinement.confidence,
                        estimatedRadiusPx = classicSeed?.estimatedRadiusPx ?: refinement.radiusPx,
                        trackingSource = when {
                            classicSeed != null -> "locked"
                            else -> "seed"
                        },
                        objectLuma = classicSeed?.objectLuma ?: 0f,
                        backgroundLuma = classicSeed?.backgroundLuma ?: 0f,
                        objectIsBrighter = classicSeed?.objectIsBrighter ?: true,
                        sourceWidth = classicSeed?.sourceWidth ?: bitmap.width,
                        sourceHeight = classicSeed?.sourceHeight ?: bitmap.height
                    )
                }
            }.getOrNull()
        }
        if (seedResult == null) {
            autoTrackStatus = "Tap on the video object"
            return
        }
        autoTrackSeed = seedResult
        autoTrackAwaitingSeed = false
        if (twoTrackMode) saveCurrentTrackAutoState()
        val refinedScreenPoint = videoToScreenOffset(seedResult.center, seedResult.sourceWidth, seedResult.sourceHeight, canvasSize)
        viewModel.addPoint(
            refinedScreenPoint.x.toDouble(),
            refinedScreenPoint.y.toDouble(),
            activeTrack.name,
            activeTrackMassKg()
        )
        trackTrayPanel = TrackTrayPanel.AUTO_RUN
        autoTrackStatus = null
    }

    fun nudgePrecisionTarget(deltaX: Float, deltaY: Float) {
        val maxX = canvasSize.width.toFloat().coerceAtLeast(1f)
        val maxY = canvasSize.height.toFloat().coerceAtLeast(1f)
        fun moveBySourcePixels(base: Offset): Offset {
            if (sourceVideoSize.width <= 0 || sourceVideoSize.height <= 0 || canvasSize == IntSize.Zero) {
                return Offset(
                    x = (base.x + deltaX).coerceIn(0f, maxX),
                    y = (base.y + deltaY).coerceIn(0f, maxY)
                )
            }
            val sourcePoint = screenToVideoOffsetClamped(
                base,
                sourceVideoSize.width,
                sourceVideoSize.height,
                canvasSize
            )
            val movedSourcePoint = Offset(
                x = (sourcePoint.x + deltaX).coerceIn(0f, (sourceVideoSize.width - 1).toFloat()),
                y = (sourcePoint.y + deltaY).coerceIn(0f, (sourceVideoSize.height - 1).toFloat())
            )
            return videoToScreenOffset(
                movedSourcePoint,
                sourceVideoSize.width,
                sourceVideoSize.height,
                canvasSize
            )
        }
        showMagnifier = true
        when {
            pendingSeedScreenPoint != null -> {
                pendingSeedScreenPoint = moveBySourcePixels(pendingSeedScreenPoint ?: return)
                if (twoTrackMode) saveCurrentTrackAutoState()
                autoTrackStatus = null
            }
            manualPendingPoint != null && selectedPointId == -1L -> {
                manualPendingPoint = moveBySourcePixels(manualPendingPoint ?: return)
            }
            selectedPointId != -1L -> {
                val point = trackPoints.firstOrNull { it.id == selectedPointId } ?: return
                val base = draggedPointDraft ?: Offset(point.pixelX.toFloat(), point.pixelY.toFloat())
                val updated = moveBySourcePixels(base)
                draggedPointDraft = updated
                viewModel.updatePoint(point, updated.x.toDouble(), updated.y.toDouble())
            }
        }
    }

    fun confirmPrecisionTarget() {
        when {
            pendingSeedScreenPoint != null -> {
                val seedPoint = pendingSeedScreenPoint ?: return
                autoTrackSelectionMode = false
                autoTrackAwaitingSeed = false
                autoTrackStatus = "Analyzing seed"
                scope.launch {
                    seedAutoTracker(seedPoint)
                    pendingSeedScreenPoint = null
                }
            }
            manualPendingPoint != null || selectedPointId != -1L -> confirmManualSelection()
        }
    }
    suspend fun advanceAutoSeedOnBitmap(
        seed: AutoTrackSeed,
        bitmap: Bitmap,
        localPositionMs: Long,
        deepTracker: DeepSegmentationTracker?,
        mediaPipeTracker: MediaPipeObjectTracker?,
        classicEngine: ClassicTrackerEngine?
    ): AutoTrackBitmapStepResult? {
        if (autoTrackMode == AutoTrackMode.DEEP_ACCURATE) {
            val deepState = seed.deepTrackingState ?: return null
            val deepMatch = deepTracker?.track(bitmap, deepState) ?: return null
            val updatedSeed = seed.copy(
                center = deepMatch.state.center,
                velocity = deepMatch.state.velocity,
                deepTrackingState = deepMatch.state,
                meanError = 1.0 - deepMatch.confidence,
                searchRadiusPx = autoTrackSearchRadiusPx,
                confidence = deepMatch.confidence,
                estimatedRadiusPx = seed.estimatedRadiusPx,
                trackingSource = "deep",
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height
            )
            return AutoTrackBitmapStepResult(
                seed = updatedSeed,
                screenPoint = videoToScreenOffset(updatedSeed.center, bitmap.width, bitmap.height, canvasSize),
                statusText = "conf ${formatCalibratedValue(deepMatch.confidence)}"
            )
        }
        if (autoTrackMode == AutoTrackMode.MEDIAPIPE_OBJECT) {
            val mediaPipeState = seed.mediaPipeTrackingState ?: return null
            val mediaPipeMatch = mediaPipeTracker?.track(bitmap, mediaPipeState, trimStart + localPositionMs) ?: return null
            val updatedSeed = seed.copy(
                center = mediaPipeMatch.state.center,
                velocity = mediaPipeMatch.state.velocity,
                mediaPipeTrackingState = mediaPipeMatch.state,
                meanError = 1.0 - mediaPipeMatch.confidence,
                searchRadiusPx = autoTrackSearchRadiusPx,
                confidence = mediaPipeMatch.confidence,
                estimatedRadiusPx = seed.estimatedRadiusPx,
                trackingSource = "mediapipe",
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height
            )
            val label = mediaPipeMatch.state.label ?: "object"
            return AutoTrackBitmapStepResult(
                seed = updatedSeed,
                screenPoint = videoToScreenOffset(updatedSeed.center, bitmap.width, bitmap.height, canvasSize),
                statusText = "$label ${formatCalibratedValue(mediaPipeMatch.confidence)}"
            )
        }
        val classicUpdate = withContext(Dispatchers.Default) {
            classicEngine?.trackFrame(bitmap, seed.toClassicSeed())
        } ?: return null
        val nextClassicSeed = classicUpdate.seed
        val updatedSeed = seed.copy(
            template = nextClassicSeed.template,
            originalTemplate = seed.originalTemplate ?: seed.template,
            center = nextClassicSeed.center,
            velocity = nextClassicSeed.velocity,
            meanError = nextClassicSeed.meanError,
            searchRadiusPx = nextClassicSeed.searchRadiusPx,
            confidence = classicUpdate.confidence,
            estimatedRadiusPx = classicUpdate.estimatedRadiusPx,
            trackingSource = classicUpdate.trackingSource,
            objectLuma = nextClassicSeed.objectLuma,
            backgroundLuma = nextClassicSeed.backgroundLuma,
            objectIsBrighter = nextClassicSeed.objectIsBrighter,
            sourceWidth = nextClassicSeed.sourceWidth,
            sourceHeight = nextClassicSeed.sourceHeight
        )
        val status = if (autoTrackMode == AutoTrackMode.CLASSIC_FAST) {
            "${classicUpdate.trackingSource} ${autoTrackConfidenceBucket(classicUpdate.confidence)}"
        } else {
            "${classicUpdate.trackingSource} ${autoTrackConfidenceBucket(classicUpdate.confidence)}"
        }
        return AutoTrackBitmapStepResult(
            seed = updatedSeed,
            screenPoint = videoToScreenOffset(updatedSeed.center, bitmap.width, bitmap.height, canvasSize),
            statusText = status
        )
    }
    suspend fun runAutoTrack(frameCount: Int) {
        var seed = autoTrackSeed
        val targetTrackLabel = activeTrack.name
        val targetTrackMassKg = activeTrackMassKg()
        val startPositionMs = viewModel.player.currentPosition
        if (seed == null || canvasSize == IntSize.Zero) {
            autoTrackStatus = "Seed auto-track first"
            return
        }
        if (autoTrackMode == AutoTrackMode.DEEP_ACCURATE) {
            if (!deepTrackingSupport.isModelReady) {
                autoTrackStatus = deepTrackingSupport.unavailableMessage()
                autoTrackRunning = false
                autoTrackAwaitingSeed = true
                if (twoTrackMode) saveCurrentTrackAutoState()
                return
            }
        }
        if (autoTrackMode == AutoTrackMode.MEDIAPIPE_OBJECT) {
            if (!mediaPipeTrackingSupport.isModelReady) {
                autoTrackStatus = mediaPipeTrackingSupport.unavailableMessage()
                autoTrackRunning = false
                autoTrackAwaitingSeed = true
                if (twoTrackMode) saveCurrentTrackAutoState()
                return
            }
        }
        val trackingProfile = autoTrackProfile(autoTrackMode)
        val classicEngine = classicTrackerMode(autoTrackMode)?.let { ClassicTrackerEngine(it) }
        val startFrame = (startPositionMs / frameDurationMs.coerceAtLeast(1L)).toInt()
        val frameStride = trackingStepOption.frameStep.coerceAtLeast(1)
        autoTrackRunning = true
        autoTrackAwaitingSeed = false
        if (twoTrackMode) saveCurrentTrackAutoState()
        liveAutoTrackPreview.clear()
        viewModel.pausePlayback()
        liveAutoTrackPreview += AutoTrackPreviewPoint(
            timestampUs = (trimStart + startPositionMs) * 1000L,
            position = videoToScreenOffset(seed.center, seed.sourceWidth, seed.sourceHeight, canvasSize),
            frameNumber = startFrame,
            trackLabel = targetTrackLabel
        )
        val result = runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, Uri.parse(videoUri))
                var currentSeed = seed ?: return@use Triple<AutoTrackSeed?, Long?, String>(null, null, "Seed auto-track first")
                var trackedFrames = 0
                val endFrameExclusive = startFrame + (frameCount * frameStride)
                val deepTracker = if (autoTrackMode == AutoTrackMode.DEEP_ACCURATE && deepTrackingSupport.isModelReady) {
                    DeepSegmentationTracker(context, deepTrackingSupport)
                } else {
                    null
                }
                val mediaPipeTracker =
                    if (autoTrackMode == AutoTrackMode.MEDIAPIPE_OBJECT && mediaPipeTrackingSupport.isModelReady) {
                        MediaPipeObjectTracker(context, mediaPipeTrackingSupport)
                    } else {
                        null
                    }
                for (frame in (startFrame + frameStride)..endFrameExclusive step frameStride) {
                    if (!autoTrackRunning) break
                    val localPositionMs = frame.toLong() * frameDurationMs.coerceAtLeast(1L)
                    if (localPositionMs > duration) break
                    autoTrackStatus = "${trackingProfile.label} ${trackedFrames + 1}/$frameCount  step ${frameStride}f"
                    val bitmap = withContext(Dispatchers.IO) {
                        retriever.getFrameAtTime((trimStart + localPositionMs) * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                    } ?: return@use Triple<AutoTrackSeed?, Long?, String>(currentSeed, trackedFrames.toLong(), "Lost: frame read failed")
                    if (autoTrackMode == AutoTrackMode.DEEP_ACCURATE) {
                        val deepState = currentSeed.deepTrackingState
                            ?: return@use Triple(currentSeed, trackedFrames.toLong(), "Lost: deep tracker not seeded")
                        val deepMatch = deepTracker?.track(bitmap, deepState)
                            ?: return@use Triple(currentSeed, trackedFrames.toLong(), "Lost: deep segmentation target")
                        val screenPoint = videoToScreenOffset(deepMatch.state.center, bitmap.width, bitmap.height, canvasSize)
                        currentSeed = currentSeed.copy(
                            center = deepMatch.state.center,
                            velocity = deepMatch.state.velocity,
                            deepTrackingState = deepMatch.state,
                            meanError = 1.0 - deepMatch.confidence,
                            searchRadiusPx = autoTrackSearchRadiusPx,
                            confidence = deepMatch.confidence,
                            estimatedRadiusPx = currentSeed.estimatedRadiusPx,
                            trackingSource = "deep",
                            sourceWidth = bitmap.width,
                            sourceHeight = bitmap.height
                        )
                        autoTrackSeed = currentSeed
                        if (twoTrackMode) saveCurrentTrackAutoState()
                        liveAutoTrackPreview += AutoTrackPreviewPoint(
                            timestampUs = (trimStart + localPositionMs) * 1000L,
                            position = screenPoint,
                            frameNumber = frame,
                            trackLabel = targetTrackLabel
                        )
                        withContext(Dispatchers.Main) {
                            viewModel.addPointAtTracked(
                                positionMs = localPositionMs,
                                x = screenPoint.x.toDouble(),
                                y = screenPoint.y.toDouble(),
                                trackLabel = targetTrackLabel,
                                massKg = targetTrackMassKg
                            )
                            viewModel.seekTo(localPositionMs)
                        }
                        delay(trackingStepOption.previewDelayMs)
                        autoTrackStatus =
                            if (trackedFrames == 0 || trackedFrames % 12 == 0) {
                                "${trackingProfile.label} ${trackedFrames + 1}/$frameCount  conf ${formatCalibratedValue(deepMatch.confidence)}"
                            } else {
                                autoTrackStatus
                            }
                        trackedFrames += 1
                        continue
                    }
                    if (autoTrackMode == AutoTrackMode.MEDIAPIPE_OBJECT) {
                        val mediaPipeState = currentSeed.mediaPipeTrackingState
                            ?: return@use Triple(currentSeed, trackedFrames.toLong(), "Lost: MediaPipe tracker not seeded")
                        val mediaPipeMatch = mediaPipeTracker?.track(bitmap, mediaPipeState, trimStart + localPositionMs)
                            ?: return@use Triple(currentSeed, trackedFrames.toLong(), "Lost: MediaPipe object")
                        val screenPoint = videoToScreenOffset(mediaPipeMatch.state.center, bitmap.width, bitmap.height, canvasSize)
                        currentSeed = currentSeed.copy(
                            center = mediaPipeMatch.state.center,
                            velocity = mediaPipeMatch.state.velocity,
                            mediaPipeTrackingState = mediaPipeMatch.state,
                            meanError = 1.0 - mediaPipeMatch.confidence,
                            searchRadiusPx = autoTrackSearchRadiusPx,
                            confidence = mediaPipeMatch.confidence,
                            estimatedRadiusPx = currentSeed.estimatedRadiusPx,
                            trackingSource = "mediapipe",
                            sourceWidth = bitmap.width,
                            sourceHeight = bitmap.height
                        )
                        autoTrackSeed = currentSeed
                        if (twoTrackMode) saveCurrentTrackAutoState()
                        liveAutoTrackPreview += AutoTrackPreviewPoint(
                            timestampUs = (trimStart + localPositionMs) * 1000L,
                            position = screenPoint,
                            frameNumber = frame,
                            trackLabel = targetTrackLabel
                        )
                        withContext(Dispatchers.Main) {
                            viewModel.addPointAtTracked(
                                positionMs = localPositionMs,
                                x = screenPoint.x.toDouble(),
                                y = screenPoint.y.toDouble(),
                                trackLabel = targetTrackLabel,
                                massKg = targetTrackMassKg
                            )
                            viewModel.seekTo(localPositionMs)
                        }
                        delay(trackingStepOption.previewDelayMs)
                        if (trackedFrames == 0 || trackedFrames % 12 == 0) {
                            val label = mediaPipeMatch.state.label ?: "object"
                            autoTrackStatus =
                                "${trackingProfile.label} ${trackedFrames + 1}/$frameCount  $label ${formatCalibratedValue(mediaPipeMatch.confidence)}"
                        }
                        trackedFrames += 1
                        continue
                    }
                    val classicUpdate = withContext(Dispatchers.Default) {
                        classicEngine?.trackFrame(bitmap, currentSeed.toClassicSeed())
                    } ?: return@use Triple(currentSeed, trackedFrames.toLong(), "Lost: no match")
                    val nextClassicSeed = classicUpdate.seed
                    val screenPoint = videoToScreenOffset(nextClassicSeed.center, bitmap.width, bitmap.height, canvasSize)
                    currentSeed = currentSeed.copy(
                        template = nextClassicSeed.template,
                        center = nextClassicSeed.center,
                        velocity = nextClassicSeed.velocity,
                        meanError = nextClassicSeed.meanError,
                        searchRadiusPx = nextClassicSeed.searchRadiusPx,
                        confidence = classicUpdate.confidence,
                        estimatedRadiusPx = classicUpdate.estimatedRadiusPx,
                        trackingSource = classicUpdate.trackingSource,
                        objectLuma = nextClassicSeed.objectLuma,
                        backgroundLuma = nextClassicSeed.backgroundLuma,
                        objectIsBrighter = nextClassicSeed.objectIsBrighter,
                        sourceWidth = nextClassicSeed.sourceWidth,
                        sourceHeight = nextClassicSeed.sourceHeight
                    )
                    autoTrackSeed = currentSeed
                    if (twoTrackMode) saveCurrentTrackAutoState()
                    liveAutoTrackPreview += AutoTrackPreviewPoint(
                        timestampUs = (trimStart + localPositionMs) * 1000L,
                        position = screenPoint,
                        frameNumber = frame,
                        trackLabel = targetTrackLabel
                    )
                    withContext(Dispatchers.Main) {
                        viewModel.addPointAtTracked(
                            positionMs = localPositionMs,
                            x = screenPoint.x.toDouble(),
                            y = screenPoint.y.toDouble(),
                            trackLabel = targetTrackLabel,
                            massKg = targetTrackMassKg
                        )
                        viewModel.seekTo(localPositionMs)
                    }
                    delay(trackingStepOption.previewDelayMs)
                    if (trackedFrames == 0 || trackedFrames % 12 == 0) {
                        autoTrackStatus =
                            if (autoTrackMode == AutoTrackMode.CLASSIC_FAST) {
                                "${trackingProfile.label} ${trackedFrames + 1}/$frameCount  ${classicUpdate.trackingSource} ${autoTrackConfidenceBucket(classicUpdate.confidence)}  radius ${classicUpdate.estimatedRadiusPx.toInt()}px"
                            } else {
                                "${trackingProfile.label} ${trackedFrames + 1}/$frameCount  ${classicUpdate.trackingSource} ${autoTrackConfidenceBucket(classicUpdate.confidence)}  err ${formatCalibratedValue(classicUpdate.errorScore)}  radius ${classicUpdate.estimatedRadiusPx.toInt()}px"
                            }
                    }
                    trackedFrames += 1
                }
                withContext(Dispatchers.Main) {
                    liveAutoTrackPreview.clear()
                }
                deepTracker?.close()
                mediaPipeTracker?.close()
                Triple(currentSeed, trackedFrames.toLong(), null)
            }
        }.getOrElse { Triple(seed, null, "Auto-track failed") }
        seed = result.first
        autoTrackSeed = seed
        if (twoTrackMode) saveCurrentTrackAutoState()
        val trackedFrames = result.second?.toInt() ?: 0
        val failure = result.third
        if (failure != null) {
            autoTrackStatus = if (trackedFrames > 0) "$failure after $trackedFrames frames" else failure
            autoTrackRunning = false
            autoTrackAwaitingSeed = true
            if (twoTrackMode) saveCurrentTrackAutoState()
            liveAutoTrackPreview.clear()
            return
        }
        if (autoTrackRunning) {
            autoTrackStatus = "${trackingProfile.label} complete: $trackedFrames frames"
        }
        autoTrackRunning = false
        if (twoTrackMode) saveCurrentTrackAutoState()
        liveAutoTrackPreview.clear()
    }
    suspend fun runAutoTrackToEnd() {
        val remainingMs = (duration - viewModel.player.currentPosition).coerceAtLeast(0L)
        val frameStride = trackingStepOption.frameStep.coerceAtLeast(1)
        val steps = ((remainingMs / frameDurationMs.coerceAtLeast(1L)).toInt() / frameStride).coerceAtLeast(0)
        if (steps <= 0) {
            autoTrackStatus = "Reached end of clip"
            return
        }
        runAutoTrack(steps)
    }
    suspend fun runPreparedAutoTracks(frameCount: Int) {
        val preparedTracks = runEligibleAutoTracks()
        if (preparedTracks.isEmpty()) {
            autoTrackStatus = "Confirm A or B first"
            return
        }
        val originalTrack = activeTrack
        val originalPanel = trackTrayPanel
        if (!twoTrackMode) {
            runAutoTrack(frameCount)
            return
        }
        val startPositionMs = viewModel.player.currentPosition
        if (preparedTracks.size == 1) {
            activeTrack = preparedTracks.first()
            loadCurrentTrackAutoState()
            runAutoTrack(frameCount)
            saveCurrentTrackAutoState()
            activeTrack = originalTrack
            loadCurrentTrackAutoState()
            trackTrayPanel = originalPanel ?: TrackTrayPanel.AUTO_RUN
            return
        }
        var failure: String? = null
        var completedTracks = 0
        for (track in preparedTracks) {
            if (failure != null) break
            activeTrack = track
            loadCurrentTrackAutoState()
            withContext(Dispatchers.Main) {
                viewModel.seekTo(startPositionMs)
            }
            liveAutoTrackPreview.clear()
            autoTrackStatus = "Tracking ${track.name}..."
            runAutoTrack(frameCount)
            saveCurrentTrackAutoState()
            if (autoTrackAwaitingSeed && !autoTrackRunning) {
                failure = autoTrackStatus ?: "Auto-track failed for ${track.name}"
            } else {
                completedTracks += 1
            }
        }
        activeTrack = originalTrack
        loadCurrentTrackAutoState()
        trackTrayPanel = originalPanel ?: TrackTrayPanel.AUTO_RUN
        liveAutoTrackPreview.clear()
        autoTrackRunning = false
        autoTrackStatus = when {
            failure != null && completedTracks > 0 -> "$failure after $completedTracks track(s)"
            failure != null -> failure
            completedTracks > 1 -> "A and B tracked separately"
            completedTracks == 1 -> "${preparedTracks.first().name} tracked"
            else -> "Confirm A or B first"
        }
    }
    suspend fun runPreparedAutoTracksToEnd() {
        val remainingMs = (duration - viewModel.player.currentPosition).coerceAtLeast(0L)
        val frameStride = trackingStepOption.frameStep.coerceAtLeast(1)
        val steps = ((remainingMs / frameDurationMs.coerceAtLeast(1L)).toInt() / frameStride).coerceAtLeast(0)
        if (steps <= 0) {
            autoTrackStatus = "Reached end of clip"
            return
        }
        runPreparedAutoTracks(steps)
    }
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(viewModel.player) {
        fun updateSourceSize(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                sourceVideoSize = IntSize(videoSize.width, videoSize.height)
            }
        }
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                updateSourceSize(videoSize)
            }
        }
        viewModel.player.addListener(listener)
        updateSourceSize(viewModel.player.videoSize)
        onDispose { viewModel.player.removeListener(listener) }
    }
    var magnifierBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val magnifierFocusPoint = pendingSeedScreenPoint ?: manualPendingPoint ?: selectedTrackPointPosition
    val shouldShowCustomMagnifier = trackMode && magnifierFocusPoint != null && (showMagnifier || autoTrackSelectionMode)
    val magnifierRadiusPx = when {
        magnifierZoom >= 20f -> 4
        magnifierZoom >= 10f -> 6
        magnifierZoom >= 5f -> 10
        else -> 16
    }

    LaunchedEffect(shouldShowCustomMagnifier, magnifierFocusPoint, playerViewRef, magnifierZoom, canvasSize, videoScale, videoOffsetX, videoOffsetY) {
        if (!shouldShowCustomMagnifier) {
            magnifierBitmap = null
            return@LaunchedEffect
        }
        val currentActivity = activity ?: run {
            magnifierBitmap = null
            return@LaunchedEffect
        }
        val currentPlayerView = playerViewRef ?: run {
            magnifierBitmap = null
            return@LaunchedEffect
        }
        while (shouldShowCustomMagnifier) {
            val focus = magnifierFocusPoint ?: break
            val captured = capturePlayerBitmap(currentActivity, currentPlayerView)
            captured?.let { bitmap ->
                val mappedFocus = screenToVideoOffsetClamped(focus, bitmap.width, bitmap.height, canvasSize)
                magnifierBitmap = buildMagnifierBitmap(
                    source = bitmap,
                    focus = mappedFocus,
                    radiusPx = magnifierRadiusPx,
                    outputSizePx = 260
                )
            }
            delay(33)
        }
    }

    LaunchedEffect(
        lightMode,
        playerViewRef,
        canvasSize,
        absoluteCurrentPos,
        lightGeometryA,
        lightGeometryB,
        lightGeometryC,
        lightGeometryD,
        lightTool,
        lightChannel,
        lightBackgroundSubtract,
        lightSmooth
    ) {
        if (!lightMode) return@LaunchedEffect
        val currentActivity = activity ?: return@LaunchedEffect
        val currentPlayerView = playerViewRef ?: return@LaunchedEffect
        if (canvasSize.width <= 1 || canvasSize.height <= 1) return@LaunchedEffect
        val geometryA = lightGeometryA ?: lightDefaultGeometry(lightTool, canvasSize, LightSlot.A).also { lightGeometryA = it }
        val geometryB = lightGeometryB ?: lightDefaultGeometry(lightTool, canvasSize, LightSlot.B).also { lightGeometryB = it }
        val geometryC = lightGeometryC ?: lightDefaultGeometry(lightTool, canvasSize, LightSlot.C).also { lightGeometryC = it }
        val geometryD = lightGeometryD ?: lightDefaultGeometry(lightTool, canvasSize, LightSlot.D).also { lightGeometryD = it }
        val captured = capturePlayerBitmap(currentActivity, currentPlayerView) ?: return@LaunchedEffect
        lightProfileA = buildLightProfile(
            bitmap = captured,
            geometry = geometryA,
            channel = lightChannel,
            canvasSize = canvasSize,
            calibration = calibration,
            backgroundSubtract = lightBackgroundSubtract,
            smoothing = lightSmooth
        )
        lightProfileB = buildLightProfile(
            bitmap = captured,
            geometry = geometryB,
            channel = lightChannel,
            canvasSize = canvasSize,
            calibration = calibration,
            backgroundSubtract = lightBackgroundSubtract,
            smoothing = lightSmooth
        )
        lightProfileC = buildLightProfile(
            bitmap = captured,
            geometry = geometryC,
            channel = lightChannel,
            canvasSize = canvasSize,
            calibration = calibration,
            backgroundSubtract = lightBackgroundSubtract,
            smoothing = lightSmooth
        )
        lightProfileD = buildLightProfile(
            bitmap = captured,
            geometry = geometryD,
            channel = lightChannel,
            canvasSize = canvasSize,
            calibration = calibration,
            backgroundSubtract = lightBackgroundSubtract,
            smoothing = lightSmooth
        )
        val currentTimestampUs = absoluteCurrentPos * 1000L
        if (lightTimeRecording) {
            sampleLightIntensityValue(
                bitmap = captured,
                geometry = geometryA,
                channel = lightChannel,
                canvasSize = canvasSize,
                backgroundSubtract = lightBackgroundSubtract
            )?.let { value ->
                updateLightTimeSeries(lightTimeSeriesA, currentTimestampUs, value)
            }
            if (lightDualMode) {
                sampleLightIntensityValue(
                    bitmap = captured,
                    geometry = geometryB,
                    channel = lightChannel,
                    canvasSize = canvasSize,
                    backgroundSubtract = lightBackgroundSubtract
                )?.let { value ->
                    updateLightTimeSeries(lightTimeSeriesB, currentTimestampUs, value)
                }
                sampleLightIntensityValue(
                    bitmap = captured,
                    geometry = geometryC,
                    channel = lightChannel,
                    canvasSize = canvasSize,
                    backgroundSubtract = lightBackgroundSubtract
                )?.let { value ->
                    updateLightTimeSeries(lightTimeSeriesC, currentTimestampUs, value)
                }
                sampleLightIntensityValue(
                    bitmap = captured,
                    geometry = geometryD,
                    channel = lightChannel,
                    canvasSize = canvasSize,
                    backgroundSubtract = lightBackgroundSubtract
                )?.let { value ->
                    updateLightTimeSeries(lightTimeSeriesD, currentTimestampUs, value)
                }
            }
        }
    }

    LaunchedEffect(selectedTrackPoint?.id) {
        val point = selectedTrackPoint ?: return@LaunchedEffect
        val target = (point.timestampUs / 1000L - trimStart).coerceAtLeast(0L)
        if (kotlin.math.abs(currentPos - target) > 8L) {
            viewModel.pausePlayback()
            viewModel.seekTo(target)
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently delete every tracking point in this project.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllPoints()
                        clearTrackingWorkspaceState()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("CLEAR ALL", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("CANCEL") } }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Leave Project?") },
            text = { Text("Save before leaving this project?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveProjectNow()
                    showExitConfirm = false
                    onBack()
                }) { Text("SAVE & EXIT") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showExitConfirm = false
                        onBack()
                    }) { Text("EXIT") }
                    TextButton(onClick = { showExitConfirm = false }) { Text("CANCEL") }
                }
            }
        )
    }

    if (showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            title = { Text("Calibration Distance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = calibrationDistanceText,
                        onValueChange = { calibrationDistanceText = it },
                        placeholder = { Text("1.0") },
                        suffix = { Text(calibrationUnit) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        calibrationUnits.forEach { unit ->
                            FilterChip(
                                selected = calibrationUnit == unit.symbol,
                                onClick = { calibrationUnit = unit.symbol },
                                label = { Text(unit.symbol) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val value = calibrationDistanceText.toDoubleOrNull()
                    if (value != null && value > 0.0) {
                        viewModel.updateCalibrationDistance(value, calibrationUnit)
                        showCalibrationDialog = false
                    }
                }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showCalibrationDialog = false }) { Text("CANCEL") }
            }
        )
    }

    LaunchedEffect(videoUri, projectName) {
        viewModel.initializeProject(videoUri, projectName)
    }

    // Keyboard shortcut state
    val keyboardFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { keyboardFocusRequester.requestFocus() }
    }

    PhysicsTrackerWorkspaceTheme {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PhysicsColorBackground)
            .focusRequester(keyboardFocusRequester)
            .focusable()
            .onKeyEvent { event: androidx.compose.ui.input.key.KeyEvent ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft  -> { viewModel.stepFrames(-1); true }
                    Key.DirectionRight -> { viewModel.stepFrames(1);  true }
                    Key.Spacebar       -> { viewModel.togglePlayPause(); true }
                    else               -> false
                }
            }
    ) {
        val videoPaneModifier = if (useLandscapeSplit) {
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.55f)
                .align(Alignment.CenterStart)
        } else {
            Modifier.fillMaxSize()
        }
        val sharedVideoModifier = videoPaneModifier
            .graphicsLayer {
                scaleX = videoScale
                scaleY = videoScale
                translationX = videoOffsetX
                translationY = videoOffsetY
            }
            .pointerInput(videoZoomEnabled, videoScale) {
                if (videoZoomEnabled) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (videoScale * zoom).coerceIn(1f, 6f)
                        val maxPanX = (size.width * (newScale - 1f) * 0.5f).coerceAtLeast(0f)
                        val maxPanY = (size.height * (newScale - 1f) * 0.5f).coerceAtLeast(0f)
                        videoScale = newScale
                        videoScaleSlider = newScale
                        videoOffsetX = (videoOffsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                        videoOffsetY = (videoOffsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                        clampVideoPan(newScale)
                    }
                }
            }

        // Determine if we should show the video layer.
        // Hide the video during pure GRAPH/ANALYSIS mode to give graphs maximum screen space.
        val hideVideoForGraph = dataMode && dataPanelMode != DataPanelMode.TABLE && !lightMode && !graphFullScreen

        // 1. VIDEO LAYER
        if (!hideVideoForGraph) {
            AndroidView(
                factory = { context ->
                    val view = LayoutInflater.from(context).inflate(R.layout.physics_player_view, null) as PlayerView
                    view.apply {
                        player = viewModel.player
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        playerViewRef = this
                    }
                },
                update = { playerViewRef = it },
                modifier = sharedVideoModifier
            )
        }

        // 2. TRACKING OVERLAY
        Canvas(
            modifier = sharedVideoModifier
                .onSizeChanged { canvasSize = it }
                .pointerInput(
                    calibrationMode,
                    trackPoints,
                    lightMode,
                    lightWorkspaceTab,
                    lightTool,
                    trackMode,
                    autoTrackEnabled,
                    autoTrackSelectionMode,
                    autoTrackAwaitingSeed,
                    analysisMode,
                    disableExistingPointSelection,
                    videoScale,
                    videoOffsetX,
                    videoOffsetY
                ) {
                    detectTapGestures(
                        onTap = { offset ->
                            val overlayOffset = inverseVideoTransformOffset(
                                touch = offset,
                                canvasSize = canvasSize,
                                scale = videoScale,
                                translationX = videoOffsetX,
                                translationY = videoOffsetY
                            )
                            if (calibrationMode) return@detectTapGestures
                            if (lightMode) {
                                if (lightWorkspaceTab == LightWorkspaceTab.SELECT) {
                                    val geometry = ensureLightGeometry(lightActiveSlot)
                                    val nextGeometry = when (lightTool) {
                                        LightTool.POINT -> LightGeometry(LightTool.POINT, overlayOffset, overlayOffset)
                                        LightTool.LINE -> LightGeometry(LightTool.LINE, overlayOffset - Offset(80f, 0f), overlayOffset + Offset(80f, 0f))
                                        LightTool.BOX -> LightGeometry(LightTool.BOX, overlayOffset - Offset(70f, 50f), overlayOffset + Offset(70f, 50f))
                                    }
                                    setLightGeometry(lightActiveSlot, if (geometry.tool == lightTool) geometry.copy(start = nextGeometry.start, end = nextGeometry.end, tool = lightTool) else nextGeometry)
                                }
                                return@detectTapGestures
                            }
                            if (analysisMode && (
                                    isInsidePhotogateHandle(overlayOffset, gateAX, density) ||
                                        isInsidePhotogateHandle(overlayOffset, gateBX, density)
                                )
                            ) return@detectTapGestures
                            if (trackMode && autoTrackEnabled) {
                                if (autoTrackSelectionMode || autoTrackAwaitingSeed) {
                                    pendingSeedScreenPoint = selectionPointFromTouch(overlayOffset, canvasSize, autoSelectTouchOffsetPx)
                                    if (twoTrackMode) saveCurrentTrackAutoState()
                                }
                                selectedPointId = -1L
                                manualPendingPoint = null
                                return@detectTapGestures
                            }
                            val hit = if (disableExistingPointSelection) null else (
                                selectVisibleEditablePoint(
                                    points = filteredTrackPoints,
                                    tap = overlayOffset,
                                    alternateTap = offset
                                ) ?: selectEditablePointNearFrame(
                                    points = filteredTrackPoints,
                                    tap = overlayOffset,
                                    alternateTap = offset,
                                    currentTimeMs = absoluteCurrentPos,
                                    frameDurationMs = frameDurationMs
                                )
                            )
                            if (hit != null) {
                                selectedPointId = hit.id
                                manualPendingPoint = null
                                manualPendingTouchDelta = null
                                manualDragUsesHandle = false
                            } else {
                                selectedPointId = -1L
                                draggedPointDraft = null
                                draggedPointTouchDelta = null
                                manualPendingPoint = overlayOffset
                                manualPendingTouchDelta = Offset.Zero
                                manualDragUsesHandle = false
                            }
                        },
                        onLongPress = { offset ->
                            val overlayOffset = inverseVideoTransformOffset(
                                touch = offset,
                                canvasSize = canvasSize,
                                scale = videoScale,
                                translationX = videoOffsetX,
                                translationY = videoOffsetY
                            )
                            if (calibrationMode) return@detectTapGestures
                            if (lightMode) {
                                setLightGeometry(lightActiveSlot, lightDefaultGeometry(lightTool, canvasSize, lightActiveSlot).copy(start = overlayOffset, end = overlayOffset))
                                return@detectTapGestures
                            }
                            if (analysisMode && (
                                    isInsidePhotogateHandle(overlayOffset, gateAX, density) ||
                                        isInsidePhotogateHandle(overlayOffset, gateBX, density)
                                )
                            ) return@detectTapGestures
                            if (trackMode && autoTrackEnabled && (autoTrackSelectionMode || autoTrackAwaitingSeed)) {
                                pendingSeedScreenPoint = selectionPointFromTouch(overlayOffset, canvasSize, autoSelectTouchOffsetPx)
                                if (twoTrackMode) saveCurrentTrackAutoState()
                                return@detectTapGestures
                            }
                            if (trackMode && autoTrackEnabled && autoTrackSeed != null && !autoTrackRunning) {
                                scope.launch { runAutoTrackToEnd() }
                                return@detectTapGestures
                            }
                            val hit = if (disableExistingPointSelection) null else (
                                selectVisibleEditablePoint(
                                    points = filteredTrackPoints,
                                    tap = overlayOffset,
                                    alternateTap = offset
                                ) ?: selectEditablePointNearFrame(
                                    points = filteredTrackPoints,
                                    tap = overlayOffset,
                                    alternateTap = offset,
                                    currentTimeMs = absoluteCurrentPos,
                                    frameDurationMs = frameDurationMs
                                )
                            )
                            if (hit != null) {
                                selectedPointId = hit.id
                                confirmManualSelection()
                            } else {
                                selectedPointId = -1L
                                manualPendingPoint = overlayOffset
                                manualPendingTouchDelta = Offset.Zero
                                confirmManualSelection()
                            }
                        },
                        onDoubleTap = { offset ->
                            if (!calibrationMode) return@detectTapGestures
                            val currentCalibration = displayedCalibration ?: return@detectTapGestures
                            val start = Offset(currentCalibration.stickP1x.toFloat(), currentCalibration.stickP1y.toFloat())
                            val end = Offset(currentCalibration.stickP2x.toFloat(), currentCalibration.stickP2y.toFloat())
                            if (calibrationTool == CalibrationTool.SCALE &&
                                isInsideCalibrationLabel(offset, start, end, density * calibrationGuideScale)) {
                                showCalibrationDialog = true
                            }
                        }
                    )
                }
                .pointerInput(
                    selectedPointId,
                    calibrationMode,
                    calibration,
                    canvasSize,
                    lightMode,
                    lightWorkspaceTab,
                    lightTool,
                    lightActiveSlot,
                    trackMode,
                    autoTrackEnabled,
                    autoTrackSelectionMode,
                    autoTrackAwaitingSeed,
                    analysisMode,
                    videoScale,
                    videoOffsetX,
                    videoOffsetY
                ) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val overlayOffset = inverseVideoTransformOffset(
                                touch = offset,
                                canvasSize = canvasSize,
                                scale = videoScale,
                                translationX = videoOffsetX,
                                translationY = videoOffsetY
                            )
                            if (lightMode) {
                                if (lightWorkspaceTab != LightWorkspaceTab.SELECT) {
                                    lightDragSlot = null
                                    lightDragHandle = null
                                    return@detectDragGestures
                                }
                                val slot = lightActiveSlot
                                val geometry = ensureLightGeometry(slot)
                                lightDragSlot = slot
                                lightDragHandle = lightGeometryHitHandle(geometry, overlayOffset, density)
                                if (lightDragHandle == null) {
                                    val bodyFallback = when (geometry.tool) {
                                        LightTool.POINT -> distanceBetween(overlayOffset, geometry.start) <= 64f * density
                                        LightTool.LINE -> lightDistanceToSegment(overlayOffset, geometry.start, geometry.end) <= 48f * density
                                        LightTool.BOX -> {
                                            val left = minOf(geometry.start.x, geometry.end.x) - 20f * density
                                            val right = maxOf(geometry.start.x, geometry.end.x) + 20f * density
                                            val top = minOf(geometry.start.y, geometry.end.y) - 20f * density
                                            val bottom = maxOf(geometry.start.y, geometry.end.y) + 20f * density
                                            overlayOffset.x in left..right && overlayOffset.y in top..bottom
                                        }
                                    }
                                    if (bodyFallback) {
                                        lightDragHandle = LightDragHandle.BODY
                                    }
                                }
                                return@detectDragGestures
                            }
                            if (trackMode && autoTrackEnabled && (autoTrackSelectionMode || autoTrackAwaitingSeed)) {
                                val resolvedPoint = pendingSeedScreenPoint
                                    ?: selectionPointFromTouch(overlayOffset, canvasSize, autoSelectTouchOffsetPx)
                                pendingSeedScreenPoint = resolvedPoint
                                val handleCenter = autoSeedHandleCenter(resolvedPoint, canvasSize, autoSeedHandleLiftPx)
                                val handleHit = distanceBetween(overlayOffset, handleCenter) <= 36f * density
                                val pointHit = distanceBetween(overlayOffset, resolvedPoint) <= 30f * density
                                autoSeedDragUsesHandle = handleHit && !pointHit
                                pendingSeedTouchDelta = resolvedPoint - overlayOffset
                                return@detectDragGestures
                            }
                            if (!autoTrackEnabled && !calibrationMode && selectedPointId == -1L) {
                                manualPendingPoint?.let { pending ->
                                    val handleCenter = manualHandleCenter(pending, canvasSize, manualHandleLiftPx)
                                    val handleHit = distanceBetween(overlayOffset, handleCenter) <= 30f * density
                                    val pointHit = distanceBetween(overlayOffset, pending) <= 28f * density
                                    if (handleHit || pointHit) {
                                        draggedPointDraft = null
                                        draggedPointTouchDelta = null
                                        manualDragUsesHandle = handleHit && !pointHit
                                        manualPendingTouchDelta = if (manualDragUsesHandle) Offset(0f, -manualHandleLiftPx) else Offset.Zero
                                        return@detectDragGestures
                                    }
                                }
                                if (disableExistingPointSelection) {
                                    manualPendingPoint = overlayOffset
                                    manualPendingTouchDelta = Offset.Zero
                                    manualDragUsesHandle = false
                                    return@detectDragGestures
                                }
                                (
                                    selectVisibleEditablePoint(
                                        points = filteredTrackPoints,
                                        tap = overlayOffset,
                                        alternateTap = offset
                                    ) ?: selectEditablePointNearFrame(
                                        points = filteredTrackPoints,
                                        tap = overlayOffset,
                                        alternateTap = offset,
                                        currentTimeMs = absoluteCurrentPos,
                                        frameDurationMs = frameDurationMs
                                    )
                                )?.let { hit ->
                                    selectedPointId = hit.id
                                    val anchor = Offset(hit.pixelX.toFloat(), hit.pixelY.toFloat())
                                    val handleCenter = manualHandleCenter(anchor, canvasSize, manualHandleLiftPx)
                                    draggedPointDraft = anchor
                                    manualDragUsesHandle = distanceBetween(overlayOffset, handleCenter) <= 30f * density &&
                                        distanceBetween(overlayOffset, anchor) > 24f * density
                                    draggedPointTouchDelta = if (manualDragUsesHandle) Offset(0f, -manualHandleLiftPx) else Offset.Zero
                                    return@detectDragGestures
                                }
                                manualPendingPoint = overlayOffset
                                manualPendingTouchDelta = Offset.Zero
                                manualDragUsesHandle = false
                                return@detectDragGestures
                            }
                            draggedPhotogateHandle = if (analysisMode && !calibrationMode) {
                                when {
                                    isInsidePhotogateHandle(overlayOffset, gateAX, density) -> PhotogateHandle.A
                                    isInsidePhotogateHandle(overlayOffset, gateBX, density) -> PhotogateHandle.B
                                    else -> null
                                }
                            } else null
                            if (draggedPhotogateHandle != null) return@detectDragGestures

                            val selectedPoint = visibleOverlayPoints.find { it.id == selectedPointId }
                            draggedPointDraft = selectedPoint?.let {
                                Offset(it.pixelX.toFloat(), it.pixelY.toFloat())
                            }
                            draggedPointTouchDelta = selectedPoint?.let {
                                val anchor = Offset(it.pixelX.toFloat(), it.pixelY.toFloat())
                                val handleCenter = manualHandleCenter(anchor, canvasSize, manualHandleLiftPx)
                                manualDragUsesHandle = distanceBetween(overlayOffset, handleCenter) <= 30f * density &&
                                    distanceBetween(overlayOffset, anchor) > 24f * density
                                if (manualDragUsesHandle) Offset(0f, -manualHandleLiftPx) else Offset.Zero
                            }

                            val currentCalibration = displayedCalibration
                            draggedCalibrationHandle = if (calibrationMode && currentCalibration != null) {
                                val scaleStart = Offset(currentCalibration.stickP1x.toFloat(), currentCalibration.stickP1y.toFloat())
                                val scaleEnd = Offset(currentCalibration.stickP2x.toFloat(), currentCalibration.stickP2y.toFloat())
                                val origin = Offset(currentCalibration.originX.toFloat(), currentCalibration.originY.toFloat())
                                val measurement = measurementStickDraft
                                val activeStart = if (calibrationTool == CalibrationTool.MEASURE) measurement?.start ?: scaleStart else scaleStart
                                val activeEnd = if (calibrationTool == CalibrationTool.MEASURE) measurement?.end ?: scaleEnd else scaleEnd
                                val mid = Offset((activeStart.x + activeEnd.x) / 2f, (activeStart.y + activeEnd.y) / 2f)
                                val rotationHandle = axisEndpoint(origin, currentCalibration.rotationDegrees, 96f * density * calibrationGuideScale)
                                val startDistance = distanceBetween(overlayOffset, activeStart)
                                val endDistance = distanceBetween(overlayOffset, activeEnd)
                                val originDistance = distanceBetween(overlayOffset, origin)
                                val rotationDistance = distanceBetween(overlayOffset, rotationHandle)
                                val bodyDistance = distanceToSegment(overlayOffset, activeStart, activeEnd)
                                val midDistance = distanceBetween(overlayOffset, mid)
                                when {
                                    calibrationTool == CalibrationTool.AXIS &&
                                        originDistance <= 42f * density * calibrationGuideScale -> CalibrationHandle.ORIGIN
                                    calibrationTool == CalibrationTool.AXIS &&
                                        rotationDistance <= 44f * density * calibrationGuideScale -> CalibrationHandle.ROTATE
                                    calibrationTool == CalibrationTool.SCALE || calibrationTool == CalibrationTool.MEASURE -> when {
                                        startDistance <= 64f * density * calibrationGuideScale &&
                                            startDistance <= endDistance -> CalibrationHandle.START
                                        endDistance <= 64f * density * calibrationGuideScale -> CalibrationHandle.END
                                        midDistance <= 80f * density * calibrationGuideScale ||
                                            bodyDistance <= 40f * density * calibrationGuideScale -> CalibrationHandle.BODY
                                        else -> null
                                    }
                                    else -> null
                                }
                            } else null
                        },
                        onDragEnd = {
                            val currentCalibration = calibrationDraft
                            if (lightMode) {
                                lightDragHandle = null
                                lightDragSlot = null
                                return@detectDragGestures
                            }
                            if (trackMode && autoTrackEnabled && autoTrackSelectionMode) {
                                if (twoTrackMode) saveCurrentTrackAutoState()
                            } else if (draggedPhotogateHandle != null) {
                                draggedPhotogateHandle = null
                            } else if (calibrationMode && currentCalibration != null && calibrationTool != CalibrationTool.MEASURE) {
                                viewModel.updateCalibration(currentCalibration)
                            } else if (manualPendingPoint != null && selectedPointId == -1L) {
                                manualPendingTouchDelta = null
                            } else if (selectedPointId != -1L) {
                                val point = trackPoints.find { it.id == selectedPointId }
                                val draft = draggedPointDraft
                                if (point != null && draft != null) {
                                    viewModel.updatePoint(point, draft.x.toDouble(), draft.y.toDouble())
                                }
                            }
                            draggedCalibrationHandle = null
                            draggedPhotogateHandle = null
                            draggedPointDraft = null
                            draggedPointTouchDelta = null
                            manualPendingTouchDelta = null
                            manualDragUsesHandle = false
                            pendingSeedTouchDelta = null
                            autoSeedDragUsesHandle = false
                        },
                        onDragCancel = {
                            lightDragHandle = null
                            lightDragSlot = null
                            draggedCalibrationHandle = null
                            draggedPhotogateHandle = null
                            draggedPointDraft = null
                            draggedPointTouchDelta = null
                            manualPendingTouchDelta = null
                            manualDragUsesHandle = false
                            pendingSeedTouchDelta = null
                            autoSeedDragUsesHandle = false
                            calibrationDraft = calibration
                        }
                    ) { change, dragAmount ->
                        val overlayPosition = inverseVideoTransformOffset(
                            touch = change.position,
                            canvasSize = canvasSize,
                            scale = videoScale,
                            translationX = videoOffsetX,
                            translationY = videoOffsetY
                        )
                        if (lightMode) {
                            if (lightWorkspaceTab != LightWorkspaceTab.SELECT) return@detectDragGestures
                            val dragHandle = lightDragHandle ?: return@detectDragGestures
                            val slot = lightDragSlot ?: lightActiveSlot
                            val geometry = lightGeometryFor(slot) ?: return@detectDragGestures
                            val previousOverlayPosition = inverseVideoTransformOffset(
                                touch = change.previousPosition,
                                canvasSize = canvasSize,
                                scale = videoScale,
                                translationX = videoOffsetX,
                                translationY = videoOffsetY
                            )
                            val overlayDelta = overlayPosition - previousOverlayPosition
                            change.consume()
                            setLightGeometry(slot, moveLightGeometry(geometry, dragHandle, overlayDelta, canvasSize))
                            return@detectDragGestures
                        }
                        val currentCalibration = calibrationDraft ?: displayedCalibration
                        if (trackMode && autoTrackEnabled && (autoTrackSelectionMode || autoTrackAwaitingSeed)) {
                            change.consume()
                            val dragDelta = pendingSeedTouchDelta ?: Offset.Zero
                            val maxX = canvasSize.width.toFloat().coerceAtLeast(1f)
                            val maxY = canvasSize.height.toFloat().coerceAtLeast(1f)
                            pendingSeedScreenPoint = Offset(
                                (overlayPosition.x + dragDelta.x).coerceIn(0f, maxX),
                                (overlayPosition.y + dragDelta.y).coerceIn(0f, maxY)
                            )
                            if (twoTrackMode) saveCurrentTrackAutoState()
                        } else if (draggedPhotogateHandle != null) {
                            change.consume()
                            val fraction = (overlayPosition.x / canvasSize.width.toFloat().coerceAtLeast(1f)).coerceIn(0.05f, 0.95f)
                            if (draggedPhotogateHandle == PhotogateHandle.A) {
                                photogateAFraction = fraction
                            } else {
                                photogateBFraction = fraction
                            }
                        } else if (calibrationMode && currentCalibration != null && draggedCalibrationHandle != null) {
                            change.consume()
                            val maxX = canvasSize.width.toFloat().coerceAtLeast(1f)
                            val maxY = canvasSize.height.toFloat().coerceAtLeast(1f)
                            val start = if (calibrationTool == CalibrationTool.MEASURE) {
                                measurementStickDraft?.start ?: Offset(currentCalibration.stickP1x.toFloat(), currentCalibration.stickP1y.toFloat())
                            } else {
                                Offset(currentCalibration.stickP1x.toFloat(), currentCalibration.stickP1y.toFloat())
                            }
                            val end = if (calibrationTool == CalibrationTool.MEASURE) {
                                measurementStickDraft?.end ?: Offset(currentCalibration.stickP2x.toFloat(), currentCalibration.stickP2y.toFloat())
                            } else {
                                Offset(currentCalibration.stickP2x.toFloat(), currentCalibration.stickP2y.toFloat())
                            }
                            val origin = Offset(currentCalibration.originX.toFloat(), currentCalibration.originY.toFloat())
                            val updatedStart = if (draggedCalibrationHandle == CalibrationHandle.START) {
                                Offset(
                                    (start.x + dragAmount.x).coerceIn(0f, maxX),
                                    (start.y + dragAmount.y).coerceIn(0f, maxY)
                                )
                            } else if (draggedCalibrationHandle == CalibrationHandle.BODY) {
                                Offset(
                                    (start.x + dragAmount.x).coerceIn(0f, maxX),
                                    (start.y + dragAmount.y).coerceIn(0f, maxY)
                                )
                            } else start
                            val updatedEnd = if (draggedCalibrationHandle == CalibrationHandle.END) {
                                Offset(
                                    (end.x + dragAmount.x).coerceIn(0f, maxX),
                                    (end.y + dragAmount.y).coerceIn(0f, maxY)
                                )
                            } else if (draggedCalibrationHandle == CalibrationHandle.BODY) {
                                Offset(
                                    (end.x + dragAmount.x).coerceIn(0f, maxX),
                                    (end.y + dragAmount.y).coerceIn(0f, maxY)
                                )
                            } else end
                            val updatedOrigin = if (draggedCalibrationHandle == CalibrationHandle.ORIGIN) {
                                Offset(
                                    (origin.x + dragAmount.x).coerceIn(0f, maxX),
                                    (origin.y + dragAmount.y).coerceIn(0f, maxY)
                                )
                            } else origin
                            val updatedRotation = if (draggedCalibrationHandle == CalibrationHandle.ROTATE) {
                                angleDegrees(origin, change.position)
                            } else currentCalibration.rotationDegrees
                            if (calibrationTool == CalibrationTool.MEASURE) {
                                measurementStickDraft = MeasurementStickDraft(
                                    start = updatedStart,
                                    end = updatedEnd
                                )
                            } else {
                                calibrationDraft = currentCalibration.copy(
                                    stickP1x = updatedStart.x.toDouble(),
                                    stickP1y = updatedStart.y.toDouble(),
                                    stickP2x = updatedEnd.x.toDouble(),
                                    stickP2y = updatedEnd.y.toDouble(),
                                    originX = updatedOrigin.x.toDouble(),
                                    originY = updatedOrigin.y.toDouble(),
                                    rotationDegrees = updatedRotation
                                )
                            }
                        } else if (selectedPointId != -1L) {
                            change.consume()
                            val maxX = canvasSize.width.toFloat().coerceAtLeast(1f)
                            val maxY = canvasSize.height.toFloat().coerceAtLeast(1f)
                            val dragDelta = draggedPointTouchDelta ?: Offset.Zero
                            draggedPointDraft = Offset(
                                (overlayPosition.x + dragDelta.x).coerceIn(0f, maxX),
                                (overlayPosition.y + dragDelta.y).coerceIn(0f, maxY)
                            )
                        } else if (!autoTrackEnabled && !calibrationMode && manualPendingPoint != null) {
                            change.consume()
                            val maxX = canvasSize.width.toFloat().coerceAtLeast(1f)
                            val maxY = canvasSize.height.toFloat().coerceAtLeast(1f)
                            val dragDelta = manualPendingTouchDelta ?: Offset.Zero
                            manualPendingPoint = Offset(
                                (overlayPosition.x + dragDelta.x).coerceIn(0f, maxX),
                                (overlayPosition.y + dragDelta.y).coerceIn(0f, maxY)
                            )
                        }
                    }
                }
                .background(Color.Transparent)
        ) {
            if (!videoFocusMode) {
            // A. Connecting Path
            val trailPoints = when (pointHistoryMode) {
                PointHistoryMode.ALL -> trackPoints
                PointHistoryMode.FADE -> trackPoints.filter {
                    val ageMs = (absoluteCurrentPos - (it.timestampUs / 1000L)).coerceAtLeast(0L)
                    ageMs <= if (isPlaying) 420L else 1100L
                }
                PointHistoryMode.CURRENT -> emptyList()
            }
            trailPoints
                .groupBy { it.trackLabel }
                .values
                .forEach { trackTrail ->
                    if (trackTrail.size < 2) return@forEach
                    val path = Path()
                    path.moveTo(trackTrail[0].pixelX.toFloat(), trackTrail[0].pixelY.toFloat())
                    for (i in 1 until trackTrail.size) {
                        path.lineTo(trackTrail[i].pixelX.toFloat(), trackTrail[i].pixelY.toFloat())
                    }
                    drawPath(
                        path,
                        overlayColorFromArgb(vectorColorArgb).copy(alpha = if (pointHistoryMode == PointHistoryMode.ALL) 0.24f else 0.1f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            if (trackMode && autoTrackRunning && visibleAutoTrackPreview.isNotEmpty()) {
                visibleAutoTrackPreview.groupBy { it.trackLabel }.forEach { (trackLabel, previews) ->
                    val previewColor = trackColor(trackLabel, trackAColorArgb, trackBColorArgb)
                    if (previews.size > 1) {
                        val livePath = Path()
                        livePath.moveTo(previews.first().position.x, previews.first().position.y)
                        previews.drop(1).forEach { preview ->
                            livePath.lineTo(preview.position.x, preview.position.y)
                        }
                        drawPath(
                            livePath,
                            previewColor.copy(alpha = 0.42f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    val currentPreview = previews.last()
                    previews.forEachIndexed { index, preview ->
                        val ageFromCurrent = (previews.lastIndex - index).coerceAtLeast(0)
                        val alpha = when {
                            ageFromCurrent == 0 -> 1f
                            ageFromCurrent <= 2 -> 0.72f
                            ageFromCurrent <= 5 -> 0.4f
                            ageFromCurrent <= 9 -> 0.2f
                            else -> 0.08f
                        }
                        drawCircle(
                            color = previewColor.copy(alpha = alpha),
                            radius = if (ageFromCurrent == 0) 4.5.dp.toPx() else 2.25.dp.toPx(),
                            center = preview.position
                        )
                    }
                    drawCircle(
                        color = previewColor.copy(alpha = 0.18f),
                        radius = 14.dp.toPx(),
                        center = currentPreview.position,
                        style = Fill
                    )
                    drawCircle(
                        color = previewColor,
                        radius = 6.dp.toPx(),
                        center = currentPreview.position
                    )
                    drawIntoCanvas { canvas ->
                        val labelPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = previewColor.toArgb()
                            textSize = 24f
                            isFakeBoldText = true
                        }
                        canvas.nativeCanvas.drawText(
                            "${trackLabel} F${currentPreview.frameNumber}",
                            currentPreview.position.x + 14.dp.toPx(),
                            currentPreview.position.y - 12.dp.toPx(),
                            labelPaint
                        )
                    }
                }
            }

            

            // B. Points Drawing
            visibleOverlayPoints.forEach { point ->
                val isSelected = point.id == selectedPointId
                val isActive = point.id == activePoint?.id
                val pointTimeMs = point.timestampUs / 1000
                val opacity = pointHistoryAlpha(pointTimeMs, absoluteCurrentPos, isSelected, isActive, pointHistoryMode)
                if (opacity <= 0f) return@forEach

                val draftPosition = if (isSelected) draggedPointDraft else null
                val px = draftPosition?.x ?: point.pixelX.toFloat()
                val py = draftPosition?.y ?: point.pixelY.toFloat()
                val trackBaseColor = trackColor(point.trackLabel, trackAColorArgb, trackBColorArgb)
                val baseColor = if (isSelected) trackBaseColor.copy(red = 0.95f, green = 0.96f, blue = 1f) else if (isActive) trackBaseColor else trackBaseColor.copy(alpha = 0.9f)

                if (isSelected) {
                    drawCircle(color = baseColor.copy(alpha = 0.10f), radius = 14.dp.toPx(), center = Offset(px, py), style = Fill)
                }

                drawCircle(
                    color = baseColor.copy(alpha = opacity),
                    radius = if (isSelected) 4.dp.toPx() else if (isActive) 3.25.dp.toPx() else 2.2.dp.toPx(),
                    center = Offset(px, py)
                )

                drawCircle(
                    color = baseColor.copy(alpha = opacity),
                    radius = if (isSelected) 12.dp.toPx() else if (isActive) 9.dp.toPx() else 4.8.dp.toPx(),
                    center = Offset(px, py),
                    style = Stroke(width = if (isActive || isSelected) 1.25.dp.toPx() else 0.9.dp.toPx())
                )

                if (isSelected) {
                    val handleOffset = 56.dp.toPx()
                    drawLine(baseColor.copy(0.7f), Offset(px, py), Offset(px, py + handleOffset), 1.dp.toPx())
                    drawCircle(baseColor.copy(alpha = 0.10f), 14.dp.toPx(), Offset(px, py + handleOffset), style = Fill)
                    drawCircle(baseColor, 12.dp.toPx(), Offset(px, py + handleOffset), style = Stroke(1.1.dp.toPx()))
                    drawLine(baseColor, Offset(px - 4.dp.toPx(), py + handleOffset), Offset(px + 4.dp.toPx(), py + handleOffset), 1.dp.toPx())
                    drawLine(baseColor, Offset(px, py + handleOffset - 4.dp.toPx()), Offset(px, py + handleOffset + 4.dp.toPx()), 1.dp.toPx())
                }
                if (twoTrackMode) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = baseColor.toArgb()
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 18f
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawText(point.trackLabel, px, py - 10.dp.toPx(), paint)
                    }
                }
            }
            if (trackMode && !autoTrackEnabled && manualPendingPoint != null) {
                val pending = manualPendingPoint ?: Offset.Zero
                val selectorColor = overlayColorFromArgb(manualSelectorColorArgb)
                val centerGap = 3.dp.toPx()
                val handleCenter = manualHandleCenter(pending, canvasSize, manualHandleLiftPx)
                drawCircle(
                    color = selectorColor.copy(alpha = 0.14f),
                    radius = 16.dp.toPx(),
                    center = pending,
                    style = Fill
                )
                drawCircle(
                    color = selectorColor,
                    radius = 9.dp.toPx(),
                    center = pending,
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawLine(
                    selectorColor,
                    Offset(pending.x - 12.dp.toPx(), pending.y),
                    Offset(pending.x - centerGap, pending.y),
                    1.1.dp.toPx()
                )
                drawLine(
                    selectorColor,
                    Offset(pending.x + centerGap, pending.y),
                    Offset(pending.x + 12.dp.toPx(), pending.y),
                    1.1.dp.toPx()
                )
                drawLine(
                    selectorColor,
                    Offset(pending.x, pending.y - 12.dp.toPx()),
                    Offset(pending.x, pending.y - centerGap),
                    1.1.dp.toPx()
                )
                drawLine(
                    selectorColor,
                    Offset(pending.x, pending.y + centerGap),
                    Offset(pending.x, pending.y + 12.dp.toPx()),
                    1.1.dp.toPx()
                )
                drawLine(
                    selectorColor.copy(alpha = 0.72f),
                    pending,
                    handleCenter,
                    1.dp.toPx()
                )
                drawCircle(
                    color = selectorColor.copy(alpha = 0.12f),
                    radius = 14.dp.toPx(),
                    center = handleCenter,
                    style = Fill
                )
                drawCircle(
                    color = selectorColor,
                    radius = 12.dp.toPx(),
                    center = handleCenter,
                    style = Stroke(width = 1.1.dp.toPx())
                )
                drawLine(
                    selectorColor,
                    Offset(handleCenter.x - 5.dp.toPx(), handleCenter.y),
                    Offset(handleCenter.x + 5.dp.toPx(), handleCenter.y),
                    1.dp.toPx()
                )
                drawLine(
                    selectorColor,
                    Offset(handleCenter.x, handleCenter.y - 5.dp.toPx()),
                    Offset(handleCenter.x, handleCenter.y + 5.dp.toPx()),
                    1.dp.toPx()
                )
            }
            if (trackMode && autoTrackEnabled) {
                pendingSeedScreenPoint?.let { pending ->
                    val pendingColor = overlayColorFromArgb(manualSelectorColorArgb)
                    val supportRadius = if (seedLockMode == SeedLockMode.EXACT_PIXEL) 18.dp.toPx() else 24.dp.toPx()
                    val lineStroke = if (showMagnifier || autoTrackSelectionMode) 0.8.dp.toPx() else 1.2.dp.toPx()
                    val centerGap = 3.2.dp.toPx()
                    val handleCenter = autoSeedHandleCenter(pending, canvasSize, autoSeedHandleLiftPx)
                    drawCircle(
                        color = pendingColor.copy(alpha = 0.12f),
                        radius = supportRadius,
                        center = pending,
                        style = Fill
                    )
                    drawCircle(
                        color = pendingColor,
                        radius = if (seedLockMode == SeedLockMode.EXACT_PIXEL) 8.dp.toPx() else 11.dp.toPx(),
                        center = pending,
                        style = Stroke(width = lineStroke)
                    )
                    drawCircle(
                        color = pendingColor,
                        radius = 3.6.dp.toPx(),
                        center = pending,
                        style = Stroke(width = lineStroke)
                    )
                    drawRect(
                        color = pendingColor.copy(alpha = 0.24f),
                        topLeft = Offset(pending.x - supportRadius, pending.y - supportRadius),
                        size = Size(supportRadius * 2f, supportRadius * 2f),
                        style = Stroke(width = lineStroke)
                    )
                    drawLine(
                        pendingColor,
                        Offset(pending.x - 14.dp.toPx(), pending.y),
                        Offset(pending.x - centerGap, pending.y),
                        lineStroke
                    )
                    drawLine(
                        pendingColor,
                        Offset(pending.x + centerGap, pending.y),
                        Offset(pending.x + 14.dp.toPx(), pending.y),
                        lineStroke
                    )
                    drawLine(
                        pendingColor,
                        Offset(pending.x, pending.y - 14.dp.toPx()),
                        Offset(pending.x, pending.y - centerGap),
                        lineStroke
                    )
                    drawLine(
                        pendingColor,
                        Offset(pending.x, pending.y + centerGap),
                        Offset(pending.x, pending.y + 14.dp.toPx()),
                        lineStroke
                    )
                    drawLine(
                        pendingColor.copy(alpha = 0.72f),
                        pending,
                        handleCenter,
                        lineStroke
                    )
                    drawCircle(
                        color = pendingColor.copy(alpha = 0.12f),
                        radius = 14.dp.toPx(),
                        center = handleCenter,
                        style = Fill
                    )
                    drawCircle(
                        color = pendingColor,
                        radius = 12.dp.toPx(),
                        center = handleCenter,
                        style = Stroke(width = 1.1.dp.toPx())
                    )
                    drawLine(
                        pendingColor,
                        Offset(handleCenter.x - 5.dp.toPx(), handleCenter.y),
                        Offset(handleCenter.x + 5.dp.toPx(), handleCenter.y),
                        1.dp.toPx()
                    )
                    drawLine(
                        pendingColor,
                        Offset(handleCenter.x, handleCenter.y - 5.dp.toPx()),
                        Offset(handleCenter.x, handleCenter.y + 5.dp.toPx()),
                        1.dp.toPx()
                    )
                }
                autoTrackSeed?.let { seed ->
                    val center = videoToScreenOffset(seed.center, seed.sourceWidth, seed.sourceHeight, canvasSize)
                    val supportRadius = seed.estimatedRadiusPx.coerceAtLeast(8f)
                    val lineStroke = if (showMagnifier || autoTrackSelectionMode) 0.8.dp.toPx() else 1.dp.toPx()
                    val centerGap = 3.dp.toPx()
                    val seedColor = overlayColorFromArgb(manualSelectorColorArgb)
                    drawCircle(
                        color = seedColor.copy(alpha = 0.14f),
                        radius = 18.dp.toPx(),
                        center = center,
                        style = Fill
                    )
                    drawCircle(
                        color = seedColor,
                        radius = 9.dp.toPx(),
                        center = center,
                        style = Stroke(width = lineStroke)
                    )
                    drawCircle(
                        color = seedColor,
                        radius = 3.4.dp.toPx(),
                        center = center,
                        style = Stroke(width = lineStroke)
                    )
                    drawRect(
                        color = seedColor.copy(alpha = 0.28f),
                        topLeft = Offset(center.x - supportRadius, center.y - supportRadius),
                        size = Size(supportRadius * 2f, supportRadius * 2f),
                        style = Stroke(width = lineStroke)
                    )
                    drawLine(
                        seedColor,
                        Offset(center.x - 10.dp.toPx(), center.y),
                        Offset(center.x - centerGap, center.y),
                        lineStroke
                    )
                    drawLine(
                        seedColor,
                        Offset(center.x + centerGap, center.y),
                        Offset(center.x + 10.dp.toPx(), center.y),
                        lineStroke
                    )
                    drawLine(
                        seedColor,
                        Offset(center.x, center.y - 10.dp.toPx()),
                        Offset(center.x, center.y - centerGap),
                        lineStroke
                    )
                    drawLine(
                        seedColor,
                        Offset(center.x, center.y + centerGap),
                        Offset(center.x, center.y + 10.dp.toPx()),
                        lineStroke
                    )
                }
            }

            if (lightMode) {
                val slotCColor = Color(0xFF9B59B6)
                val slotDColor = Color(0xFF3498DB)
                buildList {
                    add(LightSlot.A to (lightGeometryA ?: lightDefaultGeometry(lightTool, canvasSize, LightSlot.A)))
                    if (lightDualMode) {
                        add(LightSlot.B to (lightGeometryB ?: lightDefaultGeometry(lightTool, canvasSize, LightSlot.B)))
                        add(LightSlot.C to (lightGeometryC ?: lightDefaultGeometry(lightTool, canvasSize, LightSlot.C)))
                        add(LightSlot.D to (lightGeometryD ?: lightDefaultGeometry(lightTool, canvasSize, LightSlot.D)))
                    }
                }.forEach { (slot, geometry) ->
                    val tint = when (slot) {
                        LightSlot.A -> overlayColorFromArgb(trackAColorArgb)
                        LightSlot.B -> overlayColorFromArgb(trackBColorArgb)
                        LightSlot.C -> slotCColor
                        LightSlot.D -> slotDColor
                    }
                    when (geometry.tool) {
                        LightTool.POINT -> {
                            drawCircle(tint.copy(alpha = 0.14f), 15.dp.toPx(), geometry.start, style = Fill)
                            drawCircle(tint, 9.dp.toPx(), geometry.start, style = Stroke(width = 1.2.dp.toPx()))
                            drawLine(tint, geometry.start - Offset(10.dp.toPx(), 0f), geometry.start + Offset(10.dp.toPx(), 0f), 1.dp.toPx())
                            drawLine(tint, geometry.start - Offset(0f, 10.dp.toPx()), geometry.start + Offset(0f, 10.dp.toPx()), 1.dp.toPx())
                        }
                        LightTool.LINE -> {
                            drawLine(tint, geometry.start, geometry.end, 2.dp.toPx())
                            drawCircle(tint.copy(alpha = 0.15f), 12.dp.toPx(), geometry.start, style = Fill)
                            drawCircle(tint.copy(alpha = 0.15f), 12.dp.toPx(), geometry.end, style = Fill)
                            drawCircle(tint, 9.dp.toPx(), geometry.start, style = Stroke(width = 1.1.dp.toPx()))
                            drawCircle(tint, 9.dp.toPx(), geometry.end, style = Stroke(width = 1.1.dp.toPx()))
                        }
                        LightTool.BOX -> {
                            val topLeft = Offset(minOf(geometry.start.x, geometry.end.x), minOf(geometry.start.y, geometry.end.y))
                            val size = Size(abs(geometry.end.x - geometry.start.x), abs(geometry.end.y - geometry.start.y))
                            drawRect(tint.copy(alpha = 0.10f), topLeft = topLeft, size = size, style = Fill)
                            drawRect(tint, topLeft = topLeft, size = size, style = Stroke(width = 1.2.dp.toPx()))
                            drawCircle(tint, 8.dp.toPx(), geometry.start, style = Stroke(width = 1.1.dp.toPx()))
                            drawCircle(tint, 8.dp.toPx(), geometry.end, style = Stroke(width = 1.1.dp.toPx()))
                        }
                    }
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            textSize = 22f
                            isFakeBoldText = true
                            color = tint.toArgb()
                        }
                        val labelPoint = when (geometry.tool) {
                            LightTool.POINT -> geometry.start
                            LightTool.LINE -> Offset((geometry.start.x + geometry.end.x) / 2f, (geometry.start.y + geometry.end.y) / 2f)
                            LightTool.BOX -> Offset(minOf(geometry.start.x, geometry.end.x), minOf(geometry.start.y, geometry.end.y) - 8.dp.toPx())
                        }
                        canvas.nativeCanvas.drawText(slot.name, labelPoint.x + 8.dp.toPx(), labelPoint.y - 8.dp.toPx(), paint)
                    }
                }
            }

            displayedCalibration?.let {
                val start = Offset(it.stickP1x.toFloat(), it.stickP1y.toFloat())
                val end = Offset(it.stickP2x.toFloat(), it.stickP2y.toFloat())
                val origin = Offset(it.originX.toFloat(), it.originY.toFloat())
                val xAxisLength = 110f * density * calibrationGuideScale
                val yAxisLength = 88f * density * calibrationGuideScale
                val rotationHandleRadius = 14.dp.toPx() * calibrationGuideScale
                val rotationHandle = axisEndpoint(origin, it.rotationDegrees, xAxisLength)
                val xAxisEnd = rotationHandle
                val yAxisEnd = perpendicularAxisEndpoint(origin, it.rotationDegrees, yAxisLength)
                val calibrationTint = overlayColorFromArgb(calibrationColorArgb)

                if (calibrationMode && calibrationTool == CalibrationTool.AXIS) {
                    drawCircle(
                        color = calibrationTint.copy(alpha = 0.22f),
                        radius = xAxisLength,
                        center = origin,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFFB74D).copy(alpha = 0.18f),
                        radius = yAxisLength,
                        center = origin,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawLine(
                        color = calibrationTint,
                        start = origin,
                        end = xAxisEnd,
                        strokeWidth = 3.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFFFB74D),
                        start = origin,
                        end = yAxisEnd,
                        strokeWidth = 3.dp.toPx()
                    )
                    drawCircle(
                        color = calibrationTint.copy(alpha = 0.26f),
                        radius = rotationHandleRadius * 2.2f,
                        center = rotationHandle,
                        style = Fill
                    )
                    drawCircle(calibrationTint, rotationHandleRadius, rotationHandle)
                    drawCircle(
                        color = Color.White,
                        radius = rotationHandleRadius,
                        center = rotationHandle,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(Color.White, 9.dp.toPx() * calibrationGuideScale, origin, style = Stroke(width = 2.dp.toPx()))
                    drawLine(
                        Color.White,
                        Offset(origin.x - 10.dp.toPx() * calibrationGuideScale, origin.y),
                        Offset(origin.x + 10.dp.toPx() * calibrationGuideScale, origin.y),
                        1.dp.toPx()
                    )
                    drawLine(
                        Color.White,
                        Offset(origin.x, origin.y - 10.dp.toPx() * calibrationGuideScale),
                        Offset(origin.x, origin.y + 10.dp.toPx() * calibrationGuideScale),
                        1.dp.toPx()
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.35f),
                        startAngle = 0f,
                        sweepAngle = normalizedDegrees(it.rotationDegrees).toFloat(),
                        useCenter = false,
                        topLeft = Offset(origin.x - 28.dp.toPx() * calibrationGuideScale, origin.y - 28.dp.toPx() * calibrationGuideScale),
                        size = Size(56.dp.toPx() * calibrationGuideScale, 56.dp.toPx() * calibrationGuideScale),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.LEFT
                            textSize = 24f * calibrationGuideScale
                            isAntiAlias = true
                        }
                        val angleText = "${normalizedDegrees(it.rotationDegrees).toInt()}°"
                        canvas.nativeCanvas.drawText(
                            angleText,
                            origin.x + 18.dp.toPx() * calibrationGuideScale,
                            origin.y - 18.dp.toPx() * calibrationGuideScale,
                            paint
                        )
                        val axisPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(220, 255, 255, 255)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 20f * calibrationGuideScale
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawText("x", xAxisEnd.x, xAxisEnd.y - 14.dp.toPx() * calibrationGuideScale, axisPaint)
                        canvas.nativeCanvas.drawText("y", yAxisEnd.x, yAxisEnd.y - 10.dp.toPx() * calibrationGuideScale, axisPaint)
                    }
                }

                if (calibrationMode && calibrationTool == CalibrationTool.SCALE) {
                    drawLine(
                        color = Color(0xFFFFC107),
                        start = start,
                        end = end,
                        strokeWidth = 3.5.dp.toPx()
                    )
                    // Hollow endpoint circles — outer glow, ring, and tiny center dot so the exact point is visible
                    val scaleEndpointR = 13.dp.toPx() * calibrationGuideScale
                    listOf(start, end).forEach { ep ->
                        drawCircle(Color(0xFFFFC107).copy(alpha = 0.20f), scaleEndpointR * 1.7f, ep, style = Fill)
                        drawCircle(Color(0xFFFFC107), scaleEndpointR, ep, style = Stroke(width = 3.dp.toPx()))
                        drawCircle(Color.White, 3.dp.toPx() * calibrationGuideScale, ep, style = Fill)
                    }

                    val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
                    drawRect(
                        color = Color.Black.copy(alpha = 0.65f),
                        topLeft = Offset(mid.x - 54.dp.toPx() * calibrationGuideScale, mid.y - 16.dp.toPx() * calibrationGuideScale),
                        size = Size(108.dp.toPx() * calibrationGuideScale, 28.dp.toPx() * calibrationGuideScale)
                    )
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 28f * calibrationGuideScale
                            isAntiAlias = true
                        }
                        val label = "${displayCalibrationDistance(it)} ${it.calibrationUnit}"
                        canvas.nativeCanvas.drawText(label, mid.x, mid.y + 9f * calibrationGuideScale, paint)
                    }
                }
                if (calibrationMode && calibrationTool == CalibrationTool.MEASURE) {
                    val measurement = measurementStickDraft
                    val unitLabel = displayedCalibration?.calibrationUnit ?: "m"
                    val measuredValue = measurementLengthInDisplayUnits(measurement, displayedCalibration)
                    if (measurement != null) {
                        val start = measurement.start
                        val end = measurement.end
                        drawLine(
                            color = calibrationTint,
                            start = start,
                            end = end,
                            strokeWidth = 3.dp.toPx()
                        )
                        // Hollow endpoint circles — outer glow, ring, and tiny center dot
                        val measureEndpointR = 12.dp.toPx() * calibrationGuideScale
                        listOf(start, end).forEach { ep ->
                            drawCircle(calibrationTint.copy(alpha = 0.18f), measureEndpointR * 1.7f, ep, style = Fill)
                            drawCircle(calibrationTint, measureEndpointR, ep, style = Stroke(width = 2.5.dp.toPx()))
                            drawCircle(Color.White, 3.dp.toPx() * calibrationGuideScale, ep, style = Fill)
                        }
                        val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
                        drawRect(
                            color = Color.Black.copy(alpha = 0.68f),
                            topLeft = Offset(mid.x - 64.dp.toPx() * calibrationGuideScale, mid.y - 16.dp.toPx() * calibrationGuideScale),
                            size = Size(128.dp.toPx() * calibrationGuideScale, 28.dp.toPx() * calibrationGuideScale)
                        )
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = calibrationTint.toArgb()
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 24f * calibrationGuideScale
                                isAntiAlias = true
                            }
                            val label = measuredValue?.let {
                                "${formatCalibratedValue(it)} $unitLabel"
                            } ?: "Measure"
                            canvas.nativeCanvas.drawText(label, mid.x, mid.y + 8f * calibrationGuideScale, paint)
                        }
                    }
                }
            }
        }
        }

        // 3. UI Layer
        val topChromeModifier = if (useLandscapeSplit) {
            Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(0.45f)
        } else {
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        }
        if (!analysisMode) {
        Column(
            modifier = Modifier
                .then(topChromeModifier)
                .onSizeChanged { topOverlayHeightPx = it.height }
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .zIndex(3f)
        ) {
            val saveNeedsAttention = lastCheckpointSavedAt == null || canUndo || canRedo
            Surface(
                color = PhysicsColorSurface.copy(alpha = 0.98f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PhyzxLogoMark(
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(24.dp)
                            .aspectRatio(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.undo() }, enabled = canUndo, modifier = Modifier.size(40.dp)) {
                        Text("↶", color = if (canUndo) PhysicsColorOnBackground else PhysicsColorOnSurface.copy(alpha = 0.3f))
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = canRedo, modifier = Modifier.size(40.dp)) {
                        Text("↷", color = if (canRedo) PhysicsColorOnBackground else PhysicsColorOnSurface.copy(alpha = 0.3f))
                    }
                    FilledTonalButton(
                        onClick = { videoFocusMode = !videoFocusMode },
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PhysicsColorSurfaceVariant,
                            contentColor = PhysicsColorOnBackground
                        ),
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (videoFocusMode) "▣" else "◎", style = MaterialTheme.typography.labelLarge)
                    }
                    FilledTonalButton(
                        onClick = { viewModel.saveProjectNow() },
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PhysicsColorSurfaceVariant,
                            contentColor = if (saveNeedsAttention) PhysicsColorAccent else PhysicsColorOnSurface
                        ),
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("⬢", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (!videoFocusMode && shouldShowCustomMagnifier && magnifierBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp)
                            .zIndex(3.4f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = PhysicsColorSurface.copy(alpha = 0.98f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider),
                            modifier = Modifier.padding(top = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PIXEL ADJUST",
                                    color = PhysicsColorAccent,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(1f, 2f, 5f).forEach { step ->
                                        TextButton(
                                            onClick = { seedNudgeStepPx = step },
                                            colors = ButtonDefaults.textButtonColors(
                                                containerColor = if (abs(seedNudgeStepPx - step) < 0.01f) {
                                                    PhysicsColorAccent.copy(alpha = 0.22f)
                                                } else {
                                                    PhysicsColorSurfaceVariant.copy(alpha = 0.55f)
                                                },
                                                contentColor = if (abs(seedNudgeStepPx - step) < 0.01f) {
                                                    PhysicsColorAccent
                                                } else {
                                                    PhysicsColorOnSurface
                                                }
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text("${step.toInt()} px", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                PrecisionDirectionPad(
                                    enabled = precisionTargetPosition != null,
                                    stepPx = seedNudgeStepPx,
                                    onNudge = { dx, dy -> nudgePrecisionTarget(dx, dy) },
                                    onConfirm = { confirmPrecisionTarget() }
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(132.dp),
                                color = PhysicsColorSurface.copy(alpha = 0.98f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = magnifierBitmap!!.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f
                                        val gap = 5.dp.toPx()
                                        val line = 0.8.dp.toPx()
                                        val arm = 18.dp.toPx()
                                        drawCircle(
                                            color = PhysicsColorAccent.copy(alpha = 0.16f),
                                            radius = 20.dp.toPx(),
                                            center = Offset(cx, cy),
                                            style = Fill
                                        )
                                        drawCircle(
                                            color = PhysicsColorAccent,
                                            radius = 10.dp.toPx(),
                                            center = Offset(cx, cy),
                                            style = Stroke(width = line)
                                        )
                                        drawLine(PhysicsColorAccent, Offset(cx - arm, cy), Offset(cx - gap, cy), line)
                                        drawLine(PhysicsColorAccent, Offset(cx + gap, cy), Offset(cx + arm, cy), line)
                                        drawLine(PhysicsColorAccent, Offset(cx, cy - arm), Offset(cx, cy - gap), line)
                                        drawLine(PhysicsColorAccent, Offset(cx, cy + gap), Offset(cx, cy + arm), line)
                                    }
                                }
                            }
                            precisionTargetPixelPosition?.let { point ->
                                Surface(
                                    color = PhysicsColorSurface.copy(alpha = 0.94f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider)
                                ) {
                                    Text(
                                        text = "X ${point.x.roundToInt()}  Y ${point.y.roundToInt()}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = PhysicsColorOnSurface,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
            error?.let {
                Text(
                    it,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(Color.Black.copy(0.8f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            exportStatus?.let {
                Text(
                    it,
                    color = Color(0xFF00E676),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(Color.Black.copy(0.8f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        }

        val bottomChromeModifier = if (useLandscapeSplit) {
            Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(0.45f)
        } else {
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        }
        if (!analysisMode) {
        Column(
            modifier = Modifier
                .then(bottomChromeModifier)
                .onSizeChanged { bottomOverlayHeightPx = it.height }
                .zIndex(6f)
        ) {
            if (!videoFocusMode && !analysisMode && showTimeline && duration > 0) {
                Surface(
                    color = PhysicsColorSurface.copy(alpha = 0.98f),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatTimestampMs(currentPos),
                            color = PhysicsColorOnSurface,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = pendingSeekPosition ?: currentPos.toFloat(),
                            onValueChange = {
                                if (!isTimelineDragging) {
                                    isTimelineDragging = true
                                    viewModel.beginScrubbing()
                                    viewModel.pausePlayback()
                                }
                                pendingSeekPosition = it
                                viewModel.scrubTo(it.toLong())
                            },
                            onValueChangeFinished = {
                                viewModel.endScrubbing()
                                pendingSeekPosition = null
                                isTimelineDragging = false
                            },
                            valueRange = 0f..duration.toFloat(),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = PhysicsColorPrimary,
                                activeTrackColor = PhysicsColorPrimary,
                                inactiveTrackColor = PhysicsColorDivider
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            formatTimestampMs(duration),
                            color = PhysicsColorOnSurface,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (!videoFocusMode) {
                Surface(
                    color = Color.Black.copy(alpha = 0.86f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (useLandscapeSplit) Modifier.heightIn(max = 260.dp) else Modifier)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MainBottomNavRow(
                                activeBottomTab = activeBottomTab,
                                playWhenReady = playWhenReady,
                                onToggleCal = {
                                    val opening = activeBottomTab != BottomMainTab.CAL
                                    activeBottomTab = if (opening) BottomMainTab.CAL else null
                                    if (opening) majorMode = MajorMode.CAL
                                },
                                onToggleTrack = {
                                    val opening = activeBottomTab != BottomMainTab.TRACK
                                    activeBottomTab = if (opening) BottomMainTab.TRACK else null
                                    if (opening) majorMode = MajorMode.TRACK
                                },
                                onTogglePlayPause = {
                                    activeBottomTab = BottomMainTab.PLAY
                                    viewModel.togglePlayPause()
                                },
                                onToggleLight = {
                                    val opening = activeBottomTab != BottomMainTab.LIGHT
                                    activeBottomTab = if (opening) BottomMainTab.LIGHT else null
                                    if (opening) {
                                        majorMode = MajorMode.LIGHT
                                        lightWorkspaceTab = LightWorkspaceTab.SELECT
                                        ensureLightGeometry(LightSlot.A)
                                        ensureLightGeometry(LightSlot.B)
                                    }
                                },
                                onToggleMore = {
                                    activeBottomTab = if (activeBottomTab == BottomMainTab.MORE) null else BottomMainTab.MORE
                                }
                            )

                        when (activeBottomTab) {
                            BottomMainTab.TRACK -> {
                                TrackBottomTrayContent(
                                    activeTrack = activeTrack,
                                    twoTrackMode = twoTrackMode,
                                    autoTrackEnabled = autoTrackEnabled,
                                    hasPreparedAutoTrack = hasPreparedAutoTrack,
                                    trackTrayPanel = trackTrayPanel,
                                    selectedPointId = selectedPointId,
                                    pendingSeedScreenPoint = pendingSeedScreenPoint,
                                    disableExistingPointSelection = disableExistingPointSelection,
                                    seedLockMode = seedLockMode,
                                    trackingStepOption = trackingStepOption,
                                    showDataTable = showDataTable,
                                    dataPanelMode = dataPanelMode,
                                    showGraphTrackA = showGraphTrackA,
                                    showGraphTrackB = showGraphTrackB,
                                    showTableTrackA = showTableTrackA,
                                    showTableTrackB = showTableTrackB,
                                    selectedGraphPointId = selectedGraphPointId,
                                    trackAColor = trackColor("A", trackAColorArgb, trackBColorArgb),
                                    trackBColor = trackColor("B", trackAColorArgb, trackBColorArgb),
                                    onAutoMode = {
                                        majorMode = MajorMode.TRACK
                                        if (twoTrackMode) saveCurrentTrackAutoState()
                                        autoTrackEnabled = true
                                        autoTrackRunning = false
                                        autoTrackSelectionMode = false
                                        manualPendingPoint = null
                                        manualPendingTouchDelta = null
                                        pendingSeedTouchDelta = null
                                        liveAutoTrackPreview.clear()
                                        autoTrackSeed = if (twoTrackMode) autoTrackSeed else null
                                        pendingSeedScreenPoint = if (twoTrackMode) pendingSeedScreenPoint else null
                                        autoTrackAwaitingSeed = if (twoTrackMode) autoTrackSeed == null else true
                                        trackTrayPanel = TrackTrayPanel.TARGET
                                        if (twoTrackMode) saveCurrentTrackAutoState()
                                    },
                                    onManualMode = {
                                        majorMode = MajorMode.TRACK
                                        if (twoTrackMode) saveCurrentTrackAutoState()
                                        autoTrackEnabled = false
                                        autoTrackRunning = false
                                        autoTrackSelectionMode = false
                                        manualPendingPoint = null
                                        manualPendingTouchDelta = null
                                        pendingSeedTouchDelta = null
                                        liveAutoTrackPreview.clear()
                                        autoTrackAwaitingSeed = false
                                        trackTrayPanel = TrackTrayPanel.TARGET
                                        if (twoTrackMode) saveCurrentTrackAutoState()
                                    },
                                    onToggleTarget = {
                                        majorMode = MajorMode.TRACK
                                        trackTrayPanel = if (trackTrayPanel == TrackTrayPanel.TARGET) null else TrackTrayPanel.TARGET
                                    },
                                    onToggleRun = {
                                        majorMode = MajorMode.TRACK
                                        trackTrayPanel = if (trackTrayPanel == TrackTrayPanel.AUTO_RUN) null else TrackTrayPanel.AUTO_RUN
                                    },
                                    onToggleStep = {
                                        majorMode = MajorMode.TRACK
                                        trackTrayPanel = if (trackTrayPanel == TrackTrayPanel.STEP) null else TrackTrayPanel.STEP
                                    },
                                    onToggleData = {
                                        majorMode = MajorMode.DATA
                                        showDataTable = true
                                        trackTrayPanel = if (trackTrayPanel == TrackTrayPanel.DATA) null else TrackTrayPanel.DATA
                                    },
                                    onShowClearConfirm = { showClearConfirm = true },
                                    onDeleteSelectedPoint = {
                                        if (selectedPointId != -1L) {
                                            viewModel.deletePoint(selectedPointId)
                                            selectedPointId = -1L
                                            draggedPointDraft = null
                                            draggedPointTouchDelta = null
                                        }
                                    },
                                    onSelectTrackA = {
                                        saveCurrentTrackAutoState()
                                        activeTrack = ActiveTrack.A
                                        loadCurrentTrackAutoState()
                                    },
                                    onSelectTrackB = {
                                        saveCurrentTrackAutoState()
                                        activeTrack = ActiveTrack.B
                                        loadCurrentTrackAutoState()
                                    },
                                    onBeginAutoSelect = {
                                        majorMode = MajorMode.TRACK
                                        autoTrackEnabled = true
                                        autoTrackSelectionMode = true
                                        autoTrackAwaitingSeed = true
                                        autoTrackRunning = false
                                        liveAutoTrackPreview.clear()
                                        pendingSeedScreenPoint = autoTrackSeed?.let {
                                            videoToScreenOffset(it.center, it.sourceWidth, it.sourceHeight, canvasSize)
                                        } ?: pendingSeedScreenPoint ?: Offset(
                                            canvasSize.width.coerceAtLeast(1) / 2f,
                                            canvasSize.height.coerceAtLeast(1) / 2f
                                        )
                                        pendingSeedTouchDelta = Offset.Zero
                                        if (twoTrackMode) saveCurrentTrackAutoState()
                                    },
                                    onConfirmAutoSeed = {
                                        val seedPoint = pendingSeedScreenPoint
                                        if (seedPoint == null) {
                                            Unit
                                        } else {
                                        autoTrackSelectionMode = false
                                        autoTrackAwaitingSeed = false
                                        pendingSeedTouchDelta = null
                                        if (twoTrackMode) saveCurrentTrackAutoState()
                                        scope.launch {
                                            seedAutoTracker(seedPoint)
                                            pendingSeedScreenPoint = null
                                            if (twoTrackMode) saveCurrentTrackAutoState()
                                        }
                                        }
                                    },
                                    onCancelAutoSeed = {
                                        autoTrackSelectionMode = false
                                        autoTrackAwaitingSeed = false
                                        pendingSeedScreenPoint = null
                                        pendingSeedTouchDelta = null
                                        if (twoTrackMode) saveCurrentTrackAutoState()
                                    },
                                    onSetExactSeedLock = { seedLockMode = SeedLockMode.EXACT_PIXEL },
                                    onSetSnapSeedLock = { seedLockMode = SeedLockMode.SNAP_OBJECT },
                                    onConfirmManualSelection = { confirmManualSelection() },
                                    onTogglePointPick = {
                                        val updated = !currentDisableExistingPointSelection()
                                        setCurrentDisableExistingPointSelection(updated)
                                        if (updated) {
                                            selectedPointId = -1L
                                            draggedPointDraft = null
                                            draggedPointTouchDelta = null
                                        }
                                    },
                                    onNextAutoTrack = { scope.launch { runPreparedAutoTracks(1) } },
                                    onTrackAllAuto = { scope.launch { runPreparedAutoTracksToEnd() } },
                                    autoTrackRunning = autoTrackRunning,
                                    onStopAutoTrack = {
                                        autoTrackRunning = false
                                        liveAutoTrackPreview.clear()
                                    },
                                    onTrackingStepSelected = { trackingStepOption = it },
                                    onGraphMode = {
                                        majorMode = MajorMode.DATA
                                        showDataTable = true
                                        dataPanelMode = DataPanelMode.GRAPH
                                        trackTrayPanel = TrackTrayPanel.DATA  // keep tray open
                                    },
                                    onTableMode = {
                                        majorMode = MajorMode.DATA
                                        showDataTable = true
                                        dataPanelMode = DataPanelMode.TABLE
                                        trackTrayPanel = TrackTrayPanel.DATA  // keep tray open
                                    },
                                    onAnalysisMode = {
                                        majorMode = MajorMode.DATA
                                        showDataTable = true
                                        dataPanelMode = DataPanelMode.ANALYSIS
                                        trackTrayPanel = TrackTrayPanel.DATA  // keep tray open
                                        // Always open the Analysis Desk when entering Analysis Mode
                                        showGraphOptions = true
                                    },
                                    onRemoveSelectedGraphPoint = {
                                        selectedGraphPointId?.let(viewModel::deletePoint)
                                        selectedGraphSampleIndex = -1
                                    },
                                    onToggleShowData = { showDataTable = !showDataTable },
                                    onGraphViewA = {
                                        showGraphTrackA = true
                                        showGraphTrackB = false
                                        showGraphDelta = false
                                        activeTrack = ActiveTrack.A
                                    },
                                    onGraphViewB = {
                                        showGraphTrackA = false
                                        showGraphTrackB = true
                                        showGraphDelta = false
                                        activeTrack = ActiveTrack.B
                                    },
                                    onGraphViewBoth = {
                                        showGraphTrackA = true
                                        showGraphTrackB = true
                                    },
                                    onTableViewA = {
                                        showTableTrackA = true
                                        showTableTrackB = false
                                        activeTrack = ActiveTrack.A
                                    },
                                    onTableViewB = {
                                        showTableTrackA = false
                                        showTableTrackB = true
                                        activeTrack = ActiveTrack.B
                                    },
                                    onTableViewBoth = {
                                        showTableTrackA = true
                                        showTableTrackB = true
                                    },
                                    onExportCsv = { runExportAction { artifacts -> "CSV ${artifacts.csvFile.name}" } },
                                    onExportXlsx = { runExportAction { artifacts -> "XLSX ${artifacts.excelFile.name}" } },
                                    onExportPdf = { runExportAction { artifacts -> "PDF ${artifacts.pdfFile.name}" } }
                                )
                            }
                            BottomMainTab.CAL -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    BottomDeckButton("SCALE", selected = calibrationMode && calibrationTool == CalibrationTool.SCALE, accent = LabGold) {
                                        majorMode = MajorMode.CAL
                                        calibrationTool = CalibrationTool.SCALE
                                    }
                                    BottomDeckButton("AXIS", selected = calibrationMode && calibrationTool == CalibrationTool.AXIS, accent = LabSuccess) {
                                        majorMode = MajorMode.CAL
                                        calibrationTool = CalibrationTool.AXIS
                                    }
                                    BottomDeckButton("MEASURE", selected = calibrationMode && calibrationTool == CalibrationTool.MEASURE, accent = overlayColorFromArgb(calibrationColorArgb)) {
                                        majorMode = MajorMode.CAL
                                        calibrationTool = CalibrationTool.MEASURE
                                    }
                                    displayedCalibration?.let {
                                        BottomDeckButton("${displayCalibrationDistance(it)}${it.calibrationUnit}", accent = LabAccent) {
                                            showCalibrationDialog = true
                                        }
                                    }
                                }
                            }
                            BottomMainTab.PLAY -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val currentFrame = if (frameDurationMs > 0) (currentPos / frameDurationMs).toInt() else 0
                                        val totalFrames = if (frameDurationMs > 0 && duration > 0) (duration / frameDurationMs).toInt() else 0
                                        Text(
                                            "Frame ${currentFrame.coerceAtLeast(0)} / ${totalFrames.coerceAtLeast(0)}",
                                            color = PhysicsColorOnBackground.copy(alpha = 0.72f),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            "${formatTimestampMs(currentPos)} / ${formatTimestampMs(duration)}",
                                            color = PhysicsColorOnBackground.copy(alpha = 0.72f),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ScrubberButton("-10", -10, viewModel)
                                        ScrubberButton("-1", -1, viewModel)
                                        OutlinedButton(
                                            onClick = { viewModel.restart() },
                                            border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorAccent.copy(alpha = 0.45f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = PhysicsColorSurfaceVariant.copy(alpha = 0.84f),
                                                contentColor = PhysicsColorAccent
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.heightIn(min = 42.dp)
                                        ) {
                                            Text("RESTART", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Button(
                                            onClick = { viewModel.togglePlayPause() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (playWhenReady) PhysicsColorAccent else PhysicsColorPrimary,
                                                contentColor = PhysicsColorBackground
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.heightIn(min = 44.dp)
                                        ) {
                                            Text(
                                                if (playWhenReady) "PAUSE" else "PLAY",
                                                fontFamily = FontFamily.Monospace,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                        ScrubberButton("+1", 1, viewModel)
                                        ScrubberButton("+10", 10, viewModel)
                                        OutlinedButton(
                                            onClick = { viewModel.setLoopPlayback(!loopPlayback) },
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (loopPlayback) PhysicsColorPrimary else PhysicsColorDivider),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (loopPlayback) PhysicsColorPrimary.copy(alpha = 0.18f) else PhysicsColorSurfaceVariant.copy(alpha = 0.6f),
                                                contentColor = if (loopPlayback) PhysicsColorPrimary else PhysicsColorOnBackground
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.heightIn(min = 42.dp)
                                        ) {
                                            Text("LOOP", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                        }
                                        listOf(0.25f, 0.5f, 1f, 2f, 4f).forEach { speed ->
                                            OutlinedButton(
                                                onClick = { viewModel.setPlaybackSpeed(speed) },
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (abs(playbackSpeed - speed) < 0.01f) PhysicsColorPrimary else PhysicsColorDivider
                                                ),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (abs(playbackSpeed - speed) < 0.01f) PhysicsColorPrimary.copy(alpha = 0.18f) else PhysicsColorSurfaceVariant.copy(alpha = 0.6f),
                                                    contentColor = if (abs(playbackSpeed - speed) < 0.01f) PhysicsColorPrimary else PhysicsColorOnBackground
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.heightIn(min = 36.dp)
                                            ) {
                                                Text("${speed}x", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                            BottomMainTab.DATA -> Unit
                            BottomMainTab.LIGHT -> {
                                LightTopTray(
                                    workspaceTab = lightWorkspaceTab,
                                    onWorkspaceTabSelected = { tab ->
                                        majorMode = MajorMode.LIGHT
                                        lightWorkspaceTab = tab
                                        if (tab == LightWorkspaceTab.COMPARE) {
                                            lightPanelMode = LightPanelMode.COMPARE
                                        } else if (tab == LightWorkspaceTab.GRAPH || tab == LightWorkspaceTab.TABLE) {
                                            if (lightPanelMode == LightPanelMode.COMPARE) lightPanelMode = LightPanelMode.PROFILE
                                        }
                                    },
                                    lightTool = lightTool,
                                    onLightToolSelected = { tool ->
                                        lightTool = tool
                                        setLightGeometry(LightSlot.A, lightDefaultGeometry(tool, canvasSize, LightSlot.A))
                                        setLightGeometry(LightSlot.B, lightDefaultGeometry(tool, canvasSize, LightSlot.B))
                                    },
                                    lightDualMode = lightDualMode,
                                    lightActiveSlot = lightActiveSlot,
                                    onLightActiveSlotSelected = { lightActiveSlot = it },
                                    lightPanelMode = lightPanelMode,
                                    timeRecording = lightTimeRecording,
                                    onTimeRecordingToggle = { lightTimeRecording = !lightTimeRecording },
                                    onClearTimeSeries = ::clearAllLightTimeSeries,
                                    slotAColor = overlayColorFromArgb(trackAColorArgb),
                                    slotBColor = overlayColorFromArgb(trackBColorArgb)
                                )
                            }
                            BottomMainTab.MORE -> {
                                MoreBottomTrayContent(
                                    moreTrayPanel = moreTrayPanel,
                                    showMagnifier = showMagnifier,
                                    magnifierZoom = magnifierZoom,
                                    videoScale = videoScale,
                                    videoScaleSlider = videoScaleSlider,
                                    videoZoomEnabled = videoZoomEnabled,
                                    maxPanX = maxVideoPanX(),
                                    maxPanY = maxVideoPanY(),
                                    videoOffsetX = videoOffsetX,
                                    videoOffsetY = videoOffsetY,
                                    precisionStepPx = seedNudgeStepPx,
                                    precisionTargetPosition = precisionTargetPixelPosition,
                                    precisionTargetLabel = precisionTargetLabel,
                                    canConfirmPrecisionTarget = canConfirmPrecisionTarget,
                                    forceLandscapeMode = forceLandscapeMode,
                                    twoTrackMode = twoTrackMode,
                                    showTimeline = showTimeline,
                                    showAnalysisGuides = showAnalysisGuides,
                                    pointHistoryMode = pointHistoryMode,
                                    activeOverlayRole = activeOverlayRole,
                                    trackAColorArgb = trackAColorArgb,
                                    trackBColorArgb = trackBColorArgb,
                                    manualSelectorColorArgb = manualSelectorColorArgb,
                                    vectorColorArgb = vectorColorArgb,
                                    calibrationColorArgb = calibrationColorArgb,
                                    onToggleEdit = {
                                        majorMode = MajorMode.EDIT
                                        moreTrayPanel = if (moreTrayPanel == MoreTrayPanel.EDIT) null else MoreTrayPanel.EDIT
                                    },
                                    onToggleMag = { moreTrayPanel = if (moreTrayPanel == MoreTrayPanel.MAG) null else MoreTrayPanel.MAG },
                                    onToggleView = { moreTrayPanel = if (moreTrayPanel == MoreTrayPanel.VIEW) null else MoreTrayPanel.VIEW },
                                    onToggleColors = { moreTrayPanel = if (moreTrayPanel == MoreTrayPanel.COLORS) null else MoreTrayPanel.COLORS },
                                    onToggleExport = { moreTrayPanel = if (moreTrayPanel == MoreTrayPanel.EXPORT) null else MoreTrayPanel.EXPORT },
                                    onResetTrim = { viewModel.resetTrim() },
                                    onTrimStart = { viewModel.setTrimStart() },
                                    onTrimEnd = { viewModel.setTrimEnd() },
                                    onShowMagnifierChange = { showMagnifier = it },
                                    onMagnifierZoomChange = {
                                        magnifierZoom = it
                                        showMagnifier = true
                                    },
                                    onVideoScaleChange = {
                                        val newScale = it.coerceIn(1f, 6f)
                                        videoZoomEnabled = it > 1.02f || videoZoomEnabled
                                        clampVideoPan(newScale)
                                    },
                                    onVideoOffsetXChange = { fraction ->
                                        val maxPanX = maxVideoPanX().coerceAtLeast(1f)
                                        videoOffsetX = (fraction * maxPanX).coerceIn(-maxPanX, maxPanX)
                                    },
                                    onVideoOffsetYChange = { fraction ->
                                        val maxPanY = maxVideoPanY().coerceAtLeast(1f)
                                        videoOffsetY = (fraction * maxPanY).coerceIn(-maxPanY, maxPanY)
                                    },
                                    onPrecisionStepChange = { seedNudgeStepPx = it },
                                    onNudgePrecision = { dx, dy -> nudgePrecisionTarget(dx, dy) },
                                    onConfirmPrecisionTarget = { confirmPrecisionTarget() },
                                    onForceLandscapeModeChange = { forceLandscapeMode = it },
                                    onToggleTrackCountMode = { toggleTrackCountMode() },
                                    onShowTimelineChange = { showTimeline = it },
                                    onShowAnalysisGuidesChange = { showAnalysisGuides = it },
                                    onPointHistoryModeChange = { pointHistoryMode = it },
                                    onActiveOverlayRoleChange = { activeOverlayRole = it },
                                    onOverlayColorSelected = { role, color ->
                                        when (role) {
                                            OverlayColorRole.TRACK_A -> trackAColorArgb = color.toArgb()
                                            OverlayColorRole.TRACK_B -> trackBColorArgb = color.toArgb()
                                            OverlayColorRole.MANUAL_SELECTOR -> manualSelectorColorArgb = color.toArgb()
                                            OverlayColorRole.VECTOR -> vectorColorArgb = color.toArgb()
                                            OverlayColorRole.CALIBRATION -> calibrationColorArgb = color.toArgb()
                                        }
                                    },
                                    onExportCsv = { runExportAction { artifacts -> "CSV ${artifacts.csvFile.name}" } },
                                    onExportXlsx = { runExportAction { artifacts -> "XLSX ${artifacts.excelFile.name}" } },
                                    onExportPdf = { runExportAction { artifacts -> "PDF ${artifacts.pdfFile.name}" } },
                                    onShareCsv = {
                                        runExportAction { artifacts ->
                                            shareExportFile(context, artifacts.csvFile, "text/csv")
                                            "Shared CSV"
                                        }
                                    },
                                    onSharePdf = {
                                        runExportAction { artifacts ->
                                            shareExportFile(context, artifacts.pdfFile, "application/pdf")
                                            "Shared PDF"
                                        }
                                    }
                                )
                            }
                            null -> Unit
                        }
                    }
                }
            }

            if (!videoFocusMode && calibrationMode && activeBottomTab == BottomMainTab.CAL) {
                Surface(color = Color.Black.copy(alpha = 0.62f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Slider(
                            value = calibrationGuideScale,
                            onValueChange = { calibrationGuideScale = it },
                            valueRange = 0.7f..1.8f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF)
                            )
                        )
                    }
                }
            }

        }
        }

        WorkspaceOverlayPanels(
            videoFocusMode = videoFocusMode,
            dataMode = dataMode,
            trackMode = trackMode,
            showDataTable = showDataTable,
            showLandscapeLiveGraph = showLandscapeLiveGraph,
            dataPanelMode = dataPanelMode,
            displayedUnit = displayedUnit,
            visibleTablePoints = visibleTablePoints,
            selectedTableSourcePointsCount = selectedTableSourcePoints.size,
            twoTrackMode = twoTrackMode,
            showTableTrackA = showTableTrackA,
            showTableTrackB = showTableTrackB,
            trackAColor = overlayColorFromArgb(trackAColorArgb),
            trackBColor = overlayColorFromArgb(trackBColorArgb),
            isPlaying = isPlaying,
            isLandscape = isLandscape,
            panelModifier = run {
                // When video is hidden we want the graph to expand and fill the full screen.
                val hideVideo = dataMode && dataPanelMode != DataPanelMode.TABLE && !lightMode
                when {
                    analysisMode -> Modifier
                        .fillMaxSize()
                        .zIndex(20f)
                    hideVideo && !useLandscapeSplit -> Modifier
                        .fillMaxSize()
                        .zIndex(6f)
                        .padding(top = dataPanelTopPadding, start = 8.dp, end = 8.dp, bottom = dataPanelBottomPadding)
                    useLandscapeSplit && (dataMode || showLandscapeLiveGraph || lightMode) -> Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxWidth(if (dataPanelMode == DataPanelMode.ANALYSIS) 0.62f else 0.45f)
                        .fillMaxHeight()
                        .zIndex(6f)
                        .padding(top = dataPanelTopPadding, start = 8.dp, end = 8.dp, bottom = dataPanelBottomPadding)
                    else -> Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(6f)
                        .padding(top = dataPanelTopPadding, start = 8.dp, end = 8.dp, bottom = dataPanelBottomPadding)
                }
            },
            selectedGraphPlotPoints = selectedGraphPlotPoints,
            totalGraphPointCount = totalGraphPointCount,
            compareTrackAGraphPoints = compareTrackAGraphPoints,
            compareTrackBGraphPoints = compareTrackBGraphPoints,
            primaryGraphTrackColor = primaryGraphTrackColor,
            showGraphTrackA = showGraphTrackA,
            showGraphTrackB = showGraphTrackB,
            showGraphDelta = showGraphDelta,
            onShowGraphTrackAChange = { showGraphTrackA = it },
            onShowGraphTrackBChange = { showGraphTrackB = it },
            onShowGraphDeltaChange = { showGraphDelta = it },
            showGraphOptions = showGraphOptions,
            onShowGraphOptionsChange = { showGraphOptions = it },
            onViewportSummaryChanged = { graphViewportSummary = it },
            availableGraphSeries = availableGraphSeries,
            selectedGraphSeries = selectedGraphSeries,
            onSelectedGraphSeriesChange = { selectedGraphSeries = it },
            availableGraphSeriesX = availableGraphSeriesX,
            selectedGraphSeriesX = selectedGraphSeriesX,
            onSelectedGraphSeriesXChange = { selectedGraphSeriesX = it },
            selectedGraphSampleIndex = selectedGraphSampleIndex,
            onSelectedGraphSampleIndexChange = { selectedGraphSampleIndex = it },
            onExitAnalysisMode = {
                dataPanelMode = DataPanelMode.GRAPH
                showDataTable = true
                showGraphOptions = true
            },
            graphProfileMode = graphProfileMode,
            onGraphProfileModeChange = { graphProfileMode = it },
            graphDisplayMode = graphDisplayMode,
            onGraphDisplayModeChange = { graphDisplayMode = it },
            blendAlpha = blendAlpha,
            onBlendAlphaChange = { blendAlpha = it },
            selectedProfileResult = selectedProfileResult,
            photogateAFraction = photogateAFraction,
            onPhotogateAFractionChange = { photogateAFraction = it },
            photogateBFraction = photogateBFraction,
            onPhotogateBFractionChange = { photogateBFraction = it },
            photogateA = photogateA,
            photogateB = photogateB,
            videoPhotogateDeltaUs = videoPhotogateDeltaUs,
            currentTwoBodySample = currentTwoBodySample,
            displayedSeparationUnitFactor = displayedSeparationUnitFactor,
            minimumSeparation = minimumSeparation,
            maximumSeparation = maximumSeparation,
            averageSeparation = averageSeparation,
            currentRelativeSpeed = currentRelativeSpeed,
            minimumRelativeSpeed = minimumRelativeSpeed,
            maximumRelativeSpeed = maximumRelativeSpeed,
            averageRelativeSpeed = averageRelativeSpeed,
            twoBodySamplesSize = twoBodySamples.size,
            calibrationUnit = calibration?.calibrationUnit ?: "m",
            closestApproachSample = closestApproachSample,
            closestApproachMomentumA = closestApproachMomentumA,
            closestApproachMomentumB = closestApproachMomentumB,
            closestApproachTotalMomentum = closestApproachTotalMomentum,
            closestApproachRelativeMomentum = closestApproachRelativeMomentum,
            interactionClassification = interactionClassification,
            preInteractionSnapshot = preInteractionSnapshot,
            postInteractionSnapshot = postInteractionSnapshot,
            gateAToClosestUs = gateAToClosestUs,
            closestToGateBUs = closestToGateBUs,
            onFocusClosestApproach = ::focusClosestApproach,
            onFocusTimestampUs = ::focusTimestampUs,
            currentAbsoluteTimeUs = absoluteCurrentPos * 1000L,
            trackAMomentum = trackAMomentum,
            trackBMomentum = trackBMomentum,
            totalMomentum = totalMomentum,
            relativeMomentum = relativeMomentum,
            momentumUnitLabel = momentumUnitLabel,
            showAnalysisGuides = showAnalysisGuides,
            lightMode = lightMode,
            lightWorkspaceTab = lightWorkspaceTab,
            onLightWorkspaceTabSelected = {
                lightWorkspaceTab = it
                if (it == LightWorkspaceTab.COMPARE) lightPanelMode = LightPanelMode.COMPARE
            },
            lightPanelMode = lightPanelMode,
            onLightPanelModeSelected = { lightPanelMode = it },
            lightCompareSource = lightCompareSource,
            onLightCompareSourceSelected = { lightCompareSource = it },
            lightTool = lightTool,
            onLightToolSelected = {
                lightTool = it
                setLightGeometry(LightSlot.A, lightDefaultGeometry(it, canvasSize, LightSlot.A))
                setLightGeometry(LightSlot.B, lightDefaultGeometry(it, canvasSize, LightSlot.B))
                setLightGeometry(LightSlot.C, lightDefaultGeometry(it, canvasSize, LightSlot.C))
                setLightGeometry(LightSlot.D, lightDefaultGeometry(it, canvasSize, LightSlot.D))
            },
            lightChannel = lightChannel,
            onLightChannelSelected = { lightChannel = it },
            lightDualMode = lightDualMode,
            onLightDualModeChange = {
                lightDualMode = it
                if (!it) {
                    lightActiveSlot = LightSlot.A
                    showLightGraphA = true
                    showLightGraphB = false
                    showLightGraphC = false
                    showLightGraphD = false
                    showLightTableA = true
                    showLightTableB = false
                    showLightTableC = false
                    showLightTableD = false
                }
            },
            lightActiveSlot = lightActiveSlot,
            onLightActiveSlotSelected = { lightActiveSlot = it },
            showLightGraphA = showLightGraphA,
            showLightGraphB = showLightGraphB,
            showLightGraphC = showLightGraphC,
            showLightGraphD = showLightGraphD,
            onShowLightGraphAChange = { showLightGraphA = it },
            onShowLightGraphBChange = { showLightGraphB = it },
            onShowLightGraphCChange = { showLightGraphC = it },
            onShowLightGraphDChange = { showLightGraphD = it },
            showLightTableA = showLightTableA,
            showLightTableB = showLightTableB,
            showLightTableC = showLightTableC,
            showLightTableD = showLightTableD,
            onShowLightTableAChange = { showLightTableA = it },
            onShowLightTableBChange = { showLightTableB = it },
            onShowLightTableCChange = { showLightTableC = it },
            onShowLightTableDChange = { showLightTableD = it },
            lightBackgroundSubtract = lightBackgroundSubtract,
            onLightBackgroundSubtractChange = { lightBackgroundSubtract = it },
            lightSmooth = lightSmooth,
            onLightSmoothChange = { lightSmooth = it },
            lightDetectPeaks = lightDetectPeaks,
            onLightDetectPeaksChange = { lightDetectPeaks = it },
            lightTimeRecording = lightTimeRecording,
            onStartLightTimeRecording = { lightTimeRecording = true },
            onStopLightTimeRecording = { lightTimeRecording = false },
            onClearAllLightTimeSeries = ::clearAllLightTimeSeries,
            onLightExportCsv = { runExportAction { artifacts -> "CSV ${artifacts.csvFile.name}" } },
            onLightExportXlsx = { runExportAction { artifacts -> "XLSX ${artifacts.excelFile.name}" } },
            onLightExportPdf = { runExportAction { artifacts -> "PDF ${artifacts.pdfFile.name}" } },
            onAnalysisExportCsv = {
                runExportAction { artifacts ->
                    shareExportFile(context, artifacts.csvFile, "text/csv")
                    "Shared CSV"
                }
            },
            onAnalysisExportXlsx = {
                runExportAction { artifacts ->
                    shareExportFile(
                        context,
                        artifacts.excelFile,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
                    "Shared XLSX"
                }
            },
            onAnalysisExportPdf = {
                runExportAction { artifacts ->
                    shareExportFile(context, artifacts.pdfFile, "application/pdf")
                    "Shared PDF"
                }
            },
            lightUnitLabel = calibration?.calibrationUnit ?: "px",
            lightGeometryA = lightGeometryA,
            lightGeometryB = lightGeometryB,
            lightGeometryC = lightGeometryC,
            lightGeometryD = lightGeometryD,
            lightProfileA = lightProfileA,
            lightProfileB = lightProfileB,
            lightProfileC = lightProfileC,
            lightProfileD = lightProfileD,
            lightTimeSeriesA = lightTimeSeriesA.toList(),
            lightTimeSeriesB = lightTimeSeriesB.toList(),
            lightTimeSeriesC = lightTimeSeriesC.toList(),
            lightTimeSeriesD = lightTimeSeriesD.toList(),
            onClearLightTimeSeries = ::clearLightTimeSeries,
            graphFullScreen = graphFullScreen,
            onGraphFullScreenChange = { graphFullScreen = it }
        )

        if (useLandscapeSplit && !analysisMode) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.55f)
                    .wrapContentWidth(Alignment.End)
                    .width(1.dp)
                    .background(PhysicsColorDivider)
                    .zIndex(1.5f)
            )
        }

        if (graphFullScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PhysicsColorBackground)
                    .zIndex(10f)
            ) {
                TrackGraphOverlay(
                    analysisMode = dataPanelMode == DataPanelMode.ANALYSIS,
                    displayedUnit = displayedUnit,
                    graphPlotPoints = selectedGraphPlotPoints,
                    totalPointCount = totalGraphPointCount,
                    isLivePlayback = isPlaying,
                    graphFullScreen = graphFullScreen,
                    onGraphFullScreenChange = { graphFullScreen = it },
                    compareTrackAPlotPoints = compareTrackAGraphPoints,
                    compareTrackBPlotPoints = compareTrackBGraphPoints,
                    primaryTrackColor = primaryGraphTrackColor,
                    trackAColor = overlayColorFromArgb(trackAColorArgb),
                    trackBColor = overlayColorFromArgb(trackBColorArgb),
                    showCompareTrackA = showGraphTrackA,
                    showCompareTrackB = showGraphTrackB,
                    showCompareDelta = showGraphDelta,
                    onShowCompareTrackAChange = { showGraphTrackA = it },
                    onShowCompareTrackBChange = { showGraphTrackB = it },
                    onShowCompareDeltaChange = { showGraphDelta = it },
                    showOptionPanels = false,
                    onShowOptionPanelsChange = { showGraphOptions = it },
                    onViewportSummaryChanged = { graphViewportSummary = it },
                    availableSeries = availableGraphSeries,
                    selectedSeries = selectedGraphSeries,
                    onSeriesSelected = { selectedGraphSeries = it },
                    availableSeriesX = availableGraphSeriesX,
                    selectedSeriesX = selectedGraphSeriesX,
                    onSeriesXSelected = { selectedGraphSeriesX = it },
                    selectedSampleIndex = selectedGraphSampleIndex,
                    onSampleSelected = { selectedGraphSampleIndex = it },
                    onExitAnalysisMode = {
                        dataPanelMode = DataPanelMode.GRAPH
                        showDataTable = true
                        showGraphOptions = true
                    },
                    profileMode = graphProfileMode,
                    onProfileModeSelected = { graphProfileMode = it },
                    graphDisplayMode = graphDisplayMode,
                    onGraphDisplayModeChange = { graphDisplayMode = it },
                    blendAlpha = blendAlpha,
                    onBlendAlphaChange = { blendAlpha = it },
                    profileResult = selectedProfileResult,
                    photogateAFraction = photogateAFraction,
                    onPhotogateAFractionChange = { photogateAFraction = it },
                    photogateBFraction = photogateBFraction,
                    onPhotogateBFractionChange = { photogateBFraction = it },
                    photogateA = photogateA,
                    photogateB = photogateB,
                    photogateDeltaUs = videoPhotogateDeltaUs,
                    twoBodyCurrentSeparation = currentTwoBodySample?.separationMeters?.div(displayedSeparationUnitFactor).takeIf { twoTrackMode },
                    twoBodyMinimumSeparation = minimumSeparation.takeIf { twoTrackMode },
                    twoBodyMaximumSeparation = maximumSeparation.takeIf { twoTrackMode },
                    twoBodyAverageSeparation = averageSeparation.takeIf { twoTrackMode },
                    twoBodyCurrentRelativeSpeed = currentRelativeSpeed.takeIf { twoTrackMode },
                    twoBodyMinimumRelativeSpeed = minimumRelativeSpeed.takeIf { twoTrackMode },
                    twoBodyMaximumRelativeSpeed = maximumRelativeSpeed.takeIf { twoTrackMode },
                    twoBodyAverageRelativeSpeed = averageRelativeSpeed.takeIf { twoTrackMode },
                    twoBodyFrameCount = if (twoTrackMode) twoBodySamples.size else 0,
                    twoBodyUnit = calibrationUnit,
                    closestApproachTimeUs = closestApproachSample?.timestampUs.takeIf { twoTrackMode },
                    closestApproachFrame = closestApproachSample?.frameNumber.takeIf { twoTrackMode },
                    closestApproachSeparation = closestApproachSample?.separationMeters?.div(displayedSeparationUnitFactor).takeIf { twoTrackMode },
                    closestApproachRelativeSpeed = closestApproachSample?.relativeSpeedMetersPerSec?.div(displayedSeparationUnitFactor).takeIf { twoTrackMode },
                    closestApproachMomentumA = closestApproachMomentumA.takeIf { twoTrackMode },
                    closestApproachMomentumB = closestApproachMomentumB.takeIf { twoTrackMode },
                    closestApproachTotalMomentum = closestApproachTotalMomentum.takeIf { twoTrackMode },
                    closestApproachRelativeMomentum = closestApproachRelativeMomentum.takeIf { twoTrackMode },
                    interactionClass = interactionClassification.takeIf { twoTrackMode },
                    preInteractionSnapshot = preInteractionSnapshot.takeIf { twoTrackMode },
                    postInteractionSnapshot = postInteractionSnapshot.takeIf { twoTrackMode },
                    gateAToClosestUs = gateAToClosestUs.takeIf { twoTrackMode },
                    closestToGateBUs = closestToGateBUs.takeIf { twoTrackMode },
                    onFocusClosestApproach = ::focusClosestApproach,
                    onFocusTimestampUs = ::focusTimestampUs,
                    currentAbsoluteTimeUs = absoluteCurrentPos * 1000L,
                    trackAMomentum = trackAMomentum,
                    trackBMomentum = trackBMomentum,
                    totalMomentum = totalMomentum,
                    relativeMomentum = relativeMomentum,
                    momentumUnitLabel = momentumUnitLabel,
                    showAnalysisGuides = showAnalysisGuides,
                    onExportCsv = { runExportAction { artifacts -> "CSV ${artifacts.csvFile.name}" } },
                    onExportXlsx = { runExportAction { artifacts -> "XLSX ${artifacts.excelFile.name}" } },
                    onExportPdf = { runExportAction { artifacts -> "PDF ${artifacts.pdfFile.name}" } },
                    isLandscape = isLandscape,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    }
}

@Composable
private fun MainBottomNavRow(
    activeBottomTab: BottomMainTab?,
    playWhenReady: Boolean,
    onToggleCal: () -> Unit,
    onToggleTrack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleLight: () -> Unit,
    onToggleMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavIconButton(symbol = "⌁", selected = activeBottomTab == BottomMainTab.CAL, accent = PhysicsColorPrimary, prominent = true) {
            onToggleCal()
        }
        BottomNavIconButton(symbol = "∿", selected = activeBottomTab == BottomMainTab.TRACK, accent = PhysicsColorPrimary, prominent = true) {
            onToggleTrack()
        }
        Button(
            onClick = onTogglePlayPause,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (playWhenReady) PhysicsColorAccent else PhysicsColorPrimary,
                contentColor = PhysicsColorBackground
            ),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(56.dp)
        ) {
            Text(if (playWhenReady) "⏸" else "▶", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.displaySmall)
        }
        BottomNavIconButton(symbol = "☼", selected = activeBottomTab == BottomMainTab.LIGHT, accent = PhysicsColorAccent, prominent = true) {
            onToggleLight()
        }
        BottomNavIconButton(symbol = "⋮", selected = activeBottomTab == BottomMainTab.MORE, accent = PhysicsColorOnBackground, prominent = true) {
            onToggleMore()
        }
    }
}

@Composable
private fun WorkspaceOverlayPanels(
    videoFocusMode: Boolean,
    dataMode: Boolean,
    trackMode: Boolean,
    showDataTable: Boolean,
    showLandscapeLiveGraph: Boolean,
    dataPanelMode: DataPanelMode,
    displayedUnit: String,
    visibleTablePoints: List<DisplayKinematicsPoint>,
    selectedTableSourcePointsCount: Int,
    twoTrackMode: Boolean,
    showTableTrackA: Boolean,
    showTableTrackB: Boolean,
    trackAColor: Color,
    trackBColor: Color,
    isPlaying: Boolean,
    isLandscape: Boolean,
    panelModifier: Modifier,
    selectedGraphPlotPoints: List<GraphPlotPoint>,
    totalGraphPointCount: Int,
    compareTrackAGraphPoints: List<GraphPlotPoint>,
    compareTrackBGraphPoints: List<GraphPlotPoint>,
    primaryGraphTrackColor: Color,
    showGraphTrackA: Boolean,
    showGraphTrackB: Boolean,
    showGraphDelta: Boolean,
    onShowGraphTrackAChange: (Boolean) -> Unit,
    onShowGraphTrackBChange: (Boolean) -> Unit,
    onShowGraphDeltaChange: (Boolean) -> Unit,
    showGraphOptions: Boolean,
    onShowGraphOptionsChange: (Boolean) -> Unit,
    onViewportSummaryChanged: (GraphViewportSummary) -> Unit,
    availableGraphSeries: List<GraphSeries>,
    selectedGraphSeries: GraphSeries,
    onSelectedGraphSeriesChange: (GraphSeries) -> Unit,
    availableGraphSeriesX: List<GraphSeries>,
    selectedGraphSeriesX: GraphSeries,
    onSelectedGraphSeriesXChange: (GraphSeries) -> Unit,
    selectedGraphSampleIndex: Int,
    onSelectedGraphSampleIndexChange: (Int) -> Unit,
    onExitAnalysisMode: () -> Unit,
    graphProfileMode: GraphProfileMode,
    onGraphProfileModeChange: (GraphProfileMode) -> Unit,
    graphDisplayMode: GraphDisplayMode,
    onGraphDisplayModeChange: (GraphDisplayMode) -> Unit,
    blendAlpha: Float,
    onBlendAlphaChange: (Float) -> Unit,
    selectedProfileResult: ProfileResult?,
    photogateAFraction: Float,
    onPhotogateAFractionChange: (Float) -> Unit,
    photogateBFraction: Float,
    onPhotogateBFractionChange: (Float) -> Unit,
    photogateA: PhotogateCrossing,
    photogateB: PhotogateCrossing,
    videoPhotogateDeltaUs: Long?,
    currentTwoBodySample: TwoBodySample?,
    displayedSeparationUnitFactor: Double,
    minimumSeparation: Double?,
    maximumSeparation: Double?,
    averageSeparation: Double?,
    currentRelativeSpeed: Double?,
    minimumRelativeSpeed: Double?,
    maximumRelativeSpeed: Double?,
    averageRelativeSpeed: Double?,
    twoBodySamplesSize: Int,
    calibrationUnit: String,
    closestApproachSample: TwoBodySample?,
    closestApproachMomentumA: Double?,
    closestApproachMomentumB: Double?,
    closestApproachTotalMomentum: Double?,
    closestApproachRelativeMomentum: Double?,
    interactionClassification: String?,
    preInteractionSnapshot: InteractionSnapshot?,
    postInteractionSnapshot: InteractionSnapshot?,
    gateAToClosestUs: Long?,
    closestToGateBUs: Long?,
    onFocusClosestApproach: () -> Unit,
    onFocusTimestampUs: (Long) -> Unit,
    currentAbsoluteTimeUs: Long,
    trackAMomentum: Double?,
    trackBMomentum: Double?,
    totalMomentum: Double?,
    relativeMomentum: Double?,
    momentumUnitLabel: String,
    showAnalysisGuides: Boolean,
    lightMode: Boolean,
    lightWorkspaceTab: LightWorkspaceTab,
    onLightWorkspaceTabSelected: (LightWorkspaceTab) -> Unit,
    lightPanelMode: LightPanelMode,
    onLightPanelModeSelected: (LightPanelMode) -> Unit,
    lightCompareSource: LightCompareSource,
    onLightCompareSourceSelected: (LightCompareSource) -> Unit,
    lightTool: LightTool,
    onLightToolSelected: (LightTool) -> Unit,
    lightChannel: LightChannel,
    onLightChannelSelected: (LightChannel) -> Unit,
    lightDualMode: Boolean,
    onLightDualModeChange: (Boolean) -> Unit,
    lightActiveSlot: LightSlot,
    onLightActiveSlotSelected: (LightSlot) -> Unit,
    showLightGraphA: Boolean,
    showLightGraphB: Boolean,
    showLightGraphC: Boolean,
    showLightGraphD: Boolean,
    onShowLightGraphAChange: (Boolean) -> Unit,
    onShowLightGraphBChange: (Boolean) -> Unit,
    onShowLightGraphCChange: (Boolean) -> Unit,
    onShowLightGraphDChange: (Boolean) -> Unit,
    showLightTableA: Boolean,
    showLightTableB: Boolean,
    showLightTableC: Boolean,
    showLightTableD: Boolean,
    onShowLightTableAChange: (Boolean) -> Unit,
    onShowLightTableBChange: (Boolean) -> Unit,
    onShowLightTableCChange: (Boolean) -> Unit,
    onShowLightTableDChange: (Boolean) -> Unit,
    lightBackgroundSubtract: Boolean,
    onLightBackgroundSubtractChange: (Boolean) -> Unit,
    lightSmooth: Boolean,
    onLightSmoothChange: (Boolean) -> Unit,
    lightDetectPeaks: Boolean,
    onLightDetectPeaksChange: (Boolean) -> Unit,
    lightTimeRecording: Boolean,
    onStartLightTimeRecording: () -> Unit,
    onStopLightTimeRecording: () -> Unit,
    onClearAllLightTimeSeries: () -> Unit,
    onLightExportCsv: () -> Unit,
    onLightExportXlsx: () -> Unit,
    onLightExportPdf: () -> Unit,
    onAnalysisExportCsv: () -> Unit,
    onAnalysisExportXlsx: () -> Unit,
    onAnalysisExportPdf: () -> Unit,
    lightUnitLabel: String,
    lightGeometryA: LightGeometry?,
    lightGeometryB: LightGeometry?,
    lightGeometryC: LightGeometry?,
    lightGeometryD: LightGeometry?,
    lightProfileA: List<LightSamplePoint>,
    lightProfileB: List<LightSamplePoint>,
    lightProfileC: List<LightSamplePoint>,
    lightProfileD: List<LightSamplePoint>,
    lightTimeSeriesA: List<LightTimePoint>,
    lightTimeSeriesB: List<LightTimePoint>,
    lightTimeSeriesC: List<LightTimePoint>,
    lightTimeSeriesD: List<LightTimePoint>,
    onClearLightTimeSeries: (LightSlot) -> Unit,
    graphFullScreen: Boolean = false,
    onGraphFullScreenChange: (Boolean) -> Unit = {}
) {
    if (!videoFocusMode && ((dataMode && showDataTable) || showLandscapeLiveGraph)) {
        val resolvedDataPanelMode = if (showLandscapeLiveGraph && trackMode && dataPanelMode != DataPanelMode.ANALYSIS) {
            DataPanelMode.GRAPH
        } else {
            dataPanelMode
        }
        when (resolvedDataPanelMode) {
            DataPanelMode.TABLE -> {
                FloatingKinematicsPanel(
                    displayedUnit = displayedUnit,
                    displayTablePoints = visibleTablePoints,
                    totalPointCount = selectedTableSourcePointsCount,
                    showTrackColumn = twoTrackMode && showTableTrackA && showTableTrackB,
                    trackAColor = trackAColor,
                    trackBColor = trackBColor,
                    isLivePlayback = isPlaying,
                    isLandscape = isLandscape,
                    modifier = panelModifier
                )
            }
            DataPanelMode.GRAPH,
            DataPanelMode.ANALYSIS -> {
                TrackGraphOverlay(
                    analysisMode = resolvedDataPanelMode == DataPanelMode.ANALYSIS,
                    displayedUnit = displayedUnit,
                    graphPlotPoints = selectedGraphPlotPoints,
                    totalPointCount = totalGraphPointCount,
                    isLivePlayback = isPlaying,
                    graphFullScreen = graphFullScreen,
                    onGraphFullScreenChange = onGraphFullScreenChange,
                    compareTrackAPlotPoints = compareTrackAGraphPoints,
                    compareTrackBPlotPoints = compareTrackBGraphPoints,
                    primaryTrackColor = primaryGraphTrackColor,
                    trackAColor = trackAColor,
                    trackBColor = trackBColor,
                    showCompareTrackA = showGraphTrackA,
                    showCompareTrackB = showGraphTrackB,
                    showCompareDelta = showGraphDelta,
                    onShowCompareTrackAChange = onShowGraphTrackAChange,
                    onShowCompareTrackBChange = onShowGraphTrackBChange,
                    onShowCompareDeltaChange = onShowGraphDeltaChange,
                    showOptionPanels = showGraphOptions,
                    onShowOptionPanelsChange = onShowGraphOptionsChange,
                    onViewportSummaryChanged = onViewportSummaryChanged,
                    availableSeries = availableGraphSeries,
                    selectedSeries = selectedGraphSeries,
                    onSeriesSelected = onSelectedGraphSeriesChange,
                    availableSeriesX = availableGraphSeriesX,
                    selectedSeriesX = selectedGraphSeriesX,
                    onSeriesXSelected = onSelectedGraphSeriesXChange,
                    selectedSampleIndex = selectedGraphSampleIndex,
                    onSampleSelected = onSelectedGraphSampleIndexChange,
                    onExitAnalysisMode = onExitAnalysisMode,
                    profileMode = graphProfileMode,
                    onProfileModeSelected = onGraphProfileModeChange,
                    graphDisplayMode = graphDisplayMode,
                    onGraphDisplayModeChange = onGraphDisplayModeChange,
                    blendAlpha = blendAlpha,
                    onBlendAlphaChange = onBlendAlphaChange,
                    profileResult = selectedProfileResult,
                    photogateAFraction = photogateAFraction,
                    onPhotogateAFractionChange = onPhotogateAFractionChange,
                    photogateBFraction = photogateBFraction,
                    onPhotogateBFractionChange = onPhotogateBFractionChange,
                    photogateA = photogateA,
                    photogateB = photogateB,
                    photogateDeltaUs = videoPhotogateDeltaUs,
                    twoBodyCurrentSeparation = currentTwoBodySample?.separationMeters?.div(displayedSeparationUnitFactor).takeIf { twoTrackMode },
                    twoBodyMinimumSeparation = minimumSeparation.takeIf { twoTrackMode },
                    twoBodyMaximumSeparation = maximumSeparation.takeIf { twoTrackMode },
                    twoBodyAverageSeparation = averageSeparation.takeIf { twoTrackMode },
                    twoBodyCurrentRelativeSpeed = currentRelativeSpeed.takeIf { twoTrackMode },
                    twoBodyMinimumRelativeSpeed = minimumRelativeSpeed.takeIf { twoTrackMode },
                    twoBodyMaximumRelativeSpeed = maximumRelativeSpeed.takeIf { twoTrackMode },
                    twoBodyAverageRelativeSpeed = averageRelativeSpeed.takeIf { twoTrackMode },
                    twoBodyFrameCount = if (twoTrackMode) twoBodySamplesSize else 0,
                    twoBodyUnit = calibrationUnit,
                    closestApproachTimeUs = closestApproachSample?.timestampUs.takeIf { twoTrackMode },
                    closestApproachFrame = closestApproachSample?.frameNumber.takeIf { twoTrackMode },
                    closestApproachSeparation = closestApproachSample?.separationMeters?.div(displayedSeparationUnitFactor).takeIf { twoTrackMode },
                    closestApproachRelativeSpeed = closestApproachSample?.relativeSpeedMetersPerSec?.div(displayedSeparationUnitFactor).takeIf { twoTrackMode },
                    closestApproachMomentumA = closestApproachMomentumA.takeIf { twoTrackMode },
                    closestApproachMomentumB = closestApproachMomentumB.takeIf { twoTrackMode },
                    closestApproachTotalMomentum = closestApproachTotalMomentum.takeIf { twoTrackMode },
                    closestApproachRelativeMomentum = closestApproachRelativeMomentum.takeIf { twoTrackMode },
                    interactionClass = interactionClassification.takeIf { twoTrackMode },
                    preInteractionSnapshot = preInteractionSnapshot.takeIf { twoTrackMode },
                    postInteractionSnapshot = postInteractionSnapshot.takeIf { twoTrackMode },
                    gateAToClosestUs = gateAToClosestUs.takeIf { twoTrackMode },
                    closestToGateBUs = closestToGateBUs.takeIf { twoTrackMode },
                    onFocusClosestApproach = onFocusClosestApproach,
                    onFocusTimestampUs = onFocusTimestampUs,
                    currentAbsoluteTimeUs = currentAbsoluteTimeUs,
                    trackAMomentum = trackAMomentum,
                    trackBMomentum = trackBMomentum,
                    totalMomentum = totalMomentum,
                    relativeMomentum = relativeMomentum,
                    momentumUnitLabel = momentumUnitLabel,
                    showAnalysisGuides = showAnalysisGuides,
                    onExportCsv = onAnalysisExportCsv,
                    onExportXlsx = onAnalysisExportXlsx,
                    onExportPdf = onAnalysisExportPdf,
                    isLandscape = isLandscape,
                    modifier = panelModifier
                )
            }
        }
    }

    if (!videoFocusMode && lightMode && lightWorkspaceTab != LightWorkspaceTab.SELECT) {
        FloatingLightPanel(
            workspaceTab = lightWorkspaceTab,
            onWorkspaceTabSelected = onLightWorkspaceTabSelected,
            mode = lightPanelMode,
            onModeSelected = onLightPanelModeSelected,
            compareSource = lightCompareSource,
            onCompareSourceSelected = onLightCompareSourceSelected,
            tool = lightTool,
            onToolSelected = onLightToolSelected,
            channel = lightChannel,
            onChannelSelected = onLightChannelSelected,
            dualMode = lightDualMode,
            onDualModeChange = onLightDualModeChange,
            activeSlot = lightActiveSlot,
            onActiveSlotSelected = onLightActiveSlotSelected,
            showGraphA = showLightGraphA,
            showGraphB = showLightGraphB,
            showGraphC = showLightGraphC,
            showGraphD = showLightGraphD,
            onShowGraphAChange = onShowLightGraphAChange,
            onShowGraphBChange = onShowLightGraphBChange,
            onShowGraphCChange = onShowLightGraphCChange,
            onShowGraphDChange = onShowLightGraphDChange,
            showTableA = showLightTableA,
            showTableB = showLightTableB,
            showTableC = showLightTableC,
            showTableD = showLightTableD,
            onShowTableAChange = onShowLightTableAChange,
            onShowTableBChange = onShowLightTableBChange,
            onShowTableCChange = onShowLightTableCChange,
            onShowTableDChange = onShowLightTableDChange,
            backgroundSubtract = lightBackgroundSubtract,
            onBackgroundSubtractChange = onLightBackgroundSubtractChange,
            smoothing = lightSmooth,
            onSmoothingChange = onLightSmoothChange,
            detectPeaks = lightDetectPeaks,
            onDetectPeaksChange = onLightDetectPeaksChange,
            timeRecording = lightTimeRecording,
            onStartTimeRecording = onStartLightTimeRecording,
            onStopTimeRecording = onStopLightTimeRecording,
            onClearAllTimeSeries = onClearAllLightTimeSeries,
            onExportCsv = onLightExportCsv,
            onExportXlsx = onLightExportXlsx,
            onExportPdf = onLightExportPdf,
            showOptions = lightWorkspaceTab == LightWorkspaceTab.MORE,
            onShowOptionsChange = { onLightWorkspaceTabSelected(if (it) LightWorkspaceTab.MORE else LightWorkspaceTab.GRAPH) },
            slotAColor = trackAColor,
            slotBColor = trackBColor,
            slotCColor = Color(0xFF9B59B6),
            slotDColor = Color(0xFF3498DB),
            unitLabel = lightUnitLabel,
            geometryA = lightGeometryA,
            geometryB = lightGeometryB,
            geometryC = lightGeometryC,
            geometryD = lightGeometryD,
            profileA = lightProfileA,
            profileB = lightProfileB,
            profileC = lightProfileC,
            profileD = lightProfileD,
            timeA = lightTimeSeriesA,
            timeB = lightTimeSeriesB,
            timeC = lightTimeSeriesC,
            timeD = lightTimeSeriesD,
            onClearTimeSeries = onClearLightTimeSeries,
            modifier = panelModifier
        )
    }
}

@Composable
private fun TrackDataTray(
    showDataTable: Boolean,
    dataPanelMode: DataPanelMode,
    twoTrackMode: Boolean,
    showGraphTrackA: Boolean,
    showGraphTrackB: Boolean,
    showTableTrackA: Boolean,
    showTableTrackB: Boolean,
    trackAColor: Color,
    trackBColor: Color,
    selectedGraphPointId: Long?,
    onGraphMode: () -> Unit,
    onTableMode: () -> Unit,
    onAnalysisMode: () -> Unit,
    onRemoveSelectedGraphPoint: () -> Unit,
    onToggleShowData: () -> Unit,
    onGraphViewA: () -> Unit,
    onGraphViewB: () -> Unit,
    onGraphViewBoth: () -> Unit,
    onTableViewA: () -> Unit,
    onTableViewB: () -> Unit,
    onTableViewBoth: () -> Unit,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit,
    onExportPdf: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomDeckButton("GRAPH", selected = showDataTable && dataPanelMode == DataPanelMode.GRAPH, accent = LabAccent) { onGraphMode() }
            BottomDeckButton("TABLE", selected = showDataTable && dataPanelMode == DataPanelMode.TABLE, accent = LabSuccess) { onTableMode() }
            BottomDeckButton("ANALYSIS", selected = showDataTable && dataPanelMode == DataPanelMode.ANALYSIS, accent = LabGold) { onAnalysisMode() }
            if (selectedGraphPointId != null) {
                BottomDeckButton("REMOVE", enabled = true, accent = LabDanger) { onRemoveSelectedGraphPoint() }
            }
        }
        if (twoTrackMode) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dataPanelMode == DataPanelMode.TABLE) {
                    BottomDeckButton("A", selected = showTableTrackA && !showTableTrackB, accent = trackAColor) { onTableViewA() }
                    BottomDeckButton("B", selected = showTableTrackB && !showTableTrackA, accent = trackBColor) { onTableViewB() }
                    BottomDeckButton("BOTH", selected = showTableTrackA && showTableTrackB, accent = LabGold) { onTableViewBoth() }
                } else {
                    BottomDeckButton("A", selected = showGraphTrackA && !showGraphTrackB, accent = trackAColor) { onGraphViewA() }
                    BottomDeckButton("B", selected = showGraphTrackB && !showGraphTrackA, accent = trackBColor) { onGraphViewB() }
                    BottomDeckButton("BOTH", selected = showGraphTrackA && showGraphTrackB, accent = LabGold) { onGraphViewBoth() }
                }
            }
        }
        }
    }
@Composable
private fun TrackGraphOverlay(
    analysisMode: Boolean,
    displayedUnit: String,
    graphPlotPoints: List<GraphPlotPoint>,
    totalPointCount: Int,
    isLivePlayback: Boolean,
    compareTrackAPlotPoints: List<GraphPlotPoint>,
    compareTrackBPlotPoints: List<GraphPlotPoint>,
    primaryTrackColor: Color,
    trackAColor: Color,
    trackBColor: Color,
    showCompareTrackA: Boolean,
    showCompareTrackB: Boolean,
    showCompareDelta: Boolean,
    onShowCompareTrackAChange: (Boolean) -> Unit,
    onShowCompareTrackBChange: (Boolean) -> Unit,
    onShowCompareDeltaChange: (Boolean) -> Unit,
    showOptionPanels: Boolean,
    onShowOptionPanelsChange: (Boolean) -> Unit,
    onViewportSummaryChanged: (GraphViewportSummary) -> Unit,
    availableSeries: List<GraphSeries>,
    selectedSeries: GraphSeries,
    onSeriesSelected: (GraphSeries) -> Unit,
    availableSeriesX: List<GraphSeries>,
    selectedSeriesX: GraphSeries,
    onSeriesXSelected: (GraphSeries) -> Unit,
    selectedSampleIndex: Int,
    onSampleSelected: (Int) -> Unit,
    onExitAnalysisMode: () -> Unit = {},
    profileMode: GraphProfileMode,
    onProfileModeSelected: (GraphProfileMode) -> Unit,
    graphDisplayMode: GraphDisplayMode,
    onGraphDisplayModeChange: (GraphDisplayMode) -> Unit,
    blendAlpha: Float,
    onBlendAlphaChange: (Float) -> Unit,
    profileResult: ProfileResult?,
    photogateAFraction: Float,
    onPhotogateAFractionChange: (Float) -> Unit,
    photogateBFraction: Float,
    onPhotogateBFractionChange: (Float) -> Unit,
    photogateA: PhotogateCrossing,
    photogateB: PhotogateCrossing,
    photogateDeltaUs: Long?,
    twoBodyCurrentSeparation: Double?,
    twoBodyMinimumSeparation: Double?,
    twoBodyMaximumSeparation: Double?,
    twoBodyAverageSeparation: Double?,
    twoBodyCurrentRelativeSpeed: Double?,
    twoBodyMinimumRelativeSpeed: Double?,
    twoBodyMaximumRelativeSpeed: Double?,
    twoBodyAverageRelativeSpeed: Double?,
    twoBodyFrameCount: Int,
    twoBodyUnit: String,
    closestApproachTimeUs: Long?,
    closestApproachFrame: Int?,
    closestApproachSeparation: Double?,
    closestApproachRelativeSpeed: Double?,
    closestApproachMomentumA: Double?,
    closestApproachMomentumB: Double?,
    closestApproachTotalMomentum: Double?,
    closestApproachRelativeMomentum: Double?,
    interactionClass: String?,
    preInteractionSnapshot: InteractionSnapshot?,
    postInteractionSnapshot: InteractionSnapshot?,
    gateAToClosestUs: Long?,
    closestToGateBUs: Long?,
    onFocusClosestApproach: () -> Unit,
    onFocusTimestampUs: (Long) -> Unit,
    currentAbsoluteTimeUs: Long,
    trackAMomentum: Double?,
    trackBMomentum: Double?,
    totalMomentum: Double?,
    relativeMomentum: Double?,
    momentumUnitLabel: String,
    showAnalysisGuides: Boolean,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit,
    onExportPdf: () -> Unit,
    isLandscape: Boolean,
    graphFullScreen: Boolean = false,
    onGraphFullScreenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    FloatingGraphPanel(
        displayedUnit = displayedUnit,
        graphPlotPoints = graphPlotPoints,
        totalPointCount = totalPointCount,
        isLivePlayback = isLivePlayback,
        graphFullScreen = graphFullScreen,
        onGraphFullScreenChange = onGraphFullScreenChange,
        compareTrackAPlotPoints = compareTrackAPlotPoints,
        compareTrackBPlotPoints = compareTrackBPlotPoints,
        primaryTrackColor = primaryTrackColor,
        trackAColor = trackAColor,
        trackBColor = trackBColor,
        showCompareTrackA = showCompareTrackA,
        showCompareTrackB = showCompareTrackB,
        showCompareDelta = showCompareDelta,
        onShowCompareTrackAChange = onShowCompareTrackAChange,
        onShowCompareTrackBChange = onShowCompareTrackBChange,
        onShowCompareDeltaChange = onShowCompareDeltaChange,
        showOptionPanels = showOptionPanels,
        onShowOptionPanelsChange = onShowOptionPanelsChange,
        onViewportSummaryChanged = onViewportSummaryChanged,
        availableSeries = availableSeries,
        selectedSeries = selectedSeries,
        onSeriesSelected = onSeriesSelected,
        availableSeriesX = availableSeriesX,
        selectedSeriesX = selectedSeriesX,
        onSeriesXSelected = onSeriesXSelected,
        selectedSampleIndex = selectedSampleIndex,
        onSampleSelected = onSampleSelected,
        onExitAnalysisMode = onExitAnalysisMode,
        profileMode = profileMode,
        onProfileModeSelected = onProfileModeSelected,
        graphDisplayMode = graphDisplayMode,
        onGraphDisplayModeChange = onGraphDisplayModeChange,
        blendAlpha = blendAlpha,
        onBlendAlphaChange = onBlendAlphaChange,
        profileResult = profileResult,
        photogateAFraction = photogateAFraction,
        onPhotogateAFractionChange = onPhotogateAFractionChange,
        photogateBFraction = photogateBFraction,
        onPhotogateBFractionChange = onPhotogateBFractionChange,
        photogateA = photogateA,
        photogateB = photogateB,
        photogateDeltaUs = photogateDeltaUs,
        twoBodyCurrentSeparation = twoBodyCurrentSeparation,
        twoBodyMinimumSeparation = twoBodyMinimumSeparation,
        twoBodyMaximumSeparation = twoBodyMaximumSeparation,
        twoBodyAverageSeparation = twoBodyAverageSeparation,
        twoBodyCurrentRelativeSpeed = twoBodyCurrentRelativeSpeed,
        twoBodyMinimumRelativeSpeed = twoBodyMinimumRelativeSpeed,
        twoBodyMaximumRelativeSpeed = twoBodyMaximumRelativeSpeed,
        twoBodyAverageRelativeSpeed = twoBodyAverageRelativeSpeed,
        twoBodyFrameCount = twoBodyFrameCount,
        twoBodyUnit = twoBodyUnit,
        closestApproachTimeUs = closestApproachTimeUs,
        closestApproachFrame = closestApproachFrame,
        closestApproachSeparation = closestApproachSeparation,
        closestApproachRelativeSpeed = closestApproachRelativeSpeed,
        closestApproachMomentumA = closestApproachMomentumA,
        closestApproachMomentumB = closestApproachMomentumB,
        closestApproachTotalMomentum = closestApproachTotalMomentum,
        closestApproachRelativeMomentum = closestApproachRelativeMomentum,
        interactionClass = interactionClass,
        preInteractionSnapshot = preInteractionSnapshot,
        postInteractionSnapshot = postInteractionSnapshot,
        gateAToClosestUs = gateAToClosestUs,
        closestToGateBUs = closestToGateBUs,
        onFocusClosestApproach = onFocusClosestApproach,
        onFocusTimestampUs = onFocusTimestampUs,
        currentAbsoluteTimeUs = currentAbsoluteTimeUs,
        trackAMomentum = trackAMomentum,
        trackBMomentum = trackBMomentum,
        totalMomentum = totalMomentum,
        relativeMomentum = relativeMomentum,
        momentumUnitLabel = momentumUnitLabel,
        showAnalysisGuides = showAnalysisGuides,
        onExportCsv = onExportCsv,
        onExportXlsx = onExportXlsx,
        onExportPdf = onExportPdf,
        analysisMode = analysisMode,
        isLandscape = isLandscape,
        modifier = modifier
    )
}

@Composable
private fun TrackBottomTrayContent(
    activeTrack: ActiveTrack,
    twoTrackMode: Boolean,
    autoTrackEnabled: Boolean,
    hasPreparedAutoTrack: Boolean,
    trackTrayPanel: TrackTrayPanel?,
    selectedPointId: Long,
    pendingSeedScreenPoint: Offset?,
    disableExistingPointSelection: Boolean,
    seedLockMode: SeedLockMode,
    trackingStepOption: TrackingStepOption,
    showDataTable: Boolean,
    dataPanelMode: DataPanelMode,
    showGraphTrackA: Boolean,
    showGraphTrackB: Boolean,
    showTableTrackA: Boolean,
    showTableTrackB: Boolean,
    selectedGraphPointId: Long?,
    trackAColor: Color,
    trackBColor: Color,
    onAutoMode: () -> Unit,
    onManualMode: () -> Unit,
    onToggleTarget: () -> Unit,
    onToggleRun: () -> Unit,
    onToggleStep: () -> Unit,
    onToggleData: () -> Unit,
    onShowClearConfirm: () -> Unit,
    onDeleteSelectedPoint: () -> Unit,
    onSelectTrackA: () -> Unit,
    onSelectTrackB: () -> Unit,
    onBeginAutoSelect: () -> Unit,
    onConfirmAutoSeed: () -> Unit,
    onCancelAutoSeed: () -> Unit,
    onSetExactSeedLock: () -> Unit,
    onSetSnapSeedLock: () -> Unit,
    onConfirmManualSelection: () -> Unit,
    onTogglePointPick: () -> Unit,
    onNextAutoTrack: () -> Unit,
    onTrackAllAuto: () -> Unit,
    autoTrackRunning: Boolean,
    onStopAutoTrack: () -> Unit,
    onTrackingStepSelected: (TrackingStepOption) -> Unit,
    onGraphMode: () -> Unit,
    onTableMode: () -> Unit,
    onAnalysisMode: () -> Unit,
    onRemoveSelectedGraphPoint: () -> Unit,
    onToggleShowData: () -> Unit,
    onGraphViewA: () -> Unit,
    onGraphViewB: () -> Unit,
    onGraphViewBoth: () -> Unit,
    onTableViewA: () -> Unit,
    onTableViewB: () -> Unit,
    onTableViewBoth: () -> Unit,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit,
    onExportPdf: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomDeckButton("AUTO", selected = autoTrackEnabled, accent = LabGold) { onAutoMode() }
            BottomDeckButton("MANUAL", selected = !autoTrackEnabled, accent = LabSurfaceStrong) { onManualMode() }
            BottomDeckButton("TARGET", selected = trackTrayPanel == TrackTrayPanel.TARGET, accent = LabAccent) { onToggleTarget() }
            if (autoTrackEnabled && hasPreparedAutoTrack) {
                BottomDeckButton("RUN", selected = trackTrayPanel == TrackTrayPanel.AUTO_RUN, accent = LabSuccess) { onToggleRun() }
            }
            BottomDeckButton("STEP", selected = trackTrayPanel == TrackTrayPanel.STEP, accent = LabGold) { onToggleStep() }
            BottomDeckButton("DATA", selected = trackTrayPanel == TrackTrayPanel.DATA, accent = LabAccent) { onToggleData() }
            BottomDeckButton("CLEAR", accent = LabDanger) { onShowClearConfirm() }
            BottomDeckButton("REMOVE", enabled = selectedPointId != -1L, accent = LabDanger) { onDeleteSelectedPoint() }
        }
        if (twoTrackMode) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomDeckButton("A", selected = activeTrack == ActiveTrack.A, accent = trackAColor) { onSelectTrackA() }
                BottomDeckButton("B", selected = activeTrack == ActiveTrack.B, accent = trackBColor) { onSelectTrackB() }
            }
        }
        when (trackTrayPanel) {
            TrackTrayPanel.TARGET -> {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (autoTrackEnabled) {
                        BottomDeckButton(if (pendingSeedScreenPoint == null) "SELECT" else "ADJUST", accent = LabAccent) { onBeginAutoSelect() }
                        BottomDeckButton("CONFIRM", enabled = pendingSeedScreenPoint != null, accent = LabSuccess) { onConfirmAutoSeed() }
                        BottomDeckButton("CANCEL", enabled = pendingSeedScreenPoint != null, accent = LabDanger) { onCancelAutoSeed() }
                        BottomDeckButton("EXACT", selected = seedLockMode == SeedLockMode.EXACT_PIXEL, accent = LabSuccess) { onSetExactSeedLock() }
                        BottomDeckButton("SNAP", selected = seedLockMode == SeedLockMode.SNAP_OBJECT, accent = LabGold) { onSetSnapSeedLock() }
                    } else {
                        BottomDeckButton("SELECTED", enabled = selectedPointId != -1L, accent = LabSuccess) { onConfirmManualSelection() }
                        BottomDeckButton(if (disableExistingPointSelection) "NO PICK" else "POINT PICK", selected = disableExistingPointSelection, accent = LabGold) { onTogglePointPick() }
                        BottomDeckButton("REMOVE", enabled = selectedPointId != -1L, accent = LabDanger) { onDeleteSelectedPoint() }
                        BottomDeckButton("CLEAR", accent = LabDanger) { onShowClearConfirm() }
                    }
                }
            }
            TrackTrayPanel.AUTO_RUN -> {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (autoTrackEnabled) {
                        BottomDeckButton("NEXT", enabled = hasPreparedAutoTrack && !autoTrackRunning, accent = LabAccent) { onNextAutoTrack() }
                        BottomDeckButton(
                            if (autoTrackRunning) "STOP" else "TRACK ALL",
                            selected = autoTrackRunning,
                            enabled = hasPreparedAutoTrack,
                            accent = LabDanger
                        ) {
                            if (autoTrackRunning) onStopAutoTrack() else onTrackAllAuto()
                        }
                    }
                }
            }
            TrackTrayPanel.STEP -> {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackingStepOption.entries.forEach { option ->
                        BottomDeckButton(option.label, selected = trackingStepOption == option, accent = LabGold) {
                            onTrackingStepSelected(option)
                        }
                    }
                }
            }
            TrackTrayPanel.DATA -> {
                TrackDataTray(
                    showDataTable = showDataTable,
                    dataPanelMode = dataPanelMode,
                    twoTrackMode = twoTrackMode,
                    showGraphTrackA = showGraphTrackA,
                    showGraphTrackB = showGraphTrackB,
                    showTableTrackA = showTableTrackA,
                    showTableTrackB = showTableTrackB,
                    trackAColor = trackAColor,
                    trackBColor = trackBColor,
                    selectedGraphPointId = selectedGraphPointId,
                    onGraphMode = onGraphMode,
                    onTableMode = onTableMode,
                    onAnalysisMode = onAnalysisMode,
                    onRemoveSelectedGraphPoint = onRemoveSelectedGraphPoint,
                    onToggleShowData = onToggleShowData,
                    onGraphViewA = onGraphViewA,
                    onGraphViewB = onGraphViewB,
                    onGraphViewBoth = onGraphViewBoth,
                    onTableViewA = onTableViewA,
                    onTableViewB = onTableViewB,
                    onTableViewBoth = onTableViewBoth,
                    onExportCsv = onExportCsv,
                    onExportXlsx = onExportXlsx,
                    onExportPdf = onExportPdf
                )
            }
            null -> Unit
        }
    }
}

@Composable
private fun MoreBottomTrayContent(
    moreTrayPanel: MoreTrayPanel?,
    showMagnifier: Boolean,
    magnifierZoom: Float,
    videoScale: Float,
    videoScaleSlider: Float,
    videoZoomEnabled: Boolean,
    maxPanX: Float,
    maxPanY: Float,
    videoOffsetX: Float,
    videoOffsetY: Float,
    precisionStepPx: Float,
    precisionTargetPosition: Offset?,
    precisionTargetLabel: String,
    canConfirmPrecisionTarget: Boolean,
    forceLandscapeMode: Boolean,
    twoTrackMode: Boolean,
    showTimeline: Boolean,
    showAnalysisGuides: Boolean,
    pointHistoryMode: PointHistoryMode,
    activeOverlayRole: OverlayColorRole,
    trackAColorArgb: Int,
    trackBColorArgb: Int,
    manualSelectorColorArgb: Int,
    vectorColorArgb: Int,
    calibrationColorArgb: Int,
    onToggleEdit: () -> Unit,
    onToggleMag: () -> Unit,
    onToggleView: () -> Unit,
    onToggleColors: () -> Unit,
    onToggleExport: () -> Unit,
    onResetTrim: () -> Unit,
    onTrimStart: () -> Unit,
    onTrimEnd: () -> Unit,
    onShowMagnifierChange: (Boolean) -> Unit,
    onMagnifierZoomChange: (Float) -> Unit,
    onVideoScaleChange: (Float) -> Unit,
    onVideoOffsetXChange: (Float) -> Unit,
    onVideoOffsetYChange: (Float) -> Unit,
    onPrecisionStepChange: (Float) -> Unit,
    onNudgePrecision: (Float, Float) -> Unit,
    onConfirmPrecisionTarget: () -> Unit,
    onForceLandscapeModeChange: (Boolean) -> Unit,
    onToggleTrackCountMode: () -> Unit,
    onShowTimelineChange: (Boolean) -> Unit,
    onShowAnalysisGuidesChange: (Boolean) -> Unit,
    onPointHistoryModeChange: (PointHistoryMode) -> Unit,
    onActiveOverlayRoleChange: (OverlayColorRole) -> Unit,
    onOverlayColorSelected: (OverlayColorRole, Color) -> Unit,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit,
    onExportPdf: () -> Unit,
    onShareCsv: () -> Unit,
    onSharePdf: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomDeckButton("MAG", selected = moreTrayPanel == MoreTrayPanel.MAG, accent = LabAccent) { onToggleMag() }
            BottomDeckButton("VIEW", selected = moreTrayPanel == MoreTrayPanel.VIEW, accent = LabGold) { onToggleView() }
            BottomDeckButton("CLR", selected = moreTrayPanel == MoreTrayPanel.COLORS, accent = LabAccent) { onToggleColors() }
            BottomDeckButton("OUT", selected = moreTrayPanel == MoreTrayPanel.EXPORT, accent = LabSuccess) { onToggleExport() }
            BottomDeckButton("CUT", selected = moreTrayPanel == MoreTrayPanel.EDIT, accent = LabDanger) { onToggleEdit() }
        }
        when (moreTrayPanel) {
            MoreTrayPanel.EDIT -> {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomDeckButton("RESET", accent = LabSurfaceStrong) { onResetTrim() }
                    BottomDeckButton("TRIM START", accent = LabSurfaceStrong) { onTrimStart() }
                    BottomDeckButton("TRIM END", accent = LabSurfaceStrong) { onTrimEnd() }
                }
            }
            MoreTrayPanel.MAG -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomDeckButton(if (showMagnifier) "MAG ON" else "MAG OFF", selected = showMagnifier, accent = LabAccent) {
                            onShowMagnifierChange(!showMagnifier)
                        }
                        Text(
                            "${String.format(Locale.US, "%.1f", magnifierZoom)}x  /  ${String.format(Locale.US, "%.2f", videoScale)}x",
                            color = PhysicsColorOnSurface,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Surface(
                        color = PhysicsColorSurface.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "PRECISION PIXEL EDITOR",
                                    color = PhysicsColorOnSurface,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    precisionTargetLabel,
                                    color = if (precisionTargetPosition != null) PhysicsColorAccent else PhysicsColorOnSurface.copy(alpha = 0.45f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(1f, 2f, 5f).forEach { step ->
                                    OutlinedButton(
                                        onClick = { onPrecisionStepChange(step) },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (abs(precisionStepPx - step) < 0.01f) PhysicsColorAccent else PhysicsColorDivider
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (abs(precisionStepPx - step) < 0.01f) PhysicsColorAccent.copy(alpha = 0.14f) else PhysicsColorSurfaceVariant.copy(alpha = 0.52f),
                                            contentColor = if (abs(precisionStepPx - step) < 0.01f) PhysicsColorAccent else PhysicsColorOnSurface
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("${step.roundToInt()} px", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PrecisionDirectionPad(
                                    enabled = precisionTargetPosition != null,
                                    stepPx = precisionStepPx,
                                    onNudge = onNudgePrecision,
                                    onConfirm = onConfirmPrecisionTarget
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        precisionTargetPosition?.let {
                                            "X ${it.x.roundToInt()}   Y ${it.y.roundToInt()}"
                                        } ?: "Select a point to nudge",
                                        color = PhysicsColorOnSurface,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        "Tap moves one step. Hold repeats.",
                                        color = PhysicsColorOnSurface.copy(alpha = 0.62f),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    FilledTonalButton(
                                        onClick = onConfirmPrecisionTarget,
                                        enabled = canConfirmPrecisionTarget,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = PhysicsColorSurfaceVariant,
                                            contentColor = PhysicsColorOnBackground
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("CONFIRM TARGET", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                    Slider(
                        value = magnifierZoom,
                        onValueChange = { onMagnifierZoomChange(it) },
                        valueRange = 2f..30f,
                        colors = SliderDefaults.colors(thumbColor = LabAccent, activeTrackColor = LabAccent, inactiveTrackColor = PhysicsColorDivider)
                    )
                    Slider(
                        value = videoScaleSlider,
                        onValueChange = { onVideoScaleChange(it) },
                        valueRange = 1f..6f,
                        colors = SliderDefaults.colors(thumbColor = LabGold, activeTrackColor = LabGold, inactiveTrackColor = PhysicsColorDivider)
                    )
                    if (videoZoomEnabled && videoScale > 1.02f) {
                        Slider(
                            value = (videoOffsetX / maxPanX.coerceAtLeast(1f)).coerceIn(-1f, 1f),
                            onValueChange = { onVideoOffsetXChange(it) },
                            valueRange = -1f..1f,
                            colors = SliderDefaults.colors(thumbColor = LabAccent, activeTrackColor = LabAccent, inactiveTrackColor = PhysicsColorDivider)
                        )
                        Slider(
                            value = (videoOffsetY / maxPanY.coerceAtLeast(1f)).coerceIn(-1f, 1f),
                            onValueChange = { onVideoOffsetYChange(it) },
                            valueRange = -1f..1f,
                            colors = SliderDefaults.colors(thumbColor = LabGold, activeTrackColor = LabGold, inactiveTrackColor = PhysicsColorDivider)
                        )
                    }
                }
            }
            MoreTrayPanel.VIEW -> {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomDeckButton(if (twoTrackMode) "TWO TRACKS" else "SINGLE TRACK", selected = twoTrackMode, accent = LabAccent) {
                        onToggleTrackCountMode()
                    }
                    BottomDeckButton(if (showTimeline) "TIMELINE BAR" else "TIMELINE HIDDEN", selected = showTimeline, accent = LabAccent) {
                        onShowTimelineChange(!showTimeline)
                    }
                    BottomDeckButton(if (showAnalysisGuides) "ANALYSIS LINES" else "LINES OFF", selected = showAnalysisGuides, accent = LabGold) {
                        onShowAnalysisGuidesChange(!showAnalysisGuides)
                    }
                    BottomDeckButton(
                        if (forceLandscapeMode) "LANDSCAPE LOCK" else "SYSTEM ROTATE",
                        selected = forceLandscapeMode,
                        accent = LabSuccess
                    ) {
                        onForceLandscapeModeChange(!forceLandscapeMode)
                    }
                    PointHistoryMode.values().forEach { mode ->
                        BottomDeckButton(
                            when (mode) {
                                PointHistoryMode.FADE -> "FADE"
                                PointHistoryMode.ALL -> "ALL"
                                PointHistoryMode.CURRENT -> "CURRENT"
                            },
                            selected = pointHistoryMode == mode,
                            accent = LabSurfaceStrong
                        ) {
                            onPointHistoryModeChange(mode)
                        }
                    }
                }
            }
            MoreTrayPanel.COLORS -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OverlayColorRole.values().forEach { role ->
                            val color = roleColor(role, trackAColorArgb, trackBColorArgb, manualSelectorColorArgb, vectorColorArgb, calibrationColorArgb)
                            BottomDeckButton(
                                label = when (role) {
                                    OverlayColorRole.TRACK_A -> "TRACK A"
                                    OverlayColorRole.TRACK_B -> "TRACK B"
                                    OverlayColorRole.MANUAL_SELECTOR -> "SELECTOR"
                                    OverlayColorRole.VECTOR -> "VECTOR"
                                    OverlayColorRole.CALIBRATION -> "CAL"
                                },
                                selected = activeOverlayRole == role,
                                accent = color
                            ) { onActiveOverlayRoleChange(role) }
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        overlayColorPresets.forEach { color ->
                            BottomDeckButton(
                                overlayColorLabel(color),
                                selected = roleColor(activeOverlayRole, trackAColorArgb, trackBColorArgb, manualSelectorColorArgb, vectorColorArgb, calibrationColorArgb).toArgb() == color.toArgb(),
                                accent = color
                            ) {
                                onOverlayColorSelected(activeOverlayRole, color)
                            }
                        }
                    }
                }
            }
            MoreTrayPanel.EXPORT -> {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomDeckButton("CSV", accent = LabSuccess) { onExportCsv() }
                    BottomDeckButton("XLSX", accent = LabSuccess) { onExportXlsx() }
                    BottomDeckButton("PDF", accent = LabSuccess) { onExportPdf() }
                    BottomDeckButton("SHARE CSV", accent = LabAccent) { onShareCsv() }
                    BottomDeckButton("SHARE PDF", accent = LabAccent) { onSharePdf() }
                }
            }
            null -> Unit
        }
    }
}

@Composable
private fun PrecisionDirectionPad(
    enabled: Boolean,
    stepPx: Float,
    onNudge: (Float, Float) -> Unit,
    onConfirm: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val directions = listOf(
        listOf("↖" to Offset(-stepPx, -stepPx), "▲" to Offset(0f, -stepPx), "↗" to Offset(stepPx, -stepPx)),
        listOf("◀" to Offset(-stepPx, 0f), "●" to null, "▶" to Offset(stepPx, 0f)),
        listOf("↙" to Offset(-stepPx, stepPx), "▼" to Offset(0f, stepPx), "↘" to Offset(stepPx, stepPx))
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        directions.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, delta) ->
                    PrecisionPadButton(
                        label = label,
                        enabled = enabled || delta == null,
                        accent = if (delta == null) LabGold else LabAccent,
                        onTap = {
                            if (delta == null) onConfirm() else onNudge(delta.x, delta.y)
                        },
                        onHoldStart = if (delta == null || !enabled) null else {
                            {
                                scope.launch {
                                    delay(260)
                                    while (true) {
                                        onNudge(delta.x, delta.y)
                                        delay(55)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PrecisionPadButton(
    label: String,
    enabled: Boolean,
    accent: Color,
    onTap: () -> Unit,
    onHoldStart: (() -> kotlinx.coroutines.Job)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    Surface(
        color = if (!enabled) PhysicsColorSurfaceVariant.copy(alpha = 0.35f)
        else if (pressed) accent.copy(alpha = 0.22f)
        else PhysicsColorSurfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) accent.copy(alpha = 0.45f) else PhysicsColorDivider),
        modifier = Modifier
            .size(54.dp)
            .pointerInput(enabled, label) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onTap()
                        val repeatJob = onHoldStart?.invoke()
                        try {
                            tryAwaitRelease()
                        } finally {
                            repeatJob?.cancel()
                            pressed = false
                        }
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                color = if (enabled) accent else PhysicsColorOnSurface.copy(alpha = 0.32f),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun FloatingKinematicsPanel(
    displayedUnit: String,
    displayTablePoints: List<DisplayKinematicsPoint>,
    totalPointCount: Int,
    showTrackColumn: Boolean,
    trackAColor: Color,
    trackBColor: Color,
    isLivePlayback: Boolean,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val listState = rememberLazyListState()
    val latestPointId = displayTablePoints.lastOrNull()?.pointId
    val progress = if (totalPointCount <= 0) 0f else displayTablePoints.size.toFloat() / totalPointCount.toFloat()
    val liveDots by produceState(initialValue = ".", isLivePlayback, totalPointCount, displayTablePoints.size) {
        if (!isLivePlayback || displayTablePoints.size >= totalPointCount) {
            value = "."
            return@produceState
        }
        val frames = listOf(".", "..", "...")
        var index = 0
        while (true) {
            value = frames[index % frames.size]
            index++
            delay(220)
        }
    }
    LaunchedEffect(displayTablePoints.size) {
        if (displayTablePoints.isNotEmpty()) {
            listState.scrollToItem(displayTablePoints.lastIndex)
        }
    }

    Surface(
        color = Color.Black.copy(alpha = 0.86f),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .heightIn(min = if (isLandscape) 220.dp else 244.dp, max = if (isLandscape) 320.dp else 360.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kinematics Table",
                    color = Color(0xFF00E5FF),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isLivePlayback && displayTablePoints.size < totalPointCount) {
                        "LIVE ${displayTablePoints.size}/$totalPointCount$liveDots"
                    } else {
                        "${displayTablePoints.size}/$totalPointCount"
                    },
                    color = if (isLivePlayback) Color(0xFF00E676) else Color.LightGray,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (isLivePlayback) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF00E676),
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (displayTablePoints.size < totalPointCount) {
                        "Scanning pixels and generating values$liveDots"
                    } else {
                        "Live generation complete"
                    },
                    color = Color(0xFF00E676),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (showTrackColumn) {
                    KinematicsHeaderText("Track", LabGold)
                }
                KinematicsHeaderText("Time", Color(0xFF00E5FF))
                KinematicsHeaderText("Gap", Color(0xFFFFB74D))
                KinematicsHeaderText("x($displayedUnit)", Color(0xFF00E5FF))
                KinematicsHeaderText("y($displayedUnit)", Color(0xFF00E5FF))
                KinematicsHeaderText("vx($displayedUnit/s)", Color(0xFF00E5FF))
                KinematicsHeaderText("vy($displayedUnit/s)", Color(0xFF00E5FF))
                KinematicsHeaderText("v($displayedUnit/s)", Color(0xFF00E5FF))
                KinematicsHeaderText("ax($displayedUnit/s²)", Color(0xFF00E5FF))
                KinematicsHeaderText("ay($displayedUnit/s²)", Color(0xFF00E5FF))
                KinematicsHeaderText("a($displayedUnit/s²)", Color(0xFF00E5FF))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (displayTablePoints.isEmpty()) {
                    Text(
                        "Track and calibrate points to populate the table.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayTablePoints, key = { it.pointId }) { point ->
                            val hasGap = point.gapLabel != null
                            val isLatest = point.pointId == latestPointId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(horizontalScrollState)
                                    .background(
                                        if (isLivePlayback && isLatest) Color(0xFF00E676).copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                if (showTrackColumn) {
                                    KinematicsRowText(point.trackLabel, if (point.trackLabel == "B") trackBColor else trackAColor)
                                }
                                KinematicsRowText(formatSeconds(point.timestampUs), Color.White)
                                KinematicsRowText(point.gapLabel ?: "-", if (hasGap) Color(0xFFFFB74D) else Color.Gray)
                                KinematicsRowText(formatCalibratedValue(point.xValue), Color.White)
                                KinematicsRowText(formatCalibratedValue(point.yValue), Color.White)
                                KinematicsRowText(formatOptionalCalibratedValue(point.vxValue), Color.White)
                                KinematicsRowText(formatOptionalCalibratedValue(point.vyValue), Color.White)
                                KinematicsRowText(formatOptionalCalibratedValue(point.speedValue), if (isLatest) Color(0xFF00E676) else Color.White)
                                KinematicsRowText(formatOptionalCalibratedValue(point.axValue), Color.White)
                                KinematicsRowText(formatOptionalCalibratedValue(point.ayValue), Color.White)
                                KinematicsRowText(formatOptionalCalibratedValue(point.accelerationValue), if (isLatest) Color(0xFF00E676) else Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                        }
                        if (isLivePlayback && displayTablePoints.size < totalPointCount) {
                            item(key = "live-placeholder") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(horizontalScrollState)
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    repeat(if (showTrackColumn) 11 else 10) {
                                        KinematicsRowText("calculating$liveDots", Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloatingGraphPanel(
    displayedUnit: String,
    graphPlotPoints: List<GraphPlotPoint>,
    totalPointCount: Int,
    isLivePlayback: Boolean,
    compareTrackAPlotPoints: List<GraphPlotPoint>,
    compareTrackBPlotPoints: List<GraphPlotPoint>,
    primaryTrackColor: Color,
    trackAColor: Color,
    trackBColor: Color,
    showCompareTrackA: Boolean,
    showCompareTrackB: Boolean,
    showCompareDelta: Boolean,
    onShowCompareTrackAChange: (Boolean) -> Unit,
    onShowCompareTrackBChange: (Boolean) -> Unit,
    onShowCompareDeltaChange: (Boolean) -> Unit,
    showOptionPanels: Boolean,
    onShowOptionPanelsChange: (Boolean) -> Unit,
    onViewportSummaryChanged: (GraphViewportSummary) -> Unit,
    availableSeries: List<GraphSeries>,
    selectedSeries: GraphSeries,
    onSeriesSelected: (GraphSeries) -> Unit,
    availableSeriesX: List<GraphSeries>,
    selectedSeriesX: GraphSeries,
    onSeriesXSelected: (GraphSeries) -> Unit,
    selectedSampleIndex: Int,
    onSampleSelected: (Int) -> Unit,
    onExitAnalysisMode: () -> Unit = {},
    profileMode: GraphProfileMode,
    onProfileModeSelected: (GraphProfileMode) -> Unit,
    graphDisplayMode: GraphDisplayMode,
    onGraphDisplayModeChange: (GraphDisplayMode) -> Unit,
    blendAlpha: Float,
    onBlendAlphaChange: (Float) -> Unit,
    profileResult: ProfileResult?,
    photogateAFraction: Float,
    onPhotogateAFractionChange: (Float) -> Unit,
    photogateBFraction: Float,
    onPhotogateBFractionChange: (Float) -> Unit,
    photogateA: PhotogateCrossing,
    photogateB: PhotogateCrossing,
    photogateDeltaUs: Long?,
    twoBodyCurrentSeparation: Double?,
    twoBodyMinimumSeparation: Double?,
    twoBodyMaximumSeparation: Double?,
    twoBodyAverageSeparation: Double?,
    twoBodyCurrentRelativeSpeed: Double?,
    twoBodyMinimumRelativeSpeed: Double?,
    twoBodyMaximumRelativeSpeed: Double?,
    twoBodyAverageRelativeSpeed: Double?,
    twoBodyFrameCount: Int,
    twoBodyUnit: String,
    closestApproachTimeUs: Long?,
    closestApproachFrame: Int?,
    closestApproachSeparation: Double?,
    closestApproachRelativeSpeed: Double?,
    closestApproachMomentumA: Double?,
    closestApproachMomentumB: Double?,
    closestApproachTotalMomentum: Double?,
    closestApproachRelativeMomentum: Double?,
    interactionClass: String?,
    preInteractionSnapshot: InteractionSnapshot?,
    postInteractionSnapshot: InteractionSnapshot?,
    gateAToClosestUs: Long?,
    closestToGateBUs: Long?,
    onFocusClosestApproach: () -> Unit,
    onFocusTimestampUs: (Long) -> Unit,
    currentAbsoluteTimeUs: Long,
    trackAMomentum: Double?,
    trackBMomentum: Double?,
    totalMomentum: Double?,
    relativeMomentum: Double?,
    momentumUnitLabel: String,
    showAnalysisGuides: Boolean,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit,
    onExportPdf: () -> Unit,
    analysisMode: Boolean,
    isLandscape: Boolean,
    graphFullScreen: Boolean = false,
    onGraphFullScreenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val gateAColor = Color(0xFF81C784)
    val gateBColor = Color(0xFFFFB74D)
    val seriesPoints = graphPlotPoints
    val calculatedFps = remember(seriesPoints) {
        if (seriesPoints.size < 2) 0.0
        else {
            val diffs = seriesPoints.zipWithNext { a, b -> b.timestampUs - a.timestampUs }
                .filter { it > 0 }
            if (diffs.isEmpty()) 0.0
            else {
                val averageDtSeconds = diffs.average() / 1_000_000.0
                1.0 / averageDtSeconds
            }
        }
    }
    val resolvedPrimarySeriesColor = remember(selectedSeries, primaryTrackColor) {
        when (selectedSeries) {
            GraphSeries.X,
            GraphSeries.Y,
            GraphSeries.SPEED,
            GraphSeries.AX,
            GraphSeries.AY,
            GraphSeries.ACCEL -> primaryTrackColor
            else -> graphSeriesColor(selectedSeries)
        }
    }
    var drawerPanel by rememberSaveable { mutableStateOf<GraphDrawerPanel?>(null) }
    var graphScaleX by rememberSaveable { mutableFloatStateOf(1f) }
    var graphScaleY by rememberSaveable { mutableFloatStateOf(1f) }
    var viewportCenterX by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var viewportCenterY by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var showScaleBars by rememberSaveable { mutableStateOf(true) }
    var showGraphDetails by rememberSaveable { mutableStateOf(analysisMode) }
    var showSmoothedCurve by rememberSaveable { mutableStateOf(true) }
    var filterOutliers by rememberSaveable { mutableStateOf(false) }
    var xMinLimitInput by rememberSaveable { mutableStateOf("") }
    var xMaxLimitInput by rememberSaveable { mutableStateOf("") }
    var yMinLimitInput by rememberSaveable { mutableStateOf("") }
    var yMaxLimitInput by rememberSaveable { mutableStateOf("") }
    var manualXMin by rememberSaveable { mutableStateOf<Float?>(null) }
    var manualXMax by rememberSaveable { mutableStateOf<Float?>(null) }
    var manualYMin by rememberSaveable { mutableStateOf<Float?>(null) }
    var manualYMax by rememberSaveable { mutableStateOf<Float?>(null) }
    var draggedGraphGateHandle by remember { mutableStateOf<PhotogateHandle?>(null) }
    var graphViewportSize by remember { mutableStateOf(IntSize.Zero) }
    // Phase 3: Analysis range selection state
    var useAnalysisRange by rememberSaveable { mutableStateOf(false) }
    var analysisRangeStartFraction by rememberSaveable { mutableFloatStateOf(0.1f) }
    var analysisRangeEndFraction by rememberSaveable { mutableFloatStateOf(0.9f) }
    var draggedRangeHandle by remember { mutableStateOf<Int?>(null) } // 0 = start, 1 = end

    // Phase 5: Graph layout and interactive zoom states
    var graphLayoutMode by rememberSaveable { mutableStateOf(GraphLayoutMode.OVERLAY) }
    var boxZoomModeEnabled by rememberSaveable { mutableStateOf(false) }
    var activeAxisInspector by rememberSaveable { mutableStateOf(AxisInspector.Y) }
    var xAxisTitle by rememberSaveable { mutableStateOf("Time") }
    var yAxisTitle by rememberSaveable { mutableStateOf("Value") }
    var xAxisUnits by rememberSaveable { mutableStateOf("s") }
    var yAxisUnits by rememberSaveable { mutableStateOf(displayedUnit) }
    var xAxisAutoScale by rememberSaveable { mutableStateOf(true) }
    var yAxisAutoScale by rememberSaveable { mutableStateOf(true) }
    var xAxisLogScale by rememberSaveable { mutableStateOf(false) }
    var yAxisLogScale by rememberSaveable { mutableStateOf(false) }
    var showMinorGrid by rememberSaveable { mutableStateOf(false) }
    var useScientificNotation by rememberSaveable { mutableStateOf(false) }
    var axisDecimalPrecision by rememberSaveable { mutableStateOf("4") }
    var tickIntervalInput by rememberSaveable { mutableStateOf("") }
    var peakDetectionEnabled by rememberSaveable { mutableStateOf(false) }
    var derivativeEnabled by rememberSaveable { mutableStateOf(false) }
    var integrationEnabled by rememberSaveable { mutableStateOf(false) }
    var lowPassFilterEnabled by rememberSaveable { mutableStateOf(false) }
    var highPassFilterEnabled by rememberSaveable { mutableStateOf(false) }
    var bandPassFilterEnabled by rememberSaveable { mutableStateOf(false) }
    var fftEnabled by rememberSaveable { mutableStateOf(false) }
    var baselineCorrectionEnabled by rememberSaveable { mutableStateOf(false) }
    var normalizeDataEnabled by rememberSaveable { mutableStateOf(false) }
    var roiSelectionEnabled by rememberSaveable { mutableStateOf(false) }
    var measurementCursorEnabled by rememberSaveable { mutableStateOf(false) }
    var forceShowCoordinates by rememberSaveable { mutableStateOf(false) }
    var analysisMainTab by rememberSaveable { mutableStateOf(AnalysisMainTab.AXES) }
    var analysisSubTab by rememberSaveable { mutableStateOf(AnalysisSubTab.Y_AXIS) }
    var analysisTableVisible by rememberSaveable { mutableStateOf(false) }

    // Split-pane and accordion states
    var analysisPanelWidthFraction by rememberSaveable { mutableFloatStateOf(0.3f) }
    var analysisTableHeightFraction by rememberSaveable { mutableFloatStateOf(0.26f) }
    var seriesExpanded by rememberSaveable { mutableStateOf(true) }
    var statsExpanded by rememberSaveable { mutableStateOf(true) }
    var fittingExpanded by rememberSaveable { mutableStateOf(true) }
    var enhanceExpanded by rememberSaveable { mutableStateOf(true) }
    var rangeExpanded by rememberSaveable { mutableStateOf(false) }
    var tableExpanded by rememberSaveable { mutableStateOf(false) }
    var exportExpanded by rememberSaveable { mutableStateOf(false) }
    val analysisDeskScrollState = rememberScrollState()
    val displaySeriesPoints = remember(
        seriesPoints,
        showSmoothedCurve,
        filterOutliers,
        lowPassFilterEnabled,
        highPassFilterEnabled,
        bandPassFilterEnabled,
        baselineCorrectionEnabled,
        normalizeDataEnabled,
        blendAlpha
    ) {
        val filtered = if (filterOutliers) filterOutliersPlotPoints(seriesPoints) else seriesPoints
        val signalFiltered = when {
            bandPassFilterEnabled -> bandPassPlotPoints(filtered, blendAlpha)
            highPassFilterEnabled -> highPassPlotPoints(filtered, blendAlpha)
            lowPassFilterEnabled -> lowPassPlotPoints(filtered, blendAlpha)
            else -> filtered
        }
        val smoothed = if (showSmoothedCurve) smoothGraphPlotPoints(signalFiltered) else signalFiltered
        val baselineCorrected = if (baselineCorrectionEnabled) baselineCorrectPlotPoints(smoothed) else smoothed
        if (normalizeDataEnabled) normalizePlotPoints(baselineCorrected) else baselineCorrected
    }
    val compareSeriesA = remember(compareTrackAPlotPoints, showSmoothedCurve, filterOutliers) {
        val filtered = if (filterOutliers) filterOutliersPlotPoints(compareTrackAPlotPoints) else compareTrackAPlotPoints
        if (showSmoothedCurve) smoothGraphPlotPoints(filtered) else filtered
    }
    val compareSeriesB = remember(compareTrackBPlotPoints, showSmoothedCurve, filterOutliers) {
        val filtered = if (filterOutliers) filterOutliersPlotPoints(compareTrackBPlotPoints) else compareTrackBPlotPoints
        if (showSmoothedCurve) smoothGraphPlotPoints(filtered) else filtered
    }
    val deltaSeries = remember(compareSeriesA, compareSeriesB) {
        buildDeltaGraphPlotPoints(compareSeriesA, compareSeriesB)
    }
    val curatedSeriesPoints = remember(displaySeriesPoints, profileResult) {
        profileResult?.let { profile ->
            displaySeriesPoints.map { point ->
                val curatedValue = evaluateProfile(profile, point.timeSeconds.toDouble()).toFloat()
                point.copy(value = curatedValue)
            }
        } ?: emptyList()
    }
    val blendedSeriesPoints = remember(displaySeriesPoints, curatedSeriesPoints, blendAlpha) {
        if (curatedSeriesPoints.isEmpty()) {
            displaySeriesPoints
        } else {
            displaySeriesPoints.zip(curatedSeriesPoints).map { (orig, cur) ->
                val blendedValue = (1.0f - blendAlpha) * orig.value + blendAlpha * cur.value
                orig.copy(value = blendedValue)
            }
        }
    }
    val residualSeriesPoints = remember(displaySeriesPoints, profileResult) {
        buildResidualPlotPoints(displaySeriesPoints, profileResult)
    }
    val derivativeSeriesPoints = remember(displaySeriesPoints) {
        buildDerivativePlotPoints(displaySeriesPoints)
    }
    val integralSeriesPoints = remember(displaySeriesPoints) {
        buildIntegralPlotPoints(displaySeriesPoints)
    }
    val primaryGraphPoints = remember(
        displaySeriesPoints, curatedSeriesPoints, blendedSeriesPoints, graphDisplayMode,
        compareSeriesA, compareSeriesB, deltaSeries, showCompareTrackA, showCompareTrackB, showCompareDelta
    ) {
        val basePoints = when (graphDisplayMode) {
            GraphDisplayMode.ORIGINAL -> displaySeriesPoints
            GraphDisplayMode.CURATED -> if (curatedSeriesPoints.isNotEmpty()) curatedSeriesPoints else displaySeriesPoints
            GraphDisplayMode.BLEND -> blendedSeriesPoints
            GraphDisplayMode.OVERLAY -> displaySeriesPoints
        }
        when {
            basePoints.size >= 2 -> basePoints
            showCompareTrackA && compareSeriesA.size >= 2 -> compareSeriesA
            showCompareTrackB && compareSeriesB.size >= 2 -> compareSeriesB
            showCompareDelta && deltaSeries.size >= 2 -> deltaSeries
            else -> basePoints
        }
    }
    val latestValue = primaryGraphPoints.lastOrNull()?.value
    val profileOverlayValues = remember(displaySeriesPoints, profileResult, graphDisplayMode) {
        if (graphDisplayMode == GraphDisplayMode.ORIGINAL) {
            profileResult?.let { profile ->
                displaySeriesPoints.map { evaluateProfile(profile, it.timeSeconds.toDouble()).toFloat() }
            } ?: emptyList()
        } else {
            emptyList()
        }
    }
    val minValue = sequenceOf(
        primaryGraphPoints.minOfOrNull { it.value },
        if (showCompareTrackA) compareSeriesA.minOfOrNull { it.value } else null,
        if (showCompareTrackB) compareSeriesB.minOfOrNull { it.value } else null,
        if (showCompareDelta) deltaSeries.minOfOrNull { it.value } else null,
        if (graphDisplayMode == GraphDisplayMode.OVERLAY) curatedSeriesPoints.minOfOrNull { it.value } else null,
        profileOverlayValues.minOrNull()
    ).filterNotNull().minOrNull() ?: 0f
    val maxValue = sequenceOf(
        primaryGraphPoints.maxOfOrNull { it.value },
        if (showCompareTrackA) compareSeriesA.maxOfOrNull { it.value } else null,
        if (showCompareTrackB) compareSeriesB.maxOfOrNull { it.value } else null,
        if (showCompareDelta) deltaSeries.maxOfOrNull { it.value } else null,
        if (graphDisplayMode == GraphDisplayMode.OVERLAY) curatedSeriesPoints.maxOfOrNull { it.value } else null,
        profileOverlayValues.maxOrNull()
    ).filterNotNull().maxOrNull() ?: 0f
    val avgValue = if (primaryGraphPoints.isEmpty()) null else primaryGraphPoints.map { it.value }.average().toFloat()
    val graphValues = remember(primaryGraphPoints) { primaryGraphPoints.map { it.value.toDouble() } }
    val sortedGraphValues = remember(graphValues) { graphValues.sorted() }
    val medianValue = remember(sortedGraphValues) { percentileValue(sortedGraphValues, 0.50) }
    val p05Value = remember(sortedGraphValues) { percentileValue(sortedGraphValues, 0.05) }
    val p95Value = remember(sortedGraphValues) { percentileValue(sortedGraphValues, 0.95) }
    val modeValue = remember(graphValues) { approximateMode(graphValues) }
    val varianceValue = remember(graphValues, avgValue) {
        avgValue?.let { mean -> graphValues.map { (it - mean) * (it - mean) }.average() }
    }
    val stdDevValue = varianceValue?.let { sqrt(it) }
    val skewnessValue = remember(graphValues, avgValue, stdDevValue) {
        skewnessValue(graphValues, avgValue?.toDouble(), stdDevValue)
    }
    val kurtosisValue = remember(graphValues, avgValue, stdDevValue) {
        kurtosisValue(graphValues, avgValue?.toDouble(), stdDevValue)
    }
    val covarianceValue = remember(primaryGraphPoints) { covarianceValue(primaryGraphPoints) }
    val correlationValue = remember(primaryGraphPoints) { correlationValue(primaryGraphPoints) }
    val histogramSummary = remember(graphValues) { histogramSummary(graphValues) }
    val frequencySpectrum = remember(primaryGraphPoints) { buildFrequencySpectrum(primaryGraphPoints) }
    val dominantFrequency = remember(frequencySpectrum) { frequencySpectrum.maxByOrNull { it.amplitude } }
    val rmsValue = remember(graphValues) {
        if (graphValues.isEmpty()) null else sqrt(graphValues.map { it * it }.average())
    }
    val detailSeries = remember(primaryGraphPoints) { primaryGraphPoints.mapNotNull { it.detailPoint } }
    val distanceTravelled = remember(detailSeries) {
        detailSeries.zipWithNext { a, b ->
            val dx = b.xValue - a.xValue
            val dy = b.yValue - a.yValue
            sqrt(dx * dx + dy * dy)
        }.sum().takeIf { detailSeries.size >= 2 }
    }
    val displacementValue = remember(detailSeries) {
        if (detailSeries.size < 2) null else {
            val first = detailSeries.first()
            val last = detailSeries.last()
            val dx = last.xValue - first.xValue
            val dy = last.yValue - first.yValue
            sqrt(dx * dx + dy * dy)
        }
    }
    val directionDegrees = remember(detailSeries) {
        if (detailSeries.size < 2) null else {
            val first = detailSeries.first()
            val last = detailSeries.last()
            Math.toDegrees(atan2(last.yValue - first.yValue, last.xValue - first.xValue))
        }
    }
    val averageSpeed = remember(detailSeries) {
        detailSeries.mapNotNull { it.speedValue }.takeIf { it.isNotEmpty() }?.average()
    }
    val averageAcceleration = remember(detailSeries) {
        detailSeries.mapNotNull { it.accelerationValue }.takeIf { it.isNotEmpty() }?.average()
    }
    val averageJerk = remember(detailSeries) {
        detailSeries.zipWithNext().mapNotNull { (a, b) ->
            val aa = a.accelerationValue ?: return@mapNotNull null
            val bb = b.accelerationValue ?: return@mapNotNull null
            val dt = (b.timestampUs - a.timestampUs) / 1_000_000.0
            if (dt > 0.0) (bb - aa) / dt else null
        }.takeIf { it.isNotEmpty() }?.average()
    }
    val angularPositions = remember(detailSeries) {
        detailSeries.map { atan2(it.yValue, it.xValue) }
    }
    val averageAngularVelocity = remember(detailSeries, angularPositions) {
        angularPositions.zipWithNext().mapIndexedNotNull { index, (left, right) ->
            val dt = (detailSeries[index + 1].timestampUs - detailSeries[index].timestampUs) / 1_000_000.0
            if (dt > 0.0) (right - left) / dt else null
        }.takeIf { it.isNotEmpty() }?.average()
    }
    val averageAngularAcceleration = remember(detailSeries, angularPositions) {
        val angularVelocity = angularPositions.zipWithNext().mapIndexedNotNull { index, (left, right) ->
            val dt = (detailSeries[index + 1].timestampUs - detailSeries[index].timestampUs) / 1_000_000.0
            if (dt > 0.0) (right - left) / dt else null
        }
        angularVelocity.zipWithNext().mapIndexedNotNull { index, (left, right) ->
            val dt = (detailSeries.getOrNull(index + 2)?.timestampUs?.minus(detailSeries.getOrNull(index + 1)?.timestampUs ?: return@mapIndexedNotNull null) ?: return@mapIndexedNotNull null) / 1_000_000.0
            if (dt > 0.0) (right - left) / dt else null
        }.takeIf { it.isNotEmpty() }?.average()
    }
    val averageCurvature = remember(detailSeries) {
        detailSeries.mapNotNull { point ->
            val vx = point.vxValue ?: return@mapNotNull null
            val vy = point.vyValue ?: return@mapNotNull null
            val ax = point.axValue ?: return@mapNotNull null
            val ay = point.ayValue ?: return@mapNotNull null
            val denominator = (vx * vx + vy * vy).pow(1.5)
            if (denominator <= 1e-12) null else kotlin.math.abs(vx * ay - vy * ax) / denominator
        }.takeIf { it.isNotEmpty() }?.average()
    }
    val xRangeSeconds = (primaryGraphPoints.lastOrNull()?.xValue ?: 0f) - (primaryGraphPoints.firstOrNull()?.xValue ?: 0f)
    val selectedSample = displaySeriesPoints.getOrNull(selectedSampleIndex)
    val selectedSamplePrediction = remember(selectedSample, profileResult) {
        if (selectedSample == null || profileResult == null) null
        else evaluateProfile(profileResult, selectedSample.timeSeconds.toDouble())
    }
    val selectedSampleResidual = remember(selectedSample, selectedSamplePrediction) {
        if (selectedSample == null || selectedSamplePrediction == null) null
        else selectedSample.value.toDouble() - selectedSamplePrediction
    }
    val selectedSamplePercentError = remember(selectedSamplePrediction, selectedSampleResidual) {
        if (selectedSamplePrediction == null || selectedSampleResidual == null || kotlin.math.abs(selectedSamplePrediction) < 1e-9) null
        else (selectedSampleResidual / selectedSamplePrediction) * 100.0
    }
    val closestApproachGraphPoint = remember(displaySeriesPoints, selectedSeries, closestApproachFrame) {
        if (closestApproachFrame == null || (selectedSeries != GraphSeries.SEPARATION && selectedSeries != GraphSeries.RELATIVE_SPEED)) {
            null
        } else {
            displaySeriesPoints.firstOrNull { it.frameNumber == closestApproachFrame }
        }
    }
    val timelineEvents = remember(
        photogateA.timestampUs,
        photogateB.timestampUs,
        closestApproachTimeUs
    ) {
        buildList {
            photogateA.timestampUs?.let {
                add(AnalysisTimelineEvent("Gate A", it, gateAColor, "A"))
            }
            photogateB.timestampUs?.let {
                add(AnalysisTimelineEvent("Gate B", it, gateBColor, "B"))
            }
            closestApproachTimeUs?.let {
                add(AnalysisTimelineEvent("Closest", it, Color(0xFFFFF176), "CA"))
            }
        }.sortedBy { it.timestampUs }
    }
    val progress = if (totalPointCount <= 0) 0f else graphPlotPoints.size.toFloat() / totalPointCount.toFloat()
    val dataMinX = primaryGraphPoints.minOfOrNull { it.xValue } ?: 0f
    val dataMaxX = primaryGraphPoints.maxOfOrNull { it.xValue }?.coerceAtLeast(dataMinX + 1e-6f) ?: (dataMinX + 1f)
    val baseTimeRange = closedRangeLength(dataMinX, dataMaxX)
    val dataMinY = minValue
    val dataMaxY = maxValue.takeIf { it != minValue } ?: (minValue + 1f)
    val baseValueRange = closedRangeLength(dataMinY, dataMaxY)
    val visibleTimeRange = (baseTimeRange / graphScaleX).coerceIn(baseTimeRange / 6f, baseTimeRange)
    val visibleValueRange = (baseValueRange / graphScaleY).coerceIn(baseValueRange / 6f, baseValueRange)
    val initialCenterX = (dataMinX + dataMaxX) / 2f
    val initialCenterY = if (dataMinY <= 0f && dataMaxY >= 0f) 0f else (dataMinY + dataMaxY) / 2f
    val effectiveCenterX = if (viewportCenterX.isNaN()) initialCenterX else viewportCenterX
    val effectiveCenterY = if (viewportCenterY.isNaN()) initialCenterY else viewportCenterY
    val visibleXBounds = if (!xAxisAutoScale && manualXMin != null && manualXMax != null && manualXMax!! > manualXMin!!) {
        manualXMin!! to manualXMax!!
    } else {
        viewportBounds(
            center = effectiveCenterX,
            visibleLength = visibleTimeRange,
            domainStart = dataMinX,
            domainEnd = dataMaxX
        )
    }
    val visibleYBounds = if (!yAxisAutoScale && manualYMin != null && manualYMax != null && manualYMax!! > manualYMin!!) {
        manualYMin!! to manualYMax!!
    } else {
        viewportBounds(
            center = effectiveCenterY,
            visibleLength = visibleValueRange,
            domainStart = dataMinY,
            domainEnd = dataMaxY
        )
    }
    val visibleStartTime = visibleXBounds.first
    val visibleEndTime = visibleXBounds.second
    val visibleMinValue = visibleYBounds.first
    val visibleMaxValue = visibleYBounds.second
    val applyAxisLimits: () -> Unit = {
        val requestedXMin = xMinLimitInput.toFloatOrNull()
        val requestedXMax = xMaxLimitInput.toFloatOrNull()
        val requestedYMin = yMinLimitInput.toFloatOrNull()
        val requestedYMax = yMaxLimitInput.toFloatOrNull()
        manualXMin = requestedXMin?.coerceAtLeast(dataMinX)
        manualXMax = requestedXMax?.coerceAtMost(dataMaxX)
        manualYMin = requestedYMin
        manualYMax = requestedYMax
        if (requestedXMin != null || requestedXMax != null) xAxisAutoScale = false
        if (requestedYMin != null || requestedYMax != null) yAxisAutoScale = false
        if (manualXMin != null && manualXMax != null && manualXMax!! <= manualXMin!!) {
            manualXMin = null
            manualXMax = null
        }
        if (manualYMin != null && manualYMax != null && manualYMax!! <= manualYMin!!) {
            manualYMin = null
            manualYMax = null
        }
    }
    val resetGraphViewport: () -> Unit = {
        graphScaleX = 1f
        graphScaleY = 1f
        viewportCenterX = initialCenterX
        viewportCenterY = initialCenterY
        manualXMin = null
        manualXMax = null
        manualYMin = null
        manualYMax = null
        xAxisAutoScale = true
        yAxisAutoScale = true
    }
    val liveDots by produceState(initialValue = ".", isLivePlayback, totalPointCount, graphPlotPoints.size) {
        if (!isLivePlayback || graphPlotPoints.size >= totalPointCount) {
            value = "."
            return@produceState
        }
        val frames = listOf(".", "..", "...")
        var index = 0
        while (true) {
            value = frames[index % frames.size]
            index++
            delay(220)
        }
    }
    LaunchedEffect(dataMinX, dataMaxX, visibleTimeRange) {
        viewportCenterX = clampViewportCenter(
            center = if (viewportCenterX.isNaN()) initialCenterX else viewportCenterX,
            visibleLength = visibleTimeRange,
            domainStart = dataMinX,
            domainEnd = dataMaxX
        )
    }
    LaunchedEffect(dataMinY, dataMaxY, visibleValueRange) {
        viewportCenterY = clampViewportCenter(
            center = if (viewportCenterY.isNaN()) initialCenterY else viewportCenterY,
            visibleLength = visibleValueRange,
            domainStart = dataMinY,
            domainEnd = dataMaxY
        )
    }
    LaunchedEffect(analysisMode, isLandscape) {
        if (analysisMode) {
            showGraphDetails = true
            drawerPanel = GraphDrawerPanel.STATS
            // Always ensure the Analysis Desk is open and at a visible width
            onShowOptionPanelsChange(true)
            val minimumAnalysisWidth = if (isLandscape) 0.46f else 0.52f
            if (analysisPanelWidthFraction < minimumAnalysisWidth) {
                analysisPanelWidthFraction = minimumAnalysisWidth
            }
            analysisDeskScrollState.scrollTo(0)
        }
    }
    LaunchedEffect(analysisMode, analysisDeskScrollState.maxValue) {
        if (analysisMode) {
            analysisDeskScrollState.scrollTo(0)
        }
    }
    LaunchedEffect(showOptionPanels, analysisMode) {
        if (showOptionPanels && !analysisMode && drawerPanel == null) {
            drawerPanel = GraphDrawerPanel.SERIES
        }
    }
    LaunchedEffect(analysisMainTab) {
        if (analysisSubTab !in subTabsForAnalysisMain(analysisMainTab)) {
            analysisSubTab = defaultAnalysisSubTab(analysisMainTab)
        }
    }
    // Phase 5: In landscape, ensure Analysis Desk is always open by default
    LaunchedEffect(isLandscape) {
        if (isLandscape && !showOptionPanels) {
            onShowOptionPanelsChange(true)
            analysisPanelWidthFraction = 0.30f
        }
    }
    LaunchedEffect(visibleStartTime, visibleEndTime, visibleMinValue, visibleMaxValue) {
        xMinLimitInput = formatGraphLimitInput(visibleStartTime)
        xMaxLimitInput = formatGraphLimitInput(visibleEndTime)
        yMinLimitInput = formatGraphLimitInput(visibleMinValue)
        yMaxLimitInput = formatGraphLimitInput(visibleMaxValue)
        onViewportSummaryChanged(
            GraphViewportSummary(
                xMin = visibleStartTime,
                xMax = visibleEndTime,
                yMin = visibleMinValue,
                yMax = visibleMaxValue
            )
        )
    }
    LaunchedEffect(selectedSampleIndex, selectedSeries, analysisMode, displaySeriesPoints) {
        if (!analysisMode) return@LaunchedEffect
        if (selectedSeries != GraphSeries.SEPARATION && selectedSeries != GraphSeries.RELATIVE_SPEED) return@LaunchedEffect
        val sample = displaySeriesPoints.getOrNull(selectedSampleIndex) ?: return@LaunchedEffect
        val horizontalMargin = visibleTimeRange * 0.15f
        val verticalMargin = visibleValueRange * 0.2f
        val needsHorizontalShift = sample.timeSeconds < (visibleStartTime + horizontalMargin) || sample.timeSeconds > (visibleEndTime - horizontalMargin)
        val needsVerticalShift = sample.value < (visibleMinValue + verticalMargin) || sample.value > (visibleMaxValue - verticalMargin)
        if (needsHorizontalShift) {
            viewportCenterX = clampViewportCenter(
                center = sample.timeSeconds,
                visibleLength = visibleTimeRange,
                domainStart = dataMinX,
                domainEnd = dataMaxX
            )
        }
        if (needsVerticalShift) {
            viewportCenterY = clampViewportCenter(
                center = sample.value,
                visibleLength = visibleValueRange,
                domainStart = dataMinY,
                domainEnd = dataMaxY
            )
        }
    }
    if (analysisMode) {
        val analysisRows = remember(primaryGraphPoints) {
            primaryGraphPoints.map { point ->
                val detail = point.detailPoint
                AnalysisTableRow(
                    frameNumber = point.frameNumber,
                    timestampUs = point.timestampUs,
                    timeSeconds = point.timeSeconds,
                    xValue = detail?.xValue,
                    yValue = detail?.yValue,
                    value = point.value.toDouble(),
                    velocityX = detail?.vxValue,
                    velocityY = detail?.vyValue,
                    speed = detail?.speedValue,
                    accelerationX = detail?.axValue,
                    accelerationY = detail?.ayValue,
                    acceleration = detail?.accelerationValue,
                    trackLabel = detail?.trackLabel
                )
            }
        }
        val selectedAnalysisModule = when (analysisMainTab) {
            AnalysisMainTab.DATA,
            AnalysisMainTab.AXES -> AnalysisModuleId.DATA
            AnalysisMainTab.GRAPH -> AnalysisModuleId.GRAPH
            AnalysisMainTab.MOTION -> AnalysisModuleId.MOTION
            AnalysisMainTab.FITTING -> AnalysisModuleId.FITTING
            AnalysisMainTab.STATS -> AnalysisModuleId.STATISTICS
            AnalysisMainTab.SIGNAL -> AnalysisModuleId.SIGNAL
            AnalysisMainTab.MEASURE -> AnalysisModuleId.MEASUREMENTS
            AnalysisMainTab.EXPORT -> AnalysisModuleId.EXPORT
        }
        val fitOptions = remember(profileMode, onProfileModeSelected) {
            GraphProfileMode.values()
                .filter { it != GraphProfileMode.CUSTOM }
                .map { mode ->
                    AnalysisFitOption(
                        mode = mode,
                        label = when (mode) {
                            GraphProfileMode.NONE -> "None"
                            GraphProfileMode.AUTO -> "Auto"
                            GraphProfileMode.LINEAR -> "Linear"
                            GraphProfileMode.POLY_2 -> "Poly 2"
                            GraphProfileMode.POLY_3 -> "Poly 3"
                            GraphProfileMode.POLY_4 -> "Poly 4"
                            GraphProfileMode.POLY_5 -> "Poly 5"
                            GraphProfileMode.EXPONENTIAL -> "Exp"
                            GraphProfileMode.LOGARITHMIC -> "Log"
                            GraphProfileMode.POWER_LAW -> "Power"
                            GraphProfileMode.GAUSSIAN -> "Gaussian"
                            GraphProfileMode.LORENTZIAN -> "Lorentz"
                            GraphProfileMode.SINE -> "Sine"
                            GraphProfileMode.DAMPED_SINE -> "Damped Sine"
                            GraphProfileMode.DAMPED_COSINE -> "Damped Cos"
                            GraphProfileMode.DAMPED_HARMONIC -> "Damped Harm"
                            GraphProfileMode.MULTI_SINE -> "Multi Sine"
                            GraphProfileMode.CHIRP -> "Chirp"
                            GraphProfileMode.CUSTOM -> "Custom"
                        },
                        selected = profileMode == mode,
                        onSelect = { onProfileModeSelected(mode) }
                    )
                }
        }
        val ySeriesOptions = remember(availableSeries, selectedSeries, displayedUnit, onSeriesSelected) {
            availableSeries.map { series ->
                AnalysisSeriesOption(
                    label = graphSeriesLabel(series, displayedUnit),
                    selected = selectedSeries == series,
                    onSelect = { onSeriesSelected(series) }
                )
            }
        }
        val xSeriesOptions = remember(availableSeriesX, selectedSeriesX, displayedUnit, onSeriesXSelected) {
            availableSeriesX.map { series ->
                AnalysisSeriesOption(
                    label = graphSeriesLabel(series, displayedUnit),
                    selected = selectedSeriesX == series,
                    onSelect = { onSeriesXSelected(series) }
                )
            }
        }
        val plotPresets = remember(
            availableSeries,
            availableSeriesX,
            selectedSeries,
            selectedSeriesX,
            selectedAnalysisModule,
            onSeriesSelected,
            onSeriesXSelected
        ) {
            buildList {
                fun addPreset(label: String, y: GraphSeries, x: GraphSeries = GraphSeries.TIME) {
                    if (y in availableSeries && x in availableSeriesX) {
                        add(
                            AnalysisPlotPreset(
                                label = label,
                                selected = selectedSeries == y && selectedSeriesX == x,
                                onSelect = {
                                    onSeriesSelected(y)
                                    onSeriesXSelected(x)
                                }
                            )
                        )
                    }
                }
                addPreset("x vs t", GraphSeries.X)
                addPreset("y vs t", GraphSeries.Y)
                addPreset("vx vs t", GraphSeries.VX)
                addPreset("vy vs t", GraphSeries.VY)
                addPreset("speed vs t", GraphSeries.SPEED)
                addPreset("ax vs t", GraphSeries.AX)
                addPreset("ay vs t", GraphSeries.AY)
                addPreset("a vs t", GraphSeries.ACCEL)
                addPreset("trajectory y vs x", GraphSeries.Y, GraphSeries.X)
                addPreset("separation vs t", GraphSeries.SEPARATION)
                addPreset("relative speed vs t", GraphSeries.RELATIVE_SPEED)
                add(
                    AnalysisPlotPreset(
                        label = "Fourier / FFT",
                        selected = selectedAnalysisModule == AnalysisModuleId.SIGNAL,
                        onSelect = { analysisMainTab = AnalysisMainTab.SIGNAL }
                    )
                )
            }
        }
        AnalysisWorkspace(
            state = AnalysisWorkspaceState(
                selectedModule = selectedAnalysisModule,
                rows = analysisRows,
                selectedSeriesLabel = graphSeriesLabel(selectedSeries, displayedUnit),
                selectedXAxisLabel = graphSeriesLabel(selectedSeriesX, displayedUnit),
                selectedFit = profileResult,
                ySeriesOptions = ySeriesOptions,
                xSeriesOptions = xSeriesOptions,
                plotPresets = plotPresets,
                fitOptions = fitOptions,
                showTrackA = showCompareTrackA,
                showTrackB = showCompareTrackB,
                showDelta = showCompareDelta,
                tableVisible = analysisTableVisible,
                boxZoomEnabled = boxZoomModeEnabled,
                showGraphDetails = forceShowCoordinates || measurementCursorEnabled,
                smoothingEnabled = showSmoothedCurve,
                outlierFilterEnabled = filterOutliers,
                lowPassFilterEnabled = lowPassFilterEnabled,
                highPassFilterEnabled = highPassFilterEnabled,
                bandPassFilterEnabled = bandPassFilterEnabled,
                baselineCorrectionEnabled = baselineCorrectionEnabled,
                normalizationEnabled = normalizeDataEnabled,
                filterStrength = blendAlpha,
                onShowTrackAChange = onShowCompareTrackAChange,
                onShowTrackBChange = onShowCompareTrackBChange,
                onShowDeltaChange = onShowCompareDeltaChange,
                onTableVisibleChange = { analysisTableVisible = it },
                onBoxZoomEnabledChange = {
                    boxZoomModeEnabled = it
                    if (it) {
                        forceShowCoordinates = false
                        measurementCursorEnabled = false
                    }
                },
                onShowGraphDetailsChange = { enabled ->
                    forceShowCoordinates = enabled
                    measurementCursorEnabled = enabled
                    showGraphDetails = enabled
                    if (enabled) {
                        boxZoomModeEnabled = false
                        if (selectedSampleIndex !in displaySeriesPoints.indices && displaySeriesPoints.isNotEmpty()) {
                            onSampleSelected(displaySeriesPoints.lastIndex)
                        }
                    }
                },
                onSmoothingEnabledChange = { showSmoothedCurve = it },
                onOutlierFilterEnabledChange = { filterOutliers = it },
                onLowPassFilterEnabledChange = {
                    lowPassFilterEnabled = it
                    if (it) {
                        highPassFilterEnabled = false
                        bandPassFilterEnabled = false
                    }
                },
                onHighPassFilterEnabledChange = {
                    highPassFilterEnabled = it
                    if (it) {
                        lowPassFilterEnabled = false
                        bandPassFilterEnabled = false
                    }
                },
                onBandPassFilterEnabledChange = {
                    bandPassFilterEnabled = it
                    if (it) {
                        lowPassFilterEnabled = false
                        highPassFilterEnabled = false
                    }
                },
                onBaselineCorrectionEnabledChange = { baselineCorrectionEnabled = it },
                onNormalizationEnabledChange = { normalizeDataEnabled = it },
                onFilterStrengthChange = { onBlendAlphaChange(it.coerceIn(0f, 1f)) },
                onResetCuration = {
                    showSmoothedCurve = false
                    filterOutliers = false
                    lowPassFilterEnabled = false
                    highPassFilterEnabled = false
                    bandPassFilterEnabled = false
                    baselineCorrectionEnabled = false
                    normalizeDataEnabled = false
                    onBlendAlphaChange(0.5f)
                },
                onDeleteSelectedPoint = null,
                canUndo = false,
                canRedo = false,
                onUndo = {},
                onRedo = {},
                onResetView = {
                    resetGraphViewport()
                    boxZoomModeEnabled = false
                    forceShowCoordinates = false
                    measurementCursorEnabled = false
                    showGraphDetails = false
                    onSampleSelected(-1)
                },
                onExitFullScreen = onExitAnalysisMode,
                onExportCsv = onExportCsv,
                onExportXlsx = onExportXlsx,
                onExportPdf = onExportPdf
            ),
            onSelectedModuleChange = { module ->
                analysisMainTab = when (module) {
                    AnalysisModuleId.DATA -> AnalysisMainTab.DATA
                    AnalysisModuleId.MOTION -> AnalysisMainTab.MOTION
                    AnalysisModuleId.GRAPH -> AnalysisMainTab.GRAPH
                    AnalysisModuleId.FITTING -> AnalysisMainTab.FITTING
                    AnalysisModuleId.STATISTICS -> AnalysisMainTab.STATS
                    AnalysisModuleId.SIGNAL -> AnalysisMainTab.SIGNAL
                    AnalysisModuleId.MEASUREMENTS -> AnalysisMainTab.MEASURE
                    AnalysisModuleId.EXPORT -> AnalysisMainTab.EXPORT
                }
            },
            fullGraphVisible = graphFullScreen,
            onFullGraphVisibleChange = onGraphFullScreenChange,
            modifier = modifier
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (primaryGraphPoints.size < 2) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Track at least two points to plot analysis data.",
                            color = Color.White.copy(alpha = 0.68f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    GraphCanvasWidget(
                        modifier = Modifier.fillMaxSize(),
                        label = "${graphSeriesLabel(selectedSeries, displayedUnit)} (Primary)",
                        points = primaryGraphPoints,
                        color = resolvedPrimarySeriesColor,
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
                        showAnalysisGuides = showAnalysisGuides,
                        photogateAFraction = photogateAFraction,
                        onPhotogateAFractionChange = onPhotogateAFractionChange,
                        photogateBFraction = photogateBFraction,
                        onPhotogateBFractionChange = onPhotogateBFractionChange,
                        useAnalysisRange = useAnalysisRange,
                        analysisRangeStartFraction = analysisRangeStartFraction,
                        onAnalysisRangeStartFractionChange = { analysisRangeStartFraction = it },
                        analysisRangeEndFraction = analysisRangeEndFraction,
                        onAnalysisRangeEndFractionChange = { analysisRangeEndFraction = it },
                        selectedSampleIndex = selectedSampleIndex,
                        onSampleSelected = onSampleSelected,
                        primarySeriesPoints = primaryGraphPoints,
                        graphDisplayMode = graphDisplayMode,
                        curatedSeriesPoints = curatedSeriesPoints,
                        blendedSeriesPoints = blendedSeriesPoints,
                        profileResult = profileResult,
                        selectedSample = selectedSample,
                        selectedSamplePrediction = selectedSamplePrediction,
                        selectedSampleResidual = selectedSampleResidual,
                        closestApproachGraphPoint = closestApproachGraphPoint,
                        onFocusClosestApproach = onFocusClosestApproach,
                        showScaleBars = showScaleBars,
                        showGraphDetails = showGraphDetails,
                        boxZoomModeEnabled = boxZoomModeEnabled,
                        onBoxZoomModeEnabledChange = { boxZoomModeEnabled = it },
                        graphFullScreen = graphFullScreen,
                        onGraphFullScreenChange = onGraphFullScreenChange,
                        displayedUnit = displayedUnit,
                        selectedSeries = selectedSeries,
                        selectedSeriesX = selectedSeriesX,
                        xAxisTitle = xAxisTitle,
                        yAxisTitle = yAxisTitle,
                        xAxisUnits = xAxisUnits,
                        yAxisUnits = yAxisUnits,
                        xAxisLogScale = xAxisLogScale,
                        yAxisLogScale = yAxisLogScale,
                        showMinorGrid = showMinorGrid,
                        useScientificNotation = useScientificNotation,
                        axisDecimalPrecision = axisDecimalPrecision.toIntOrNull()?.coerceIn(0, 9) ?: 4,
                        tickInterval = tickIntervalInput.toFloatOrNull()?.takeIf { it > 0f },
                        peakDetectionEnabled = peakDetectionEnabled,
                        measurementCursorEnabled = measurementCursorEnabled,
                        derivativeEnabled = derivativeEnabled,
                        integrationEnabled = integrationEnabled,
                        lowPassFilterEnabled = lowPassFilterEnabled,
                        highPassFilterEnabled = highPassFilterEnabled,
                        bandPassFilterEnabled = bandPassFilterEnabled,
                        fftEnabled = fftEnabled,
                        baselineCorrectionEnabled = baselineCorrectionEnabled,
                        roiSelectionEnabled = roiSelectionEnabled,
                        forceShowCoordinates = forceShowCoordinates,
                        showCompareTrackA = showCompareTrackA,
                        compareSeriesA = compareSeriesA,
                        trackAColor = trackAColor,
                        showCompareTrackB = showCompareTrackB,
                        compareSeriesB = compareSeriesB,
                        trackBColor = trackBColor,
                        showCompareDelta = showCompareDelta,
                        deltaSeries = deltaSeries
                    )
                }
            }
        }
        return
    }
    Surface(
        color = Color(0xFF07110C),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(if (isLandscape || graphFullScreen) 0.dp else 12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider),
        modifier = modifier.then(
            when {
                graphFullScreen -> Modifier.fillMaxSize()
                isLandscape -> Modifier.fillMaxSize()
                else -> Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp)
                    .heightIn(min = 300.dp, max = 680.dp)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (primaryGraphPoints.size < 2) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Track at least two points to plot graph data.",
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                GraphCanvasWidget(
                    modifier = Modifier.fillMaxSize(),
                    label = "${graphSeriesLabel(selectedSeries, displayedUnit)} (Primary)",
                    points = primaryGraphPoints,
                    color = resolvedPrimarySeriesColor,
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
                    analysisMode = false,
                    showAnalysisGuides = false,
                    photogateAFraction = photogateAFraction,
                    onPhotogateAFractionChange = onPhotogateAFractionChange,
                    photogateBFraction = photogateBFraction,
                    onPhotogateBFractionChange = onPhotogateBFractionChange,
                    useAnalysisRange = false,
                    analysisRangeStartFraction = analysisRangeStartFraction,
                    onAnalysisRangeStartFractionChange = { analysisRangeStartFraction = it },
                    analysisRangeEndFraction = analysisRangeEndFraction,
                    onAnalysisRangeEndFractionChange = { analysisRangeEndFraction = it },
                    selectedSampleIndex = selectedSampleIndex,
                    onSampleSelected = onSampleSelected,
                    primarySeriesPoints = primaryGraphPoints,
                    graphDisplayMode = graphDisplayMode,
                    curatedSeriesPoints = curatedSeriesPoints,
                    blendedSeriesPoints = blendedSeriesPoints,
                    profileResult = profileResult,
                    selectedSample = selectedSample,
                    selectedSamplePrediction = selectedSamplePrediction,
                    selectedSampleResidual = selectedSampleResidual,
                    closestApproachGraphPoint = closestApproachGraphPoint,
                    onFocusClosestApproach = onFocusClosestApproach,
                    showScaleBars = showScaleBars,
                    showGraphDetails = false,
                    boxZoomModeEnabled = false,
                    onBoxZoomModeEnabledChange = { boxZoomModeEnabled = it },
                    graphFullScreen = graphFullScreen,
                    onGraphFullScreenChange = onGraphFullScreenChange,
                    displayedUnit = displayedUnit,
                    selectedSeries = selectedSeries,
                    selectedSeriesX = selectedSeriesX,
                    xAxisTitle = xAxisTitle,
                    yAxisTitle = yAxisTitle,
                    xAxisUnits = xAxisUnits,
                    yAxisUnits = yAxisUnits,
                    xAxisLogScale = xAxisLogScale,
                    yAxisLogScale = yAxisLogScale,
                    showMinorGrid = showMinorGrid,
                    useScientificNotation = useScientificNotation,
                    axisDecimalPrecision = axisDecimalPrecision.toIntOrNull()?.coerceIn(0, 9) ?: 4,
                    tickInterval = tickIntervalInput.toFloatOrNull()?.takeIf { it > 0f },
                    peakDetectionEnabled = false,
                    measurementCursorEnabled = false,
                    derivativeEnabled = false,
                    integrationEnabled = false,
                    lowPassFilterEnabled = false,
                    highPassFilterEnabled = false,
                    bandPassFilterEnabled = false,
                    fftEnabled = false,
                    baselineCorrectionEnabled = false,
                    roiSelectionEnabled = false,
                    forceShowCoordinates = false,
                    showCompareTrackA = showCompareTrackA,
                    compareSeriesA = compareSeriesA,
                    trackAColor = trackAColor,
                    showCompareTrackB = showCompareTrackB,
                    compareSeriesB = compareSeriesB,
                    trackBColor = trackBColor,
                    showCompareDelta = showCompareDelta,
                    deltaSeries = deltaSeries
                )
            }
        }
    }
    return
    Surface(
        color = Color(0xFF07110C),
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider),
        modifier = modifier.then(
            when {
                graphFullScreen -> Modifier.fillMaxSize()
                isLandscape -> Modifier.fillMaxSize()
                else -> Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp)
                    .heightIn(min = 300.dp, max = 680.dp)
            }
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val totalWidthDp = maxWidth
            val totalHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            val totalHeightDp = maxHeight
            
            // Layout preset helper
            val minimumAnalysisWidth = if (isLandscape || graphFullScreen) 0.46f else 0.52f
            val resolvedPanelWidthFraction = if (analysisMode) {
                analysisPanelWidthFraction.coerceIn(minimumAnalysisWidth, 0.70f)
            } else {
                analysisPanelWidthFraction.coerceIn(0.10f, 0.70f)
            }
            val actualLeftWidthFraction = if (analysisMode || showOptionPanels) resolvedPanelWidthFraction else 0f
            val currentLeftWidthDp = totalWidthDp * actualLeftWidthFraction
            
            Row(modifier = Modifier.fillMaxSize()) {
                if (actualLeftWidthFraction > 0.05f) {
                    // Left Panel: Resizable controls, accordions
                    Column(
                        modifier = Modifier
                            .width(currentLeftWidthDp)
                            .fillMaxHeight()
                            .background(PhysicsColorSurface)
                            .padding(end = 4.dp)
                    ) {
                        // Left Panel Header (Presets & Collapsing)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF020604), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Analysis Desk",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!analysisMode) {
                                    CompactPresetButton("Hide", Modifier.weight(1f)) {
                                        onShowOptionPanelsChange(false)
                                        analysisPanelWidthFraction = 0.10f
                                    }
                                }
                                CompactPresetButton("Compact", Modifier.weight(1f)) {
                                    onShowOptionPanelsChange(true)
                                    analysisPanelWidthFraction = minimumAnalysisWidth
                                }
                                CompactPresetButton("Wide", Modifier.weight(1f)) {
                                    onShowOptionPanelsChange(true)
                                    analysisPanelWidthFraction = 0.55f
                                }
                            }
                        }
                        
                        Divider(color = PhysicsColorDivider)

                        if (analysisMode) {
                            AnalysisDeskCard("Axis, Profile & Stats") {
                                Text(
                                    text = "${graphSeriesLabel(selectedSeries, displayedUnit)} vs ${graphSeriesLabel(selectedSeriesX, displayedUnit)}",
                                    color = resolvedPrimarySeriesColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AnalysisOptionChip("Y Axis", activeAxisInspector == AxisInspector.Y, Color(0xFF00E5FF), Modifier.weight(1f)) {
                                        activeAxisInspector = AxisInspector.Y
                                    }
                                    AnalysisOptionChip("X Axis", activeAxisInspector == AxisInspector.X, Color(0xFFFFB74D), Modifier.weight(1f)) {
                                        activeAxisInspector = AxisInspector.X
                                    }
                                }
                                availableSeries.take(4).chunked(2).forEach { rowSeries ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rowSeries.forEach { series ->
                                            AnalysisOptionChip(
                                                label = graphSeriesLabel(series, displayedUnit),
                                                selected = selectedSeries == series,
                                                accent = graphSeriesColor(series),
                                                modifier = Modifier.weight(1f),
                                                onClick = { onSeriesSelected(series) }
                                            )
                                        }
                                        if (rowSeries.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                availableSeriesX.take(4).chunked(2).forEach { rowSeries ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rowSeries.forEach { series ->
                                            AnalysisOptionChip(
                                                label = "X: ${graphSeriesLabel(series, displayedUnit)}",
                                                selected = selectedSeriesX == series,
                                                accent = graphSeriesColor(series),
                                                modifier = Modifier.weight(1f),
                                                onClick = { onSeriesXSelected(series) }
                                            )
                                        }
                                        if (rowSeries.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AnalysisOptionChip("Profile", profileMode != GraphProfileMode.NONE, Color(0xFFFF8A65), Modifier.weight(1f)) {
                                        analysisMainTab = AnalysisMainTab.FITTING
                                        analysisSubTab = AnalysisSubTab.REGRESSION
                                    }
                                    AnalysisOptionChip("Data Curation", graphDisplayMode != GraphDisplayMode.ORIGINAL || filterOutliers || showSmoothedCurve, Color(0xFFCE93D8), Modifier.weight(1f)) {
                                        analysisMainTab = AnalysisMainTab.DATA
                                        analysisSubTab = AnalysisSubTab.CLEANUP
                                    }
                                }
                                TableCellCompact("Current", latestValue?.let { formatCalibratedValue(it.toDouble()) } ?: "-", resolvedPrimarySeriesColor)
                                TableCellCompact("Min / Max", "${formatCalibratedValue(minValue.toDouble())} / ${formatCalibratedValue(maxValue.toDouble())}")
                                TableCellCompact("Zoom X/Y", "${String.format("%.2f", graphScaleX)}x / ${String.format("%.2f", graphScaleY)}x")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF020604), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AnalysisMainTab.values().forEach { tab ->
                                        CompactToolButton(
                                            label = tab.label,
                                            selected = analysisMainTab == tab,
                                            accent = when (tab) {
                                                AnalysisMainTab.DATA -> Color(0xFFCE93D8)
                                                AnalysisMainTab.AXES -> Color(0xFF00E5FF)
                                                AnalysisMainTab.GRAPH -> Color(0xFF80CBC4)
                                                AnalysisMainTab.MOTION -> Color(0xFFFFB74D)
                                                AnalysisMainTab.FITTING -> Color(0xFFFF8A65)
                                                AnalysisMainTab.STATS -> Color(0xFF81C784)
                                                AnalysisMainTab.SIGNAL -> Color(0xFFBA68C8)
                                                AnalysisMainTab.MEASURE -> Color(0xFFFFB74D)
                                                AnalysisMainTab.EXPORT -> Color(0xFF83D49A)
                                            }
                                        ) {
                                            analysisMainTab = tab
                                            analysisSubTab = defaultAnalysisSubTab(tab)
                                        }
                                    }
                                }

                                val availableAnalysisSubTabs = subTabsForAnalysisMain(analysisMainTab)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    availableAnalysisSubTabs.forEach { subTab ->
                                        CompactToolButton(
                                            label = subTab.label,
                                            selected = analysisSubTab == subTab,
                                            accent = Color.White.copy(alpha = 0.82f)
                                        ) {
                                            analysisSubTab = subTab
                                        }
                                    }
                                }

                                when (analysisMainTab) {
                                    AnalysisMainTab.AXES -> {
                                        when (analysisSubTab) {
                                            AnalysisSubTab.X_AXIS, AnalysisSubTab.Y_AXIS -> {
                                                activeAxisInspector = if (analysisSubTab == AnalysisSubTab.X_AXIS) AxisInspector.X else AxisInspector.Y
                                                AnalysisDeskCard("${analysisSubTab.label} Module") {
                                                    val editingXAxis = activeAxisInspector == AxisInspector.X
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        AnalysisOptionChip("X-Axis", editingXAxis, Color(0xFFFFB74D), Modifier.weight(1f)) { analysisSubTab = AnalysisSubTab.X_AXIS }
                                                        AnalysisOptionChip("Y-Axis", !editingXAxis, Color(0xFF00E5FF), Modifier.weight(1f)) { analysisSubTab = AnalysisSubTab.Y_AXIS }
                                                    }
                                                    OutlinedTextField(
                                                        value = if (editingXAxis) xAxisTitle else yAxisTitle,
                                                        onValueChange = { if (editingXAxis) xAxisTitle = it else yAxisTitle = it },
                                                        singleLine = true,
                                                        label = { Text("Title") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    OutlinedTextField(
                                                        value = if (editingXAxis) xAxisUnits else yAxisUnits,
                                                        onValueChange = { if (editingXAxis) xAxisUnits = it else yAxisUnits = it },
                                                        singleLine = true,
                                                        label = { Text("Units") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        AnalysisOptionChip("Auto Scale", if (editingXAxis) xAxisAutoScale else yAxisAutoScale, Color(0xFF81C784), Modifier.weight(1f)) {
                                                            if (editingXAxis) xAxisAutoScale = !xAxisAutoScale else yAxisAutoScale = !yAxisAutoScale
                                                        }
                                                        AnalysisOptionChip("Log Scale", if (editingXAxis) xAxisLogScale else yAxisLogScale, Color(0xFFBA68C8), Modifier.weight(1f)) {
                                                            if (editingXAxis) xAxisLogScale = !xAxisLogScale else yAxisLogScale = !yAxisLogScale
                                                        }
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        OutlinedTextField(value = if (editingXAxis) xMinLimitInput else yMinLimitInput, onValueChange = { if (editingXAxis) xMinLimitInput = it else yMinLimitInput = it }, singleLine = true, label = { Text("Min") }, modifier = Modifier.weight(1f))
                                                        OutlinedTextField(value = if (editingXAxis) xMaxLimitInput else yMaxLimitInput, onValueChange = { if (editingXAxis) xMaxLimitInput = it else yMaxLimitInput = it }, singleLine = true, label = { Text("Max") }, modifier = Modifier.weight(1f))
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        OutlinedTextField(value = tickIntervalInput, onValueChange = { tickIntervalInput = it }, singleLine = true, label = { Text("Tick spacing") }, modifier = Modifier.weight(1f))
                                                        OutlinedTextField(value = axisDecimalPrecision, onValueChange = { axisDecimalPrecision = it.filter { c -> c.isDigit() }.take(2) }, singleLine = true, label = { Text("Decimals") }, modifier = Modifier.weight(1f))
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        AnalysisOptionChip("Major Grid", showScaleBars, Color(0xFF00E5FF), Modifier.weight(1f)) { showScaleBars = !showScaleBars }
                                                        AnalysisOptionChip("Minor Grid", showMinorGrid, Color(0xFF80CBC4), Modifier.weight(1f)) { showMinorGrid = !showMinorGrid }
                                                    }
                                                    AnalysisOptionChip("Scientific Notation", useScientificNotation, Color(0xFFFFB74D), Modifier.fillMaxWidth()) { useScientificNotation = !useScientificNotation }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        CompactActionButton("Apply") {
                                                            if (editingXAxis) xAxisAutoScale = false else yAxisAutoScale = false
                                                            applyAxisLimits()
                                                            focusManager.clearFocus()
                                                        }
                                                        CompactActionButton("Reset Axis") {
                                                            if (editingXAxis) {
                                                                xAxisAutoScale = true
                                                                manualXMin = null
                                                                manualXMax = null
                                                            } else {
                                                                yAxisAutoScale = true
                                                                manualYMin = null
                                                                manualYMax = null
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            AnalysisSubTab.GRID -> AnalysisDeskCard("Grid") {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    AnalysisOptionChip("Major Grid", showScaleBars, Color(0xFF00E5FF), Modifier.weight(1f)) { showScaleBars = !showScaleBars }
                                                    AnalysisOptionChip("Minor Grid", showMinorGrid, Color(0xFF80CBC4), Modifier.weight(1f)) { showMinorGrid = !showMinorGrid }
                                                }
                                            }
                                            AnalysisSubTab.SCALE -> AnalysisDeskCard("Scale") {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    AnalysisOptionChip("X Auto", xAxisAutoScale, Color(0xFFFFB74D), Modifier.weight(1f)) { xAxisAutoScale = !xAxisAutoScale }
                                                    AnalysisOptionChip("Y Auto", yAxisAutoScale, Color(0xFF00E5FF), Modifier.weight(1f)) { yAxisAutoScale = !yAxisAutoScale }
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    AnalysisOptionChip("X Log", xAxisLogScale, Color(0xFFFFB74D), Modifier.weight(1f)) { xAxisLogScale = !xAxisLogScale }
                                                    AnalysisOptionChip("Y Log", yAxisLogScale, Color(0xFF00E5FF), Modifier.weight(1f)) { yAxisLogScale = !yAxisLogScale }
                                                }
                                            }
                                            AnalysisSubTab.LABELS -> AnalysisDeskCard("Labels") {
                                                OutlinedTextField(value = xAxisTitle, onValueChange = { xAxisTitle = it }, singleLine = true, label = { Text("X Title") }, modifier = Modifier.fillMaxWidth())
                                                OutlinedTextField(value = yAxisTitle, onValueChange = { yAxisTitle = it }, singleLine = true, label = { Text("Y Title") }, modifier = Modifier.fillMaxWidth())
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedTextField(value = xAxisUnits, onValueChange = { xAxisUnits = it }, singleLine = true, label = { Text("X Units") }, modifier = Modifier.weight(1f))
                                                    OutlinedTextField(value = yAxisUnits, onValueChange = { yAxisUnits = it }, singleLine = true, label = { Text("Y Units") }, modifier = Modifier.weight(1f))
                                                }
                                            }
                                            AnalysisSubTab.TICKS -> AnalysisDeskCard("Tick Marks") {
                                                OutlinedTextField(value = tickIntervalInput, onValueChange = { tickIntervalInput = it }, singleLine = true, label = { Text("Tick spacing") }, modifier = Modifier.fillMaxWidth())
                                                OutlinedTextField(value = axisDecimalPrecision, onValueChange = { axisDecimalPrecision = it.filter { c -> c.isDigit() }.take(2) }, singleLine = true, label = { Text("Decimal precision") }, modifier = Modifier.fillMaxWidth())
                                                AnalysisOptionChip("Scientific Notation", useScientificNotation, Color(0xFFFFB74D), Modifier.fillMaxWidth()) { useScientificNotation = !useScientificNotation }
                                            }
                                            AnalysisSubTab.DATASET -> {
                                                AnalysisDeskCard("Dataset & Variable Mapping") {
                                                    Text("Y Variable", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                                    availableSeries.chunked(2).forEach { rowSeries ->
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            rowSeries.forEach { series ->
                                                                AnalysisOptionChip(graphSeriesLabel(series, displayedUnit), selectedSeries == series, graphSeriesColor(series), Modifier.weight(1f)) { onSeriesSelected(series) }
                                                            }
                                                            if (rowSeries.size == 1) Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                    Text("X Variable", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                                    availableSeriesX.chunked(2).forEach { rowSeries ->
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            rowSeries.forEach { series ->
                                                                AnalysisOptionChip(graphSeriesLabel(series, displayedUnit), selectedSeriesX == series, graphSeriesColor(series), Modifier.weight(1f)) { onSeriesXSelected(series) }
                                                            }
                                                            if (rowSeries.size == 1) Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        AnalysisOptionChip("Track A", showCompareTrackA, trackAColor, Modifier.weight(1f)) { onShowCompareTrackAChange(!showCompareTrackA) }
                                                        AnalysisOptionChip("Track B", showCompareTrackB, trackBColor, Modifier.weight(1f)) { onShowCompareTrackBChange(!showCompareTrackB) }
                                                    }
                                                    if (compareSeriesA.isNotEmpty() && compareSeriesB.isNotEmpty()) {
                                                        AnalysisOptionChip("Delta / Difference", showCompareDelta, Color(0xFFFFF176), Modifier.fillMaxWidth()) { onShowCompareDeltaChange(!showCompareDelta) }
                                                    }
                                                }
                                            }
                                            else -> Unit
                                        }
                                    }
                                    AnalysisMainTab.FITTING -> {
                                        when (analysisSubTab) {
                                            AnalysisSubTab.REGRESSION -> {
                                                AnalysisDeskCard("Curve/Profile Fitting") {
                                                    GraphProfileMode.values().filter { it != GraphProfileMode.CUSTOM }.chunked(2).forEach { rowModes ->
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            rowModes.forEach { mode ->
                                                                val label = mode.name.lowercase().replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                                                                AnalysisOptionChip(label, profileMode == mode, Color(0xFFFF8A65), Modifier.weight(1f)) { onProfileModeSelected(mode) }
                                                            }
                                                            if (rowModes.size == 1) Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                    Slider(value = graphScaleY, onValueChange = { graphScaleY = it }, valueRange = 1f..6f)
                                                    TableCellCompact("Polynomial / fit sensitivity", "${String.format("%.2f", graphScaleY)}x")
                                                }
                                            }
                                            AnalysisSubTab.RESIDUALS -> {
                                                AnalysisDeskCard("Fit Parameters & Residuals") {
                                                    profileResult?.let { result ->
                                                        TableCellCompact("Active Fit", result.label, Color(0xFFFF8A65))
                                                        TableCellCompact("R2", String.format("%.5f", result.rSquared))
                                                        TableCellCompact("MAE", formatCalibratedValue(result.mae))
                                                        TableCellCompact("RMSE", formatCalibratedValue(result.rmse))
                                                        TableCellCompact("Reduced chi2", formatCalibratedValue(result.reducedChiSq))
                                                        val parameterCount = result.parameters.size.coerceAtLeast(1)
                                                        TableCellCompact("Adjusted R2", adjustedRSquared(result.rSquared, primaryGraphPoints.size, parameterCount)?.let { String.format(Locale.US, "%.5f", it) } ?: "-")
                                                        TableCellCompact("AIC", akaikeInformationCriterion(primaryGraphPoints.size, parameterCount, result.rmse)?.let(::formatCalibratedValue) ?: "-")
                                                        TableCellCompact("BIC", bayesianInformationCriterion(primaryGraphPoints.size, parameterCount, result.rmse)?.let(::formatCalibratedValue) ?: "-")
                                                        if (result.equation.isNotBlank()) Text(result.equation, color = Color(0xFFFF8A65), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                                        selectedSampleResidual?.let { TableCellCompact("Selected Residual", formatCalibratedValue(it), Color(0xFFFF8A65)) }
                                                    } ?: Text("Select a fitting model to view regression parameters and residuals.", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                            else -> Unit
                                        }
                                    }
                                    AnalysisMainTab.DATA -> {
                                        when (analysisSubTab) {
                                            AnalysisSubTab.DATASET -> {
                                                AnalysisDeskCard("Dataset & Variable Mapping") {
                                                    Text("Y Variable", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                                    availableSeries.chunked(2).forEach { rowSeries ->
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            rowSeries.forEach { series ->
                                                                AnalysisOptionChip(graphSeriesLabel(series, displayedUnit), selectedSeries == series, graphSeriesColor(series), Modifier.weight(1f)) { onSeriesSelected(series) }
                                                            }
                                                            if (rowSeries.size == 1) Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                    Text("X Variable", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                                    availableSeriesX.chunked(2).forEach { rowSeries ->
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            rowSeries.forEach { series ->
                                                                AnalysisOptionChip(graphSeriesLabel(series, displayedUnit), selectedSeriesX == series, graphSeriesColor(series), Modifier.weight(1f)) { onSeriesXSelected(series) }
                                                            }
                                                            if (rowSeries.size == 1) Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        AnalysisOptionChip("Track A", showCompareTrackA, trackAColor, Modifier.weight(1f)) { onShowCompareTrackAChange(!showCompareTrackA) }
                                                        AnalysisOptionChip("Track B", showCompareTrackB, trackBColor, Modifier.weight(1f)) { onShowCompareTrackBChange(!showCompareTrackB) }
                                                    }
                                                    if (compareSeriesA.isNotEmpty() && compareSeriesB.isNotEmpty()) {
                                                        AnalysisOptionChip("Delta / Difference", showCompareDelta, Color(0xFFFFF176), Modifier.fillMaxWidth()) { onShowCompareDeltaChange(!showCompareDelta) }
                                                    }
                                                }
                                            }
                                            AnalysisSubTab.RANGE -> {
                                                AnalysisDeskCard("Range Selection & Crop") {
                                                    AnalysisOptionChip("Use Range", useAnalysisRange, Color(0xFFFFB300), Modifier.fillMaxWidth()) { useAnalysisRange = !useAnalysisRange }
                                                    Text("Start ${(analysisRangeStartFraction * 100).toInt()}%", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                                                    Slider(value = analysisRangeStartFraction, onValueChange = { analysisRangeStartFraction = it.coerceIn(0f, analysisRangeEndFraction - 0.02f) }, valueRange = 0f..0.98f)
                                                    Text("End ${(analysisRangeEndFraction * 100).toInt()}%", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                                                    Slider(value = analysisRangeEndFraction, onValueChange = { analysisRangeEndFraction = it.coerceIn(analysisRangeStartFraction + 0.02f, 1f) }, valueRange = 0.02f..1f)
                                                }
                                            }
                                            AnalysisSubTab.CLEANUP -> {
                                                AnalysisDeskCard("Outliers, Smoothing & Missing Data") {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        AnalysisOptionChip("Filter Outliers", filterOutliers, Color(0xFFEF5350), Modifier.weight(1f)) { filterOutliers = !filterOutliers }
                                                        AnalysisOptionChip("Smooth", showSmoothedCurve, Color(0xFFCE93D8), Modifier.weight(1f)) { showSmoothedCurve = !showSmoothedCurve }
                                                    }
                                                    Text("Smoothing strength ${(blendAlpha * 100).toInt()}%", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                                                    Slider(value = blendAlpha, onValueChange = onBlendAlphaChange, valueRange = 0f..1f)
                                                }
                                            }
                                            AnalysisSubTab.TRANSFORM -> {
                                                AnalysisDeskCard("Normalize, Blend & Baseline") {
                                                    GraphDisplayMode.values().toList().chunked(2).forEach { rowModes ->
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            rowModes.forEach { mode ->
                                                                val (label, color) = when (mode) {
                                                                    GraphDisplayMode.ORIGINAL -> "Original" to Color(0xFF00E5FF)
                                                                    GraphDisplayMode.CURATED -> "Curated" to Color(0xFF81C784)
                                                                    GraphDisplayMode.OVERLAY -> "Overlay" to Color(0xFFFFB74D)
                                                                    GraphDisplayMode.BLEND -> "Blend" to Color(0xFFCE93D8)
                                                                }
                                                                AnalysisOptionChip(label, graphDisplayMode == mode, color, Modifier.weight(1f)) { onGraphDisplayModeChange(mode) }
                                                            }
                                                            if (rowModes.size == 1) Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                    AnalysisOptionChip("Baseline Correction", baselineCorrectionEnabled, Color(0xFFEF5350), Modifier.fillMaxWidth()) { baselineCorrectionEnabled = !baselineCorrectionEnabled }
                                                    AnalysisOptionChip("Normalize 0-1", normalizeDataEnabled, Color(0xFF81C784), Modifier.fillMaxWidth()) { normalizeDataEnabled = !normalizeDataEnabled }
                                                    Text("Blend ${(blendAlpha * 100).toInt()}%", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                                                    Slider(value = blendAlpha, onValueChange = onBlendAlphaChange, valueRange = 0f..1f)
                                                }
                                            }
                                            else -> Unit
                                        }
                                    }
                                    AnalysisMainTab.MOTION -> {
                                        when (analysisSubTab) {
                                            AnalysisSubTab.MOTION -> AnalysisDeskCard("Motion Analysis") {
                                                TableCellCompact("Distance", distanceTravelled?.let(::formatCalibratedValue)?.plus(" $displayedUnit") ?: "-")
                                                TableCellCompact("Displacement", displacementValue?.let(::formatCalibratedValue)?.plus(" $displayedUnit") ?: "-")
                                                TableCellCompact("Direction", directionDegrees?.let { "%.2f deg".format(it) } ?: "-")
                                                TableCellCompact("Average Speed", averageSpeed?.let(::formatCalibratedValue)?.plus(" $displayedUnit/s") ?: "-")
                                                TableCellCompact("Average Accel", averageAcceleration?.let(::formatCalibratedValue)?.plus(" $displayedUnit/s2") ?: "-")
                                                TableCellCompact("Average Jerk", averageJerk?.let(::formatCalibratedValue)?.plus(" $displayedUnit/s3") ?: "-")
                                                TableCellCompact("Angular Position", angularPositions.lastOrNull()?.let { String.format(Locale.US, "%.5f rad", it) } ?: "-")
                                                TableCellCompact("Avg Angular Velocity", averageAngularVelocity?.let { String.format(Locale.US, "%.5f rad/s", it) } ?: "-")
                                                TableCellCompact("Avg Angular Accel", averageAngularAcceleration?.let { String.format(Locale.US, "%.5f rad/s2", it) } ?: "-")
                                                TableCellCompact("Avg Curvature", averageCurvature?.let(::formatCalibratedValue) ?: "-")
                                            }
                                            AnalysisSubTab.EVENTS -> AnalysisDeskCard("Events, Photogates & Two-Body Results") {
                                                TableCellCompact("Gate A", photogateA.timestampUs?.let(::formatSeconds)?.plus(" s") ?: "-", gateAColor)
                                                TableCellCompact("Gate B", photogateB.timestampUs?.let(::formatSeconds)?.plus(" s") ?: "-", gateBColor)
                                                TableCellCompact("Gate dt", photogateDeltaUs?.let(::formatDeltaSeconds)?.plus(" s") ?: "-")
                                                if (twoBodyFrameCount > 0) {
                                                    Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                                    TableCellCompact("Current Sep", twoBodyCurrentSeparation?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit") ?: "-")
                                                    TableCellCompact("Average Sep", twoBodyAverageSeparation?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit") ?: "-")
                                                    TableCellCompact("Current Rel Speed", twoBodyCurrentRelativeSpeed?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit/s") ?: "-")
                                                    TableCellCompact("Closest Frame", closestApproachFrame?.toString() ?: "-")
                                                    TableCellCompact("Closest Separation", closestApproachSeparation?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit") ?: "-")
                                                    TableCellCompact("Total Momentum", totalMomentum?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-")
                                                }
                                                interactionClass?.let { TableCellCompact("Interaction", it, Color(0xFFFFB74D)) }
                                            }
                                            else -> Unit
                                        }
                                    }
                                    AnalysisMainTab.STATS -> {
                                        when (analysisSubTab) {
                                            AnalysisSubTab.DISTRIBUTION -> AnalysisDeskCard("Distribution Statistics") {
                                                TableCellCompact("Current", latestValue?.let { formatCalibratedValue(it.toDouble()) } ?: "-", resolvedPrimarySeriesColor)
                                                TableCellCompact("Minimum", formatCalibratedValue(minValue.toDouble()))
                                                TableCellCompact("Maximum", formatCalibratedValue(maxValue.toDouble()))
                                                TableCellCompact("Range", formatCalibratedValue((maxValue - minValue).toDouble()))
                                                TableCellCompact("Mean", avgValue?.let { formatCalibratedValue(it.toDouble()) } ?: "-")
                                                TableCellCompact("Median", medianValue?.let(::formatCalibratedValue) ?: "-")
                                                TableCellCompact("Mode", modeValue?.let(::formatCalibratedValue) ?: "-")
                                                TableCellCompact("Std Dev", stdDevValue?.let(::formatCalibratedValue) ?: "-")
                                                TableCellCompact("Variance", varianceValue?.let(::formatCalibratedValue) ?: "-")
                                                TableCellCompact("RMS", rmsValue?.let(::formatCalibratedValue) ?: "-")
                                                TableCellCompact("P05 / P95", "${p05Value?.let(::formatCalibratedValue) ?: "-"} / ${p95Value?.let(::formatCalibratedValue) ?: "-"}")
                                                TableCellCompact("Skewness", skewnessValue?.let(::formatCalibratedValue) ?: "-")
                                                TableCellCompact("Kurtosis", kurtosisValue?.let(::formatCalibratedValue) ?: "-")
                                                TableCellCompact("Covariance", covarianceValue?.let(::formatCalibratedValue) ?: "-")
                                                TableCellCompact("Correlation", correlationValue?.let { String.format(Locale.US, "%.5f", it) } ?: "-")
                                                TableCellCompact("Histogram", histogramSummary)
                                            }
                                            AnalysisSubTab.MOTION -> AnalysisDeskCard("Motion Analysis") {
                                                TableCellCompact("Distance", distanceTravelled?.let(::formatCalibratedValue)?.plus(" $displayedUnit") ?: "-")
                                                TableCellCompact("Displacement", displacementValue?.let(::formatCalibratedValue)?.plus(" $displayedUnit") ?: "-")
                                                TableCellCompact("Direction", directionDegrees?.let { "%.2f deg".format(it) } ?: "-")
                                                TableCellCompact("Average Speed", averageSpeed?.let(::formatCalibratedValue)?.plus(" $displayedUnit/s") ?: "-")
                                                TableCellCompact("Average Accel", averageAcceleration?.let(::formatCalibratedValue)?.plus(" $displayedUnit/s2") ?: "-")
                                                TableCellCompact("Average Jerk", averageJerk?.let(::formatCalibratedValue)?.plus(" $displayedUnit/s3") ?: "-")
                                            }
                                            AnalysisSubTab.EVENTS -> AnalysisDeskCard("Events & Photogates") {
                                                TableCellCompact("Gate A", photogateA.timestampUs?.let(::formatSeconds)?.plus(" s") ?: "-", gateAColor)
                                                TableCellCompact("Gate B", photogateB.timestampUs?.let(::formatSeconds)?.plus(" s") ?: "-", gateBColor)
                                                TableCellCompact("Gate dt", photogateDeltaUs?.let(::formatDeltaSeconds)?.plus(" s") ?: "-")
                                                if (twoBodyFrameCount > 0) {
                                                    Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                                    TableCellCompact("Current Sep", twoBodyCurrentSeparation?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit") ?: "-")
                                                    TableCellCompact("Average Sep", twoBodyAverageSeparation?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit") ?: "-")
                                                    TableCellCompact("Current Rel Speed", twoBodyCurrentRelativeSpeed?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit/s") ?: "-")
                                                }
                                                interactionClass?.let { TableCellCompact("Interaction", it, Color(0xFFFFB74D)) }
                                            }
                                            else -> Unit
                                        }
                                    }
                                    AnalysisMainTab.SIGNAL -> {
                                        when (analysisSubTab) {
                                            AnalysisSubTab.FFT -> AnalysisDeskCard("FFT & Frequency Spectrum") {
                                                AnalysisOptionChip("Enable FFT", fftEnabled, Color(0xFFBA68C8), Modifier.fillMaxWidth()) { fftEnabled = !fftEnabled }
                                                TableCellCompact("Spectrum Bins", frequencySpectrum.size.toString())
                                                TableCellCompact("Dominant Frequency", dominantFrequency?.let { String.format(Locale.US, "%.5f Hz", it.frequencyHz) } ?: "-")
                                                TableCellCompact("Dominant Amplitude", dominantFrequency?.let { formatCalibratedValue(it.amplitude) } ?: "-")
                                                TableCellCompact("Dominant Power", dominantFrequency?.let { formatCalibratedValue(it.power) } ?: "-")
                                                TableCellCompact("Dominant Phase", dominantFrequency?.let { String.format(Locale.US, "%.5f rad", it.phaseRadians) } ?: "-")
                                            }
                                            AnalysisSubTab.FILTERS -> AnalysisDeskCard("Filters & Signal Processing") {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    AnalysisOptionChip("Derivative", derivativeEnabled, Color(0xFF4DD0E1), Modifier.weight(1f)) { derivativeEnabled = !derivativeEnabled }
                                                    AnalysisOptionChip("Integration", integrationEnabled, Color(0xFF81C784), Modifier.weight(1f)) { integrationEnabled = !integrationEnabled }
                                                }
                                                Text("Filter strength ${(blendAlpha * 100).toInt()}%", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                                                Slider(value = blendAlpha, onValueChange = onBlendAlphaChange, valueRange = 0f..1f)
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    AnalysisOptionChip("Low-pass", lowPassFilterEnabled, Color(0xFF80CBC4), Modifier.weight(1f)) {
                                                        lowPassFilterEnabled = !lowPassFilterEnabled
                                                        if (lowPassFilterEnabled) {
                                                            highPassFilterEnabled = false
                                                            bandPassFilterEnabled = false
                                                        }
                                                    }
                                                    AnalysisOptionChip("High-pass", highPassFilterEnabled, Color(0xFF80CBC4), Modifier.weight(1f)) {
                                                        highPassFilterEnabled = !highPassFilterEnabled
                                                        if (highPassFilterEnabled) {
                                                            lowPassFilterEnabled = false
                                                            bandPassFilterEnabled = false
                                                        }
                                                    }
                                                }
                                                AnalysisOptionChip("Band-pass", bandPassFilterEnabled, Color(0xFF80CBC4), Modifier.fillMaxWidth()) {
                                                    bandPassFilterEnabled = !bandPassFilterEnabled
                                                    if (bandPassFilterEnabled) {
                                                        lowPassFilterEnabled = false
                                                        highPassFilterEnabled = false
                                                    }
                                                }
                                            }
                                            AnalysisSubTab.BASELINE -> AnalysisDeskCard("Baseline & Peaks") {
                                                AnalysisOptionChip("Baseline Correction", baselineCorrectionEnabled, Color(0xFFEF5350), Modifier.fillMaxWidth()) { baselineCorrectionEnabled = !baselineCorrectionEnabled }
                                                AnalysisOptionChip("Peak Detection", peakDetectionEnabled, Color(0xFFFFC107), Modifier.fillMaxWidth()) { peakDetectionEnabled = !peakDetectionEnabled }
                                                TableCellCompact("Baseline", if (baselineCorrectionEnabled) "Linear endpoint subtraction" else "Off")
                                                TableCellCompact("Peak Markers", if (peakDetectionEnabled) "On graph" else "Off")
                                            }
                                            else -> Unit
                                        }
                                    }
                                    AnalysisMainTab.MEASURE -> {
                                        when (analysisSubTab) {
                                            AnalysisSubTab.CURSORS -> AnalysisDeskCard("Cursors & Measurement") {
                                                AnalysisOptionChip("Measurement Cursor", measurementCursorEnabled, Color(0xFF00E5FF), Modifier.fillMaxWidth()) { measurementCursorEnabled = !measurementCursorEnabled }
                                                AnalysisOptionChip("Coordinate Labels", forceShowCoordinates, Color(0xFF80CBC4), Modifier.fillMaxWidth()) { forceShowCoordinates = !forceShowCoordinates }
                                                selectedSample?.let { sample ->
                                                    TableCellCompact("Selected t", formatSeconds(sample.timestampUs) + " s")
                                                    TableCellCompact(graphSeriesLabel(selectedSeries, displayedUnit), formatCalibratedValue(sample.value.toDouble()), resolvedPrimarySeriesColor)
                                                    TableCellCompact("Residual", selectedSampleResidual?.let(::formatCalibratedValue) ?: "-")
                                                } ?: Text("Tap a point on the graph to inspect coordinates.", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
                                            }
                                            AnalysisSubTab.ROI -> AnalysisDeskCard("ROI Selection") {
                                                AnalysisOptionChip("ROI Selection", roiSelectionEnabled, Color(0xFFFF8A65), Modifier.fillMaxWidth()) { roiSelectionEnabled = !roiSelectionEnabled }
                                                AnalysisOptionChip("Use Range As ROI", useAnalysisRange, Color(0xFFFFB300), Modifier.fillMaxWidth()) { useAnalysisRange = !useAnalysisRange }
                                                TableCellCompact("ROI Start", "${(analysisRangeStartFraction * 100).toInt()}%")
                                                TableCellCompact("ROI End", "${(analysisRangeEndFraction * 100).toInt()}%")
                                                TableCellCompact("ROI Samples", if (useAnalysisRange) primaryGraphPoints.size.toString() else "-")
                                            }
                                            AnalysisSubTab.CALIBRATION -> AnalysisDeskCard("Calibration & Physical Units") {
                                                TableCellCompact("Display Unit", displayedUnit)
                                                TableCellCompact("X Units", xAxisUnits.ifBlank { "-" })
                                                TableCellCompact("Y Units", yAxisUnits.ifBlank { "-" })
                                                Text("Calibration and object-tracking measurement tools are kept in the Cal and Track modes, with their computed results summarized here.", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
                                            }
                                            else -> Unit
                                        }
                                    }
                                    AnalysisMainTab.GRAPH -> {
                                        when (analysisSubTab) {
                                            AnalysisSubTab.GRAPH -> AnalysisDeskCard("Graph Style") {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    AnalysisOptionChip("Major Grid", showScaleBars, Color(0xFF00E5FF), Modifier.weight(1f)) { showScaleBars = !showScaleBars }
                                                    AnalysisOptionChip("Minor Grid", showMinorGrid, Color(0xFF80CBC4), Modifier.weight(1f)) { showMinorGrid = !showMinorGrid }
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    AnalysisOptionChip("Overlay", graphLayoutMode == GraphLayoutMode.OVERLAY, PhysicsColorPrimary, Modifier.weight(1f)) { graphLayoutMode = GraphLayoutMode.OVERLAY }
                                                    AnalysisOptionChip("Multi-View", graphLayoutMode == GraphLayoutMode.STACKED, Color(0xFFBA68C8), Modifier.weight(1f)) { graphLayoutMode = GraphLayoutMode.STACKED }
                                                }
                                            }
                                            AnalysisSubTab.SERIES -> AnalysisDeskCard("Series Appearance") {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    AnalysisOptionChip("Track A", showCompareTrackA, trackAColor, Modifier.weight(1f)) { onShowCompareTrackAChange(!showCompareTrackA) }
                                                    AnalysisOptionChip("Track B", showCompareTrackB, trackBColor, Modifier.weight(1f)) { onShowCompareTrackBChange(!showCompareTrackB) }
                                                }
                                                AnalysisOptionChip("Delta", showCompareDelta, Color(0xFFFFF176), Modifier.fillMaxWidth()) { onShowCompareDeltaChange(!showCompareDelta) }
                                            }
                                            else -> Unit
                                        }
                                    }
                                    AnalysisMainTab.EXPORT -> {
                                        when (analysisSubTab) {
                                            AnalysisSubTab.FILES -> AnalysisDeskCard("Data & Graph Export") {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    CompactActionButton("CSV") { onExportCsv() }
                                                    CompactActionButton("XLSX") { onExportXlsx() }
                                                    CompactActionButton("PDF") { onExportPdf() }
                                                }
                                                CompactDataTable(primaryGraphPoints)
                                            }
                                            AnalysisSubTab.REPORT -> AnalysisDeskCard("Analysis Report") {
                                                TableCellCompact("Samples", primaryGraphPoints.size.toString())
                                                TableCellCompact("Fit", profileResult?.label ?: "-")
                                                TableCellCompact("Viewport", "${formatGraphLimitInput(visibleStartTime)} - ${formatGraphLimitInput(visibleEndTime)}")
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    CompactActionButton("Export PDF") { onExportPdf() }
                                                }
                                            }
                                            else -> Unit
                                        }
                                    }
                                }
                            }
                        } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF020604), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                                .verticalScroll(analysisDeskScrollState)
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            // Section 1: Series Selection
                            AccordionSection(
                                title = "Series Selection",
                                expanded = seriesExpanded,
                                onToggle = { seriesExpanded = !seriesExpanded }
                            ) {
                                Text("Y Axis", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                availableSeries.chunked(2).forEach { rowSeries ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowSeries.forEach { series ->
                                            AnalysisOptionChip(
                                                label = graphSeriesLabel(series, displayedUnit),
                                                selected = selectedSeries == series,
                                                accent = graphSeriesColor(series),
                                                modifier = Modifier.weight(1f),
                                                onClick = { onSeriesSelected(series) }
                                            )
                                        }
                                        if (rowSeries.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("X Axis", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                availableSeriesX.chunked(2).forEach { rowSeries ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowSeries.forEach { series ->
                                            AnalysisOptionChip(
                                                label = graphSeriesLabel(series, displayedUnit),
                                                selected = selectedSeriesX == series,
                                                accent = graphSeriesColor(series),
                                                modifier = Modifier.weight(1f),
                                                onClick = { onSeriesXSelected(series) }
                                            )
                                        }
                                        if (rowSeries.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                if (selectedSeries != GraphSeries.SEPARATION && selectedSeries != GraphSeries.RELATIVE_SPEED) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = showCompareTrackA,
                                            onClick = { onShowCompareTrackAChange(!showCompareTrackA) },
                                            label = { Text("Track A") },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = trackAColor, selectedLabelColor = Color.Black, containerColor = Color(0xFF2B2B2B), labelColor = Color.White)
                                        )
                                        FilterChip(
                                            selected = showCompareTrackB,
                                            onClick = { onShowCompareTrackBChange(!showCompareTrackB) },
                                            label = { Text("Track B") },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = trackBColor, selectedLabelColor = Color.Black, containerColor = Color(0xFF2B2B2B), labelColor = Color.White)
                                        )
                                        if (compareSeriesA.isNotEmpty() && compareSeriesB.isNotEmpty()) {
                                            FilterChip(
                                                selected = showCompareDelta,
                                                onClick = { onShowCompareDeltaChange(!showCompareDelta) },
                                                label = { Text("Δ") },
                                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFF176), selectedLabelColor = Color.Black, containerColor = Color(0xFF2B2B2B), labelColor = Color.White)
                                            )
                                        }
                                    }
                                    // Multi-view layout selector
                                    if (showCompareTrackA || showCompareTrackB || showCompareDelta) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Plot Layout",
                                            color = Color.White.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = graphLayoutMode == GraphLayoutMode.OVERLAY,
                                                onClick = { graphLayoutMode = GraphLayoutMode.OVERLAY },
                                                label = { Text("Overlay") },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PhysicsColorPrimary,
                                                    selectedLabelColor = Color.Black,
                                                    containerColor = Color(0xFF2B2B2B),
                                                    labelColor = Color.White
                                                )
                                            )
                                            FilterChip(
                                                selected = graphLayoutMode == GraphLayoutMode.STACKED,
                                                onClick = { graphLayoutMode = GraphLayoutMode.STACKED },
                                                label = { Text("Multi-View") },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PhysicsColorAccent,
                                                    selectedLabelColor = Color.Black,
                                                    containerColor = Color(0xFF2B2B2B),
                                                    labelColor = Color.White
                                                )
                                            )
                                    }
                                }
                                if (calculatedFps > 0.0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Capture rate: %.1f FPS".format(calculatedFps),
                                        color = Color.White.copy(alpha = 0.52f),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            
                            // Section 2: Scientific Statistics
                            AccordionSection(
                                title = "Scientific Statistics",
                                expanded = statsExpanded,
                                onToggle = { statsExpanded = !statsExpanded }
                            ) {
                                if (selectedSample != null) {
                                    Text("Selected Point", fontWeight = FontWeight.Bold, color = resolvedPrimarySeriesColor, style = MaterialTheme.typography.labelSmall)
                                    val sample = selectedSample!!
                                    TableCellCompact("Time", formatSeconds(sample.timestampUs) + " s")
                                    TableCellCompact(graphSeriesLabel(selectedSeries, displayedUnit), formatCalibratedValue(sample.value.toDouble()), resolvedPrimarySeriesColor)
                                    TableCellCompact("Fit Value", selectedSamplePrediction?.let(::formatCalibratedValue) ?: "-")
                                    TableCellCompact("Residual", selectedSampleResidual?.let(::formatCalibratedValue) ?: "-")
                                    TableCellCompact("% Error", selectedSamplePercentError?.let { formatCalibratedValue(it) } ?: "-")
                                    sample.detailPoint?.let { detailPoint ->
                                        TableCellCompact("x coordinate", formatCalibratedValue(detailPoint.xValue))
                                        TableCellCompact("y coordinate", formatCalibratedValue(detailPoint.yValue))
                                        TableCellCompact("Speed", formatOptionalCalibratedValue(detailPoint.speedValue))
                                    }
                                    sample.twoBodySample?.let { twoBodySample ->
                                        TableCellCompact("Frame", twoBodySample.frameNumber.toString())
                                    }
                                } else {
                                    Text("Dataset Summary", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                                    TableCellCompact("Current Value", latestValue?.let { formatCalibratedValue(it.toDouble()) } ?: "-", resolvedPrimarySeriesColor)
                                    TableCellCompact("Minimum", formatCalibratedValue(minValue.toDouble()))
                                    TableCellCompact("Maximum", formatCalibratedValue(maxValue.toDouble()))
                                    TableCellCompact("Average", avgValue?.let { formatCalibratedValue(it.toDouble()) } ?: "-")
                                    TableCellCompact("Time Span", "${formatCalibratedValue(xRangeSeconds.toDouble())} s")
                                    TableCellCompact("X/Y Zoom Level", "${String.format("%.2f", graphScaleX)}x / ${String.format("%.2f", graphScaleY)}x")
                                }

                                if (analysisMode) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                    Text("Photogates", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                                    TableCellCompact("Gate A Crossing", photogateA.timestampUs?.let(::formatSeconds)?.plus(" s") ?: "-")
                                    TableCellCompact("Gate B Crossing", photogateB.timestampUs?.let(::formatSeconds)?.plus(" s") ?: "-")
                                    TableCellCompact("Gate Δt Duration", photogateDeltaUs?.let(::formatDeltaSeconds)?.plus(" s") ?: "-")
                                    TableCellCompact("Frame Difference", if (photogateA.frameNumber != null && photogateB.frameNumber != null) kotlin.math.abs(photogateB.frameNumber - photogateA.frameNumber).toString() else "-")
                                    
                                    if (gateAToClosestUs != null || closestToGateBUs != null || photogateDeltaUs != null) {
                                        TableCellCompact("A -> Closest", gateAToClosestUs?.let(::formatDeltaSeconds)?.plus(" s") ?: "-", Color(0xFF81C784))
                                        TableCellCompact("Closest -> B", closestToGateBUs?.let(::formatDeltaSeconds)?.plus(" s") ?: "-", Color(0xFFFFF176))
                                        TableCellCompact("Gate A -> B", photogateDeltaUs?.let(::formatDeltaSeconds)?.plus(" s") ?: "-", Color(0xFFFFB74D))
                                    }
                                }

                                if (analysisMode && twoBodyFrameCount > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                    Text("Two-Body Separation", fontWeight = FontWeight.Bold, color = Color(0xFFBA68C8), style = MaterialTheme.typography.labelSmall)
                                    TableCellCompact("Current Sep", twoBodyCurrentSeparation?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit") ?: "-")
                                    TableCellCompact("Minimum Sep", twoBodyMinimumSeparation?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit") ?: "-")
                                    TableCellCompact("Maximum Sep", twoBodyMaximumSeparation?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit") ?: "-")
                                    TableCellCompact("Average Sep", twoBodyAverageSeparation?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit") ?: "-")
                                    TableCellCompact("Active Frames", twoBodyFrameCount.toString())
                                }

                                if (analysisMode && twoBodyMinimumRelativeSpeed != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                    Text("Two-Body Relative Speed", fontWeight = FontWeight.Bold, color = Color(0xFF4DD0E1), style = MaterialTheme.typography.labelSmall)
                                    TableCellCompact("Current Rel Speed", twoBodyCurrentRelativeSpeed?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit/s") ?: "-")
                                    TableCellCompact("Minimum Rel Speed", twoBodyMinimumRelativeSpeed?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit/s") ?: "-")
                                    TableCellCompact("Maximum Rel Speed", twoBodyMaximumRelativeSpeed?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit/s") ?: "-")
                                    TableCellCompact("Average Rel Speed", twoBodyAverageRelativeSpeed?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit/s") ?: "-")
                                }

                                if (analysisMode && (closestApproachFrame != null || trackAMomentum != null || trackBMomentum != null)) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                    Text("Collision & Momentum", fontWeight = FontWeight.Bold, color = Color(0xFFFFF176), style = MaterialTheme.typography.labelSmall)
                                    closestApproachFrame?.let {
                                        TableCellCompact("Closest Approach t", closestApproachTimeUs?.let(::formatSeconds)?.plus(" s") ?: "-")
                                        TableCellCompact("Closest Frame", closestApproachFrame.toString())
                                        TableCellCompact("Closest Separation", closestApproachSeparation?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit") ?: "-")
                                        TableCellCompact("Closest Rel Speed", closestApproachRelativeSpeed?.let(::formatCalibratedValue)?.plus(" $twoBodyUnit/s") ?: "-")
                                        TableCellCompact("pA @ Closest", closestApproachMomentumA?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-", trackAColor)
                                        TableCellCompact("pB @ Closest", closestApproachMomentumB?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-", trackBColor)
                                        TableCellCompact("Total p @ Closest", closestApproachTotalMomentum?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-")
                                        TableCellCompact("Rel p @ Closest", closestApproachRelativeMomentum?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-")
                                    }
                                    
                                    preInteractionSnapshot?.let { snapshot ->
                                        TableCellCompact("Before t", formatSeconds(snapshot.timestampUs) + " s")
                                        TableCellCompact("pA Before", snapshot.momentumA?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-", trackAColor)
                                        TableCellCompact("pB Before", snapshot.momentumB?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-", trackBColor)
                                        TableCellCompact("Total p Before", snapshot.totalMomentum?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-")
                                    }
                                    
                                    postInteractionSnapshot?.let { snapshot ->
                                        TableCellCompact("After t", formatSeconds(snapshot.timestampUs) + " s")
                                        TableCellCompact("pA After", snapshot.momentumA?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-", trackAColor)
                                        TableCellCompact("pB After", snapshot.momentumB?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-", trackBColor)
                                        TableCellCompact("Total p After", snapshot.totalMomentum?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-")
                                    }
                                    
                                    TableCellCompact("pA Now", trackAMomentum?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-", trackAColor)
                                    TableCellCompact("pB Now", trackBMomentum?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-", trackBColor)
                                    TableCellCompact("Total p Now", totalMomentum?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-")
                                    TableCellCompact("Relative p Now", relativeMomentum?.let(::formatCalibratedValue)?.plus(" $momentumUnitLabel") ?: "-")
                                    interactionClass?.let {
                                        TableCellCompact("Interaction Class", it, Color(0xFFFFB74D))
                                    }
                                }

                                if (timelineEvents.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                    Text("Event Timeline", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                                    
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .padding(vertical = 2.dp)
                                    ) {
                                        val left = 8.dp.toPx()
                                        val right = size.width - 8.dp.toPx()
                                        val centerY = size.height / 2f
                                        val spanStartUs = timelineEvents.first().timestampUs
                                        val spanEndUs = timelineEvents.last().timestampUs
                                        val spanUs = (spanEndUs - spanStartUs).coerceAtLeast(1L)
                                        
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.28f),
                                            start = Offset(left, centerY),
                                            end = Offset(right, centerY),
                                            strokeWidth = 2f
                                        )
                                        
                                        timelineEvents.forEach { event ->
                                            val fraction = ((event.timestampUs - spanStartUs).toFloat() / spanUs.toFloat()).coerceIn(0f, 1f)
                                            val x = left + (right - left) * fraction
                                            drawLine(
                                                color = event.color.copy(alpha = 0.7f),
                                                start = Offset(x, centerY - 8.dp.toPx()),
                                                end = Offset(x, centerY + 8.dp.toPx()),
                                                strokeWidth = 2f
                                            )
                                            drawCircle(
                                                color = event.color,
                                                radius = 4.dp.toPx(),
                                                center = Offset(x, centerY)
                                            )
                                        }
                                        
                                        val playheadFraction = ((currentAbsoluteTimeUs - spanStartUs).toFloat() / spanUs.toFloat()).coerceIn(0f, 1f)
                                        val playheadX = left + (right - left) * playheadFraction
                                        drawLine(
                                            color = Color.White,
                                            start = Offset(playheadX, 2.dp.toPx()),
                                            end = Offset(playheadX, size.height - 2.dp.toPx()),
                                            strokeWidth = 1.5.dp.toPx()
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        timelineEvents.forEach { event ->
                                            FilterChip(
                                                selected = kotlin.math.abs(currentAbsoluteTimeUs - event.timestampUs) < 40_000L,
                                                onClick = {
                                                    if (event.label == "Closest") onFocusClosestApproach()
                                                    else onFocusTimestampUs(event.timestampUs)
                                                },
                                                label = {
                                                    Text(text = "${event.actionLabel} ${formatSeconds(event.timestampUs)}s", style = MaterialTheme.typography.labelSmall)
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = event.color,
                                                    selectedLabelColor = Color.Black,
                                                    containerColor = Color(0xFF2B2B2B),
                                                    labelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Section 3: Curve Fitting
                            AccordionSection(
                                title = "Curve Fitting & Math",
                                expanded = fittingExpanded,
                                onToggle = { fittingExpanded = !fittingExpanded }
                            ) {
                                GraphProfileMode.values().toList().chunked(2).forEach { rowModes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowModes.forEach { mode ->
                                            AnalysisOptionChip(
                                                label = mode.name.lowercase().replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                                                selected = profileMode == mode,
                                                accent = Color(0xFFFF8A65),
                                                modifier = Modifier.weight(1f),
                                                onClick = { onProfileModeSelected(mode) }
                                            )
                                        }
                                        if (rowModes.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                profileResult?.let { result ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // --- Fit label + quality badge ---
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Active Fit: ${result.label}",
                                            color = Color(0xFFFF8A65),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        val qualityLabel = when {
                                            result.rSquared >= 0.99 -> "Excellent"
                                            result.rSquared >= 0.95 -> "Good"
                                            result.rSquared >= 0.85 -> "Fair"
                                            else -> "Poor"
                                        }
                                        val qualityColor = when {
                                            result.rSquared >= 0.99 -> Color(0xFF81C784)
                                            result.rSquared >= 0.95 -> Color(0xFF00E5FF)
                                            result.rSquared >= 0.85 -> Color(0xFFFFB74D)
                                            else -> Color(0xFFEF5350)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(qualityColor.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(qualityLabel, color = qualityColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    // --- Equation box ---
                                    if (result.equation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = result.equation,
                                            color = Color(0xFFFF8A65).copy(alpha = 0.85f),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                                .padding(6.dp)
                                        )
                                    }
                                    // --- 4-metric goodness-of-fit grid (2 columns) ---
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                    Text("Goodness of Fit", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            val r2Color = when {
                                                result.rSquared >= 0.99 -> Color(0xFF81C784)
                                                result.rSquared >= 0.95 -> Color(0xFF00E5FF)
                                                result.rSquared >= 0.85 -> Color(0xFFFFB74D)
                                                else -> Color(0xFFEF5350)
                                            }
                                            TableCellCompact("R²", "%.5f".format(result.rSquared), r2Color)
                                            TableCellCompact("MAE", formatCalibratedValue(result.mae))
                                        }
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            TableCellCompact("RMSE", formatCalibratedValue(result.rmse))
                                            val redChiColor = when {
                                                result.reducedChiSq <= 0.0 -> Color.White.copy(alpha = 0.4f)
                                                result.reducedChiSq < 1.5 -> Color(0xFF81C784)
                                                result.reducedChiSq < 3.0 -> Color(0xFFFFB74D)
                                                else -> Color(0xFFEF5350)
                                            }
                                            TableCellCompact("χ²_red", if (result.reducedChiSq <= 0.0) "N/A" else "%.4f".format(result.reducedChiSq), redChiColor)
                                        }
                                    }
                                    // --- Selected point readout ---
                                    if (selectedSamplePrediction != null && selectedSampleResidual != null) {
                                        Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                        Text("Selected Point", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        TableCellCompact("Fit Value", formatCalibratedValue(selectedSamplePrediction))
                                        val residualColor = if (kotlin.math.abs(selectedSampleResidual) <= result.rmse) Color(0xFF81C784) else Color(0xFFEF5350)
                                        TableCellCompact("Residual", formatCalibratedValue(selectedSampleResidual), residualColor)
                                    }
                                    // --- Parameters with ±uncertainty ---
                                    val uncertaintyMap = result.parameterUncertainties.toMap()
                                    val internalKeys = setOf("tMean", "tShift", "tOffset", "yOffset")
                                    val physicsParams = result.parameters.filter { (name, _) -> name !in internalKeys }
                                    if (physicsParams.isNotEmpty()) {
                                        Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                        Text("Fit Parameters", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        physicsParams.forEach { (name, value) ->
                                            val unc = uncertaintyMap[name]
                                            val valueStr = formatCalibratedValue(value)
                                            val displayStr = if (unc != null && unc > 0.0) "$valueStr ± ${formatCalibratedValue(unc)}" else valueStr
                                            val paramColor = if (unc != null && unc > 0.0 && value != 0.0 && kotlin.math.abs(unc / value) > 0.5)
                                                Color(0xFFFFB74D) else Color.White
                                            TableCellCompact(name, displayStr, paramColor)
                                        }
                                    }
                                    // --- Derived physics quantities ---
                                    val pmap = result.parameters.toMap()
                                    val omega = pmap["omega"] ?: pmap["omega_1"] ?: pmap["omega_d"] ?: pmap["omega_0"]
                                    val lambda = pmap["lambda"] ?: pmap["damping_beta"]
                                    val isOscillatory = result.mode in listOf(
                                        GraphProfileMode.SINE, GraphProfileMode.DAMPED_SINE,
                                        GraphProfileMode.DAMPED_COSINE, GraphProfileMode.DAMPED_HARMONIC,
                                        GraphProfileMode.MULTI_SINE, GraphProfileMode.CHIRP
                                    )
                                    val isDamped = result.mode in listOf(
                                        GraphProfileMode.DAMPED_SINE, GraphProfileMode.DAMPED_COSINE, GraphProfileMode.DAMPED_HARMONIC
                                    )
                                    if (isOscillatory && omega != null && omega > 0.0) {
                                        Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                        Text("Derived Physics", color = Color(0xFF00E5FF).copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        val period = 2.0 * kotlin.math.PI / omega
                                        TableCellCompact("Period T", "%.4f s".format(period), Color(0xFF00E5FF))
                                        TableCellCompact("Frequency f", "%.4f Hz".format(1.0 / period), Color(0xFF00E5FF))
                                        if (isDamped && lambda != null && lambda > 0.0) {
                                            TableCellCompact("Half-life τ½", "%.4f s".format(kotlin.math.ln(2.0) / lambda), Color(0xFFCE93D8))
                                            TableCellCompact("Time const τ", "%.4f s".format(1.0 / lambda), Color(0xFFCE93D8))
                                            TableCellCompact("Q-factor", "%.3f".format(omega / (2.0 * lambda)), Color(0xFFCE93D8))
                                        }
                                    }
                                } ?: run {
                                    Text("No profile fit active.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            
                            // Section 4: Data Filtering & Blending
                            AccordionSection(
                                title = "Data Filtering & Blending",
                                expanded = enhanceExpanded,
                                onToggle = { enhanceExpanded = !enhanceExpanded }
                            ) {
                                Text(
                                    "Noise & Outlier Controls",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AnalysisOptionChip(
                                        label = "Filter Outliers",
                                        selected = filterOutliers,
                                        accent = Color(0xFFEF5350),
                                        modifier = Modifier.weight(1f),
                                        onClick = { filterOutliers = !filterOutliers }
                                    )
                                    AnalysisOptionChip(
                                        label = "Smooth",
                                        selected = showSmoothedCurve,
                                        accent = Color(0xFFCE93D8),
                                        modifier = Modifier.weight(1f),
                                        onClick = { showSmoothedCurve = !showSmoothedCurve }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Outlier filter detects tracking drops using local Median Absolute Deviation (Hampel filter). Smoothing reduces quantization noise.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Trajectory Blending Mode",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                GraphDisplayMode.values().toList().chunked(2).forEach { rowModes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowModes.forEach { mode ->
                                            val (label, color) = when (mode) {
                                                GraphDisplayMode.ORIGINAL -> "Original" to Color(0xFF00E5FF)
                                                GraphDisplayMode.CURATED  -> "Curated"  to Color(0xFF81C784)
                                                GraphDisplayMode.OVERLAY  -> "Overlay"  to Color(0xFFFFB74D)
                                                GraphDisplayMode.BLEND    -> "Blend"    to Color(0xFFCE93D8)
                                            }
                                            AnalysisOptionChip(
                                                label = label,
                                                selected = graphDisplayMode == mode,
                                                accent = color,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onGraphDisplayModeChange(mode) }
                                            )
                                        }
                                        if (rowModes.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                if (graphDisplayMode == GraphDisplayMode.BLEND) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Blend: ${(blendAlpha * 100).toInt()}%  (${(blendAlpha * 100).toInt()}% physics, ${((1f - blendAlpha) * 100).toInt()}% raw)",
                                        color = Color(0xFFCE93D8),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Slider(
                                        value = blendAlpha,
                                        onValueChange = onBlendAlphaChange,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFCE93D8),
                                            activeTrackColor = Color(0xFFCE93D8)
                                        )
                                    )
                                }
                                Text(
                                    text = when (graphDisplayMode) {
                                        GraphDisplayMode.ORIGINAL -> "Showing raw experimental data with fitted curve overlay."
                                        GraphDisplayMode.CURATED  -> "Showing idealized physics curve in place of raw data."
                                        GraphDisplayMode.OVERLAY  -> "Ideal curve (solid) over raw data (faded), same axis."
                                        GraphDisplayMode.BLEND    -> "Weighted blend: α×ideal + (1-α)×raw data."
                                    },
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            // Section 5: Analysis Window Range
                            AccordionSection(
                                title = "Analysis Window & Limits",
                                expanded = rangeExpanded,
                                onToggle = { rangeExpanded = !rangeExpanded }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = useAnalysisRange,
                                        onClick = { useAnalysisRange = !useAnalysisRange },
                                        label = { Text("Use Range") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFFB300),
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF2B2B2B),
                                            labelColor = Color.White
                                        )
                                    )
                                    if (useAnalysisRange) {
                                        CompactActionButton("Reset Range") {
                                            analysisRangeStartFraction = 0.1f
                                            analysisRangeEndFraction = 0.9f
                                        }
                                    }
                                }

                                if (analysisMode) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Photogate Placements", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        CompactActionButton("Clear Gates") {
                                            onPhotogateAFractionChange(0.33f)
                                            onPhotogateBFractionChange(0.66f)
                                        }
                                    }
                                    Text(
                                        "Gate A position: ${(photogateAFraction * 100).toInt()}%",
                                        color = gateAColor,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Slider(
                                        value = photogateAFraction,
                                        onValueChange = { onPhotogateAFractionChange(it.coerceIn(0.05f, 0.95f)) },
                                        valueRange = 0.05f..0.95f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = gateAColor,
                                            activeTrackColor = gateAColor
                                        )
                                    )
                                    Text(
                                        "Gate B position: ${(photogateBFraction * 100).toInt()}%",
                                        color = gateBColor,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Slider(
                                        value = photogateBFraction,
                                        onValueChange = { onPhotogateBFractionChange(it.coerceIn(0.05f, 0.95f)) },
                                        valueRange = 0.05f..0.95f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = gateBColor,
                                            activeTrackColor = gateBColor
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Divider(color = PhysicsColorDivider.copy(alpha = 0.5f))
                                Text("Axis Limits & Scale", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = xMinLimitInput,
                                        onValueChange = { xMinLimitInput = it },
                                        singleLine = true,
                                        label = { Text("X min") },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                applyAxisLimits()
                                                focusManager.clearFocus()
                                            }
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White.copy(alpha = 0.92f),
                                            focusedLabelColor = Color.White.copy(alpha = 0.95f),
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.75f),
                                            focusedBorderColor = Color(0xFF00E5FF),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                                            cursorColor = Color(0xFF00E5FF)
                                        )
                                    )
                                    OutlinedTextField(
                                        value = xMaxLimitInput,
                                        onValueChange = { xMaxLimitInput = it },
                                        singleLine = true,
                                        label = { Text("X max") },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                applyAxisLimits()
                                                focusManager.clearFocus()
                                            }
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White.copy(alpha = 0.92f),
                                            focusedLabelColor = Color.White.copy(alpha = 0.95f),
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.75f),
                                            focusedBorderColor = Color(0xFF00E5FF),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                                            cursorColor = Color(0xFF00E5FF)
                                        )
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = yMinLimitInput,
                                        onValueChange = { yMinLimitInput = it },
                                        singleLine = true,
                                        label = { Text("Y min") },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                applyAxisLimits()
                                                focusManager.clearFocus()
                                            }
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White.copy(alpha = 0.92f),
                                            focusedLabelColor = Color.White.copy(alpha = 0.95f),
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.75f),
                                            focusedBorderColor = Color(0xFFFF8A65),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                                            cursorColor = Color(0xFFFF8A65)
                                        )
                                    )
                                    OutlinedTextField(
                                        value = yMaxLimitInput,
                                        onValueChange = { yMaxLimitInput = it },
                                        singleLine = true,
                                        label = { Text("Y max") },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                applyAxisLimits()
                                                focusManager.clearFocus()
                                            }
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White.copy(alpha = 0.92f),
                                            focusedLabelColor = Color.White.copy(alpha = 0.95f),
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.75f),
                                            focusedBorderColor = Color(0xFFFF8A65),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                                            cursorColor = Color(0xFFFF8A65)
                                        )
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Slider(
                                        modifier = Modifier.weight(1f).height(24.dp),
                                        value = visibleTimeRange,
                                        onValueChange = { requested ->
                                            graphScaleX = (baseTimeRange / requested.coerceAtLeast(baseTimeRange / 6f)).coerceIn(1f, 6f)
                                            viewportCenterX = clampViewportCenter(
                                                center = if (viewportCenterX.isNaN()) initialCenterX else viewportCenterX,
                                                visibleLength = (baseTimeRange / graphScaleX).coerceIn(baseTimeRange / 6f, baseTimeRange),
                                                domainStart = dataMinX,
                                                domainEnd = dataMaxX
                                            )
                                        },
                                        valueRange = (baseTimeRange / 6f)..baseTimeRange,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF00E5FF),
                                            activeTrackColor = Color(0xFF00E5FF)
                                        )
                                    )
                                    Slider(
                                        modifier = Modifier.weight(1f).height(24.dp),
                                        value = visibleValueRange,
                                        onValueChange = { requested ->
                                            graphScaleY = (baseValueRange / requested.coerceAtLeast(baseValueRange / 6f)).coerceIn(1f, 6f)
                                            viewportCenterY = clampViewportCenter(
                                                center = if (viewportCenterY.isNaN()) initialCenterY else viewportCenterY,
                                                visibleLength = (baseValueRange / graphScaleY).coerceIn(baseValueRange / 6f, baseValueRange),
                                                domainStart = dataMinY,
                                                domainEnd = dataMaxY
                                            )
                                        },
                                        valueRange = (baseValueRange / 6f)..baseValueRange,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFFF8A65),
                                            activeTrackColor = Color(0xFFFF8A65)
                                        )
                                    )
                                }
                            }
                            
                            // Section 6: Experimental Data Table
                            AccordionSection(
                                title = "Experimental Data Table",
                                expanded = tableExpanded,
                                onToggle = { tableExpanded = !tableExpanded }
                            ) {
                                CompactDataTable(primaryGraphPoints)
                            }
                            
                            // Section 7: Export Actions
                            AccordionSection(
                                title = "Export Actions",
                                expanded = exportExpanded,
                                onToggle = { exportExpanded = !exportExpanded }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CompactActionButton("Reset View") {
                                        graphScaleX = 1f
                                        graphScaleY = 1f
                                        viewportCenterX = initialCenterX
                                        viewportCenterY = initialCenterY
                                        manualXMin = null
                                        manualXMax = null
                                        manualYMin = null
                                        manualYMax = null
                                        onSampleSelected(-1)
                                    }
                                }
                            }
                        }
                        }
                    }
                    
                    // Vertical Splitter / Divider
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .fillMaxHeight()
                            .background(Color.Transparent)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaFraction = dragAmount.x / totalWidthPx
                                    val minimumWidth = if (analysisMode) minimumAnalysisWidth else 0.10f
                                    val target = (analysisPanelWidthFraction + deltaFraction).coerceIn(minimumWidth, 0.70f)
                                    analysisPanelWidthFraction = target
                                    if (!analysisMode && target < 0.12f) {
                                        onShowOptionPanelsChange(false)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight(0.9f)
                                .background(PhysicsColorDivider.copy(alpha = 0.5f))
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(48.dp)
                                .background(PhysicsColorDivider, RoundedCornerShape(2.dp))
                        )
                    }
                }
                
                // Right Panel: Graph Workspace
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = if (actualLeftWidthFraction > 0.05f) 4.dp else 0.dp)
                ) {
                    // Right Panel Header
                    if (!graphFullScreen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val fpsSuffix = if (calculatedFps > 0.0) " (%.1f fps)".format(calculatedFps) else ""
                            Text(
                                text = "${graphSeriesLabel(selectedSeries, displayedUnit)} vs ${graphSeriesLabel(selectedSeriesX, displayedUnit)}$fpsSuffix",
                                color = resolvedPrimarySeriesColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (!showOptionPanels && !analysisMode) {
                                    TextButton(
                                        onClick = {
                                            onShowOptionPanelsChange(true)
                                            analysisPanelWidthFraction = 0.3f
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("◀ Analysis Desk", color = PhysicsColorAccent, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                CompactToolButton("Auto Fit", accent = PhysicsColorPrimary) {
                                    graphScaleX = 1f
                                    graphScaleY = 1f
                                    viewportCenterX = Float.NaN
                                    viewportCenterY = Float.NaN
                                    manualXMin = null
                                    manualXMax = null
                                    manualYMin = null
                                    manualYMax = null
                                }
                                CompactToolButton("Grid", selected = showScaleBars, accent = Color(0xFF00E5FF)) {
                                    showScaleBars = !showScaleBars
                                }
                                CompactToolButton("Minor", selected = showMinorGrid, accent = Color(0xFF80CBC4)) {
                                    showMinorGrid = !showMinorGrid
                                }
                                CompactToolButton("Details", selected = showGraphDetails, accent = PhysicsColorAccent) {
                                    showGraphDetails = !showGraphDetails
                                }
                                CompactToolButton("Multi-View", selected = graphLayoutMode == GraphLayoutMode.STACKED, accent = Color(0xFFBA68C8)) {
                                    graphLayoutMode = if (graphLayoutMode == GraphLayoutMode.STACKED) GraphLayoutMode.OVERLAY else GraphLayoutMode.STACKED
                                }
                                IconButton(
                                    onClick = {
                                        graphScaleX = 1f
                                        graphScaleY = 1f
                                        viewportCenterX = Float.NaN
                                        viewportCenterY = Float.NaN
                                        manualXMin = null
                                        manualXMax = null
                                        manualYMin = null
                                        manualYMax = null
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text(
                                        text = "⟳",
                                        color = PhysicsColorAccent.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                IconButton(
                                    onClick = { boxZoomModeEnabled = !boxZoomModeEnabled },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text(
                                        text = if (boxZoomModeEnabled) "⬚✓" else "⬚",
                                        color = if (boxZoomModeEnabled) Color(0xFF00E5FF) else PhysicsColorAccent.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                IconButton(
                                    onClick = { onGraphFullScreenChange(!graphFullScreen) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text(
                                        text = if (graphFullScreen) "⤡" else "⤢",
                                        color = PhysicsColorAccent,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                if (showOptionPanels && !analysisMode) {
                                    TextButton(
                                        onClick = { onShowOptionPanelsChange(false) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Collapse", color = PhysicsColorOnSurface, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(if (analysisMode) 4.dp else 6.dp))
                    }
                    
                    // Graph Canvas Viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val hasRenderableGraph =
                            displaySeriesPoints.size >= 2 ||
                                (showCompareTrackA && compareSeriesA.size >= 2) ||
                                (showCompareTrackB && compareSeriesB.size >= 2) ||
                                (showCompareDelta && deltaSeries.size >= 2)

                        // Build ordered multi-view plot list. Each entry must represent distinct data.
                        val activePlots = remember(
                            selectedSeries, showCompareTrackA, showCompareTrackB, showCompareDelta,
                            displaySeriesPoints, compareSeriesA, compareSeriesB, deltaSeries,
                            residualSeriesPoints, derivativeSeriesPoints, integralSeriesPoints,
                            profileResult, derivativeEnabled, integrationEnabled,
                            displayedUnit, resolvedPrimarySeriesColor, trackAColor, trackBColor
                        ) {
                            val list = mutableListOf<SubPlotInfo>()
                            fun addDistinctPlot(plot: SubPlotInfo) {
                                val alreadyPresent = list.any { existing ->
                                    existing.label == plot.label || existing.points == plot.points
                                }
                                if (!alreadyPresent) list.add(plot)
                            }
                            list.add(
                                SubPlotInfo(
                                    label = "${graphSeriesLabel(selectedSeries, displayedUnit)} (Primary)",
                                    points = displaySeriesPoints,
                                    color = resolvedPrimarySeriesColor,
                                    isPrimary = true
                                )
                            )
                            if (showCompareTrackA && compareSeriesA.size >= 2) {
                                addDistinctPlot(SubPlotInfo(label = "Track A", points = compareSeriesA, color = trackAColor))
                            }
                            if (showCompareTrackB && compareSeriesB.size >= 2) {
                                addDistinctPlot(SubPlotInfo(label = "Track B", points = compareSeriesB, color = trackBColor))
                            }
                            if (showCompareDelta && deltaSeries.size >= 2) {
                                addDistinctPlot(SubPlotInfo(label = "Δ Delta", points = deltaSeries, color = Color(0xFFFFF176)))
                            }
                            if (profileResult != null && residualSeriesPoints.size >= 2) {
                                addDistinctPlot(SubPlotInfo(label = "Fit Residuals", points = residualSeriesPoints, color = Color(0xFFFF8A65)))
                            }
                            if (derivativeEnabled && derivativeSeriesPoints.size >= 2) {
                                addDistinctPlot(SubPlotInfo(label = "Derivative dY/dX", points = derivativeSeriesPoints, color = Color(0xFF4DD0E1)))
                            }
                            if (integrationEnabled && integralSeriesPoints.size >= 2) {
                                addDistinctPlot(SubPlotInfo(label = "Integral ∫Y dX", points = integralSeriesPoints, color = Color(0xFF81C784)))
                            }
                            list
                        }
                        val showStacked = graphLayoutMode == GraphLayoutMode.STACKED && activePlots.size >= 2
                        val axisPrecision = axisDecimalPrecision.toIntOrNull()?.coerceIn(0, 9) ?: 4
                        val tickInterval = tickIntervalInput.toFloatOrNull()?.takeIf { it > 0f }

                        if (!hasRenderableGraph) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {}
                        } else if (showStacked) {
                            // Multi-view mode: each distinct series gets its own sub-graph with independent Y axis.
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                activePlots.forEachIndexed { idx, subPlot ->
                                    val subMinY = subPlot.points.minOfOrNull { it.value } ?: 0f
                                    val subMaxY = subPlot.points.maxOfOrNull { it.value }
                                        ?.takeIf { it != subMinY } ?: (subMinY + 1f)
                                    val subBaseValueRange = closedRangeLength(subMinY, subMaxY)
                                    val subVisibleValueRange = (subBaseValueRange / graphScaleY)
                                        .coerceIn(subBaseValueRange / 6f, subBaseValueRange)
                                    val subVisibleYBounds = viewportBounds(
                                        center = (subMinY + subMaxY) / 2f,
                                        visibleLength = subVisibleValueRange,
                                        domainStart = subMinY,
                                        domainEnd = subMaxY
                                    )
                                    GraphCanvasWidget(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        label = subPlot.label,
                                        points = subPlot.points,
                                        color = subPlot.color,
                                        isPrimary = subPlot.isPrimary,
                                        isBottomSubplot = idx == activePlots.lastIndex,
                                        visibleStartTime = visibleStartTime,
                                        visibleEndTime = visibleEndTime,
                                        visibleMinValue = subVisibleYBounds.first,
                                        visibleMaxValue = subVisibleYBounds.second,
                                        dataMinX = dataMinX,
                                        dataMaxX = dataMaxX,
                                        dataMinY = subMinY,
                                        dataMaxY = subMaxY,
                                        baseTimeRange = baseTimeRange,
                                        baseValueRange = subBaseValueRange,
                                        visibleTimeRange = visibleTimeRange,
                                        visibleValueRange = subVisibleValueRange,
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
                                        analysisMode = analysisMode,
                                        showAnalysisGuides = showAnalysisGuides,
                                        photogateAFraction = photogateAFraction,
                                        onPhotogateAFractionChange = onPhotogateAFractionChange,
                                        photogateBFraction = photogateBFraction,
                                        onPhotogateBFractionChange = onPhotogateBFractionChange,
                                        useAnalysisRange = useAnalysisRange,
                                        analysisRangeStartFraction = analysisRangeStartFraction,
                                        onAnalysisRangeStartFractionChange = { analysisRangeStartFraction = it },
                                        analysisRangeEndFraction = analysisRangeEndFraction,
                                        onAnalysisRangeEndFractionChange = { analysisRangeEndFraction = it },
                                        selectedSampleIndex = selectedSampleIndex,
                                        onSampleSelected = onSampleSelected,
                                        primarySeriesPoints = displaySeriesPoints,
                                        graphDisplayMode = graphDisplayMode,
                                        curatedSeriesPoints = curatedSeriesPoints,
                                        blendedSeriesPoints = blendedSeriesPoints,
                                        profileResult = if (subPlot.isPrimary) profileResult else null,
                                        selectedSample = selectedSample,
                                        selectedSamplePrediction = if (subPlot.isPrimary) selectedSamplePrediction else null,
                                        selectedSampleResidual = if (subPlot.isPrimary) selectedSampleResidual else null,
                                        closestApproachGraphPoint = if (subPlot.isPrimary) closestApproachGraphPoint else null,
                                        onFocusClosestApproach = onFocusClosestApproach,
                                        showScaleBars = showScaleBars,
                                        showGraphDetails = showGraphDetails,
                                        boxZoomModeEnabled = boxZoomModeEnabled,
                                        onBoxZoomModeEnabledChange = { boxZoomModeEnabled = it },
                                        graphFullScreen = graphFullScreen,
                                        onGraphFullScreenChange = onGraphFullScreenChange,
                                        displayedUnit = displayedUnit,
                                        selectedSeries = selectedSeries,
                                        selectedSeriesX = selectedSeriesX,
                                        xAxisTitle = xAxisTitle,
                                        yAxisTitle = if (subPlot.isPrimary) yAxisTitle else subPlot.label,
                                        xAxisUnits = xAxisUnits,
                                        yAxisUnits = yAxisUnits,
                                        xAxisLogScale = xAxisLogScale,
                                        yAxisLogScale = yAxisLogScale,
                                        showMinorGrid = showMinorGrid,
                                        useScientificNotation = useScientificNotation,
                                        axisDecimalPrecision = axisPrecision,
                                        tickInterval = tickInterval,
                                        peakDetectionEnabled = peakDetectionEnabled,
                                        measurementCursorEnabled = measurementCursorEnabled,
                                        derivativeEnabled = derivativeEnabled,
                                        integrationEnabled = integrationEnabled,
                                        lowPassFilterEnabled = lowPassFilterEnabled,
                                        highPassFilterEnabled = highPassFilterEnabled,
                                        bandPassFilterEnabled = bandPassFilterEnabled,
                                        fftEnabled = fftEnabled,
                                        baselineCorrectionEnabled = baselineCorrectionEnabled,
                                        roiSelectionEnabled = roiSelectionEnabled,
                                        forceShowCoordinates = forceShowCoordinates,
                                        showCompareTrackA = false,
                                        compareSeriesA = emptyList(),
                                        trackAColor = trackAColor,
                                        showCompareTrackB = false,
                                        compareSeriesB = emptyList(),
                                        trackBColor = trackBColor,
                                        showCompareDelta = false,
                                        deltaSeries = emptyList()
                                    )
                                }
                            }
                        } else {
                            // Overlay mode: single graph with all series overlaid
                            GraphCanvasWidget(
                                modifier = Modifier.fillMaxSize(),
                                label = "${graphSeriesLabel(selectedSeries, displayedUnit)} (Primary)",
                                points = displaySeriesPoints,
                                color = resolvedPrimarySeriesColor,
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
                                analysisMode = analysisMode,
                                showAnalysisGuides = showAnalysisGuides,
                                photogateAFraction = photogateAFraction,
                                onPhotogateAFractionChange = onPhotogateAFractionChange,
                                photogateBFraction = photogateBFraction,
                                onPhotogateBFractionChange = onPhotogateBFractionChange,
                                useAnalysisRange = useAnalysisRange,
                                analysisRangeStartFraction = analysisRangeStartFraction,
                                onAnalysisRangeStartFractionChange = { analysisRangeStartFraction = it },
                                analysisRangeEndFraction = analysisRangeEndFraction,
                                onAnalysisRangeEndFractionChange = { analysisRangeEndFraction = it },
                                selectedSampleIndex = selectedSampleIndex,
                                onSampleSelected = onSampleSelected,
                                primarySeriesPoints = displaySeriesPoints,
                                graphDisplayMode = graphDisplayMode,
                                curatedSeriesPoints = curatedSeriesPoints,
                                blendedSeriesPoints = blendedSeriesPoints,
                                profileResult = profileResult,
                                selectedSample = selectedSample,
                                selectedSamplePrediction = selectedSamplePrediction,
                                selectedSampleResidual = selectedSampleResidual,
                                closestApproachGraphPoint = closestApproachGraphPoint,
                                onFocusClosestApproach = onFocusClosestApproach,
                                showScaleBars = showScaleBars,
                                showGraphDetails = showGraphDetails,
                                boxZoomModeEnabled = boxZoomModeEnabled,
                                onBoxZoomModeEnabledChange = { boxZoomModeEnabled = it },
                                graphFullScreen = graphFullScreen,
                                onGraphFullScreenChange = onGraphFullScreenChange,
                                displayedUnit = displayedUnit,
                                selectedSeries = selectedSeries,
                                selectedSeriesX = selectedSeriesX,
                                xAxisTitle = xAxisTitle,
                                yAxisTitle = yAxisTitle,
                                xAxisUnits = xAxisUnits,
                                yAxisUnits = yAxisUnits,
                                xAxisLogScale = xAxisLogScale,
                                yAxisLogScale = yAxisLogScale,
                                showMinorGrid = showMinorGrid,
                                useScientificNotation = useScientificNotation,
                                axisDecimalPrecision = axisPrecision,
                                tickInterval = tickInterval,
                                peakDetectionEnabled = peakDetectionEnabled,
                                measurementCursorEnabled = measurementCursorEnabled,
                                derivativeEnabled = derivativeEnabled,
                                integrationEnabled = integrationEnabled,
                                lowPassFilterEnabled = lowPassFilterEnabled,
                                highPassFilterEnabled = highPassFilterEnabled,
                                bandPassFilterEnabled = bandPassFilterEnabled,
                                fftEnabled = fftEnabled,
                                baselineCorrectionEnabled = baselineCorrectionEnabled,
                                roiSelectionEnabled = roiSelectionEnabled,
                                forceShowCoordinates = forceShowCoordinates,
                                showCompareTrackA = showCompareTrackA,
                                compareSeriesA = compareSeriesA,
                                trackAColor = trackAColor,
                                showCompareTrackB = showCompareTrackB,
                                compareSeriesB = compareSeriesB,
                                trackBColor = trackBColor,
                                showCompareDelta = showCompareDelta,
                                deltaSeries = deltaSeries
                            )
                        }
                    }
                    if (analysisMode) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(Color.Transparent)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val deltaFraction = -dragAmount.y / totalHeightPx
                                        analysisTableHeightFraction = (analysisTableHeightFraction + deltaFraction).coerceIn(0.16f, 0.46f)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(2.dp)
                                    .background(PhysicsColorDivider.copy(alpha = 0.65f), RoundedCornerShape(1.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .width(54.dp)
                                    .height(5.dp)
                                    .background(PhysicsColorAccent.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
                            )
                        }
                        Surface(
                            color = Color(0xFF020604),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider.copy(alpha = 0.75f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((totalHeightDp * analysisTableHeightFraction).coerceIn(112.dp, 280.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Synchronized Data Table",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${primaryGraphPoints.size} rows",
                                        color = Color.White.copy(alpha = 0.62f),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                CompactDataTable(primaryGraphPoints)
                            }
                        }
                    }
        } // closes Right Panel column
    } // closes Row
} // closes BoxWithConstraints
} // closes Surface
} // closes Surface lambda / FloatingGraphPanel body
} // closes FloatingGraphPanel function

@Composable
private fun AccordionSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(PhysicsColorSurfaceVariant, shape = RoundedCornerShape(8.dp))
            .border(1.dp, PhysicsColorDivider, shape = RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (expanded) "▲" else "▼",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (expanded) {
            Divider(color = PhysicsColorDivider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun AnalysisDeskCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B1F14), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Divider(color = PhysicsColorDivider.copy(alpha = 0.55f))
        content()
    }
}

@Composable
private fun TableCellCompact(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        Text(value, color = valueColor, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompactDataTable(points: List<GraphPlotPoint>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 160.dp)
            .border(1.dp, PhysicsColorDivider, RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PhysicsColorSurfaceVariant)
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Time (s)", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("Value", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Divider(color = PhysicsColorDivider)
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(points) { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(String.format("%.4f", point.timeSeconds), color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                    Text(String.format("%.4f", point.value), color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun CompactActionButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = PhysicsColorSurfaceVariant,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.heightIn(min = 28.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
private fun percentileValue(sortedValues: List<Double>, percentile: Double): Double? {
    if (sortedValues.isEmpty()) return null
    val clamped = percentile.coerceIn(0.0, 1.0)
    val index = clamped * sortedValues.lastIndex
    val lower = kotlin.math.floor(index).toInt()
    val upper = kotlin.math.ceil(index).toInt()
    if (lower == upper) return sortedValues[lower]
    val weight = index - lower
    return sortedValues[lower] * (1.0 - weight) + sortedValues[upper] * weight
}

private fun approximateMode(values: List<Double>): Double? {
    if (values.isEmpty()) return null
    return values
        .groupingBy { String.format(Locale.US, "%.3f", it) }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?.toDoubleOrNull()
}

private fun buildAxisLabel(title: String, units: String): String {
    val cleanTitle = title.ifBlank { "Axis" }
    val cleanUnits = units.trim()
    return if (cleanUnits.isBlank()) cleanTitle else "$cleanTitle ($cleanUnits)"
}

private fun formatAxisValue(value: Float, precision: Int, scientific: Boolean): String {
    val safePrecision = precision.coerceIn(0, 9)
    val pattern = if (scientific) "%.${safePrecision}e" else "%.${safePrecision}f"
    return String.format(Locale.US, pattern, value)
}

private fun axisTickValues(min: Float, max: Float, requestedInterval: Float?): List<Float> {
    if (!min.isFinite() || !max.isFinite() || max <= min) return listOf(min, max)
    val range = max - min
    val interval = requestedInterval
        ?.takeIf { it.isFinite() && it > 0f && range / it <= 16f }
        ?: (range / 4f)
    if (interval <= 0f) return listOf(min, max)
    val first = kotlin.math.ceil((min / interval).toDouble()).toFloat() * interval
    val values = mutableListOf<Float>()
    if (first > min) values.add(min)
    var current = first
    var guard = 0
    while (current <= max && guard < 18) {
        values.add(current)
        current += interval
        guard++
    }
    if (values.lastOrNull()?.let { kotlin.math.abs(it - max) > range * 0.01f } != false) {
        values.add(max)
    }
    return values.distinctBy { formatAxisValue(it, 6, false) }
}

private fun detectLocalPeaks(
    points: List<GraphPlotPoint>,
    visibleStartTime: Float,
    visibleEndTime: Float,
    visibleMinValue: Float,
    visibleMaxValue: Float
): List<GraphPlotPoint> {
    if (points.size < 3) return emptyList()
    return points.windowed(3)
        .mapNotNull { window ->
            val candidate = window[1]
            if (
                candidate.xValue in visibleStartTime..visibleEndTime &&
                candidate.value in visibleMinValue..visibleMaxValue &&
                candidate.value > window[0].value &&
                candidate.value >= window[2].value
            ) candidate else null
        }
}

private fun defaultAnalysisSubTab(tab: AnalysisMainTab): AnalysisSubTab =
    subTabsForAnalysisMain(tab).first()

private fun subTabsForAnalysisMain(tab: AnalysisMainTab): List<AnalysisSubTab> = when (tab) {
    AnalysisMainTab.DATA -> listOf(AnalysisSubTab.DATASET, AnalysisSubTab.RANGE, AnalysisSubTab.CLEANUP, AnalysisSubTab.TRANSFORM)
    AnalysisMainTab.AXES -> listOf(AnalysisSubTab.X_AXIS, AnalysisSubTab.Y_AXIS, AnalysisSubTab.GRID, AnalysisSubTab.SCALE, AnalysisSubTab.LABELS, AnalysisSubTab.TICKS)
    AnalysisMainTab.GRAPH -> listOf(AnalysisSubTab.GRAPH, AnalysisSubTab.SERIES)
    AnalysisMainTab.MOTION -> listOf(AnalysisSubTab.MOTION, AnalysisSubTab.EVENTS)
    AnalysisMainTab.FITTING -> listOf(AnalysisSubTab.REGRESSION, AnalysisSubTab.RESIDUALS)
    AnalysisMainTab.STATS -> listOf(AnalysisSubTab.DISTRIBUTION)
    AnalysisMainTab.SIGNAL -> listOf(AnalysisSubTab.FFT, AnalysisSubTab.FILTERS, AnalysisSubTab.BASELINE)
    AnalysisMainTab.MEASURE -> listOf(AnalysisSubTab.CURSORS, AnalysisSubTab.ROI, AnalysisSubTab.CALIBRATION)
    AnalysisMainTab.EXPORT -> listOf(AnalysisSubTab.FILES, AnalysisSubTab.REPORT)
}

@Composable
private fun CompactToolButton(
    label: String,
    selected: Boolean = false,
    accent: Color = PhysicsColorAccent,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.22f) else PhysicsColorSurfaceVariant.copy(alpha = 0.84f),
        contentColor = if (selected) accent else PhysicsColorOnBackground,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) accent else PhysicsColorDivider),
        modifier = Modifier
            .heightIn(min = 34.dp)
            .widthIn(min = 58.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) accent else PhysicsColorOnBackground,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnalysisOptionChip(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accent else Color(0xFF020604),
        contentColor = if (selected) Color.Black else Color.White,
        shape = RoundedCornerShape(7.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Color.White.copy(alpha = 0.92f) else accent.copy(alpha = 0.78f)
        ),
        modifier = modifier
            .heightIn(min = 38.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) Color.Black else Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun CompactPresetButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF102217),
        contentColor = Color.White,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorAccent.copy(alpha = 0.85f)),
        modifier = modifier
            .heightIn(min = 34.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ScrubberButton(label: String, frames: Int, viewModel: VideoPlayerViewModel) {
    OutlinedButton(
        onClick = { viewModel.stepFrames(frames) },
        border = androidx.compose.foundation.BorderStroke(1.dp, PhysicsColorDivider),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = PhysicsColorSurfaceVariant.copy(alpha = 0.84f),
            contentColor = PhysicsColorOnBackground
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.heightIn(min = 42.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
    }
}


private data class SubPlotInfo(
    val label: String,
    val points: List<GraphPlotPoint>,
    val color: Color,
    val isPrimary: Boolean = false
)

@Composable
private fun GraphCanvasWidget(
    modifier: Modifier = Modifier,
    label: String,
    points: List<GraphPlotPoint>,
    color: Color,
    isPrimary: Boolean,
    isBottomSubplot: Boolean,
    visibleStartTime: Float,
    visibleEndTime: Float,
    visibleMinValue: Float,
    visibleMaxValue: Float,
    dataMinX: Float,
    dataMaxX: Float,
    dataMinY: Float,
    dataMaxY: Float,
    baseTimeRange: Float,
    baseValueRange: Float,
    visibleTimeRange: Float,
    visibleValueRange: Float,
    graphScaleX: Float,
    onGraphScaleXChange: (Float) -> Unit,
    graphScaleY: Float,
    onGraphScaleYChange: (Float) -> Unit,
    viewportCenterX: Float,
    onViewportCenterXChange: (Float) -> Unit,
    viewportCenterY: Float,
    onViewportCenterYChange: (Float) -> Unit,
    manualXMin: Float?,
    onManualXMinChange: (Float?) -> Unit,
    manualXMax: Float?,
    onManualXMaxChange: (Float?) -> Unit,
    manualYMin: Float?,
    onManualYMinChange: (Float?) -> Unit,
    manualYMax: Float?,
    onManualYMaxChange: (Float?) -> Unit,
    analysisMode: Boolean,
    showAnalysisGuides: Boolean,
    photogateAFraction: Float,
    onPhotogateAFractionChange: (Float) -> Unit,
    photogateBFraction: Float,
    onPhotogateBFractionChange: (Float) -> Unit,
    useAnalysisRange: Boolean,
    analysisRangeStartFraction: Float,
    onAnalysisRangeStartFractionChange: (Float) -> Unit,
    analysisRangeEndFraction: Float,
    onAnalysisRangeEndFractionChange: (Float) -> Unit,
    selectedSampleIndex: Int,
    onSampleSelected: (Int) -> Unit,
    primarySeriesPoints: List<GraphPlotPoint>,
    graphDisplayMode: GraphDisplayMode,
    curatedSeriesPoints: List<GraphPlotPoint>,
    blendedSeriesPoints: List<GraphPlotPoint>,
    profileResult: ProfileResult?,
    selectedSample: GraphPlotPoint?,
    selectedSamplePrediction: Double?,
    selectedSampleResidual: Double?,
    closestApproachGraphPoint: GraphPlotPoint?,
    onFocusClosestApproach: () -> Unit,
    showScaleBars: Boolean,
    showGraphDetails: Boolean,
    boxZoomModeEnabled: Boolean,
    onBoxZoomModeEnabledChange: (Boolean) -> Unit,
    graphFullScreen: Boolean,
    onGraphFullScreenChange: (Boolean) -> Unit,
    displayedUnit: String,
    selectedSeries: GraphSeries,
    selectedSeriesX: GraphSeries,
    xAxisTitle: String,
    yAxisTitle: String,
    xAxisUnits: String,
    yAxisUnits: String,
    xAxisLogScale: Boolean,
    yAxisLogScale: Boolean,
    showMinorGrid: Boolean,
    useScientificNotation: Boolean,
    axisDecimalPrecision: Int,
    tickInterval: Float?,
    peakDetectionEnabled: Boolean,
    measurementCursorEnabled: Boolean,
    derivativeEnabled: Boolean,
    integrationEnabled: Boolean,
    lowPassFilterEnabled: Boolean,
    highPassFilterEnabled: Boolean,
    bandPassFilterEnabled: Boolean,
    fftEnabled: Boolean,
    baselineCorrectionEnabled: Boolean,
    roiSelectionEnabled: Boolean,
    forceShowCoordinates: Boolean,
    
    // Comparison series overlay (for Overlay mode)
    showCompareTrackA: Boolean,
    compareSeriesA: List<GraphPlotPoint>,
    trackAColor: Color,
    showCompareTrackB: Boolean,
    compareSeriesB: List<GraphPlotPoint>,
    trackBColor: Color,
    showCompareDelta: Boolean,
    deltaSeries: List<GraphPlotPoint>
) {
    var draggedGraphGateHandle by remember { mutableStateOf<PhotogateHandle?>(null) }
    var draggedRangeHandle by remember { mutableStateOf<Int?>(null) } // 0 = start, 1 = end
    var graphViewportSize by remember { mutableStateOf(IntSize.Zero) }

    var boxZoomStartOffset by remember { mutableStateOf<Offset?>(null) }
    var boxZoomCurrentOffset by remember { mutableStateOf<Offset?>(null) }

    val resolvedPrimarySeriesColor = color
    val valueRange = closedRangeLength(visibleMinValue, visibleMaxValue)
    val timeRange = closedRangeLength(visibleStartTime, visibleEndTime)
    val xAxisLabel = buildAxisLabel(
        xAxisTitle.ifBlank { graphSeriesLabel(selectedSeriesX, displayedUnit) },
        xAxisUnits
    ).let { if (xAxisLogScale) "$it log" else it }
    val yAxisLabel = buildAxisLabel(
        yAxisTitle.ifBlank { graphSeriesLabel(selectedSeries, displayedUnit) },
        yAxisUnits
    ).let { if (yAxisLogScale) "$it log" else it }
    val activeAnalysisLabels = buildList {
        if (peakDetectionEnabled) add("Peaks")
        if (measurementCursorEnabled) add("Cursor")
        if (derivativeEnabled) add("Derivative")
        if (integrationEnabled) add("Integral")
        if (lowPassFilterEnabled) add("Low-pass")
        if (highPassFilterEnabled) add("High-pass")
        if (bandPassFilterEnabled) add("Band-pass")
        if (fftEnabled) add("FFT")
        if (baselineCorrectionEnabled) add("Baseline")
        if (roiSelectionEnabled) add("ROI")
    }

    val gateAColor = Color(0xFF81C784)
    val gateBColor = Color(0xFFFFB74D)

    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { graphViewportSize = it }
            .pointerInput(points, graphScaleX, graphScaleY, visibleStartTime, visibleEndTime, visibleMinValue, visibleMaxValue, graphViewportSize, analysisMode, photogateAFraction, photogateBFraction, boxZoomModeEnabled) {
                if (!boxZoomModeEnabled) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val viewportWidth = graphViewportSize.width.toFloat().coerceAtLeast(1f)
                        val viewportHeight = graphViewportSize.height.toFloat().coerceAtLeast(1f)
                        val leftPad = 72f
                        val rightPad = 12f
                        val topPad = 12f
                        val bottomPad = 44f
                        val chartWidth = (viewportWidth - leftPad - rightPad).coerceAtLeast(1f)
                        val chartHeight = (viewportHeight - topPad - bottomPad).coerceAtLeast(1f)
                        val oldTimeRange = visibleTimeRange
                        val oldValueRange = visibleValueRange
                        if (manualXMin != null || manualXMax != null || manualYMin != null || manualYMax != null) {
                            onManualXMinChange(null)
                            onManualXMaxChange(null)
                            onManualYMinChange(null)
                            onManualYMaxChange(null)
                        }
                        val newScaleX = (graphScaleX * zoom).coerceIn(1f, 6f)
                        val newScaleY = (graphScaleY * zoom).coerceIn(1f, 6f)
                        val newTimeRange = (baseTimeRange / newScaleX).coerceIn(baseTimeRange / 6f, baseTimeRange)
                        val newValueRangeAfterZoom = (baseValueRange / newScaleY).coerceIn(baseValueRange / 6f, baseValueRange)
                        val focusFractionX = ((centroid.x - leftPad) / chartWidth).coerceIn(0f, 1f)
                        val focusFractionY = ((centroid.y - topPad) / chartHeight).coerceIn(0f, 1f)
                        val anchorX = visibleStartTime + oldTimeRange * focusFractionX
                        // Y is inverted: top of chart = max value, bottom = min
                        val anchorY = visibleMaxValue - oldValueRange * focusFractionY

                        onGraphScaleXChange(newScaleX)
                        onGraphScaleYChange(newScaleY)
                        onViewportCenterXChange(
                            clampViewportCenter(
                                center = anchorX + (0.5f - focusFractionX) * newTimeRange - (pan.x / chartWidth) * newTimeRange,
                                visibleLength = newTimeRange,
                                domainStart = dataMinX,
                                domainEnd = dataMaxX
                            )
                        )
                        // Apply vertical pan: dragging up increases center (shows higher values)
                        onViewportCenterYChange(
                            clampViewportCenter(
                                center = anchorY + (0.5f - focusFractionY) * newValueRangeAfterZoom + (pan.y / chartHeight) * newValueRangeAfterZoom,
                                visibleLength = newValueRangeAfterZoom,
                                domainStart = dataMinY,
                                domainEnd = dataMaxY
                            )
                        )
                    }
                }
            }
            .pointerInput(points, visibleStartTime, visibleEndTime, visibleMinValue, visibleMaxValue, boxZoomModeEnabled) {
                if (boxZoomModeEnabled) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            boxZoomStartOffset = offset
                            boxZoomCurrentOffset = offset
                        },
                        onDragEnd = {
                            val start = boxZoomStartOffset
                            val end = boxZoomCurrentOffset
                            if (start != null && end != null) {
                                val leftPad = 72f
                                val rightPad = 12f
                                val topPad = 12f
                                val bottomPad = 44f
                                val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                                val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                                val t1 = (visibleStartTime + ((start.x - leftPad) / chartWidth) * timeRange).coerceIn(dataMinX, dataMaxX)
                                val t2 = (visibleStartTime + ((end.x - leftPad) / chartWidth) * timeRange).coerceIn(dataMinX, dataMaxX)
                                val v1 = (visibleMaxValue - ((start.y - topPad) / chartHeight) * valueRange).coerceIn(dataMinY, dataMaxY)
                                val v2 = (visibleMaxValue - ((end.y - topPad) / chartHeight) * valueRange).coerceIn(dataMinY, dataMaxY)

                                if (kotlin.math.abs(t1 - t2) > 0.01f && kotlin.math.abs(v1 - v2) > 0.01f) {
                                    onManualXMinChange(minOf(t1, t2))
                                    onManualXMaxChange(maxOf(t1, t2))
                                    onManualYMinChange(minOf(v1, v2))
                                    onManualYMaxChange(maxOf(v1, v2))
                                    onBoxZoomModeEnabledChange(false)
                                }
                            }
                            boxZoomStartOffset = null
                            boxZoomCurrentOffset = null
                        },
                        onDragCancel = {
                            boxZoomStartOffset = null
                            boxZoomCurrentOffset = null
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        boxZoomCurrentOffset = change.position
                    }
                }
            }
            .pointerInput(points, visibleStartTime, visibleEndTime, visibleMinValue, visibleMaxValue, analysisMode, photogateAFraction, photogateBFraction, useAnalysisRange, analysisRangeStartFraction, analysisRangeEndFraction) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val leftPad = 72f
                        val rightPad = 12f
                        val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                        if (useAnalysisRange) {
                            val rangeStartX = leftPad + chartWidth * analysisRangeStartFraction.coerceIn(0.02f, 0.98f)
                            val rangeEndX   = leftPad + chartWidth * analysisRangeEndFraction.coerceIn(0.02f, 0.98f)
                            draggedRangeHandle = when {
                                kotlin.math.abs(offset.x - rangeStartX) <= 28f -> 0
                                kotlin.math.abs(offset.x - rangeEndX)   <= 28f -> 1
                                else -> null
                            }
                            if (draggedRangeHandle != null) {
                                draggedGraphGateHandle = null
                                return@detectDragGestures
                            }
                        }
                        draggedRangeHandle = null
                        if (!analysisMode) {
                            draggedGraphGateHandle = null
                            return@detectDragGestures
                        }
                        val gateAX = leftPad + chartWidth * photogateAFraction.coerceIn(0.05f, 0.95f)
                        val gateBX = leftPad + chartWidth * photogateBFraction.coerceIn(0.05f, 0.95f)
                        draggedGraphGateHandle = when {
                            isInsidePhotogateHandle(offset, gateAX, 1f) -> PhotogateHandle.A
                            isInsidePhotogateHandle(offset, gateBX, 1f) -> PhotogateHandle.B
                            else -> null
                        }
                    },
                    onDragEnd = { draggedGraphGateHandle = null; draggedRangeHandle = null },
                    onDragCancel = { draggedGraphGateHandle = null; draggedRangeHandle = null }
                ) { change, _ ->
                    val leftPad = 72f
                    val rightPad = 12f
                    val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                    val rHandle = draggedRangeHandle
                    if (rHandle != null) {
                        val fraction = ((change.position.x - leftPad) / chartWidth).coerceIn(0.02f, 0.98f)
                        change.consume()
                        if (rHandle == 0) {
                            onAnalysisRangeStartFractionChange(fraction.coerceAtMost(analysisRangeEndFraction - 0.05f))
                        } else {
                            onAnalysisRangeEndFractionChange(fraction.coerceAtLeast(analysisRangeStartFraction + 0.05f))
                        }
                        return@detectDragGestures
                    }
                    val handle = draggedGraphGateHandle ?: return@detectDragGestures
                    val fraction = ((change.position.x - leftPad) / chartWidth).coerceIn(0.05f, 0.95f)
                    change.consume()
                    if (handle == PhotogateHandle.A) {
                        onPhotogateAFractionChange(fraction)
                    } else {
                        onPhotogateBFractionChange(fraction)
                    }
                }
            }
            .pointerInput(points, visibleStartTime, visibleEndTime, visibleMinValue, visibleMaxValue, graphFullScreen) {
                detectTapGestures(
                    onDoubleTap = {
                        onManualXMinChange(null)
                        onManualXMaxChange(null)
                        onManualYMinChange(null)
                        onManualYMaxChange(null)
                        onGraphScaleXChange(1f)
                        onGraphScaleYChange(1f)
                        onViewportCenterXChange(Float.NaN)
                        onViewportCenterYChange(Float.NaN)
                    },
                    onTap = { offset ->
                        val leftPad = 72f
                        val rightPad = 12f
                        val topPad = 12f
                        val bottomPad = 44f
                        val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                        val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                        val tRange = closedRangeLength(visibleStartTime, visibleEndTime)
                        val vRange = closedRangeLength(visibleMinValue, visibleMaxValue)
                        closestApproachGraphPoint?.let { point ->
                            val markerX = leftPad + ((point.xValue - visibleStartTime) / tRange) * chartWidth
                            val markerY = topPad + chartHeight - ((point.value - visibleMinValue) / vRange) * chartHeight
                            if (distanceBetween(offset, Offset(markerX, markerY)) <= 28f) {
                                onFocusClosestApproach()
                                return@detectTapGestures
                            }
                        }
                        val hit = points.mapIndexed { index, point ->
                            val x = leftPad + ((point.xValue - visibleStartTime) / tRange) * chartWidth
                            val y = topPad + chartHeight - ((point.value - visibleMinValue) / vRange) * chartHeight
                            index to distanceBetween(offset, Offset(x, y))
                        }.minByOrNull { it.second }
                        val hitRadius = 36f
                        if (hit != null && hit.second <= hitRadius) {
                            if (isPrimary) {
                                onSampleSelected(hit.first)
                            } else {
                                val targetTime = points[hit.first].xValue
                                val primaryIndex = primarySeriesPoints.mapIndexed { idx, p ->
                                    idx to kotlin.math.abs(p.xValue - targetTime)
                                }.minByOrNull { it.second }?.first ?: -1
                                onSampleSelected(primaryIndex)
                            }
                        } else {
                            onSampleSelected(-1)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPad = 72f
            val rightPad = 12f
            val topPad = 12f
            val bottomPad = 44f
            val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val plotLeft = leftPad
            val plotTop = topPad
            val plotRight = leftPad + chartWidth
            val plotBottom = topPad + chartHeight
            val positiveMinX = points
                .asSequence()
                .map { it.xValue }
                .filter { it > 0f && it in visibleStartTime..visibleEndTime }
                .minOrNull() ?: visibleStartTime.takeIf { it > 0f } ?: 1e-6f
            val positiveMinY = points
                .asSequence()
                .map { it.value }
                .filter { it > 0f && it in visibleMinValue..visibleMaxValue }
                .minOrNull() ?: visibleMinValue.takeIf { it > 0f } ?: 1e-6f
            val logStartX = positiveMinX.coerceAtLeast(1e-6f)
            val logEndX = visibleEndTime.coerceAtLeast(logStartX * 1.0001f)
            val logStartY = positiveMinY.coerceAtLeast(1e-6f)
            val logEndY = visibleMaxValue.coerceAtLeast(logStartY * 1.0001f)
            fun xToScreen(raw: Float): Float {
                val fraction = if (xAxisLogScale && raw > 0f) {
                    val denom = kotlin.math.ln(logEndX.toDouble()) - kotlin.math.ln(logStartX.toDouble())
                    if (denom > 0.0) {
                        ((kotlin.math.ln(raw.coerceAtLeast(logStartX).toDouble()) - kotlin.math.ln(logStartX.toDouble())) / denom).toFloat()
                    } else {
                        (raw - visibleStartTime) / timeRange
                    }
                } else {
                    (raw - visibleStartTime) / timeRange
                }
                return leftPad + fraction.coerceIn(0f, 1f) * chartWidth
            }
            fun yToScreen(raw: Float): Float {
                val fractionFromBottom = if (yAxisLogScale && raw > 0f) {
                    val denom = kotlin.math.ln(logEndY.toDouble()) - kotlin.math.ln(logStartY.toDouble())
                    if (denom > 0.0) {
                        ((kotlin.math.ln(raw.coerceAtLeast(logStartY).toDouble()) - kotlin.math.ln(logStartY.toDouble())) / denom).toFloat()
                    } else {
                        (raw - visibleMinValue) / valueRange
                    }
                } else {
                    (raw - visibleMinValue) / valueRange
                }
                return topPad + chartHeight - fractionFromBottom.coerceIn(0f, 1f) * chartHeight
            }

            drawRect(
                color = Color(0xFF07110C),
                topLeft = Offset.Zero,
                size = size
            )
            drawRoundRect(
                color = Color(0xFF020604),
                topLeft = Offset(plotLeft, plotTop),
                size = Size(chartWidth, chartHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.20f),
                topLeft = Offset(plotLeft, plotTop),
                size = Size(chartWidth, chartHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                style = Stroke(width = 1.4f)
            )

            drawLine(
                color = Color.White.copy(alpha = 0.62f),
                start = Offset(leftPad, topPad),
                end = Offset(leftPad, topPad + chartHeight),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.62f),
                start = Offset(leftPad, topPad + chartHeight),
                end = Offset(leftPad + chartWidth, topPad + chartHeight),
                strokeWidth = 2f
            )

            val xMajorTicks = axisTickValues(visibleStartTime, visibleEndTime, tickInterval)
            val yMajorTicks = axisTickValues(visibleMinValue, visibleMaxValue, tickInterval)
            val xMajorFractions = xMajorTicks.map { ((xToScreen(it) - leftPad) / chartWidth).coerceIn(0f, 1f) }
            val yMajorFractions = yMajorTicks.map { ((yToScreen(it) - topPad) / chartHeight).coerceIn(0f, 1f) }
            xMajorFractions.forEach { fraction ->
                val x = leftPad + (chartWidth * fraction)
                drawLine(
                    color = Color.White.copy(alpha = if (showScaleBars) 0.20f else 0.09f),
                    start = Offset(x, topPad),
                    end = Offset(x, topPad + chartHeight),
                    strokeWidth = 1f
                )
            }
            yMajorFractions.forEach { fraction ->
                val y = topPad + (chartHeight * fraction)
                drawLine(
                    color = Color.White.copy(alpha = if (showScaleBars) 0.20f else 0.09f),
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + chartWidth, y),
                    strokeWidth = 1f
                )
            }
            if (showMinorGrid && showScaleBars) {
                xMajorFractions.zipWithNext { a, b ->
                    repeat(4) { step ->
                        val fraction = a + (b - a) * ((step + 1) / 5f)
                        val x = leftPad + chartWidth * fraction
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(x, topPad),
                            end = Offset(x, topPad + chartHeight),
                            strokeWidth = 0.8f
                        )
                    }
                }
                yMajorFractions.zipWithNext { a, b ->
                    repeat(4) { step ->
                        val fraction = a + (b - a) * ((step + 1) / 5f)
                        val y = topPad + chartHeight * fraction
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(leftPad, y),
                            end = Offset(leftPad + chartWidth, y),
                            strokeWidth = 0.8f
                        )
                    }
                }
            }
            if (dataMinY <= 0f && dataMaxY >= 0f) {
                val zeroY = yToScreen(0f)
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(leftPad, zeroY),
                    end = Offset(leftPad + chartWidth, zeroY),
                    strokeWidth = 1.5f
                )
            }
            clipRect(left = plotLeft, top = plotTop, right = plotRight, bottom = plotBottom) {
                if (useAnalysisRange || roiSelectionEnabled) {
                    val startX = leftPad + chartWidth * analysisRangeStartFraction.coerceIn(0.02f, 0.98f)
                    val endX   = leftPad + chartWidth * analysisRangeEndFraction.coerceIn(0.02f, 0.98f)
                    drawRect(
                        color = Color.Black.copy(alpha = 0.38f),
                        topLeft = Offset(leftPad, topPad),
                        size = androidx.compose.ui.geometry.Size(startX - leftPad, chartHeight)
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.38f),
                        topLeft = Offset(endX, topPad),
                        size = androidx.compose.ui.geometry.Size(leftPad + chartWidth - endX, chartHeight)
                    )
                    drawRect(
                        color = Color(0xFFFFB300).copy(alpha = 0.07f),
                        topLeft = Offset(startX, topPad),
                        size = androidx.compose.ui.geometry.Size(endX - startX, chartHeight)
                    )
                    val startDragged = draggedRangeHandle == 0
                    drawLine(
                        color = Color(0xFFFFB300).copy(alpha = if (startDragged) 1f else 0.85f),
                        start = Offset(startX, topPad),
                        end = Offset(startX, topPad + chartHeight),
                        strokeWidth = if (startDragged) 3.5f else 2.5f
                    )
                    val endDragged = draggedRangeHandle == 1
                    drawLine(
                        color = Color(0xFFFFB300).copy(alpha = if (endDragged) 1f else 0.85f),
                        start = Offset(endX, topPad),
                        end = Offset(endX, topPad + chartHeight),
                        strokeWidth = if (endDragged) 3.5f else 2.5f
                    )
                    drawCircle(Color(0xFFFFB300), radius = 7f, center = Offset(startX, topPad + 10f))
                    drawCircle(Color(0xFFFFB300), radius = 7f, center = Offset(endX, topPad + 10f))
                }
                if (analysisMode && showAnalysisGuides) {
                    listOf(
                        Triple("A", photogateAFraction, gateAColor),
                        Triple("B", photogateBFraction, gateBColor)
                    ).forEach { (label, fraction, color) ->
                        val x = leftPad + chartWidth * fraction.coerceIn(0.05f, 0.95f)
                        val dragged = (label == "A" && draggedGraphGateHandle == PhotogateHandle.A) ||
                            (label == "B" && draggedGraphGateHandle == PhotogateHandle.B)
                        drawLine(
                            color = color.copy(alpha = if (dragged) 1f else 0.92f),
                            start = Offset(x, topPad),
                            end = Offset(x, topPad + chartHeight),
                            strokeWidth = if (dragged) 3.4f else 2.2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f))
                        )
                    }
                }

                // Underlay/overlay comparison plots (only for overlay mode)
                fun drawCompareSeries(seriesList: List<GraphPlotPoint>, color: Color, alpha: Float, strokeWidth: Float) {
                    val comparePath = Path()
                    var started = false
                    seriesList.forEach { point ->
                        val x = xToScreen(point.xValue)
                        val y = yToScreen(point.value)
                        if (point.xValue in visibleStartTime..visibleEndTime) {
                            if (!started) {
                                comparePath.moveTo(x, y)
                                started = true
                            } else {
                                comparePath.lineTo(x, y)
                            }
                        }
                    }
                    if (started) {
                        drawPath(
                            path = comparePath,
                            color = Color.Black.copy(alpha = 0.72f),
                            style = Stroke(width = strokeWidth + 3.2f)
                        )
                        drawPath(
                            path = comparePath,
                            color = color.copy(alpha = alpha.coerceAtLeast(0.82f)),
                            style = Stroke(width = strokeWidth + 0.8f)
                        )
                    }
                }

                if (selectedSeries != GraphSeries.SEPARATION && selectedSeries != GraphSeries.RELATIVE_SPEED) {
                    if (showCompareTrackA) {
                        drawCompareSeries(compareSeriesA, trackAColor, 0.58f, 2.2f)
                    }
                    if (showCompareTrackB) {
                        drawCompareSeries(compareSeriesB, trackBColor, 0.58f, 2.2f)
                    }
                    if (showCompareDelta) {
                        drawCompareSeries(deltaSeries, Color(0xFFFFF176), 0.7f, 1.8f)
                    }
                }

                val pointsToDraw = when (graphDisplayMode) {
                    GraphDisplayMode.ORIGINAL -> points
                    GraphDisplayMode.CURATED -> if (curatedSeriesPoints.isNotEmpty() && isPrimary) curatedSeriesPoints else points
                    GraphDisplayMode.BLEND -> if (isPrimary) blendedSeriesPoints else points
                    GraphDisplayMode.OVERLAY -> if (curatedSeriesPoints.isNotEmpty() && isPrimary) curatedSeriesPoints else points
                }

                if (graphDisplayMode == GraphDisplayMode.OVERLAY && curatedSeriesPoints.isNotEmpty() && isPrimary) {
                    val rawPath = Path()
                    var startedRawPath = false
                    points.forEach { point ->
                        val x = xToScreen(point.xValue)
                        val y = yToScreen(point.value)
                        if (point.xValue in visibleStartTime..visibleEndTime) {
                            if (!startedRawPath) {
                                rawPath.moveTo(x, y)
                                startedRawPath = true
                            } else {
                                rawPath.lineTo(x, y)
                            }
                        }
                    }
                    if (startedRawPath) {
                        drawPath(
                            path = rawPath,
                            color = resolvedPrimarySeriesColor.copy(alpha = 0.35f),
                            style = Stroke(width = 2.5f)
                        )
                    }
                    points.forEach { point ->
                        if (point.xValue in visibleStartTime..visibleEndTime && point.value in visibleMinValue..visibleMaxValue) {
                            val x = xToScreen(point.xValue)
                            val y = yToScreen(point.value)
                            drawCircle(
                                color = resolvedPrimarySeriesColor.copy(alpha = 0.3f),
                                radius = 3.5f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                val graphPath = Path()
                var startedGraphPath = false
                pointsToDraw.forEachIndexed { index, point ->
                    val x = xToScreen(point.xValue)
                    val y = yToScreen(point.value)
                    if (point.xValue in visibleStartTime..visibleEndTime) {
                        if (!startedGraphPath) {
                            graphPath.moveTo(x, y)
                            startedGraphPath = true
                        } else {
                            graphPath.lineTo(x, y)
                        }
                    }
                    val isSelectedPoint = selectedSampleIndex == index && isPrimary
                    val isLatestPoint = index == pointsToDraw.lastIndex && isPrimary
                    if (point.xValue in visibleStartTime..visibleEndTime && point.value in visibleMinValue..visibleMaxValue) {
                        if (isLatestPoint) {
                            drawCircle(
                                color = resolvedPrimarySeriesColor.copy(alpha = 0.22f),
                                radius = 10f,
                                center = Offset(x, y)
                            )
                        }
                        val pointRadius = if (isSelectedPoint) 8.5f else if (isLatestPoint) 6.5f else 4.5f
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.86f),
                            radius = pointRadius + 2.5f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = resolvedPrimarySeriesColor,
                            radius = pointRadius,
                            center = Offset(x, y)
                        )
                        if (isSelectedPoint) {
                            drawCircle(
                                color = Color.White,
                                radius = 13f,
                                center = Offset(x, y),
                                style = Stroke(width = 2f)
                            )
                        } else if (isLatestPoint) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.72f),
                                radius = 9.5f,
                                center = Offset(x, y),
                                style = Stroke(width = 1.6f)
                            )
                        }
                    }
                }
                if (startedGraphPath) {
                    drawPath(
                        path = graphPath,
                        color = Color.Black.copy(alpha = 0.84f),
                        style = Stroke(width = 7f)
                    )
                    drawPath(
                        path = graphPath,
                        color = resolvedPrimarySeriesColor,
                        style = Stroke(width = 4f)
                    )
                }
                if (peakDetectionEnabled) {
                    detectLocalPeaks(pointsToDraw, visibleStartTime, visibleEndTime, visibleMinValue, visibleMaxValue)
                        .take(24)
                        .forEach { peak ->
                            val peakX = xToScreen(peak.xValue)
                            val peakY = yToScreen(peak.value)
                            drawCircle(Color(0xFFFFC107).copy(alpha = 0.24f), radius = 12f, center = Offset(peakX, peakY))
                            drawCircle(Color(0xFFFFC107), radius = 5.5f, center = Offset(peakX, peakY))
                        }
                }

                // Synced Hover Crosshair and Dot on all plots
                if (measurementCursorEnabled || forceShowCoordinates) selectedSample?.let { sample ->
                    val closest = points.minByOrNull { kotlin.math.abs(it.xValue - sample.xValue) }
                    if (closest != null && closest.xValue in visibleStartTime..visibleEndTime && closest.value in visibleMinValue..visibleMaxValue) {
                        val sampleX = xToScreen(closest.xValue)
                        val sampleY = yToScreen(closest.value)
                        
                        // Vertical line
                        drawLine(
                            color = Color.White.copy(alpha = 0.28f),
                            start = Offset(sampleX, topPad),
                            end = Offset(sampleX, topPad + chartHeight),
                            strokeWidth = 1.2f
                        )
                        // Horizontal line
                        drawLine(
                            color = Color.White.copy(alpha = 0.28f),
                            start = Offset(leftPad, sampleY),
                            end = Offset(leftPad + chartWidth, sampleY),
                            strokeWidth = 1.2f
                        )
                        
                        // Dot readout
                        drawCircle(
                            color = color,
                            radius = 6f,
                            center = Offset(sampleX, sampleY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 9f,
                            center = Offset(sampleX, sampleY),
                            style = Stroke(width = 1.5f)
                        )

                        // Info tooltip readout
                        with(density) {
                            val showThreeLines = selectedSeriesX != GraphSeries.TIME
                            val labelWidth = 160.dp.toPx()
                            val labelHeight = if (showThreeLines) 58.dp.toPx() else 44.dp.toPx()
                            val labelX = (sampleX + 12.dp.toPx()).coerceIn(plotLeft, plotRight - labelWidth)
                            val labelY = (sampleY - labelHeight - 10.dp.toPx()).coerceIn(plotTop, plotBottom - labelHeight)
                            drawRect(
                                color = Color.Black.copy(alpha = 0.86f),
                                topLeft = Offset(labelX, labelY),
                                size = Size(labelWidth, labelHeight)
                            )
                            drawIntoCanvas { canvas ->
                                val valuePaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    textSize = 18f
                                    textAlign = android.graphics.Paint.Align.LEFT
                                }
                                valuePaint.color = android.graphics.Color.WHITE
                                canvas.nativeCanvas.drawText(
                                    "t: ${formatSeconds(closest.timestampUs)}s",
                                    labelX + 8.dp.toPx(),
                                    labelY + 17.dp.toPx(),
                                    valuePaint
                                )
                                if (showThreeLines) {
                                    val xLabel = graphSeriesLabel(selectedSeriesX, displayedUnit).split(" ")[0]
                                    val xValStr = formatCalibratedValue(closest.xValue.toDouble())
                                    canvas.nativeCanvas.drawText(
                                        "$xLabel: $xValStr",
                                        labelX + 8.dp.toPx(),
                                        labelY + 34.dp.toPx(),
                                        valuePaint
                                    )
                                    val yLabel = graphSeriesLabel(selectedSeries, displayedUnit).split(" ")[0]
                                    val yValStr = formatCalibratedValue(closest.value.toDouble())
                                    canvas.nativeCanvas.drawText(
                                        "$yLabel: $yValStr",
                                        labelX + 8.dp.toPx(),
                                        labelY + 51.dp.toPx(),
                                        valuePaint
                                    )
                                } else {
                                    val yLabel = graphSeriesLabel(selectedSeries, displayedUnit).split(" ")[0]
                                    val yValStr = formatCalibratedValue(closest.value.toDouble())
                                    canvas.nativeCanvas.drawText(
                                        "$yLabel: $yValStr",
                                        labelX + 8.dp.toPx(),
                                        labelY + 34.dp.toPx(),
                                        valuePaint
                                    )
                                }
                            }
                        }
                    }
                }

                closestApproachGraphPoint?.let { point ->
                    if (isPrimary && point.xValue in visibleStartTime..visibleEndTime && point.value in visibleMinValue..visibleMaxValue) {
                        val markerX = xToScreen(point.xValue)
                        val markerY = yToScreen(point.value)
                        val markerColor = Color(0xFFFFF176)
                        drawCircle(
                            color = markerColor.copy(alpha = 0.18f),
                            radius = 12f,
                            center = Offset(markerX, markerY),
                            style = Fill
                        )
                        drawCircle(
                            color = markerColor,
                            radius = 6f,
                            center = Offset(markerX, markerY)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 10f,
                            center = Offset(markerX, markerY),
                            style = Stroke(width = 1.5f)
                        )
                        with(density) {
                            val markerLabelWidth = 118.dp.toPx()
                            val markerLabelHeight = 24.dp.toPx()
                            val markerLabelX = (markerX + 8.dp.toPx()).coerceIn(plotLeft, plotRight - markerLabelWidth)
                            val markerLabelY = (markerY - markerLabelHeight - 10.dp.toPx()).coerceIn(plotTop, plotBottom - markerLabelHeight)
                            drawRect(
                                color = Color.Black.copy(alpha = 0.82f),
                                topLeft = Offset(markerLabelX, markerLabelY),
                                size = Size(markerLabelWidth, markerLabelHeight)
                            )
                            drawIntoCanvas { canvas ->
                                val markerPaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    textSize = 17f
                                    textAlign = android.graphics.Paint.Align.LEFT
                                }
                                markerPaint.color = markerColor.toArgb()
                                canvas.nativeCanvas.drawText(
                                    "closest approach",
                                    markerLabelX + 7.dp.toPx(),
                                    markerLabelY + 16.dp.toPx(),
                                    markerPaint
                                )
                            }
                        }
                    }
                }

                // Fitted curve & mathematical envelopes/equilibrium lines (only for primary series)
                if (profileResult != null) {
                    val profile = profileResult
                    val profilePath = Path()
                    var startedProfilePath = false
                    val sampleSteps = 120
                    repeat(sampleSteps + 1) { step ->
                        val fraction = step / sampleSteps.toFloat()
                        val time = visibleStartTime + (visibleTimeRange * fraction)
                        val predicted = evaluateProfile(profile, time.toDouble()).toFloat()
                        val x = xToScreen(time)
                        val y = yToScreen(predicted)
                        if (!startedProfilePath) {
                            profilePath.moveTo(x, y)
                            startedProfilePath = true
                        } else {
                            profilePath.lineTo(x, y)
                        }
                    }
                    if (startedProfilePath) {
                        drawPath(
                            path = profilePath,
                            color = Color(0xFFFF8A65),
                            style = Stroke(
                                width = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                            )
                        )
                    }

                    // Visual annotations (equilibrium, envelopes, peak diamonds)
                    val pAnnot = profile.parameters.toMap()
                    val annotOffset = pAnnot["offset"] ?: 0.0
                    val annotAmp   = pAnnot["amplitude"] ?: 0.0
                    val annotLambda = pAnnot["lambda"] ?: pAnnot["damping_beta"]
                    val annotTShift = pAnnot["tShift"] ?: 0.0
                    val isDampedAnnot = profile.mode in listOf(
                        GraphProfileMode.DAMPED_SINE,
                        GraphProfileMode.DAMPED_COSINE,
                        GraphProfileMode.DAMPED_HARMONIC
                    )
                    val isSineAnnot = profile.mode in listOf(
                        GraphProfileMode.SINE,
                        GraphProfileMode.DAMPED_SINE,
                        GraphProfileMode.DAMPED_COSINE,
                        GraphProfileMode.DAMPED_HARMONIC
                    )
                    if (isSineAnnot && annotOffset in visibleMinValue..visibleMaxValue) {
                        val eqY = yToScreen(annotOffset.toFloat())
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha = 0.55f),
                            start = Offset(leftPad, eqY),
                            end = Offset(leftPad + chartWidth, eqY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )
                    }
                    if (isDampedAnnot && annotLambda != null && annotLambda > 0.0 && annotAmp != 0.0) {
                        val envSteps = 80
                        val upperPath = Path()
                        val lowerPath = Path()
                        var startedEnv = false
                        repeat(envSteps + 1) { step ->
                            val frac = step / envSteps.toFloat()
                            val t = visibleStartTime + visibleTimeRange * frac
                            val decay = kotlin.math.exp(-annotLambda * (t - annotTShift)).toFloat()
                            val upper = (annotOffset + annotAmp * decay).toFloat()
                            val lower = (annotOffset - annotAmp * decay).toFloat()
                            val x = xToScreen(t)
                            val uy = yToScreen(upper)
                            val ly = yToScreen(lower)
                            if (!startedEnv) {
                                upperPath.moveTo(x, uy)
                                lowerPath.moveTo(x, ly)
                                startedEnv = true
                            } else {
                                upperPath.lineTo(x, uy)
                                lowerPath.lineTo(x, ly)
                            }
                        }
                        val envelopeStroke = Stroke(
                            width = 1.8f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f))
                        )
                        drawPath(upperPath, Color(0xFF81C784).copy(alpha = 0.75f), style = envelopeStroke)
                        drawPath(lowerPath, Color(0xFF81C784).copy(alpha = 0.75f), style = envelopeStroke)
                    }
                    if (isSineAnnot) {
                        val annotOmega = pAnnot["omega"] ?: pAnnot["omega_d"] ?: pAnnot["omega_1"]
                        if (annotOmega != null && annotOmega > 0.0) {
                            val period = (2.0 * kotlin.math.PI / annotOmega).toFloat()
                            var t = visibleStartTime
                            while (t <= visibleEndTime) {
                                val predicted = evaluateProfile(profile, t.toDouble()).toFloat()
                                val predictedHalf = evaluateProfile(profile, (t + period / 2.0).toDouble()).toFloat()
                                val mx = xToScreen(t)
                                val my = yToScreen(predicted)
                                val halfX = xToScreen((t + period / 2.0).toFloat())
                                val halfY = yToScreen(predictedHalf)
                                if (predicted in visibleMinValue..visibleMaxValue) {
                                    drawPath(
                                        Path().apply {
                                            moveTo(mx, my - 7f)
                                            lineTo(mx + 5f, my)
                                            lineTo(mx, my + 7f)
                                            lineTo(mx - 5f, my)
                                            close()
                                        },
                                        color = Color(0xFFCE93D8).copy(alpha = 0.85f)
                                    )
                                }
                                if (predictedHalf in visibleMinValue..visibleMaxValue && (t + period / 2.0) <= visibleEndTime) {
                                    drawPath(
                                        Path().apply {
                                            moveTo(halfX, halfY - 7f)
                                            lineTo(halfX + 5f, halfY)
                                            lineTo(halfX, halfY + 7f)
                                            lineTo(halfX - 5f, halfY)
                                            close()
                                        },
                                        color = Color(0xFFCE93D8).copy(alpha = 0.55f)
                                    )
                                }
                                t += period
                            }
                        }
                    }

                    if (analysisMode && selectedSample != null && selectedSamplePrediction != null) {
                        val actualX = xToScreen(selectedSample.xValue)
                        val predictedYValue = selectedSamplePrediction.toFloat()
                        val predictedY = yToScreen(predictedYValue)
                        drawCircle(
                            color = Color(0xFFFF8A65),
                            radius = 4.5f,
                            center = Offset(actualX, predictedY)
                        )
                        with(density) {
                            val labelWidth = 112.dp.toPx()
                            val labelHeight = 26.dp.toPx()
                            val labelX = (actualX + 10.dp.toPx()).coerceIn(plotLeft, plotRight - labelWidth)
                            val labelY = (predictedY - labelHeight - 8.dp.toPx()).coerceIn(plotTop, plotBottom - labelHeight)
                            drawRect(
                                color = Color.Black.copy(alpha = 0.82f),
                                topLeft = Offset(labelX, labelY),
                                size = Size(labelWidth, labelHeight)
                            )
                            drawIntoCanvas { canvas ->
                                val residualPaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    textSize = 18f
                                    textAlign = android.graphics.Paint.Align.LEFT
                                }
                                residualPaint.color = android.graphics.Color.WHITE
                                canvas.nativeCanvas.drawText(
                                    "res ${selectedSampleResidual?.let(::formatCalibratedValue) ?: "--"}",
                                    labelX + 8.dp.toPx(),
                                    labelY + 17.dp.toPx(),
                                    residualPaint
                                )
                            }
                        }
                    }
                }

                // Box zoom selection marquee
                val startO = boxZoomStartOffset
                val endO = boxZoomCurrentOffset
                if (boxZoomModeEnabled && startO != null && endO != null) {
                    val rectLeft = minOf(startO.x, endO.x).coerceIn(plotLeft, plotRight)
                    val rectTop = minOf(startO.y, endO.y).coerceIn(plotTop, plotBottom)
                    val rectRight = maxOf(startO.x, endO.x).coerceIn(plotLeft, plotRight)
                    val rectBottom = maxOf(startO.y, endO.y).coerceIn(plotTop, plotBottom)

                    drawRect(
                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        topLeft = Offset(rectLeft, rectTop),
                        size = Size(rectRight - rectLeft, rectBottom - rectTop)
                    )
                    drawRect(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(rectLeft, rectTop),
                        size = Size(rectRight - rectLeft, rectBottom - rectTop),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                        )
                    )
                }
            }

            drawIntoCanvas { canvas ->
                val labelPaint = android.graphics.Paint().apply {
                    textSize = 24f
                    isAntiAlias = true
                }
                labelPaint.color = android.graphics.Color.argb(220, 255, 255, 255)
                
                // Y-axis ticks
                yMajorTicks.forEach { yValue ->
                    val y = yToScreen(yValue)
                    canvas.nativeCanvas.drawText(
                        formatAxisValue(yValue, axisDecimalPrecision, useScientificNotation),
                        6f,
                        y + 6f,
                        labelPaint
                    )
                    
                    // X-axis ticks (only on bottom subplot to avoid clutter)
                }
                if (isBottomSubplot) {
                    xMajorTicks.forEach { xValue ->
                        val x = xToScreen(xValue)
                        canvas.nativeCanvas.drawText(
                            formatAxisValue(xValue, axisDecimalPrecision, useScientificNotation),
                            (x - 22f).coerceIn(leftPad, plotRight - 56f),
                            topPad + chartHeight + 20f,
                            labelPaint
                        )
                    }
                }
                if (dataMinY <= 0f && dataMaxY >= 0f) {
                    val zeroY = yToScreen(0f)
                    canvas.nativeCanvas.drawText(
                        "0",
                        18f,
                        zeroY - 6f,
                        labelPaint
                    )
                }
                labelPaint.color = android.graphics.Color.argb(220, 255, 255, 255)
                if (analysisMode) {
                    listOf(
                        Triple("A", photogateAFraction, gateAColor),
                        Triple("B", photogateBFraction, gateBColor)
                    ).forEach { (label, fraction, color) ->
                        val x = leftPad + chartWidth * fraction.coerceIn(0.05f, 0.95f)
                        labelPaint.color = color.toArgb()
                        canvas.nativeCanvas.drawText(label, x - 7f, topPad + 22f, labelPaint)
                    }
                    labelPaint.color = android.graphics.Color.argb(220, 255, 255, 255)
                }
                val axisPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                axisPaint.color = android.graphics.Color.argb(235, 255, 255, 255)
                if (isBottomSubplot) {
                    canvas.nativeCanvas.drawText(xAxisLabel, leftPad + chartWidth / 2f, size.height - 4f, axisPaint)
                }
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.rotate(-90f, 15f, topPad + chartHeight / 2f)
                canvas.nativeCanvas.drawText(yAxisLabel, 15f, topPad + chartHeight / 2f, axisPaint)
                canvas.nativeCanvas.restore()
            }
        }
        
        // Subplot Title label overlay
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 64.dp, top = 16.dp)
        ) {
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        if (activeAnalysisLabels.isNotEmpty()) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 76.dp, bottom = 26.dp)
            ) {
                Text(
                    text = activeAnalysisLabels.joinToString("  |  "),
                    color = Color(0xFFFFF176),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }
        }

        if (isPrimary && profileResult != null && showScaleBars) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Fit: ${profileResult.label}",
                    color = Color(0xFFFF8A65),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
