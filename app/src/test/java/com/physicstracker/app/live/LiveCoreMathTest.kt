package com.physicstracker.app.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class KalmanFilter2DTest {

    @Test
    fun `filter converges to constant velocity track`() {
        val kf = KalmanFilter2D(0.0, 0.0, processNoise = 5.0)
        // true motion: x = 5*px/s * t, noisy measurements at 30 Hz
        var seed = 7L
        fun rand(): Double {
            seed = (seed * 6364136223846793005L + 1442695040888963407L)
            return ((seed ushr 11) and 0xFFFF).toDouble() / 0xFFFF - 0.5
        }
        val dt = 1.0 / 30.0
        var lastX = 0.0
        for (i in 0 until 120) {
            kf.predict(dt)
            val noisy = 5.0 * i * dt + rand() * 6.0 // +-3 px noise
            kf.update(noisy, 10.0)
            lastX = kf.x
        }
        assertTrue(abs(kf.speed - 5.0) < 1.5)
        assertTrue(abs(kf.vx - 5.0) < 1.5)
    }

    @Test
    fun `prediction extrapolates during missed measurements`() {
        val kf = KalmanFilter2D(0.0, 0.0)
        val dt = 1.0 / 30.0
        for (i in 1..60) {
            kf.predict(dt)
            kf.update(i * dt * 10.0, 0.0) // 10 px/s
        }
        // three frames without measurement: prediction should still lead the motion
        // (last measurement was at 60 * dt * 10 = 20 px, leading at ~10 px/s)
        repeat(3) { kf.predict(dt) }
        assertTrue(kf.x > 20.0)
        assertTrue(kf.x < 30.0)
        assertTrue(kf.positionUncertainty > 0.0)
    }

    @Test
    fun `remap rotates state consistently`() {
        val kf = KalmanFilter2D(10.0, 20.0)
        val dt = 0.5
        kf.predict(dt)
        kf.update(14.0, 20.0) // ~8 px/s in +x
        kf.remap { x, y -> y to (100.0 - x) } // 90° CW of a 100-ish frame
        assertEquals(20.0, kf.x, 1e-9)
        // Kalman pulled partway toward the measurement (gain < 1 after one update)
        assertEquals(100.0 - 14.0, kf.y, 0.5)
        // velocity +x rotated to -y (new frame)
        assertTrue(kf.vy < 0.0)
    }
}

class LiveScaleEstimatorTest {

    @Test
    fun `pinhole scale from focus distance matches analytic FOV coverage`() {
        // f=4mm, sensor 5.6mm wide at 4000px, crop 2000px, analysis width 480px, D=1m
        val est = LiveScaleEstimator.estimate(
            focalLengthMm = 4.0,
            sensorWidthMm = 5.6,
            sensorFullWidthPx = 4000,
            cropWidthSensorPx = 2000,
            analysisWidthPx = 480,
            focusDistanceDiopters = 1.0,
            focusQuality = LiveScaleEstimator.FocusQuality.CALIBRATED
        )
        assertNotNull(est)
        // scene width at 1m = 2*tan(atan(2.8/4)) = 0.7m over 480 px
        val expected = 0.7 / 480.0
        assertEquals(expected, est!!.metersPerPixel, expected * 0.02)
        assertEquals(1.0, est.distanceMeters, 1e-12)
    }

    @Test
    fun `closer distance yields proportionally finer scale`() {
        val near = LiveScaleEstimator.estimate(4.0, 5.6, 4000, 2000, 480, 2.0) // 0.5 m
        val far = LiveScaleEstimator.estimate(4.0, 5.6, 4000, 2000, 480, 1.0) // 1.0 m
        assertNotNull(near); assertNotNull(far)
        assertEquals(far!!.metersPerPixel / 2.0, near!!.metersPerPixel, far.metersPerPixel * 0.01)
    }

    @Test
    fun `missing or degenerate focus distance is rejected, not guessed`() {
        assertNull(LiveScaleEstimator.estimate(4.0, 5.6, 4000, 2000, 480, null))
        assertNull(LiveScaleEstimator.estimate(4.0, 5.6, 4000, 2000, 480, 0.0))
        assertNull(LiveScaleEstimator.estimate(0.0, 5.6, 4000, 2000, 480, 1.0))
        assertNull(LiveScaleEstimator.estimate(4.0, 5.6, 4000, 2000, 480, 20.0)) // 5cm: degenerate
        assertNull(LiveScaleEstimator.estimate(4.0, 5.6, 0, 2000, 480, 1.0))
    }
}

class FrameRotationTest {

    @Test
    fun `upright source roundtrip for all orientations`() {
        val srcW = 1000; val srcH = 2000
        for (rot in listOf(0, 90, 180, 270)) {
            val upW = FrameRotation.uprightWidth(srcW, srcH, rot)
            val upH = FrameRotation.uprightHeight(srcW, srcH, rot)
            val points = listOf(0 to 0, upW / 2 to upH / 2, upW - 1 to upH - 1, 3 to 7)
            for ((ux, uy) in points) {
                val (sx, sy) = FrameRotation.uprightToSource(ux, uy, rot, srcW, srcH)
                assertTrue(sx in 0 until srcW && sy in 0 until srcH)
                val (bx, by) = FrameRotation.sourceToUpright(sx, sy, rot, srcW, srcH)
                assertEquals("rot=$rot", ux, bx)
                assertEquals("rot=$rot", uy, by)
            }
        }
    }

    @Test
    fun `remap across 90 degree rotation keeps physical correspondences`() {
        val srcW = 1000; val srcH = 2000
        val (ux, uy) = 250.0 to 400.0
        val (nx, ny) = FrameRotation.remapUpright(ux, uy, oldRotationDeg = 0, newRotationDeg = 90, srcW = srcW, srcH = srcH)
        // verify roundtrip back to the same point
        val (rx, ry) = FrameRotation.remapUpright(nx, ny, 90, 0, srcW, srcH)
        assertEquals(ux, rx, 1.5)
        assertEquals(uy, ry, 1.5)
    }
}
