package com.physicstracker.app.tracking

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlin.math.abs

data class VideoTimingInfo(
    val width: Int = 0,
    val height: Int = 0,
    val rotationDegrees: Int = 0,
    val durationUs: Long = 0L,
    val nominalFrameRate: Double? = null,
    val averageFrameRate: Double? = null,
    val sampleTimesUs: LongArray = longArrayOf(),
    val variableFrameTiming: Boolean = false
) {
    val frameCount: Int get() = sampleTimesUs.size

    fun nearestFrame(requestedTimestampUs: Long): FrameIdentity {
        if (sampleTimesUs.isEmpty()) return FrameIdentity(-1, requestedTimestampUs, null)
        val insertion = sampleTimesUs.binarySearch(requestedTimestampUs)
        val index = if (insertion >= 0) insertion else {
            val after = (-insertion - 1).coerceIn(0, sampleTimesUs.lastIndex)
            val before = (after - 1).coerceAtLeast(0)
            if (abs(sampleTimesUs[before] - requestedTimestampUs) <=
                abs(sampleTimesUs[after] - requestedTimestampUs)) before else after
        }
        return FrameIdentity(index, requestedTimestampUs, sampleTimesUs[index])
    }
}

data class FrameIdentity(
    val frameNumber: Int,
    val requestedTimestampUs: Long,
    val actualTimestampUs: Long?
)

object VideoTimelineIndex {
    fun inspect(context: Context, uriString: String): VideoTimingInfo = runCatching {
        MediaExtractor().useCompat { extractor ->
            extractor.setDataSource(context, Uri.parse(uriString), null)
            val trackIndex = (0 until extractor.trackCount).first { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            }
            val format = extractor.getTrackFormat(trackIndex)
            extractor.selectTrack(trackIndex)
            val samples = ArrayList<Long>()
            while (extractor.sampleTime >= 0L) {
                samples += extractor.sampleTime
                if (!extractor.advance()) break
            }
            val times = samples.toLongArray()
            val deltas = times.asList().zipWithNext { left, right -> right - left }.filter { it > 0L }
            val medianDelta = deltas.sorted().let { sorted -> sorted.getOrNull(sorted.size / 2) }
            val irregular = medianDelta != null && deltas.any { delta ->
                abs(delta - medianDelta).toDouble() / medianDelta.coerceAtLeast(1L) > 0.08
            }
            val nominal = format.optionalInt(MediaFormat.KEY_FRAME_RATE)?.takeIf { it > 0 }?.toDouble()
            val duration = format.optionalLong(MediaFormat.KEY_DURATION) ?: times.lastOrNull() ?: 0L
            val average = if (times.size > 1 && times.last() > times.first())
                (times.size - 1) * 1_000_000.0 / (times.last() - times.first()) else null
            VideoTimingInfo(
                width = format.optionalInt(MediaFormat.KEY_WIDTH) ?: 0,
                height = format.optionalInt(MediaFormat.KEY_HEIGHT) ?: 0,
                rotationDegrees = format.optionalInt(MediaFormat.KEY_ROTATION) ?: 0,
                durationUs = duration,
                nominalFrameRate = nominal,
                averageFrameRate = average,
                sampleTimesUs = times,
                variableFrameTiming = irregular || (nominal != null && average != null && abs(nominal - average) > 0.5)
            )
        }
    }.getOrDefault(VideoTimingInfo())
}

private inline fun <T> MediaExtractor.useCompat(block: (MediaExtractor) -> T): T = try {
    block(this)
} finally {
    release()
}

private fun MediaFormat.optionalInt(key: String): Int? =
    if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

private fun MediaFormat.optionalLong(key: String): Long? =
    if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null
