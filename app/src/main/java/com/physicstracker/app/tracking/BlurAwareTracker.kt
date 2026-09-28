package com.physicstracker.app.tracking

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Experimental translation-only local-reference tracker. No detector/centroid substitution.
 * Callers supply a consistent full-resolution pixel grid and its indexed presentation timestamp.
 * Bitmap/PTS correspondence must be verified by the decoder, not inferred by this class.
 */
class ArgbTrackingFrame(val width: Int, val height: Int, private val pixels: IntArray) {
    init { require(width > 0 && height > 0 && pixels.size.toLong() == width.toLong() * height) }
    internal fun pixel(x: Int, y: Int): Int = pixels[y * width + x]
}

enum class TargetReference { SELECTED_POINT, OBJECT_PART, EXPLICIT_OBJECT_CENTRE }
enum class BlurTrackingPhase { RELIABLE, RECOVERING, NEEDS_CORRECTION }

/** Development values, not calibrated universal blur/confidence thresholds. */
data class BlurTrackingConfig(
    val patchRadius: Int = 8,
    val localSearchRadius: Int = 20,
    val maximumSearchRadius: Int = 64,
    val recoveryDurationUs: Long = 750_000,
    val clearFramesToResume: Int = 2,
    val minimumContrast: Double = 0.015,
    val sharpnessRatio: Double = 0.45,
    val contrastRatio: Double = 0.55,
    val minimumAppearance: Double = 0.82,
    val updateAppearance: Double = 0.94
) {
    init {
        require(patchRadius in 2..24 && localSearchRadius in 2..maximumSearchRadius)
        require(maximumSearchRadius <= 180 && recoveryDurationUs > 0 && clearFramesToResume >= 2)
        require(listOf(minimumContrast, sharpnessRatio, contrastRatio, minimumAppearance,
            updateAppearance).all { it.isFinite() && it in 0.0..1.0 })
        require(updateAppearance >= minimumAppearance)
    }
}

internal class ReferencePatch(
    val radius: Int,
    val luma: DoubleArray,
    val mean: Double,
    val energy: Double,
    val redGreen: Double,
    val blueGreen: Double,
    val quality: RegionQuality,
    val contextLuma: Double?
)

/** State is owned by one tracker. Clear patches are never exposed or mutated by consumers. */
class BlurTrackingState internal constructor(
    val reference: TargetReference,
    val selectedAnchor: Offset,
    val motion: TimedTargetMotion,
    val frameWidth: Int,
    val frameHeight: Int,
    internal val anchorFraction: Offset,
    internal val initialPatch: ReferencePatch,
    internal val clearPatch: ReferencePatch,
    val lastTimestampUs: Long,
    val phase: BlurTrackingPhase = BlurTrackingPhase.RELIABLE,
    val recoveryStartedUs: Long? = null,
    internal val pendingPosition: Offset? = null,
    internal val clearStreak: Int = 0,
    val unresolvedFrames: Int = 0
) {
    internal fun advance(
        timestampUs: Long,
        phase: BlurTrackingPhase,
        motion: TimedTargetMotion = this.motion,
        clearPatch: ReferencePatch = this.clearPatch,
        recoveryStartedUs: Long? = this.recoveryStartedUs,
        pendingPosition: Offset? = null,
        clearStreak: Int = 0,
        unresolvedFrames: Int = this.unresolvedFrames
    ) = BlurTrackingState(reference, selectedAnchor, motion, frameWidth, frameHeight, anchorFraction,
        initialPatch, clearPatch, timestampUs, phase, recoveryStartedUs, pendingPosition, clearStreak, unresolvedFrames)
}

data class BlurTrackingStep(
    val state: BlurTrackingState,
    /** Non-null only after visual validation AND TrackingIntegrityGate.ACCEPT. */
    val measuredPosition: Offset? = null,
    val prediction: Offset? = null,
    val assessment: TrackingAssessment? = null,
    val diagnostics: TrackingDiagnostics? = null,
    val reason: String,
    val candidateCount: Int = 0,
    val processingNanos: Long = 0
)

class BlurAwareTracker(private val config: BlurTrackingConfig = BlurTrackingConfig()) {
    private val gate = TrackingIntegrityGate(TrackingQuality.SMART, RecoveryPolicy.CONTROLLED_THEN_PAUSE)

    /** Null means insufficient visible support. Ask for a clearer seed, never silently move it. */
    fun seed(frame: ArgbTrackingFrame, selected: Offset, timestampUs: Long,
             reference: TargetReference = TargetReference.SELECTED_POINT): BlurTrackingState? {
        if (!selected.x.isFinite() || !selected.y.isFinite() || timestampUs < 0 ||
            selected.x !in 0f..(frame.width - 1f) || selected.y !in 0f..(frame.height - 1f)) return null
        val x = selected.x.roundToInt()
        val y = selected.y.roundToInt()
        val radius = min(config.patchRadius, min(min(x, y), min(frame.width - 1 - x, frame.height - 1 - y)))
        if (radius < 2) return null
        val patch = patch(frame, x, y, radius)
        if (!patch.quality.usable(config.minimumContrast)) return null
        return BlurTrackingState(reference, selected, TimedTargetMotion(selected, timestampUs),
            frame.width, frame.height, selected - Offset(x.toFloat(), y.toFloat()), patch, patch, timestampUs)
    }

    fun track(frame: ArgbTrackingFrame, state: BlurTrackingState, timestampUs: Long,
              checkCancellation: () -> Unit = {}): BlurTrackingStep {
        val started = System.nanoTime()
        if (timestampUs <= state.lastTimestampUs || frame.width != state.frameWidth || frame.height != state.frameHeight) {
            return BlurTrackingStep(state.advance(state.lastTimestampUs, BlurTrackingPhase.NEEDS_CORRECTION),
                reason = "Frame timing or dimensions changed. Select the target again.")
        }
        if (state.phase == BlurTrackingPhase.NEEDS_CORRECTION) return BlurTrackingStep(state,
            reason = "The target is unclear here. Tap it again to continue.")
        val prediction = state.motion.predict(timestampUs)
        val elapsed = (timestampUs - state.motion.timestampUs) / 1_000_000.0
        val radius = (config.localSearchRadius + elapsed * 30 +
            (state.motion.velocityPerSecond.getDistance() * elapsed * 0.35))
            .roundToInt().coerceIn(config.localSearchRadius, config.maximumSearchRadius)
        fun uncertain(reason: String, assessment: TrackingAssessment? = null,
                      diagnostics: TrackingDiagnostics? = null, pending: Offset? = null,
                      streak: Int = 0, count: Int = 0): BlurTrackingStep {
            val recoveryStart = state.recoveryStartedUs ?: timestampUs
            val phase = if (timestampUs - recoveryStart >= config.recoveryDurationUs)
                BlurTrackingPhase.NEEDS_CORRECTION else BlurTrackingPhase.RECOVERING
            return BlurTrackingStep(state.advance(timestampUs, phase, recoveryStartedUs = recoveryStart,
                pendingPosition = pending, clearStreak = streak,
                unresolvedFrames = state.unresolvedFrames + 1), prediction = prediction,
                assessment = assessment, diagnostics = diagnostics, reason = reason,
                candidateCount = count, processingNanos = System.nanoTime() - started)
        }
        if (state.recoveryStartedUs?.let { timestampUs - it >= config.recoveryDurationUs } == true)
            return uncertain("The target is unclear here. Tap it again to continue.")
        val support = state.clearPatch.radius
        val cx = (prediction.x - state.anchorFraction.x).roundToInt()
        val cy = (prediction.y - state.anchorFraction.y).roundToInt()
        val left = max(support, cx - radius)
        val right = min(frame.width - support - 1, cx + radius)
        val top = max(support, cy - radius)
        val bottom = min(frame.height - support - 1, cy + radius)
        if (left > right || top > bottom) return uncertain("Target is outside the searchable image.")

        // Full-resolution integer search preserves tiny targets; no image pyramid rounding.
        // Retain response values for a second distinct peak; do not average candidate positions.
        val responses = ArrayList<Response>((right - left + 1) * (bottom - top + 1))
        for (y in top..bottom) {
            checkCancellation()
            for (x in left..right) {
                val appearance = correlation(frame, x, y, state.clearPatch)
                val point = Offset(x.toFloat(), y.toFloat()) + state.anchorFraction
                val evidence = CandidateEvidence(point, appearance)
                val score = ConservativeEvidenceScorer.score(evidence, prediction, radius.toDouble()) ?: continue
                responses += Response(x, y, appearance, score)
            }
        }
        val best = responses.maxByOrNull { it.score } ?: return uncertain("No visible target support.")
        val point = Offset(best.x.toFloat(), best.y.toFloat()) + state.anchorFraction
        val candidatePatch = patch(frame, best.x, best.y, support)
        val quality = candidatePatch.quality
        val initialAgreement = correlation(frame, best.x, best.y, state.initialPatch)
        val colour = (1.0 - (abs(candidatePatch.redGreen - state.clearPatch.redGreen) +
            abs(candidatePatch.blueGreen - state.clearPatch.blueGreen)) / 2).coerceIn(0.0, 1.0)
        val context = state.clearPatch.contextLuma?.let { previous ->
            candidatePatch.contextLuma?.let { current -> 1.0 - abs(current - previous) }
        }
        val score = ConservativeEvidenceScorer.score(
            CandidateEvidence(point, best.appearance, colour = colour, context = context), prediction, radius.toDouble())!!
        val second = responses.filter {
            abs(it.x - best.x) > max(2, support / 2) || abs(it.y - best.y) > max(2, support / 2)
        }.maxOfOrNull { it.appearance }
        val ambiguity = second?.let { (1.0 - it).coerceAtLeast(0.001) / (1.0 - best.appearance).coerceAtLeast(0.001) }
        val diagnostics = TrackingDiagnostics(matchScore = score, ambiguityRatio = ambiguity,
            appearanceChange = 1.0 - min(initialAgreement, best.appearance),
            residualPixels = (point - prediction).getDistance().toDouble(),
            displacementPixels = (point - state.motion.position).getDistance().toDouble(), searchRadiusPixels = radius.toDouble())
        val assessment = gate.assess(diagnostics)
        if (!quality.usable(config.minimumContrast) || quality.degradedFrom(state.clearPatch.quality,
                config.sharpnessRatio, config.contrastRatio)) {
            return uncertain("Image detail has dropped. Checking nearby frames.", assessment, diagnostics, count = responses.size)
        }
        if (best.appearance < config.minimumAppearance || initialAgreement < config.minimumAppearance ||
            colour < 0.8 || assessment.decision != TrackingDecision.ACCEPT) {
            return uncertain("The target is unclear here. Checking nearby frames.", assessment, diagnostics, count = responses.size)
        }
        if (state.phase == BlurTrackingPhase.RECOVERING) {
            val pendingDistance = state.pendingPosition?.let { (point - it).getDistance() }
            val allowed = config.localSearchRadius + state.motion.velocityPerSecond.getDistance() *
                ((timestampUs - state.lastTimestampUs) / 1_000_000f)
            val streak = if (pendingDistance != null && pendingDistance <= allowed) state.clearStreak + 1 else 1
            if (streak < config.clearFramesToResume) return uncertain("Verifying the recovered target.",
                assessment, diagnostics, point, streak, responses.size)
        }
        // Freeze on recovery transitions and jumps, and retain the protected initial appearance.
        val mayUpdate = state.phase == BlurTrackingPhase.RELIABLE && best.appearance >= config.updateAppearance &&
            initialAgreement >= config.updateAppearance && (point - state.motion.position).getDistance() <= support * 2 &&
            quality.sharpness >= state.clearPatch.quality.sharpness * 0.9
        return BlurTrackingStep(state.advance(timestampUs, BlurTrackingPhase.RELIABLE,
            motion = state.motion.measured(point, timestampUs), clearPatch = if (mayUpdate) candidatePatch else state.clearPatch,
            recoveryStartedUs = null), measuredPosition = point, prediction = prediction, assessment = assessment,
            diagnostics = diagnostics, reason = if (state.phase == BlurTrackingPhase.RECOVERING) "Target recovered" else "Measured",
            candidateCount = responses.size, processingNanos = System.nanoTime() - started)
    }

    private data class Response(val x: Int, val y: Int, val appearance: Double, val score: Double)

    private fun correlation(frame: ArgbTrackingFrame, x: Int, y: Int, reference: ReferencePatch): Double {
        var sum = 0.0
        var squared = 0.0
        var cross = 0.0
        var i = 0
        for (dy in -reference.radius..reference.radius) for (dx in -reference.radius..reference.radius) {
            val value = luma(frame.pixel(x + dx, y + dy))
            sum += value
            squared += value * value
            cross += (reference.luma[i++] - reference.mean) * value
        }
        val energy = (squared - sum * sum / reference.luma.size).coerceAtLeast(0.0)
        val denominator = sqrt(energy * reference.energy)
        return if (denominator <= 1e-10) 0.0 else (cross / denominator).coerceIn(0.0, 1.0)
    }

    private fun patch(frame: ArgbTrackingFrame, x: Int, y: Int, radius: Int): ReferencePatch {
        val side = radius * 2 + 1
        val values = DoubleArray(side * side)
        var redGreen = 0.0
        var blueGreen = 0.0
        var i = 0
        for (dy in -radius..radius) for (dx in -radius..radius) {
            val pixel = frame.pixel(x + dx, y + dy)
            values[i++] = luma(pixel)
            redGreen += ((pixel shr 16 and 255) - (pixel shr 8 and 255)) / 255.0
            blueGreen += ((pixel and 255) - (pixel shr 8 and 255)) / 255.0
        }
        var ringSum = 0.0
        var count = 0
        for (dy in -radius - 3..radius + 3) for (dx in -radius - 3..radius + 3) {
            if (abs(dx) <= radius && abs(dy) <= radius) continue
            if (x + dx !in 0 until frame.width || y + dy !in 0 until frame.height) continue
            ringSum += luma(frame.pixel(x + dx, y + dy))
            count++
        }
        val mean = values.average()
        return ReferencePatch(radius, values, mean, values.sumOf { (it - mean) * (it - mean) },
            redGreen / values.size, blueGreen / values.size, RegionQuality.measure(values, side),
            if (count > 0) ringSum / count else null)
    }

    private fun luma(argb: Int): Double = ((argb shr 16 and 255) * 0.299 +
        (argb shr 8 and 255) * 0.587 + (argb and 255) * 0.114) / 255.0
}
