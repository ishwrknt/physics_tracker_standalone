package com.physicstracker.app.tracking

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import com.bmad.new_start.shared_c_core.NativeTemplateTracker
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val templateSizeFastPx = 32
private const val templateRadiusAccuratePx = 12
private const val searchRadiusBasePx = 24
private const val searchRadiusFastMaxPx = 72
private const val searchRadiusAccurateMaxPx = 88
private const val lockPatchRadiusPx = 10
private const val minimumGeometryPixels = 18
private const val maxGeometryTemplateDivergencePx = 18f

enum class ClassicTrackerMode {
    FAST,
    ACCURATE
}

/** Explicit comparison paths. Keep the existing default for shared LIVE callers. */
enum class ClassicCandidatePolicy { ORIGINAL_BASELINE, INITIAL_HYBRID }

data class ClassicTrackingSeed(
    val template: Bitmap,
    val center: Offset,
    val velocity: Offset = Offset.Zero,
    val meanError: Double? = null,
    val searchRadiusPx: Int = searchRadiusBasePx,
    val confidence: Double? = null,
    val estimatedRadiusPx: Float = lockPatchRadiusPx.toFloat(),
    val objectLuma: Float = 0f,
    val backgroundLuma: Float = 0f,
    val objectIsBrighter: Boolean = true,
    val objectHue: Float = 0f,
    val objectSaturation: Float = 0f,
    val sourceWidth: Int,
    val sourceHeight: Int
)

data class ClassicTrackingUpdate(
    val seed: ClassicTrackingSeed,
    val effectiveSearchRadius: Int,
    val errorScore: Double,
    val confidence: Double,
    val estimatedRadiusPx: Float,
    val trackingSource: String,
    val ambiguityRatio: Double?,
    val residualErrorPx: Double,
    val diagnostics: TrackingDiagnostics
)

class ClassicTrackerEngine(
    private val mode: ClassicTrackerMode,
    quality: TrackingQuality = if (mode == ClassicTrackerMode.FAST) TrackingQuality.QUICK else TrackingQuality.SMART,
    private val targetProfile: TargetProfile = TargetProfile.OBJECT,
    recoveryPolicy: RecoveryPolicy = quality.defaultRecoveryPolicy(),
    private val candidatePolicy: ClassicCandidatePolicy = ClassicCandidatePolicy.INITIAL_HYBRID
) {
    private val integrityGate = TrackingIntegrityGate(quality, recoveryPolicy)
    private val profile = when (mode) {
        ClassicTrackerMode.FAST -> Profile(
            maxSearchRadiusPx = if (targetProfile == TargetProfile.ROBUST) 160 else searchRadiusFastMaxPx,
            maxMeanError = 900.0,
            templateRefreshThreshold = 10.0
        )
        ClassicTrackerMode.ACCURATE -> Profile(
            maxSearchRadiusPx = if (targetProfile == TargetProfile.ROBUST) 180 else searchRadiusAccurateMaxPx,
            maxMeanError = 28.0,
            templateRefreshThreshold = 14.0
        )
    }

    fun seed(
        bitmap: Bitmap,
        center: Offset,
        templateWidthPx: Int? = null,
        templateHeightPx: Int? = null
    ): ClassicTrackingSeed? {
        val template = (
            if (templateWidthPx != null && templateHeightPx != null) {
                cropTemplateRect(bitmap, center, templateWidthPx, templateHeightPx)
            } else {
                when (mode) {
                    ClassicTrackerMode.FAST -> cropTemplateSized(bitmap, center, templateSizeFastPx)
                    ClassicTrackerMode.ACCURATE -> cropTemplate(bitmap, center, templateRadiusAccuratePx)
                }
            }
        ) ?: return null
        val geometrySeed = buildGeometrySeed(bitmap, center)

        return ClassicTrackingSeed(
            template = template,
            center = center,
            confidence = 1.0,
            estimatedRadiusPx = geometrySeed.estimatedRadiusPx,
            objectLuma = geometrySeed.objectLuma,
            backgroundLuma = geometrySeed.backgroundLuma,
            objectIsBrighter = geometrySeed.objectIsBrighter,
            objectHue = geometrySeed.objectHue,
            objectSaturation = geometrySeed.objectSaturation,
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height
        )
    }

    fun trackFrame(bitmap: Bitmap, seed: ClassicTrackingSeed): ClassicTrackingUpdate? {
        val predictedCenter = clampOffsetToBitmap(seed.center + seed.velocity, bitmap, seed.template)
        val searchRadiusBoost = (offsetMagnitude(seed.velocity) * 1.4f).toInt()
            .coerceAtMost(profile.maxSearchRadiusPx - searchRadiusBasePx)
        val effectiveSearchRadius = (seed.searchRadiusPx + searchRadiusBoost)
            .coerceIn(searchRadiusBasePx, profile.maxSearchRadiusPx)
        val geometryMatch = if (targetProfile == TargetProfile.MARKER && seed.objectSaturation >= 0.28f) {
            findMarkerMatch(bitmap, seed, predictedCenter, effectiveSearchRadius)
        } else {
            findGeometryMatch(bitmap, seed, predictedCenter, effectiveSearchRadius)
        }

        val match = when (mode) {
            ClassicTrackerMode.FAST -> findNativeTemplateMatch(bitmap, seed.template, predictedCenter, effectiveSearchRadius)
            ClassicTrackerMode.ACCURATE -> findTemplateMatch(bitmap, seed.template, predictedCenter, effectiveSearchRadius)
        }

        val templateRejected = match != null && match.error > profile.maxMeanError
        val templateScore = if (match == null || templateRejected) null else match.error
        val templateConfidence = templateScore?.let { score ->
            when (mode) {
                ClassicTrackerMode.FAST -> (1.0 - (score / 5000.0)).coerceIn(0.0, 1.0)
                ClassicTrackerMode.ACCURATE -> (1.0 - (score / profile.maxMeanError)).coerceIn(0.0, 1.0)
            }
        } ?: 0.0

        val geometryTemplateDistance = if (geometryMatch != null && templateScore != null) {
            (geometryMatch.center - match!!.center).getDistance()
        } else {
            Float.MAX_VALUE
        }
        val geometryCanAssist = geometryTemplateDistance <= max(
            seed.estimatedRadiusPx * 1.15f,
            maxGeometryTemplateDivergencePx
        )

        val hybridCandidates = buildList {
            if (templateScore != null && match != null) add(
                HybridTrackingCandidate(
                    source = "template",
                    center = match.center,
                    confidence = templateConfidence,
                    appearanceSimilarity = templateConfidence,
                    shapeSimilarity = if (geometryMatch == null) templateConfidence else (1.0 - (geometryTemplateDistance / maxGeometryTemplateDivergencePx).coerceIn(0f, 1f).toDouble()),
                    residualPixels = (match.center - predictedCenter).getDistance().toDouble()
                )
            )
            if (geometryMatch != null && (templateScore == null || geometryCanAssist)) add(
                HybridTrackingCandidate(
                    source = "geometry",
                    center = geometryMatch.center,
                    confidence = geometryMatch.confidence,
                    appearanceSimilarity = if (templateScore == null) geometryMatch.confidence else templateConfidence,
                    shapeSimilarity = geometryMatch.confidence,
                    residualPixels = (geometryMatch.center - predictedCenter).getDistance().toDouble()
                )
            )
        }
        val selectedHybridCandidate = HybridCandidateScorer.choose(
            hybridCandidates, predictedCenter, effectiveSearchRadius.toDouble()
        )
        val useGeometry = when {
            geometryMatch == null && templateScore == null -> return null
            candidatePolicy == ClassicCandidatePolicy.ORIGINAL_BASELINE -> when {
                geometryMatch != null && templateScore == null -> true
                geometryMatch == null || !geometryCanAssist -> false
                mode == ClassicTrackerMode.ACCURATE ->
                    geometryMatch.confidence >= 0.78 && geometryMatch.confidence >= templateConfidence + 0.12
                else -> geometryMatch.confidence >= 0.7 && geometryMatch.confidence >= templateConfidence + 0.1
            }
            selectedHybridCandidate?.source == "geometry" -> true
            else -> false
        }

        val acceptedCenter = when {
            useGeometry && templateScore != null && geometryMatch != null -> blendOffset(
                match!!.center,
                geometryMatch.center,
                if (mode == ClassicTrackerMode.ACCURATE) 0.22f else 0.18f
            )
            useGeometry && geometryMatch != null -> geometryMatch.center
            templateScore != null -> when (mode) {
                ClassicTrackerMode.FAST -> match!!.center
                ClassicTrackerMode.ACCURATE -> {
                    val templateMatch = match!!
                    when {
                        templateMatch.error < 10.0 -> templateMatch.center
                        templateMatch.error < 18.0 -> blendOffset(predictedCenter, templateMatch.center, 0.88f)
                        else -> blendOffset(predictedCenter, templateMatch.center, 0.72f)
                    }
                }
            }
            else -> return null
        }

        val anchoredCenter = if (templateScore != null) {
            blendOffset(
                predictedCenter,
                acceptedCenter,
                if (templateConfidence >= 0.78) 0.92f else 0.8f
            )
        } else {
            acceptedCenter
        }
        val rawVelocity = offsetDelta(seed.center, anchoredCenter)
        val nextVelocity = blendOffset(
            seed.velocity,
            rawVelocity,
            if ((geometryMatch?.confidence ?: templateConfidence) > 0.75) 0.78f else 0.56f
        )
        val refreshedTemplate = when (mode) {
            ClassicTrackerMode.FAST -> seed.template
            ClassicTrackerMode.ACCURATE -> {
                val refreshScore = templateScore ?: Double.MAX_VALUE
                when {
                    refreshScore < profile.templateRefreshThreshold ->
                        cropTemplate(bitmap, anchoredCenter, templateRadiusAccuratePx) ?: seed.template
                    else -> seed.template
                }
            }
        }
        val nextSearchRadius = when {
            geometryMatch?.confidence ?: 0.0 > 0.82 && (templateScore ?: 0.0) < 12.0 -> searchRadiusBasePx
            geometryMatch?.confidence ?: 0.0 > 0.68 -> (searchRadiusBasePx + 8).coerceAtMost(profile.maxSearchRadiusPx)
            (templateScore ?: Double.MAX_VALUE) < 22.0 -> (searchRadiusBasePx + 18).coerceAtMost(profile.maxSearchRadiusPx)
            else -> (seed.searchRadiusPx + 16).coerceAtMost(profile.maxSearchRadiusPx)
        }

        val rawConfidence = maxOf(geometryMatch?.confidence ?: 0.0, templateConfidence)
        val residual = (anchoredCenter - predictedCenter).getDistance().toDouble()
        val diagnostics = TrackingDiagnostics(
            matchScore = rawConfidence,
            ambiguityRatio = match?.ambiguityRatio,
            residualPixels = residual,
            displacementPixels = (anchoredCenter - seed.center).getDistance().toDouble(),
            searchRadiusPixels = effectiveSearchRadius.toDouble()
        )
        val assessment = integrityGate.assess(diagnostics)
        if (assessment.decision != TrackingDecision.ACCEPT) return null
        val confidence = assessment.confidence
        val geometrySeed = buildGeometrySeed(bitmap, acceptedCenter)
        val nextSeed = ClassicTrackingSeed(
            template = refreshedTemplate,
            center = anchoredCenter,
            velocity = nextVelocity,
            meanError = templateScore ?: geometryMatch?.errorScore,
            searchRadiusPx = nextSearchRadius,
            confidence = confidence,
            estimatedRadiusPx = geometryMatch?.estimatedRadiusPx ?: geometrySeed.estimatedRadiusPx,
            objectLuma = geometrySeed.objectLuma,
            backgroundLuma = geometrySeed.backgroundLuma,
            objectIsBrighter = geometrySeed.objectIsBrighter,
            objectHue = geometrySeed.objectHue,
            objectSaturation = geometrySeed.objectSaturation,
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height
        )

        return ClassicTrackingUpdate(
            seed = nextSeed,
            effectiveSearchRadius = effectiveSearchRadius,
            errorScore = templateScore ?: geometryMatch?.errorScore ?: Double.MAX_VALUE,
            confidence = confidence,
            estimatedRadiusPx = nextSeed.estimatedRadiusPx,
            trackingSource = when {
                useGeometry && templateScore != null -> "hybrid"
                useGeometry -> "geometry"
                else -> "template"
            },
            ambiguityRatio = match?.ambiguityRatio,
            residualErrorPx = residual,
            diagnostics = diagnostics
        )
    }

    private fun buildGeometrySeed(bitmap: Bitmap, center: Offset): GeometrySeed {
        val centerX = center.x.toInt().coerceIn(0, bitmap.width - 1)
        val centerY = center.y.toInt().coerceIn(0, bitmap.height - 1)
        val objectLuma = sampleDisk(bitmap, centerX, centerY, lockPatchRadiusPx)
        val backgroundLuma = sampleRing(bitmap, centerX, centerY, lockPatchRadiusPx + 6, lockPatchRadiusPx + 16)
        val appearance = sampleAppearance(bitmap, centerX, centerY, lockPatchRadiusPx)
        return GeometrySeed(
            objectLuma = objectLuma,
            backgroundLuma = backgroundLuma,
            objectIsBrighter = objectLuma >= backgroundLuma,
            estimatedRadiusPx = lockPatchRadiusPx.toFloat(),
            objectHue = appearance.first,
            objectSaturation = appearance.second
        )
    }

    private fun findMarkerMatch(
        bitmap: Bitmap,
        seed: ClassicTrackingSeed,
        predictedCenter: Offset,
        searchRadiusPx: Int
    ): GeometryMatch? {
        val left = max(0, predictedCenter.x.toInt() - searchRadiusPx)
        val top = max(0, predictedCenter.y.toInt() - searchRadiusPx)
        val right = min(bitmap.width - 1, predictedCenter.x.toInt() + searchRadiusPx)
        val bottom = min(bitmap.height - 1, predictedCenter.y.toInt() + searchRadiusPx)
        var sumX = 0.0
        var sumY = 0.0
        var weightSum = 0.0
        var count = 0
        for (y in top..bottom step 2) {
            for (x in left..right step 2) {
                val hsv = rgbToHsv(bitmap.getPixel(x, y))
                val hueDistance = min(abs(hsv.first - seed.objectHue), 1f - abs(hsv.first - seed.objectHue))
                if (hueDistance <= 0.075f && hsv.second >= max(0.24f, seed.objectSaturation * 0.52f)) {
                    val weight = ((1f - hueDistance / 0.075f) * hsv.second).toDouble()
                    sumX += x * weight
                    sumY += y * weight
                    weightSum += weight
                    count++
                }
            }
        }
        if (count < 5 || weightSum <= 0.0) return null
        val center = Offset((sumX / weightSum).toFloat(), (sumY / weightSum).toFloat())
        val residualRatio = ((center - predictedCenter).getDistance() / searchRadiusPx.coerceAtLeast(1)).coerceIn(0f, 1f)
        val confidence = ((count / 80.0).coerceIn(0.0, 1.0) * 0.55 +
            (seed.objectSaturation.toDouble().coerceIn(0.0, 1.0)) * 0.45) * (1.0 - residualRatio * 0.35)
        return GeometryMatch(
            center = center,
            confidence = confidence.coerceIn(0.0, 1.0),
            estimatedRadiusPx = sqrt((count * 4.0 / PI)).toFloat(),
            errorScore = (1.0 - confidence) * 100.0
        )
    }

    private fun findGeometryMatch(
        bitmap: Bitmap,
        seed: ClassicTrackingSeed,
        predictedCenter: Offset,
        searchRadiusPx: Int
    ): GeometryMatch? {
        val left = max(0, predictedCenter.x.toInt() - searchRadiusPx)
        val top = max(0, predictedCenter.y.toInt() - searchRadiusPx)
        val right = min(bitmap.width - 1, predictedCenter.x.toInt() + searchRadiusPx)
        val bottom = min(bitmap.height - 1, predictedCenter.y.toInt() + searchRadiusPx)
        val contrast = abs(seed.objectLuma - seed.backgroundLuma).coerceAtLeast(12f)
        val threshold = if (seed.objectIsBrighter) {
            seed.backgroundLuma + contrast * 0.35f
        } else {
            seed.backgroundLuma - contrast * 0.35f
        }

        var sumX = 0f
        var sumY = 0f
        var totalWeight = 0f
        var objectPixels = 0

        for (y in top..bottom) {
            for (x in left..right) {
                val value = luma(bitmap.getPixel(x, y)).toFloat()
                val signedContrast = if (seed.objectIsBrighter) value - threshold else threshold - value
                if (signedContrast > 0f) {
                    val weight = signedContrast * signedContrast
                    sumX += x * weight
                    sumY += y * weight
                    totalWeight += weight
                    objectPixels += 1
                }
            }
        }

        if (objectPixels < minimumGeometryPixels || totalWeight <= 0f) {
            return null
        }

        val center = Offset(sumX / totalWeight, sumY / totalWeight)
        val radiusEstimate = sqrt(objectPixels / PI.toFloat())
        val driftPenalty = ((center - predictedCenter).getDistance() / searchRadiusPx).coerceIn(0f, 1f)
        val supportScore = (objectPixels / 220f).coerceIn(0f, 1f)
        val contrastScore = (contrast / 90f).coerceIn(0f, 1f)
        val confidence = ((supportScore * 0.55f) + (contrastScore * 0.45f)) * (1f - driftPenalty * 0.35f)

        return GeometryMatch(
            center = center,
            confidence = confidence.toDouble().coerceIn(0.0, 1.0),
            estimatedRadiusPx = radiusEstimate,
            errorScore = (1.0 - confidence.toDouble().coerceIn(0.0, 1.0)) * 100.0
        )
    }

    private fun sampleDisk(bitmap: Bitmap, centerX: Int, centerY: Int, radius: Int): Float {
        var total = 0f
        var count = 0
        val radiusSq = radius * radius
        val left = max(0, centerX - radius)
        val top = max(0, centerY - radius)
        val right = min(bitmap.width - 1, centerX + radius)
        val bottom = min(bitmap.height - 1, centerY + radius)
        for (y in top..bottom) {
            val dy = y - centerY
            for (x in left..right) {
                val dx = x - centerX
                if (dx * dx + dy * dy <= radiusSq) {
                    total += luma(bitmap.getPixel(x, y))
                    count += 1
                }
            }
        }
        return if (count == 0) 0f else total / count
    }

    private fun sampleRing(bitmap: Bitmap, centerX: Int, centerY: Int, innerRadius: Int, outerRadius: Int): Float {
        var total = 0f
        var count = 0
        val innerSq = innerRadius * innerRadius
        val outerSq = outerRadius * outerRadius
        val left = max(0, centerX - outerRadius)
        val top = max(0, centerY - outerRadius)
        val right = min(bitmap.width - 1, centerX + outerRadius)
        val bottom = min(bitmap.height - 1, centerY + outerRadius)
        for (y in top..bottom) {
            val dy = y - centerY
            for (x in left..right) {
                val dx = x - centerX
                val distanceSq = dx * dx + dy * dy
                if (distanceSq > innerSq && distanceSq <= outerSq) {
                    total += luma(bitmap.getPixel(x, y))
                    count += 1
                }
            }
        }
        return if (count == 0) 0f else total / count
    }

    private data class GeometrySeed(
        val objectLuma: Float,
        val backgroundLuma: Float,
        val objectIsBrighter: Boolean,
        val estimatedRadiusPx: Float,
        val objectHue: Float,
        val objectSaturation: Float
    )

    private data class GeometryMatch(
        val center: Offset,
        val confidence: Double,
        val estimatedRadiusPx: Float,
        val errorScore: Double
    )

    private data class Profile(
        val maxSearchRadiusPx: Int,
        val maxMeanError: Double,
        val templateRefreshThreshold: Double
    )
}

private fun sampleAppearance(bitmap: Bitmap, centerX: Int, centerY: Int, radius: Int): Pair<Float, Float> {
    var hueX = 0.0
    var hueY = 0.0
    var saturation = 0.0
    var count = 0
    for (y in max(0, centerY - radius)..min(bitmap.height - 1, centerY + radius) step 2) {
        for (x in max(0, centerX - radius)..min(bitmap.width - 1, centerX + radius) step 2) {
            val hsv = rgbToHsv(bitmap.getPixel(x, y))
            if (hsv.second < 0.12f) continue
            hueX += kotlin.math.cos(hsv.first * 2.0 * PI) * hsv.second
            hueY += kotlin.math.sin(hsv.first * 2.0 * PI) * hsv.second
            saturation += hsv.second
            count++
        }
    }
    if (count == 0) return 0f to 0f
    val angle = kotlin.math.atan2(hueY, hueX)
    val hue = ((angle / (2.0 * PI) + 1.0) % 1.0).toFloat()
    return hue to (saturation / count).toFloat()
}

private fun rgbToHsv(color: Int): Pair<Float, Float> {
    val r = ((color shr 16) and 0xFF) / 255f
    val g = ((color shr 8) and 0xFF) / 255f
    val b = (color and 0xFF) / 255f
    val maximum = max(r, max(g, b))
    val minimum = min(r, min(g, b))
    val delta = maximum - minimum
    val hue = when {
        delta == 0f -> 0f
        maximum == r -> (((g - b) / delta) % 6f) / 6f
        maximum == g -> (((b - r) / delta) + 2f) / 6f
        else -> (((r - g) / delta) + 4f) / 6f
    }.let { if (it < 0f) it + 1f else it }
    return hue to if (maximum == 0f) 0f else delta / maximum
}

private fun cropTemplate(bitmap: Bitmap, center: Offset, radiusPx: Int): Bitmap? {
    val size = radiusPx * 2 + 1
    val left = center.x.toInt() - radiusPx
    val top = center.y.toInt() - radiusPx
    if (left < 0 || top < 0 || left + size > bitmap.width || top + size > bitmap.height) return null
    return Bitmap.createBitmap(bitmap, left, top, size, size)
}

private fun cropTemplateSized(bitmap: Bitmap, center: Offset, sizePx: Int): Bitmap? {
    val left = center.x.toInt() - (sizePx / 2)
    val top = center.y.toInt() - (sizePx / 2)
    if (left < 0 || top < 0 || left + sizePx > bitmap.width || top + sizePx > bitmap.height) return null
    return Bitmap.createBitmap(bitmap, left, top, sizePx, sizePx)
}

private fun cropTemplateRect(bitmap: Bitmap, center: Offset, widthPx: Int, heightPx: Int): Bitmap? {
    val width = widthPx.coerceIn(8, bitmap.width)
    val height = heightPx.coerceIn(8, bitmap.height)
    val left = (center.x.toInt() - width / 2).coerceIn(0, bitmap.width - width)
    val top = (center.y.toInt() - height / 2).coerceIn(0, bitmap.height - height)
    return Bitmap.createBitmap(bitmap, left, top, width, height)
}

private fun luma(color: Int): Int {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    return (r * 30 + g * 59 + b * 11) / 100
}

private data class TemplateMatch(
    val center: Offset,
    val error: Double,
    val secondBestError: Double?
) {
    val ambiguityRatio: Double?
        get() = secondBestError?.let { it / error.coerceAtLeast(1e-6) }
}

private fun findTemplateMatch(
    bitmap: Bitmap,
    template: Bitmap,
    predictedCenter: Offset,
    searchRadiusPx: Int
): TemplateMatch? {
    val templateWidth = template.width
    val templateHeight = template.height
    if (templateWidth <= 0 || templateHeight <= 0) return null
    val halfWidth = templateWidth / 2
    val halfHeight = templateHeight / 2
    val startX = max(halfWidth, predictedCenter.x.toInt() - searchRadiusPx)
    val endX = minOf(bitmap.width - halfWidth - 1, predictedCenter.x.toInt() + searchRadiusPx)
    val startY = max(halfHeight, predictedCenter.y.toInt() - searchRadiusPx)
    val endY = minOf(bitmap.height - halfHeight - 1, predictedCenter.y.toInt() + searchRadiusPx)
    if (startX > endX || startY > endY) return null

    val templatePixels = IntArray(templateWidth * templateHeight)
    template.getPixels(templatePixels, 0, templateWidth, 0, 0, templateWidth, templateHeight)
    val templateSamples = buildList {
        for (y in 0 until templateHeight step 2) for (x in 0 until templateWidth step 2) {
            add(luma(templatePixels[y * templateWidth + x]).toDouble())
        }
    }
    val templateMean = templateSamples.average()
    val templateVariance = templateSamples.sumOf { value -> (value - templateMean) * (value - templateMean) }
    var bestScore = Double.MAX_VALUE
    var bestCenter: Offset? = null
    val candidates = ArrayList<Pair<Offset, Double>>()
    for (centerY in startY..endY step 2) {
        for (centerX in startX..endX step 2) {
            var sourceSum = 0.0
            val sourceSamples = DoubleArray(templateSamples.size)
            var sampleIndex = 0
            for (y in 0 until templateHeight step 2) {
                for (x in 0 until templateWidth step 2) {
                    val sourceColor = bitmap.getPixel(centerX - halfWidth + x, centerY - halfHeight + y)
                    val value = luma(sourceColor).toDouble()
                    sourceSamples[sampleIndex++] = value
                    sourceSum += value
                }
            }
            val sourceMean = sourceSum / sourceSamples.size.coerceAtLeast(1)
            var numerator = 0.0
            var sourceVariance = 0.0
            sourceSamples.indices.forEach { index ->
                val centeredSource = sourceSamples[index] - sourceMean
                numerator += (templateSamples[index] - templateMean) * centeredSource
                sourceVariance += centeredSource * centeredSource
            }
            val denominator = sqrt(templateVariance * sourceVariance).coerceAtLeast(1e-9)
            val correlation = (numerator / denominator).coerceIn(-1.0, 1.0)
            val error = (1.0 - correlation) * 20.0
            val center = Offset(centerX.toFloat(), centerY.toFloat())
            candidates += center to error
            if (error < bestScore) {
                bestScore = error
                bestCenter = Offset(centerX.toFloat(), centerY.toFloat())
            }
        }
    }
    return bestCenter?.let {
        val exclusionRadius = max(templateWidth, templateHeight).toFloat()
        val secondBestScore = candidates.asSequence()
            .filter { (center, _) -> (center - it).getDistance() >= exclusionRadius }
            .minOfOrNull { (_, error) -> error }
        TemplateMatch(it, bestScore, secondBestScore)
    }
}

private fun findNativeTemplateMatch(
    bitmap: Bitmap,
    template: Bitmap,
    predictedCenter: Offset,
    searchRadiusPx: Int
): TemplateMatch? {
    val framePixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(framePixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val templatePixels = IntArray(template.width * template.height)
    template.getPixels(templatePixels, 0, template.width, 0, 0, template.width, template.height)
    val result = NativeTemplateTracker.localizedSsdMatchNative(
        framePixels = framePixels,
        frameWidth = bitmap.width,
        frameHeight = bitmap.height,
        templatePixels = templatePixels,
        templateWidth = template.width,
        templateHeight = template.height,
        predictedCenterX = predictedCenter.x.toInt(),
        predictedCenterY = predictedCenter.y.toInt(),
        searchRadiusPx = searchRadiusPx
    )
    if (result.size < 3) return null
    return TemplateMatch(Offset(result[0], result[1]), result[2].toDouble(), null)
}

private fun blendOffset(previous: Offset, next: Offset, factor: Float): Offset =
    Offset(
        x = previous.x + (next.x - previous.x) * factor,
        y = previous.y + (next.y - previous.y) * factor
    )

private fun clampOffsetToBitmap(offset: Offset, bitmap: Bitmap, template: Bitmap): Offset {
    val halfWidth = (template.width / 2).toFloat()
    val halfHeight = (template.height / 2).toFloat()
    return Offset(
        x = offset.x.coerceIn(halfWidth, bitmap.width - halfWidth - 1f),
        y = offset.y.coerceIn(halfHeight, bitmap.height - halfHeight - 1f)
    )
}

private fun offsetDelta(from: Offset, to: Offset): Offset = Offset(to.x - from.x, to.y - from.y)

private fun offsetMagnitude(offset: Offset): Float = sqrt((offset.x * offset.x) + (offset.y * offset.y))
