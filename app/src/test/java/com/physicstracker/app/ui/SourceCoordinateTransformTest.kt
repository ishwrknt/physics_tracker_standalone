package com.physicstracker.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceCoordinateTransformTest {
    @Test
    fun sourceViewRoundTripStaysWithinHalfPixelAcrossLayoutsAndRotations() {
        val sources = listOf(848 to 478, 1920 to 1080, 1080 to 1920, 641 to 479)
        val views = listOf(1080 to 1920, 1920 to 1080, 1280 to 800, 800 to 1280)

        for ((sourceWidth, sourceHeight) in sources) {
            val points = listOf(
                SourcePoint(0.0, 0.0),
                SourcePoint(sourceWidth - 1.0, 0.0),
                SourcePoint(0.0, sourceHeight - 1.0),
                SourcePoint(sourceWidth - 1.0, sourceHeight - 1.0),
                SourcePoint((sourceWidth - 1.0) / 2.0, (sourceHeight - 1.0) / 2.0),
                SourcePoint(17.25, 31.75)
            )
            for ((viewWidth, viewHeight) in views) {
                for (rotation in QuarterRotation.entries) {
                    val transform = SourceCoordinateTransform(
                        sourceWidth,
                        sourceHeight,
                        viewWidth,
                        viewHeight,
                        rotation
                    )
                    for (point in points) {
                        val result = transform.viewToSource(transform.sourceToView(point))!!
                        assertEquals(point.x.coerceIn(0.0, sourceWidth - 1.0), result.x, 0.5)
                        assertEquals(point.y.coerceIn(0.0, sourceHeight - 1.0), result.y, 0.5)
                    }
                }
            }
        }
    }

    @Test
    fun tenOnePixelNudgesProduceExactlyTenSourcePixels() {
        val transform = SourceCoordinateTransform(848, 478, 1920, 1080)
        var point = SourcePoint(200.25, 100.75)
        repeat(10) { point = transform.nudge(point, 1.0, 0.0) }
        assertEquals(210.25, point.x, 0.0)
        assertEquals(100.75, point.y, 0.0)
    }

    @Test
    fun unclampedTouchOutsideLetterboxedVideoIsRejected() {
        val transform = SourceCoordinateTransform(848, 478, 1080, 1920)
        assertNull(transform.viewToSource(ViewPoint(540.0, 10.0), clamp = false))
    }

    @Test
    fun sourceCoordinateDoesNotChangeWhenViewZoomOrOverlayLayoutChanges() {
        val source = SourcePoint(411.375, 207.625)
        val layouts = listOf(
            SourceCoordinateTransform(848, 478, 1080, 1920),
            SourceCoordinateTransform(848, 478, 1920, 1080),
            SourceCoordinateTransform(848, 478, 713, 511)
        )
        layouts.forEach { transform ->
            val restored = transform.viewToSource(transform.sourceToView(source))!!
            assertEquals(source.x, restored.x, 0.5)
            assertEquals(source.y, restored.y, 0.5)
        }
    }
}
