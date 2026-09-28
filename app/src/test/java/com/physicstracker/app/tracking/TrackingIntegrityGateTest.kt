package com.physicstracker.app.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.geometry.Offset

class TrackingIntegrityGateTest {
    // Hybrid scorer tests cover deterministic motion/context fusion.

    @Test
    fun hybridScorerPrefersStrongCandidateNearPrediction() {
        val chosen = HybridCandidateScorer.choose(
            listOf(
                HybridTrackingCandidate("weak-near", Offset(101f, 100f), confidence = 0.78),
                HybridTrackingCandidate("strong-far", Offset(140f, 100f), confidence = 0.80)
            ),
            predictedCenter = Offset(100f, 100f),
            searchRadiusPx = 64.0
        )
        assertEquals("weak-near", chosen?.source)
    }


    @Test
    fun hybridScorerReturnsNullForNoCandidates() {
        assertEquals(null, HybridCandidateScorer.choose(emptyList(), Offset.Zero, 32.0))
    }
    @Test
    fun precisePausesOnAmbiguousDistractor() {
        val assessment = TrackingIntegrityGate(TrackingQuality.PRECISE).assess(
            TrackingDiagnostics(
                matchScore = 0.94,
                peakToSidelobeRatio = 4.2,
                ambiguityRatio = 1.04,
                appearanceChange = 0.08,
                residualPixels = 3.0,
                displacementPixels = 18.0,
                searchRadiusPixels = 64.0
            )
        )
        assertEquals(TrackingDecision.PAUSE, assessment.decision)
        assertTrue("ambiguity must be reported", "ambiguous-match" in assessment.reasons)
    }

    @Test
    fun smartAcceptsDistinctMotionConsistentObservation() {
        val assessment = TrackingIntegrityGate(TrackingQuality.SMART).assess(
            TrackingDiagnostics(
                matchScore = 0.92,
                peakToSidelobeRatio = 10.0,
                ambiguityRatio = 1.65,
                appearanceChange = 0.06,
                residualPixels = 4.0,
                displacementPixels = 31.0,
                searchRadiusPixels = 90.0
            )
        )
        assertEquals(TrackingDecision.ACCEPT, assessment.decision)
    }

    @Test
    fun quickMarksPredictionInsteadOfFabricatingMeasurement() {
        val assessment = TrackingIntegrityGate(TrackingQuality.QUICK).assess(
            TrackingDiagnostics(
                matchScore = 0.30,
                ambiguityRatio = 1.01,
                residualPixels = 55.0,
                displacementPixels = 70.0,
                searchRadiusPixels = 60.0
            )
        )
        assertEquals(TrackingDecision.MARK_PREDICTED, assessment.decision)
    }

    @Test
    fun adaptiveRadiusAccountsForFastMotionAndMisses() {
        val state = MotionState(10.0, 20.0, velocityX = 35.0, velocityY = 12.0, missedFrames = 1)
        assertTrue(state.adaptiveSearchRadius(base = 24.0, maximum = 140.0) > 80.0)
    }
}
