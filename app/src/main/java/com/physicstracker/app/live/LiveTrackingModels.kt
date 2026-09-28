package com.physicstracker.app.live

import com.physicstracker.app.ui.calibrationUnitFactor
import kotlin.math.hypot

/**
 * Live Tracking module state machine. Independent from the video workflow.
 */
enum class LiveTrackingState(val label: String) {
    IDLE("Ready"),
    AWAITING_TARGET("Tap an object to track"),
    CALIBRATING("Calibrating"),
    TRACKING("Tracking"),
    RECORDING("Recording"),
    PAUSED("Paused"),
    REACQUIRING("Reacquiring"),
    LOW_CONFIDENCE("Low confidence"),
    OBJECT_LOST("Object lost"),
    CAMERA_ERROR("Camera error")
}

/**
 * Scale calibration for live tracking, expressed in *analysis-frame* pixel coordinates
 * (the upright, downscaled frames the tracker actually consumes).
 */
data class LiveCalibration(
    val stickP1x: Float,
    val stickP1y: Float,
    val stickP2x: Float,
    val stickP2y: Float,
    val knownDistanceMeters: Double,
    val unit: String,
    val metersPerPixel: Double,
    val pixelsPerMeter: Double
) {
    companion object {
        fun create(
            p1x: Float, p1y: Float, p2x: Float, p2y: Float,
            knownLength: Double, unit: String
        ): LiveCalibration? {
            if (knownLength <= 0.0) return null
            val pixelDistance = hypot((p2x - p1x).toDouble(), (p2y - p1y).toDouble())
            if (pixelDistance <= 0.0) return null
            val knownMeters = knownLength * calibrationUnitFactor(unit)
            if (knownMeters <= 0.0) return null
            return LiveCalibration(
                stickP1x = p1x, stickP1y = p1y, stickP2x = p2x, stickP2y = p2y,
                knownDistanceMeters = knownMeters, unit = unit,
                metersPerPixel = knownMeters / pixelDistance,
                pixelsPerMeter = pixelDistance / knownMeters
            )
        }
    }
}

/** Raw tracker observation in analysis-frame pixels with an actual elapsed timestamp. */
data class LiveRawSample(
    val elapsedUs: Long,
    val xPx: Double,
    val yPx: Double,
    val confidence: Double
)

/** Calibrated, filtered kinematics sample derived from raw live observations. */
data class LivePhysicsPoint(
    val elapsedUs: Long,
    val xMeters: Double,
    val yMeters: Double,
    val vxMetersPerSec: Double? = null,
    val vyMetersPerSec: Double? = null,
    val axMetersPerSec2: Double? = null,
    val ayMetersPerSec2: Double? = null,
    val speedMetersPerSec: Double? = null,
    val accelerationMetersPerSec2: Double? = null
)

/** Per-frame event handed from the tracking pipeline to the UI layer. */
data class LiveFrameUpdate(
    val state: LiveTrackingState,
    val positionXPx: Float?,
    val positionYPx: Float?,
    val confidence: Double,
    val frameWidth: Int,
    val frameHeight: Int,
    val recordedSamples: Int
)
