package com.physicstracker.app.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

data class MediaPipeTrackingState(
    val label: String?,
    val center: Offset,
    val box: RectF,
    val anchorFractionX: Float,
    val anchorFractionY: Float,
    val velocity: Offset = Offset.Zero,
    val frameWidth: Int,
    val frameHeight: Int
)

data class MediaPipeTrackingMatch(
    val state: MediaPipeTrackingState,
    val confidence: Double
)

class MediaPipeObjectTracker(
    context: Context,
    support: MediaPipeTrackingSupport
) : AutoCloseable {

    private val detector: ObjectDetector

    init {
        val modelBytes = when (support.source) {
            MediaPipeTrackingSource.APP_FILE -> requireNotNull(support.modelFilePath) {
                "Missing MediaPipe model file path"
            }.let { java.io.File(it) }.readBytes()
            MediaPipeTrackingSource.ASSET -> context.assets.open(support.modelAssetPath).use { it.readBytes() }
            MediaPipeTrackingSource.NONE -> error("MediaPipe model is not available")
        }
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size).order(ByteOrder.nativeOrder())
        modelBuffer.put(modelBytes)
        modelBuffer.rewind()
        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(modelBuffer)
            .build()
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setMaxResults(6)
            .setScoreThreshold(0.35f)
            .build()
        detector = ObjectDetector.createFromOptions(context, options)
    }

    fun seed(bitmap: Bitmap, tappedPoint: Offset, timestampMs: Long): MediaPipeTrackingState? {
        val detections = detect(bitmap, timestampMs)
        val selected = detections
            .filter { it.box.contains(tappedPoint.x, tappedPoint.y) }
            .maxByOrNull { it.confidence }
            ?: detections.minByOrNull { distance(it.center, tappedPoint) }
            ?: return null
        return MediaPipeTrackingState(
            label = selected.label,
            center = selected.anchoredPoint(tappedPoint),
            box = selected.box,
            anchorFractionX = selected.anchorFractionX(tappedPoint),
            anchorFractionY = selected.anchorFractionY(tappedPoint),
            velocity = Offset.Zero,
            frameWidth = bitmap.width,
            frameHeight = bitmap.height
        )
    }

    fun track(bitmap: Bitmap, previous: MediaPipeTrackingState, timestampMs: Long): MediaPipeTrackingMatch? {
        val detections = detect(bitmap, timestampMs)
        if (detections.isEmpty()) return null
        val predictedCenter = previous.center + previous.velocity
        val predictedBox = RectF(previous.box).apply {
            offset(previous.velocity.x, previous.velocity.y)
        }
        val selected = detections.maxByOrNull { candidate ->
            val candidatePoint = candidate.anchoredPoint(
                previous.anchorFractionX,
                previous.anchorFractionY
            )
            val distancePenalty = distance(candidatePoint, predictedCenter)
            val overlapBoost = iou(candidate.box, predictedBox) * 120f
            val labelBoost = if (previous.label != null && previous.label == candidate.label) 20f else 0f
            (candidate.confidence * 100f) + overlapBoost + labelBoost - distancePenalty
        } ?: return null
        if (selected.confidence < 0.3f) return null
        val selectedPoint = selected.anchoredPoint(previous.anchorFractionX, previous.anchorFractionY)
        val rawVelocity = selectedPoint - previous.center
        return MediaPipeTrackingMatch(
            state = previous.copy(
                label = selected.label,
                center = selectedPoint,
                box = selected.box,
                velocity = blend(previous.velocity, rawVelocity, 0.7f),
                frameWidth = bitmap.width,
                frameHeight = bitmap.height
            ),
            confidence = selected.confidence.toDouble()
        )
    }

    override fun close() {
        detector.close()
    }

    private fun detect(bitmap: Bitmap, timestampMs: Long): List<DetectionCandidate> {
        val image = BitmapImageBuilder(bitmap).build()
        return detector.detectForVideo(image, timestampMs).detections().mapNotNull { detection ->
            val category = detection.categories().maxByOrNull { it.score() } ?: return@mapNotNull null
            val box = detection.boundingBox()
            val label = category.categoryName().takeUnless { it.isNullOrBlank() } ?: category.displayName()
            DetectionCandidate(
                label = label,
                confidence = category.score(),
                box = box,
                center = Offset((box.left + box.right) / 2f, (box.top + box.bottom) / 2f)
            )
        }
    }

    private data class DetectionCandidate(
        val label: String?,
        val confidence: Float,
        val box: RectF,
        val center: Offset
    ) {
        fun anchorFractionX(point: Offset): Float =
            ((point.x - box.left) / box.width().coerceAtLeast(1f)).coerceIn(0f, 1f)

        fun anchorFractionY(point: Offset): Float =
            ((point.y - box.top) / box.height().coerceAtLeast(1f)).coerceIn(0f, 1f)

        fun anchoredPoint(point: Offset): Offset =
            anchoredPoint(anchorFractionX(point), anchorFractionY(point))

        fun anchoredPoint(anchorX: Float, anchorY: Float): Offset =
            Offset(
                x = box.left + (box.width() * anchorX.coerceIn(0f, 1f)),
                y = box.top + (box.height() * anchorY.coerceIn(0f, 1f))
            )
    }
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt((dx * dx) + (dy * dy))
}

private fun blend(previous: Offset, next: Offset, factor: Float): Offset =
    Offset(
        x = previous.x + ((next.x - previous.x) * factor),
        y = previous.y + ((next.y - previous.y) * factor)
    )

private fun iou(a: RectF, b: RectF): Float {
    val left = max(a.left, b.left)
    val top = max(a.top, b.top)
    val right = minOf(a.right, b.right)
    val bottom = minOf(a.bottom, b.bottom)
    val intersection = max(0f, right - left) * max(0f, bottom - top)
    if (intersection <= 0f) return 0f
    val union = a.width() * a.height() + b.width() * b.height() - intersection
    return if (union <= 0f) 0f else intersection / union
}
