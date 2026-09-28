@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.physicstracker.app.ui

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.physicstracker.app.data.CalibrationEntity
import com.physicstracker.app.data.PhysicsRepository
import com.physicstracker.app.data.TrackPointEntity
import com.physicstracker.app.data.ProjectEntity
import com.physicstracker.app.data.TrackerEntity
import com.physicstracker.app.tracking.VideoTimelineIndex
import com.physicstracker.app.tracking.VideoTimingInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PhysicsRepository,
    liveSessionDao: com.physicstracker.app.live.data.LiveSessionDao
) : ViewModel() {

    /** Saved live-tracking sessions for the home HISTORY half. */
    val liveSessions = liveSessionDao.getSessions()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
        repeatMode = Player.REPEAT_MODE_OFF
    }

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _frameDurationMs = MutableStateFlow(33L)
    val frameDurationMs: StateFlow<Long> = _frameDurationMs.asStateFlow()
    private val _videoTimingInfo = MutableStateFlow(VideoTimingInfo())
    val videoTimingInfo: StateFlow<VideoTimingInfo> = _videoTimingInfo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _playWhenReady = MutableStateFlow(false)
    val playWhenReady: StateFlow<Boolean> = _playWhenReady.asStateFlow()
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    private val _loopPlayback = MutableStateFlow(false)
    val loopPlayback: StateFlow<Boolean> = _loopPlayback.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _projectId = MutableStateFlow(-1L)
    val projectId: StateFlow<Long> = _projectId.asStateFlow()

    private val _trimStart = MutableStateFlow(0L)
    val trimStart: StateFlow<Long> = _trimStart.asStateFlow()

    private val _trimEnd = MutableStateFlow(C.TIME_END_OF_SOURCE)
    val trimEnd: StateFlow<Long> = _trimEnd.asStateFlow()

    private var currentVideoUri: String? = null
    private val undoStack = mutableListOf<EditAction>()
    private val redoStack = mutableListOf<EditAction>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    private val _lastCheckpointSavedAt = MutableStateFlow<Long?>(null)
    val lastCheckpointSavedAt: StateFlow<Long?> = _lastCheckpointSavedAt.asStateFlow()
    private var wasPlayingBeforeScrub = false
    private var isScrubbing = false
    private var lastScrubSeekAtMs = 0L
    private var lastScrubSeekPositionMs = Long.MIN_VALUE
    private var pendingScrubPositionMs: Long? = null
    private var resumeAfterScrubJob: Job? = null

    private sealed interface EditAction {
        suspend fun undo(viewModel: VideoPlayerViewModel)
        suspend fun redo(viewModel: VideoPlayerViewModel)
    }

    private data class PointAdded(val point: TrackPointEntity) : EditAction {
        override suspend fun undo(viewModel: VideoPlayerViewModel) {
            viewModel.deletePointInternal(point.id)
        }

        override suspend fun redo(viewModel: VideoPlayerViewModel) {
            viewModel.insertPointInternal(point)
        }
    }

    private data class PointDeleted(val point: TrackPointEntity) : EditAction {
        override suspend fun undo(viewModel: VideoPlayerViewModel) {
            viewModel.insertPointInternal(point)
        }

        override suspend fun redo(viewModel: VideoPlayerViewModel) {
            viewModel.deletePointInternal(point.id)
        }
    }

    private data class PointUpdated(
        val before: TrackPointEntity,
        val after: TrackPointEntity
    ) : EditAction {
        override suspend fun undo(viewModel: VideoPlayerViewModel) {
            viewModel.insertPointInternal(before)
        }

        override suspend fun redo(viewModel: VideoPlayerViewModel) {
            viewModel.insertPointInternal(after)
        }
    }

    private data class PointsCleared(val points: List<TrackPointEntity>) : EditAction {
        override suspend fun undo(viewModel: VideoPlayerViewModel) = viewModel.restorePointsInternal(points)
        override suspend fun redo(viewModel: VideoPlayerViewModel) {
            val projectId = points.firstOrNull()?.projectId ?: return
            viewModel.deleteAllPointsInternal(projectId)
        }
    }

    private data class PointsReplaced(
        val removed: List<TrackPointEntity>,
        val added: List<TrackPointEntity>
    ) : EditAction {
        override suspend fun undo(viewModel: VideoPlayerViewModel) {
            viewModel.replacePointsInternal(added, removed)
        }

        override suspend fun redo(viewModel: VideoPlayerViewModel) {
            viewModel.replacePointsInternal(removed, added)
        }
    }

    private data class CalibrationChanged(
        val before: CalibrationEntity?,
        val after: CalibrationEntity?
    ) : EditAction {
        override suspend fun undo(viewModel: VideoPlayerViewModel) = viewModel.applyCalibrationInternal(before)
        override suspend fun redo(viewModel: VideoPlayerViewModel) = viewModel.applyCalibrationInternal(after)
    }

    private data class TrimChanged(
        val beforeStart: Long,
        val beforeEnd: Long,
        val afterStart: Long,
        val afterEnd: Long
    ) : EditAction {
        override suspend fun undo(viewModel: VideoPlayerViewModel) =
            viewModel.applyTrimInternal(beforeStart, beforeEnd)

        override suspend fun redo(viewModel: VideoPlayerViewModel) =
            viewModel.applyTrimInternal(afterStart, afterEnd)
    }

    private suspend fun touchActiveProject() {
        val id = _projectId.value
        if (id == -1L) return
        repository.touchProject(id, System.currentTimeMillis())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val trackPoints: StateFlow<List<TrackPointEntity>> = _projectId
        .flatMapLatest { id ->
            if (id == -1L) flowOf(emptyList())
            else repository.getPointsForProject(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val calibration: StateFlow<CalibrationEntity?> = _projectId
        .flatMapLatest { id ->
            if (id == -1L) flowOf(null)
            else repository.getCalibrationForProject(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val trackers: StateFlow<List<TrackerEntity>> = _projectId
        .flatMapLatest { id -> if (id == -1L) flowOf(emptyList()) else repository.getTrackersForProject(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calibrationMetrics: StateFlow<CalibrationMetrics?> = calibration
        .map { current -> if (current == null) null else calibrationMetrics(current) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val calibratedTrackPoints: StateFlow<List<CalibratedPoint>> = combine(trackPoints, calibration) { points, currentCalibration ->
        if (currentCalibration == null) emptyList()
        else points.mapNotNull { calibratePoint(it, currentCalibration) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val kinematicsTrackPoints: StateFlow<List<KinematicsPoint>> = calibratedTrackPoints
        .map(::computeKinematics)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = repository.getProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _isReady.value = (state == Player.STATE_READY || state == Player.STATE_BUFFERING || state == Player.STATE_ENDED)
                _duration.value = player.duration.coerceAtLeast(0L)
                if (state == Player.STATE_READY) _error.value = null
                if (state == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    _playWhenReady.value = false
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                _playWhenReady.value = playWhenReady
            }
            override fun onPlayerError(err: PlaybackException) {
                _error.value = "Video Error: ${err.message}"
            }
        })

        viewModelScope.launch {
            while (true) {
                val latestPosition = player.currentPosition
                if (_currentPosition.value != latestPosition) {
                    _currentPosition.value = latestPosition
                }
                delay(if (player.isPlaying) 33 else 90)
            }
        }
    }

    fun loadProject(project: ProjectEntity) {
        resetProjectUiState()
        _projectId.value = project.id
        _trimStart.value = project.trimStartMs
        _trimEnd.value = project.trimEndMs ?: C.TIME_END_OF_SOURCE
        _lastCheckpointSavedAt.value = project.updatedAt
        currentVideoUri = project.videoUri
        setVideoUri(project.videoUri)
        ensureInitialTrackers(project.id)
    }

    fun createAndLoadProject(uri: String, name: String) {
        viewModelScope.launch {
            val newId = repository.insertProject(
                ProjectEntity(name = name, videoUri = uri, videoWidth = 0, videoHeight = 0)
            )
            _projectId.value = newId
            _trimStart.value = 0L
            _trimEnd.value = C.TIME_END_OF_SOURCE
            _lastCheckpointSavedAt.value = System.currentTimeMillis()
            currentVideoUri = uri
            setVideoUri(uri)
            ensureInitialTrackers(newId)
        }
    }

    fun initializeProject(uri: String, name: String) {
        if (currentVideoUri == uri && _projectId.value != -1L) return
        viewModelScope.launch {
            resetProjectUiState()
            val projects = repository.getProjects().first()
            val existingProject = projects.find { it.name == name && it.videoUri == uri }
            if (existingProject != null) {
                _projectId.value = existingProject.id
                _trimStart.value = existingProject.trimStartMs
                _trimEnd.value = existingProject.trimEndMs ?: C.TIME_END_OF_SOURCE
                _lastCheckpointSavedAt.value = existingProject.updatedAt
            } else {
                val newId = repository.insertProject(
                    ProjectEntity(name = name, videoUri = uri, videoWidth = 0, videoHeight = 0)
                )
                _projectId.value = newId
                _lastCheckpointSavedAt.value = System.currentTimeMillis()
            }
            currentVideoUri = uri
            setVideoUri(uri)
            ensureInitialTrackers(_projectId.value)
        }
    }

    private fun ensureInitialTrackers(projectId: Long) {
        if (projectId == -1L) return
        viewModelScope.launch {
            if (repository.getTrackersForProject(projectId).first().isEmpty()) {
                listOf(
                    TrackerEntity("$projectId:A", projectId, "A", 0xFF00E676, 0),
                    TrackerEntity("$projectId:B", projectId, "B", 0xFFFF8870, 1)
                ).forEach { tracker -> repository.upsertTracker(tracker) }
            }
        }
    }

    fun addTracker(): Boolean {
        val projectId = _projectId.value
        val existing = trackers.value
        if (projectId == -1L || existing.size >= 4) return false
        val order = (existing.maxOfOrNull { it.displayOrder } ?: -1) + 1
        val label = ('A'.code + order).toChar().toString()
        val colors = longArrayOf(0xFF00E676, 0xFFFF8870, 0xFF53B7FF, 0xFFFFD166)
        viewModelScope.launch {
            repository.upsertTracker(TrackerEntity("$projectId:$label", projectId, label, colors[order % colors.size], order))
            touchActiveProject()
        }
        return true
    }

    fun updateTracker(tracker: TrackerEntity, displayName: String, colorArgb: Long) {
        viewModelScope.launch {
            repository.upsertTracker(tracker.copy(displayName = displayName.trim().ifEmpty { tracker.displayName }, colorArgb = colorArgb))
            touchActiveProject()
        }
    }

    suspend fun replaceTrackedSegment(
        trackerId: String,
        startFrame: Int,
        endFrame: Int,
        replacements: List<TrackPointEntity>
    ) {
        val projectId = _projectId.value
        if (projectId == -1L || endFrame < startFrame) return
        val removed = trackPoints.value.filter {
            it.trackerId == trackerId && it.frameNumber in startFrame..endFrame
        }
        replacePointsInternal(removed, replacements)
        recordAction(PointsReplaced(removed, replacements))
    }

    private fun resetProjectUiState() {
        undoStack.clear()
        redoStack.clear()
        syncHistoryState()
        _trimStart.value = 0L
        _trimEnd.value = C.TIME_END_OF_SOURCE
        _currentPosition.value = 0L
        _duration.value = 0L
        _frameDurationMs.value = 33L
        _videoTimingInfo.value = VideoTimingInfo()
        _error.value = null
        _lastCheckpointSavedAt.value = null
    }

    private fun setVideoUri(uri: String) {
        try {
            _videoTimingInfo.value = VideoTimelineIndex.inspect(context, uri)
            _frameDurationMs.value = _videoTimingInfo.value.sampleTimesUs.asList()
                .zipWithNext { left, right -> right - left }
                .filter { it > 0L }
                .sorted()
                .let { deltas -> deltas.getOrNull(deltas.size / 2)?.div(1000L) }
                ?.coerceAtLeast(1L)
                ?: VideoFrameTimingHelper.estimateFrameDurationMs(context, uri)
            val parsedUri = android.net.Uri.parse(uri)
            
            val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(_trimStart.value)
                .setEndPositionMs(_trimEnd.value)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(parsedUri)
                .setClippingConfiguration(clippingConfig)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = false 
            _error.value = null
        } catch (e: Exception) {
            _error.value = "Load Failed: ${e.message}"
        }
    }

    private fun recordAction(action: EditAction) {
        undoStack.add(action)
        redoStack.clear()
        syncHistoryState()
    }

    private fun syncHistoryState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun setTrimStart() {
        val previousStart = _trimStart.value
        val previousEnd = _trimEnd.value
        val newStart = previousStart + player.currentPosition
        if (previousEnd != C.TIME_END_OF_SOURCE && newStart >= previousEnd) return
        viewModelScope.launch {
            applyTrimInternal(newStart, previousEnd)
            recordAction(TrimChanged(previousStart, previousEnd, newStart, previousEnd))
        }
    }

    fun setTrimEnd() {
        val previousStart = _trimStart.value
        val previousEnd = _trimEnd.value
        val absolutePos = _trimStart.value + player.currentPosition
        if (absolutePos <= _trimStart.value) return
        viewModelScope.launch {
            applyTrimInternal(previousStart, absolutePos)
            recordAction(TrimChanged(previousStart, previousEnd, previousStart, absolutePos))
        }
    }

    fun resetTrim() {
        val previousStart = _trimStart.value
        val previousEnd = _trimEnd.value
        if (previousStart == 0L && previousEnd == C.TIME_END_OF_SOURCE) return
        viewModelScope.launch {
            applyTrimInternal(0L, C.TIME_END_OF_SOURCE)
            recordAction(TrimChanged(previousStart, previousEnd, 0L, C.TIME_END_OF_SOURCE))
        }
    }

    fun saveProjectNow() {
        viewModelScope.launch {
            persistTrimState(_trimStart.value, _trimEnd.value)
            val savedAt = System.currentTimeMillis()
            repository.touchProject(_projectId.value, savedAt)
            _lastCheckpointSavedAt.value = savedAt
        }
    }

    fun renameProject(projectId: Long, name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        viewModelScope.launch {
            repository.renameProject(projectId, normalizedName, System.currentTimeMillis())
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    fun togglePlayPause() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
            player.play()
        } else {
            if (player.playWhenReady || player.isPlaying) player.pause() else player.play()
        }
        _playWhenReady.value = player.playWhenReady
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(clamped)
        _currentPosition.value = clamped
    }

    fun pausePlayback() {
        player.pause()
        _playWhenReady.value = false
    }

    fun play() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
        }
        player.play()
        _playWhenReady.value = true
    }

    fun restart() {
        player.seekTo(0)
        player.playWhenReady = false 
        _playWhenReady.value = false
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        player.playbackParameters = PlaybackParameters(clamped)
        _playbackSpeed.value = clamped
    }

    fun setLoopPlayback(enabled: Boolean) {
        player.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _loopPlayback.value = enabled
    }

    fun stepFrames(count: Int) {
        val stepMs = _frameDurationMs.value.coerceAtLeast(1L)
        val target = player.currentPosition + (count * stepMs)
        seekTo(target.coerceIn(0, player.duration.coerceAtLeast(0L)))
    }

    fun beginScrubbing() {
        if (isScrubbing) return
        resumeAfterScrubJob?.cancel()
        wasPlayingBeforeScrub = false
        isScrubbing = true
        pendingScrubPositionMs = null
        player.pause()
        _isPlaying.value = false
        _playWhenReady.value = false
    }

    fun scrubTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        _currentPosition.value = clamped
        pendingScrubPositionMs = clamped

        val now = SystemClock.elapsedRealtime()
        val minimumStepMs = (_frameDurationMs.value / 3L).coerceAtLeast(12L)
        val movedEnough = kotlin.math.abs(clamped - lastScrubSeekPositionMs) >= minimumStepMs
        val enoughTimePassed = (now - lastScrubSeekAtMs) >= 16L
        if (!movedEnough && !enoughTimePassed) return

        lastScrubSeekAtMs = now
        lastScrubSeekPositionMs = clamped
        player.seekTo(clamped)
    }

    fun endScrubbing() {
        val finalPosition = pendingScrubPositionMs ?: _currentPosition.value
        pendingScrubPositionMs = null
        isScrubbing = false
        seekTo(finalPosition)
        wasPlayingBeforeScrub = false
    }

    fun addPoint(x: Double, y: Double, trackLabel: String = "A", massKg: Double = 1.0) {
        val id = _projectId.value
        if (id == -1L) return
        viewModelScope.launch {
            val frameTimeMs = player.currentPosition
            addPointAtInternal(frameTimeMs, x, y, trackLabel, massKg)
        }
    }

    fun addPointAt(positionMs: Long, x: Double, y: Double, trackLabel: String = "A", massKg: Double = 1.0) {
        val id = _projectId.value
        if (id == -1L) return
        viewModelScope.launch {
            addPointAtInternal(positionMs, x, y, trackLabel, massKg)
        }
    }

    suspend fun addPointAtTracked(positionMs: Long, x: Double, y: Double, trackLabel: String = "A", massKg: Double = 1.0) {
        val id = _projectId.value
        if (id == -1L) return
        addPointAtInternal(positionMs, x, y, trackLabel, massKg)
    }

    data class ObservationWrite(
        val requestedPositionMs: Long,
        /** Absolute video time when a caller has microsecond PTS; avoids millisecond truncation. */
        val requestedVideoTimestampUs: Long? = null,
        val sourceX: Double,
        val sourceY: Double,
        val trackLabel: String = "A",
        val massKg: Double = 1.0,
        val kind: String = "MEASURED",
        val algorithm: String,
        val confidence: Double? = null,
        val matchScore: Double? = null,
        val ambiguity: Double? = null,
        val residualErrorPx: Double? = null,
        val refinementMethod: String? = null,
        val recoveryStatus: String? = null,
        val manualCorrection: Boolean = false,
        val protectedKeyframe: Boolean = false
    )

    suspend fun addSourceObservationTracked(write: ObservationWrite) {
        val id = _projectId.value
        if (id == -1L) return
        val requestedUs = write.requestedVideoTimestampUs?.coerceAtLeast(0L)
            ?: ((_trimStart.value + write.requestedPositionMs.coerceAtLeast(0L)) * 1000L)
        val identity = _videoTimingInfo.value.nearestFrame(requestedUs)
        val point = TrackPointEntity(
            projectId = id,
            frameNumber = identity.frameNumber,
            timestampUs = identity.actualTimestampUs ?: requestedUs,
            trackLabel = write.trackLabel,
            trackerId = "$id:${write.trackLabel}",
            massKg = write.massKg,
            pixelX = write.sourceX,
            pixelY = write.sourceY,
            requestedTimestampUs = requestedUs,
            actualTimestampUs = identity.actualTimestampUs,
            sourceWidth = _videoTimingInfo.value.width.takeIf { it > 0 },
            sourceHeight = _videoTimingInfo.value.height.takeIf { it > 0 },
            coordinateSpace = "SOURCE_VIDEO",
            observationKind = write.kind,
            sourceAlgorithm = write.algorithm,
            confidence = write.confidence,
            matchScore = write.matchScore,
            ambiguity = write.ambiguity,
            residualErrorPx = write.residualErrorPx,
            refinementMethod = write.refinementMethod,
            recoveryStatus = write.recoveryStatus,
            isManualCorrection = write.manualCorrection,
            isProtectedKeyframe = write.protectedKeyframe
        )
        val existing = repository.getPointsForProject(id).first().firstOrNull {
            it.frameNumber == identity.frameNumber && it.trackLabel == write.trackLabel
        }
        // Automatic reruns must never erase a seed or a user correction, including the other track.
        if (existing?.isProtectedKeyframe == true && write.kind != "MANUAL") return
        if (existing != null) {
            val updated = point.copy(id = existing.id)
            insertPointInternal(updated)
            recordAction(PointUpdated(existing, updated))
        } else {
            val inserted = insertPointInternal(point)
            recordAction(PointAdded(inserted))
        }
    }

    suspend fun addTrackedPointsBatch(
        points: List<TrackedPointWrite>,
        trackLabel: String = "A",
        massKg: Double = 1.0
    ) {
        val id = _projectId.value
        if (id == -1L) return
        points.forEach { point ->
            addPointAtInternal(point.positionMs, point.x, point.y, trackLabel, massKg)
        }
    }

    data class TrackedPointWrite(
        val positionMs: Long,
        val x: Double,
        val y: Double
    )

    fun updatePoint(point: TrackPointEntity, newX: Double, newY: Double) {
        viewModelScope.launch {
            val updatedPoint = point.copy(pixelX = newX, pixelY = newY)
            if (updatedPoint == point) return@launch
            insertPointInternal(updatedPoint)
            recordAction(PointUpdated(point, updatedPoint))
        }
    }

    fun updateSourcePoint(point: TrackPointEntity, sourceX: Double, sourceY: Double) {
        viewModelScope.launch {
            val updatedPoint = point.copy(
                pixelX = sourceX,
                pixelY = sourceY,
                coordinateSpace = "SOURCE_VIDEO",
                observationKind = "MANUAL",
                sourceAlgorithm = "manual-correction",
                confidence = 1.0,
                isManualCorrection = true,
                isProtectedKeyframe = true
            )
            if (updatedPoint == point) return@launch
            insertPointInternal(updatedPoint)
            recordAction(PointUpdated(point, updatedPoint))
        }
    }

    fun deletePoint(pointId: Long) {
        viewModelScope.launch {
            val point = trackPoints.value.firstOrNull { it.id == pointId } ?: return@launch
            deletePointInternal(pointId)
            recordAction(PointDeleted(point))
        }
    }

    fun clearAllPoints() {
        val id = _projectId.value
        if (id == -1L) return
        viewModelScope.launch {
            val points = trackPoints.value
            if (points.isEmpty()) return@launch
            deleteAllPointsInternal(id)
            recordAction(PointsCleared(points))
        }
    }

    fun updateTrackMass(trackLabel: String, massKg: Double) {
        val id = _projectId.value
        if (id == -1L) return
        viewModelScope.launch {
            repository.updateMassForTrack(id, trackLabel, massKg)
            repository.touchProject(id, System.currentTimeMillis())
        }
    }

    fun undo() {
        val action = undoStack.removeLastOrNull() ?: return
        viewModelScope.launch {
            action.undo(this@VideoPlayerViewModel)
            redoStack.add(action)
            syncHistoryState()
        }
    }

    fun redo() {
        val action = redoStack.removeLastOrNull() ?: return
        viewModelScope.launch {
            action.redo(this@VideoPlayerViewModel)
            undoStack.add(action)
            syncHistoryState()
        }
    }

    fun ensureCalibrationDefaults(sourceWidth: Int, sourceHeight: Int) {
        val id = _projectId.value
        if (id == -1L || calibration.value != null || sourceWidth <= 0 || sourceHeight <= 0) return
        val centerX = (sourceWidth - 1) / 2.0
        val centerY = (sourceHeight - 1) / 2.0
        val halfStick = sourceWidth.coerceAtLeast(240) / 6.0
        saveCalibration(
            CalibrationEntity(
                projectId = id,
                stickP1x = centerX - halfStick,
                stickP1y = centerY,
                stickP2x = centerX + halfStick,
                stickP2y = centerY,
                knownDistanceMeters = 1.0,
                calibrationUnit = "m",
                originX = centerX,
                originY = centerY,
                coordinateSpace = "SOURCE_VIDEO",
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight
            ),
            recordHistory = false
        )
    }

    fun updateCalibrationStick(startX: Double, startY: Double, endX: Double, endY: Double) {
        val current = calibration.value ?: return
        saveCalibration(
            current.copy(
                stickP1x = startX,
                stickP1y = startY,
                stickP2x = endX,
                stickP2y = endY
            )
        )
    }

    fun updateCalibrationDistance(distanceValue: Double, unit: String) {
        val current = calibration.value ?: return
        val factor = when (unit) {
            "mm" -> 0.001
            "cm" -> 0.01
            "m" -> 1.0
            else -> 1.0
        }
        saveCalibration(
            current.copy(
                knownDistanceMeters = distanceValue * factor,
                calibrationUnit = unit
            )
        )
    }

    fun updateCalibration(calibration: CalibrationEntity) {
        saveCalibration(calibration)
    }

    private suspend fun insertPointInternal(point: TrackPointEntity): TrackPointEntity {
        val insertedId = repository.insertPoint(point)
        touchActiveProject()
        return if (point.id != 0L) point else point.copy(id = insertedId)
    }

    private suspend fun addPointAtInternal(
        positionMs: Long,
        x: Double,
        y: Double,
        trackLabel: String,
        massKg: Double
    ) {
        val id = _projectId.value
        if (id == -1L) return
        val clampedPositionMs = positionMs.coerceAtLeast(0L)
        val stepMs = _frameDurationMs.value.coerceAtLeast(1L)
        val frameNumber = (clampedPositionMs / stepMs).toInt()
        val newPoint = TrackPointEntity(
            projectId = id,
            frameNumber = frameNumber,
            timestampUs = (_trimStart.value + clampedPositionMs) * 1000,
            trackLabel = trackLabel,
            massKg = massKg,
            pixelX = x,
            pixelY = y,
            trackerId = "$id:$trackLabel",
            requestedTimestampUs = (_trimStart.value + clampedPositionMs) * 1000L,
            coordinateSpace = "LEGACY_DISPLAY",
            observationKind = "MANUAL",
            sourceAlgorithm = "legacy-ui"
        )
        val existingPoint = trackPoints.value.firstOrNull { it.frameNumber == frameNumber && it.trackLabel == trackLabel }
        if (existingPoint != null) {
            val updatedPoint = existingPoint.copy(
                timestampUs = newPoint.timestampUs,
                pixelX = x,
                pixelY = y
            )
            insertPointInternal(updatedPoint)
            recordAction(PointUpdated(existingPoint, updatedPoint))
        } else {
            val insertedPoint = insertPointInternal(newPoint)
            recordAction(PointAdded(insertedPoint))
        }
    }

    private suspend fun deletePointInternal(pointId: Long) {
        repository.deletePoint(pointId)
        touchActiveProject()
    }

    private suspend fun deleteAllPointsInternal(projectId: Long) {
        repository.deleteAllPoints(projectId)
        touchActiveProject()
    }

    private suspend fun restorePointsInternal(points: List<TrackPointEntity>) {
        points.forEach { repository.insertPoint(it) }
        touchActiveProject()
    }

    private suspend fun replacePointsInternal(remove: List<TrackPointEntity>, add: List<TrackPointEntity>) {
        remove.forEach { repository.deletePoint(it.id) }
        add.forEach { repository.insertPoint(it) }
        touchActiveProject()
    }

    private suspend fun applyCalibrationInternal(calibration: CalibrationEntity?) {
        val projectId = _projectId.value
        if (calibration != null) {
            repository.upsertCalibration(calibration)
            touchActiveProject()
        } else if (projectId != -1L) {
            repository.deleteCalibration(projectId)
            touchActiveProject()
        }
    }

    private suspend fun applyTrimInternal(start: Long, end: Long) {
        _trimStart.value = start
        _trimEnd.value = end
        persistTrimState(start, end)
        currentVideoUri?.let { setVideoUri(it) }
    }

    private suspend fun persistTrimState(start: Long, end: Long) {
        val projectId = _projectId.value
        if (projectId == -1L) return
        repository.updateProjectTrim(
            projectId = projectId,
            trimStartMs = start,
            trimEndMs = end.takeUnless { it == C.TIME_END_OF_SOURCE },
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun saveCalibration(calibration: CalibrationEntity, recordHistory: Boolean = true) {
        viewModelScope.launch {
            val previous = this@VideoPlayerViewModel.calibration.value
            if (previous == calibration) return@launch
            applyCalibrationInternal(calibration)
            if (recordHistory) {
                recordAction(CalibrationChanged(previous, calibration))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
