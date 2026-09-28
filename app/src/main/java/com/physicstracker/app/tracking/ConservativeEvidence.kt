package com.physicstracker.app.tracking

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

/** Missing evidence is null, never a copy of another cue's confidence. */
data class CandidateEvidence(
    val position: Offset,
    val appearance: Double,
    val colour: Double? = null,
    val flow: Double? = null,
    val shape: Double? = null,
    val context: Double? = null
)

object ConservativeEvidenceScorer {
    /** Appearance dominates; motion/context can rank visual matches, never create one. */
    fun score(candidate: CandidateEvidence, prediction: Offset, uncertaintyPx: Double): Double? {
        val values = listOfNotNull(candidate.appearance, candidate.colour, candidate.flow,
            candidate.shape, candidate.context)
        if (!candidate.position.x.isFinite() || !candidate.position.y.isFinite() ||
            !prediction.x.isFinite() || !prediction.y.isFinite() ||
            !uncertaintyPx.isFinite() || uncertaintyPx <= 0 ||
            values.any { !it.isFinite() || it !in 0.0..1.0 }) return null
        var weighted = candidate.appearance * 0.8
        var weight = 0.8
        fun add(value: Double?, importance: Double) {
            if (value != null) { weighted += value * importance; weight += importance }
        }
        add(candidate.colour, 0.08)
        add(candidate.flow, 0.15)
        add(candidate.shape, 0.05)
        // Context changes naturally as an object moves: weak support, never an identity veto.
        add(candidate.context, 0.02)
        val residual = (candidate.position - prediction).getDistance() / uncertaintyPx
        return (weighted / weight) * (0.95 + 0.05 / (1.0 + residual * residual))
    }
}

/** Per-source-pixel metrics, averaged over sample count; no resized patches here. */
data class RegionQuality(val contrast: Double, val sharpness: Double) {
    fun usable(minimumContrast: Double) = contrast.isFinite() && sharpness.isFinite() &&
        contrast >= minimumContrast

    fun degradedFrom(reliable: RegionQuality, sharpnessRatio: Double, contrastRatio: Double): Boolean =
        contrast < reliable.contrast * contrastRatio ||
            (reliable.sharpness > 1e-8 && sharpness < reliable.sharpness * sharpnessRatio)

    companion object {
        fun measure(luma: DoubleArray, side: Int): RegionQuality {
            require(side >= 3 && luma.size == side * side && luma.all { it.isFinite() })
            val mean = luma.average()
            val variance = luma.sumOf { (it - mean) * (it - mean) } / luma.size
            var laplace = 0.0
            var laplaceSquared = 0.0
            for (y in 1 until side - 1) for (x in 1 until side - 1) {
                val i = y * side + x
                val lap = luma[i - 1] + luma[i + 1] + luma[i - side] + luma[i + side] - 4 * luma[i]
                laplace += lap
                laplaceSquared += lap * lap
            }
            val count = (side - 2) * (side - 2)
            return RegionQuality(sqrt(variance),
                (laplaceSquared / count - (laplace / count) * (laplace / count)).coerceAtLeast(0.0))
        }
    }
}

/** Velocity is pixels/second, not pixels/frame. Raw measurements are never smoothed. */
data class TimedTargetMotion(
    val position: Offset,
    val timestampUs: Long,
    val velocityPerSecond: Offset = Offset.Zero
) {
    fun predict(timestampUs: Long): Offset = position + velocityPerSecond *
        ((timestampUs - this.timestampUs).coerceAtLeast(0) / 1_000_000f)

    fun measured(position: Offset, timestampUs: Long): TimedTargetMotion {
        require(timestampUs > this.timestampUs)
        return TimedTargetMotion(position, timestampUs,
            (position - this.position) / ((timestampUs - this.timestampUs) / 1_000_000f))
    }
}
