package com.physicstracker.app.ui.light

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.physicstracker.app.ui.LightChannel
import com.physicstracker.app.ui.LightGeometry
import com.physicstracker.app.ui.LightTool
import com.physicstracker.app.ui.demoSourceNote
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

enum class LightPage { SELECT, GRAPH, TABLE, COMPARE, SHARE }
data class LightWorkspaceState(
    val uri: String? = null,
    val name: String = "",
    val video: Boolean = false,
    val preview: Bitmap? = null,
    val previewTimestampUs: Long? = null,
    val provenance: String = "",
    val geometries: Map<String, LightGeometry> = emptyMap(),
    val datasets: Map<String, LightDataset> = emptyMap(),
    val active: String = "A",
    val dual: Boolean = false,
    val regionCount: Int = 1,
    val channel: LightChannel = LightChannel.GRAY,
    val page: LightPage = LightPage.SELECT,
    val busy: Boolean = false,
    val recording: Boolean = false,
    val selectedRow: Int = 0,
    val status: String = "Choose a photo or video to begin.",
    val projectId: String? = null,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val timeMode: Boolean = false,
    val smooth: Boolean = false,
    val baseline: Boolean = false,
    val peaks: Boolean = false,
    val compare: Boolean = false,
    val saveStatus: String = "",
    val demoStep: Int = -1
)

/** Separate from the motion ViewModel/Room observations. Retained across activity rotation. */
@OptIn(kotlinx.coroutines.FlowPreview::class)
class LightAnalysisViewModel @JvmOverloads constructor(application: Application,
    storageFolder: File = File(application.filesDir, "light-projects")) : AndroidViewModel(application) {
    private val mutable = MutableStateFlow(LightWorkspaceState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    @Volatile private var stopRequested = false
    @Volatile private var playbackRate = 1f
    private var generation = 0
    private var linkedProjectUri: String? = null
    private val store = LightProjectStore(storageFolder)
    private val saveMutex = Mutex()
    private val mutableHistory = MutableStateFlow<List<LightProjectSummary>>(emptyList())
    val history = mutableHistory.asStateFlow()
    private val mutableHistoryError = MutableStateFlow<String?>(null)
    val historyError = mutableHistoryError.asStateFlow()

    init {
        refreshHistory()
        viewModelScope.launch {
            mutable.map { snapshot(it) }.distinctUntilChanged().debounce(350).collect {
                if (it != null) saveCurrent()
            }
        }
    }
    private fun snapshot(s: LightWorkspaceState): SavedLightProject? {
        if (s.busy || s.projectId == null || s.uri == null || s.sourceWidth <= 0 || s.sourceHeight <= 0) return null
        return SavedLightProject(s.projectId, s.uri, s.name, s.video, s.sourceWidth, s.sourceHeight,
            s.previewTimestampUs, s.provenance, s.active, s.regionCount, s.channel, s.geometries, s.datasets,
            s.timeMode, s.smooth, s.baseline, s.peaks, s.compare, updatedAt = 0L)
    }
    private suspend fun persist(saved: SavedLightProject) {
        withContext(Dispatchers.IO) { store.save(saved.copy(updatedAt = System.currentTimeMillis())) }
        mutableHistory.value = withContext(Dispatchers.IO) { store.history() }
        mutableHistoryError.value = if (store.unreadableProjects > 0) "${store.unreadableProjects} light projects could not be read. Their files have been kept." else null
        if (mutable.value.projectId == saved.id) mutable.update { it.copy(saveStatus = "Saved on device") }
    }
    private suspend fun saveCurrent() = saveMutex.withLock {
        val saved = snapshot(mutable.value) ?: return@withLock
        try { persist(saved) }
        catch (error: Exception) {
            if (error is CancellationException) throw error
            mutable.update { it.copy(saveStatus = "Save failed — ${error.message}") }
        }
    }
    fun saveProjectNow() { viewModelScope.launch { saveCurrent() } }
    fun refreshHistory() { viewModelScope.launch {
        try { mutableHistory.value = withContext(Dispatchers.IO) { store.history() }
            mutableHistoryError.value = if (store.unreadableProjects > 0) "${store.unreadableProjects} light projects could not be read. Their files have been kept." else null }
        catch (error: Exception) { mutableHistoryError.value = "Could not read light history: ${error.message}" }
    } }
    fun reopen(id: String) { viewModelScope.launch {
        saveCurrent()
        try {
            val saved = withContext(Dispatchers.IO) { store.load(id) }
            open(Uri.parse(saved.uri), saved.video, saved)
        } catch (error: Exception) { message("Could not reopen project: ${error.message}") }
    } }
    fun options(time: Boolean? = null, smooth: Boolean? = null, baseline: Boolean? = null, peaks: Boolean? = null, compare: Boolean? = null) {
        mutable.update { it.copy(timeMode = time ?: it.timeMode, smooth = smooth ?: it.smooth,
            baseline = baseline ?: it.baseline, peaks = peaks ?: it.peaks, compare = compare ?: it.compare) }
    }
    fun rename(name: String) {
        if (name.isNotBlank()) mutable.update { it.copy(name = name.trim().take(150)) }
        saveProjectNow()
    }
    fun newProject() { viewModelScope.launch {
        stop(); job?.cancelAndJoin(); saveCurrent(); linkedProjectUri = null
        mutable.value = LightWorkspaceState()
    } }

    fun attachProjectVideo(uri: String) {
        if (linkedProjectUri == uri) return
        linkedProjectUri = uri
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) { store.history().firstOrNull { it.uri == uri } }
            if (existing != null) reopen(existing.id) else open(Uri.parse(uri), true)
        }
    }

    fun message(text: String) { mutable.update { it.copy(status = text) } }
    fun page(page: LightPage) { mutable.update { it.copy(page = page) } }
    fun row(index: Int) { mutable.update { it.copy(selectedRow = index) } }
    fun selectSlot(slot: String) {
        if (!state.value.busy && slot in lightRegionLabels(state.value.regionCount)) mutable.update { it.copy(active = slot, selectedRow = 0) }
    }
    fun dual(enabled: Boolean) {
        regions(if (enabled) 2 else 1)
    }
    fun regions(count: Int) {
        if (state.value.busy || count !in 1..MAX_LIGHT_REGIONS) return
        mutable.update { current ->
            val sourceWidth = current.preview?.width ?: current.sourceWidth
            val sourceHeight = current.preview?.height ?: current.sourceHeight
            if (sourceWidth <= 0 || sourceHeight <= 0) return@update current
            val newGeometry = lightNewRegionGeometry(current.geometries[current.active], sourceWidth, sourceHeight)
            val geometries = lightRegionLabels(count).fold(current.geometries) { map, slot ->
                if (slot in map) map else map + (slot to newGeometry)
            }
            current.copy(regionCount = count, dual = count > 1, geometries = geometries,
                active = if (current.active in lightRegionLabels(count)) current.active else "A")
        }
    }
    fun addRegion() {
        val current = state.value
        if (current.busy || current.regionCount >= MAX_LIGHT_REGIONS || current.preview == null) return
        val next = current.regionCount + 1
        val slot = lightRegionLabels(next).last()
        val geometry = lightNewRegionGeometry(current.geometries[current.active], current.preview.width, current.preview.height)
        mutable.update { it.copy(regionCount = next, dual = true, active = slot,
            geometries = it.geometries + (slot to geometry), selectedRow = 0,
            status = "Region $slot added as ${geometry.tool.name.lowercase()}. Drag it to another place.") }
    }
    fun channel(channel: LightChannel) {
        if (state.value.busy || state.value.channel == channel) return
        mutable.update { it.copy(channel = channel, datasets = emptyMap(), selectedRow = 0,
            status = "Channel changed. Analyze again to create matching results.") }
    }
    fun geometry(geometry: LightGeometry) {
        val current = state.value.geometries[state.value.active] ?: return
        setGeometry(geometry.copy(tool = current.tool))
    }
    private fun setGeometry(geometry: LightGeometry) {
        if (state.value.busy) return
        val preview = state.value.preview ?: return
        val selected = canonicalLightGeometry(geometry, preview.width, preview.height)
        if (selected == state.value.geometries[state.value.active]) return
        mutable.update { it.copy(geometries = it.geometries + (it.active to selected),
            datasets = it.datasets - it.active, selectedRow = 0, status = "Selection changed. Tap Analyze or Start video.") }
    }
    fun tool(tool: LightTool) {
        val current = state.value.geometries[state.value.active] ?: return
        val preview = state.value.preview ?: return
        setGeometry(lightGeometryForTool(current, tool, preview.width, preview.height))
    }
    fun clear() {
        if (!state.value.busy) mutable.update { it.copy(datasets = it.datasets - it.active,
            selectedRow = 0, status = "Results for ${it.active} cleared.") }
    }
    fun stop() { stopRequested = true }
    fun playbackRate(rate: Float) { playbackRate = rate.coerceIn(0.1f, 4f) }

    /** Seek to a real decoded frame; retain its actual PTS rather than claiming the request was exact. */
    fun previewAt(requestedUs: Long) {
        val snapshot = state.value
        if (snapshot.busy || !snapshot.video || snapshot.uri == null) return
        val version = ++generation
        mutable.update { it.copy(busy = true, status = "Opening video frame…") }
        job = viewModelScope.launch {
            try {
                var found = false
                withContext(Dispatchers.Default) {
                    decodeLightVideo(getApplication(), Uri.parse(snapshot.uri), { ensureActive() }) { pixels, pts, metadata ->
                        if (pts >= requestedUs.coerceAtLeast(0L)) {
                            val bitmap = pixels.toPreviewBitmap { ensureActive() }
                            if (generation == version) mutable.update { it.copy(preview = bitmap, previewTimestampUs = pts,
                                provenance = metadata + demoSourceNote(Uri.parse(snapshot.uri)), status = "Frame at ${pts / 1_000_000.0} s. Select or measure a region.") }
                            found = true
                            false
                        } else true
                    }
                }
                if (!found && generation == version) message("No frame at that time. The previous selection frame is unchanged.")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (generation == version) message("Could not open frame: ${error.message}") }
            finally { if (generation == version) mutable.update { it.copy(busy = false) } }
        }
    }

    fun openDemo() {
        if (state.value.busy) return
        val previous = snapshot(state.value)
        mutable.update { it.copy(busy = true, status = "Opening guided demo…") }
        viewModelScope.launch {
        try {
            if (previous != null) saveMutex.withLock { persist(previous) }
            val file = withContext(Dispatchers.IO) {
                val directory = File(getApplication<Application>().filesDir, "demos").apply { mkdirs() }
                val destination = File(directory, "light-pulse.mp4")
                getApplication<Application>().assets.open("demos/light-pulse.mp4").use { input ->
                    destination.outputStream().use { input.copyTo(it) }
                }
                destination
            }
            open(Uri.fromFile(file), true, demo = true)
        } catch (error: Exception) { mutable.update { it.copy(busy = false, status = "Could not open demo: ${error.message}") } }
    } }
    fun demoStep(step: Int) { mutable.update { it.copy(demoStep = step) } }

    fun open(uri: Uri, video: Boolean, restored: SavedLightProject? = null, demo: Boolean = false, displayName: String? = null) {
        val previous = snapshot(mutable.value)
        job?.cancel()
        val version = ++generation
        if (restored != null) mutable.value = LightWorkspaceState(
            uri = restored.uri, name = restored.name, video = restored.video, previewTimestampUs = restored.timestampUs,
            provenance = restored.provenance, geometries = restored.geometries, datasets = restored.datasets,
            active = restored.active, dual = restored.regionCount > 1, regionCount = restored.regionCount, channel = restored.channel,
            projectId = restored.id, sourceWidth = restored.width, sourceHeight = restored.height,
                        timeMode = restored.timeMode, smooth = restored.smooth, baseline = restored.baseline, peaks = restored.peaks,
            compare = restored.compare, saveStatus = "Saved on device")
        mutable.update { it.copy(busy = true, status = "Opening media…") }
        job = viewModelScope.launch {
            try {
                if (previous != null) saveMutex.withLock { persist(previous) }
                val result = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val name = displayName ?: if (demo) "Demo — Light profile and pulse (synthetic)" else runCatching { context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                        if (it.moveToFirst()) it.getString(0) else null
                    } }.getOrNull() ?: uri.lastPathSegment ?: "Light analysis"
                    var timestamp: Long? = null
                    var provenance = "Original photo pixels; EXIF orientation applied; decoded RGB; opaque pixels only"
                    val bitmap = if (!video) decodeLightPhoto(context, uri) else {
                        var first: Bitmap? = null
                        decodeLightVideo(context, uri, { ensureActive() }) { pixels, pts, metadata ->
                            if (pts < (restored?.timestampUs ?: 0L)) return@decodeLightVideo true
                            first = pixels.toPreviewBitmap { ensureActive() }
                            timestamp = pts
                            provenance = metadata
                            false
                        }
                        requireNotNull(first)
                    }
                    val centre = Offset((bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())
                    if (restored != null) {
                        require(bitmap.width == restored.width && bitmap.height == restored.height) { "Source dimensions changed. Saved results are retained; do not measure with mismatched media." }
                        return@withContext mutable.value.copy(preview = bitmap, previewTimestampUs = timestamp,
                            status = "Project reopened. Regions and raw results restored.")
                    }
                    LightWorkspaceState(uri.toString(), name, video, bitmap, timestamp, provenance + demoSourceNote(uri),
                        geometries = mapOf("A" to LightGeometry(LightTool.POINT, centre, centre)),
                        projectId = UUID.randomUUID().toString(), sourceWidth = bitmap.width, sourceHeight = bitmap.height,
                        timeMode = video,
                        demoStep = if (demo) 0 else -1,
                        status = "Tap the image to choose a pixel. Choose Line or Area for a region.")
                }
                if (generation == version) mutable.value = result
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (generation == version) message(if (restored != null)
                "Saved results retained, but source media could not reopen: ${error.message}. Graphs and exports remain available."
                else error.message ?: "Could not open this media.") }
            finally { if (generation == version) mutable.update { it.copy(busy = false) } }
        }
    }

    fun analyze(timeSeries: Boolean) {
        val snapshot = state.value
        if (snapshot.busy || snapshot.preview == null || snapshot.uri == null || (timeSeries && !snapshot.video)) return
        val geometry = snapshot.geometries[snapshot.active] ?: return
        val version = ++generation
        stopRequested = false
        mutable.update { it.copy(busy = true, recording = timeSeries, datasets = it.datasets - snapshot.active,
            status = "Analyzing source pixels…") }
        job = viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    val observations = mutableListOf<LightObservation>()
                    val metadata = snapshot.provenance
                    var allFrames = true
                    var missing = 0
                    var gap = false
                    var lastProgressNs = 0L
                    var firstPresentationUs: Long? = null
                    var targetPresentationNs = 0L
                    var previousPresentationUs: Long? = null
                    fun publish(complete: Boolean) {
                        val samples = if (timeSeries) canonicalLightTimes(observations) else observations.toList()
                        val data = LightDataset(snapshot.name, snapshot.uri, snapshot.preview.width, snapshot.preview.height,
                            snapshot.active, snapshot.channel, geometry, timeSeries, samples, metadata, complete)
                        mutable.update { it.copy(datasets = it.datasets + (snapshot.active to data)) }
                    }
                    try {
                    if (!timeSeries) observations += measureLightProfile(BitmapLightPixels(snapshot.preview), geometry, snapshot.channel) { ensureActive() }
                        .map { it.copy(timestampUs = snapshot.previewTimestampUs) }
                    else decodeLightVideo(getApplication(), Uri.parse(snapshot.uri), { ensureActive() }) { pixels, pts, provenance ->
                        if (stopRequested) { allFrames = false; false } else {
                            if (firstPresentationUs == null) {
                                firstPresentationUs = pts
                                targetPresentationNs = System.nanoTime()
                            }
                            // Present samples at their actual video timestamps so the graph grows
                            // alongside playback. Slow decoders naturally fall behind without
                            // inventing frames or changing recorded presentation times.
                            val deltaUs = (pts - (previousPresentationUs ?: pts)).coerceIn(0L, 86_400_000_000L)
                            targetPresentationNs += (deltaUs * 1_000.0 / playbackRate).toLong()
                            previousPresentationUs = pts
                            while (!stopRequested && System.nanoTime() < targetPresentationNs) {
                                ensureActive()
                                Thread.sleep(10)
                            }
                            if (stopRequested) { allFrames = false; return@decodeLightVideo false }
                            require(pixels.width == snapshot.preview.width && pixels.height == snapshot.preview.height) {
                                "Video dimensions changed. Analysis stopped to preserve selection accuracy."
                            }
                            require(metadata == provenance + demoSourceNote(Uri.parse(snapshot.uri))) { "Video colour metadata changed during analysis." }
                            val value = measureLightMean(pixels, geometry, snapshot.channel) { ensureActive() }
                            if (value == null || pts < 0) { missing++; gap = true } else {
                                observations += LightObservation(pts / 1_000_000.0, value, pts, gap)
                                gap = false
                            }
                            if (System.nanoTime() - lastProgressNs >= 250_000_000L) {
                                lastProgressNs = System.nanoTime()
                                publish(false)
                                message("${observations.size} frames measured · ${(pts / 1_000_000.0).toString().take(6)} s · Stop keeps partial results")
                            }
                            if (observations.size >= 200_000) { allFrames = false; stopRequested = true; false } else true
                        }
                    }
                    } catch (error: Exception) {
                        // Even a failure before the first progress update must preserve the new
                        // run's partial samples, never expose an older completed run as current.
                        if (error !is CancellationException) publish(false)
                        throw error
                    }
                    ensureActive()
                    publish(allFrames && missing == 0)
                    message(if (observations.isEmpty()) "No opaque pixels were measurable in this selection."
                        else "${observations.size} samples · ${if (allFrames) "Complete" else "Stopped; partial results"}" +
                            if (missing > 0) " · $missing frames unavailable" else "")
                }
                mutable.update { it.copy(selectedRow = 0) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                message("Analysis stopped: ${error.message ?: "could not read media"}. Any saved partial results are labelled incomplete.")
            } finally { if (generation == version) mutable.update { it.copy(busy = false, recording = false) } }
        }
    }

    /** Preview the exact stored timestamp, not a closest-frame timestamp assigned to another image. */
    fun showSelectedFrame() {
        val snapshot = state.value
        val data = snapshot.datasets[snapshot.active] ?: return
        val pts = data.observations.getOrNull(snapshot.selectedRow)?.timestampUs ?: return
        if (snapshot.busy) return
        mutable.update { it.copy(busy = true, status = "Opening measured frame…") }
        job = viewModelScope.launch {
            try {
                var found = false
                withContext(Dispatchers.Default) {
                    decodeLightVideo(getApplication(), Uri.parse(data.mediaUri), { ensureActive() }) { pixels, timestamp, _ ->
                        if (timestamp == pts) {
                            val bitmap = pixels.toPreviewBitmap { ensureActive() }
                            mutable.update { it.copy(preview = bitmap, previewTimestampUs = timestamp, page = LightPage.SELECT,
                                status = "Measured frame at ${timestamp / 1_000_000.0} s") }
                            found = true
                            false
                        } else true
                    }
                }
                if (!found) message("The exact measured frame could not be decoded. No substitute was shown.")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message(error.message ?: "Could not show this frame.") }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
