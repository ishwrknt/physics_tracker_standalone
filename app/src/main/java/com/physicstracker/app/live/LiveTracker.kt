package com.physicstracker.app.live

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import com.physicstracker.app.tracking.ClassicTrackerEngine
import com.physicstracker.app.tracking.ClassicTrackerMode
import com.physicstracker.app.tracking.ClassicTrackingSeed

data class LiveTrackResult(
    val xPx: Double,
    val yPx: Double,
    val confidence: Double,
    val matchScore: Double?,
    val estimatedRadiusPx: Float = 14f
)

/**
 * Stable interface between the live frame pipeline and any tracking implementation.
 * An ML-based tracker (e.g. MediaPipe/LiteRT) can be added later by implementing this
 * same contract; the pipeline does not know which tracker it drives.
 */
interface LiveTracker {
    /** Stable identifier persisted with saved sessions. */
    val algorithmId: String
    val isSeeded: Boolean

    /** Estimated object extent for the overlay bounding indicator. */
    val estimatedRadiusPx: Float get() = 14f

    /** Predicted next center (REACQUIRING cues). */
    val predictedCenter: Pair<Double, Double>? get() = null

    /** Consecutive failed measurements since the last accepted one. */
    val consecutiveMisses: Int get() = 0

    /** Seed the tracker on the tapped location of an upright analysis frame. */
    fun seed(frame: Bitmap, x: Float, y: Float): Boolean

    /** Track on the next upright analysis frame. Null means the object could not be measured.
     *  [timestampUs] is the sensor/monotonic elapsed timestamp used for motion prediction. */
    fun track(frame: Bitmap, timestampUs: Long): LiveTrackResult?

    /** Handle a miss that the tracker itself accepted (pipeline-level gating). */
    fun noteExternalMiss() {}

    /** Remap internal state after device rotation. */
    fun remapAfterRotation(oldRotationDeg: Int, newRotationDeg: Int) {}

    fun release()
}

/**
 * Adapter over the existing classical template tracker ([ClassicTrackerEngine] FAST mode),
 * reusing the proven SSD/geometry matching pipeline for live frames without modifying it.
 * Only called from the single live-pipeline thread.
 */
class TemplateLiveTracker : LiveTracker {
    override val algorithmId: String = "classic-template-fast"
    override val isSeeded: Boolean get() = seed != null

    private val engine = ClassicTrackerEngine(ClassicTrackerMode.FAST)
    private var seed: ClassicTrackingSeed? = null

    override fun seed(frame: Bitmap, x: Float, y: Float): Boolean {
        val newSeed = engine.seed(frame, Offset(x, y))?.copy(velocity = Offset.Zero)
        seed = newSeed
        return newSeed != null
    }

    override fun track(frame: Bitmap, timestampUs: Long): LiveTrackResult? {
        val current = seed ?: return null
        if (frame.width != current.sourceWidth || frame.height != current.sourceHeight) return null
        val update = engine.trackFrame(frame, current) ?: return null
        val updatedSeed = update.seed.copy(velocity = Offset.Zero)
        seed = updatedSeed
        return LiveTrackResult(
            xPx = update.seed.center.x.toDouble(),
            yPx = update.seed.center.y.toDouble(),
            confidence = update.confidence,
            matchScore = update.errorScore
        )
    }

    override fun release() {
        seed = null
    }
}
