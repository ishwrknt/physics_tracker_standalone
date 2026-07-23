package com.physicstracker.app.tracking

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoTimingInfoTest {
    @Test
    fun nearestFrameUsesPresentationTimelineNotNominalDivision() {
        val timeline = VideoTimingInfo(sampleTimesUs = longArrayOf(0, 16_656, 33_322, 49_989, 116_644))
        val identity = timeline.nearestFrame(83_000)
        assertEquals(3, identity.frameNumber)
        assertEquals(49_989L, identity.actualTimestampUs)
        assertEquals(83_000L, identity.requestedTimestampUs)
    }
}
