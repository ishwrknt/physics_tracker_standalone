package com.physicstracker.app.live

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.physicstracker.app.live.data.LiveExperimentConverter
import com.physicstracker.app.live.data.LiveSessionDao
import com.physicstracker.app.live.data.LiveSessionEntity
import com.physicstracker.app.live.physics.KinematicsLiveModule
import com.physicstracker.app.live.physics.LivePhysicsModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private const val GRAPH_WINDOW_US = 12_000_000L
private const val TRAIL_SIZE = 60
private const val MAX_RECORDED_SAMPLES = 24_000
private const val CONFIDENCE_LOST = 0.30
private const val CONFIDENCE_RECORD = 0.5
/** Missed frames tolerated in REACQUIRING before declaring OBJECT_LOST (~3s at 30fps). */
private const val REACQUIRE_MAX_MISSES = 90

data class LiveUiState(
    val state: LiveTrackingState = LiveTrackingState.IDLE,
    val cameraError: String? = null,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val positionPx: Pair<Float, Float>? = null,
    val predictedPx: Pair<Float, Float>? = null,
    val estimatedRadiusPx: Float = 14f,
    val trail: List<Pair<Float, Float>> = emptyList(),
    val confidence: Double = 0.0,
    val reacquireProgress: Float = 0f,
    // calibration
    val calibration: LiveCalibration? = null,
    val calibrationConfirmed: Boolean = false,
    val calibrationPoints: List<Pair<Float, Float>> = emptyList(),
    val scaleEstimate: LiveScaleEstimator.Estimate? = null,
    // physics
    val current: LivePhysicsPoint? = null,
    val physicsAvailable: Boolean = false,
    // run
    val recording: Boolean = false,
    val reviewMode: Boolean = false,
    val recordedCount: Int = 0,
    val recordedDurationUs: Long = 0L,
    val stats: List<Pair<String, Double?>> = emptyList(),
    val graphPoints: List<LivePhysicsPoint> = emptyList(),
    /** Full recorded physics series for the shared analysis workspace (throttled snapshots). */
    val analysisPoints: List<LivePhysicsPoint> = emptyList(),
    val analysisTick: Int = 0,
    // display toggles
    val showOverlay: Boolean = true,
    val showTrajectory: Boolean = true
)

/**
 * Owns the isolated real-time live-tracking pipeline.
 *
 * Flow: tap target → live readouts → confirm scale (auto estimate or stick) → START →
 * RECORDING → pause / stop → review → save. Saved runs become first-class experiments
 * through [LiveExperimentConverter]. Object loss enters REACQUIRING then OBJECT_LOST;
 * long-press reselection does not discard run data.
 *
 * Threading: all frame processing runs on a dedicated single-thread executor; frames are
 * dropped (never queued) when busy; pipeline exceptions are contained per frame.
 */
@HiltViewModel
class LiveTrackingViewModel @Inject constructor(
    private val liveSessionDao: LiveSessionDao,
    private val experimentConverter: LiveExperimentConverter
) : ViewModel() {

    private data class RecordedSample(
        val raw: LiveRawSample,
        val physics: LivePhysicsPoint?
    )

    val sessions = liveSessionDao.getSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(
        LiveUiState(state = LiveTrackingState.AWAITING_TARGET)
    )
    val uiState: StateFlow<LiveUiState> = _uiState

    private val pipeline = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "live-tracking-pipeline").apply { isDaemon = true }
    }
    private val busy = AtomicBoolean(false)

    // ---- pipeline-thread-only state ----
    private val converter = YuvBitmapConverter()
    private val tracker: LiveTracker = runCatching {
        OpenCvLiveTracker()
    }.getOrElse { failure ->
        android.util.Log.w("LiveTracking", "OpenCV unavailable, falling back to robust tracker", failure)
        RobustLiveTracker()
    }
    private val kinematics = LiveKinematicsEngine(metersPerPixel = 1.0)
    private val physicsModule: LivePhysicsModule = KinematicsLiveModule()
    private val recorded = RollingLiveBuffer<RecordedSample>(MAX_RECORDED_SAMPLES)
    private val physicsTrail = RollingLiveBuffer<LivePhysicsPoint>(MAX_RECORDED_SAMPLES)
    private val trailPx = RollingLiveBuffer<Pair<Float, Float>>(TRAIL_SIZE)
    private var lastFrame: android.graphics.Bitmap? = null
    private var sessionStartNs: Long? = null
    private var currentRotation: Int = -1
    private var firstRecordedUs: Long? = null
    private var lastRecordedUs: Long? = null
    // camera optics for automatic scale estimation
    private var focalLengthMm: Double? = null
    private var sensorWidthMm: Double? = null
    private var focusQuality = LiveScaleEstimator.FocusQuality.UNKNOWN
    @Volatile private var focusDistanceDiopters: Double? = null

    @Volatile private var recording = false
    @Volatile private var seeded = false
    @Volatile private var state: LiveTrackingState = LiveTrackingState.AWAITING_TARGET
    @Volatile private var calibration: LiveCalibration? = null
    @Volatile private var calibrationConfirmed = false
    /** (elapsedUs, metersPerPixel, source) — preserved so recalibration never corrupts earlier data. */
    private val calibrationHistory = mutableListOf<Triple<Long, Double, String>>()

    // ===================== camera/scale inputs =====================

    fun onCameraCharacteristics(
        focalLengths: FloatArray?,
        sensorPhysicalWidthMm: Double?,
        focusDistanceCalibration: Int?
    ) {
        focalLengthMm = focalLengths?.firstOrNull()?.takeIf { it > 0 }?.toDouble()
        sensorWidthMm = sensorPhysicalWidthMm?.takeIf { it > 0 }
        focusQuality = LiveScaleEstimator.mapFocusCalibration(focusDistanceCalibration)
    }

    fun onFocusDistance(focusDistanceDiopters: Double?) {
        this.focusDistanceDiopters = focusDistanceDiopters?.takeIf { it > 0 }
    }

    fun onCameraError(message: String) {
        state = LiveTrackingState.CAMERA_ERROR
        _uiState.value = _uiState.value.copy(state = state, cameraError = message)
    }

    // ===================== calibration =====================

    fun beginCalibration() {
        recording = false.also { }
        state = LiveTrackingState.CALIBRATING
        pipeline.execute { kinematics.breakSegment() }
        _uiState.value = _uiState.value.copy(
            state = state, calibrationPoints = emptyList(), recording = false
        )
    }

    fun onCalibrationTap(xPx: Float, yPx: Float) {
        val current = _uiState.value
        val points = when {
            current.calibrationPoints.size < 2 -> current.calibrationPoints + (xPx to yPx)
            else -> listOf(xPx to yPx)
        }
        _uiState.value = current.copy(calibrationPoints = points)
    }

    /** Drag-move a calibration stick endpoint (user can adjust the stick after placing it). */
    fun moveCalibrationPoint(index: Int, xPx: Float, yPx: Float) {
        val current = _uiState.value.calibrationPoints
        if (index !in current.indices) return
        val updated = current.toMutableList().also { it[index] = xPx to yPx }
        _uiState.value = _uiState.value.copy(calibrationPoints = updated)
    }

    fun applyCalibration(knownLength: Double, unit: String) {
        val points = _uiState.value.calibrationPoints
        if (points.size < 2) return
        val result = LiveCalibration.create(
            points[0].first, points[0].second, points[1].first, points[1].second, knownLength, unit
        ) ?: return
        applyConfirmedCalibration(result, source = "stick")
    }

    /** Confirm the camera-derived automatic scale estimate. */
    fun confirmScaleEstimate() {
        val estimate = _uiState.value.scaleEstimate ?: return
        val mpp = estimate.metersPerPixel
        val result = LiveCalibration(
            stickP1x = 0f, stickP1y = 0f,
            stickP2x = (1.0 / mpp).toFloat(), stickP2y = 0f,
            knownDistanceMeters = 1.0, unit = "m",
            metersPerPixel = mpp, pixelsPerMeter = 1.0 / mpp
        )
        applyConfirmedCalibration(result, source = "auto")
    }

    /** Drop the current scale and re-open the automatic estimate for confirmation. */
    fun recalibrateAuto() {
        calibrationConfirmed = false
        _uiState.value = _uiState.value.copy(calibrationConfirmed = false, scaleEstimate = null)
    }

    private fun applyConfirmedCalibration(result: LiveCalibration, source: String) {
        val hadCalibration = calibrationConfirmed
        calibration = result
        calibrationConfirmed = true
        pipeline.execute {
            // Mid-run recalibration: break forward filtering but never rewrite earlier data.
            if (hadCalibration && recording) kinematics.breakSegment() else kinematics.reset()
            kinematics.setScale(result.metersPerPixel)
        }
        synchronized(calibrationHistory) {
            calibrationHistory.add(Triple(lastRecordedUs ?: 0L, result.metersPerPixel, source))
        }
        if (state == LiveTrackingState.CALIBRATING) {
            state = if (recording) LiveTrackingState.RECORDING
                    else if (seeded) LiveTrackingState.TRACKING
                    else LiveTrackingState.AWAITING_TARGET
        }
        _uiState.value = _uiState.value.copy(
            state = state,
            calibration = result,
            calibrationConfirmed = true
        )
    }

    fun cancelCalibration() {
        if (state != LiveTrackingState.CALIBRATING) return
        state = when {
            recording -> LiveTrackingState.RECORDING
            seeded -> LiveTrackingState.TRACKING
            else -> LiveTrackingState.IDLE
        }
        _uiState.value = _uiState.value.copy(state = state, calibrationPoints = emptyList())
    }

    // ===================== run control =====================

    /** Fresh entry: camera visible, waiting for the user to tap the object. */
    fun armStart() {
        pipeline.execute {
            tracker.release()
            kinematics.reset()
            physicsModule.reset()
            recorded.clear()
            physicsTrail.clear()
            trailPx.clear()
            lastFrame = null
            sessionStartNs = null
            seeded = false
            firstRecordedUs = null
            lastRecordedUs = null
            synchronized(calibrationHistory) { calibrationHistory.clear() }
        }
        recording = false
        calibrationConfirmed = false
        calibration = null
        state = LiveTrackingState.AWAITING_TARGET
        _uiState.value = _uiState.value.copy(
            state = state,
            positionPx = null, predictedPx = null, trail = emptyList(),
            confidence = 0.0, reacquireProgress = 0f,
            calibration = null, calibrationConfirmed = false, calibrationPoints = emptyList(),
            current = null, physicsAvailable = false,
            recordedCount = 0, recordedDurationUs = 0L,
            recording = false, reviewMode = false,
            stats = emptyList(), graphPoints = emptyList(),
            analysisPoints = emptyList(), analysisTick = 0
        )
    }

    /** Long-press: re-tap the object WITHOUT discarding the recorded run. */
    fun reselectObject() {
        pipeline.execute {
            tracker.release()
            kinematics.breakSegment()
            trailPx.clear()
            seeded = false
        }
        state = LiveTrackingState.AWAITING_TARGET
        _uiState.value = _uiState.value.copy(
            state = state,
            positionPx = null, predictedPx = null, trail = emptyList(),
            confidence = 0.0, reacquireProgress = 0f
        )
    }

    /** Seed the tracker on an upright analysis-frame coordinate (mapped from the tap). */
    fun seedAt(xPx: Float, yPx: Float) {
        android.util.Log.i("LiveTracking", "seedAt ($xPx, $yPx)")
        pipeline.execute {
            val frame = lastFrame
            if (frame == null) {
                android.util.Log.w("LiveTracking", "seedAt: no frame yet")
                return@execute
            }
            val ok = tracker.seed(frame, xPx, yPx)
            android.util.Log.i("LiveTracking", "seed result=$ok radius=${tracker.estimatedRadiusPx}")
            if (ok) {
                seeded = true
                kinematics.breakSegment()
                state = if (recording) LiveTrackingState.RECORDING else LiveTrackingState.TRACKING
                _uiState.value = _uiState.value.copy(
                    state = state,
                    confidence = 1.0,
                    positionPx = xPx to yPx,
                    predictedPx = xPx to yPx,
                    estimatedRadiusPx = tracker.estimatedRadiusPx
                )
            }
        }
    }

    /** START: begin recording physics from this moment (positions re-origin here). */
    fun startRecording() {
        if (!seeded && state != LiveTrackingState.TRACKING) return
        pipeline.execute {
            kinematics.reset()
            physicsModule.reset()
            sessionStartNs = null
        }
        recorded.clear()
        physicsTrail.clear()
        firstRecordedUs = null
        lastRecordedUs = null
        recording = true
        state = LiveTrackingState.RECORDING
        _uiState.value = _uiState.value.copy(
            state = state, recording = true, reviewMode = false,
            recordedCount = 0, recordedDurationUs = 0L,
            stats = emptyList(), graphPoints = emptyList()
        )
    }

    fun pause() {
        if (state == LiveTrackingState.RECORDING || state == LiveTrackingState.REACQUIRING) {
            recording = false
            pipeline.execute { kinematics.breakSegment() }
            state = LiveTrackingState.PAUSED
            _uiState.value = _uiState.value.copy(state = state, recording = false)
        }
    }

    fun resume() {
        if (state == LiveTrackingState.PAUSED) {
            recording = true
            state = if (seeded) LiveTrackingState.RECORDING else LiveTrackingState.AWAITING_TARGET
            _uiState.value = _uiState.value.copy(state = state, recording = true)
        }
    }

    /** Stop acquisition; keep data for review and saving. */
    fun stop() {
        recording = false
        pipeline.execute { kinematics.flush() }
        state = LiveTrackingState.TRACKING
        publishReviewSnapshot()
    }

    fun resetRun() = armStart()

    /** Leave review mode and continue live tracking without recording. */
    fun dismissReview() {
        _uiState.value = _uiState.value.copy(reviewMode = false, graphPoints = emptyList())
    }

    fun toggleOverlay() {
        _uiState.value = _uiState.value.copy(showOverlay = !_uiState.value.showOverlay)
    }

    fun toggleTrajectory() {
        _uiState.value = _uiState.value.copy(showTrajectory = !_uiState.value.showTrajectory)
    }

    fun saveSession(name: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            // Snapshot on the pipeline thread so buffers are never read mid-frame.
            val rows = pipeline.submit(Callable { recorded.toList() }).get()
            if (rows.isEmpty()) return@launch
            saveSessionRows(name.ifBlank { "Live session" }, rows, onSaved)
        }
    }

    private val _pendingCsv = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
    val pendingCsv: kotlinx.coroutines.flow.SharedFlow<String> = _pendingCsv

    /** Enqueue a CSV share of the current run (used by the analysis EXPORT module). */
    fun shareCurrentRunCsv() {
        viewModelScope.launch {
            currentRunCsv()?.let { _pendingCsv.tryEmit(it) }
        }
    }

    /** CSV of the current recorded run (same schema as saved sessions). */
    suspend fun currentRunCsv(): String? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val rows = pipeline.submit(Callable { recorded.toList() }).get()
        if (rows.isEmpty()) null else buildRunCsv(rows)
    }

    private fun buildRunCsv(rows: List<RecordedSample>): String = buildString(rows.size * 48) {
            rows.forEach { row ->
                val p = row.physics
                append(row.raw.elapsedUs).append(',')
                append(fmt(row.raw.xPx)).append(',')
                append(fmt(row.raw.yPx)).append(',')
                append(String.format(Locale.US, "%.3f", row.raw.confidence)).append(',')
                append(p?.xMeters?.let(::fmt) ?: "").append(',')
                append(p?.yMeters?.let(::fmt) ?: "").append(',')
                append(p?.vxMetersPerSec?.let(::fmt) ?: "").append(',')
                append(p?.vyMetersPerSec?.let(::fmt) ?: "").append(',')
                append(p?.axMetersPerSec2?.let(::fmt) ?: "").append(',')
                append(p?.ayMetersPerSec2?.let(::fmt) ?: "").append(',')
                append(p?.speedMetersPerSec?.let(::fmt) ?: "").append(',')
                append(p?.accelerationMetersPerSec2?.let(::fmt) ?: "")
                append('\n')
            }
        }

    private fun saveSessionRows(name: String, rows: List<RecordedSample>, onSaved: () -> Unit) {
        val cal = calibration
        val first = rows.first().raw.elapsedUs
        val last = rows.last().raw.elapsedUs
        val csv = buildRunCsv(rows)
        val stickLength = cal?.let {
            kotlin.math.hypot(
                (it.stickP2x - it.stickP1x).toDouble(),
                (it.stickP2y - it.stickP1y).toDouble()
            )
        }
        val historyJson = synchronized(calibrationHistory) {
            calibrationHistory.joinToString(
                prefix = "{\"calibrationHistory\":[",
                postfix = "]}"
            ) { h ->
                "{\"t_us\":${h.first},\"mpp\":${h.second},\"source\":\"${h.third}\"}"
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            liveSessionDao.insert(
                LiveSessionEntity(
                    name = name,
                    createdAtEpochMs = System.currentTimeMillis(),
                    durationUs = (last - first).coerceAtLeast(0L),
                    sampleCount = rows.size,
                    frameWidth = converter.uprightWidth,
                    frameHeight = converter.uprightHeight,
                    metersPerPixel = cal?.metersPerPixel,
                    stickPixelLength = stickLength,
                    knownDistanceMeters = cal?.knownDistanceMeters,
                    calibrationUnit = cal?.unit,
                    physicsMode = physicsModule.modeId,
                    trackerAlgorithm = tracker.algorithmId,
                    samplesCsv = csv,
                    metaJson = historyJson
                )
            )
            // A live session becomes a first-class experiment: same analysis engine.
            experimentConverter.convert(
                name = name,
                samples = rows.map { row ->
                    LiveExperimentConverter.LiveSampleRow(
                        elapsedUs = row.raw.elapsedUs,
                        xPx = row.raw.xPx, yPx = row.raw.yPx,
                        confidence = row.raw.confidence, matchScore = null
                    )
                },
                calibration = cal,
                frameWidth = converter.uprightWidth,
                frameHeight = converter.uprightHeight,
                trackerAlgorithm = tracker.algorithmId
            )
            onSaved()
        }
    }

    fun deleteSession(session: LiveSessionEntity) {
        viewModelScope.launch { liveSessionDao.delete(session) }
    }

    // ===================== frame pipeline =====================

    /** CameraX ImageAnalysis callback entry point. Never blocks. */
    fun onFrame(image: ImageProxy) {
        if (!busy.compareAndSet(false, true)) {
            image.close()
            return
        }
        pipeline.execute {
            try {
                processFrame(image)
            } catch (t: Throwable) {
                android.util.Log.w("LiveTracking", "frame processing failed", t)
            } finally {
                image.close()
                busy.set(false)
            }
        }
    }

    private fun processFrame(image: ImageProxy) {
        val frame = converter.convert(image) ?: return
        lastFrame = frame

        // Rotation changes: remap tracked coordinates into the new upright frame and
        // restart filtering/origin — physical axes genuinely changed orientation.
        val rotation = image.imageInfo.rotationDegrees
        if (currentRotation < 0) {
            currentRotation = rotation
        } else if (rotation != currentRotation) {
            tracker.remapAfterRotation(currentRotation, rotation)
            kinematics.reset()
            trailPx.clear()
            currentRotation = rotation
        }

        if (state == LiveTrackingState.IDLE ||
            state == LiveTrackingState.CALIBRATING ||
            state == LiveTrackingState.CAMERA_ERROR
        ) {
            publishIdleFrame(frame, image)
            return
        }
        if (state == LiveTrackingState.AWAITING_TARGET) {
            publishIdleFrame(frame, image)
            updateScaleEstimate(image)
            return
        }

        val timestampNs = image.imageInfo.timestamp
        var start = sessionStartNs ?: timestampNs.also { sessionStartNs = it }
        if (timestampNs < start) {
            // Camera rebind/rotation can restart the sensor clock: rebase honestly
            // instead of emitting negative timestamps.
            sessionStartNs = timestampNs
            start = timestampNs
            kinematics.breakSegment()
        }
        val elapsedUs = (timestampNs - start) / 1_000L

        val result = tracker.track(frame, elapsedUs)
        val confidence = result?.confidence ?: 0.0
        updateScaleEstimate(image)

        when {
            result == null -> handleMiss(frame, trackerCounted = true)
            confidence < CONFIDENCE_LOST -> handleMiss(frame, trackerCounted = false)
            confidence < CONFIDENCE_RECORD -> {
                handleMeasurement(frame, result, elapsedUs, recordIt = false,
                    interimState = LiveTrackingState.LOW_CONFIDENCE)
            }
            state == LiveTrackingState.PAUSED -> {
                _uiState.value = _uiState.value.copy(
                    frameWidth = frame.width, frameHeight = frame.height,
                    positionPx = result.xPx.toFloat() to result.yPx.toFloat(),
                    predictedPx = tracker.predictedCenter?.let { it.first.toFloat() to it.second.toFloat() },
                    confidence = confidence
                )
            }
            else -> {
                handleMeasurement(frame, result, elapsedUs, recordIt = recording,
                    interimState = if (recording) LiveTrackingState.RECORDING else LiveTrackingState.TRACKING)
            }
        }
    }

    private fun handleMiss(frame: android.graphics.Bitmap, trackerCounted: Boolean) {
        if (!trackerCounted) tracker.noteExternalMiss()
        kinematics.breakSegment()
        if (tracker.consecutiveMisses % 30 == 0) {
            android.util.Log.d("LiveTracking", "miss streak=${tracker.consecutiveMisses} state=$state")
        }
        val misses = tracker.consecutiveMisses
        val newState = if (misses < REACQUIRE_MAX_MISSES) {
            LiveTrackingState.REACQUIRING
        } else {
            LiveTrackingState.OBJECT_LOST
        }
        val effectiveState = if (state == LiveTrackingState.PAUSED) LiveTrackingState.PAUSED else newState
        state = effectiveState
        _uiState.value = _uiState.value.copy(
            state = effectiveState,
            frameWidth = frame.width, frameHeight = frame.height,
            predictedPx = tracker.predictedCenter?.let { it.first.toFloat() to it.second.toFloat() },
            trail = emptyList(),
            confidence = 0.0,
            reacquireProgress = (misses / REACQUIRE_MAX_MISSES.toFloat()).coerceIn(0f, 1f),
            recordedCount = recorded.size
        )
    }

    private fun handleMeasurement(
        frame: android.graphics.Bitmap,
        result: LiveTrackResult,
        elapsedUs: Long,
        recordIt: Boolean,
        interimState: LiveTrackingState
    ) {
        state = interimState
        if (interimState == LiveTrackingState.LOW_CONFIDENCE) {
            // Never feed uncertain measurements into the physics stream.
            kinematics.breakSegment()
            _uiState.value = _uiState.value.copy(
                state = interimState,
                frameWidth = frame.width, frameHeight = frame.height,
                positionPx = result.xPx.toFloat() to result.yPx.toFloat(),
                predictedPx = tracker.predictedCenter?.let { it.first.toFloat() to it.second.toFloat() },
                estimatedRadiusPx = result.estimatedRadiusPx,
                confidence = result.confidence,
                reacquireProgress = 0f,
                recordedCount = recorded.size,
                physicsAvailable = false
            )
            return
        }
        val raw = LiveRawSample(elapsedUs, result.xPx, result.yPx, result.confidence)
        val point = kinematics.onSample(raw)
        if (recordIt && recorded.size < MAX_RECORDED_SAMPLES) {
            recorded.add(RecordedSample(raw, point))
            point?.let {
                physicsTrail.add(it)
                physicsModule.onPoint(it)
            }
        }
        trailPx.add(result.xPx.toFloat() to result.yPx.toFloat())
        if (recordIt) {
            if (firstRecordedUs == null) firstRecordedUs = elapsedUs
            lastRecordedUs = elapsedUs
        }
        val duration = listOfNotNull(firstRecordedUs, lastRecordedUs)
            .let { values -> if (values.size == 2) values[1] - values[0] else 0L }
        val physicsOk = calibrationConfirmed && point?.vxMetersPerSec != null
        _uiState.value = _uiState.value.copy(
            state = interimState,
            frameWidth = frame.width, frameHeight = frame.height,
            positionPx = result.xPx.toFloat() to result.yPx.toFloat(),
            predictedPx = tracker.predictedCenter?.let { it.first.toFloat() to it.second.toFloat() },
            estimatedRadiusPx = result.estimatedRadiusPx,
            trail = trailPx.toList(),
            confidence = result.confidence,
            reacquireProgress = 0f,
            current = point,
            physicsAvailable = physicsOk,
            recordedCount = recorded.size,
            recordedDurationUs = duration,
            recording = recording,
            stats = physicsModule.stats(),
            graphPoints = if (recording || recorded.size > 0) {
                physicsTrail.window(GRAPH_WINDOW_US) { it.elapsedUs }
            } else emptyList(),
            analysisPoints = if (recorded.size % 30 == 0) physicsTrail.toList() else _uiState.value.analysisPoints,
            analysisTick = if (recorded.size % 30 == 0) _uiState.value.analysisTick + 1 else _uiState.value.analysisTick
        )
    }

    /** Refreshes the camera-optics scale estimate whenever inputs allow. */
    private fun updateScaleEstimate(image: ImageProxy) {
        if (calibrationConfirmed) return
        val focal = focalLengthMm ?: return
        val sensorW = sensorWidthMm ?: return
        if (image.width <= 0 || image.cropRect.width() <= 0) return
        val estimate = LiveScaleEstimator.estimate(
            focalLengthMm = focal,
            sensorWidthMm = sensorW,
            sensorFullWidthPx = image.width,
            cropWidthSensorPx = image.cropRect.width(),
            analysisWidthPx = converter.uprightWidth.takeIf { it > 0 } ?: return,
            focusDistanceDiopters = focusDistanceDiopters,
            focusQuality = focusQuality
        ) ?: return
        if (_uiState.value.scaleEstimate == null ||
            kotlin.math.abs(estimate.metersPerPixel / _uiState.value.scaleEstimate!!.metersPerPixel - 1.0) > 0.05
        ) {
            _uiState.value = _uiState.value.copy(scaleEstimate = estimate)
        }
    }

    private fun publishIdleFrame(frame: android.graphics.Bitmap, image: ImageProxy) {
        _uiState.value = _uiState.value.copy(frameWidth = frame.width, frameHeight = frame.height)
    }

    private fun publishReviewSnapshot() {
        val duration = listOfNotNull(firstRecordedUs, lastRecordedUs)
            .let { values -> if (values.size == 2) values[1] - values[0] else 0L }
        _uiState.value = _uiState.value.copy(
            state = state,
            recording = false,
            reviewMode = recorded.size > 0,
            recordedCount = recorded.size,
            recordedDurationUs = duration,
            stats = physicsModule.stats(),
            graphPoints = physicsTrail.tail(4096),
            analysisPoints = physicsTrail.toList(),
            analysisTick = _uiState.value.analysisTick + 1
        )
    }

    private fun fmt(value: Double): String = String.format(Locale.US, "%.5f", value)

    override fun onCleared() {
        pipeline.execute {
            tracker.release()
            converter.release()
        }
        pipeline.shutdown()
        super.onCleared()
    }
}
