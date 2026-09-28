package com.physicstracker.app.live

import kotlin.math.hypot

/**
 * Fixed-capacity rolling buffer. Old samples are dropped once capacity is reached so
 * long-running live sessions cannot exhaust memory.
 */
class RollingLiveBuffer<T>(private val capacity: Int) {
    private val values = ArrayDeque<T>(capacity.coerceAtLeast(8))

    val size: Int get() = values.size
    val lastOrNull: T? get() = values.lastOrNull()

    fun add(value: T) {
        if (values.size >= capacity) values.removeFirst()
        values.addLast(value)
    }

    fun addAll(items: Collection<T>) = items.forEach(::add)

    /** Returns the most recent [count] items, oldest first. */
    fun tail(count: Int): List<T> {
        if (count >= values.size) return values.toList()
        return values.toList().subList(values.size - count, values.size)
    }

    /** Returns items with timestamps (via [selector]) within [windowUs] of the newest item. */
    fun window(windowUs: Long, selector: (T) -> Long): List<T> {
        val newest = values.lastOrNull() ?: return emptyList()
        val cutoff = selector(newest) - windowUs
        return values.filter { selector(it) >= cutoff }
    }

    fun toList(): List<T> = values.toList()
    fun clear() = values.clear()
}

/**
 * Live kinematics engine.
 *
 * Pipeline: raw pixel sample -> calibration (pixels -> meters, relative to the first
 * sample) -> bounded centered triangular smoothing (fixed [smoothingRadius] frames of
 * latency) -> timestamp-delta velocity -> timestamp-delta acceleration.
 *
 * - Derivatives always use the actual elapsed sensor timestamps, never an assumed FPS.
 * - Time gaps (pause, object loss) break segments: smoothing and differentiation never
 *   bridge a gap. Call [breakSegment] whenever tracking identity or continuity is lost.
 * - Pure Kotlin, no Android dependencies, so it is directly unit-testable.
 */
class LiveKinematicsEngine(
    private var metersPerPixel: Double,
    private val smoothingRadius: Int = 3,
    private val maxGapUs: Long = 250_000L
) {
    private data class Filtered(
        val elapsedUs: Long,
        val xM: Double,
        val yM: Double
    )

    private val window = ArrayDeque<LiveRawSample>()
    private val filtered = ArrayDeque<Filtered>(64)
    private var originXPx: Double? = null
    private var originYPx: Double? = null
    private var lastRaw: LiveRawSample? = null

    val pointCount: Int get() = filtered.size

    fun setScale(metersPerPixel: Double) {
        this.metersPerPixel = metersPerPixel
    }

    /** Ends the current segment: smoothing and differentiation restart after the gap. */
    fun breakSegment() {
        window.clear()
        lastRaw = null
    }

    fun reset() {
        window.clear()
        filtered.clear()
        originXPx = null
        originYPx = null
        lastRaw = null
    }

    /**
     * Feed a raw sample. Returns the newest finished [LivePhysicsPoint], or null while
     * the smoothing window is still filling (first [smoothingRadius] frames of a segment).
     */
    fun onSample(sample: LiveRawSample): LivePhysicsPoint? {
        val previous = lastRaw
        if (previous != null && sample.elapsedUs - previous.elapsedUs > maxGapUs) {
            breakSegment()
        }
        if (originXPx == null) {
            originXPx = sample.xPx
            originYPx = sample.yPx
        }
        lastRaw = sample
        window.addLast(sample)
        var emitted = false
        while (window.size >= smoothingRadius * 2 + 1) {
            // Full centered window ready: emit the filtered center sample.
            emitFiltered(smoothCenter())
            window.removeFirst()
            emitted = true
        }
        return if (emitted) buildPoint() else null
    }

    /**
     * Flush remaining samples when recording stops, emitting trailing points with
     * truncated (past-only) windows.
     */
    fun flush() {
        while (window.isNotEmpty()) {
            emitFiltered(smoothTrailing())
            window.removeFirst()
        }
    }

    /** Triangular weighted average over the full window; timestamp of the center sample. */
    private fun smoothCenter(): Filtered {
        val samples = window.toList()
        val centerIndex = smoothingRadius
        var wSum = 0.0
        var xSum = 0.0
        var ySum = 0.0
        samples.forEachIndexed { index, raw ->
            val w = (smoothingRadius + 1 - kotlin.math.abs(index - centerIndex)).toDouble()
            wSum += w
            xSum += raw.xPx * w
            ySum += raw.yPx * w
        }
        val center = samples[centerIndex]
        return toFiltered(center.elapsedUs, xSum / wSum, ySum / wSum)
    }

    /** Causal trailing average used only when flushing the tail of a segment. */
    private fun smoothTrailing(): Filtered {
        val samples = window.toList()
        val head = samples.first()
        var wSum = 0.0
        var xSum = 0.0
        var ySum = 0.0
        samples.forEachIndexed { index, raw ->
            val w = (index + 1).toDouble()
            wSum += w
            xSum += raw.xPx * w
            ySum += raw.yPx * w
        }
        return toFiltered(head.elapsedUs, xSum / wSum, ySum / wSum)
    }

    private fun toFiltered(elapsedUs: Long, xPx: Double, yPx: Double): Filtered {
        val ox = originXPx ?: 0.0
        val oy = originYPx ?: 0.0
        return Filtered(elapsedUs, (xPx - ox) * metersPerPixel, (yPx - oy) * metersPerPixel)
    }

    private fun emitFiltered(f: Filtered) {
        filtered.addLast(f)
        while (filtered.size > 32) filtered.removeFirst()
    }

    /** Builds the physics point for the newest filtered sample using backward differences. */
    private fun buildPoint(): LivePhysicsPoint? {
        val list = filtered.toList()
        val current = list.lastOrNull() ?: return null
        val prev = list.getOrNull(list.size - 2)
        var vx: Double? = null
        var vy: Double? = null
        if (prev != null) {
            val dt = (current.elapsedUs - prev.elapsedUs) / 1_000_000.0
            if (dt > 0.0) {
                vx = (current.xM - prev.xM) / dt
                vy = (current.yM - prev.yM) / dt
            }
        }
        var ax: Double? = null
        var ay: Double? = null
        if (prev != null && vx != null && vy != null && list.size >= 3) {
            val prevPrev = list[list.size - 3]
            val dtPrev = (prev.elapsedUs - prevPrev.elapsedUs) / 1_000_000.0
            val dt = (current.elapsedUs - prev.elapsedUs) / 1_000_000.0
            if (dtPrev > 0.0 && dt > 0.0) {
                val prevVx = (prev.xM - prevPrev.xM) / dtPrev
                val prevVy = (prev.yM - prevPrev.yM) / dtPrev
                ax = (vx - prevVx) / dt
                ay = (vy - prevVy) / dt
            }
        }
        return LivePhysicsPoint(
            elapsedUs = current.elapsedUs,
            xMeters = current.xM,
            yMeters = current.yM,
            vxMetersPerSec = vx,
            vyMetersPerSec = vy,
            axMetersPerSec2 = ax,
            ayMetersPerSec2 = ay,
            speedMetersPerSec = if (vx != null && vy != null) hypot(vx, vy) else null,
            accelerationMetersPerSec2 = if (ax != null && ay != null) hypot(ax, ay) else null
        )
    }
}
