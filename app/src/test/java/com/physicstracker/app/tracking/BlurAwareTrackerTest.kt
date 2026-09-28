package com.physicstracker.app.tracking

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException
import kotlin.random.Random

class BlurAwareTrackerTest {
    private val config = BlurTrackingConfig(patchRadius = 4, localSearchRadius = 12, maximumSearchRadius = 24)
    private val tracker = BlurAwareTracker(config)

    private fun frame(x: Int = 30, y: Int = 30, distractorX: Int? = null, flat: Boolean = false,
                      width: Int = 80, height: Int = 70): ArgbTrackingFrame {
        val pixels = IntArray(width * height) { 0xff202020.toInt() }
        fun target(cx: Int) {
            val random = Random(41)
            for (dy in -4..4) for (dx in -4..4) {
                val value = if (flat) 100 else random.nextInt(50, 250)
                pixels[(y + dy) * width + cx + dx] = 0xff000000.toInt() or (value shl 16) or (value shl 8) or value
            }
        }
        target(x)
        distractorX?.let(::target)
        return ArgbTrackingFrame(width, height, pixels)
    }

    @Test fun selectedPointMovesExactlyWithoutPredictionBlending() {
        val seed = tracker.seed(frame(), Offset(30f, 30f), 0)!!
        val step = tracker.track(frame(33, 32), seed, 40_001)
        assertEquals(Offset(33f, 32f), step.measuredPosition)
        assertEquals(TrackingDecision.ACCEPT, step.assessment?.decision)
        assertEquals(40_001L, step.state.motion.timestampUs)
        assertEquals(Offset(30f, 30f), step.state.selectedAnchor)
    }

    @Test fun fractionalAnchorIsPreservedWithoutChangingPixelSamplingConvention() {
        val seed = tracker.seed(frame(), Offset(30.25f, 30.4f), 0)!!
        val step = tracker.track(frame(32, 31), seed, 33_333)
        assertEquals(32.25f, step.measuredPosition!!.x, 0.00001f)
        assertEquals(31.4f, step.measuredPosition!!.y, 0.00001f)
    }

    @Test fun twoIdenticalTargetsAreNotAveragedOrSilentlyAccepted() {
        val seed = tracker.seed(frame(), Offset(30f, 30f), 0)!!
        val step = tracker.track(frame(23, 30, distractorX = 37), seed, 40_000)
        assertNull(step.measuredPosition)
        assertEquals(BlurTrackingPhase.RECOVERING, step.state.phase)
        assertTrue("ambiguous-match" in step.assessment!!.reasons)
        assertSame(seed.clearPatch, step.state.clearPatch)
    }

    @Test fun lossFreezesModelAndTimeoutRequiresExplicitCorrection() {
        val seed = tracker.seed(frame(), Offset(30f, 30f), 0)!!
        val first = tracker.track(frame(flat = true), seed, 40_000)
        assertNull(first.measuredPosition)
        assertSame(seed.clearPatch, first.state.clearPatch)
        val expired = tracker.track(frame(), first.state, 800_000)
        assertNull(expired.measuredPosition)
        assertEquals(BlurTrackingPhase.NEEDS_CORRECTION, expired.state.phase)
        val stillPaused = tracker.track(frame(), expired.state, 900_000)
        assertNull(stillPaused.measuredPosition)
        val corrected = tracker.seed(frame(35), Offset(35f, 30f), 900_000)!!
        assertEquals(Offset.Zero, corrected.motion.velocityPerSecond)
        assertEquals(BlurTrackingPhase.RELIABLE, corrected.phase)
    }

    @Test fun recoveryNeedsTwoClearMatchesAndDoesNotBackfillTheGap() {
        val seed = tracker.seed(frame(), Offset(30f, 30f), 0)!!
        val lost = tracker.track(frame(flat = true), seed, 40_000)
        val pending = tracker.track(frame(32), lost.state, 80_000)
        assertNull(pending.measuredPosition)
        assertSame(seed.clearPatch, pending.state.clearPatch)
        val recovered = tracker.track(frame(33), pending.state, 120_000)
        assertEquals(Offset(33f, 30f), recovered.measuredPosition)
        assertEquals(BlurTrackingPhase.RELIABLE, recovered.state.phase)
        assertEquals(2, recovered.state.unresolvedFrames)
        assertSame(seed.clearPatch, recovered.state.clearPatch)
    }

    @Test fun initiallyFlatSeedAndBorderDoNotSilentlySnapToOtherFeatures() {
        assertNull(tracker.seed(frame(flat = true), Offset(30f, 30f), 0))
        assertNull(tracker.seed(frame(), Offset(0f, 0f), 0))
        assertNull(tracker.seed(frame(), Offset(Float.NaN, 30f), 0))
    }

    @Test fun resizeAndNonIncreasingTimeRequireReseed() {
        val seed = tracker.seed(frame(), Offset(30f, 30f), 100)!!
        assertEquals(BlurTrackingPhase.NEEDS_CORRECTION, tracker.track(frame(), seed, 100).state.phase)
        assertNull(tracker.track(frame(width = 90), seed, 200).measuredPosition)
    }

    @Test fun independentTracksDoNotShareRecoveryOrReferenceState() {
        val a = tracker.seed(frame(), Offset(30f, 30f), 0)!!
        val b = tracker.seed(frame(50), Offset(50f, 30f), 0, TargetReference.OBJECT_PART)!!
        tracker.track(frame(flat = true), a, 40_000)
        val movedB = tracker.track(frame(52), b, 40_000)
        assertEquals(Offset(52f, 30f), movedB.measuredPosition)
        assertEquals(TargetReference.OBJECT_PART, movedB.state.reference)
        assertEquals(0, movedB.state.unresolvedFrames)
        assertEquals(BlurTrackingPhase.RELIABLE, a.phase)
    }

    @Test fun portraitAndLandscapeGridsPreserveAnchor() {
        for ((width, height) in listOf(80 to 70, 70 to 80, 120 to 90)) {
            val seed = tracker.seed(frame(width = width, height = height), Offset(30f, 30f), 0)!!
            val result = tracker.track(frame(32, 33, width = width, height = height), seed, 40_000)
            assertEquals(Offset(32f, 33f), result.measuredPosition)
        }
    }

    @Test fun variablePresentationIntervalsUsePixelsPerSecond() {
        val motion = TimedTargetMotion(Offset(10f, 10f), 20_000)
            .measured(Offset(12f, 14f), 120_000)
        assertEquals(Offset(18f, 26f), motion.predict(420_000))
    }

    @Test fun invalidAndMissingEvidenceCannotCreateConfidence() {
        val base = CandidateEvidence(Offset.Zero, 0.8)
        assertEquals(0.8, ConservativeEvidenceScorer.score(base, Offset.Zero, 20.0)!!, 1e-9)
        assertNull(ConservativeEvidenceScorer.score(base.copy(flow = Double.NaN), Offset.Zero, 20.0))
        assertNull(ConservativeEvidenceScorer.score(base.copy(appearance = Double.POSITIVE_INFINITY), Offset.Zero, 20.0))
        assertNull(ConservativeEvidenceScorer.score(base, Offset.Zero, 0.0))
        assertNull(ConservativeEvidenceScorer.score(base.copy(position = Offset(Float.NaN, 0f)), Offset.Zero, 20.0))
    }

    @Test fun sharpnessIsRelativeAndFlatAppearanceIsNotAutomaticallyNewBlur() {
        val flat = RegionQuality.measure(DoubleArray(81) { 0.5 }, 9)
        assertFalse(flat.degradedFrom(flat, 0.45, 0.55))
        val clear = RegionQuality(0.2, 0.1)
        assertTrue(RegionQuality(0.15, 0.01).degradedFrom(clear, 0.45, 0.55))
        assertFalse(clear.degradedFrom(clear, 0.45, 0.55))
    }

    @Test(expected = CancellationException::class)
    fun cancellationInterruptsSearchWithoutMutatingSeed() {
        val seed = tracker.seed(frame(), Offset(30f, 30f), 0)!!
        tracker.track(frame(), seed, 40_000) { throw CancellationException() }
    }
}
