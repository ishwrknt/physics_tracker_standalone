package com.physicstracker.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.physicstracker.app.data.ProjectEntity
import com.physicstracker.app.live.ui.LiveTrackingScreen
import com.physicstracker.app.ui.PhysicsColorError
import com.physicstracker.app.ui.PhysicsTrackerWorkspaceTheme
import com.physicstracker.app.ui.ProjectLibraryScreen
import com.physicstracker.app.ui.TrackingScreen
import com.physicstracker.app.ui.light.LightAnalysisScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
@androidx.media3.common.util.UnstableApi
@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val orientationPreferences = getSharedPreferences("physics_tracker_settings", MODE_PRIVATE)
        requestedOrientation = when (orientationPreferences.getString("screen_orientation_mode", null)) {
            "PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "SYSTEM" -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            "LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> if (orientationPreferences.getBoolean("force_landscape", true)) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val viewModel: com.physicstracker.app.ui.VideoPlayerViewModel =
                androidx.hilt.navigation.compose.hiltViewModel()
            val projects by viewModel.projects.collectAsState()
            val liveSessions by viewModel.liveSessions.collectAsState()
            val lightModel: com.physicstracker.app.ui.light.LightAnalysisViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val lightProjects by lightModel.history.collectAsState()
            val lightHistoryError by lightModel.historyError.collectAsState()
            var selectedVideoUri by rememberSaveable { mutableStateOf<String?>(null) }
            var activeProjectName by rememberSaveable { mutableStateOf<String?>(null) }
            var projectNameDraft by rememberSaveable { mutableStateOf("") }
            var showNewProjectDialog by rememberSaveable { mutableStateOf(false) }
            var activeProject by rememberSaveable { mutableStateOf(false) }
            var liveMode by rememberSaveable { mutableStateOf(false) }
            var lightMode by rememberSaveable { mutableStateOf(false) }
            var openCalibration by rememberSaveable { mutableStateOf(false) }
            var showDemos by rememberSaveable { mutableStateOf(false) }
            var demoBusy by remember { mutableStateOf(false) }
            var demoError by remember { mutableStateOf<String?>(null) }
            val demoScope = androidx.compose.runtime.rememberCoroutineScope()
            val workspaceUiState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
            var projectForActions by remember { mutableStateOf<ProjectEntity?>(null) }
            var renameDraft by rememberSaveable { mutableStateOf("") }
            var confirmDelete by rememberSaveable { mutableStateOf(false) }
            val appVersion = remember {
                runCatching {
                    packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
                }.getOrDefault("unknown")
            }

            val pickVideo = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    selectedVideoUri = uri.toString()
                    projectNameDraft = ""
                    showNewProjectDialog = true
                }
            }

            val pickLightMedia = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    val type = contentResolver.getType(uri).orEmpty()
                    val extension = uri.toString().substringBefore('?').substringAfterLast('.', "").lowercase()
                    val video = type.startsWith("video/") || (!type.startsWith("image/") &&
                        extension in setOf("mp4", "mov", "m4v", "mkv", "webm", "avi"))
                    lightModel.open(uri, video)
                    lightMode = true
                }
            }

            PhysicsTrackerWorkspaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (lightMode) {
                        workspaceUiState.SaveableStateProvider("light-workspace") {
                        LightAnalysisScreen(onBack = { lightMode = false },
                            model = lightModel,
                            initialVideoUri = selectedVideoUri.takeIf { activeProject },
                            returnToTracking = activeProject,
                            onOpenMotion = { uri, name, calibration ->
                                selectedVideoUri = uri; activeProjectName = name
                                openCalibration = calibration; activeProject = true; lightMode = false
                            },
                            onOpenHistory = {
                                lightMode = false; activeProject = false; activeProjectName = null
                                selectedVideoUri = null; openCalibration = false; lightModel.refreshHistory()
                            })
                        }
                    } else if (liveMode) {
                        LiveTrackingScreen(
                            onBack = { liveMode = false }
                        )
                    } else if (activeProject) {
                        workspaceUiState.SaveableStateProvider("tracking-${selectedVideoUri.orEmpty()}") {
                        TrackingScreen(
                            videoUri = selectedVideoUri.orEmpty(),
                            projectName = activeProjectName.orEmpty(),
                            onBack = {
                                activeProject = false
                                activeProjectName = null
                                selectedVideoUri = null
                                projectNameDraft = ""
                            },
                            viewModel = viewModel,
                            onOpenLight = { openCalibration = false; lightMode = true },
                            startInCalibration = openCalibration
                        )
                        }
                    } else {
                        workspaceUiState.SaveableStateProvider("project-library") {
                            ProjectLibraryScreen(
                                projects = projects,
                                liveSessions = liveSessions,
                                appVersion = appVersion,
                                onNewExperiment = {
                                    pickVideo.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                onOpenProject = { project ->
                                    openCalibration = false
                                    viewModel.loadProject(project)
                                    activeProjectName = project.name
                                    selectedVideoUri = project.videoUri
                                    activeProject = true
                                },
                                onProjectActions = { project ->
                                    projectForActions = project
                                    renameDraft = project.name
                                },
                                onOpenLearning = { url ->
                                    runCatching {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                onOpenLive = { liveMode = true },
                                onOpenLight = { pickLightMedia.launch(arrayOf("image/*", "video/*")) },
                                lightProjects = lightProjects,
                                lightHistoryError = lightHistoryError,
                                onOpenLightProject = { id -> lightModel.reopen(id); lightMode = true },
                                onOpenLightDemo = { demoError = null; showDemos = true }
                            )
                        }
                    }
                }

                if (showDemos) {
                    com.physicstracker.app.ui.DemoPicker(demoBusy, demoError,
                        onClose = { showDemos = false }, onChoose = { demo ->
                            demoBusy = true
                            demoError = null
                            demoScope.launch {
                                try {
                                    val uri = com.physicstracker.app.ui.prepareDemo(this@MainActivity, demo)
                                    openCalibration = false
                                    if (demo.light) {
                                        activeProject = false
                                        selectedVideoUri = null
                                        lightModel.open(uri, demo.video, demo = demo.guided, displayName = demo.projectName)
                                        lightMode = true
                                    } else {
                                        selectedVideoUri = uri.toString()
                                        activeProjectName = demo.projectName
                                        activeProject = true
                                        lightMode = false
                                    }
                                    showDemos = false
                                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                catch (error: Exception) { demoError = "Could not open demo: ${error.message}" }
                                finally { demoBusy = false }
                            }
                        })
                }

                if (showNewProjectDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showNewProjectDialog = false
                            selectedVideoUri = null
                        },
                        title = { Text("Name experiment") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "Use a short name that identifies the apparatus and trial.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextField(
                                    value = projectNameDraft,
                                    onValueChange = { projectNameDraft = it },
                                    singleLine = true,
                                    placeholder = { Text("Pendulum · Trial 01") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(min = 320.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val name = projectNameDraft.trim()
                                    if (name.isNotEmpty() && selectedVideoUri != null) {
                                        activeProjectName = name
                                        showNewProjectDialog = false
                                        activeProject = true
                                    }
                                },
                                enabled = projectNameDraft.isNotBlank()
                            ) {
                                Text("Open workspace")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showNewProjectDialog = false
                                selectedVideoUri = null
                            }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                projectForActions?.let { project ->
                    AlertDialog(
                        onDismissRequest = { projectForActions = null },
                        title = { Text("Experiment details") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TextField(
                                    value = renameDraft,
                                    onValueChange = { renameDraft = it },
                                    singleLine = true,
                                    label = { Text("Experiment name") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    "Renaming keeps the video, calibration, observations, and analysis data.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.renameProject(project.id, renameDraft)
                                    projectForActions = null
                                },
                                enabled = renameDraft.isNotBlank()
                            ) {
                                Text("Save name")
                            }
                        },
                        dismissButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { confirmDelete = true }) {
                                    Text("Delete", color = PhysicsColorError)
                                }
                                TextButton(onClick = { projectForActions = null }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    )
                }

                if (confirmDelete) {
                    val project = projectForActions
                    AlertDialog(
                        onDismissRequest = { confirmDelete = false },
                        title = { Text("Delete experiment?") },
                        text = {
                            Text(
                                if (project == null) "No experiment selected."
                                else "“${project.name}” and all of its observations will be permanently deleted."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    project?.let(viewModel::deleteProject)
                                    confirmDelete = false
                                    projectForActions = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PhysicsColorError)
                            ) {
                                Text("Delete permanently", color = Color.Black)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmDelete = false }) {
                                Text("Keep experiment")
                            }
                        }
                    )
                }
            }
        }
    }
}
