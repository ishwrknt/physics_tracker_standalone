package com.physicstracker.app.ui.light

import androidx.compose.ui.geometry.Offset
import com.physicstracker.app.ui.LightChannel
import com.physicstracker.app.ui.LightGeometry
import com.physicstracker.app.ui.LightTool
import org.junit.Assert.*
import org.junit.Test

class LightMeasurementTest {
    private fun pixels(w: Int = 8, h: Int = 6, pixel: (Int, Int) -> Int): LightPixels = object : LightPixels {
        override val width = w
        override val height = h
        override fun argb(x: Int, y: Int) = pixel(x, y)
    }
    private fun grey(value: Int) = (255 shl 24) or (value shl 16) or (value shl 8) or value
    private fun point(x: Float, y: Float) = LightGeometry(LightTool.POINT, Offset(x, y), Offset(x, y))
    private fun data(samples: List<LightObservation>, slot: String = "A") = LightDataset(
        "photo, trial 1.png", "content://private/image", 8, 6, slot, LightChannel.R,
        point(3f, 2f), false, samples, "Original source pixels")

    @Test fun knownChannelsUseActualOpaquePixelValues() {
        assertEquals(255.0, lightChannelValue(0xffff0000.toInt(), LightChannel.R)!!, 1e-9)
        assertEquals(0.0, lightChannelValue(0xffff0000.toInt(), LightChannel.B)!!, 1e-9)
        assertEquals(76.245, lightChannelValue(0xffff0000.toInt(), LightChannel.GRAY)!!, 1e-9)
        assertNull(lightChannelValue(0x00ffffff, LightChannel.GRAY))
    }

    @Test fun pointIsNotAnUnlabelledNeighbourhoodAverage() {
        val frame = pixels { x, y -> grey(if (x == 3 && y == 2) 255 else 0) }
        val measured = measureLightProfile(frame, point(3f, 2f), LightChannel.R)
        assertEquals(listOf(LightObservation(0.0, 255.0)), measured)
    }

    @Test fun quantizationKeepsMarkerReadoutAndSampleOnTheSamePixel() {
        val geometry = canonicalLightGeometry(point(3.5f, 2.5f), 8, 6)
        assertEquals(Offset(4f, 3f), geometry.start)
        assertEquals(geometry.start, geometry.end)
        val value = measureLightProfile(pixels { x, y -> grey(x + y * 8) }, geometry, LightChannel.R).single().value
        assertEquals(28.0, value, 1e-9)
    }

    @Test fun sourcePixelRoundTripsAtDifferentScalesAndAspectRatios() {
        for ((w, h) in listOf(80 to 60, 60 to 80, 1 to 1)) {
            for (zoom in listOf(1f, 1.5f, 2f, 4f, 8f)) {
                val map = LightImageMapping(w, h, 123.4f * zoom, 234.5f * zoom)
                for (p in listOf(Offset.Zero, Offset((w - 1).toFloat(), (h - 1).toFloat()), Offset((w / 2).toFloat(), (h / 2).toFloat()))) {
                    assertEquals(p, map.toSource(map.toView(p)))
                }
                assertNull(map.toSource(Offset(-1f, -1f)))
            }
        }
    }

    @Test fun lineAndBoxHandlesCanBeDraggedWithoutChangingOtherRegions() {
        val mapping = LightImageMapping(100, 80, 200f, 160f)
        val line = LightGeometry(LightTool.LINE, Offset(10f, 20f), Offset(40f, 20f))
        assertEquals(LightDragTarget.END, lightDragTarget(line, mapping, mapping.toView(line.end), 12f))
        val resized = dragLightGeometry(line, LightDragTarget.END, line.end, Offset(50f, 30f), 100, 80)
        assertEquals(line.start, resized.start)
        assertEquals(Offset(50f, 30f), resized.end)
        val box = LightGeometry(LightTool.BOX, Offset(10f, 10f), Offset(30f, 30f))
        assertEquals(LightDragTarget.MOVE, lightDragTarget(box, mapping, mapping.toView(Offset(20f, 20f)), 10f))
        val moved = dragLightGeometry(box, LightDragTarget.MOVE, Offset(20f, 20f), Offset(95f, 75f), 100, 80)
        assertEquals(Offset(79f, 59f), moved.start)
        assertEquals(Offset(99f, 79f), moved.end)
    }

    @Test fun lineAxisUsesSourceDistanceAndIncludesBothEndpoints() {
        val line = LightGeometry(LightTool.LINE, Offset(1f, 2f), Offset(6f, 2f))
        val samples = measureLightProfile(pixels { x, _ -> grey(x * 10) }, line, LightChannel.R)
        assertEquals(6, samples.size)
        assertEquals(LightObservation(0.0, 10.0), samples.first())
        assertEquals(LightObservation(5.0, 60.0), samples.last())
    }

    @Test fun zeroLengthLineIsOneSample() {
        val samples = measureLightProfile(pixels { _, _ -> grey(7) }, point(2f, 2f).copy(tool = LightTool.LINE), LightChannel.R)
        assertEquals(listOf(LightObservation(0.0, 7.0)), samples)
    }

    @Test fun areaMeanWeightsOpaquePixelsRatherThanColumnMeans() {
        val frame = pixels(2, 2) { x, y -> when { x == 0 -> grey(0); y == 0 -> grey(90); else -> 0 } }
        val box = LightGeometry(LightTool.BOX, Offset.Zero, Offset(1f, 1f))
        assertEquals(30.0, measureLightMean(frame, box, LightChannel.R)!!, 1e-9)
        assertEquals(listOf(0.0, 90.0), measureLightProfile(frame, box, LightChannel.R).map { it.value })
    }

    @Test fun reversedAreaKeepsIdenticalMeasurements() {
        val a = LightGeometry(LightTool.BOX, Offset(1f, 1f), Offset(5f, 4f))
        val frame = pixels { x, y -> grey(x * y) }
        assertEquals(measureLightProfile(frame, a, LightChannel.R), measureLightProfile(frame, a.copy(start = a.end, end = a.start), LightChannel.R))
    }

    @Test fun closeButDistinctTimestampsAreNeverMerged() {
        val values = listOf(LightObservation(0.01, 1.0, 10_000), LightObservation(0.0105, 2.0, 10_500),
            LightObservation(0.01, 3.0, 10_000))
        val sorted = canonicalLightTimes(values.reversed())
        assertEquals(listOf(10_000L, 10_500L), sorted.map { it.timestampUs })
        assertEquals(2, sorted.size)
    }

    @Test fun flatAndSingleSampleGraphsHaveFiniteMatchingMarkerCoordinates() {
        for (samples in listOf(emptyList(), listOf(LightObservation(0.0, 100.0)),
            (0..8).map { LightObservation(it.toDouble(), 100.0) })) {
            val bounds = LightPlotBounds.of(samples)
            assertTrue(bounds.maxX > bounds.minX)
            samples.forEach { assertTrue(bounds.normalized(it).x.isFinite() && bounds.normalized(it).y.isFinite()) }
        }
    }

    @Test fun exportContainsSameValuesAndSeparatesRegionsWithoutLeakingUri() {
        val csv = lightCsv(listOf(data(listOf(LightObservation(0.0, 15.0))),
            data(listOf(LightObservation(0.0, 231.0)), "B")))
        assertTrue(csv.contains("\"A\""))
        assertTrue(csv.contains("\"B\""))
        assertTrue(csv.contains(",15.0,"))
        assertTrue(csv.contains(",231.0,"))
        assertTrue(csv.contains("\"photo, trial 1.png\""))
        assertFalse(csv.contains("content://"))
    }

    @Test fun csvPreservesMicrosecondsAndDefusesSpreadsheetFormulasInNames() {
        val dataset = data(listOf(LightObservation(0.010501, 20.0, 10_501))).copy(mediaName = "=BAD()", timeSeries = true, complete = false)
        val csv = lightCsv(listOf(dataset))
        assertTrue(csv.contains("\"'=BAD()\""))
        assertTrue(csv.contains(",10501,"))
        assertTrue(csv.contains(",false,"))
    }

    @Test fun plotReductionPreservesNarrowPeaksWithoutChangingRawData() {
        val samples = (0..5000).map { LightObservation(it.toDouble(), if (it == 2371) 255.0 else 10.0) }
        assertEquals(255.0, lightPlotSamples(samples).maxOf { it.value }, 1e-9)
        assertEquals(5001, samples.size)
        assertTrue(lightPlotSamples(samples).size <= 1200)
    }

    @Test fun sdrYuvBlackWhiteAndNeutralGreyRespectRange() {
        assertEquals(0xff000000.toInt(), lightYuvToArgb(16, 128, 128, false, false))
        assertEquals(0xffffffff.toInt(), lightYuvToArgb(235, 128, 128, true, false))
        assertEquals(0xff808080.toInt(), lightYuvToArgb(128, 128, 128, true, true))
    }

    @Test fun missingPixelsCreateGraphBreaksAndAreRecordedInCsv() {
        val geometry = LightGeometry(LightTool.LINE, Offset.Zero, Offset(7f, 0f))
        val samples = measureLightProfile(pixels { x, _ -> if (x == 3) 0 else grey(x * 10) }, geometry, LightChannel.R)
        assertTrue(samples.first { it.axis == 4.0 }.gapBefore)
        assertFalse(samples.first { it.axis == 5.0 }.gapBefore)
        assertTrue(lightCsv(listOf(data(samples))).contains("gap_before"))
        assertTrue(lightPlotSamples(samples).first { it.axis == 4.0 }.gapBefore)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidSelectionIsRejectedInsteadOfClampedToUnrelatedPixels() {
        measureLightProfile(pixels { _, _ -> grey(0) }, point(-1f, 2f), LightChannel.R)
    }
}
