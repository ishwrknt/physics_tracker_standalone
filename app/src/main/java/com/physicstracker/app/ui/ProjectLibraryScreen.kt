package com.physicstracker.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.physicstracker.app.data.ProjectEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LearningResource(
    val title: String,
    val author: String,
    val url: String
)

private val learningResources = listOf(
    LearningResource(
        "Nonlinear Pendulum Simulation",
        "Prof. OSKS Sastri",
        "https://sastriosks.github.io/2D-Nonlinear-Simple-Pendulum-Simulation/"
    ),
    LearningResource(
        "Projectile Motion",
        "Ayushi Awasthi",
        "https://projectile-motion-9hvyj5sn93tifpiu4ezlbf.streamlit.app/"
    ),
    LearningResource(
        "Schrödinger Solver",
        "Interactive reference",
        "https://schrodinger-solver-web.streamlit.app/"
    )
)

@Composable
fun ProjectLibraryScreen(
    projects: List<ProjectEntity>,
    appVersion: String,
    onNewExperiment: () -> Unit,
    onOpenProject: (ProjectEntity) -> Unit,
    onProjectActions: (ProjectEntity) -> Unit,
    onOpenLearning: (String) -> Unit
) {
    val context = LocalContext.current
    val homePreferences = remember(context) {
        context.getSharedPreferences("physics_tracker_home", 0)
    }
    var showLearningLinks by rememberSaveable {
        mutableStateOf(homePreferences.getBoolean("show_learning_links", true))
    }
    val toggleLearningLinks = {
        showLearningLinks = !showLearningLinks
        homePreferences.edit().putBoolean("show_learning_links", showLearningLinks).apply()
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PhysicsColorBackground)
    ) {
        val wide = maxWidth >= 760.dp
        Column(modifier = Modifier.fillMaxSize()) {
            LibraryHeader(
                appVersion = appVersion,
                showLearningLinks = showLearningLinks,
                onToggleLearningLinks = toggleLearningLinks
            )
            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    LibraryPrimaryColumn(
                        projects = projects,
                        onNewExperiment = onNewExperiment,
                        onOpenProject = onOpenProject,
                        onProjectActions = onProjectActions,
                        modifier = Modifier.weight(1.7f)
                    )
                    if (showLearningLinks) {
                        LearningColumn(
                            onOpenLearning = onOpenLearning,
                            modifier = Modifier
                                .widthIn(min = 280.dp, max = 360.dp)
                                .fillMaxHeight()
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { NewExperimentCard(onNewExperiment) }
                    item { SectionHeading("Recent experiments", "${projects.size}") }
                    if (projects.isEmpty()) {
                        item { EmptyLibraryState(onNewExperiment) }
                    } else {
                        items(projects, key = { it.id }) { project ->
                            ProjectRow(project, onOpenProject, onProjectActions)
                        }
                    }
                    if (showLearningLinks) {
                        item { SectionHeading("Learn", "Optional") }
                        items(learningResources) { resource ->
                            LearningRow(resource, onOpenLearning)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    appVersion: String,
    showLearningLinks: Boolean,
    onToggleLearningLinks: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            color = PhysicsColorPrimary.copy(alpha = 0.14f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, PhysicsColorPrimary.copy(alpha = 0.35f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "P",
                    color = PhysicsColorPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "PhyZX",
                color = PhysicsColorOnBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Motion measurement workspace",
                color = PhysicsColorOnSurface,
                style = MaterialTheme.typography.bodySmall
            )
        }
        OutlinedButton(
            onClick = onToggleLearningLinks,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.padding(end = 10.dp)
        ) {
            Text(
                if (showLearningLinks) "HIDE LINKS" else "SHOW LINKS",
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            "v$appVersion",
            color = PhysicsColorOnSurface,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun LibraryPrimaryColumn(
    projects: List<ProjectEntity>,
    onNewExperiment: () -> Unit,
    onOpenProject: (ProjectEntity) -> Unit,
    onProjectActions: (ProjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NewExperimentCard(onNewExperiment)
        SectionHeading("Recent experiments", "${projects.size}")
        if (projects.isEmpty()) {
            EmptyLibraryState(onNewExperiment)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectRow(project, onOpenProject, onProjectActions)
                }
            }
        }
    }
}

@Composable
private fun NewExperimentCard(onNewExperiment: () -> Unit) {
    Surface(
        color = PhysicsColorSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "Start a measurement",
                    color = PhysicsColorOnBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Choose a video, calibrate the scene, then track one or more objects.",
                    color = PhysicsColorOnSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = onNewExperiment,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhysicsColorPrimary,
                    contentColor = PhysicsColorCanvas
                )
            ) {
                Text("New experiment", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(onNewExperiment: () -> Unit) {
    Surface(
        color = PhysicsColorSurface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "No experiments yet",
                color = PhysicsColorOnBackground,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "1. Choose a video   2. Set scale and axes   3. Track and analyze",
                color = PhysicsColorOnSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(onClick = onNewExperiment) {
                Text("Choose video")
            }
        }
    }
}

@Composable
private fun ProjectRow(
    project: ProjectEntity,
    onOpenProject: (ProjectEntity) -> Unit,
    onProjectActions: (ProjectEntity) -> Unit
) {
    Surface(
        color = PhysicsColorSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenProject(project) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = PhysicsColorTrackA.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        project.name.take(1).uppercase(),
                        color = PhysicsColorTrackA,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    color = PhysicsColorOnBackground,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Modified ${formatProjectTimestamp(project.updatedAt)}",
                    color = PhysicsColorOnSurface,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
            IconButton(
                onClick = { onProjectActions(project) },
                modifier = Modifier.size(48.dp)
            ) {
                Text("⋮", color = PhysicsColorOnSurface, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun LearningColumn(
    onOpenLearning: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading("Learn", "Optional")
        learningResources.forEach { LearningRow(it, onOpenLearning) }
        Spacer(Modifier.weight(1f))
        Text(
            "Learning resources open in your browser. Tracking and analysis remain fully offline.",
            color = PhysicsColorOnSurface,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun LearningRow(resource: LearningResource, onOpenLearning: (String) -> Unit) {
    Surface(
        color = PhysicsColorSurface.copy(alpha = 0.76f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenLearning(resource.url) }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                resource.title,
                color = PhysicsColorOnBackground,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                resource.author,
                color = PhysicsColorOnSurface,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = PhysicsColorOnBackground,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            detail,
            color = PhysicsColorOnSurface,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatProjectTimestamp(timestampMs: Long): String =
    SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(timestampMs))
