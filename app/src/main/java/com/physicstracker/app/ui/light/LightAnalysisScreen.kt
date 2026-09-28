package com.physicstracker.app.ui.light

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.physicstracker.app.ui.LightChannel
import com.physicstracker.app.ui.LightTool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@android.annotation.SuppressLint("SuspiciousIndentation")
@androidx.media3.common.util.UnstableApi
fun LightAnalysisScreen(onBack: () -> Unit, model: LightAnalysisViewModel = viewModel(), initialVideoUri: String? = null,
    returnToTracking: Boolean = false, onOpenMotion: ((String, String, Boolean) -> Unit)? = null,
    onOpenHistory: () -> Unit = onBack) {
    val state by model.state.collectAsState()
    LaunchedEffect(initialVideoUri) {
        if (initialVideoUri != null) model.attachProjectVideo(initialVideoUri)
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editStart by rememberSaveable { mutableStateOf(true) }
    val compare = state.compare
    val timeMode = state.timeMode
    val smooth = state.smooth
    val baseline = state.baseline
    val peaks = state.peaks
    var precision by rememberSaveable { mutableStateOf(false) }
    var analysisFullScreen by rememberSaveable { mutableStateOf(false) }
    var analysisTab by rememberSaveable { mutableStateOf(LightDeskTab.GRAPH) }
    var analysisOptions by rememberSaveable { mutableStateOf(false) }
    var analysisFit by rememberSaveable { mutableStateOf(false) }
    var compareSelected by rememberSaveable { mutableStateOf(listOf("A", "B")) }
    var compareSecond by rememberSaveable { mutableStateOf("B") }
    var playbackRate by rememberSaveable { mutableFloatStateOf(1f) }
    var exporting by remember { mutableStateOf(false) }
    var pendingFile by rememberSaveable { mutableStateOf<String?>(null) }
    fun leave() { model.stop(); model.saveProjectNow(); onBack() }
    BackHandler { if (analysisFullScreen) analysisFullScreen = false else leave() }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            model.open(uri, false)
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            model.open(uri, true)
        }
    }
    fun save(uri: Uri?) {
        val path = pendingFile
        if (uri == null || path == null) { model.message("Save cancelled. Analysis results are unchanged."); return }
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { destination ->
                        File(path).inputStream().use { it.copyTo(destination) }
                    }
                }
                model.message("Light Analysis export saved.")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { model.message("Could not save: ${error.message}") }
        }
    }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf"), ::save)
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv"), ::save)
    val saveXlsx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(LIGHT_XLSX_MIME), ::save)
    val comparisonSlots = lightComparisonSlots(state.active, compareSecond, state.regionCount)
    val effectiveSecond = comparisonSlots.getOrNull(1) ?: "B"
    val selectedCompareSlots = compareSelected.filter { it in lightRegionLabels(state.regionCount) }
        .distinct().ifEmpty { listOf(state.active) }
    val rawSelectedData = if (compare && state.dual) selectedCompareSlots.mapNotNull { state.datasets[it] }
        else listOfNotNull(state.datasets[state.active])
    val selectedData = remember(rawSelectedData, baseline, smooth) { rawSelectedData.map { processedLightData(it, baseline, smooth) } }
    fun export(pdf: Boolean, share: Boolean, spreadsheet: Boolean = false) {
        val snapshot = selectedData.toList()
        if (exporting || snapshot.isEmpty()) return
        exporting = true
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { if (spreadsheet) createLightSpreadsheet(context, snapshot) else createLightExport(context, snapshot, pdf) }
                pendingFile = file.absolutePath
                if (share) context.startActivity(Intent.createChooser(lightShareIntent(context, file, pdf), "Share Light Analysis"))
                else if (spreadsheet) saveXlsx.launch(file.name) else if (pdf) savePdf.launch(file.name) else saveCsv.launch(file.name)
                model.message(if (share) "Choose an app to share the light results." else "Choose where to save the light results.")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { model.message("Export failed: ${error.message ?: "no compatible sharing app"}") }
            finally { exporting = false }
        }
    }
    val geometry = state.geometries[state.active]
    val data = state.datasets[state.active]?.let { processedLightData(it, baseline, smooth) }
    val player = rememberLightPlayer(state)
    var playing by remember { mutableStateOf(false) }
    var deck by rememberSaveable { mutableStateOf("Light") }
    var windows by rememberSaveable { mutableStateOf(listOf<String>()) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    var panMode by rememberSaveable { mutableStateOf(false) }
    var trackingPlayback by remember { mutableStateOf(false) }
    fun toggleWindow(page: LightPage) {
        windows = if (page.name in windows) emptyList() else listOf(page.name)
    }
    fun viewSelectedFrame() {
        player.pause()
        windows = emptyList()
        analysisFullScreen = false
        deck = "Light"
        model.showSelectedFrame()
    }
    fun setPlaybackRate(rate: Float) {
        playbackRate = rate.coerceIn(0.1f, 4f)
        model.playbackRate(playbackRate)
        player.setPlaybackSpeed(playbackRate)
    }
    fun pick(point: Offset) {
        geometry?.let { model.geometry(if (it.tool == LightTool.POINT) it.copy(start = point, end = point)
            else if (editStart) it.copy(start = point) else it.copy(end = point)) }
    }
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player, playbackRate) {
        model.playbackRate(playbackRate)
        player.setPlaybackSpeed(playbackRate)
    }
    LaunchedEffect(state.recording) {
        if (state.recording && state.video) {
            player.seekTo(0)
            player.play()
            trackingPlayback = true
        } else if (trackingPlayback) {
            player.pause()
            trackingPlayback = false
            if (!state.busy) model.previewAt(player.currentPosition.coerceAtLeast(0L) * 1000L)
        }
    }
    LaunchedEffect(state.uri) { zoom = 1f; panX = 0f; panY = 0f; windows = emptyList() }
    LaunchedEffect(state.page) {
        if (state.page != LightPage.SELECT) {
            windows = listOf(state.page.name)
            model.page(LightPage.SELECT)
        }
    }
    if (analysisFullScreen) {
        LightAnalysisDesk(state, analysisTab, selectedCompareSlots, analysisOptions, analysisFit,
            onBack = { analysisFullScreen = false },
            onTab = { analysisTab = it; model.options(compare = it == LightDeskTab.COMPARE) },
            onToggleCompareSlot = { slot ->
                compareSelected = if (slot in compareSelected && compareSelected.size > 1) compareSelected - slot
                    else (compareSelected + slot).distinct()
            },
            onFirstSlot = { slot ->
                model.selectSlot(slot)
                if (slot == compareSecond) compareSecond = lightRegionLabels(state.regionCount).firstOrNull { it != slot } ?: "B"
            },
            onAddRegion = {
                if (state.regionCount < MAX_LIGHT_REGIONS) {
                    model.addRegion()
                    compareSelected = (compareSelected + lightRegionLabels(state.regionCount + 1).last()).distinct()
                }
            },
            onMeasureRegion = { slot -> model.selectSlot(slot); analysisFullScreen = false; deck = "Light" },
            onOptions = { analysisOptions = !analysisOptions },
            onFit = { analysisFit = !analysisFit },
            onBaseline = { model.options(baseline = it) },
            onSmooth = { model.options(smooth = it) },
            onPeaks = { model.options(peaks = it) },
            onRow = model::row,
            onShowFrame = ::viewSelectedFrame,
            onExport = { analysisFullScreen = false; windows = listOf(LightPage.SHARE.name) },
            onFloat = {
                analysisFullScreen = false
                windows = listOf(when (analysisTab) {
                    LightDeskTab.GRAPH -> LightPage.GRAPH
                    LightDeskTab.TABLE -> LightPage.TABLE
                    LightDeskTab.COMPARE -> LightPage.COMPARE
                    LightDeskTab.STATS -> LightPage.GRAPH
                }.name)
            })
        return
    }
    fun panelContent(page: LightPage): androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
        val panelData = if (page == LightPage.COMPARE)
            comparisonSlots.mapNotNull { state.datasets[it] }.map { processedLightData(it, baseline, smooth) }
            else selectedData
            when (page) {
                LightPage.SELECT -> {
                    item {
                        LightVisibleTools(state, timeMode, { model.options(time = it) }, model::channel)
                    }
                    if (state.preview != null && geometry != null && geometry.tool != LightTool.POINT) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(editStart, { editStart = true }, label = { Text(if (geometry.tool == LightTool.BOX) "First corner" else "Line start") })
                                FilterChip(!editStart, { editStart = false }, label = { Text(if (geometry.tool == LightTool.BOX) "Opposite corner" else "Line end") })
                            }
                        }
                    }
                }
                LightPage.GRAPH, LightPage.COMPARE -> {
                    item {
                        LightProcessingTools(baseline, { model.options(baseline = it) }, smooth, { model.options(smooth = it) }, peaks, { model.options(peaks = it) })
                        if (page == LightPage.COMPARE) {
                            if (state.regionCount == 1) TextButton(onClick = model::addRegion) { Text("Add region B") }
                            else {
                                Text("First: ${state.active}")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    lightRegionLabels(state.regionCount).forEach { slot ->
                                        com.physicstracker.app.ui.BottomDeckButton(lightRegionControlLabel(slot, state.geometries[slot]),
                                            selected = state.active == slot) { model.selectSlot(slot) }
                                    }
                                }
                                Text("Second: $effectiveSecond")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    lightRegionLabels(state.regionCount).filter { it != state.active }.forEach { slot ->
                                        com.physicstracker.app.ui.BottomDeckButton(lightRegionControlLabel(slot, state.geometries[slot]),
                                            selected = effectiveSecond == slot) { compareSecond = slot }
                                    }
                                }
                            }
                        }
                    }
                    if (panelData.isEmpty()) item { Text("No result yet") }
                    panelData.forEach { dataset ->
                        item {
                            Text("Region ${dataset.slot}", style = MaterialTheme.typography.titleMedium)
                            LightResultPlot(dataset, if (dataset.slot == state.active) state.selectedRow else -1) { index ->
                                model.selectSlot(dataset.slot); model.row(index)
                            }
                            if (peaks) Text("Peaks: ${lightPeakIndices(dataset.observations).size}")
                        }
                    }
                    if (data != null) item {
                        val sample = data.observations.getOrNull(state.selectedRow)
                        if (sample?.timestampUs != null) TextButton(onClick = ::viewSelectedFrame, enabled = !state.busy) { Text("View video frame") }
                    }
                }
                LightPage.TABLE -> {
                    panelData.forEach { tableData ->
                        item {
                            Text("Region ${tableData.slot}")
                            LightResultsTable(tableData, state.selectedRow,
                                onSelect = { model.selectSlot(tableData.slot); model.row(it) },
                                onShowFrame = ::viewSelectedFrame,
                                modifier = Modifier.height(260.dp))
                        }
                    }
                }
                LightPage.SHARE -> {
                    item {
                        Text("Export: ${panelData.joinToString { it.slot }.ifEmpty { "No results" }}")
                        if (baseline || smooth) Text("Includes raw and processed values", style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button({ export(false, true, true) }, enabled = panelData.isNotEmpty() && !exporting && !state.busy) { Text("Share Excel") }
                            OutlinedButton({ export(false, false, true) }, enabled = panelData.isNotEmpty() && !exporting && !state.busy) { Text("Save Excel") }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button({ export(true, true) }, enabled = panelData.isNotEmpty() && !exporting && !state.busy) { Text("Share PDF") }
                            Button({ export(false, true) }, enabled = panelData.isNotEmpty() && !exporting && !state.busy) { Text("Share CSV") }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ export(true, false) }, enabled = panelData.isNotEmpty() && !exporting && !state.busy) { Text("Save PDF") }
                            OutlinedButton({ export(false, false) }, enabled = panelData.isNotEmpty() && !exporting && !state.busy) { Text("Save CSV") }
                        }
                    }
                    item {
                        OutlinedButton(onClick = model::clear, enabled = !state.busy && data != null) { Text("Clear results for ${state.active}") }
                    }
                }
            }
    }
    com.physicstracker.app.ui.ExperimentWorkspaceFrame(
        header = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = ::leave, modifier = Modifier.semantics { contentDescription = "Back to projects" }) { Text("‹") }
            Text(state.name.ifEmpty { "Light analysis" }, modifier = Modifier.weight(1f), maxLines = 1,
                style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = model::saveProjectNow, enabled = state.projectId != null && !state.busy) { Text("Save") }
        }
        if (state.preview != null) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(state.active, color = lightRegionColour(state.active), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp))
            LightTool.entries.forEach { tool ->
                com.physicstracker.app.ui.BottomDeckButton(tool.name, selected = geometry?.tool == tool,
                    enabled = !state.busy && !playing) { model.tool(tool) }
            }
            com.physicstracker.app.ui.BottomDeckButton("+ REGION", enabled = !state.busy && !playing && state.regionCount < MAX_LIGHT_REGIONS) {
                model.addRegion()
            }
            com.physicstracker.app.ui.BottomDeckButton("MAG", selected = precision) { precision = !precision }
        }
        if (state.preview != null) Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            com.physicstracker.app.ui.BottomDeckButton(
                if (state.recording) "STOP · KEEP RESULTS" else if (timeMode && state.video) "START TRACKING" else "MEASURE FRAME",
                enabled = state.recording || (!state.busy && (!playing || timeMode)),
                accent = Color.Cyan
            ) {
                if (state.recording) { model.stop(); player.pause() }
                else {
                    if (playing) player.pause()
                    model.analyze(timeMode && state.video)
                }
            }
        }
        if (state.busy || exporting) LinearProgressIndicator(Modifier.fillMaxWidth())
        },
        media = {
        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().background(Color.Black)
            .then(if (state.status.startsWith("Measured frame at")) Modifier.border(3.dp, lightRegionColour(state.active)) else Modifier)) {
            val stageWidthPx = constraints.maxWidth.toFloat()
            val stageHeightPx = constraints.maxHeight.toFloat()
            if (state.preview != null) {
                Box(Modifier.fillMaxSize().clipToBounds()) {
                    val transformed = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = zoom; scaleY = zoom; translationX = panX; translationY = panY
                    }
                    if (state.video && playing) LightVideoSurface(player, transformed)
                    LightSelectionImage(state.copy(busy = state.busy || playing), editStart, modifier = transformed,
                        showImage = !playing, panMode = panMode,
                        onTransform = { scale, delta ->
                            zoom = (zoom * scale).coerceIn(1f, 8f)
                            panX = (panX + delta.x).coerceIn(-stageWidthPx * (zoom - 1) / 2, stageWidthPx * (zoom - 1) / 2)
                            panY = (panY + delta.y).coerceIn(-stageHeightPx * (zoom - 1) / 2, stageHeightPx * (zoom - 1) / 2)
                        }, onPick = ::pick, onGeometry = model::geometry)
                }
            } else Column(Modifier.align(androidx.compose.ui.Alignment.Center), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("Open a photo or video", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Row {
                    Button({ photoPicker.launch(arrayOf("image/*")) }, enabled = !state.busy) { Text("Photo") }
                    Button({ videoPicker.launch(arrayOf("video/*")) }, enabled = !state.busy) { Text("Video") }
                }
            }
            if (state.status.startsWith("Measured frame at") && state.previewTimestampUs != null) {
                Surface(Modifier.align(androidx.compose.ui.Alignment.TopStart).padding(8.dp),
                    color = Color(0xFF093A41), contentColor = Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, lightRegionColour(state.active))) {
                    Text("FRAME ${String.format(Locale.US, "%.3f", state.previewTimestampUs!! / 1_000_000.0)} s  ·  ${state.active}",
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            windows.forEachIndexed { index, name ->
                key(name) {
                    val page = LightPage.valueOf(name)
                    LightWorkspaceWindow(if (page == LightPage.SHARE) "LIGHT · Export" else "LIGHT · ${page.name}",
                        maxWidth.value, maxHeight.value, index,
                        onClose = { windows = windows - name }) {
                        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp),
                            content = panelContent(page))
                    }
                }
            }
        }
        },
        tools = {
        Surface(color = Color.Black.copy(alpha = 0.86f)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.demoStep >= 0) {
                    LightDemoGuide(state.demoStep, state.busy, onNext = {
                        when (state.demoStep) {
                            0 -> { model.tool(LightTool.LINE); model.demoStep(1) }
                            1 -> { model.options(time = false); model.analyze(false); model.demoStep(2) }
                            2 -> { model.options(time = true); model.analyze(true); model.demoStep(3) }
                            else -> { model.stop(); model.saveProjectNow(); onOpenHistory() }
                        }
                    }, onDismiss = { model.demoStep(-1) })
                }
                val compactStatus = when {
                    state.saveStatus.startsWith("Save failed") -> state.saveStatus
                    state.status.contains("could not", ignoreCase = true) || state.status.contains("failed", ignoreCase = true) -> state.status
                    state.status.startsWith("Measured frame at") -> state.status
                    state.busy && !state.recording -> "Working…"
                    else -> ""
                }
                if (compactStatus.isNotBlank()) {
                    Text(compactStatus, Modifier.fillMaxWidth().padding(horizontal = 10.dp), maxLines = 2,
                        style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                }
                if (state.recording || (deck == "Light" && data != null)) {
                    Text("Result · ${state.active}", style = MaterialTheme.typography.titleSmall)
                    if (data != null) LightResultPlot(data, state.selectedRow, height = 140.dp, compact = true) { model.row(it) }
                    else LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (precision && state.preview != null && geometry != null) {
                    val point = if (geometry.tool == LightTool.POINT || editStart) geometry.start else geometry.end
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        LightPixelMagnifier(state.preview!!, point, lightRegionColour(state.active))
                        com.physicstracker.app.ui.PrecisionDirectionPad(enabled = !state.busy && !playing,
                            stepPx = 1f, onNudge = { dx, dy ->
                                pick(Offset((point.x + dx).coerceIn(0f, state.preview!!.width - 1f),
                                    (point.y + dy).coerceIn(0f, state.preview!!.height - 1f)))
                            }, onConfirm = { precision = false }, buttonSize = 34.dp,
                            buttonSpacing = 3.dp, rowSpacing = 3.dp, confirmLabel = "✓")
                    }
                    Text("${state.active}   x ${point.x.toInt()}   y ${point.y.toInt()}", style = MaterialTheme.typography.labelSmall)
                }

                run {
                    if (deck == "Light") {
                        if (state.regionCount > 1) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            lightRegionLabels(state.regionCount).forEach { slot ->
                                com.physicstracker.app.ui.BottomDeckButton(lightRegionControlLabel(slot, state.geometries[slot]),
                                    selected = state.active == slot, accent = lightRegionColour(slot),
                                    enabled = !state.busy && !playing) { model.selectSlot(slot) }
                            }
                        }
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (state.video) {
                                com.physicstracker.app.ui.BottomDeckButton("OVER TIME", selected = timeMode,
                                    enabled = !state.busy) { model.options(time = true) }
                                com.physicstracker.app.ui.BottomDeckButton("CURRENT FRAME", selected = !timeMode,
                                    enabled = !state.busy) { model.options(time = false) }
                            }
                        }
                    } else if (deck == "Data") {
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            com.physicstracker.app.ui.BottomDeckButton("OPEN ANALYSIS") { analysisFullScreen = true }
                        }
                    } else if (deck == "Play") {
                        if (state.video) {
                            LightPlaybackControls(player, !state.busy, model::previewAt)
                            LightSpeedControls(playbackRate, !state.busy || state.recording, ::setPlaybackRate)
                        }
                        else Text("Photo selected — playback applies to videos.", style = MaterialTheme.typography.bodySmall)
                    } else if (deck == "View") {
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            com.physicstracker.app.ui.BottomDeckButton(if (panMode) "PAN / ZOOM" else "SELECT", selected = panMode) { panMode = !panMode }
                            com.physicstracker.app.ui.BottomDeckButton("−") { zoom = (zoom / 1.25f).coerceAtLeast(1f); panX = 0f; panY = 0f }
                            com.physicstracker.app.ui.BottomDeckButton("+") { zoom = (zoom * 1.25f).coerceAtMost(8f) }
                            com.physicstracker.app.ui.BottomDeckButton("FIT") { zoom = 1f; panX = 0f; panY = 0f }
                            com.physicstracker.app.ui.BottomDeckButton("HIDE WINDOWS") { windows = emptyList(); precision = false }
                        }
                    } else {
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            com.physicstracker.app.ui.BottomDeckButton("OPEN PHOTO", enabled = !state.busy && !exporting) { player.pause(); photoPicker.launch(arrayOf("image/*")) }
                            com.physicstracker.app.ui.BottomDeckButton("OPEN VIDEO", enabled = !state.busy && !exporting) { player.pause(); videoPicker.launch(arrayOf("video/*")) }
                            com.physicstracker.app.ui.BottomDeckButton("PROPERTIES") { toggleWindow(LightPage.SELECT) }
                            com.physicstracker.app.ui.BottomDeckButton("CLEAR ${state.active}", enabled = !state.busy && data != null) { model.clear() }
                            com.physicstracker.app.ui.BottomDeckButton("EXPORT") { toggleWindow(LightPage.SHARE) }
                            com.physicstracker.app.ui.BottomDeckButton("SAVE PROJECT", enabled = !state.busy) { model.saveProjectNow() }
                            com.physicstracker.app.ui.BottomDeckButton("GUIDED DEMO", enabled = !state.busy) { model.openDemo() }
                            com.physicstracker.app.ui.BottomDeckButton("HISTORY") { model.stop(); model.saveProjectNow(); onOpenHistory() }
                        }
                    }
                }
            }
        }
        },
        navigation = {
                com.physicstracker.app.ui.MainBottomNavRow(
                    workspaceMode = com.physicstracker.app.ui.WorkspaceAnalysisMode.LIGHT,
                    viewSelected = deck == "View",
                    activeBottomTab = when (deck) {
                        "Light" -> com.physicstracker.app.ui.BottomMainTab.LIGHT
                        "Data" -> com.physicstracker.app.ui.BottomMainTab.DATA
                        "Play" -> com.physicstracker.app.ui.BottomMainTab.PLAY
                        "View" -> null
                        else -> com.physicstracker.app.ui.BottomMainTab.MORE
                    }, dataShortcutSelected = deck == "Data", playWhenReady = playing,
                    motionEnabled = state.video && state.uri != null && !state.busy && onOpenMotion != null,
                    playbackEnabled = state.video && !state.busy,
                    onToggleCal = { model.saveProjectNow(); player.pause(); state.uri?.let { onOpenMotion?.invoke(it, state.name, true) } },
                    onToggleTrack = { model.saveProjectNow(); player.pause(); state.uri?.let { onOpenMotion?.invoke(it, state.name, false) } },
                    onOpenData = { analysisFullScreen = true; analysisTab = LightDeskTab.GRAPH; model.options(compare = false) },
                    onTogglePlayPause = {
                        deck = "Play"
                        if (state.recording) { model.stop(); player.pause() }
                        else if (playing) { player.pause(); model.previewAt(player.currentPosition * 1000) }
                        else { player.seekTo((state.previewTimestampUs ?: 0) / 1000); player.play() }
                    }, onOpenView = { deck = "View" },
                    onToggleLight = { deck = "Light" },
                    onToggleMore = { deck = "More" }
                )
        }
    )
}

private fun formatLight(value: Double) = String.format(Locale.US, "%.4f", value)

/** Odd-sized source patch. Padding at edges preserves the exact same centre pixel. */
@Composable
private fun LightPixelMagnifier(bitmap: android.graphics.Bitmap, point: Offset, colour: Color) {
    Canvas(Modifier.size(112.dp).background(Color.Black).semantics {
        contentDescription = "Magnified source pixel x ${point.x.toInt()}, y ${point.y.toInt()}. Same pixel as the image crosshair."
    }) {
        val x = point.x.toInt()
        val y = point.y.toInt()
        val left = (x - 4).coerceAtLeast(0)
        val top = (y - 4).coerceAtLeast(0)
        val right = (x + 4).coerceAtMost(bitmap.width - 1)
        val bottom = (y + 4).coerceAtMost(bitmap.height - 1)
        val cell = size.width / 9
        withTransform({
            scale(cell, cell, pivot = Offset.Zero)
            translate((left - x + 4).toFloat(), (top - y + 4).toFloat())
        }) {
            val patch = IntSize(right - left + 1, bottom - top + 1)
            drawImage(bitmap.asImageBitmap(), srcOffset = IntOffset(left, top), srcSize = patch,
                dstOffset = IntOffset.Zero, dstSize = patch, filterQuality = FilterQuality.None)
        }
        val centre = Offset(size.width / 2f, size.height / 2f)
        drawRect(colour, Offset(cell * 4, cell * 4), Size(cell, cell), style = Stroke(2.dp.toPx()))
        drawCircle(colour, 9.dp.toPx(), centre, style = Stroke(1.5.dp.toPx()))
        drawLine(colour, centre - Offset(17.dp.toPx(), 0f), centre + Offset(17.dp.toPx(), 0f), 1.dp.toPx())
        drawLine(colour, centre - Offset(0f, 17.dp.toPx()), centre + Offset(0f, 17.dp.toPx()), 1.dp.toPx())
    }
}

@Composable
@android.annotation.SuppressLint("SuspiciousIndentation")
private fun LightSelectionImage(state: LightWorkspaceState, editStart: Boolean, modifier: Modifier = Modifier.fillMaxSize(),
    showImage: Boolean = true, panMode: Boolean = false, onTransform: (Float, Offset) -> Unit = { _, _ -> },
    onPick: (Offset) -> Unit, onGeometry: (com.physicstracker.app.ui.LightGeometry) -> Unit) {
    val bitmap = state.preview ?: return
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val mapping = LightImageMapping(bitmap.width, bitmap.height, viewport.width.toFloat(), viewport.height.toFloat())
    val latestPick by rememberUpdatedState(onPick)
    val latestGeometry by rememberUpdatedState(onGeometry)
    val activeGeometry by rememberUpdatedState(state.geometries[state.active])
    val latestTransform by rememberUpdatedState(onTransform)
    Box(modifier.onSizeChanged { viewport = it }
        .semantics { contentDescription = "Source image. Select a pixel by touch or use the labelled one-pixel arrow controls below." }
        .pointerInput(mapping, state.busy, panMode) { detectTapGestures { if (!state.busy && !panMode) mapping.toSource(it)?.let(latestPick) } }
        .pointerInput(mapping, state.busy, panMode, state.active, state.geometries[state.active]?.tool) {
            if (panMode) detectTransformGestures { _, pan, scale, _ -> latestTransform(scale, pan) }
            else {
                var original: com.physicstracker.app.ui.LightGeometry? = null
                var origin: Offset? = null
                var target = LightDragTarget.NEW
                detectDragGestures(
                    onDragStart = { touch ->
                        original = activeGeometry
                        origin = mapping.toSourceClamped(touch)
                        original?.let { target = lightDragTarget(it, mapping, touch, 36.dp.toPx()) }
                    },
                    onDrag = { change, _ ->
                        val start = origin
                        val selection = original
                        val current = mapping.toSourceClamped(change.position)
                        if (!state.busy && start != null && selection != null && current != null) {
                            change.consume()
                            latestGeometry(dragLightGeometry(selection, target, start, current, bitmap.width, bitmap.height))
                        }
                    })
            }
        }) {
        if (showImage) Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Canvas(Modifier.fillMaxSize()) {
            state.geometries.filterKeys { it in lightRegionLabels(state.regionCount) }.forEach { (slot, geometry) ->
                val a = mapping.toView(geometry.start)
                val b = mapping.toView(geometry.end)
                val colour = lightRegionColour(slot)
                when (geometry.tool) {
                    LightTool.LINE -> drawLine(colour, a, b, 2.dp.toPx())
                    LightTool.BOX -> drawRect(colour, Offset(min(a.x, b.x), min(a.y, b.y)), Size(abs(b.x - a.x), abs(b.y - a.y)), style = Stroke(2.dp.toPx()))
                    LightTool.POINT -> Unit
                }
                val chosen = if (geometry.tool == LightTool.POINT || editStart) a else b
                drawCircle(Color.Black, 11.dp.toPx(), a)
                drawCircle(colour, 9.dp.toPx(), a, style = Stroke(3.dp.toPx()))
                if (geometry.tool != LightTool.POINT) {
                    drawCircle(Color.Black, 11.dp.toPx(), b)
                    drawCircle(colour, 9.dp.toPx(), b, style = Stroke(3.dp.toPx()))
                }
                if (slot == state.active) {
                    val arm = 17.dp.toPx()
                    drawLine(colour, chosen - Offset(arm, 0f), chosen + Offset(arm, 0f), 2.dp.toPx())
                    drawLine(colour, chosen - Offset(0f, arm), chosen + Offset(0f, arm), 2.dp.toPx())
                }
            }
        }
    }
}

@Composable
internal fun LightResultPlot(data: LightDataset, selected: Int, height: androidx.compose.ui.unit.Dp = 190.dp,
    compact: Boolean = false, fit: LightStatistics? = null, onSelect: (Int) -> Unit) {
    val bounds = remember(data) { LightPlotBounds.of(data.observations) }
    val plotted = remember(data) { lightPlotSamples(data.observations) }
    val colour = lightRegionColour(data.slot)
    if (!compact) Text("${data.channel.displayName()} · 0–255")
    Canvas(Modifier.fillMaxWidth().height(height).clipToBounds().background(Color(0xff101820))
        .semantics { contentDescription = "${data.channel.displayName()} graph for region ${data.slot}. Tap to select a sample." }
        .pointerInput(data) { detectTapGestures { touch ->
            val index = data.observations.indices.minByOrNull { i ->
                val p = bounds.normalized(data.observations[i])
                (Offset(12 + p.x * (size.width - 24), 12 + p.y * (size.height - 24)) - touch).getDistance()
            }
            index?.let(onSelect)
        } }) {
        fun position(sample: LightObservation): Offset {
            val p = bounds.normalized(sample)
            return Offset(12 + p.x * (size.width - 24), 12 + p.y * (size.height - 24))
        }
        (0..4).forEach { step ->
            val y = 12f + step * (size.height - 24f) / 4f
            drawLine(Color.Gray.copy(alpha = 0.28f), Offset(12f, y), Offset(size.width - 12f, y), 1f)
        }
        drawLine(Color.Gray, Offset(12f, 12f), Offset(12f, size.height - 12), 1f)
        drawLine(Color.Gray, Offset(12f, size.height - 12), Offset(size.width - 12, size.height - 12), 1f)
        plotted.zipWithNext().forEach { (a, b) -> if (!b.gapBefore) drawLine(colour, position(a), position(b), 2f) }
        if (plotted.size == 1) drawCircle(colour, 4f, position(plotted[0]))
        if (fit?.slope != null && fit.intercept != null) {
            val left = LightObservation(bounds.minX, fit.slope * bounds.minX + fit.intercept)
            val right = LightObservation(bounds.maxX, fit.slope * bounds.maxX + fit.intercept)
            drawLine(Color(0xFFFFD54F), position(left), position(right), 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 7f)))
        }
        data.observations.getOrNull(selected)?.let {
            val selectedPoint = position(it)
            drawLine(Color.White.copy(alpha = 0.7f), Offset(selectedPoint.x, 12f),
                Offset(selectedPoint.x, size.height - 12f), 1.5.dp.toPx())
            drawCircle(Color.Black, 11.dp.toPx(), selectedPoint)
            drawCircle(Color.White, 9.dp.toPx(), selectedPoint, style = Stroke(3.dp.toPx()))
            drawCircle(colour, 3.dp.toPx(), selectedPoint)
        }
    }
    if (!compact) {
        Text("${formatLight(bounds.minX)} – ${formatLight(bounds.maxX)} · ${data.axisLabel}")
    }
}
