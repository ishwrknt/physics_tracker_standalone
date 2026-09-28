package com.physicstracker.app.live.physics

import com.physicstracker.app.live.LivePhysicsPoint
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Extensible physics-consumer interface for live tracking data.
 *
 * Future experiment modules (pendulum, projectile, SHM, circular motion, collision,
 * rotation, energy, momentum ...) consume the calibrated live kinematics stream through
 * this contract without the tracking layer knowing anything about them.
 */
interface LivePhysicsModule {
    /** Stable identifier persisted with saved sessions. */
    val modeId: String
    val displayName: String

    fun reset()
    fun onPoint(point: LivePhysicsPoint)

    /** Human-readable live statistics: label -> value (null renders as a dash). */
    fun stats(): List<Pair<String, Double?>>
}

/**
 * First built-in module: generic kinematics (position, velocity, acceleration).
 */
class KinematicsLiveModule : LivePhysicsModule {
    override val modeId: String = "kinematics"
    override val displayName: String = "Kinematics"

    private var firstX: Double? = null
    private var firstY: Double? = null
    private var lastX: Double? = null
    private var lastY: Double? = null
    private var startUs: Long? = null
    private var endUs: Long? = null
    private var maxSpeed: Double? = null
    private var speedSum = 0.0
    private var speedCount = 0

    override fun reset() {
        firstX = null; firstY = null; lastX = null; lastY = null
        startUs = null; endUs = null
        maxSpeed = null; speedSum = 0.0; speedCount = 0
    }

    override fun onPoint(point: LivePhysicsPoint) {
        if (firstX == null) {
            firstX = point.xMeters
            firstY = point.yMeters
            startUs = point.elapsedUs
        }
        lastX = point.xMeters
        lastY = point.yMeters
        endUs = point.elapsedUs
        point.speedMetersPerSec?.let { speed ->
            maxSpeed = maxOf(maxSpeed ?: speed, speed)
            speedSum += speed
            speedCount += 1
        }
    }

    override fun stats(): List<Pair<String, Double?>> {
        val durationS = if (startUs != null && endUs != null) {
            (endUs!! - startUs!!) / 1_000_000.0
        } else null
        val displacement = if (firstX != null && lastX != null) {
            hypot(lastX!! - firstX!!, lastY!! - firstY!!)
        } else null
        return listOf(
            "Duration (s)" to durationS,
            "Displacement (m)" to displacement,
            "Max speed (m/s)" to maxSpeed,
            "Mean speed (m/s)" to if (speedCount > 0) speedSum / speedCount else null,
            "Samples" to speedCount.toDouble().takeIf { it > 0 }
        )
    }
}
