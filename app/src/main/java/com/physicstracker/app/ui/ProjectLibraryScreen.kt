package com.physicstracker.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.physicstracker.app.data.ProjectEntity
import androidx.activity.compose.BackHandler
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
    liveSessions: List<com.physicstracker.app.live.data.LiveSessionEntity> = emptyList(),
    appVersion: String,
    onNewExperiment: () -> Unit,
    onOpenProject: (ProjectEntity) -> Unit,
    onProjectActions: (ProjectEntity) -> Unit,
    onOpenLearning: (String) -> Unit,
    onOpenLive: (() -> Unit)? = null,
    onOpenLiveSession: ((com.physicstracker.app.live.data.LiveSessionEntity) -> Unit)? = null,
    onOpenLight: (() -> Unit)? = null,
    lightProjects: List<com.physicstracker.app.ui.light.LightProjectSummary> = emptyList(),
    lightHistoryError: String? = null,
    onOpenLightProject: (String) -> Unit = {},
    onOpenLightDemo: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val homePreferences = remember(context) { context.getSharedPreferences("physics_tracker_home", 0) }
    var homeTab by rememberSaveable { mutableStateOf(0) }
    var historyTab by rememberSaveable { mutableStateOf("ALL") }
    var projectPickerKind by rememberSaveable { mutableStateOf<String?>(null) }
    var previousHistoryTab by rememberSaveable { mutableStateOf("ALL") }
    fun openSaved(kind: String) {
        previousHistoryTab = historyTab
        historyTab = kind
        projectPickerKind = kind
        homeTab = 1
    }
    fun backToStart() {
        projectPickerKind = null
        historyTab = previousHistoryTab
        homeTab = 0
    }
    fun selectTab(index: Int) {
        if (index == 1 && projectPickerKind != null) historyTab = "ALL"
        projectPickerKind = null
        homeTab = index.coerceIn(0, 2)
    }
    BackHandler(projectPickerKind != null) { backToStart() }
    var showLearningLinks by rememberSaveable {
        mutableStateOf(homePreferences.getBoolean("show_learning_links", false))
    }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().background(PhysicsColorBackground)) {
        val wide = maxWidth >= 760.dp
        Column(Modifier.fillMaxSize()) {
            LibraryHeader(appVersion)
            androidx.compose.material3.TabRow(selectedTabIndex = homeTab, containerColor = PhysicsColorBackground) {
                listOf("Start", "Projects", "Learn").forEachIndexed { index, label ->
                    androidx.compose.material3.Tab(selected = homeTab == index, onClick = { selectTab(index) },
                        text = { Text(label) })
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = if (wide) 24.dp else 16.dp, vertical = 12.dp)
                .testTag("home-page-content")
                .pointerInput(homeTab, projectPickerKind) {
                    var travel = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { travel = 0f },
                        onHorizontalDrag = { change, amount -> travel += amount; change.consume() },
                        onDragEnd = {
                            if (kotlin.math.abs(travel) >= 72.dp.toPx()) {
                                val next = (homeTab + if (travel < 0f) 1 else -1).coerceIn(0, 2)
                                if (next != homeTab) selectTab(next)
                            }
                            travel = 0f
                        },
                        onDragCancel = { travel = 0f })
                }) {
                when (homeTab) {
                    0 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (wide) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.weight(1f)) { DestinationCard("Motion", "${projects.count { it.videoUri.isNotEmpty() }} saved projects", "Open video", onNewExperiment,
                                    onCardClick = { openSaved("EXPERIMENTS") }) }
                                Box(Modifier.weight(1f)) { DestinationCard("Light intensity", "${lightProjects.size} saved projects", "Open photo or video", { onOpenLight?.invoke() },
                                    onCardClick = { openSaved("LIGHT") }) }
                            }
                        } else {
                            DestinationCard("Motion", "${projects.count { it.videoUri.isNotEmpty() }} saved projects", "Open video", onNewExperiment,
                                onCardClick = { openSaved("EXPERIMENTS") })
                            DestinationCard("Light intensity", "${lightProjects.size} saved projects", "Open photo or video", { onOpenLight?.invoke() },
                                onCardClick = { openSaved("LIGHT") })
                        }
                        if (onOpenLive != null) OutlinedButton(onClick = onOpenLive, modifier = Modifier.heightIn(min = 48.dp)) { Text("Live camera") }
                    }
                    1 -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val shown = when (historyTab) {
                            "EXPERIMENTS" -> projects.filter { it.videoUri.isNotEmpty() }
                            "LIVE" -> projects.filter { it.videoUri.isEmpty() }
                            "LIGHT" -> emptyList()
                            else -> projects
                        }
                        val shownLight = if (historyTab in listOf("ALL", "LIGHT")) lightProjects else emptyList()
                        if (projectPickerKind != null) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = ::backToStart) { Text("‹ Back to Start") }
                                Text(if (projectPickerKind == "LIGHT") "Saved Light projects" else "Saved Motion projects",
                                    style = MaterialTheme.typography.titleMedium)
                            }
                        } else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            HistoryTabChip("All", historyTab == "ALL") { historyTab = "ALL" }
                            HistoryTabChip("Motion", historyTab == "EXPERIMENTS") { historyTab = "EXPERIMENTS" }
                            HistoryTabChip("Light", historyTab == "LIGHT") { historyTab = "LIGHT" }
                            HistoryTabChip("Live", historyTab == "LIVE") { historyTab = "LIVE" }
                        }
                        lightHistoryError?.let { Text(it, color = PhysicsColorError) }
                        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)) {
                            if (shown.isEmpty() && shownLight.isEmpty()) item {
                                Column(Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("No saved projects here yet.", color = PhysicsColorOnSurface)
                                    OutlinedButton(onClick = {
                                        when (projectPickerKind) {
                                            "LIGHT" -> onOpenLight?.invoke()
                                            "EXPERIMENTS" -> onNewExperiment()
                                            else -> selectTab(0)
                                        }
                                        Unit
                                    }) {
                                        Text(if (projectPickerKind == "LIGHT") "Open photo or video"
                                            else if (projectPickerKind == "EXPERIMENTS") "Open video" else "Start a project")
                                    }
                                }
                            }
                            items(shown, key = { "motion-${it.id}" }) { project -> ProjectRow(project, onOpenProject, onProjectActions) }
                            items(shownLight, key = { "light-${it.id}" }) { project -> LightProjectRow(project) { onOpenLightProject(project.id) } }
                        }
                    }
                    else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { DestinationCard("Try a demo", "Explore motion and light analysis with sample media.", "Browse demos", { onOpenLightDemo?.invoke() }) }
                        item {
                            OutlinedButton(onClick = {
                                showLearningLinks = !showLearningLinks
                                homePreferences.edit().putBoolean("show_learning_links", showLearningLinks).apply()
                            }) { Text(if (showLearningLinks) "Hide learning links" else "More learning resources") }
                        }
                        if (showLearningLinks) items(learningResources) { LearningRow(it, onOpenLearning) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    appVersion: String
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
                PhysicsTrackerLogo(Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "PhysicsTracker",
                color = PhysicsColorOnBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Motion & light analysis",
                color = PhysicsColorOnSurface,
                style = MaterialTheme.typography.bodySmall
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
    onOpenLive: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NewExperimentCard(onNewExperiment)
        if (onOpenLive != null) {
            LiveTrackingCard(onOpenLive)
        }
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
            modifier = Modifier.fillMaxWidth().padding(20.dp),
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
private fun LiveTrackingCard(onOpenLive: () -> Unit) {
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
                    "Live tracking",
                    color = PhysicsColorOnBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Track an object with the camera in real time and analyse it live. Fully offline.",
                    color = PhysicsColorOnSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = onOpenLive,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhysicsColorTrackA,
                    contentColor = PhysicsColorCanvas
                )
            ) {
                Text("Open live", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HistoryTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        border = BorderStroke(1.dp, if (selected) PhysicsColorPrimary else PhysicsColorDivider),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) PhysicsColorPrimary.copy(alpha = 0.18f) else Color.Transparent,
            contentColor = if (selected) PhysicsColorPrimary else PhysicsColorOnSurface
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DestinationCard(
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit,
    accent: Boolean = false,
    onCardClick: (() -> Unit)? = null
) {
    Surface(
        color = PhysicsColorSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider),
        modifier = Modifier.fillMaxWidth().then(if (onCardClick != null)
            Modifier.testTag("open-saved-$title").clickable(onClick = onCardClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = PhysicsColorOnBackground, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    if (onCardClick != null) Text("Saved ›", color = PhysicsColorPrimary,
                        style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    subtitle,
                    color = PhysicsColorOnSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (accent) PhysicsColorTrackA else PhysicsColorPrimary,
                    contentColor = PhysicsColorCanvas
                )
            ) {
                Text(buttonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LightProjectRow(project: com.physicstracker.app.ui.light.LightProjectSummary, onOpen: () -> Unit) {
    Surface(color = PhysicsColorSurface, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PhysicsColorDivider),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(42.dp), shape = CircleShape, color = PhysicsColorPrimary.copy(alpha = 0.15f)) {
                Box(contentAlignment = Alignment.Center) { Text(project.name.take(1).uppercase(), color = PhysicsColorPrimary) }
            }
            Column(Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text("${if (project.video) "Video" else "Photo"} · ${project.samples} samples · ${formatProjectTimestamp(project.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall, color = PhysicsColorOnSurface, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Text("›", color = PhysicsColorPrimary, style = MaterialTheme.typography.titleMedium)
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
                    if (project.videoUri.isEmpty()) "Live · ${formatProjectTimestamp(project.updatedAt)}"
                    else "Modified ${formatProjectTimestamp(project.updatedAt)}",
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
