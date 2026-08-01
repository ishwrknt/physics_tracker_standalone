package com.physicstracker.app.ui

import kotlin.math.max
import kotlin.math.min

/**
 * Authoritative mapping between decoded source-video pixels and a fit-center video viewport.
 *
 * Tracking/calibration state belongs in [SourcePoint]. [ViewPoint] is only an interaction and
 * rendering projection. Keeping this class free of Compose and Android types makes every
 * coordinate path deterministic and directly testable.
 */
data class SourcePoint(val x: Double, val y: Double)

data class ViewPoint(val x: Double, val y: Double)

data class ViewRect(
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double
)

enum class QuarterRotation(val degrees: Int) {
    R0(0),
    R90(90),
    R180(180),
    R270(270)
}

class SourceCoordinateTransform(
    private val sourceWidth: Int,
    private val sourceHeight: Int,
    private val viewWidth: Int,
    private val viewHeight: Int,
    private val rotation: QuarterRotation = QuarterRotation.R0
) {
    init {
        require(sourceWidth > 0 && sourceHeight > 0)
        require(viewWidth > 0 && viewHeight > 0)
    }

    private val rotatedWidth =
        if (rotation == QuarterRotation.R90 || rotation == QuarterRotation.R270) sourceHeight else sourceWidth
    private val rotatedHeight =
        if (rotation == QuarterRotation.R90 || rotation == QuarterRotation.R270) sourceWidth else sourceHeight

    val videoRect: ViewRect = run {
        val scale = min(viewWidth.toDouble() / rotatedWidth, viewHeight.toDouble() / rotatedHeight)
        val width = rotatedWidth * scale
        val height = rotatedHeight * scale
        ViewRect(
            left = (viewWidth - width) / 2.0,
            top = (viewHeight - height) / 2.0,
            width = width,
            height = height
        )
    }

    fun sourceToView(source: SourcePoint): ViewPoint {
        val bounded = clampSource(source)
        val rotated = rotateSource(bounded)
        return ViewPoint(
            x = videoRect.left + rotated.x / max(1.0, rotatedWidth - 1.0) * videoRect.width,
            y = videoRect.top + rotated.y / max(1.0, rotatedHeight - 1.0) * videoRect.height
        )
    }

    fun viewToSource(view: ViewPoint, clamp: Boolean = true): SourcePoint? {
        if (!clamp && (
                view.x < videoRect.left || view.x > videoRect.left + videoRect.width ||
                    view.y < videoRect.top || view.y > videoRect.top + videoRect.height
                )
        ) return null
        val viewX = view.x.coerceIn(videoRect.left, videoRect.left + videoRect.width)
        val viewY = view.y.coerceIn(videoRect.top, videoRect.top + videoRect.height)
        val rotated = SourcePoint(
            x = (viewX - videoRect.left) / max(1.0, videoRect.width) * max(1.0, rotatedWidth - 1.0),
            y = (viewY - videoRect.top) / max(1.0, videoRect.height) * max(1.0, rotatedHeight - 1.0)
        )
        return clampSource(unrotateSource(rotated))
    }

    fun nudge(source: SourcePoint, deltaX: Double, deltaY: Double): SourcePoint =
        clampSource(SourcePoint(source.x + deltaX, source.y + deltaY))

    fun clampSource(source: SourcePoint): SourcePoint = SourcePoint(
        x = source.x.coerceIn(0.0, max(0.0, sourceWidth - 1.0)),
        y = source.y.coerceIn(0.0, max(0.0, sourceHeight - 1.0))
    )

    private fun rotateSource(source: SourcePoint): SourcePoint = when (rotation) {
        QuarterRotation.R0 -> source
        QuarterRotation.R90 -> SourcePoint(sourceHeight - 1.0 - source.y, source.x)
        QuarterRotation.R180 -> SourcePoint(sourceWidth - 1.0 - source.x, sourceHeight - 1.0 - source.y)
        QuarterRotation.R270 -> SourcePoint(source.y, sourceWidth - 1.0 - source.x)
    }

    private fun unrotateSource(rotated: SourcePoint): SourcePoint = when (rotation) {
        QuarterRotation.R0 -> rotated
        QuarterRotation.R90 -> SourcePoint(rotated.y, sourceHeight - 1.0 - rotated.x)
        QuarterRotation.R180 -> SourcePoint(sourceWidth - 1.0 - rotated.x, sourceHeight - 1.0 - rotated.y)
        QuarterRotation.R270 -> SourcePoint(sourceWidth - 1.0 - rotated.y, rotated.x)
    }
}
