package com.physicstracker.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Full-video guides: solid positive directions, dashed negative directions. */
internal fun DrawScope.drawCalibrationAxisGuides(origin: Offset, angle: Double, viewport: PixelViewport, density: Float) {
    val length = hypot(size.width, size.height) * 2f
    clipRect(viewport.left, viewport.top, viewport.left + viewport.width, viewport.top + viewport.height) {
        listOf(angle to Color(0xFF80DEEA), (angle - 90) to Color(0xFFFFB74D)).forEach { (degrees, color) ->
            val radians = Math.toRadians(degrees)
            val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat()) * length
            drawLine(color.copy(alpha = 0.7f), origin, origin + direction, 1.5f * density)
            drawLine(color.copy(alpha = 0.5f), origin, origin - direction, 1.5f * density,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f * density, 5f * density)))
        }
    }
}
