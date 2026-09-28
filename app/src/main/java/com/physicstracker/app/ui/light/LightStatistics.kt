package com.physicstracker.app.ui.light

import kotlin.math.sqrt

/** Descriptive statistics and an ordinary least-squares trend of the selected light samples. */
data class LightStatistics(
    val count: Int,
    val minimum: Double,
    val maximum: Double,
    val mean: Double,
    val standardDeviation: Double,
    val slope: Double?,
    val intercept: Double?,
    val rSquared: Double?
)

fun lightStatistics(samples: List<LightObservation>): LightStatistics? {
    val points = samples.filter { it.axis.isFinite() && it.value.isFinite() }
    if (points.isEmpty()) return null
    val n = points.size
    val meanX = points.sumOf { it.axis } / n
    val meanY = points.sumOf { it.value } / n
    val xx = points.sumOf { (it.axis - meanX) * (it.axis - meanX) }
    val xy = points.sumOf { (it.axis - meanX) * (it.value - meanY) }
    val yy = points.sumOf { (it.value - meanY) * (it.value - meanY) }
    val slope = if (n >= 2 && xx > 0.0) xy / xx else null
    val intercept = slope?.let { meanY - it * meanX }
    val rSquared = if (slope != null && yy > 0.0) (xy * xy / (xx * yy)).coerceIn(0.0, 1.0) else null
    return LightStatistics(n, points.minOf { it.value }, points.maxOf { it.value }, meanY,
        sqrt(yy / n), slope, intercept, rSquared)
}
