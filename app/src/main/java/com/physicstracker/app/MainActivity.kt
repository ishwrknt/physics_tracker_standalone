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
import com.physicstracker.app.ui.PhysicsColorError
import com.physicstracker.app.ui.PhysicsTrackerWorkspaceTheme
import com.physicstracker.app.ui.ProjectLibraryScreen
import com.physicstracker.app.ui.TrackingScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation =
            if (getSharedPreferences("physics_tracker_settings", MODE_PRIVATE)
                    .getBoolean("force_landscape", true)
            ) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
            var selectedVideoUri by rememberSaveable { mutableStateOf<String?>(null) }
            var activeProjectName by rememberSaveable { mutableStateOf<String?>(null) }
            var projectNameDraft by rememberSaveable { mutableStateOf("") }
            var showNewProjectDialog by rememberSaveable { mutableStateOf(false) }
            var activeProject by rememberSaveable { mutableStateOf(false) }
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

            PhysicsTrackerWorkspaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (activeProject) {
                        TrackingScreen(
                            videoUri = selectedVideoUri.orEmpty(),
                            projectName = activeProjectName.orEmpty(),
                            onBack = {
                                activeProject = false
                                activeProjectName = null
                                selectedVideoUri = null
                                projectNameDraft = ""
                            },
                            viewModel = viewModel
                        )
                    } else {
                        ProjectLibraryScreen(
                            projects = projects,
                            appVersion = appVersion,
                            onNewExperiment = {
                                pickVideo.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            onOpenProject = { project ->
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
                            }
                        )
                    }
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
