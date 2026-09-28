package com.physicstracker.app.live.ui

import android.content.Intent
import android.content.res.Configuration
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.physicstracker.app.live.LivePhysicsPoint
import com.physicstracker.app.live.LiveTrackingState
import com.physicstracker.app.live.LiveTrackingViewModel
import com.physicstracker.app.live.LiveUiState
import com.physicstracker.app.live.data.LiveSessionEntity
import com.physicstracker.app.ui.PhysicsColorAccent
import com.physicstracker.app.ui.PhysicsColorBackground
import com.physicstracker.app.ui.PhysicsColorDivider
import com.physicstracker.app.ui.PhysicsColorError
import com.physicstracker.app.ui.PhysicsColorOnBackground
import com.physicstracker.app.ui.PhysicsColorOnSurface
import com.physicstracker.app.ui.PhysicsColorPrimary
import com.physicstracker.app.ui.PhysicsColorSurface
import com.physicstracker.app.ui.PhysicsColorSurfaceVariant
import com.physicstracker.app.ui.PhysicsColorTrackA
import com.physicstracker.app.ui.PhysicsColorTrackB
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * Live Tracking: a minimal real-time physics instrument.
 * Open → point → tap object → track → analyze → save.
 * Portrait-first, fixed layout: numbers change, controls never move.
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@Composable
fun LiveTrackingScreen(
    onBack: () -> Unit,
    viewModel: LiveTrackingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val uiState by viewModel.uiState.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    val activity = context as? android.app.Activity
    DisposableEffect(Unit) {
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { previous?.let { activity.requestedOrientation = it } }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showSessionsDialog by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            keepScreenOn = true
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { analysisExecutor.shutdown() } }

    LaunchedEffect(Unit) {
        viewModel.pendingCsv.collect { csv -> shareRunCsv(context, csv) }
    }

    LaunchedEffect(hasCameraPermission, configuration.orientation) {
        if (!hasCameraPermission) return@LaunchedEffect
        bindLiveCamera(context, lifecycleOwner, previewView, analysisExecutor,
            onFrame = viewModel::onFrame,
            onCharacteristics = viewModel::onCameraCharacteristics,
            onFocusDistance = viewModel::onFocusDistance,
            onError = viewModel::onCameraError)
    }

    LaunchedEffect(uiState.calibrationPoints, uiState.state) {
        if (uiState.state == LiveTrackingState.CALIBRATING && uiState.calibrationPoints.size >= 2) {
            showCalibrationDialog = true
        }
    }

    // Manual orientation only: user picks PORTRAIT / LANDSCAPE / APP in SETTINGS.
    val livePrefs = remember { context.getSharedPreferences("physics_tracker_live", 0) }
    var orientationMode by remember { mutableStateOf(livePrefs.getString("live_orientation_mode", "PORTRAIT") ?: "PORTRAIT") }
    DisposableEffect(orientationMode) {
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = when (orientationMode) {
            "LANDSCAPE" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "APP" -> previous ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose { previous?.let { activity.requestedOrientation = it } }
    }

    var activeTab by rememberSaveable { mutableStateOf(LiveMainTab.TRACK) }

    Box(modifier = Modifier.fillMaxSize().background(PhysicsColorBackground)) {
        if (hasCameraPermission) {
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                if (!isLandscape) {
                    // Portrait: thin top bar with tabs. Landscape: tabs live in the right panel.
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopBarButton("<", onBack)
                        LiveTabChip("TRACK", activeTab == LiveMainTab.TRACK) { activeTab = LiveMainTab.TRACK }
                        LiveTabChip("ANALYSIS", activeTab == LiveMainTab.ANALYSIS) { activeTab = LiveMainTab.ANALYSIS }
                        LiveTabChip("CAL", activeTab == LiveMainTab.CAL) { activeTab = LiveMainTab.CAL }
                        LiveTabChip("SETTINGS", activeTab == LiveMainTab.SETTINGS) { activeTab = LiveMainTab.SETTINGS }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        LiveMainTab.TRACK, LiveMainTab.CAL ->
                            if (isLandscape) {
                                Row(Modifier.fillMaxSize()) {
                                    Box(Modifier.weight(2.2f).fillMaxHeight()) {
                                        CameraStage(previewView, uiState, viewModel, showChrome = false)
                                    }
                                    LandscapeSidePanel(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        activeTab = activeTab,
                                        onTab = { activeTab = it },
                                        onBack = onBack,
                                        onSave = { showSaveDialog = true },
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                    )
                                }
                            } else {
                                Column(Modifier.fillMaxSize()) {
                                    Box(Modifier.weight(1f)) {
                                        CameraStage(previewView, uiState, viewModel)
                                    }
                                    if (activeTab == LiveMainTab.CAL) {
                                        ScaleStrip(uiState, viewModel)
                                    } else {
                                        TrackControlsBar(uiState, viewModel, onSave = { showSaveDialog = true })
                                    }
                                }
                            }
                        LiveMainTab.ANALYSIS -> if (isLandscape) {
                            Row(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(2.2f).fillMaxHeight()) {
                                    LiveAnalysisTab(
                                        points = uiState.analysisPoints,
                                        tick = uiState.analysisTick,
                                        calibrated = uiState.calibrationConfirmed,
                                        viewModel = viewModel,
                                        onBackToCamera = { activeTab = LiveMainTab.TRACK }
                                    )
                                }
                                LandscapeSidePanel(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    activeTab = activeTab,
                                    onTab = { activeTab = it },
                                    onBack = onBack,
                                    onSave = { showSaveDialog = true },
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        } else {
                            LiveAnalysisTab(
                                points = uiState.analysisPoints,
                                tick = uiState.analysisTick,
                                calibrated = uiState.calibrationConfirmed,
                                viewModel = viewModel,
                                onBackToCamera = { activeTab = LiveMainTab.TRACK }
                            )
                        }
                        LiveMainTab.SETTINGS -> if (isLandscape) {
                            Row(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(2.2f).fillMaxHeight()) {
                                    CameraStage(previewView, uiState, viewModel, showChrome = false)
                                }
                                Column(Modifier.weight(1f).fillMaxHeight()) {
                                    LandscapeTabRow(activeTab = activeTab, onTab = { activeTab = it }, onBack = onBack)
                                    LiveSettingsTab(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        orientationMode = orientationMode,
                                        onOrientationMode = {
                                            orientationMode = it
                                            livePrefs.edit().putString("live_orientation_mode", it).apply()
                                        },
                                        onSessions = { showSessionsDialog = true }
                                    )
                                }
                            }
                        } else {
                            LiveSettingsTab(
                                uiState = uiState,
                                viewModel = viewModel,
                                orientationMode = orientationMode,
                                onOrientationMode = {
                                    orientationMode = it
                                    livePrefs.edit().putString("live_orientation_mode", it).apply()
                                },
                                onSessions = { showSessionsDialog = true }
                            )
                        }
                    }
                }
            }
        } else {
            CameraPermissionContent(
                onRequest = { permissionLauncher.launch(android.Manifest.permission.CAMERA) }
            )
        }
    }

    if (showCalibrationDialog && uiState.calibrationPoints.size >= 2) {
        CalibrationLengthDialog(
            onDismiss = { showCalibrationDialog = false; viewModel.cancelCalibration() },
            onConfirm = { length, unit ->
                showCalibrationDialog = false
                viewModel.applyCalibration(length, unit)
            }
        )
    }
    if (showSaveDialog) {
        SaveSessionDialog(
            defaultName = "Live ${SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date())}",
            onDismiss = { showSaveDialog = false },
            onConfirm = { name -> viewModel.saveSession(name) { showSaveDialog = false } }
        )
    }
    if (showSessionsDialog) {
        LiveSessionsDialog(
            sessions = sessions,
            onDismiss = { showSessionsDialog = false },
            onDelete = viewModel::deleteSession,
            onShare = { shareLiveSession(context, it) }
        )
    }
}

// -------------------------------------------------------------------------
// Camera binding (+ camera2 optics for automatic scale estimation)
// -------------------------------------------------------------------------

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
private fun bindLiveCamera(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    analysisExecutor: Executor,
    onFrame: (androidx.camera.core.ImageProxy) -> Unit,
    onCharacteristics: (FloatArray?, Double?, Int?) -> Unit,
    onFocusDistance: (Double?) -> Unit,
    onError: (String) -> Unit
) {
    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener({
        try {
            val provider = providerFuture.get()
            val rotation: Int = previewView.display?.rotation ?: Surface.ROTATION_0

            val focusCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    onFocusDistance(result.get(CaptureResult.LENS_FOCUS_DISTANCE)?.toDouble())
                }
            }
            val previewBuilder = Preview.Builder().setTargetRotation(rotation)
            Camera2Interop.Extender(previewBuilder).setSessionCaptureCallback(focusCallback)
            val preview = previewBuilder.build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysisUseCase ->
                    analysisUseCase.setAnalyzer(analysisExecutor) { image -> onFrame(image) }
                }

            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(analysis)
                    .setViewPort(
                        ViewPort.Builder(
                            android.util.Rational(
                                maxOf(context.resources.configuration.screenWidthDp, 1),
                                maxOf(context.resources.configuration.screenHeightDp, 1)
                            ),
                            rotation
                        ).build()
                    )
                    .build()
            )

            val info = Camera2CameraInfo.from(camera.cameraInfo)
            onCharacteristics(
                info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS),
                info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.width?.toDouble(),
                info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION)
            )
        } catch (failure: Exception) {
            onError(failure.message ?: "Camera initialization failed")
        }
    }, ContextCompat.getMainExecutor(context))
}

// -------------------------------------------------------------------------
// Camera stage (preview + minimal chrome)
// -------------------------------------------------------------------------

@Composable
private fun androidx.compose.foundation.layout.BoxScope.CameraStage(
    previewView: PreviewView,
    uiState: LiveUiState,
    viewModel: LiveTrackingViewModel,
    showChrome: Boolean = true
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        // Tap = select target (only while awaiting) · long-press = reselect target.
        TrackingOverlay(
            uiState = uiState,
            onFrameTap = { frameX, frameY ->
                when (uiState.state) {
                    LiveTrackingState.AWAITING_TARGET,
                    LiveTrackingState.OBJECT_LOST -> viewModel.seedAt(frameX, frameY)
                    LiveTrackingState.CALIBRATING -> viewModel.onCalibrationTap(frameX, frameY)
                    else -> Unit
                }
            },
            onLongPress = {
                if (uiState.state != LiveTrackingState.CALIBRATING) viewModel.reselectObject()
            },
            onCalibrationDrag = viewModel::moveCalibrationPoint
        )
        if (showChrome) {
            StatusChip(
                uiState,
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
            BottomHint(uiState, modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp))
        }
    }
}

@Composable
private fun TopBarButton(label: String, onClick: () -> Unit) {
    Surface(
        color = PhysicsColorSurface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider)
    ) {
        TextButton(onClick = onClick, contentPadding = ButtonDefaults.TextButtonContentPadding) {
            Text(label, color = PhysicsColorOnBackground)
        }
    }
}


@Composable
private fun TrackingOverlay(
    uiState: LiveUiState,
    onFrameTap: (Float, Float) -> Unit,
    onLongPress: () -> Unit,
    onCalibrationDrag: (Int, Float, Float) -> Unit = { _, _, _ -> }
) {
    val mapTap: (Offset, androidx.compose.ui.unit.IntSize) -> Unit = { offset, viewSize ->
        val bw = uiState.frameWidth.toFloat()
        val bh = uiState.frameHeight.toFloat()
        if (bw > 0f && bh > 0f) {
            val scale = min(viewSize.width / bw, viewSize.height / bh)
            val dx = (viewSize.width - bw * scale) / 2f
            val dy = (viewSize.height - bh * scale) / 2f
            val x = (offset.x - dx) / scale
            val y = (offset.y - dy) / scale
            if (x >= 0f && y >= 0f && x <= bw && y <= bh) onFrameTap(x, y)
        }
    }
    val onFrameDrag: (Offset, androidx.compose.ui.unit.IntSize) -> Unit = { _, _ -> }
    val dragHandler = androidx.compose.ui.Modifier.pointerInput(uiState.state, uiState.frameWidth, uiState.frameHeight) {
        if (uiState.state != LiveTrackingState.CALIBRATING) return@pointerInput
        var draggedIndex: Int? = null
        detectDragGestures(
            onDragStart = { start ->
                val bw = uiState.frameWidth.toFloat()
                val bh = uiState.frameHeight.toFloat()
                if (bw > 0f && bh > 0f) {
                    val scale = min(size.width / bw, size.height / bh)
                    val dx = (size.width - bw * scale) / 2f
                    val dy = (size.height - bh * scale) / 2f
                    val threshold = 48f * 48f // view-space distance^2
                    draggedIndex = uiState.calibrationPoints.indices.minByOrNull { i ->
                        val p = uiState.calibrationPoints[i]
                        val vx = dx + p.first * scale
                        val vy = dy + p.second * scale
                        (vx - start.x) * (vx - start.x) + (vy - start.y) * (vy - start.y)
                    }?.takeIf { i ->
                        val p = uiState.calibrationPoints[i]
                        val vx = dx + p.first * scale
                        val vy = dy + p.second * scale
                        (vx - start.x) * (vx - start.x) + (vy - start.y) * (vy - start.y) <= threshold
                    }
                }
            },
            onDrag = { change, dragAmount ->
                val idx = draggedIndex ?: return@detectDragGestures
                val bw = uiState.frameWidth.toFloat()
                val bh = uiState.frameHeight.toFloat()
                if (bw > 0f && bh > 0f) {
                    val scale = min(size.width / bw, size.height / bh)
                    val current = uiState.calibrationPoints.getOrNull(idx) ?: return@detectDragGestures
                    onCalibrationDrag(idx, current.first + dragAmount.x / scale, current.second + dragAmount.y / scale)
                }
                change.consume()
            }
        )
    }
    if (!uiState.showOverlay) {
        // Keep taps working without drawing anything.
        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(uiState.state) {
                detectTapGestures(onTap = { mapTap(it, size) }, onLongPress = { onLongPress() })
            })
        return
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .then(dragHandler)
            .pointerInput(uiState.state, uiState.frameWidth, uiState.frameHeight) {
                detectTapGestures(onTap = { mapTap(it, size) }, onLongPress = { onLongPress() })
            }
    ) {
        val bw = uiState.frameWidth.toFloat()
        val bh = uiState.frameHeight.toFloat()
        if (bw <= 0f || bh <= 0f) return@Canvas
        val scale = min(size.width / bw, size.height / bh)
        val dx = (size.width - bw * scale) / 2f
        val dy = (size.height - bh * scale) / 2f
        fun map(p: Pair<Float, Float>) = Offset(dx + p.first * scale, dy + p.second * scale)

        // Calibration stick
        if (uiState.calibrationPoints.isNotEmpty()) {
            val color = PhysicsColorAccent
            val points = uiState.calibrationPoints.map(::map)
            if (points.size == 2) drawLine(color, points[0], points[1], strokeWidth = 3f)
            points.forEach { drawCircle(color, radius = 9f, center = it) }
        }

        // Trajectory (toggleable)
        if (uiState.showTrajectory && uiState.trail.size > 1) {
            val path = Path().apply {
                uiState.trail.map(::map).forEachIndexed { index, p ->
                    if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
            }
            drawPath(path, PhysicsColorTrackA.copy(alpha = 0.55f), style = Stroke(width = 4f))
        }

        uiState.positionPx?.let { pos ->
            val center = map(pos)
            val color = when (uiState.state) {
                LiveTrackingState.OBJECT_LOST -> PhysicsColorError
                LiveTrackingState.LOW_CONFIDENCE -> PhysicsColorTrackB
                LiveTrackingState.REACQUIRING -> PhysicsColorTrackB
                else -> PhysicsColorTrackA
            }
            val r = (uiState.estimatedRadiusPx * scale).coerceIn(14f, 90f)
            // bounding circle (object extent) + center marker — lightweight, never fills
            drawCircle(color.copy(alpha = 0.35f), radius = r, center = center, style = Stroke(3f))
            drawCircle(color, radius = 9f, center = center)
            drawCircle(Color.White, radius = 2.5f, center = center)
        }

        // Prediction ghost during reacquisition
        if (uiState.state == LiveTrackingState.REACQUIRING) {
            uiState.predictedPx?.let { pred ->
                val c = map(pred)
                drawCircle(PhysicsColorTrackB.copy(alpha = 0.45f), radius = 26f, center = c, style = Stroke(2f))
            }
        }

        if (uiState.state == LiveTrackingState.AWAITING_TARGET) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(PhysicsColorTrackB.copy(alpha = 0.4f), radius = 34f, center = center, style = Stroke(2f))
        }
    }
}

// -------------------------------------------------------------------------
// Live panel (fixed layout: numbers change, layout never moves)
// -------------------------------------------------------------------------


// -------------------------------------------------------------------------
// Scale strip (one line; auto estimate or confirmed scale; manual fallback)
// -------------------------------------------------------------------------

@Composable
private fun ScaleStrip(uiState: LiveUiState, viewModel: LiveTrackingViewModel) {
    Surface(
        color = PhysicsColorSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                uiState.calibrationConfirmed && uiState.calibration != null -> {
                    Text(
                        "Scale %.4f m/px".format(Locale.US, uiState.calibration.metersPerPixel),
                        color = PhysicsColorTrackA,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = viewModel::recalibrateAuto) { Text("Auto") }
                    TextButton(onClick = viewModel::beginCalibration) { Text("Manual") }
                }
                uiState.scaleEstimate != null -> {
                    val est = uiState.scaleEstimate
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "≈ %.4f m/px @ %.2f m".format(Locale.US, est.metersPerPixel, est.distanceMeters),
                            color = PhysicsColorOnBackground,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("estimate · %s".format(Locale.US, est.focusQuality.name.lowercase(Locale.US)),
                            color = PhysicsColorOnSurface, style = MaterialTheme.typography.labelSmall)
                    }
                    Button(onClick = viewModel::confirmScaleEstimate, shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PhysicsColorPrimary,
                            contentColor = PhysicsColorBackground)) {
                        Text("Use")
                    }
                    TextButton(onClick = viewModel::beginCalibration) { Text("Stick") }
                }
                else -> {
                    Text("Scale needed", color = PhysicsColorTrackB,
                        style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = viewModel::beginCalibration, shape = RoundedCornerShape(8.dp)) {
                        Text("Calibrate")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Compact stats strip (fixed height)
// -------------------------------------------------------------------------


// -------------------------------------------------------------------------
// Review strip (stopped run)
// -------------------------------------------------------------------------


// -------------------------------------------------------------------------
// Controls (fixed bar)
// -------------------------------------------------------------------------

@Composable
private fun TrackControlsBar(uiState: LiveUiState, viewModel: LiveTrackingViewModel, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            uiState.reviewMode -> {
                ControlButton("Save", onSave)
                ControlButton("Reset", viewModel::resetRun, accent = false)
                ControlButton("Track", accent = false, onClick = viewModel::dismissReview)
            }
            uiState.state == LiveTrackingState.PAUSED -> {
                ControlButton("Resume", viewModel::resume)
                ControlButton("Stop", viewModel::stop, accent = false)
            }
            uiState.state == LiveTrackingState.RECORDING -> {
                ControlButton("Pause", viewModel::pause, accent = false)
                ControlButton("Stop", viewModel::stop)
            }
            uiState.state == LiveTrackingState.TRACKING -> {
                ControlButton("Start", viewModel::startRecording)
            }
            uiState.state == LiveTrackingState.OBJECT_LOST -> {
                ControlButton("Reselect", viewModel::reselectObject)
            }
            uiState.state == LiveTrackingState.REACQUIRING -> {
                ControlButton("Reselect", viewModel::reselectObject, accent = false)
                ControlButton("Reset", viewModel::resetRun, accent = false)
            }
            else -> Unit // awaiting target: the camera itself is the control
        }
    }
}

@Composable
private fun ControlButton(label: String, onClick: () -> Unit, accent: Boolean = true) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = if (accent) ButtonDefaults.buttonColors(containerColor = PhysicsColorPrimary,
            contentColor = PhysicsColorBackground)
        else ButtonDefaults.buttonColors(containerColor = PhysicsColorSurfaceVariant)
    ) { Text(label) }
}

@Composable
private fun StatusChip(uiState: LiveUiState, modifier: Modifier = Modifier) {
    val (dot, label) = stateAppearance(uiState)
    Surface(
        color = PhysicsColorSurface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(9.dp).background(dot, CircleShape))
            Text(label, color = PhysicsColorOnBackground,
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            if (uiState.recording) {
                Text(
                    "%.1f s · %d pts".format(Locale.US, uiState.recordedDurationUs / 1e6, uiState.recordedCount),
                    color = PhysicsColorOnSurface,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun stateAppearance(uiState: LiveUiState): Pair<Color, String> = when (uiState.state) {
    LiveTrackingState.RECORDING ->
        PhysicsColorTrackA to "Recording · conf ${"%.2f".format(Locale.US, uiState.confidence)}"
    LiveTrackingState.TRACKING ->
        PhysicsColorTrackA to "Tracking · conf ${"%.2f".format(Locale.US, uiState.confidence)}"
    LiveTrackingState.PAUSED -> PhysicsColorTrackB to "Paused"
    LiveTrackingState.LOW_CONFIDENCE -> PhysicsColorTrackB to "Low confidence"
    LiveTrackingState.REACQUIRING -> PhysicsColorTrackB to "Reacquiring…"
    LiveTrackingState.OBJECT_LOST -> PhysicsColorError to "Object lost — tap the object again"
    LiveTrackingState.CALIBRATING -> PhysicsColorAccent to "Calibrate: tap the ends of a known length"
    LiveTrackingState.AWAITING_TARGET -> PhysicsColorTrackB to "Tap an object to track"
    LiveTrackingState.CAMERA_ERROR -> PhysicsColorError to "Camera error"
    LiveTrackingState.IDLE -> PhysicsColorOnSurface to "Ready"
}

@Composable
private fun BottomHint(uiState: LiveUiState, modifier: Modifier = Modifier) {
    val hint = when {
        uiState.reviewMode -> "Review mode — save or reset when done"
        uiState.state == LiveTrackingState.OBJECT_LOST -> "Object lost — tap it again or use Reselect"
        uiState.state == LiveTrackingState.REACQUIRING -> "Reacquiring near the last position…"
        !uiState.calibrationConfirmed && uiState.state == LiveTrackingState.TRACKING ->
            "Confirm the scale below to unlock physics units"
        else -> null
    } ?: return
    Surface(
        color = PhysicsColorSurface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            hint,
            color = PhysicsColorOnBackground,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}



// -------------------------------------------------------------------------
// Tabs (workspace-style navigation)
// -------------------------------------------------------------------------

private enum class LiveMainTab { TRACK, ANALYSIS, CAL, SETTINGS }

@Composable
private fun LiveTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        border = BorderStroke(
            1.dp,
            if (selected) PhysicsColorPrimary else PhysicsColorDivider
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) PhysicsColorPrimary.copy(alpha = 0.18f) else PhysicsColorSurface,
            contentColor = if (selected) PhysicsColorPrimary else PhysicsColorOnSurface
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

// -------------------------------------------------------------------------
// SETTINGS tab: orientation (manual only), display toggles, session history
// -------------------------------------------------------------------------

@Composable
private fun LiveSettingsTab(
    uiState: LiveUiState,
    viewModel: LiveTrackingViewModel,
    orientationMode: String,
    onOrientationMode: (String) -> Unit,
    onSessions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingsCard(title = "Orientation") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("PORTRAIT", "LANDSCAPE", "APP").forEach { mode ->
                    LiveTabChip(mode, orientationMode == mode) { onOrientationMode(mode) }
                }
            }
        }
        SettingsCard(title = "Display") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiveTabChip(if (uiState.showOverlay) "Overlay on" else "Overlay off", uiState.showOverlay) {
                    viewModel.toggleOverlay()
                }
                LiveTabChip(if (uiState.showTrajectory) "Trail on" else "Trail off", uiState.showTrajectory) {
                    viewModel.toggleTrajectory()
                }
            }
        }
        SettingsCard(title = "Sessions") {
            LiveTabChip("Saved sessions", false, onSessions)
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        color = PhysicsColorSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = PhysicsColorOnSurface, style = MaterialTheme.typography.labelSmall)
            content()
        }
    }
}

// -------------------------------------------------------------------------
// Dialogs
// -------------------------------------------------------------------------

@Composable
private fun CalibrationLengthDialog(onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var lengthText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("m") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Known reference length") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter the real length of the reference you marked on the preview.")
                TextField(value = lengthText, onValueChange = { lengthText = it }, singleLine = true,
                    label = { Text("Length") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("mm", "cm", "m").forEach { option ->
                        OutlinedButton(onClick = { unit = option },
                            colors = if (unit == option) ButtonDefaults.outlinedButtonColors(contentColor = PhysicsColorPrimary)
                            else ButtonDefaults.outlinedButtonColors()
                        ) { Text(option) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { lengthText.trim().toDoubleOrNull()?.let { onConfirm(it, unit) } },
                enabled = lengthText.trim().toDoubleOrNull()?.let { it > 0 } == true) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SaveSessionDialog(defaultName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save live session") },
        text = {
            TextField(value = name, onValueChange = { name = it }, singleLine = true,
                label = { Text("Session name") }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { Button(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LiveSessionsDialog(
    sessions: List<LiveSessionEntity>,
    onDismiss: () -> Unit,
    onDelete: (LiveSessionEntity) -> Unit,
    onShare: (LiveSessionEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved live sessions") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (sessions.isEmpty()) Text("No live sessions saved yet.", color = PhysicsColorOnSurface)
                sessions.forEach { session ->
                    Surface(color = PhysicsColorSurfaceVariant, shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(session.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "%d samples · %.1f s · %s".format(Locale.US, session.sampleCount,
                                    session.durationUs / 1e6, session.physicsMode),
                                color = PhysicsColorOnSurface, style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                            session.metersPerPixel?.let {
                                Text("Scale %.5f m/px".format(Locale.US, it), color = PhysicsColorOnSurface,
                                    style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onShare(session) }) { Text("Share CSV") }
                                TextButton(onClick = { onDelete(session) }) { Text("Delete", color = PhysicsColorError) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun shareLiveSession(context: android.content.Context, session: LiveSessionEntity) {
    runCatching {
        val dir = context.getExternalFilesDir("Documents") ?: context.filesDir
        val file = File(dir, "live_session_${session.id}.csv")
        file.writeText(
            buildString {
                appendLine("# PhysicsTracker live tracking session")
                appendLine("# name: ${session.name}")
                appendLine("# created: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(session.createdAtEpochMs))}")
                appendLine("# physics_mode: ${session.physicsMode}")
                appendLine("# tracker: ${session.trackerAlgorithm}")
                appendLine("# meters_per_pixel: ${session.metersPerPixel ?: "uncalibrated"}")
                appendLine("t_us,x_px,y_px,confidence,x_m,y_m,vx,vy,ax,ay,speed,accel")
                append(session.samplesCsv)
            }
        )
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share live session"))
    }
}

@Composable
private fun CameraPermissionContent(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Camera permission required", color = PhysicsColorOnBackground,
            style = MaterialTheme.typography.titleMedium)
        Text("Live Tracking needs the camera to follow an object in real time. Everything runs on-device and offline.",
            color = PhysicsColorOnSurface, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRequest, modifier = Modifier.padding(top = 16.dp)) { Text("Grant permission") }
    }
}


private fun shareRunCsv(context: android.content.Context, csv: String) {
    runCatching {
        val dir = context.getExternalFilesDir("Documents") ?: context.filesDir
        val file = File(dir, "live_run_${System.currentTimeMillis()}.csv")
        file.writeText("t_us,x_px,y_px,confidence,x_m,y_m,vx,vy,ax,ay,speed,accel\n" + csv)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share run CSV"))
    }
}


// -------------------------------------------------------------------------
// Landscape: dedicated right-side panel with all controls + real-time graph
// -------------------------------------------------------------------------

@Composable
private fun LandscapeSidePanel(
    uiState: LiveUiState,
    viewModel: LiveTrackingViewModel,
    activeTab: LiveMainTab,
    onTab: (LiveMainTab) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LandscapeTabRow(activeTab = activeTab, onTab = onTab, onBack = onBack)
        StatusChip(uiState, Modifier.fillMaxWidth())
        LandscapeHint(uiState)
        if (activeTab == LiveMainTab.CAL) {
            ScaleStrip(uiState, viewModel)
        }
        if (uiState.recording || uiState.graphPoints.size > 1) {
            RollingLiveGraph(uiState)
        }
        TrackControlsBar(uiState, viewModel, onSave)
    }
}

@Composable
private fun LandscapeTabRow(
    activeTab: LiveMainTab,
    onTab: (LiveMainTab) -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarButton("<", onBack)
            LiveTabChip("TRACK", activeTab == LiveMainTab.TRACK) { onTab(LiveMainTab.TRACK) }
            LiveTabChip("ANALYSIS", activeTab == LiveMainTab.ANALYSIS) { onTab(LiveMainTab.ANALYSIS) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LiveTabChip("CAL", activeTab == LiveMainTab.CAL) { onTab(LiveMainTab.CAL) }
            LiveTabChip("SETTINGS", activeTab == LiveMainTab.SETTINGS) { onTab(LiveMainTab.SETTINGS) }
        }
    }
}

/** Compact hint line used inside the landscape side panel (never over the video). */
@Composable
private fun LandscapeHint(uiState: LiveUiState) {
    val hint = when {
        uiState.reviewMode -> "Review: save or reset"
        uiState.state == LiveTrackingState.OBJECT_LOST -> "Object lost — tap it again"
        uiState.state == LiveTrackingState.REACQUIRING -> "Reacquiring…"
        uiState.state == LiveTrackingState.CALIBRATING ->
            if (uiState.calibrationPoints.size < 2) "Tap both ends of a known length" else "Drag ends to adjust"
        !uiState.calibrationConfirmed && uiState.state == LiveTrackingState.TRACKING ->
            "Confirm scale for physics units"
        else -> null
    } ?: return
    Surface(color = PhysicsColorSurfaceVariant, shape = RoundedCornerShape(8.dp)) {
        Text(
            hint,
            color = PhysicsColorOnBackground,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/** Compact real-time rolling graph for the landscape side panel (x/y vs time). */
@Composable
private fun RollingLiveGraph(uiState: LiveUiState) {
    Surface(
        color = PhysicsColorSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("position", color = PhysicsColorOnSurface, style = MaterialTheme.typography.labelSmall)
                Text(
                    "speed ${uiState.current?.speedMetersPerSec?.let { String.format(Locale.US, "%.3f", it) } ?: "—"}",
                    color = PhysicsColorOnBackground,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(top = 4.dp)
            ) {
                val pts = uiState.graphPoints
                if (pts.size < 2) return@Canvas
                val t0 = pts.first().elapsedUs
                val tSpan = (pts.last().elapsedUs - t0).toDouble().coerceAtLeast(1.0)
                val xs = pts.map { it.xMeters }
                val ys = pts.map { it.yMeters }
                val mn = (xs + ys).min()
                val mx = (xs + ys).max()
                val pad = maxOf((mx - mn) * 0.15, 1e-6)
                fun drawSeries(values: List<Double>, color: Color) {
                    if (values.size < 2) return
                    val path = Path()
                    values.forEachIndexed { i, v ->
                        val t = (pts[i].elapsedUs - t0) / tSpan
                        val x = (t * size.width).toFloat()
                        val y = size.height - ((v - mn + pad) / (mx - mn + 2 * pad) * size.height).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = color, style = Stroke(3f))
                }
                drawSeries(xs, PhysicsColorTrackA)
                drawSeries(ys, PhysicsColorPrimary.copy(alpha = 0.8f))
            }
        }
    }
}
