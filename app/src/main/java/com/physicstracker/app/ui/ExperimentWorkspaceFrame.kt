package com.physicstracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// The established experiment workspace proportions; source coordinates never depend on these.
internal const val EXPERIMENT_MEDIA_FRACTION = 0.68f
internal const val EXPERIMENT_TOOLS_FRACTION = 0.32f

/** Same full-height left media / right chrome arrangement as the motion experiment workspace. */
@Composable
internal fun ExperimentWorkspaceFrame(
    header: @Composable () -> Unit,
    media: @Composable () -> Unit,
    tools: @Composable () -> Unit,
    navigation: @Composable () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().background(PhysicsColorBackground)) {
        val landscape = maxWidth > maxHeight
        val availableHeight = maxHeight
        var sidebar by rememberSaveable { mutableFloatStateOf(EXPERIMENT_TOOLS_FRACTION) }
        if (landscape) {
            val frameWidthPx = constraints.maxWidth
            Box(Modifier.fillMaxHeight().fillMaxWidth(1f - sidebar)
                .clipToBounds().testTag("experiment-media")) { media() }
            Column(Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(sidebar)
                .background(PhysicsColorBackground).testTag("experiment-tools")) {
                header()
                Column(Modifier.fillMaxWidth().weight(1f)
                    .verticalScroll(rememberScrollState())) { tools() }
                navigation()
            }
            Surface(modifier = Modifier.align(Alignment.TopEnd).padding(top = 68.dp)
                .offset(x = -(maxWidth * sidebar)).width(48.dp),
                color = PhysicsColorSurface, contentColor = PhysicsColorAccent) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp)
                        .testTag("experiment-resize-toggle")
                        .semantics { contentDescription = if (sidebar > 0.36f) "Show more video" else "Expand tools workspace" }
                        .clickable { sidebar = if (sidebar > 0.36f) EXPERIMENT_TOOLS_FRACTION else 0.52f },
                        contentAlignment = Alignment.Center) { Text(if (sidebar > 0.36f) "›" else "‹") }
                    Box(Modifier.size(48.dp, 40.dp)
                        .semantics { contentDescription = "Drag to resize tools workspace" }
                        .pointerInput(frameWidthPx) {
                            detectDragGestures { change, delta ->
                                change.consume()
                                sidebar = (sidebar - delta.x / frameWidthPx.coerceAtLeast(1))
                                    .coerceIn(EXPERIMENT_TOOLS_FRACTION, 0.56f)
                            }
                        }, contentAlignment = Alignment.Center) { Text("⋮") }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                header()
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().testTag("experiment-media")) { media() }
                Column(Modifier.fillMaxWidth().heightIn(max = availableHeight * 0.32f)
                    .verticalScroll(rememberScrollState()).testTag("experiment-tools")) { tools() }
                navigation()
            }
        }
    }
}
