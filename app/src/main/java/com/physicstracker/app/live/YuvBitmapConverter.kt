package com.physicstracker.app.live

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import kotlin.math.max

/**
 * Converts CameraX YUV_420_888 [ImageProxy] frames into small upright ARGB bitmaps for
 * the tracking pipeline. Downscaling keeps per-frame conversion cost low; buffers are
 * reused to avoid per-frame allocation churn.
 *
 * Only called from the single live-pipeline thread.
 */
class YuvBitmapConverter(private val outputWidth: Int = DEFAULT_ANALYSIS_WIDTH) {

    companion object {
        const val DEFAULT_ANALYSIS_WIDTH = 480
    }

    private var buffer: IntArray = IntArray(0)
    private var bitmap: Bitmap? = null

    /** Last upright frame dimensions produced, for coordinate mapping in the UI. */
    var uprightWidth: Int = 0
        private set
    var uprightHeight: Int = 0
        private set

    /**
     * Returns a rotated-to-upright, downscaled bitmap, or null for unsupported formats.
     * The returned bitmap is owned by this converter and overwritten on the next call.
     */
    fun convert(image: ImageProxy): Bitmap? {
        if (image.format != ImageFormat.YUV_420_888) return null
        val rotation = ((image.imageInfo.rotationDegrees % 360) + 360) % 360
        val srcW = image.width
        val srcH = image.height
        val upW = FrameRotation.uprightWidth(srcW, srcH, rotation)
        val upH = FrameRotation.uprightHeight(srcW, srcH, rotation)
        if (upW <= 0 || upH <= 0) return null

        val aspect = upH.toDouble() / upW.toDouble()
        val outW = outputWidth
        val outH = max(2, (outW * aspect).toInt())
        uprightWidth = outW
        uprightHeight = outH

        val needed = outW * outH
        if (buffer.size != needed) buffer = IntArray(needed)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val yRowStride = yPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride

        var index = 0
        for (oy in 0 until outH) {
            val upY = (oy.toDouble() * upH / outH).toInt().coerceIn(0, upH - 1)
            for (ox in 0 until outW) {
                val upX = (ox.toDouble() * upW / outW).toInt().coerceIn(0, upW - 1)
                // Map upright coordinates back to sensor (unrotated) coordinates.
                val (fx, fy) = FrameRotation.uprightToSource(upX, upY, rotation, srcW, srcH)
                val y = yBuffer.get(fy * yRowStride + fx).toInt() and 0xFF
                val halfX = fx / 2
                val halfY = fy / 2
                val u = uBuffer.get(halfY * uRowStride + halfX * uPixelStride).toInt() and 0xFF
                val v = vBuffer.get(halfY * vRowStride + halfX * vPixelStride).toInt() and 0xFF
                buffer[index++] = yuvToArgb(y, u, v)
            }
        }

        val target = bitmap
        return if (target != null && target.width == outW && target.height == outH) {
            target.setPixels(buffer, 0, outW, 0, 0, outW, outH)
            target
        } else {
            // Bitmap.createBitmap(w, h, config) is mutable; pixels filled below.
            Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888).also { target ->
                target.setPixels(buffer, 0, outW, 0, 0, outW, outH)
                bitmap = target
            }
        }
    }

    fun release() {
        bitmap?.recycle()
        bitmap = null
        buffer = IntArray(0)
    }

    private fun yuvToArgb(y: Int, u: Int, v: Int): Int {
        val yF = if (y < 16) 0f else 1.164f * (y - 16)
        val uF = u - 128f
        val vF = v - 128f
        val r = (yF + 1.596f * vF).toInt().coerceIn(0, 255)
        val g = (yF - 0.813f * vF - 0.391f * uF).toInt().coerceIn(0, 255)
        val b = (yF + 2.018f * uF).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
