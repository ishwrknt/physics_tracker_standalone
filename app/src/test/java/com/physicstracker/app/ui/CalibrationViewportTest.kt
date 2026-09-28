package com.physicstracker.app.ui

import com.physicstracker.app.data.CalibrationEntity
import com.physicstracker.app.data.TrackPointEntity
import org.junit.Assert.*
import org.junit.Test

class CalibrationViewportTest {
    private val calibration = CalibrationEntity(1L, 100.0, 100.0, 300.0, 100.0, 2.0,
        originX = 200.0, originY = 150.0, coordinateSpace = "SOURCE_VIDEO", sourceWidth = 640, sourceHeight = 480)

    @Test fun viewportRoundTripPreservesCalibrationAcrossRotationsAndSizes() {
        for (rotation in listOf(0, 90, 180, 270)) for (scale in listOf(0.5f, 1f, 1.5f, 2f, 4f, 8f)) {
            val swapped = rotation == 90 || rotation == 270
            val adapter = CalibrationViewport(640, 480, rotation,
                PixelViewport(31f, 75f, (if (swapped) 480 else 640) * scale, (if (swapped) 640 else 480) * scale))
            for (angle in listOf(-135.0, 0.0, 30.0, 90.0, 179.0)) {
                val source = calibration.copy(rotationDegrees = angle)
                val restored = adapter.toSource(adapter.toView(source))
                assertEquals(source.originX, restored.originX, 0.001)
                assertEquals(source.originY, restored.originY, 0.001)
                assertEquals(source.stickP1x, restored.stickP1x, 0.001)
                assertEquals(source.stickP2y, restored.stickP2y, 0.001)
                assertEquals(angle, restored.rotationDegrees, 0.001)
                assertEquals(calibrationMetrics(source)!!.metersPerPixel, calibrationMetrics(restored)!!.metersPerPixel, 0.000001)
            }
        }
    }

    @Test fun originIsZeroAndRotatedAxesHaveCorrectSignsAndUnits() {
        for (angle in listOf(0.0, 30.0, 90.0, 180.0, 270.0)) {
            val cal = calibration.copy(rotationDegrees = angle)
            fun position(x: Double, y: Double) = calibratePoint(TrackPointEntity(projectId = 1L, frameNumber = 0,
                timestampUs = 0L, pixelX = x, pixelY = y, sourceWidth = 640, sourceHeight = 480), cal)!!
            val zero = position(cal.originX, cal.originY)
            assertEquals(0.0, zero.xMeters, 0.0)
            assertEquals(0.0, zero.yMeters, 0.0)
            val radians = Math.toRadians(angle)
            val x = position(cal.originX + 100 * kotlin.math.cos(radians), cal.originY + 100 * kotlin.math.sin(radians))
            val y = position(cal.originX + 100 * kotlin.math.sin(radians), cal.originY - 100 * kotlin.math.cos(radians))
            assertEquals(1.0, x.xMeters, 1e-9)
            assertEquals(0.0, x.yMeters, 1e-9)
            assertEquals(0.0, y.xMeters, 1e-9)
            assertEquals(1.0, y.yMeters, 1e-9)
        }
    }

    @Test fun editingAxesPreservesExactScaleAndEditingScalePreservesOrigin() {
        val adapter = CalibrationViewport(640, 480, 90, PixelViewport(23f, 47f, 333f, 444f))
        val source = calibration.copy(stickP1x = 100.123456789, originX = 200.123456789)
        val view = adapter.toView(source)
        val moved = adapter.applyViewEdit(view.copy(originX = view.originX + 20, rotationDegrees = 35.0), source)
        assertEquals(source.stickP1x, moved.stickP1x, 0.0)
        assertEquals(calibrationMetrics(source), calibrationMetrics(moved))
        val scaled = adapter.applyViewEdit(view.copy(stickP2x = view.stickP2x + 20), source)
        assertEquals(source.originX, scaled.originX, 0.0)
        assertEquals(source.rotationDegrees, scaled.rotationDegrees, 0.0)
    }

    @Test fun legacyCalibrationIsNotSilentlyReinterpreted() {
        val legacy = calibration.copy(coordinateSpace = "LEGACY_DISPLAY")
        val adapter = CalibrationViewport(640, 480, 90, PixelViewport(20f, 30f, 240f, 320f))
        assertEquals(legacy, adapter.toView(legacy))
        assertEquals(legacy, adapter.toSource(legacy))
    }

    @Test fun centreZeroChangesOnlyOrigin() {
        for (rotation in listOf(0, 90, 180, 270)) {
            val adapter = CalibrationViewport(640, 480, rotation, PixelViewport(20f, 30f, 240f, 320f))
            val centred = adapter.centreZero(calibration)
            assertEquals(319.5, centred.originX, 0.0)
            assertEquals(239.5, centred.originY, 0.0)
            assertEquals(calibration.copy(originX = 319.5, originY = 239.5), centred)
        }
    }

    @Test fun resetDirectionIsHorizontalOnScreenAndPreservesEverythingElse() {
        for (rotation in listOf(0, 90, 180, 270)) {
            val adapter = CalibrationViewport(640, 480, rotation, PixelViewport(20f, 30f, 240f, 320f))
            val source = calibration.copy(rotationDegrees = 37.0)
            val reset = adapter.resetDirection(source)
            assertEquals(0.0, adapter.toView(reset).rotationDegrees, 0.001)
            assertEquals(source.copy(rotationDegrees = reset.rotationDegrees), reset)
            assertEquals(calibrationMetrics(source), calibrationMetrics(reset))
        }
    }

    @Test fun lockBlocksCalibrationButAllowsMeasurement() {
        assertFalse(calibrationEditAllowed(locked = true, measurementOnly = false))
        assertTrue(calibrationEditAllowed(locked = true, measurementOnly = true))
        assertTrue(calibrationEditAllowed(locked = false, measurementOnly = false))
        assertTrue(calibrationEditAllowed(locked = false, measurementOnly = true))
    }
}
