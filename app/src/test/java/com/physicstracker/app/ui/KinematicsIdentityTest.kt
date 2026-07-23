package com.physicstracker.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KinematicsIdentityTest {
    @Test
    fun strengthControlsPositionAverageWindow() {
        assertEquals(3, positionAverageWindowForStrength(0f))
        assertEquals(5, positionAverageWindowForStrength(0.5f))
        assertEquals(9, positionAverageWindowForStrength(1f))
    }

    @Test
    fun derivativesNeverCrossTrackerBoundary() {
        val samples = listOf(
            CalibratedPoint(1, "A", 0, 0, 0.0, 0.0),
            CalibratedPoint(3, "B", 0, 0, 100.0, 0.0),
            CalibratedPoint(2, "A", 1, 1_000_000, 1.0, 0.0),
            CalibratedPoint(4, "B", 1, 1_000_000, 102.0, 0.0)
        )
        val result = computeKinematics(samples)
        val speedsA = result.filter { it.trackerId == "A" }.mapNotNull { it.speedMetersPerSec }
        val speedsB = result.filter { it.trackerId == "B" }.mapNotNull { it.speedMetersPerSec }
        assertTrue(speedsA.all { kotlin.math.abs(it - 1.0) < 1e-9 })
        assertTrue(speedsB.all { kotlin.math.abs(it - 2.0) < 1e-9 })
        assertEquals(4, result.size)
    }

    @Test
    fun positionAveragingReducesTrackingNoiseBeforeVelocityCalculation() {
        val noise = doubleArrayOf(0.5, -0.5, 0.0)
        val samples = (0..20).map { index ->
            CalibratedPoint(
                pointId = index.toLong(),
                trackerId = "A",
                frameNumber = index,
                timestampUs = index * 1_000_000L,
                xMeters = index + noise[index % noise.size],
                yMeters = 0.0
            )
        }

        val originalPositions = samples.map { it.xMeters }
        val raw = computeKinematics(samples)
        val averaged = computeKinematics(samples, positionAverageRadius = 2)
        val rawMeanError = raw.mapNotNull { it.vxMetersPerSec }.map { kotlin.math.abs(it - 1.0) }.average()
        val averagedMeanError = averaged.mapNotNull { it.vxMetersPerSec }.map { kotlin.math.abs(it - 1.0) }.average()

        assertTrue("position averaging should reduce velocity noise", averagedMeanError < rawMeanError)
        assertEquals(originalPositions, samples.map { it.xMeters })
    }

    @Test
    fun averagingAndDerivativesDoNotCrossMissingFrameSegments() {
        val samples = listOf(
            CalibratedPoint(1, "A", 0, 0, 0.0, 0.0),
            CalibratedPoint(2, "A", 1, 1_000_000, 1.0, 0.0),
            CalibratedPoint(3, "A", 4, 4_000_000, 100.0, 0.0),
            CalibratedPoint(4, "A", 5, 5_000_000, 101.0, 0.0)
        )

        val result = computeKinematics(samples, positionAverageRadius = 2)
        val velocities = result.mapNotNull { it.vxMetersPerSec }

        assertEquals(4, velocities.size)
        assertTrue(velocities.all { kotlin.math.abs(it - 1.0) < 1e-9 })
    }
}
