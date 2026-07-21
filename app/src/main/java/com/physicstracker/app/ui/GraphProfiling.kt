package com.physicstracker.app.ui

import android.graphics.Bitmap
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.pow

enum class GraphProfileMode {
    NONE,
    AUTO,
    LINEAR,
    POLY_2,
    POLY_3,
    POLY_4,
    POLY_5,
    EXPONENTIAL,
    LOGARITHMIC,
    POWER_LAW,
    GAUSSIAN,
    LORENTZIAN,
    SINE,
    DAMPED_SINE,
    DAMPED_COSINE,
    DAMPED_HARMONIC,
    MULTI_SINE,
    CHIRP,
    CUSTOM
}

enum class GraphDisplayMode {
    ORIGINAL,
    CURATED,
    OVERLAY,
    BLEND
}

enum class GraphLayoutMode {
    OVERLAY,
    STACKED
}

data class GraphSample(
    val timeSeconds: Double,
    val value: Double
)

data class ProfileResult(
    val mode: GraphProfileMode,
    val label: String,
    val equation: String,
    val parameters: List<Pair<String, Double>>,
    val rmse: Double,
    val rSquared: Double,
    val mae: Double = 0.0,
    val reducedChiSq: Double = 0.0,
    val parameterUncertainties: List<Pair<String, Double>> = emptyList()
)

private fun mean(values: List<Double>): Double = if (values.isEmpty()) 0.0 else values.sum() / values.size

private fun rmse(samples: List<GraphSample>, predict: (Double) -> Double): Double {
    if (samples.isEmpty()) return 0.0
    val mse = samples.sumOf { sample ->
        val error = sample.value - predict(sample.timeSeconds)
        error * error
    } / samples.size
    return sqrt(mse)
}

private fun mae(samples: List<GraphSample>, predict: (Double) -> Double): Double {
    if (samples.isEmpty()) return 0.0
    return samples.sumOf { abs(it.value - predict(it.timeSeconds)) } / samples.size
}

private fun rSquared(samples: List<GraphSample>, predict: (Double) -> Double): Double {
    if (samples.size < 2) return 0.0
    val avg = mean(samples.map { it.value })
    val ssTot = samples.sumOf { (it.value - avg) * (it.value - avg) }
    if (ssTot == 0.0) return 1.0
    val ssRes = samples.sumOf {
        val error = it.value - predict(it.timeSeconds)
        error * error
    }
    return 1.0 - (ssRes / ssTot)
}

private fun reducedChiSq(samples: List<GraphSample>, degreesOfFreedom: Int, predict: (Double) -> Double): Double {
    if (degreesOfFreedom <= 0) return 0.0
    val ssRes = samples.sumOf {
        val error = it.value - predict(it.timeSeconds)
        error * error
    }
    // Assuming constant variance proportional to RSS / N for simple reduced chi-square representation
    return ssRes / degreesOfFreedom
}

private fun aicc(n: Int, k: Int, rss: Double): Double {
    if (n <= k + 1 || rss <= 0.0) return Double.MAX_VALUE
    val aic = n * ln(rss / n) + 2.0 * k
    val correction = (2.0 * k * (k + 1)) / (n - k - 1)
    return aic + correction
}

private fun solveLinear2(
    a11: Double, a12: Double,
    a21: Double, a22: Double,
    b1: Double, b2: Double
): Pair<Double, Double>? {
    val det = a11 * a22 - a12 * a21
    if (abs(det) < 1e-12) return null
    return Pair(
        (b1 * a22 - b2 * a12) / det,
        (a11 * b2 - a21 * b1) / det
    )
}

private fun solveLinear(matrix: Array<DoubleArray>, vector: DoubleArray): DoubleArray? {
    val n = vector.size
    val a = Array(n) { row -> DoubleArray(n + 1) { col -> if (col < n) matrix[row][col] else vector[row] } }
    for (pivot in 0 until n) {
        var maxRow = pivot
        for (row in pivot + 1 until n) {
            if (abs(a[row][pivot]) > abs(a[maxRow][pivot])) maxRow = row
        }
        if (abs(a[maxRow][pivot]) < 1e-12) return null
        val tmp = a[pivot]
        a[pivot] = a[maxRow]
        a[maxRow] = tmp
        val divisor = a[pivot][pivot]
        for (col in pivot until n + 1) a[pivot][col] /= divisor
        for (row in 0 until n) {
            if (row == pivot) continue
            val factor = a[row][pivot]
            for (col in pivot until n + 1) {
                a[row][col] -= factor * a[pivot][col]
            }
        }
    }
    return DoubleArray(n) { a[it][n] }
}

private fun estimateUncertainties(
    samples: List<GraphSample>,
    params: List<Double>,
    predict: (Double, List<Double>) -> Double
): List<Double> {
    val n = samples.size
    val m = params.size
    if (n <= m) return List(m) { 0.0 }
    val residuals = samples.map { it.value - predict(it.timeSeconds, params) }
    val sumSqRes = residuals.sumOf { it * it }
    val variance = sumSqRes / (n - m)
    
    val jacobian = Array(n) { DoubleArray(m) }
    val delta = 1e-5
    for (j in 0 until m) {
        val pPlus = params.toDoubleArray()
        val pMinus = params.toDoubleArray()
        pPlus[j] += delta
        pMinus[j] -= delta
        for (i in 0 until n) {
            jacobian[i][j] = (predict(samples[i].timeSeconds, pPlus.toList()) - predict(samples[i].timeSeconds, pMinus.toList())) / (2.0 * delta)
        }
    }
    
    val jTj = Array(m) { DoubleArray(m) }
    for (i in 0 until m) {
        for (j in 0 until m) {
            var sum = 0.0
            for (k in 0 until n) {
                sum += jacobian[k][i] * jacobian[k][j]
            }
            jTj[i][j] = sum
        }
    }
    
    // Add small regularization to avoid singularity
    for (i in 0 until m) {
        jTj[i][i] += 1e-9
    }
    
    val identity = Array(m) { r -> DoubleArray(m) { c -> if (r == c) 1.0 else 0.0 } }
    val jTjInv = Array(m) { DoubleArray(m) }
    for (i in 0 until m) {
        val col = solveLinear(jTj, identity[i]) ?: return List(m) { 0.0 }
        for (j in 0 until m) {
            jTjInv[j][i] = col[j]
        }
    }
    
    return List(m) { j ->
        val u = sqrt(max(0.0, jTjInv[j][j] * variance))
        if (u.isNaN() || u.isInfinite()) 0.0 else u
    }
}

private fun buildResult(
    mode: GraphProfileMode,
    label: String,
    equation: String,
    parameters: List<Pair<String, Double>>,
    samples: List<GraphSample>,
    predict: (Double) -> Double,
    predictWithParams: (Double, List<Double>) -> Double
): ProfileResult {
    val rmseVal = rmse(samples, predict)
    val r2Val = rSquared(samples, predict)
    val maeVal = mae(samples, predict)
    val dof = samples.size - parameters.size
    val redChi = reducedChiSq(samples, dof, predict)
    val paramVals = parameters.map { it.second }
    val uncertainties = estimateUncertainties(samples, paramVals, predictWithParams)
    val uncertaintiesList = parameters.indices.map { idx ->
        parameters[idx].first to uncertainties[idx]
    }
    return ProfileResult(
        mode = mode,
        label = label,
        equation = equation,
        parameters = parameters,
        rmse = rmseVal,
        rSquared = r2Val,
        mae = maeVal,
        reducedChiSq = redChi,
        parameterUncertainties = uncertaintiesList
    )
}

private fun fitLinear(samples: List<GraphSample>): ProfileResult? {
    if (samples.size < 2) return null
    val sx = samples.sumOf { it.timeSeconds }
    val sy = samples.sumOf { it.value }
    val sxx = samples.sumOf { it.timeSeconds * it.timeSeconds }
    val sxy = samples.sumOf { it.timeSeconds * it.value }
    val n = samples.size.toDouble()
    val solution = solveLinear2(sxx, sx, sx, n, sxy, sy) ?: return null
    val a = solution.first
    val b = solution.second
    val predict = { x: Double -> a * x + b }
    val predictWithParams = { x: Double, p: List<Double> -> p[0] * x + p[1] }
    return buildResult(
        mode = GraphProfileMode.LINEAR,
        label = "Linear",
        equation = "y = ${"%.4f".format(a)}t + ${"%.4f".format(b)}",
        parameters = listOf("slope" to a, "intercept" to b),
        samples = samples,
        predict = predict,
        predictWithParams = predictWithParams
    )
}

private fun fitPolynomial(samples: List<GraphSample>, degree: Int): ProfileResult? {
    if (samples.size < degree + 1) return null
    val tMean = mean(samples.map { it.timeSeconds })
    val centered = samples.map { GraphSample(it.timeSeconds - tMean, it.value) }
    
    val mSize = degree + 1
    val matrix = Array(mSize) { DoubleArray(mSize) }
    val vector = DoubleArray(mSize)
    
    for (row in 0 until mSize) {
        for (col in 0 until mSize) {
            val power = (degree - row) + (degree - col)
            matrix[row][col] = centered.sumOf { it.timeSeconds.pow(power) }
        }
        val targetPower = degree - row
        vector[row] = centered.sumOf { it.timeSeconds.pow(targetPower) * it.value }
    }
    
    val coeffs = solveLinear(matrix, vector) ?: return null
    
    val predict = { x: Double ->
        val xc = x - tMean
        var sum = 0.0
        for (i in 0 until mSize) {
            sum += coeffs[i] * xc.pow(degree - i)
        }
        sum
    }
    
    val predictWithParams = { x: Double, p: List<Double> ->
        val xc = x - tMean
        var sum = 0.0
        for (i in 0 until mSize) {
            sum += p[i] * xc.pow(degree - i)
        }
        sum
    }
    
    val equationParts = coeffs.indices.map { idx ->
        val power = degree - idx
        val coeffStr = "%.4f".format(coeffs[idx])
        when (power) {
            0 -> coeffStr
            1 -> "${coeffStr}t"
            else -> "${coeffStr}t^$power"
        }
    }
    val equation = "y = " + equationParts.joinToString(" + ")
    val parameters = coeffs.indices.map { "c$it" to coeffs[it] }
    val mode = when (degree) {
        2 -> GraphProfileMode.POLY_2
        3 -> GraphProfileMode.POLY_3
        4 -> GraphProfileMode.POLY_4
        else -> GraphProfileMode.POLY_5
    }
    
    val baseResult = buildResult(
        mode = mode,
        label = "Polynomial (Degree $degree)",
        equation = equation,
        parameters = parameters,
        samples = samples,
        predict = predict,
        predictWithParams = predictWithParams
    )
    return baseResult.copy(parameters = baseResult.parameters + listOf("tMean" to tMean))
}

private fun fitExponential(samples: List<GraphSample>): ProfileResult? {
    if (samples.size < 2) return null
    val shifted = samples.map { it.value }
    val minValue = shifted.minOrNull() ?: return null
    val maxValue = shifted.maxOrNull() ?: return null
    val valueRange = maxValue - minValue
    if (valueRange < 1e-9) return null
    val offset = if (minValue <= 0.0) abs(minValue) + valueRange * 0.01 else 0.0
    val transformed = samples.mapNotNull {
        val v = it.value + offset
        if (v > 0) GraphSample(it.timeSeconds, ln(v)) else null
    }
    if (transformed.size < samples.size * 0.8) return null
    val linear = fitLinear(transformed) ?: return null
    val bCoeff = linear.parameters.first { it.first == "slope" }.second
    val tRange = (samples.maxOf { it.timeSeconds } - samples.minOf { it.timeSeconds }).coerceAtLeast(1e-6)
    if (abs(bCoeff * tRange) > 50.0) return null
    val lnA = linear.parameters.first { it.first == "intercept" }.second
    val aCoeff = exp(lnA)
    val predict = { x: Double -> aCoeff * exp(bCoeff * x) - offset }
    val predictWithParams = { x: Double, p: List<Double> -> p[0] * exp(p[1] * x) - p[2] }
    return buildResult(
        mode = GraphProfileMode.EXPONENTIAL,
        label = "Exponential",
        equation = "y = ${"%.4f".format(aCoeff)}e^(${"%.4f".format(bCoeff)}t) - ${"%.4f".format(offset)}",
        parameters = listOf("a" to aCoeff, "b" to bCoeff, "offset" to offset),
        samples = samples,
        predict = predict,
        predictWithParams = predictWithParams
    )
}

private fun fitLogarithmic(samples: List<GraphSample>): ProfileResult? {
    if (samples.size < 2) return null
    val minT = samples.minOf { it.timeSeconds }
    val tOffset = if (minT <= 0.0) abs(minT) + 0.01 else 0.0
    val transformed = samples.mapNotNull {
        val t = it.timeSeconds + tOffset
        if (t > 0) GraphSample(ln(t), it.value) else null
    }
    if (transformed.size < samples.size * 0.8) return null
    val linear = fitLinear(transformed) ?: return null
    val a = linear.parameters.first { it.first == "slope" }.second
    val b = linear.parameters.first { it.first == "intercept" }.second
    val predict = { x: Double -> a * ln(x + tOffset) + b }
    val predictWithParams = { x: Double, p: List<Double> -> p[0] * ln(x + p[2]) + p[1] }
    return buildResult(
        mode = GraphProfileMode.LOGARITHMIC,
        label = "Logarithmic",
        equation = "y = ${"%.4f".format(a)}ln(t + ${"%.4f".format(tOffset)}) + ${"%.4f".format(b)}",
        parameters = listOf("a" to a, "b" to b, "tOffset" to tOffset),
        samples = samples,
        predict = predict,
        predictWithParams = predictWithParams
    )
}

private fun fitPowerLaw(samples: List<GraphSample>): ProfileResult? {
    if (samples.size < 2) return null
    val minT = samples.minOf { it.timeSeconds }
    val tOffset = if (minT <= 0.0) abs(minT) + 0.01 else 0.0
    val minValue = samples.minOf { it.value }
    val yOffset = if (minValue <= 0.0) abs(minValue) + 0.01 else 0.0
    val transformed = samples.mapNotNull {
        val t = it.timeSeconds + tOffset
        val y = it.value + yOffset
        if (t > 0 && y > 0) GraphSample(ln(t), ln(y)) else null
    }
    if (transformed.size < samples.size * 0.8) return null
    val linear = fitLinear(transformed) ?: return null
    val b = linear.parameters.first { it.first == "slope" }.second
    val lnA = linear.parameters.first { it.first == "intercept" }.second
    val a = exp(lnA)
    val predict = { x: Double -> a * (x + tOffset).pow(b) - yOffset }
    val predictWithParams = { x: Double, p: List<Double> -> p[0] * (x + p[2]).pow(p[1]) - p[3] }
    return buildResult(
        mode = GraphProfileMode.POWER_LAW,
        label = "Power Law",
        equation = "y = ${"%.4f".format(a)}(t + ${"%.4f".format(tOffset)})^(${"%.4f".format(b)}) - ${"%.4f".format(yOffset)}",
        parameters = listOf("a" to a, "b" to b, "tOffset" to tOffset, "yOffset" to yOffset),
        samples = samples,
        predict = predict,
        predictWithParams = predictWithParams
    )
}

private fun fitGaussian(samples: List<GraphSample>): ProfileResult? {
    if (samples.size < 3) return null
    val tVals = samples.map { it.timeSeconds }
    val yVals = samples.map { it.value }
    val minT = tVals.minOrNull() ?: 0.0
    val maxT = tVals.maxOrNull() ?: 1.0
    val maxY = yVals.maxOrNull() ?: 1.0
    val meanY = mean(yVals)
    
    // Initial parameter search grid
    var bestRmse = Double.MAX_VALUE
    var bestParams = listOf(maxY, (minT + maxT) / 2.0, (maxT - minT) / 4.0, meanY)
    
    val tGrid = 15
    val sGrid = 5
    for (i in 0..tGrid) {
        val center = minT + (maxT - minT) * i / tGrid
        for (j in 1..sGrid) {
            val sigma = (maxT - minT) * j / (sGrid * 2.0)
            val p = listOf(maxY - meanY, center, sigma, meanY)
            val predict = { x: Double -> p[0] * exp(-0.5 * ((x - p[1]) / p[2]).pow(2)) + p[3] }
            val r = rmse(samples, predict)
            if (r < bestRmse) {
                bestRmse = r
                bestParams = p
            }
        }
    }
    
    val a = bestParams[0]
    val mu = bestParams[1]
    val sigma = bestParams[2]
    val d = bestParams[3]
    val predict = { x: Double -> a * exp(-0.5 * ((x - mu) / sigma).pow(2)) + d }
    val predictWithParams = { x: Double, p: List<Double> -> p[0] * exp(-0.5 * ((x - p[1]) / p[2]).pow(2)) + p[3] }
    
    return buildResult(
        mode = GraphProfileMode.GAUSSIAN,
        label = "Gaussian",
        equation = "y = ${"%.4f".format(a)}e^(-(t-${"%.4f".format(mu)})²/2(${ "%.4f".format(sigma) })²) + ${"%.4f".format(d)}",
        parameters = listOf("a" to a, "mean" to mu, "sigma" to sigma, "offset" to d),
        samples = samples,
        predict = predict,
        predictWithParams = predictWithParams
    )
}

private fun fitLorentzian(samples: List<GraphSample>): ProfileResult? {
    if (samples.size < 3) return null
    val tVals = samples.map { it.timeSeconds }
    val yVals = samples.map { it.value }
    val minT = tVals.minOrNull() ?: 0.0
    val maxT = tVals.maxOrNull() ?: 1.0
    val maxY = yVals.maxOrNull() ?: 1.0
    val meanY = mean(yVals)
    
    var bestRmse = Double.MAX_VALUE
    var bestParams = listOf(maxY, (minT + maxT) / 2.0, (maxT - minT) / 4.0, meanY)
    
    val tGrid = 15
    val wGrid = 5
    for (i in 0..tGrid) {
        val center = minT + (maxT - minT) * i / tGrid
        for (j in 1..wGrid) {
            val gamma = (maxT - minT) * j / (wGrid * 2.0)
            val p = listOf(maxY - meanY, center, gamma, meanY)
            val predict = { x: Double -> p[0] * (p[2].pow(2) / ((x - p[1]).pow(2) + p[2].pow(2))) + p[3] }
            val r = rmse(samples, predict)
            if (r < bestRmse) {
                bestRmse = r
                bestParams = p
            }
        }
    }
    
    val a = bestParams[0]
    val x0 = bestParams[1]
    val gamma = bestParams[2]
    val d = bestParams[3]
    val predict = { x: Double -> a * (gamma.pow(2) / ((x - x0).pow(2) + gamma.pow(2))) + d }
    val predictWithParams = { x: Double, p: List<Double> -> p[0] * (p[2].pow(2) / ((x - p[1]).pow(2) + p[2].pow(2))) + p[3] }
    
    return buildResult(
        mode = GraphProfileMode.LORENTZIAN,
        label = "Lorentzian",
        equation = "y = ${"%.4f".format(a)} / (1 + ((t-${"%.4f".format(x0)})/${"%.4f".format(gamma)})²) + ${"%.4f".format(d)}",
        parameters = listOf("a" to a, "center" to x0, "gamma" to gamma, "offset" to d),
        samples = samples,
        predict = predict,
        predictWithParams = predictWithParams
    )
}

private fun fitSineAtOmega(samples: List<GraphSample>, omega: Double): ProfileResult? {
    val sinVals = samples.map { sin(omega * it.timeSeconds) }
    val cosVals = samples.map { cos(omega * it.timeSeconds) }
    val s11 = sinVals.sumOf { it * it }
    val s12 = sinVals.zip(cosVals).sumOf { it.first * it.second }
    val s13 = sinVals.sum()
    val s22 = cosVals.sumOf { it * it }
    val s23 = cosVals.sum()
    val s33 = samples.size.toDouble()
    val b1 = samples.indices.sumOf { sinVals[it] * samples[it].value }
    val b2 = samples.indices.sumOf { cosVals[it] * samples[it].value }
    val b3 = samples.sumOf { it.value }
    val coeffs = solveLinear(
        arrayOf(
            doubleArrayOf(s11, s12, s13),
            doubleArrayOf(s12, s22, s23),
            doubleArrayOf(s13, s23, s33)
        ),
        doubleArrayOf(b1, b2, b3)
    ) ?: return null
    val bSin = coeffs[0]
    val cCos = coeffs[1]
    val offset = coeffs[2]
    val amplitude = sqrt(bSin * bSin + cCos * cCos)
    if (amplitude < 1e-12) return null
    val phase = atan2(cCos, bSin)
    val predict = { x: Double -> amplitude * sin(omega * x + phase) + offset }
    val predictWithParams = { x: Double, p: List<Double> -> p[0] * sin(p[1] * x + p[2]) + p[3] }
    return buildResult(
        mode = GraphProfileMode.SINE,
        label = "Sine",
        equation = "y = ${"%.4f".format(amplitude)}sin(${"%.4f".format(omega)}t + ${"%.4f".format(phase)}) + ${"%.4f".format(offset)}",
        parameters = listOf("amplitude" to amplitude, "omega" to omega, "phase" to phase, "offset" to offset),
        samples = samples,
        predict = predict,
        predictWithParams = predictWithParams
    )
}

private fun estimateOmegaFromZeroCrossings(samples: List<GraphSample>): Double? {
    if (samples.size < 4) return null
    val avg = mean(samples.map { it.value })
    val demeaned = samples.map { it.value - avg }
    val crossings = mutableListOf<Double>()
    for (i in 0 until demeaned.size - 1) {
        if (demeaned[i] * demeaned[i + 1] < 0) {
            val t0 = samples[i].timeSeconds
            val t1 = samples[i + 1].timeSeconds
            val v0 = demeaned[i]
            val v1 = demeaned[i + 1]
            val tCross = t0 - v0 * (t1 - t0) / (v1 - v0)
            crossings.add(tCross)
        }
    }
    if (crossings.size < 2) return null
    val halfPeriods = (1 until crossings.size).map { crossings[it] - crossings[it - 1] }
    val avgHalfPeriod = mean(halfPeriods)
    if (avgHalfPeriod < 1e-9) return null
    return PI / avgHalfPeriod
}

private fun estimateOmegaFromPeaks(samples: List<GraphSample>): Double? {
    if (samples.size < 5) return null
    val peaks = mutableListOf<Double>()
    for (i in 1 until samples.size - 1) {
        if (samples[i].value > samples[i - 1].value && samples[i].value > samples[i + 1].value) {
            peaks.add(samples[i].timeSeconds)
        }
    }
    if (peaks.size < 2) return null
    val periods = (1 until peaks.size).map { peaks[it] - peaks[it - 1] }
    val avgPeriod = mean(periods)
    if (avgPeriod < 1e-9) return null
    return 2.0 * PI / avgPeriod
}

private fun goldenSectionOmegaSearch(
    samples: List<GraphSample>,
    omegaLow: Double,
    omegaHigh: Double,
    iterations: Int
): ProfileResult? {
    val gr = (sqrt(5.0) + 1.0) / 2.0
    var a = omegaLow
    var b = omegaHigh
    var c = b - (b - a) / gr
    var d = a + (b - a) / gr
    for (i in 0 until iterations) {
        val fc = fitSineAtOmega(samples, c)?.rmse ?: Double.MAX_VALUE
        val fd = fitSineAtOmega(samples, d)?.rmse ?: Double.MAX_VALUE
        if (fc < fd) {
            b = d
        } else {
            a = c
        }
        c = b - (b - a) / gr
        d = a + (b - a) / gr
    }
    return fitSineAtOmega(samples, (a + b) / 2.0)
}

private fun fitSine(samples: List<GraphSample>): ProfileResult? {
    if (samples.size < 4) return null
    val minT = samples.minOf { it.timeSeconds }
    val maxT = samples.maxOf { it.timeSeconds }
    val span = max(maxT - minT, 1e-6)
    val avgDt = span / (samples.size - 1).coerceAtLeast(1)
    val minOmega = PI / (span * 2.0)
    val maxOmega = PI / max(avgDt, 1e-6)
    val logMin = ln(minOmega)
    val logMax = ln(maxOmega)
    
    val coarseSteps = 100
    data class Candidate(val omega: Double, val rmse: Double, val result: ProfileResult)
    val coarseCandidates = mutableListOf<Candidate>()
    for (i in 0 until coarseSteps) {
        val omega = exp(logMin + (logMax - logMin) * i / (coarseSteps - 1).coerceAtLeast(1))
        val result = fitSineAtOmega(samples, omega) ?: continue
        coarseCandidates.add(Candidate(omega, result.rmse, result))
    }
    
    val zcOmega = estimateOmegaFromZeroCrossings(samples)
    if (zcOmega != null) {
        val r = fitSineAtOmega(samples, zcOmega)
        if (r != null) coarseCandidates.add(Candidate(zcOmega, r.rmse, r))
    }
    
    if (coarseCandidates.isEmpty()) return null
    val bestCoarse = coarseCandidates.minByOrNull { it.rmse }!!
    
    val polished = goldenSectionOmegaSearch(samples, bestCoarse.omega * 0.9, bestCoarse.omega * 1.1, 20)
    return polished ?: bestCoarse.result
}

private fun fitDampedSine(samples: List<GraphSample>): ProfileResult? {
    if (samples.size < 5) return null
    val minT = samples.minOf { it.timeSeconds }
    val maxT = samples.maxOf { it.timeSeconds }
    val span = max(maxT - minT, 1e-6)
    
    val baseSine = fitSine(samples) ?: return null
    val omega = baseSine.parameters.firstOrNull { it.first == "omega" }?.second ?: 1.0
    
    var bestRmse = Double.MAX_VALUE
    var bestResult: ProfileResult? = null
    
    val lambdas = listOf(0.0, 0.01, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0)
    for (lambda in lambdas) {
        val transformed = samples.map {
            val decay = exp(-lambda * (it.timeSeconds - minT))
            GraphSample(it.timeSeconds, it.value / max(decay, 1e-5))
        }
        val sineFit = fitSineAtOmega(transformed, omega) ?: continue
        
        val amp = sineFit.parameters.first { it.first == "amplitude" }.second
        val phi = sineFit.parameters.first { it.first == "phase" }.second
        val offset = sineFit.parameters.first { it.first == "offset" }.second
        
        val predict = { x: Double ->
            amp * exp(-lambda * (x - minT)) * sin(omega * x + phi) + offset
        }
        
        val r = rmse(samples, predict)
        if (r < bestRmse) {
            bestRmse = r
            val predictWithParams = { x: Double, p: List<Double> ->
                p[0] * exp(-p[1] * (x - minT)) * sin(p[2] * x + p[3]) + p[4]
            }
            val baseResult = buildResult(
                mode = GraphProfileMode.DAMPED_SINE,
                label = "Damped Sine",
                equation = "y = ${"%.4f".format(amp)}e^(-${"%.4f".format(lambda)}t)sin(${"%.4f".format(omega)}t + ${"%.4f".format(phi)}) + ${"%.4f".format(offset)}",
                parameters = listOf("amplitude" to amp, "lambda" to lambda, "omega" to omega, "phase" to phi, "offset" to offset),
                samples = samples,
                predict = predict,
                predictWithParams = predictWithParams
            )
            bestResult = baseResult.copy(parameters = baseResult.parameters + listOf("tShift" to minT))
        }
    }
    return bestResult
}

private fun fitDampedCosine(samples: List<GraphSample>): ProfileResult? {
    if (samples.size < 5) return null
    val minT = samples.minOf { it.timeSeconds }
    val maxT = samples.maxOf { it.timeSeconds }
    val span = max(maxT - minT, 1e-6)
    
    val baseSine = fitSine(samples) ?: return null
    val omega = baseSine.parameters.firstOrNull { it.first == "omega" }?.second ?: 1.0
    
    var bestRmse = Double.MAX_VALUE
    var bestResult: ProfileResult? = null
    
    val lambdas = listOf(0.0, 0.01, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0)
    for (lambda in lambdas) {
        val transformed = samples.map {
            val decay = exp(-lambda * (it.timeSeconds - minT))
            GraphSample(it.timeSeconds, it.value / max(decay, 1e-5))
        }
        // y = A * cos(w*t + phi) + offset = A * sin(w*t + phi + PI/2) + offset
        val sineFit = fitSineAtOmega(transformed, omega) ?: continue
        
        val amp = sineFit.parameters.first { it.first == "amplitude" }.second
        val phiSine = sineFit.parameters.first { it.first == "phase" }.second
        val offset = sineFit.parameters.first { it.first == "offset" }.second
        val phi = phiSine - PI / 2.0
        
        val predict = { x: Double ->
            amp * exp(-lambda * (x - minT)) * cos(omega * x + phi) + offset
        }
        
        val r = rmse(samples, predict)
        if (r < bestRmse) {
            bestRmse = r
            val predictWithParams = { x: Double, p: List<Double> ->
                p[0] * exp(-p[1] * (x - minT)) * cos(p[2] * x + p[3]) + p[4]
            }
            val baseResult = buildResult(
                mode = GraphProfileMode.DAMPED_COSINE,
                label = "Damped Cosine",
                equation = "y = ${"%.4f".format(amp)}e^(-${"%.4f".format(lambda)}t)cos(${"%.4f".format(omega)}t + ${"%.4f".format(phi)}) + ${"%.4f".format(offset)}",
                parameters = listOf("amplitude" to amp, "lambda" to lambda, "omega" to omega, "phase" to phi, "offset" to offset),
                samples = samples,
                predict = predict,
                predictWithParams = predictWithParams
            )
            bestResult = baseResult.copy(parameters = baseResult.parameters + listOf("tShift" to minT))
        }
    }
    return bestResult
}

private fun fitDampedHarmonic(samples: List<GraphSample>): ProfileResult? {
    // Underdamped harmonic oscillator: y = A * e^(-beta*t) * cos(omega_d * t + phi) + offset
    // Let's reuse fitDampedCosine as it is physically identical to underdamped harmonic motion
    val dampedCos = fitDampedCosine(samples) ?: return null
    val amp = dampedCos.parameters.first { it.first == "amplitude" }.second
    val beta = dampedCos.parameters.first { it.first == "lambda" }.second
    val wd = dampedCos.parameters.first { it.first == "omega" }.second
    val phi = dampedCos.parameters.first { it.first == "phase" }.second
    val offset = dampedCos.parameters.first { it.first == "offset" }.second
    
    val predict = { x: Double -> amp * exp(-beta * x) * cos(wd * x + phi) + offset }
    val predictWithParams = { x: Double, p: List<Double> -> p[0] * exp(-p[1] * x) * cos(p[2] * x + p[3]) + p[4] }
    
    return buildResult(
        mode = GraphProfileMode.DAMPED_HARMONIC,
        label = "Damped Harmonic Oscillator",
        equation = "y = ${"%.4f".format(amp)}e^(-${"%.4f".format(beta)}t)cos(${"%.4f".format(wd)}t + ${"%.4f".format(phi)}) + ${"%.4f".format(offset)}",
        parameters = listOf("amplitude" to amp, "damping_beta" to beta, "omega_d" to wd, "phase" to phi, "offset" to offset),
        samples = samples,
        predict = predict,
        predictWithParams = predictWithParams
    )
}

private fun fitMultiSine(samples: List<GraphSample>): ProfileResult? {
    if (samples.size < 6) return null
    // Fit first dominant sine
    val firstSine = fitSine(samples) ?: return null
    val amp1 = firstSine.parameters.first { it.first == "amplitude" }.second
    val w1 = firstSine.parameters.first { it.first == "omega" }.second
    val phi1 = firstSine.parameters.first { it.first == "phase" }.second
    val offset = firstSine.parameters.first { it.first == "offset" }.second
    
    // Calculate residuals
    val residuals = samples.map {
        GraphSample(it.timeSeconds, it.value - (amp1 * sin(w1 * it.timeSeconds + phi1) + offset))
    }
    
    // Fit second sine to residuals
    val secondSine = fitSine(residuals) ?: return null
    val amp2 = secondSine.parameters.first { it.first == "amplitude" }.second
    val w2 = secondSine.parameters.first { it.first == "omega" }.second
    val phi2 = secondSine.parameters.first { it.first == "phase" }.second
    
    val predict = { x: Double ->
        amp1 * sin(w1 * x + phi1) + amp2 * sin(w2 * x + phi2) + offset
    }
    
    val predictWithParams = { x: Double, p: List<Double> ->
        p[0] * sin(p[1] * x + p[2]) + p[3] * sin(p[4] * x + p[5]) + p[6]
    }
    
    return buildResult(
        mode = GraphProfileMode.MULTI_SINE,
        label = "Multi-Frequency Sine",
        equation = "y = ${"%.4f".format(amp1)}sin(${"%.4f".format(w1)}t + ${"%.4f".format(phi1)}) + ${"%.4f".format(amp2)}sin(${"%.4f".format(w2)}t + ${"%.4f".format(phi2)}) + ${"%.4f".format(offset)}",
        parameters = listOf(
            "amplitude_1" to amp1, "omega_1" to w1, "phase_1" to phi1,
            "amplitude_2" to amp2, "omega_2" to w2, "phase_2" to phi2,
            "offset" to offset
        ),
        samples = samples,
        predict = predict,
        predictWithParams = predictWithParams
    )
}

private fun fitChirp(samples: List<GraphSample>): ProfileResult? {
    if (samples.size < 6) return null
    val minT = samples.minOf { it.timeSeconds }
    val maxT = samples.maxOf { it.timeSeconds }
    val span = max(maxT - minT, 1e-6)
    
    // Chirp: y = A * sin( (w0 + k*t)*t + phi ) + offset
    var bestRmse = Double.MAX_VALUE
    var bestResult: ProfileResult? = null
    
    // Coarse scan of sweep rate k and center freq w0
    val kGrid = listOf(-5.0, -2.0, -0.5, 0.0, 0.5, 2.0, 5.0)
    val baseSine = fitSine(samples) ?: return null
    val w0 = baseSine.parameters.first { it.first == "omega" }.second
    val amp = baseSine.parameters.first { it.first == "amplitude" }.second
    val phi = baseSine.parameters.first { it.first == "phase" }.second
    val offset = baseSine.parameters.first { it.first == "offset" }.second
    
    for (k in kGrid) {
        val predict = { x: Double ->
            val t = x - minT
            amp * sin((w0 + k * t) * x + phi) + offset
        }
        val r = rmse(samples, predict)
        if (r < bestRmse) {
            bestRmse = r
            val predictWithParams = { x: Double, p: List<Double> ->
                val t = x - minT
                p[0] * sin((p[1] + p[2] * t) * x + p[3]) + p[4]
            }
            bestResult = buildResult(
                mode = GraphProfileMode.CHIRP,
                label = "Chirp Sinusoidal Fit",
                equation = "y = ${"%.4f".format(amp)}sin((${"%.4f".format(w0)} + ${"%.4f".format(k)}t)t + ${"%.4f".format(phi)}) + ${"%.4f".format(offset)}",
                parameters = listOf("amplitude" to amp, "omega_0" to w0, "chirp_rate_k" to k, "phase" to phi, "offset" to offset),
                samples = samples,
                predict = predict,
                predictWithParams = predictWithParams
            )
        }
    }
    return bestResult
}

fun fitProfile(samples: List<GraphSample>, mode: GraphProfileMode): ProfileResult? {
    if (samples.size < 2 || mode == GraphProfileMode.NONE) return null
    if (mode == GraphProfileMode.AUTO) {
        val candidates = listOfNotNull(
            fitLinear(samples),
            fitPolynomial(samples, 2),
            fitPolynomial(samples, 3),
            fitExponential(samples),
            fitLogarithmic(samples),
            fitPowerLaw(samples),
            fitGaussian(samples),
            fitLorentzian(samples),
            fitSine(samples),
            fitDampedSine(samples),
            fitDampedCosine(samples),
            fitDampedHarmonic(samples),
            fitMultiSine(samples),
            fitChirp(samples)
        )
        if (candidates.isEmpty()) return null
        
        // Select candidate using AICc to penalize overfitting
        val n = samples.size
        return candidates.minByOrNull { result ->
            val k = result.parameters.size
            val rss = result.rmse * result.rmse * n
            aicc(n, k, rss)
        }
    }
    return when (mode) {
        GraphProfileMode.LINEAR -> fitLinear(samples)
        GraphProfileMode.POLY_2 -> fitPolynomial(samples, 2)
        GraphProfileMode.POLY_3 -> fitPolynomial(samples, 3)
        GraphProfileMode.POLY_4 -> fitPolynomial(samples, 4)
        GraphProfileMode.POLY_5 -> fitPolynomial(samples, 5)
        GraphProfileMode.EXPONENTIAL -> fitExponential(samples)
        GraphProfileMode.LOGARITHMIC -> fitLogarithmic(samples)
        GraphProfileMode.POWER_LAW -> fitPowerLaw(samples)
        GraphProfileMode.GAUSSIAN -> fitGaussian(samples)
        GraphProfileMode.LORENTZIAN -> fitLorentzian(samples)
        GraphProfileMode.SINE -> fitSine(samples)
        GraphProfileMode.DAMPED_SINE -> fitDampedSine(samples)
        GraphProfileMode.DAMPED_COSINE -> fitDampedCosine(samples)
        GraphProfileMode.DAMPED_HARMONIC -> fitDampedHarmonic(samples)
        GraphProfileMode.MULTI_SINE -> fitMultiSine(samples)
        GraphProfileMode.CHIRP -> fitChirp(samples)
        GraphProfileMode.CUSTOM -> null
        GraphProfileMode.NONE,
        GraphProfileMode.AUTO -> null
    }
}

fun evaluateProfile(result: ProfileResult, timeSeconds: Double): Double {
    val params = result.parameters.toMap()
    return when (result.mode) {
        GraphProfileMode.LINEAR -> (params["slope"] ?: 0.0) * timeSeconds + (params["intercept"] ?: 0.0)
        GraphProfileMode.POLY_2, GraphProfileMode.POLY_3, GraphProfileMode.POLY_4, GraphProfileMode.POLY_5 -> {
            val degree = when (result.mode) {
                GraphProfileMode.POLY_2 -> 2
                GraphProfileMode.POLY_3 -> 3
                GraphProfileMode.POLY_4 -> 4
                else -> 5
            }
            // Need to retrieve time mean to compute centered polynomial
            // If not found, fall back to simple uncentered evaluation if we store tMean or centered coeffs
            // We can reconstruct it from the parameters c0, c1, etc.
            // Wait, since we center during fit, our parameters are centered. Let's look at the equation or store centered parameters.
            // Actually, a simpler way is to evaluate using the parameters directly. Wait! The coefficients returned in fitPolynomial
            // were returned for the centered time. But parameters are named "c0", "c1", etc.
            // Wait, to make evaluation fully independent, let's store tMean in parameters or uncenter the coefficients.
            // Let's look at how we did it in fitPolynomial.
            // Ah! In fitPolynomial we defined:
            // val predict = { x: Double ->
            //    val xc = x - tMean
            //    var sum = 0.0
            //    for (i in 0 until mSize) {
            //        sum += coeffs[i] * xc.pow(degree - i)
            //    }
            //    sum
            // }
            // Let's make sure we uncenter the coefficients so that evaluateProfile works directly with absolute timeSeconds!
            // Wait, to do it cleanly without centering complex algebra, we can just store "tMean" as a parameter!
            val tMean = params["tMean"] ?: 0.0 // Let's check if we stored tMean. If not, we can save it.
            val xc = timeSeconds - tMean
            var sum = 0.0
            val mSize = degree + 1
            for (i in 0 until mSize) {
                val coeff = params["c$i"] ?: 0.0
                sum += coeff * xc.pow(degree - i)
            }
            sum
        }
        GraphProfileMode.EXPONENTIAL -> {
            val a = params["a"] ?: 0.0
            val b = params["b"] ?: 0.0
            val offset = params["offset"] ?: 0.0
            a * exp(b * timeSeconds) - offset
        }
        GraphProfileMode.LOGARITHMIC -> {
            val a = params["a"] ?: 0.0
            val b = params["b"] ?: 0.0
            val tOffset = params["tOffset"] ?: 0.0
            a * ln(timeSeconds + tOffset) + b
        }
        GraphProfileMode.POWER_LAW -> {
            val a = params["a"] ?: 0.0
            val b = params["b"] ?: 0.0
            val tOffset = params["tOffset"] ?: 0.0
            val yOffset = params["yOffset"] ?: 0.0
            a * (timeSeconds + tOffset).pow(b) - yOffset
        }
        GraphProfileMode.GAUSSIAN -> {
            val a = params["a"] ?: 0.0
            val mu = params["mean"] ?: 0.0
            val sigma = params["sigma"] ?: 1.0
            val d = params["offset"] ?: 0.0
            a * exp(-0.5 * ((timeSeconds - mu) / sigma).pow(2)) + d
        }
        GraphProfileMode.LORENTZIAN -> {
            val a = params["a"] ?: 0.0
            val x0 = params["center"] ?: 0.0
            val gamma = params["gamma"] ?: 1.0
            val d = params["offset"] ?: 0.0
            a * (gamma.pow(2) / ((timeSeconds - x0).pow(2) + gamma.pow(2))) + d
        }
        GraphProfileMode.SINE -> {
            val amplitude = params["amplitude"] ?: 0.0
            val omega = params["omega"] ?: 0.0
            val phase = params["phase"] ?: 0.0
            val offset = params["offset"] ?: 0.0
            amplitude * sin(omega * timeSeconds + phase) + offset
        }
        GraphProfileMode.DAMPED_SINE -> {
            val amplitude = params["amplitude"] ?: 0.0
            val lambda = params["lambda"] ?: 0.0
            val omega = params["omega"] ?: 0.0
            val phase = params["phase"] ?: 0.0
            val offset = params["offset"] ?: 0.0
            // Since we fit with decay e^(-lambda * (time - minT)) but minT is not explicitly a separate param
            // Wait, in buildResult for damped sine, minT is not in parameters. Let's make sure it is stored as "tShift"
            val tShift = params["tShift"] ?: 0.0
            amplitude * exp(-lambda * (timeSeconds - tShift)) * sin(omega * timeSeconds + phase) + offset
        }
        GraphProfileMode.DAMPED_COSINE -> {
            val amplitude = params["amplitude"] ?: 0.0
            val lambda = params["lambda"] ?: 0.0
            val omega = params["omega"] ?: 0.0
            val phase = params["phase"] ?: 0.0
            val offset = params["offset"] ?: 0.0
            val tShift = params["tShift"] ?: 0.0
            amplitude * exp(-lambda * (timeSeconds - tShift)) * cos(omega * timeSeconds + phase) + offset
        }
        GraphProfileMode.DAMPED_HARMONIC -> {
            val amplitude = params["amplitude"] ?: 0.0
            val beta = params["damping_beta"] ?: 0.0
            val wd = params["omega_d"] ?: 0.0
            val phase = params["phase"] ?: 0.0
            val offset = params["offset"] ?: 0.0
            amplitude * exp(-beta * timeSeconds) * cos(wd * timeSeconds + phase) + offset
        }
        GraphProfileMode.MULTI_SINE -> {
            val amp1 = params["amplitude_1"] ?: 0.0
            val w1 = params["omega_1"] ?: 0.0
            val phi1 = params["phase_1"] ?: 0.0
            val amp2 = params["amplitude_2"] ?: 0.0
            val w2 = params["omega_2"] ?: 0.0
            val phi2 = params["phase_2"] ?: 0.0
            val offset = params["offset"] ?: 0.0
            amp1 * sin(w1 * timeSeconds + phi1) + amp2 * sin(w2 * timeSeconds + phi2) + offset
        }
        GraphProfileMode.CHIRP -> {
            val amp = params["amplitude"] ?: 0.0
            val w0 = params["omega_0"] ?: 0.0
            val k = params["chirp_rate_k"] ?: 0.0
            val phi = params["phase"] ?: 0.0
            val offset = params["offset"] ?: 0.0
            amp * sin((w0 + k * timeSeconds) * timeSeconds + phi) + offset
        }
        GraphProfileMode.CUSTOM,
        GraphProfileMode.AUTO,
        GraphProfileMode.NONE -> 0.0
    }
}

fun curatedSamples(
    originalSamples: List<GraphSample>,
    profile: ProfileResult
): List<GraphSample> {
    return originalSamples.map { sample ->
        GraphSample(sample.timeSeconds, evaluateProfile(profile, sample.timeSeconds))
    }
}

fun blendedSamples(
    originalSamples: List<GraphSample>,
    curatedSamples: List<GraphSample>,
    alpha: Float
): List<GraphSample> {
    val a = alpha.toDouble().coerceIn(0.0, 1.0)
    return originalSamples.zip(curatedSamples).map { (orig, cur) ->
        GraphSample(orig.timeSeconds, (1.0 - a) * orig.value + a * cur.value)
    }
}
