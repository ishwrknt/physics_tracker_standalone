package com.physicstracker.app.ui

import androidx.compose.ui.geometry.Offset
import com.physicstracker.app.data.CalibrationEntity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** View-only adapter. Persisted calibration and drafts always remain in source pixels. */
internal class CalibrationViewport(
    private val width: Int,
    private val height: Int,
    private val rotation: Int,
    private val viewport: PixelViewport
) {
    fun toView(value: CalibrationEntity): CalibrationEntity = map(value, false)
    fun toSource(value: CalibrationEntity): CalibrationEntity = map(value, true)

    fun centreZero(value: CalibrationEntity): CalibrationEntity {
        if (width <= 0 || height <= 0) return value
        return if (value.coordinateSpace == "SOURCE_VIDEO") value.copy(
            originX = (width - 1) / 2.0, originY = (height - 1) / 2.0
        ) else value.copy(
            originX = (viewport.left + viewport.width / 2f).toDouble(),
            originY = (viewport.top + viewport.height / 2f).toDouble()
        )
    }

    /** Horizontal +X and upward +Y in the displayed video, including metadata rotation. */
    fun resetDirection(value: CalibrationEntity): CalibrationEntity =
        applyViewEdit(toView(value).copy(rotationDegrees = 0.0), value)

    fun applyViewEdit(value: CalibrationEntity, original: CalibrationEntity?): CalibrationEntity {
        val mapped = toSource(value)
        if (original == null) return mapped
        val previous = toView(original)
        // Do not round-trip untouched measurements: rotating/moving axes cannot change scale.
        return mapped.copy(
            stickP1x = if (value.stickP1x == previous.stickP1x && value.stickP1y == previous.stickP1y) original.stickP1x else mapped.stickP1x,
            stickP1y = if (value.stickP1x == previous.stickP1x && value.stickP1y == previous.stickP1y) original.stickP1y else mapped.stickP1y,
            stickP2x = if (value.stickP2x == previous.stickP2x && value.stickP2y == previous.stickP2y) original.stickP2x else mapped.stickP2x,
            stickP2y = if (value.stickP2x == previous.stickP2x && value.stickP2y == previous.stickP2y) original.stickP2y else mapped.stickP2y,
            originX = if (value.originX == previous.originX && value.originY == previous.originY) original.originX else mapped.originX,
            originY = if (value.originX == previous.originX && value.originY == previous.originY) original.originY else mapped.originY,
            rotationDegrees = if (value.rotationDegrees == previous.rotationDegrees) original.rotationDegrees else mapped.rotationDegrees
        )
    }

    private fun map(value: CalibrationEntity, inverse: Boolean): CalibrationEntity {
        if (value.coordinateSpace != "SOURCE_VIDEO" || width <= 0 || height <= 0 ||
            viewport.width <= 0 || viewport.height <= 0) return value
        fun point(x: Double, y: Double): Offset {
            val p = Offset(x.toFloat(), y.toFloat())
            return if (inverse) viewportPointToSourcePixel(p, width, height, rotation, viewport)
            else sourcePixelToViewport(p, width, height, rotation, viewport)
        }
        val start = point(value.stickP1x, value.stickP1y)
        val end = point(value.stickP2x, value.stickP2y)
        val origin = point(value.originX, value.originY)
        val radians = Math.toRadians(value.rotationDegrees)
        // A long direction vector avoids float cancellation near large source coordinates.
        val direction = point(value.originX + 100 * cos(radians), value.originY + 100 * sin(radians)) - origin
        return value.copy(
            stickP1x = start.x.toDouble(), stickP1y = start.y.toDouble(),
            stickP2x = end.x.toDouble(), stickP2y = end.y.toDouble(),
            originX = origin.x.toDouble(), originY = origin.y.toDouble(),
            rotationDegrees = Math.toDegrees(atan2(direction.y.toDouble(), direction.x.toDouble()))
        )
    }
}
