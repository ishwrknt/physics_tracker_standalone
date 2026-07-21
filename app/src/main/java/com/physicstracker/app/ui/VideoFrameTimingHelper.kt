package com.physicstracker.app.ui

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlin.math.roundToLong

object VideoFrameTimingHelper {
    private const val DEFAULT_FRAME_DURATION_MS = 33L
    private const val MICROS_PER_MILLI = 1_000L
    private const val SAMPLE_WINDOW_SIZE = 24

    fun estimateFrameDurationMs(context: Context, uriString: String): Long {
        val uri = Uri.parse(uriString)
        return runCatching {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
                val videoTrackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                    extractor.getTrackFormat(index)
                        .getString(MediaFormat.KEY_MIME)
                        ?.startsWith("video/") == true
                } ?: return@runCatching DEFAULT_FRAME_DURATION_MS

                val format = extractor.getTrackFormat(videoTrackIndex)
                readFrameDurationFromFormat(format)
                    ?: run {
                        extractor.selectTrack(videoTrackIndex)
                        readFrameDurationFromSamples(extractor)
                    }
            } finally {
                extractor.release()
            }
        }.getOrDefault(DEFAULT_FRAME_DURATION_MS)
    }

    private fun readFrameDurationFromFormat(format: MediaFormat): Long? {
        if (!format.containsKey(MediaFormat.KEY_FRAME_RATE)) return null
        val frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
        if (frameRate <= 0) return null
        return (1_000.0 / frameRate.toDouble()).roundToLong().coerceAtLeast(1L)
    }

    private fun readFrameDurationFromSamples(extractor: MediaExtractor): Long {
        var previousTimeUs = -1L
        val deltasUs = mutableListOf<Long>()

        repeat(SAMPLE_WINDOW_SIZE) {
            val sampleTimeUs = extractor.sampleTime
            if (sampleTimeUs < 0L) return@repeat
            if (previousTimeUs >= 0L) {
                val deltaUs = sampleTimeUs - previousTimeUs
                if (deltaUs > 0L) deltasUs += deltaUs
            }
            previousTimeUs = sampleTimeUs
            if (!extractor.advance()) return@repeat
        }

        if (deltasUs.isEmpty()) return DEFAULT_FRAME_DURATION_MS
        val averageDeltaUs = deltasUs.average().roundToLong()
        return (averageDeltaUs / MICROS_PER_MILLI).coerceAtLeast(1L)
    }
}
