package com.physicstracker.app.live

import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.video.Video
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * KLT (pyramidal Lucas–Kanade) feature tracker — the field-proven classical approach for
 * following an arbitrary textured object on a moving camera:
 *
 *  - Seed: goodFeaturesToTrack (Shi–Tomasi corners) inside a disc around the user tap.
 *  - Per frame: pyramidal LK prev→curr with forward-backward consistency filtering.
 *  - Object center = Kalman-smoothed median of inlier points (median = robust to
 *    occlusion of part of the object / outlier points on nearby objects).
 *  - Angular momentum of lock: points are redetected when the inlier pool shrinks.
 *  - Anti-switch: inlier ratio must stay high; forward-backward check removes points that
 *    wandered onto other objects before they can dominate the center estimate.
 *
 * This is markedly more forgiving than single-template matching for low-texture and
 * low-saturation targets, and stays real-time at 480px analysis resolution.
 */
class OpenCvLiveTracker : LiveTracker {

    companion object {
        private const val MIN_CORNERS_SEED = 6
        private const val REFILL_TRIGGER = 12
        private const val TARGET_CORNERS = 32
        private const val SEED_RADIUS_PX = 60.0
        private const val MIN_INLIERS = 5
        private const val FB_TOLERANCE_PX = 2.0
        private const val MAX_FLOW_ERR = 60.0

        @Volatile private var openCvReady: Boolean? = null

        fun ensureLoaded(): Boolean {
            openCvReady?.let { return it }
            val ok = runCatching { OpenCVLoader.initDebug() }.getOrDefault(false) ||
                runCatching { System.loadLibrary("opencv_java4"); true }.getOrDefault(false)
            openCvReady = ok
            return ok
        }
    }

    override val algorithmId: String = "opencv-klt-features"
    override val isSeeded: Boolean get() = prevPts != null

    override var estimatedRadiusPx: Float = 20f
        private set
    override var predictedCenter: Pair<Double, Double>? = null
        private set
    override var consecutiveMisses: Int = 0
        private set

    private var prevPts: MatOfPoint2f? = null
    private var prevGray: Mat? = null
    private var curGray: Mat? = null
    private var scratchRgba: Mat? = null
    private var frameW = 0
    private var frameH = 0
    private var lastTimestampUs: Long = 0L
    private var seedCenter: Point? = null

    init {
        if (!ensureLoaded()) error("OpenCV native library unavailable")
    }

    override fun seed(frame: Bitmap, x: Float, y: Float): Boolean {
        val gray = grayOf(frame) ?: return false
        frameW = frame.width
        frameH = frame.height
        val tap = Point(x.toDouble(), y.toDouble())
        seedCenter = tap
        val points = detectFeatures(gray, tap, SEED_RADIUS_PX, TARGET_CORNERS)
        if (points.toArray().size < MIN_CORNERS_SEED) {
            gray.release()
            return false
        }
        prevPts?.release()
        prevPts = points
        prevGray?.release()
        prevGray = gray
        kalmanFilterReset(tap.x, tap.y)
        estimatedRadiusPx = computeRadius(points, tap)
        consecutiveMisses = 0
        lastTimestampUs = 0L
        return true
    }

    override fun track(frame: Bitmap, timestampUs: Long): LiveTrackResult? {
        val previousPts = prevPts ?: return null
        val previousGray = prevGray ?: return null
        if (frame.width != frameW || frame.height != frameH) return null
        val gray = grayOf(frame) ?: return null
        val dt = if (lastTimestampUs == 0L) 0.0 else (timestampUs - lastTimestampUs) / 1_000_000.0

        val nextPts = MatOfPoint2f()
        val status = MatOfByte()
        val err = MatOfFloat()
        Video.calcOpticalFlowPyrLK(
            previousGray, gray, previousPts, nextPts, status, err,
            Size(23.0, 23.0), 4
        )
        // forward-backward consistency
        val backPts = MatOfPoint2f()
        val backStatus = MatOfByte()
        Video.calcOpticalFlowPyrLK(
            gray, previousGray, nextPts, backPts, backStatus, MatOfFloat(),
            Size(23.0, 23.0), 4
        )

        val p0 = previousPts.toArray()
        val p1 = nextPts.toArray()
        val pb = backPts.toArray()
        val st = status.toArray()
        val bst = backStatus.toArray()
        val er = err.toArray()

        val inliers = ArrayList<Point>(p0.size)
        var errSum = 0.0
        for (i in p0.indices) {
            if (st[i].toInt() != 1 || bst[i].toInt() != 1) continue
            if (er[i] > MAX_FLOW_ERR) continue
            if (abs(pb[i].x - p0[i].x) > FB_TOLERANCE_PX || abs(pb[i].y - p0[i].y) > FB_TOLERANCE_PX) continue
            if (p1[i].x < 2 || p1[i].y < 2 || p1[i].x > frameW - 3 || p1[i].y > frameH - 3) continue
            inliers.add(p1[i])
            errSum += er[i]
        }
        status.release(); err.release(); backStatus.release(); backPts.release(); nextPts.release()

        if (inliers.size < MIN_INLIERS) {
            consecutiveMisses += 1
            // Prediction-driven re-detection: look for the object's features near its
            // predicted position rather than freezing a stale anchor frame.
            if (consecutiveMisses >= 3) {
                kalmanFilterPredict(if (dt in 1e-4..1.0) dt else 1.0 / 30.0)
                val pred = Point(kalmanX(), kalmanY())
                if (pred.x >= 8 && pred.y >= 8 && pred.x <= frameW - 9 && pred.y <= frameH - 9) {
                    val fresh = detectFeatures(gray, pred, featureRadius() * 1.4, TARGET_CORNERS)
                    val freshPts = fresh.toArray()
                    if (freshPts.size >= MIN_INLIERS) {
                        val cx = freshPts.map { it.x }.average()
                        val cy = freshPts.map { it.y }.average()
                        val spread = freshPts.map { hypot2(it, Point(cx, cy)) }.average()
                        // Coherent cluster near prediction -> re-seed on it.
                        if (kotlin.math.hypot(cx - pred.x, cy - pred.y) <= featureRadius() * 1.4 &&
                            spread <= (featureRadius() * 1.4).let { it * it }
                        ) {
                            kalmanFilterReset(cx, cy)
                            kalmanFilterUpdate(cx, cy)
                            prevPts?.release()
                            prevPts = MatOfPoint2f(*freshPts)
                            previousGrayRelease(); prevGray = gray
                            lastTimestampUs = timestampUs
                            predictedCenter = cx to cy
                            consecutiveMisses = 0
                            estimatedRadiusPx = computeRadius(prevPts!!, Point(cx, cy))
                            android.util.Log.i("LiveTracking", "KLT auto-reacquired at ($cx, $cy)")
                            return LiveTrackResult(
                                xPx = cx, yPx = cy, confidence = 0.45,
                                matchScore = null, estimatedRadiusPx = estimatedRadiusPx
                            )
                        }
                    }
                    fresh.release()
                }
                gray.release()
            } else {
                gray.release()
            }
            return null
        }

        val inlierRatio = inliers.size.toDouble() / p0.size.coerceAtLeast(1)
        val meanErr = errSum / inliers.size
        val medianX = median(inliers.map { it.x })
        val medianY = median(inliers.map { it.y })

        val dtSafe = if (dt in 1e-4..1.0) dt else 1.0 / 30.0
        kalmanFilterPredict(dtSafe)
        kalmanFilterUpdate(medianX, medianY)
        predictedCenter = kalmanX() to kalmanY()

        // Rebuild the tracked point cloud around the inliers; refill when sparse.
        val keep = if (inliers.size < REFILL_TRIGGER) {
            val fresh = detectFeatures(gray, Point(medianX, medianY), featureRadius(), TARGET_CORNERS)
            val merged = ArrayList<Point>(inliers)
            for (p in fresh.toArray()) {
                if (inliers.all { hypot2(it, p) > 36.0 }) merged.add(p)
                if (merged.size >= TARGET_CORNERS) break
            }
            fresh.release()
            merged
        } else inliers
        prevPts?.release()
        prevPts = MatOfPoint2f(*keep.take(TARGET_CORNERS).toTypedArray())
        prevGray?.release()
        prevGray = gray
        lastTimestampUs = timestampUs
        consecutiveMisses = 0

        estimatedRadiusPx = computeRadius(prevPts!!, Point(medianX, medianY))
        val confidence = (inlierRatio * 0.6 + (1.0 - (meanErr / MAX_FLOW_ERR).coerceIn(0.0, 1.0)) * 0.4)
            .coerceIn(0.0, 1.0)
        return LiveTrackResult(
            xPx = kalmanX(),
            yPx = kalmanY(),
            confidence = (0.35 + confidence * 0.65).coerceAtMost(1.0),
            matchScore = meanErr,
            estimatedRadiusPx = estimatedRadiusPx
        )
    }

    override fun noteExternalMiss() { consecutiveMisses += 1 }

    /** Rotation remap: KLT restarts from the remapped center on the next frame. */
    override fun remapAfterRotation(oldRotationDeg: Int, newRotationDeg: Int) {
        val delta = ((newRotationDeg - oldRotationDeg) % 360 + 360) % 360
        if (delta == 0 || prevPts == null) return
        val oldW = frameW.toDouble()
        val oldH = frameH.toDouble()
        val (nx, ny) = kalmanXY().let { (kx, ky) ->
            val nxN = kx / oldW
            val nyN = ky / oldH
            val (rx, ry) = when (delta) {
                90 -> nyN to (1.0 - nxN)
                270 -> (1.0 - nyN) to nxN
                180 -> (1.0 - nxN) to (1.0 - nyN)
                else -> nxN to nyN
            }
            rx to ry
        }
        frameW = frameH.also { frameH = frameW }
        kalmanFilterReset(nx * frameW, ny * frameH)
        prevPts = null // force patient re-seed at remapped center via re-detection
        prevGray?.release(); prevGray = null
        predictedCenter = nx * frameW to ny * frameH
        consecutiveMisses = 1 // let the pipeline show REACQUIRING while we rebuild
    }

    /** Auto re-seed at the predicted center (used after rotation / short losses). */
    fun autoReseed(frame: Bitmap): Boolean {
        val pred = predictedCenter ?: seedCenter?.let { it.x to it.y } ?: return false
        return seed(frame, pred.first.toFloat(), pred.second.toFloat())
    }

    override fun release() {
        prevPts?.release(); prevPts = null
        prevGray?.release(); prevGray = null
        curGray?.release(); curGray = null
        scratchRgba?.release(); scratchRgba = null
        predictedCenter = null
        consecutiveMisses = 0
    }

    // ------------------------------------------------------------

    private fun grayOf(frame: Bitmap): Mat? {
        return runCatching {
            val rgba = scratchRgba?.takeIf { it.width() == frame.width && it.height() == frame.height }
                ?: Mat(frame.height, frame.width, CvType.CV_8UC4).also { scratchRgba?.release(); scratchRgba = it }
            Utils.bitmapToMat(frame, rgba)
            val gray = Mat(frame.height, frame.width, CvType.CV_8UC1)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            gray
        }.getOrNull()
    }

    private fun detectFeatures(gray: Mat, center: Point, radius: Double, maxCount: Int): MatOfPoint2f {
        val mask = Mat.zeros(gray.rows(), gray.cols(), CvType.CV_8UC1)
        Imgproc.circle(mask, center, radius.toInt(), Scalar(255.0), -1)
        val corners = MatOfPoint()
        Imgproc.goodFeaturesToTrack(gray, corners, maxCount, 0.01, 6.0, mask)
        mask.release()
        val pts = corners.toArray()
        corners.release()
        return MatOfPoint2f(*pts)
    }

    private fun computeRadius(points: MatOfPoint2f, center: Point): Float {
        val pts = points.toArray()
        if (pts.isEmpty()) return 20f
        val spread = pts.map { sqrt((it.x - center.x) * (it.x - center.x) + (it.y - center.y) * (it.y - center.y)) }
            .sorted()
        val p75 = spread[(spread.size * 0.75).toInt().coerceIn(0, spread.size - 1)]
        return (p75 * 1.6 + 12).toFloat().coerceIn(16f, 80f)
    }

    private fun featureRadius(): Double = (estimatedRadiusPx * 1.8 + 30).toDouble()

    private fun hypot2(a: Point, b: Point) = (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)

    private fun previousGrayRelease() { prevGray?.release(); prevGray = null }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
    }

    // Kalman smoothing of the measured center
    private var kf = KalmanFilter2D(0.0, 0.0)
    private var hasKf = false
    private fun kalmanFilterReset(x: Double, y: Double) { kf = KalmanFilter2D(x, y); hasKf = true }
    private fun kalmanFilterPredict(dt: Double) { if (hasKf) kf.predict(dt) }
    private fun kalmanFilterUpdate(x: Double, y: Double) { if (hasKf) kf.update(x, y) }
    private fun kalmanX() = if (hasKf) kf.x else 0.0
    private fun kalmanY() = if (hasKf) kf.y else 0.0
    private fun kalmanXY() = kalmanX() to kalmanY()
}
