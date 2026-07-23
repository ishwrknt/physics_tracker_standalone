package com.physicstracker.app.ui

import com.physicstracker.app.data.CalibrationEntity
import com.physicstracker.app.data.TrackPointEntity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

data class CalibratedPoint(
    val pointId: Long,
    val trackerId: String,
    val frameNumber: Int,
    val timestampUs: Long,
    val xMeters: Double,
    val yMeters: Double
)

data class CalibrationMetrics(
    val metersPerPixel: Double,
    val pixelsPerMeter: Double
)

data class KinematicsPoint(
    val pointId: Long,
    val trackerId: String,
    val frameNumber: Int,
    val timestampUs: Long,
    val xMeters: Double,
    val yMeters: Double,
    val vxMetersPerSec: Double?,
    val vyMetersPerSec: Double?,
    val speedMetersPerSec: Double?,
    val axMetersPerSec2: Double?,
    val ayMetersPerSec2: Double?,
    val accelerationMetersPerSec2: Double?
)

fun calibrationUnitFactor(unit: String): Double {
    return when (unit) {
        "mm" -> 0.001
        "cm" -> 0.01
        "m" -> 1.0
        else -> 1.0
    }
}

fun pixelsPerSelectedUnit(metrics: CalibrationMetrics, calibration: CalibrationEntity): Double {
    return metrics.pixelsPerMeter * calibrationUnitFactor(calibration.calibrationUnit)
}

fun calibrationMetrics(calibration: CalibrationEntity): CalibrationMetrics? {
    val dx = calibration.stickP2x - calibration.stickP1x
    val dy = calibration.stickP2y - calibration.stickP1y
    val pixelDistance = hypot(dx, dy)
    if (pixelDistance <= 0.0 || calibration.knownDistanceMeters <= 0.0) return null
    val metersPerPixel = calibration.knownDistanceMeters / pixelDistance
    return CalibrationMetrics(
        metersPerPixel = metersPerPixel,
        pixelsPerMeter = pixelDistance / calibration.knownDistanceMeters
    )
}

fun calibratePoint(point: TrackPointEntity, calibration: CalibrationEntity): CalibratedPoint? {
    if (point.coordinateSpace != calibration.coordinateSpace) return null
    if (point.coordinateSpace == "SOURCE_VIDEO" &&
        (point.sourceWidth != calibration.sourceWidth || point.sourceHeight != calibration.sourceHeight)) return null
    val metrics = calibrationMetrics(calibration) ?: return null
    val radians = (calibration.rotationDegrees / 180.0) * PI
    val xAxisX = cos(radians)
    val xAxisY = sin(radians)
    val yAxisX = cos(radians - (PI / 2.0))
    val yAxisY = sin(radians - (PI / 2.0))

    val relativeX = point.pixelX - calibration.originX
    val relativeY = point.pixelY - calibration.originY

    val xMeters = (relativeX * xAxisX + relativeY * xAxisY) * metrics.metersPerPixel
    val yMeters = (relativeX * yAxisX + relativeY * yAxisY) * metrics.metersPerPixel

    return CalibratedPoint(
        pointId = point.id,
        trackerId = point.trackerId,
        frameNumber = point.frameNumber,
        timestampUs = point.timestampUs,
        xMeters = xMeters,
        yMeters = yMeters
    )
}

private data class Vector2(
    val x: Double,
    val y: Double
)

fun positionAverageRadiusForStrength(strength: Float): Int =
    (strength.coerceIn(0f, 1f) * 4f).roundToInt().coerceIn(1, 4)

fun positionAverageWindowForStrength(strength: Float): Int =
    positionAverageRadiusForStrength(strength) * 2 + 1

private fun secondsBetween(startUs: Long, endUs: Long): Double? {
    val deltaUs = endUs - startUs
    return if (deltaUs > 0L) deltaUs / 1_000_000.0 else null
}

private fun velocityAt(points: List<CalibratedPoint>, index: Int): Vector2? {
    if (points.size < 2) return null
    val (from, to) = when (index) {
        0 -> points[0] to points[1]
        points.lastIndex -> points[points.lastIndex - 1] to points[points.lastIndex]
        else -> points[index - 1] to points[index + 1]
    }
    val dt = secondsBetween(from.timestampUs, to.timestampUs) ?: return null
    return Vector2(
        x = (to.xMeters - from.xMeters) / dt,
        y = (to.yMeters - from.yMeters) / dt
    )
}

private fun accelerationAt(
    timestampsUs: List<Long>,
    velocities: List<Vector2?>,
    index: Int
): Vector2? {
    if (velocities.size < 2) return null
    val (fromIndex, toIndex) = when (index) {
        0 -> 0 to 1
        velocities.lastIndex -> (velocities.lastIndex - 1) to velocities.lastIndex
        else -> (index - 1) to (index + 1)
    }
    val fromVelocity = velocities[fromIndex] ?: return null
    val toVelocity = velocities[toIndex] ?: return null
    val dt = secondsBetween(timestampsUs[fromIndex], timestampsUs[toIndex]) ?: return null
    return Vector2(
        x = (toVelocity.x - fromVelocity.x) / dt,
        y = (toVelocity.y - fromVelocity.y) / dt
    )
}

private fun averageCalibratedPositions(
    points: List<CalibratedPoint>,
    radius: Int
): List<CalibratedPoint> {
    if (points.size < 3 || radius <= 0) return points
    return points.indices.map { index ->
        var weightedX = 0.0
        var weightedY = 0.0
        var totalWeight = 0.0
        val start = (index - radius).coerceAtLeast(0)
        val end = (index + radius).coerceAtMost(points.lastIndex)
        for (sampleIndex in start..end) {
            val distance = kotlin.math.abs(sampleIndex - index)
            val kernelWeight = (radius + 1 - distance).toDouble()
            val sample = points[sampleIndex]
            weightedX += sample.xMeters * kernelWeight
            weightedY += sample.yMeters * kernelWeight
            totalWeight += kernelWeight
        }
        points[index].copy(
            xMeters = weightedX / totalWeight,
            yMeters = weightedY / totalWeight
        )
    }
}

/**
 * Calculates a derived kinematics series without modifying persisted observations.
 *
 * A non-zero [positionAverageRadius] applies a centered triangular average to positions
 * before differentiation. Tracks and discontinuous frame segments are processed
 * independently so an average or derivative never bridges identities or missing frames.
 */
fun computeKinematics(
    points: List<CalibratedPoint>,
    positionAverageRadius: Int = 0
): List<KinematicsPoint> =
    points.groupBy { it.trackerId }
        .values
        .flatMap { track ->
            splitContinuousSegments(track.sortedBy { it.timestampUs })
                .flatMap { segment -> computeSingleTrackKinematics(segment, positionAverageRadius) }
        }
        .sortedWith(compareBy<KinematicsPoint> { it.timestampUs }.thenBy { it.trackerId })

private fun splitContinuousSegments(points: List<CalibratedPoint>): List<List<CalibratedPoint>> {
    if (points.isEmpty()) return emptyList()
    val segments = mutableListOf<MutableList<CalibratedPoint>>()
    points.forEach { point ->
        val current = segments.lastOrNull()
        val previous = current?.lastOrNull()
        val isContinuous = previous != null &&
            point.frameNumber == previous.frameNumber + 1 &&
            point.timestampUs > previous.timestampUs
        if (current == null || !isContinuous) {
            segments += mutableListOf(point)
        } else {
            current += point
        }
    }
    return segments
}

private fun computeSingleTrackKinematics(
    points: List<CalibratedPoint>,
    positionAverageRadius: Int
): List<KinematicsPoint> {
    if (points.isEmpty()) return emptyList()
    val derivedPositions = averageCalibratedPositions(points, positionAverageRadius)
    val velocities = derivedPositions.indices.map { index -> velocityAt(derivedPositions, index) }
    val timestamps = derivedPositions.map { it.timestampUs }
    return derivedPositions.mapIndexed { index, point ->
        val velocity = velocities[index]
        val acceleration = accelerationAt(timestamps, velocities, index)
        KinematicsPoint(
            pointId = point.pointId,
            trackerId = point.trackerId,
            frameNumber = point.frameNumber,
            timestampUs = point.timestampUs,
            xMeters = point.xMeters,
            yMeters = point.yMeters,
            vxMetersPerSec = velocity?.x,
            vyMetersPerSec = velocity?.y,
            speedMetersPerSec = velocity?.let { hypot(it.x, it.y) },
            axMetersPerSec2 = acceleration?.x,
            ayMetersPerSec2 = acceleration?.y,
            accelerationMetersPerSec2 = acceleration?.let { hypot(it.x, it.y) }
        )
    }
}
