package com.physicstracker.app.ui.light

import androidx.compose.ui.geometry.Offset
import com.physicstracker.app.ui.LightGeometry
import com.physicstracker.app.ui.LightTool
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal enum class LightDragTarget { START, END, MOVE, NEW }

/** Hit testing is done in view pixels, while every edit remains in source pixels. */
internal fun lightDragTarget(geometry: LightGeometry, mapping: LightImageMapping, touch: Offset,
    handleRadiusPx: Float): LightDragTarget {
    val a = mapping.toView(geometry.start)
    val b = mapping.toView(geometry.end)
    val startDistance = (touch - a).getDistance()
    val endDistance = (touch - b).getDistance()
    if (geometry.tool == LightTool.POINT) return if (startDistance <= handleRadiusPx) LightDragTarget.START else LightDragTarget.NEW
    if (startDistance <= handleRadiusPx || endDistance <= handleRadiusPx) {
        return if (startDistance <= endDistance) LightDragTarget.START else LightDragTarget.END
    }
    val inside = if (geometry.tool == LightTool.BOX) {
        touch.x in min(a.x, b.x)..max(a.x, b.x) && touch.y in min(a.y, b.y)..max(a.y, b.y)
    } else {
        val lengthSquared = (b - a).getDistanceSquared()
        val t = if (lengthSquared == 0f) 0f else
            (((touch.x - a.x) * (b.x - a.x) + (touch.y - a.y) * (b.y - a.y)) / lengthSquared).coerceIn(0f, 1f)
        (touch - (a + (b - a) * t)).getDistance() <= handleRadiusPx
    }
    return if (inside) LightDragTarget.MOVE else LightDragTarget.NEW
}

internal fun dragLightGeometry(original: LightGeometry, target: LightDragTarget, origin: Offset,
    current: Offset, width: Int, height: Int): LightGeometry {
    val result = when (target) {
        LightDragTarget.START -> original.copy(start = current)
        LightDragTarget.END -> original.copy(end = current)
        LightDragTarget.NEW -> original.copy(start = origin, end = if (original.tool == LightTool.POINT) origin else current)
        LightDragTarget.MOVE -> {
            val delta = current - origin
            val minX = min(original.start.x, original.end.x)
            val maxX = max(original.start.x, original.end.x)
            val minY = min(original.start.y, original.end.y)
            val maxY = max(original.start.y, original.end.y)
            val dx = delta.x.coerceIn(-minX, width - 1f - maxX)
            val dy = delta.y.coerceIn(-minY, height - 1f - maxY)
            original.copy(start = original.start + Offset(dx, dy), end = original.end + Offset(dx, dy))
        }
    }
    return canonicalLightGeometry(result, width, height)
}

internal fun LightImageMapping.toSourceClamped(point: Offset): Offset? {
    if (scale <= 0f || !scale.isFinite()) return null
    return Offset(((point.x - left) / scale).toInt().coerceIn(0, max(0, width - 1)).toFloat(),
        ((point.y - top) / scale).toInt().coerceIn(0, max(0, height - 1)).toFloat())
}
