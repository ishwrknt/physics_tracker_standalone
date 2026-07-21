package com.physicstracker.app.tracking

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

data class DeepTrackingState(
    val targetClassIndex: Int,
    val center: Offset,
    val velocity: Offset = Offset.Zero,
    val frameWidth: Int,
    val frameHeight: Int
)

data class DeepTrackingMatch(
    val state: DeepTrackingState,
    val confidence: Double
)

class DeepSegmentationTracker(
    context: Context,
    support: DeepTrackingSupport
) : AutoCloseable {

    private val interpreter: Interpreter

    init {
        val modelBytes = when (support.source) {
            DeepTrackingSource.APP_FILE -> requireNotNull(support.modelFilePath) { "Missing deep model file path" }
                .let { java.io.File(it) }
                .readBytes()
            DeepTrackingSource.ASSET -> context.assets.open(support.modelAssetPath).use { input ->
                input.readBytes()
            }
            DeepTrackingSource.NONE -> error("Deep model is not available")
        }
        val buffer = ByteBuffer.allocateDirect(modelBytes.size).order(ByteOrder.nativeOrder())
        buffer.put(modelBytes)
        buffer.rewind()
        interpreter = Interpreter(buffer)
    }

    private fun outputShape(): IntArray = interpreter.getOutputTensor(0).shape()

    private fun inputShape(): IntArray = interpreter.getInputTensor(0).shape()

    private fun scaledBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap =
        Bitmap.createScaledBitmap(bitmap, width, height, true)

    private fun buildInput(bitmap: Bitmap, width: Int, height: Int): ByteBuffer {
        val scaled = scaledBitmap(bitmap, width, height)
        val input = ByteBuffer.allocateDirect(4 * width * height * 3).order(ByteOrder.nativeOrder())
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        pixels.forEach { color ->
            input.putFloat(((color shr 16) and 0xFF) / 255f)
            input.putFloat(((color shr 8) and 0xFF) / 255f)
            input.putFloat((color and 0xFF) / 255f)
        }
        input.rewind()
        return input
    }

    private fun runSegmentation(bitmap: Bitmap): SegmentationResult {
        val inputShape = inputShape()
        val outputShape = outputShape()
        val inputHeight = inputShape.getOrNull(1) ?: 257
        val inputWidth = inputShape.getOrNull(2) ?: 257
        val outputHeight = outputShape.getOrNull(1) ?: inputHeight
        val outputWidth = outputShape.getOrNull(2) ?: inputWidth
        val classCount = outputShape.getOrNull(3) ?: 21
        val input = buildInput(bitmap, inputWidth, inputHeight)
        val output = Array(1) {
            Array(outputHeight) {
                Array(outputWidth) {
                    FloatArray(classCount)
                }
            }
        }
        interpreter.run(input, output)
        return SegmentationResult(
            logits = output[0],
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            frameWidth = bitmap.width,
            frameHeight = bitmap.height
        )
    }

    fun seed(bitmap: Bitmap, tappedPoint: Offset): DeepTrackingState? {
        val segmentation = runSegmentation(bitmap)
        val seedPoint = segmentation.frameToOutput(tappedPoint)
        val targetClass = segmentation.findTargetClass(seedPoint) ?: return null
        val centroid = segmentation.centroidForClass(
            targetClassIndex = targetClass,
            predictedCenter = seedPoint,
            searchRadiusPx = max(segmentation.outputWidth, segmentation.outputHeight) / 5
        ) ?: return null
        return DeepTrackingState(
            targetClassIndex = targetClass,
            center = segmentation.outputToFrame(centroid),
            velocity = Offset.Zero,
            frameWidth = bitmap.width,
            frameHeight = bitmap.height
        )
    }

    fun track(bitmap: Bitmap, previous: DeepTrackingState): DeepTrackingMatch? {
        val segmentation = runSegmentation(bitmap)
        val predictedFrameCenter = previous.center + previous.velocity
        val predictedOutputCenter = segmentation.frameToOutput(predictedFrameCenter)
        val searchRadius = (18f + (offsetMagnitude(previous.velocity) * 0.5f)).toInt()
            .coerceIn(18, max(segmentation.outputWidth, segmentation.outputHeight) / 3)
        val centroid = segmentation.centroidForClass(
            targetClassIndex = previous.targetClassIndex,
            predictedCenter = predictedOutputCenter,
            searchRadiusPx = searchRadius
        ) ?: return null
        val confidence = segmentation.averageConfidenceForClass(
            targetClassIndex = previous.targetClassIndex,
            predictedCenter = centroid,
            radiusPx = 6
        )
        if (confidence < 0.25f) return null
        val frameCenter = segmentation.outputToFrame(centroid)
        val nextVelocity = Offset(
            x = frameCenter.x - previous.center.x,
            y = frameCenter.y - previous.center.y
        )
        return DeepTrackingMatch(
            state = previous.copy(
                center = frameCenter,
                velocity = previous.velocity + ((nextVelocity - previous.velocity) * 0.65f),
                frameWidth = bitmap.width,
                frameHeight = bitmap.height
            ),
            confidence = confidence.toDouble()
        )
    }

    override fun close() {
        interpreter.close()
    }

    private data class SegmentationResult(
        val logits: Array<Array<FloatArray>>,
        val outputWidth: Int,
        val outputHeight: Int,
        val frameWidth: Int,
        val frameHeight: Int
    ) {
        fun frameToOutput(point: Offset): Offset = Offset(
            x = ((point.x / frameWidth.coerceAtLeast(1)) * outputWidth).coerceIn(0f, outputWidth - 1f),
            y = ((point.y / frameHeight.coerceAtLeast(1)) * outputHeight).coerceIn(0f, outputHeight - 1f)
        )

        fun outputToFrame(point: Offset): Offset = Offset(
            x = (point.x / outputWidth.coerceAtLeast(1)) * frameWidth,
            y = (point.y / outputHeight.coerceAtLeast(1)) * frameHeight
        )

        fun findTargetClass(seedPoint: Offset): Int? {
            val seedX = seedPoint.x.toInt().coerceIn(0, outputWidth - 1)
            val seedY = seedPoint.y.toInt().coerceIn(0, outputHeight - 1)
            val directClass = logits[seedY][seedX].argMax()
            if (directClass != 0) return directClass
            var bestClass: Int? = null
            var bestScore = Float.NEGATIVE_INFINITY
            for (radius in 1..8) {
                for (y in (seedY - radius).coerceAtLeast(0)..(seedY + radius).coerceAtMost(outputHeight - 1)) {
                    for (x in (seedX - radius).coerceAtLeast(0)..(seedX + radius).coerceAtMost(outputWidth - 1)) {
                        val classes = logits[y][x]
                        val index = classes.argMax()
                        val score = classes[index]
                        if (index != 0 && score > bestScore) {
                            bestClass = index
                            bestScore = score
                        }
                    }
                }
                if (bestClass != null) return bestClass
            }
            return null
        }

        fun centroidForClass(
            targetClassIndex: Int,
            predictedCenter: Offset,
            searchRadiusPx: Int
        ): Offset? {
            val centerX = predictedCenter.x.toInt().coerceIn(0, outputWidth - 1)
            val centerY = predictedCenter.y.toInt().coerceIn(0, outputHeight - 1)
            var totalWeight = 0f
            var weightedX = 0f
            var weightedY = 0f
            for (y in (centerY - searchRadiusPx).coerceAtLeast(0)..(centerY + searchRadiusPx).coerceAtMost(outputHeight - 1)) {
                for (x in (centerX - searchRadiusPx).coerceAtLeast(0)..(centerX + searchRadiusPx).coerceAtMost(outputWidth - 1)) {
                    val classes = logits[y][x]
                    val index = classes.argMax()
                    if (index == targetClassIndex) {
                        val distancePenalty = 1f / (1f + sqrt(((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)).toFloat()))
                        val weight = classes[index] * distancePenalty
                        totalWeight += weight
                        weightedX += x * weight
                        weightedY += y * weight
                    }
                }
            }
            if (totalWeight <= 0f) return null
            return Offset(weightedX / totalWeight, weightedY / totalWeight)
        }

        fun averageConfidenceForClass(
            targetClassIndex: Int,
            predictedCenter: Offset,
            radiusPx: Int
        ): Float {
            val centerX = predictedCenter.x.toInt().coerceIn(0, outputWidth - 1)
            val centerY = predictedCenter.y.toInt().coerceIn(0, outputHeight - 1)
            var total = 0f
            var count = 0
            for (y in (centerY - radiusPx).coerceAtLeast(0)..(centerY + radiusPx).coerceAtMost(outputHeight - 1)) {
                for (x in (centerX - radiusPx).coerceAtLeast(0)..(centerX + radiusPx).coerceAtMost(outputWidth - 1)) {
                    total += logits[y][x][targetClassIndex]
                    count += 1
                }
            }
            return if (count == 0) 0f else total / count
        }
    }
}

private fun FloatArray.argMax(): Int {
    var bestIndex = 0
    var bestValue = Float.NEGATIVE_INFINITY
    forEachIndexed { index, value ->
        if (value > bestValue) {
            bestValue = value
            bestIndex = index
        }
    }
    return bestIndex
}

private fun offsetMagnitude(offset: Offset): Float = sqrt((offset.x * offset.x) + (offset.y * offset.y))
