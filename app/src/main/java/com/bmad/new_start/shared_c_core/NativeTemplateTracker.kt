package com.bmad.new_start.shared_c_core

import kotlin.math.max
import kotlin.math.min

object NativeTemplateTracker {
    val isNativeLoaded: Boolean = true

    fun localizedSsdMatchNative(
        framePixels: IntArray,
        frameWidth: Int,
        frameHeight: Int,
        templatePixels: IntArray,
        templateWidth: Int,
        templateHeight: Int,
        predictedCenterX: Int,
        predictedCenterY: Int,
        searchRadiusPx: Int
    ): FloatArray {
        if (frameWidth <= 0 || frameHeight <= 0 || templateWidth <= 0 || templateHeight <= 0) {
            return floatArrayOf(predictedCenterX.toFloat(), predictedCenterY.toFloat(), Float.MAX_VALUE)
        }
        if (framePixels.size < frameWidth * frameHeight || templatePixels.size < templateWidth * templateHeight) {
            return floatArrayOf(predictedCenterX.toFloat(), predictedCenterY.toFloat(), Float.MAX_VALUE)
        }

        val halfWidth = templateWidth / 2
        val halfHeight = templateHeight / 2
        val minCenterX = max(halfWidth, predictedCenterX - searchRadiusPx)
        val maxCenterX = min(frameWidth - halfWidth - 1, predictedCenterX + searchRadiusPx)
        val minCenterY = max(halfHeight, predictedCenterY - searchRadiusPx)
        val maxCenterY = min(frameHeight - halfHeight - 1, predictedCenterY + searchRadiusPx)
        if (minCenterX > maxCenterX || minCenterY > maxCenterY) {
            return floatArrayOf(predictedCenterX.toFloat(), predictedCenterY.toFloat(), Float.MAX_VALUE)
        }

        var bestScore = Double.MAX_VALUE
        var bestX = predictedCenterX.toFloat()
        var bestY = predictedCenterY.toFloat()

        var centerY = minCenterY
        while (centerY <= maxCenterY) {
            var centerX = minCenterX
            while (centerX <= maxCenterX) {
                var totalDiff = 0.0
                var samples = 0
                var y = 0
                while (y < templateHeight) {
                    val frameRow = (centerY - halfHeight + y) * frameWidth
                    val templateRow = y * templateWidth
                    var x = 0
                    while (x < templateWidth) {
                        val frameColor = framePixels[frameRow + (centerX - halfWidth + x)]
                        val templateColor = templatePixels[templateRow + x]
                        totalDiff += kotlin.math.abs(luma(frameColor) - luma(templateColor)).toDouble()
                        samples += 1
                        x += 2
                    }
                    y += 2
                }
                val score = totalDiff / samples.coerceAtLeast(1)
                if (score < bestScore) {
                    bestScore = score
                    bestX = centerX.toFloat()
                    bestY = centerY.toFloat()
                }
                centerX += 2
            }
            centerY += 2
        }

        return floatArrayOf(bestX, bestY, bestScore.toFloat())
    }

    private fun luma(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (r * 30 + g * 59 + b * 11) / 100
    }
}
