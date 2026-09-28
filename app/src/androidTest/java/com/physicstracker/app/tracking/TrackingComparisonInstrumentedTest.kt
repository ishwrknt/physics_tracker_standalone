package com.physicstracker.app.tracking

import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.math.ceil

/** Opt-in benchmark of actual Kotlin engines, not a Python reimplementation.
 * Frames must be independently annotated, full-resolution PNGs with recorded presentation times.
 * It tests tracking only, not the video decoder or UI. See tools/tracking_benchmark/README.md.
 */
class TrackingComparisonInstrumentedTest {
    @Test fun compareAnnotatedFrames() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val root = File(instrumentation.targetContext.getExternalFilesDir(null), "tracking-benchmark")
        val manifestFile = File(root, "manifest.json")
        assumeTrue("No consented, annotated benchmark installed", manifestFile.isFile)
        val manifest = JSONObject(manifestFile.readText())
        require(manifest.getBoolean("consented"))
        require(manifest.getString("annotation_provenance").isNotBlank())
        require(manifest.getString("split") in listOf("development", "held-out"))
        val clips = manifest.getJSONArray("clips")
        require(clips.length() > 0)
        val rows = StringBuilder("clip,variant,frame,timestamp_us,visible,measured,x,y,error_px,tracking_ns,reason\n")
        val summaries = StringBuilder("clip,variant,visible_frames,visible_measured,coverage,mean_px,median_px,p95_px,max_px,large_errors,measured_hidden,tracking_ms\n")
        for (clipIndex in 0 until clips.length()) {
            val clip = clips.getJSONObject(clipIndex)
            val id = clip.getString("id")
            require(id.matches(Regex("[a-zA-Z0-9_-]+")))
            val reference = TargetReference.valueOf(clip.getString("reference"))
            val seedPoint = Offset(clip.getDouble("seed_x").toFloat(), clip.getDouble("seed_y").toFloat())
            val frames = clip.getJSONArray("frames")
            require(frames.length() >= 2)
            val largeErrorPx = clip.getDouble("large_error_px")
            require(largeErrorPx.isFinite() && largeErrorPx > 0)
            for (variant in listOf("original-classic", "initial-hybrid", "blur-experimental")) {
                val classic = ClassicTrackerEngine(ClassicTrackerMode.ACCURATE,
                    candidatePolicy = if (variant == "original-classic") ClassicCandidatePolicy.ORIGINAL_BASELINE
                    else ClassicCandidatePolicy.INITIAL_HYBRID)
                val blur = BlurAwareTracker()
                var classicState: ClassicTrackingSeed? = null
                var blurState: BlurTrackingState? = null
                var paused = false
                var previousPts = -1L
                var visibleFrames = 0
                var hiddenMeasured = 0
                var trackingNanos = 0L
                val errors = mutableListOf<Double>()
                for (frameIndex in 0 until frames.length()) {
                    val annotation = frames.getJSONObject(frameIndex)
                    val timestampUs = annotation.getLong("timestamp_us")
                    require(timestampUs > previousPts)
                    previousPts = timestampUs
                    val image = File(root, annotation.getString("image")).canonicalFile
                    require(image.toPath().startsWith(root.canonicalFile.toPath()))
                    val bitmap = requireNotNull(BitmapFactory.decodeFile(image.path))
                    try {
                        if (frameIndex == 0) {
                            if (variant == "blur-experimental") blurState = blur.seed(bitmap.toTrackingFrame(), seedPoint, timestampUs, reference)
                            else classicState = classic.seed(bitmap, seedPoint)
                            paused = if (variant == "blur-experimental") blurState == null else classicState == null
                            continue // Seed is user input, not a measured benchmark result.
                        }
                        val started = System.nanoTime()
                        var measured: Offset? = null
                        var reason = "paused"
                        if (!paused && variant == "blur-experimental") {
                            val step = blur.track(bitmap.toTrackingFrame(), requireNotNull(blurState), timestampUs)
                            blurState = step.state
                            measured = step.measuredPosition
                            paused = step.state.phase == BlurTrackingPhase.NEEDS_CORRECTION
                            reason = step.reason
                        } else if (!paused) {
                            val step = classic.trackFrame(bitmap, requireNotNull(classicState))
                            if (step == null) paused = true else {
                                val oldTemplate = classicState?.template
                                classicState = step.seed
                                if (oldTemplate !== step.seed.template) oldTemplate?.recycle()
                                measured = step.seed.center
                                reason = step.trackingSource
                            }
                        }
                        val elapsed = System.nanoTime() - started
                        trackingNanos += elapsed
                        val visible = annotation.getBoolean("visible")
                        var error: Double? = null
                        if (visible) {
                            visibleFrames++
                            val truth = Offset(annotation.getDouble("x").toFloat(), annotation.getDouble("y").toFloat())
                            require(truth.x.isFinite() && truth.y.isFinite())
                            if (measured != null) {
                                error = (measured - truth).getDistance().toDouble()
                                errors += error
                            }
                        } else if (measured != null) hiddenMeasured++
                        rows.append(listOf(id, variant, frameIndex, timestampUs, visible, measured != null,
                            measured?.x ?: "", measured?.y ?: "", error ?: "", elapsed, reason.replace(',', ';')).joinToString(",")).append('\n')
                    } finally {
                        bitmap.recycle()
                    }
                }
                classicState?.template?.recycle()
                val sorted = errors.sorted()
                fun percentile(fraction: Double): Any = if (sorted.isEmpty()) "" else
                    sorted[(ceil(fraction * sorted.size).toInt() - 1).coerceIn(sorted.indices)]
                summaries.append(listOf(id, variant, visibleFrames, errors.size,
                    if (visibleFrames == 0) "" else errors.size.toDouble() / visibleFrames,
                    if (errors.isEmpty()) "" else errors.average(), percentile(0.5), percentile(0.95),
                    sorted.lastOrNull() ?: "", errors.count { it > largeErrorPx }, hiddenMeasured,
                    trackingNanos / 1_000_000.0).joinToString(",")).append('\n')
            }
        }
        // Only generated benchmark reports are written; media and annotations remain untouched.
        File(root, "frame-results.csv").writeText(rows.toString())
        File(root, "summary.csv").writeText(summaries.toString())
    }
}
