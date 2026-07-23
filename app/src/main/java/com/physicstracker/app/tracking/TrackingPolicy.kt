package com.physicstracker.app.tracking

import kotlin.math.hypot

enum class TargetProfile(val label: String, val defaultQuality: TrackingQuality) {
    MARKER("MARKER", TrackingQuality.QUICK),
    OBJECT("OBJECT", TrackingQuality.SMART),
    ROBUST("ROBUST", TrackingQuality.PRECISE)
}

enum class TrackingQuality { QUICK, SMART, PRECISE }

enum class RecoveryPolicy { PREDICT_AND_SEARCH, CONTROLLED_THEN_PAUSE, PAUSE_IMMEDIATELY }

enum class ObservationKind { MEASURED, PREDICTED, MANUAL }

enum class TrackingDecision { ACCEPT, MARK_PREDICTED, PAUSE }

data class TrackingDiagnostics(
    val matchScore: Double,
    val peakToSidelobeRatio: Double? = null,
    val ambiguityRatio: Double? = null,
    val appearanceChange: Double? = null,
    val residualPixels: Double,
    val displacementPixels: Double,
    val searchRadiusPixels: Double
)

data class TrackingAssessment(
    val confidence: Double,
    val decision: TrackingDecision,
    val reasons: Set<String>
)

/**
 * Pure, deterministic scientific-integrity gate shared by every tracking engine.
 * Prediction is allowed to guide a search but this gate never labels prediction as measurement.
 */
class TrackingIntegrityGate(
    private val quality: TrackingQuality,
    private val recoveryPolicy: RecoveryPolicy = quality.defaultRecoveryPolicy()
) {
    fun assess(diagnostics: TrackingDiagnostics): TrackingAssessment {
        val reasons = linkedSetOf<String>()
        val normalizedResidual = (diagnostics.residualPixels /
            diagnostics.searchRadiusPixels.coerceAtLeast(1.0)).coerceIn(0.0, 2.0)
        val motionScore = (1.0 - normalizedResidual).coerceIn(0.0, 1.0)
        val ambiguityScore = diagnostics.ambiguityRatio
            ?.let { ((it - 1.0) / 0.8).coerceIn(0.0, 1.0) }
            ?: 0.45
        val psrScore = diagnostics.peakToSidelobeRatio
            ?.let { ((it - 3.0) / 9.0).coerceIn(0.0, 1.0) }
            ?: 0.45
        val appearanceScore = diagnostics.appearanceChange
            ?.let { (1.0 - it).coerceIn(0.0, 1.0) }
            ?: diagnostics.matchScore.coerceIn(0.0, 1.0)
        val confidence = (
            diagnostics.matchScore.coerceIn(0.0, 1.0) * 0.34 +
                motionScore * 0.25 +
                ambiguityScore * 0.20 +
                psrScore * 0.11 +
                appearanceScore * 0.10
            ).coerceIn(0.0, 1.0)

        val minimum = when (quality) {
            TrackingQuality.QUICK -> 0.55
            TrackingQuality.SMART -> 0.67
            TrackingQuality.PRECISE -> 0.76
        }
        if (confidence < minimum) reasons += "low-confidence"
        if ((diagnostics.ambiguityRatio ?: Double.POSITIVE_INFINITY) < 1.12) reasons += "ambiguous-match"
        if ((diagnostics.peakToSidelobeRatio ?: Double.POSITIVE_INFINITY) < 5.0) reasons += "weak-peak"
        if (normalizedResidual > 0.82) reasons += "motion-inconsistent"
        if ((diagnostics.appearanceChange ?: 0.0) > 0.48) reasons += "appearance-change"

        val trustworthy = reasons.isEmpty() && confidence >= minimum
        val decision = when {
            trustworthy -> TrackingDecision.ACCEPT
            recoveryPolicy == RecoveryPolicy.PREDICT_AND_SEARCH -> TrackingDecision.MARK_PREDICTED
            else -> TrackingDecision.PAUSE
        }
        return TrackingAssessment(confidence, decision, reasons)
    }
}

fun TrackingQuality.defaultRecoveryPolicy(): RecoveryPolicy = when (this) {
    TrackingQuality.QUICK -> RecoveryPolicy.PREDICT_AND_SEARCH
    TrackingQuality.SMART -> RecoveryPolicy.CONTROLLED_THEN_PAUSE
    TrackingQuality.PRECISE -> RecoveryPolicy.PAUSE_IMMEDIATELY
}

data class MotionState(
    val x: Double,
    val y: Double,
    val velocityX: Double = 0.0,
    val velocityY: Double = 0.0,
    val missedFrames: Int = 0
) {
    fun predict(frameDelta: Double = 1.0): Pair<Double, Double> =
        x + velocityX * frameDelta to y + velocityY * frameDelta

    fun adaptiveSearchRadius(base: Double, maximum: Double): Double =
        (base + hypot(velocityX, velocityY) * 1.8 + missedFrames * base * 0.6)
            .coerceIn(base, maximum)
}
