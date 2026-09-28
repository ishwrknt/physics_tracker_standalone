package com.physicstracker.app.ui.light

import androidx.compose.ui.geometry.Offset
import com.physicstracker.app.ui.LightGeometry
import com.physicstracker.app.ui.LightTool
import kotlin.math.max

const val MAX_LIGHT_REGIONS = 256

fun lightRegionLabels(count: Int): List<String> = (0 until count.coerceIn(1, MAX_LIGHT_REGIONS)).map { index ->
    var value = index + 1
    buildString {
        while (value > 0) {
            value--
            insert(0, ('A'.code + value % 26).toChar())
            value /= 26
        }
    }
}

/** Comparison and export use the same two independent region identities. */
fun lightComparisonSlots(first: String, requestedSecond: String, count: Int): List<String> {
    val slots = lightRegionLabels(count)
    val primary = first.takeIf { it in slots } ?: slots.first()
    val secondary = requestedSecond.takeIf { it in slots && it != primary }
        ?: slots.firstOrNull { it != primary }
    return listOfNotNull(primary, secondary)
}

fun lightGeometryForTool(current: LightGeometry, tool: LightTool, width: Int, height: Int): LightGeometry {
    if (tool == LightTool.POINT) return current.copy(tool = tool, end = current.start)
    if (current.tool == tool) return current
    val dx = max(1f, width / 8f)
    val dy = if (tool == LightTool.BOX) max(1f, height / 8f) else 0f
    return canonicalLightGeometry(LightGeometry(tool, current.start - Offset(dx, dy), current.start + Offset(dx, dy)), width, height)
}

/** A new region starts with the selected region's tool, but owns fresh geometry. */
fun lightNewRegionGeometry(current: LightGeometry?, width: Int, height: Int): LightGeometry {
    val start = current?.start ?: Offset((width - 1) / 2f, (height - 1) / 2f)
    val point = canonicalLightGeometry(LightGeometry(LightTool.POINT, start, start), width, height)
    return lightGeometryForTool(point, current?.tool ?: LightTool.POINT, width, height)
}

/** Derived presentation only. Never changes the stored raw samples or bridges missing data. */
fun processedLightData(data: LightDataset, subtractBaseline: Boolean, smooth: Boolean): LightDataset {
    if (!subtractBaseline && !smooth) return data
    val baseline = if (subtractBaseline) data.observations.minOfOrNull { it.value } ?: 0.0 else 0.0
    val segments = mutableListOf<MutableList<LightObservation>>()
    data.observations.forEach {
        if (segments.isEmpty() || it.gapBefore) segments.add(mutableListOf())
        segments.last().add(it)
    }
    val samples = segments.flatMap { segment -> segment.mapIndexed { index, sample ->
        val value = if (smooth) segment.subList((index - 1).coerceAtLeast(0), (index + 2).coerceAtMost(segment.size)).map { it.value }.average() else sample.value
        sample.copy(value = (value - baseline).coerceAtLeast(0.0), rawValue = sample.rawValue ?: sample.value)
    } }
    val description = listOfNotNull(if (subtractBaseline) "minimum baseline subtracted ($baseline)" else null,
        if (smooth) "3-sample moving mean, not across gaps" else null).joinToString("; ")
    return data.copy(observations = samples, processing = description, provenance = "${data.provenance}; DERIVED: $description")
}

fun lightPeakIndices(samples: List<LightObservation>): List<Int> = (1 until samples.lastIndex).filter { i ->
    !samples[i].gapBefore && !samples[i + 1].gapBefore && samples[i].value > samples[i - 1].value && samples[i].value > samples[i + 1].value
}
