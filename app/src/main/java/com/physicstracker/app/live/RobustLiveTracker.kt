package com.physicstracker.app.live

import android.graphics.Bitmap
import com.bmad.new_start.shared_c_core.NativeTemplateTracker
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Robust real-time tracker for camera live tracking.
 *
 * Fuses three measurement channels per frame:
 *  1. SSD template match (reuses the proven [NativeTemplateTracker] localized search)
 *  2. HSV color-signature blob centroid (hue histogram of the seed patch)
 *  3. Constant-velocity Kalman prediction with ACTUAL timestamps
 *
 * Reliability mechanisms:
 *  - adaptive search radius (speed + Kalman uncertainty + miss streak growth)
 *  - anti-switch gating: candidates far from prediction or with a foreign color
 *    signature are rejected instead of jumping to a similar-looking object
 *  - template refresh only on high-confidence measurements
 *  - exposure of consecutive misses so the owning pipeline can run
 *    TRACKING -> REACQUIRING -> OBJECT_LOST without inventing data
 *
 * Runs entirely on the dedicated live-pipeline thread at ~480px analysis resolution.
 */
class RobustLiveTracker : LiveTracker {

    companion object {
        const val TEMPLATE_SIZE = 32
        private const val HUE_BINS = 24
        private const val MIN_RADIUS = 6f
        private const val MAX_RADIUS = 48f
        private const val BASE_SEARCH_RADIUS = 20
        private const val MAX_SEARCH_RADIUS = 150
        private const val HUE_SIMILARITY_REJECT = 0.40
        private const val TEMPLATE_ERROR_MAX = 45.0
        private const val TEMPLATE_REFRESH_CONFIDENCE = 0.72
    }

    override val algorithmId: String = "robust-fused-ssd-color-kalman"
    override val isSeeded: Boolean get() = kalman != null

    private var kalman: KalmanFilter2D? = null
    private var template: IntArray? = null
    private var templateStaled = false
    private var hueSignature = FloatArray(HUE_BINS)
    private var estimatedRadius: Float = 14f
    private var sourceW = 0
    private var sourceH = 0
    private var lastTimestampUs: Long = 0L
    private var missCount = 0

    /** Latest estimated object radius in analysis pixels (for the overlay box). */
    override var estimatedRadiusPx: Float = 14f
        private set

    /** Prediction of the next measurement center (for UI/reacquisition cues). */
    override var predictedCenter: Pair<Double, Double>? = null
        private set

    override val consecutiveMisses: Int get() = missCount

    override fun seed(frame: Bitmap, x: Float, y: Float): Boolean {
        val cx = x.toInt().coerceIn(0, frame.width - 1)
        val cy = y.toInt().coerceIn(0, frame.height - 1)
        val pixels = framePixels(frame) ?: return false
        estimatedRadius = estimateRadius(pixels, frame.width, frame.height, cx, cy)
        estimatedRadiusPx = estimatedRadius
        template = cropTemplate(pixels, frame.width, frame.height, cx, cy) ?: return false
        hueSignature = hueHistogram(pixels, frame.width, frame.height, cx, cy, estimatedRadius.toInt() + 4)
        if (hueSignature.sum() <= 0f) {
            // Low-saturation object: hue signature unusable. Still trackable via template,
            // but color gating is disabled for this session.
            hueSignature.fill(0f)
        }
        kalman = KalmanFilter2D(cx.toDouble(), cy.toDouble())
        sourceW = frame.width
        sourceH = frame.height
        lastTimestampUs = 0L
        missCount = 0
        templateStaled = false
        return true
    }

    override fun track(frame: Bitmap, timestampUs: Long): LiveTrackResult? {
        val kf = kalman ?: return null
        if (frame.width != sourceW || frame.height != sourceH) return null
        val pixels = framePixels(frame) ?: return null
        val dt = if (lastTimestampUs == 0L) 0.0 else (timestampUs - lastTimestampUs) / 1_000_000.0
        kf.predict(dt)

        if (templateStaled) {
            cropTemplate(pixels, frame.width, frame.height, kf.x.toInt(), kf.y.toInt())?.let {
                template = it
                templateStaled = false
            }
        }

        val predictedX = kf.x
        val predictedY = kf.y
        predictedCenter = predictedX to predictedY
        val missBoost = 1.0 + 0.35 * missCount
        val searchRadius = (BASE_SEARCH_RADIUS +
            kf.speed * max(dt, 1.0 / 30.0) * 2.0 +
            kf.positionUncertainty * 1.5)
            .toInt()
            .coerceIn(BASE_SEARCH_RADIUS, (MAX_SEARCH_RADIUS * kotlin.math.min(missBoost, 3.0)).toInt())

        // ---- Channel 1: SSD template ----
        val templateMatch = template?.let { tpl ->
            val result = NativeTemplateTracker.localizedSsdMatchNative(
                framePixels = pixels,
                frameWidth = frame.width,
                frameHeight = frame.height,
                templatePixels = tpl,
                templateWidth = TEMPLATE_SIZE,
                templateHeight = TEMPLATE_SIZE,
                predictedCenterX = predictedX.toInt(),
                predictedCenterY = predictedY.toInt(),
                searchRadiusPx = searchRadius
            )
            if (result.size >= 3 && result[2].isFinite() && result[2] < 1e9f) {
                Triple(result[0].toDouble(), result[1].toDouble(), result[2].toDouble())
            } else null
        }
        val confTemplate = templateMatch?.let {
            (1.0 - (it.third / TEMPLATE_ERROR_MAX)).coerceIn(0.0, 1.0)
        }

        // ---- Channel 2: HSV color blob centroid ----
        val colorMatch = colorCentroid(pixels, frame.width, frame.height, predictedX, predictedY, searchRadius)

        // ---- Fusion ----
        val fused = fuse(templateMatch, confTemplate, colorMatch, predictedX, predictedY, dt)
            ?: run { missCount += 1; return null }
        val (mx, my, fusedConf) = fused

        // Anti-switch hue gate at the accepted center (only when we have a signature).
        if (hueSignature.sum() > 0f) {
            val sim = hueSimilarityAt(pixels, frame.width, frame.height, mx.toInt(), my.toInt())
            if (sim < HUE_SIMILARITY_REJECT && fusedConf < 0.85) {
                missCount += 1
                return null
            }
        }

        val clampedX = mx.coerceIn(0.0, frame.width - 1.0)
        val clampedY = my.coerceIn(0.0, frame.height - 1.0)
        kf.update(clampedX, clampedY)
        missCount = 0

        if (fusedConf >= TEMPLATE_REFRESH_CONFIDENCE && templateMatch != null) {
            cropTemplate(pixels, frame.width, frame.height, clampedX.toInt(), clampedY.toInt())
                ?.let { template = it }
        }

        // Slowly adapt the color signature to appearance drift.
        if (hueSignature.sum() > 0f) {
            val fresh = hueHistogram(pixels, frame.width, frame.height,
                clampedX.toInt(), clampedY.toInt(), estimatedRadius.toInt() + 4)
            if (fresh.sum() > 0f) {
                for (i in 0 until HUE_BINS) hueSignature[i] = hueSignature[i] * 0.92f + fresh[i] * 0.08f
            }
        }

        lastTimestampUs = timestampUs
        return LiveTrackResult(
            xPx = clampedX,
            yPx = clampedY,
            confidence = (fusedConf * (1.0 - (kf.lastInnovation / (searchRadius * 2 + 1)).coerceIn(0.0, 0.35))).coerceIn(0.0, 1.0),
            matchScore = templateMatch?.third,
            estimatedRadiusPx = estimatedRadius
        )
    }

    /** After a device rotation, remap the tracked state into the new upright frame. */
    override fun remapAfterRotation(oldRotationDeg: Int, newRotationDeg: Int) {
        val kf = kalman ?: return
        val delta = ((newRotationDeg - oldRotationDeg) % 360 + 360) % 360
        if (delta == 0) return
        val oldW = sourceW
        val oldH = sourceH
        remapUprightCoords(kf, oldW, oldH, oldH, oldW, oldRotationDeg, newRotationDeg)
        sourceW = oldH
        sourceH = oldW
        templateStaled = true
        missCount = 0
    }

    private fun remapUprightCoords(
        kf: KalmanFilter2D, oldW: Int, oldH: Int, newW: Int, newH: Int,
        oldRot: Int, newRot: Int
    ) {
        val oldWf = oldW.toDouble()
        val oldHf = oldH.toDouble()
        fun toNorm(x: Double, y: Double) = (x / oldWf) to (y / oldHf)
        fun fromNorm(nx: Double, ny: Double) = (nx * newW) to (ny * newH)
        val delta = ((newRot - oldRot) % 360 + 360) % 360
        kf.remap { x, y ->
            val (nxx, nyy) = toNorm(x, y)
            val (rx, ry) = when (delta) {
                90 -> nyy to (1.0 - nxx)
                270 -> (1.0 - nyy) to nxx
                180 -> (1.0 - nxx) to (1.0 - nyy)
                else -> nxx to nyy
            }
            fromNorm(rx, ry)
        }
        predictedCenter = kf.x to kf.y
    }

    override fun noteExternalMiss() { missCount += 1 }

    override fun release() {
        kalman = null
        template = null
        hueSignature.fill(0f)
        missCount = 0
        predictedCenter = null
    }

    // ----------------------------------------------------------------

    private fun framePixels(frame: Bitmap): IntArray? {
        return try {
            IntArray(frame.width * frame.height).also {
                frame.getPixels(it, 0, frame.width, 0, 0, frame.width, frame.height)
            }
        } catch (_: Exception) { null }
    }

    private fun cropTemplate(pixels: IntArray, w: Int, h: Int, cx: Int, cy: Int): IntArray? {
        val half = TEMPLATE_SIZE / 2
        if (cx - half < 0 || cy - half < 0 || cx + half > w || cy + half > h) return null
        val tpl = IntArray(TEMPLATE_SIZE * TEMPLATE_SIZE)
        for (ty in 0 until TEMPLATE_SIZE) {
            System.arraycopy(pixels, (cy - half + ty) * w + (cx - half), tpl, ty * TEMPLATE_SIZE, TEMPLATE_SIZE)
        }
        return tpl
    }

    /** Simple luma-contrast walk to approximate the object size around the tap. */
    private fun estimateRadius(pixels: IntArray, w: Int, h: Int, cx: Int, cy: Int): Float {
        val centerLuma = luma(pixels[cy * w + cx])
        var maxR = MIN_RADIUS
        val probes = doubleArrayOf(0.0, PI / 2, PI, 3 * PI / 2)
        for (angle in probes) {
            val dx = cos(angle)
            val dy = sin(angle)
            var r = 2
            var streak = 0
            while (r < MAX_RADIUS) {
                val px = (cx + dx * r).toInt()
                val py = (cy + dy * r).toInt()
                if (px < 0 || py < 0 || px >= w || py >= h) break
                if (abs(luma(pixels[py * w + px]) - centerLuma) > 32) {
                    streak += 1
                    if (streak >= 3) break
                } else {
                    streak = 0
                }
                r += 1
            }
            maxR = max(maxR, r.toFloat() - 3)
        }
        return maxR.coerceIn(MIN_RADIUS, MAX_RADIUS)
    }

    private fun luma(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (r * 54 + g * 183 + b * 19) shr 8
    }

    private fun hueSatVal(argb: Int): Triple<Double, Double, Double> {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        val mx = maxOf(r, g, b)
        val mn = minOf(r, g, b)
        val d = mx - mn
        val h = when {
            d < 1e-9 -> 0.0
            mx == r -> (60 * ((g - b) / d) + 360) % 360
            mx == g -> (60 * ((b - r) / d) + 120) % 360
            else -> (60 * ((r - g) / d) + 240) % 360
        }
        val s = if (mx <= 1e-9) 0.0 else d / mx
        return Triple(h, s, mx)
    }

    private fun hueHistogram(pixels: IntArray, w: Int, h: Int, cx: Int, cy: Int, radius: Int): FloatArray {
        val hist = FloatArray(HUE_BINS)
        val r = radius.coerceIn(4, 32)
        for (dy in -r..r) {
            val py = cy + dy
            if (py < 0 || py >= h) continue
            for (dx in -r..r) {
                val px = cx + dx
                if (px < 0 || px >= w) continue
                if (dx * dx + dy * dy > r * r) continue
                val (hue, sat, v) = hueSatVal(pixels[py * w + px])
                if (sat < 0.25 || v < 0.08) continue
                hist[(hue / 360.0 * HUE_BINS).toInt().coerceIn(0, HUE_BINS - 1)] += (sat * v).toFloat()
            }
        }
        val total = hist.sum()
        if (total > 0f) for (i in hist.indices) hist[i] /= total
        return hist
    }

    private fun histSimilarity(a: FloatArray, b: FloatArray): Double {
        var sa = 0.0; var sb = 0.0; var dot = 0.0
        for (i in a.indices) {
            sa += a[i] * a[i]
            sb += b[i] * b[i]
            dot += a[i] * b[i]
        }
        if (sa <= 0.0 || sb <= 0.0) return 0.0
        return dot / sqrt(sa * sb)
    }

    private fun hueSimilarityAt(pixels: IntArray, w: Int, h: Int, cx: Int, cy: Int): Double {
        val local = hueHistogram(pixels, w, h, cx, cy, min(estimatedRadius.toInt() + 2, 16))
        return histSimilarity(hueSignature, local)
    }

    private fun colorCentroid(
        pixels: IntArray, w: Int, h: Int,
        predictedX: Double, predictedY: Double, radius: Int
    ): Triple<Double, Double, Double>? {
        if (hueSignature.sum() <= 0f) return null
        // Dominant hue bin of the signature
        var dominant = 0
        for (i in hueSignature.indices) if (hueSignature[i] > hueSignature[dominant]) dominant = i
        val minX = max(0, (predictedX - radius).toInt())
        val maxX = min(w - 1, (predictedX + radius).toInt())
        val minY = max(0, (predictedY - radius).toInt())
        val maxY = min(h - 1, (predictedY + radius).toInt())
        if (minX >= maxX || minY >= maxY) return null

        var sumW = 0.0
        var sumX = 0.0
        var sumY = 0.0
        var hits = 0
        for (py in minY..maxY step 2) {
            for (px in minX..maxX step 2) {
                val (hue, sat, v) = hueSatVal(pixels[py * w + px])
                if (sat < 0.3 || v < 0.08) continue
                val bin = (hue / 360.0 * HUE_BINS).toInt().coerceIn(0, HUE_BINS - 1)
                val binDist = min(abs(bin - dominant), HUE_BINS - abs(bin - dominant))
                if (binDist <= 2) {
                    val weight = sat * v
                    sumW += weight
                    sumX += px * weight
                    sumY += py * weight
                    hits += 1
                }
            }
        }
        if (hits < 4 || sumW <= 0.0) return null
        val objectArea = PI * estimatedRadius * estimatedRadius / 4.0 // step-2 sampled area
        val matchFrac = (hits / objectArea).coerceIn(0.0, 1.5)
        val localHist = hueHistogram(pixels, w, h, (sumX / sumW).toInt(), (sumY / sumW).toInt(), estimatedRadius.toInt() + 2)
        val conf = (matchFrac.coerceAtMost(1.0) * 0.6 + histSimilarity(hueSignature, localHist) * 0.4)
        return Triple(sumX / sumW, sumY / sumW, conf)
    }

    private fun fuse(
        templateMatch: Triple<Double, Double, Double>?, confTemplate: Double?,
        colorMatch: Triple<Double, Double, Double>?,
        predictedX: Double, predictedY: Double, dt: Double
    ): Triple<Double, Double, Double>? {
        if (templateMatch == null && colorMatch == null) return null
        val agreeRadius = max(estimatedRadius * 1.4f + 10f, 22f)
        val cT = confTemplate ?: 0.0
        val cC = colorMatch?.third ?: 0.0

        var centerX: Double
        var centerY: Double
        var conf: Double
        if (templateMatch != null && colorMatch != null) {
            val dist = hypot(templateMatch.first - colorMatch.first, templateMatch.second - colorMatch.second)
            if (dist <= agreeRadius) {
                // Agreement: weighted fusion, confidence bonus.
                val wT = cT + 0.15
                val wC = cC + 0.15
                centerX = (templateMatch.first * wT + colorMatch.first * wC) / (wT + wC)
                centerY = (templateMatch.second * wT + colorMatch.second * wC) / (wT + wC)
                conf = min(1.0, cT * 0.6 + cC * 0.4 + 0.15)
            } else {
                // Disagreement: pick the stronger channel, apply ambiguity penalty.
                if (cT >= cC) {
                    centerX = templateMatch.first; centerY = templateMatch.second
                    conf = cT * 0.78
                } else {
                    centerX = colorMatch.first; centerY = colorMatch.second
                    conf = cC * 0.78
                }
            }
        } else if (templateMatch != null) {
            centerX = templateMatch.first; centerY = templateMatch.second
            conf = cT * 0.9
        } else {
            centerX = colorMatch!!.first; centerY = colorMatch.second
            conf = cC * 0.9
        }

        // Jump gate: reject implausible leaps unless the measurement is very strong.
        val jump = hypot(centerX - predictedX, centerY - predictedY)
        val gate = kotlin.math.max(
            (estimatedRadius * 2.5f + 18f).toDouble(),
            (kalman?.positionUncertainty ?: 10.0) * 3.5 + (kalman?.speed ?: 0.0) * max(dt, 1.0 / 30.0) * 3.0
        )
        if (jump > gate && conf < 0.8) return null
        return Triple(centerX, centerY, conf)
    }

}
