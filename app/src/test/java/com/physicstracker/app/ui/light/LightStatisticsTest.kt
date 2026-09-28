package com.physicstracker.app.ui.light

import org.junit.Assert.*
import org.junit.Test

class LightStatisticsTest {
    @Test fun exactLinearTrendUsesObservedAxis() {
        val result = lightStatistics(listOf(
            LightObservation(0.0, 10.0), LightObservation(0.5, 20.0), LightObservation(2.0, 50.0)))!!
        assertEquals(20.0, result.slope!!, 1e-9)
        assertEquals(10.0, result.intercept!!, 1e-9)
        assertEquals(1.0, result.rSquared!!, 1e-9)
    }

    @Test fun singleObservationHasNoInventedFit() {
        val result = lightStatistics(listOf(LightObservation(7.0, 100.0)))!!
        assertNull(result.slope)
        assertNull(result.rSquared)
    }
}
