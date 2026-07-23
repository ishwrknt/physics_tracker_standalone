package com.physicstracker.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class ExportRepresentationTest {
    @Test
    fun csvIdentifiesSelectedAveragedRepresentation() {
        val point = DisplayKinematicsPoint(
            pointId = 1L,
            trackLabel = "A",
            frameNumber = 0,
            timestampUs = 0L,
            xValue = 1.0,
            yValue = 2.0,
            gapLabel = null,
            vxValue = 3.0,
            vyValue = 4.0,
            speedValue = 5.0,
            axValue = null,
            ayValue = null,
            accelerationValue = null,
            dataRepresentation = "POSITION_AVERAGED_5"
        )

        val csv = buildCsvContent("test", "m", listOf(point))

        assertTrue(csv.lineSequence().first().contains("data_representation"))
        assertTrue(csv.contains("POSITION_AVERAGED_5"))
    }
}
