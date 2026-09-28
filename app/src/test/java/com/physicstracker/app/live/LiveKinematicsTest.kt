package com.physicstracker.app.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LiveKinematicsTest {

    private fun samples(
        count: Int,
        startUs: Long = 0L,
        stepUs: Long = 33_333L,
        y: (Int) -> Double = { 0.0 },
        x: (Int) -> Double
    ): List<LiveRawSample> = (0 until count).map { i ->
        LiveRawSample(startUs + i * stepUs, x(i), y(i), 1.0)
    }

    @Test
    fun `stationary object reports zero velocity`() {
        val engine = LiveKinematicsEngine(metersPerPixel = 0.01, smoothingRadius = 1)
        val points = samples(30) { 100.0 }.mapNotNull { engine.onSample(it) }
        engine.flush()
        assertTrue(points.size > 10)
        val last = points.last()
        assertNotNull(last.vxMetersPerSec)
        assertEquals(0.0, last.vxMetersPerSec!!, 1e-9)
        assertEquals(0.0, last.vyMetersPerSec!!, 1e-9)
        assertEquals(0.0, last.speedMetersPerSec!!, 1e-9)
    }

    @Test
    fun `constant velocity is recovered from actual timestamps`() {
        val mpp = 0.01
        val dxPx = 2.0
        val stepUs = 33_333L
        val engine = LiveKinematicsEngine(metersPerPixel = mpp, smoothingRadius = 1)
        val points = samples(30, stepUs = stepUs) { it * dxPx }.mapNotNull { engine.onSample(it) }
        engine.flush()
        assertTrue(points.size > 10)
        val last = points.last()
        val expectedVx = dxPx * mpp / (stepUs / 1_000_000.0)
        assertEquals(expectedVx, last.vxMetersPerSec!!, expectedVx * 0.01)
        assertEquals(expectedVx, last.speedMetersPerSec!!, expectedVx * 0.01)
    }

    @Test
    fun `acceleration of uniformly accelerating motion is recovered`() {
        val mpp = 0.01
        val stepS = 1.0 / 60.0
        val stepUs = (stepS * 1_000_000).toLong()
        val a = 2.0 // m/s^2
        val engine = LiveKinematicsEngine(metersPerPixel = mpp, smoothingRadius = 1)
        // x = 0.5 * a * t^2, in pixels
        val points = samples(240, stepUs = stepUs) { i ->
            val t = i * stepS
            (0.5 * a * t * t) / mpp
        }.mapNotNull { engine.onSample(it) }
        val measured = points.drop(20).dropLast(3).mapNotNull { it.axMetersPerSec2 }
        assertTrue(measured.isNotEmpty())
        val meanAx = measured.average()
        assertEquals(a, meanAx, a * 0.02)
    }

    @Test
    fun `irregular timestamps use actual deltas not assumed fps`() {
        val mpp = 0.01
        val dt1 = 10_000L
        val dt2 = 50_000L
        val engine = LiveKinematicsEngine(metersPerPixel = mpp, smoothingRadius = 0)
        // 1 px per dt1 step
        var t = 0L
        repeat(4) { i ->
            engine.onSample(LiveRawSample(t, i * 1.0, 0.0, 1.0))
            t += dt1
        }
        // last sample was at t=30_000 with x=3px; place the next one dt2 later at x=4px
        val slow = engine.onSample(LiveRawSample(30_000L + dt2, 4.0, 0.0, 1.0))
        assertNotNull(slow)
        val expected = 1.0 * mpp / (dt2 / 1_000_000.0)
        assertEquals(expected, slow!!.vxMetersPerSec!!, expected * 0.01)
    }

    @Test
    fun `gap breaks segment and derivatives never bridge it`() {
        val engine = LiveKinematicsEngine(metersPerPixel = 1.0, smoothingRadius = 1, maxGapUs = 100_000L)
        samples(10) { it.toDouble() }.forEach { engine.onSample(it) }
        engine.breakSegment()
        // First sample after break must yield no velocity (window restarts).
        val afterBreak = engine.onSample(LiveRawSample(10_000_000L, 20.0, 0.0, 1.0))
        assertNull(afterBreak)
        val second = engine.onSample(LiveRawSample(10_000_000L + 33_333L, 21.0, 0.0, 1.0))
        // Second sample only completes window edge; still no full centered window yet
        // with radius 1 it needs 3 samples.
        assertNull(second?.vxMetersPerSec)
    }

    @Test
    fun `smoothing reduces positional noise`() {
        val mpp = 0.01
        val rawEngine = LiveKinematicsEngine(metersPerPixel = mpp, smoothingRadius = 0)
        val smoothEngine = LiveKinematicsEngine(metersPerPixel = mpp, smoothingRadius = 3)
        var seed = 42L
        fun rand(): Double {
            seed = (seed * 6364136223846793005L + 1442695040888963407L)
            return ((seed ushr 11) and 0xFFFF).toDouble() / 0xFFFF - 0.5
        }
        val rawPoints = samples(60) { 100.0 + 4.0 * rand() }
        val rawOut = rawPoints.mapNotNull { rawEngine.onSample(it) }
        val smoothOut = rawPoints.mapNotNull { smoothEngine.onSample(it) }
        rawEngine.flush(); smoothEngine.flush()
        fun spread(points: List<LivePhysicsPoint>): Double {
            val xs = points.map { it.xMeters }
            return xs.max() - xs.min()
        }
        assertTrue(spread(smoothOut) < spread(rawOut))
    }

    @Test
    fun `positions are relative to the first sample`() {
        val engine = LiveKinematicsEngine(metersPerPixel = 0.01, smoothingRadius = 0)
        val points = samples(5) { 50.0 + it }.mapNotNull { engine.onSample(it) }
        engine.flush()
        assertEquals(0.0, points.first().xMeters, 1e-12)
        assertTrue(abs(points.last().xMeters - 0.04) < 1e-9)
    }
}

class RollingLiveBufferTest {

    @Test
    fun `buffer caps size and drops oldest`() {
        val buffer = RollingLiveBuffer<Int>(5)
        (1..10).forEach { buffer.add(it) }
        assertEquals(5, buffer.size)
        assertEquals(listOf(6, 7, 8, 9, 10), buffer.toList())
    }

    @Test
    fun `window returns only recent items`() {
        val buffer = RollingLiveBuffer<Pair<Long, Int>>(100)
        (0 until 10).forEach { buffer.add((it * 1_000_000L) to it) }
        val window = buffer.window(2_500_000L) { it.first }
        // newest at t=9s; cutoff 6.5s -> items 7,8,9
        assertEquals(listOf(7, 8, 9), window.map { it.second })
    }

    @Test
    fun `tail returns most recent items in order`() {
        val buffer = RollingLiveBuffer<Int>(10)
        (1..6).forEach { buffer.add(it) }
        assertEquals(listOf(5, 6), buffer.tail(2))
        assertEquals(listOf(1, 2, 3, 4, 5, 6), buffer.tail(100))
    }
}

class LiveCalibrationTest {

    @Test
    fun `scale math produces meters per pixel`() {
        val cal = LiveCalibration.create(0f, 0f, 100f, 0f, 1.0, "m")
        assertNotNull(cal)
        assertEquals(0.01, cal!!.metersPerPixel, 1e-12)
        assertEquals(100.0, cal.pixelsPerMeter, 1e-9)
    }

    @Test
    fun `unit factors are applied`() {
        val cal = LiveCalibration.create(0f, 0f, 50f, 0f, 10.0, "cm")
        assertNotNull(cal)
        assertEquals(0.1 / 50.0, cal!!.metersPerPixel, 1e-12)
    }

    @Test
    fun `invalid inputs are rejected`() {
        assertNull(LiveCalibration.create(0f, 0f, 0f, 0f, 1.0, "m"))
        assertNull(LiveCalibration.create(0f, 0f, 100f, 0f, 0.0, "m"))
        assertNull(LiveCalibration.create(0f, 0f, 100f, 0f, -1.0, "m"))
    }
}
