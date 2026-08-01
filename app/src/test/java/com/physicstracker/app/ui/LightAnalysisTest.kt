package com.physicstracker.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LightAnalysisTest {
    @Test
    fun statisticsFindSeparatedPeaksAndMainPeakWidth() {
        val values = listOf(0.0, 2.0, 8.0, 2.0, 0.0, 1.0, 6.0, 1.0, 0.0)
        val samples = values.mapIndexed { index, value -> LightSamplePoint(index.toDouble(), value) }

        val stats = buildLightStats(samples, detectPeaks = true)

        assertEquals(listOf(2.0, 6.0), stats.peakPositions)
        assertEquals(4.0, stats.peakSpacing!!, 1e-9)
        assertEquals(2.0, stats.fwhm!!, 1e-9)
        assertTrue(stats.contrast!! > 0.99)
    }

    @Test
    fun flatSignalDoesNotReportFalsePeaksOrWidth() {
        val samples = (0..8).map { LightSamplePoint(it.toDouble(), 42.0) }

        val stats = buildLightStats(samples, detectPeaks = true)

        assertTrue(stats.peakPositions.isEmpty())
        assertNull(stats.peakSpacing)
        assertNull(stats.fwhm)
        assertEquals(42.0, stats.mean!!, 1e-9)
    }

    @Test
    fun timeSeriesStaysSortedAndReplacesSameFrame() {
        val target = mutableListOf<LightTimePoint>()

        updateLightTimeSeries(target, 30_000L, 30.0)
        updateLightTimeSeries(target, 10_000L, 10.0)
        updateLightTimeSeries(target, 10_500L, 11.0)

        assertEquals(2, target.size)
        assertEquals(10_500L, target[0].timestampUs)
        assertEquals(11.0, target[0].intensity, 1e-9)
        assertEquals(30_000L, target[1].timestampUs)
    }
}
