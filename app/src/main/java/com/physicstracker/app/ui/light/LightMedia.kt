package com.physicstracker.app.ui.light

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlin.math.roundToInt

class BitmapLightPixels(private val bitmap: Bitmap) : LightPixels {
    override val width get() = bitmap.width
    override val height get() = bitmap.height
    override fun argb(x: Int, y: Int) = bitmap.getPixel(x, y)
}

private fun checkLightImageSize(width: Int, height: Int) {
    val limit = (Runtime.getRuntime().maxMemory() / 16).coerceAtMost(24_000_000L)
    require(width > 0 && height > 0 && width.toLong() * height <= limit) {
        "This image is too large for full-resolution analysis on this device. Please import a smaller image."
    }
}

fun decodeLightPhoto(context: Context, uri: Uri): Bitmap {
    if (Build.VERSION.SDK_INT >= 28) return ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(context.contentResolver, uri)
    ) { decoder, info, _ ->
        checkLightImageSize(info.size.width, info.size.height)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
    checkLightImageSize(bounds.outWidth, bounds.outHeight)
    val decoded = requireNotNull(context.contentResolver.openInputStream(uri).use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 })
    }) { "Could not decode this photo." }
    val exif = context.contentResolver.openInputStream(uri).use { stream ->
        requireNotNull(stream)
        ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }
    val matrix = Matrix().apply {
        when (exif) {
            2 -> postScale(-1f, 1f)
            3 -> postRotate(180f)
            4 -> postScale(1f, -1f)
            5 -> { postRotate(90f); postScale(-1f, 1f) }
            6 -> postRotate(90f)
            7 -> { postRotate(270f); postScale(-1f, 1f) }
            8 -> postRotate(270f)
        }
    }
    return if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0,
        decoded.width, decoded.height, matrix, false).also { if (it !== decoded) decoded.recycle() }
}

/** BT.601/709 SDR conversion; unavailable metadata is reported as an assumption, never lux. */
internal fun lightYuvToArgb(y: Int, u: Int, v: Int, bt709: Boolean, fullRange: Boolean): Int {
    val luma = if (fullRange) y.toDouble() else (y - 16) * 255.0 / 219
    val scale = if (fullRange) 1.0 else 255.0 / 224
    val cb = (u - 128) * scale
    val cr = (v - 128) * scale
    val r = luma + (if (bt709) 1.5748 else 1.402) * cr
    val g = luma - (if (bt709) 0.187324 else 0.344136) * cb - (if (bt709) 0.468124 else 0.714136) * cr
    val b = luma + (if (bt709) 1.8556 else 1.772) * cb
    return (255 shl 24) or (r.roundToInt().coerceIn(0, 255) shl 16) or
        (g.roundToInt().coerceIn(0, 255) shl 8) or b.roundToInt().coerceIn(0, 255)
}

private class YuvLightPixels(private val image: Image, rotation: Int, standard: Int, range: Int) : LightPixels {
    private val crop = image.cropRect
    private val turn = ((rotation % 360) + 360) % 360
    private val planes = image.planes
    private val buffers = planes.map { it.buffer.duplicate() }
    private val bt709 = standard == MediaFormat.COLOR_STANDARD_BT709
    private val full = range == MediaFormat.COLOR_RANGE_FULL
    override val width = if (turn == 90 || turn == 270) crop.height() else crop.width()
    override val height = if (turn == 90 || turn == 270) crop.width() else crop.height()
    override fun argb(x: Int, y: Int): Int {
        val raw = when (turn) {
            90 -> y to crop.height() - 1 - x
            180 -> crop.width() - 1 - x to crop.height() - 1 - y
            270 -> crop.width() - 1 - y to x
            else -> x to y
        }
        val sx = raw.first + crop.left
        val sy = raw.second + crop.top
        fun sample(index: Int): Int {
            val divisor = if (index == 0) 1 else 2
            val plane = planes[index]
            return buffers[index].get(buffers[index].position() + (sy / divisor) * plane.rowStride +
                (sx / divisor) * plane.pixelStride).toInt() and 255
        }
        return lightYuvToArgb(sample(0), sample(1), sample(2), bt709, full)
    }
}

fun LightPixels.toPreviewBitmap(checkCancellation: () -> Unit = {}): Bitmap {
    checkLightImageSize(width, height)
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        checkCancellation()
        for (x in 0 until width) pixels[y * width + x] = argb(x, y)
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

/** Sequential decode, using output BufferInfo PTS rather than player polling or nominal FPS.
 * Pixel views are valid only for the callback. Returning false stops decoding cleanly.
 */
fun decodeLightVideo(context: Context, uri: Uri, checkCancellation: () -> Unit = {},
                     onFrame: (LightPixels, Long, String) -> Boolean) {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
        extractor.setDataSource(context, uri, null)
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        } ?: error("No video stream found.")
        val inputFormat = extractor.getTrackFormat(track)
        fun MediaFormat.intOr(key: String, fallback: Int) = if (containsKey(key)) getInteger(key) else fallback
        val rotation = inputFormat.intOr(MediaFormat.KEY_ROTATION, 0)
        require(rotation % 90 == 0) { "Unsupported video rotation." }
        val transfer = inputFormat.intOr(MediaFormat.KEY_COLOR_TRANSFER, 3)
        require(transfer != 6 && transfer != 7) { "HDR video is not supported for intensity analysis. Import an SDR copy." }
        inputFormat.setInteger(MediaFormat.KEY_ROTATION, 0)
        inputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        extractor.selectTrack(track)
        val decoder = MediaCodec.createDecoderByType(requireNotNull(inputFormat.getString(MediaFormat.KEY_MIME)))
        codec = decoder
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()
        var inputDone = false
        var lastProgress = System.nanoTime()
        val info = MediaCodec.BufferInfo()
        var delivered = false
        while (true) {
            checkCancellation()
            check(System.nanoTime() - lastProgress < 20_000_000_000L) { "Video decoder stalled. Try another video." }
            if (!inputDone) {
                val index = decoder.dequeueInputBuffer(10_000)
                if (index >= 0) {
                    val buffer = requireNotNull(decoder.getInputBuffer(index))
                    val count = extractor.readSampleData(buffer, 0)
                    if (count < 0) {
                        decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        decoder.queueInputBuffer(index, 0, count, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val output = decoder.dequeueOutputBuffer(info, 10_000)
            if (output >= 0) {
                lastProgress = System.nanoTime()
                try {
                    if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        val format = decoder.outputFormat
                        val standard = format.intOr(MediaFormat.KEY_COLOR_STANDARD,
                            inputFormat.intOr(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT601_NTSC))
                        require(standard in listOf(MediaFormat.COLOR_STANDARD_BT709, MediaFormat.COLOR_STANDARD_BT601_NTSC,
                            MediaFormat.COLOR_STANDARD_BT601_PAL)) { "Unsupported video colour standard. Import an SDR BT.601/709 video." }
                        val range = format.intOr(MediaFormat.KEY_COLOR_RANGE,
                            inputFormat.intOr(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED))
                        val assumed = !format.containsKey(MediaFormat.KEY_COLOR_STANDARD) && !inputFormat.containsKey(MediaFormat.KEY_COLOR_STANDARD)
                        val assumedRange = !format.containsKey(MediaFormat.KEY_COLOR_RANGE) && !inputFormat.containsKey(MediaFormat.KEY_COLOR_RANGE)
                        val provenance = "Decoder output PTS; ${if (standard == 1) "BT.709" else "BT.601"}${if (assumed) " (assumed)" else ""}; " +
                            "${if (range == 1) "full" else "limited"} range${if (assumedRange) " (assumed)" else ""}; rotation ${rotation}°"
                        val image = decoder.getOutputImage(output) ?: error("This device cannot expose video pixels for this format.")
                        val keepGoing = image.use {
                            require(it.format == android.graphics.ImageFormat.YUV_420_888) { "Unsupported decoded pixel format." }
                            onFrame(YuvLightPixels(it, rotation, standard, range), info.presentationTimeUs, provenance)
                        }
                        delivered = true
                        if (!keepGoing) return
                    }
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                } finally {
                    decoder.releaseOutputBuffer(output, false)
                }
            }
        }
        check(delivered) { "No readable video frames." }
    } finally {
        runCatching { codec?.stop() }
        codec?.release()
        extractor.release()
    }
}
