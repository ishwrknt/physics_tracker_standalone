package com.physicstracker.app.live

/**
 * Shared, pure coordinate mapping between the raw sensor frame and the upright analysis
 * frame produced by [YuvBitmapConverter]. Used by the converter, the tracker, and the
 * ViewModel so rotation handling can never diverge between pipeline stages.
 */
object FrameRotation {

    fun uprightWidth(srcW: Int, srcH: Int, rotationDeg: Int): Int =
        if (rotated90ish(rotationDeg)) srcH else srcW

    fun uprightHeight(srcW: Int, srcH: Int, rotationDeg: Int): Int =
        if (rotated90ish(rotationDeg)) srcW else srcH

    private fun rotated90ish(rotationDeg: Int): Boolean =
        (((rotationDeg % 360) + 360) % 360) == 90 || (((rotationDeg % 360) + 360) % 360) == 270

    /** Map an upright-frame point back to raw sensor-frame coordinates. */
    fun uprightToSource(ux: Int, uy: Int, rotationDeg: Int, srcW: Int, srcH: Int): Pair<Int, Int> {
        val r = ((rotationDeg % 360) + 360) % 360
        val upW = uprightWidth(srcW, srcH, rotationDeg)
        val upH = uprightHeight(srcW, srcH, rotationDeg)
        return when (r) {
            90 -> uy to (upW - 1 - ux)
            180 -> (srcW - 1 - ux) to (srcH - 1 - uy)
            270 -> (upH - 1 - uy) to ux
            else -> ux to uy
        }
    }

    /** Map a raw sensor-frame point to upright-frame coordinates. */
    fun sourceToUpright(sx: Int, sy: Int, rotationDeg: Int, srcW: Int, srcH: Int): Pair<Int, Int> {
        val r = ((rotationDeg % 360) + 360) % 360
        return when (r) {
            90 -> (srcH - 1 - sy) to sx
            180 -> (srcW - 1 - sx) to (srcH - 1 - sy)
            270 -> sy to (srcW - 1 - sx)
            else -> sx to sy
        }
    }

    /**
     * Remap a position from the upright frame of [oldRotationDeg] into the upright frame
     * of [newRotationDeg] for the same physical sensor point. Source dimensions unchanged.
     */
    fun remapUpright(
        ux: Double, uy: Double,
        oldRotationDeg: Int, newRotationDeg: Int,
        srcW: Int, srcH: Int
    ): Pair<Double, Double> {
        val (sx, sy) = uprightToSource(ux.toInt(), uy.toInt(), oldRotationDeg, srcW, srcH)
        val (nx, ny) = sourceToUpright(sx, sy, newRotationDeg, srcW, srcH)
        return nx.toDouble() to ny.toDouble()
    }
}
