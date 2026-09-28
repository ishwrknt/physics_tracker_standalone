package com.physicstracker.app.ui.light

import androidx.compose.ui.geometry.Offset
import com.physicstracker.app.ui.LightChannel
import com.physicstracker.app.ui.LightGeometry
import com.physicstracker.app.ui.LightTool
import kotlin.math.*

/** Coordinates name pixel centres in the orientation-normalized, full-resolution source. */
interface LightPixels {
    val width: Int
    val height: Int
    fun argb(x: Int, y: Int): Int
}

data class LightObservation(val axis: Double, val value: Double, val timestampUs: Long? = null,
                            val gapBefore: Boolean = false, val rawValue: Double? = null)

/** Quantize exactly once at the selection boundary. Marker, labels and sampling share this. */
fun canonicalLightGeometry(geometry: LightGeometry, width: Int, height: Int): LightGeometry {
    require(width > 0 && height > 0)
    fun pixel(point: Offset): Offset {
        require(point.x.isFinite() && point.y.isFinite())
        return Offset(point.x.roundToInt().coerceIn(0, width - 1).toFloat(),
            point.y.roundToInt().coerceIn(0, height - 1).toFloat())
    }
    val start = pixel(geometry.start)
    return geometry.copy(start = start, end = if (geometry.tool == LightTool.POINT) start else pixel(geometry.end))
}
data class LightDataset(
    val mediaName: String,
    val mediaUri: String,
    val width: Int,
    val height: Int,
    val slot: String,
    val channel: LightChannel,
    val geometry: LightGeometry,
    val timeSeries: Boolean,
    val observations: List<LightObservation>,
    val provenance: String,
    val complete: Boolean = true,
    val processing: String? = null
) {
    val axisLabel: String get() = if (timeSeries) "Video time (s)" else when (geometry.tool) {
        LightTool.POINT -> "Selected point"
        LightTool.LINE -> "Distance along line (source px)"
        LightTool.BOX -> "Horizontal distance (source px)"
    }
    val intensityLabel: String get() = "${if (processing != null) "Derived " else ""}${channel.displayName()} (0–255; not lux)"
    val method: String get() = samplingMethod + (processing?.let { "; then $it" } ?: "")
    private val samplingMethod: String get() = when {
        timeSeries && geometry.tool == LightTool.BOX -> "Mean of all opaque pixels in inclusive area"
        timeSeries && geometry.tool == LightTool.LINE -> "Mean of sampled line pixels"
        geometry.tool == LightTool.BOX -> "Column means of all opaque pixels in inclusive area"
        geometry.tool == LightTool.LINE -> "Nearest source pixel at uniform line positions; no smoothing"
        else -> "One exact source pixel; no neighbourhood averaging"
    }
}

fun LightChannel.displayName(): String = when (this) {
    LightChannel.GRAY -> "Brightness (luma)"
    LightChannel.R -> "Red"
    LightChannel.G -> "Green"
    LightChannel.B -> "Blue"
}

fun lightChannelValue(pixel: Int, channel: LightChannel): Double? {
    if (pixel ushr 24 != 255) return null // No physical brightness claim for transparent pixels.
    val r = (pixel shr 16 and 255).toDouble()
    val g = (pixel shr 8 and 255).toDouble()
    val b = (pixel and 255).toDouble()
    return when (channel) {
        LightChannel.GRAY -> 0.299 * r + 0.587 * g + 0.114 * b
        LightChannel.R -> r
        LightChannel.G -> g
        LightChannel.B -> b
    }
}

fun measureLightProfile(pixels: LightPixels, geometry: LightGeometry, channel: LightChannel,
                        checkCancellation: () -> Unit = {}): List<LightObservation> {
    fun valid(point: Offset) = point.x.isFinite() && point.y.isFinite() &&
        point.x in 0f..(pixels.width - 1f) && point.y in 0f..(pixels.height - 1f)
    require(valid(geometry.start) && valid(geometry.end)) { "Selection is outside the source image." }
    fun value(x: Int, y: Int) = lightChannelValue(pixels.argb(x, y), channel)
    val a = geometry.start
    val b = geometry.end
    return when (geometry.tool) {
        LightTool.POINT -> listOfNotNull(value(a.x.roundToInt(), a.y.roundToInt())?.let { LightObservation(0.0, it) })
        LightTool.LINE -> {
            val length = (b - a).getDistance().toDouble()
            val count = ceil(length).toInt().coerceAtLeast(1)
            var gap = false
            (0..count).mapNotNull { index ->
                checkCancellation()
                val fraction = index.toDouble() / count
                val x = (a.x + (b.x - a.x) * fraction).roundToInt()
                val y = (a.y + (b.y - a.y) * fraction).roundToInt()
                val intensity = value(x, y)
                if (intensity == null) { gap = true; null }
                else LightObservation(length * fraction, intensity, gapBefore = gap).also { gap = false }
            }.distinctBy { it.axis }
        }
        LightTool.BOX -> {
            val left = min(a.x, b.x).roundToInt()
            val right = max(a.x, b.x).roundToInt()
            val top = min(a.y, b.y).roundToInt()
            val bottom = max(a.y, b.y).roundToInt()
            var gap = false
            (left..right).mapNotNull { x ->
                checkCancellation()
                var sum = 0.0
                var count = 0
                for (y in top..bottom) value(x, y)?.let { sum += it; count++ }
                if (count == 0) { gap = true; null }
                else LightObservation((x - left).toDouble(), sum / count, gapBefore = gap).also { gap = false }
            }
        }
    }
}

fun measureLightMean(pixels: LightPixels, geometry: LightGeometry, channel: LightChannel,
                     checkCancellation: () -> Unit = {}): Double? {
    if (geometry.tool != LightTool.BOX) return measureLightProfile(pixels, geometry, channel, checkCancellation)
        .map { it.value }.takeIf { it.isNotEmpty() }?.average()
    // Do not average column means: columns can have different opaque-pixel counts.
    val a = geometry.start
    val b = geometry.end
    require(listOf(a, b).all { it.x.isFinite() && it.y.isFinite() &&
        it.x in 0f..(pixels.width - 1f) && it.y in 0f..(pixels.height - 1f) })
    var sum = 0.0
    var count = 0L
    for (y in min(a.y, b.y).roundToInt()..max(a.y, b.y).roundToInt()) {
        checkCancellation()
        for (x in min(a.x, b.x).roundToInt()..max(a.x, b.x).roundToInt()) {
            lightChannelValue(pixels.argb(x, y), channel)?.let { sum += it; count++ }
        }
    }
    return if (count == 0L) null else sum / count
}

/** Exact timestamp identity; nearby but distinct frames must never be merged. */
fun canonicalLightTimes(samples: List<LightObservation>): List<LightObservation> = samples
    .filter { it.timestampUs != null && it.timestampUs >= 0 && it.value.isFinite() }
    .associateBy { it.timestampUs!! }.toSortedMap().values.toList()

data class LightImageMapping(val width: Int, val height: Int, val viewWidth: Float, val viewHeight: Float) {
    val scale = min(viewWidth / width.coerceAtLeast(1), viewHeight / height.coerceAtLeast(1))
    val left = (viewWidth - width * scale) / 2f
    val top = (viewHeight - height * scale) / 2f
    fun toSource(point: Offset): Offset? {
        if (scale <= 0 || !scale.isFinite()) return null
        val x = (point.x - left) / scale
        val y = (point.y - top) / scale
        if (x !in 0f..<width.toFloat() || y !in 0f..<height.toFloat()) return null
        return Offset(floor(x), floor(y))
    }
    fun toView(point: Offset) = Offset(left + (point.x + 0.5f) * scale, top + (point.y + 0.5f) * scale)
}

/** A single normalization function for graph drawing and point hit-testing, including constants. */
data class LightPlotBounds(val minX: Double, val maxX: Double, val minY: Double, val maxY: Double) {
    fun normalized(point: LightObservation) = Offset(((point.axis - minX) / (maxX - minX)).toFloat(),
        (1 - (point.value - minY) / (maxY - minY)).toFloat())
    companion object {
        fun of(samples: List<LightObservation>): LightPlotBounds {
            val finite = samples.filter { it.axis.isFinite() && it.value.isFinite() }
            val min = finite.minOfOrNull { it.axis } ?: 0.0
            val max = finite.maxOfOrNull { it.axis } ?: 1.0
            return LightPlotBounds(if (min == max) min - 0.5 else min, if (min == max) max + 0.5 else max, 0.0, 255.0)
        }
    }
}
