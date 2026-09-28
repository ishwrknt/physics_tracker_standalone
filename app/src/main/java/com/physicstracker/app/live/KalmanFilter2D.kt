package com.physicstracker.app.live

/**
 * Constant-velocity Kalman filter for 2-D positions with ACTUAL timestamps (seconds).
 *
 * State: [x, y, vx, vy] in pixels and pixels/second. Used by the robust live tracker for
 * motion prediction and search-radius adaptation. Pure Kotlin: unit-testable.
 */
class KalmanFilter2D(
    initialX: Double,
    initialY: Double,
    private val processNoise: Double = 180.0 // pixel accel variance / s
) {
    // State
    var x: Double = initialX; private set
    var y: Double = initialY; private set
    var vx: Double = 0.0; private set
    var vy: Double = 0.0; private set

    // Covariance (row-major 4x4)
    private val p = doubleArrayOf(
        100.0, 0.0, 0.0, 0.0,
        0.0, 100.0, 0.0, 0.0,
        0.0, 0.0, 400.0, 0.0,
        0.0, 0.0, 0.0, 400.0
    )

    /** Scalar uncertainty of the predicted position (px, ~1 sigma). */
    val positionUncertainty: Double
        get() = kotlin.math.sqrt((p[0] + p[5]) / 2.0)

    val speed: Double get() = kotlin.math.hypot(vx, vy)

    fun predict(dtSeconds: Double) {
        val dt = dtSeconds.coerceIn(0.0, 1.0)
        if (dt <= 0.0) return
        x += vx * dt
        y += vy * dt
        // P' = F P F^T + Q ; F = [[1,0,dt,0],[0,1,0,dt],[0,0,1,0],[0,0,0,1]]
        val px = p[0] + 2 * dt * p[8] + dt * dt * p[10] + processNoise * dt * dt / 4
        val py = p[5] + 2 * dt * p[13] + dt * dt * p[15] + processNoise * dt * dt / 4
        val pxy_v1 = p[8] + dt * p[10]
        val pxy_v2 = p[13] + dt * p[15]
        val pvx = p[10] + processNoise * dt
        val pvy = p[15] + processNoise * dt
        p[0] = px; p[5] = py; p[8] = pxy_v1; p[13] = pxy_v2; p[10] = pvx; p[15] = pvy
        p[2] = p[8]; p[7] = p[13]
        // symmetric off-diagonals kept simple; cross x/y terms stay zero in this model
    }

    /** Innovation (measurement - prediction) of the last update. */
    var lastInnovation: Double = 0.0
        private set

    fun update(measuredX: Double, measuredY: Double, measurementVariance: Double = 9.0) {
        val r = measurementVariance
        val ix = measuredX - x
        val iy = measuredY - y
        lastInnovation = kotlin.math.hypot(ix, iy)
        updateAxis(axis = 0, innovation = ix, r = r)
        updateAxis(axis = 1, innovation = iy, r = r)
    }

    private fun updateAxis(axis: Int, innovation: Double, r: Double) {
        val posIdx = if (axis == 0) 0 else 5
        val velIdx = if (axis == 0) 10 else 15
        val covIdx = if (axis == 0) 8 else 13
        val s = p[posIdx] + r
        if (s <= 1e-9) return
        val kPos = p[posIdx] / s
        val kVel = p[covIdx] / s
        if (axis == 0) { x += kPos * innovation; vx += kVel * innovation }
        else { y += kPos * innovation; vy += kVel * innovation }
        val newPosCov = (1 - kPos) * p[posIdx]
        val newCov = (1 - kPos) * p[covIdx]
        p[velIdx] = p[velIdx] - kVel * p[covIdx]
        p[posIdx] = newPosCov
        p[covIdx] = newCov
    }

    /** Remap position and velocity through a rigid frame transform (e.g. rotation). */
    fun remap(mapping: (Double, Double) -> Pair<Double, Double>) {
        val (nx, ny) = mapping(x, y)
        val (mx, my) = mapping(x + vx, y + vy)
        x = nx; y = ny; vx = mx - nx; vy = my - ny
    }
}
