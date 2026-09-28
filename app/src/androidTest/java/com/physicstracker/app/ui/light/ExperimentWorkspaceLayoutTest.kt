package com.physicstracker.app.ui.light

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.physicstracker.app.ui.ExperimentWorkspaceFrame
import com.physicstracker.app.ui.MainBottomNavRow
import com.physicstracker.app.ui.WorkspaceAnalysisMode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExperimentWorkspaceLayoutTest {
    @get:Rule val compose = createComposeRule()

    private fun show(width: Int, height: Int) {
        compose.setContent {
            Box(Modifier.requiredSize(width.dp, height.dp).testTag("workspace-root")) {
                ExperimentWorkspaceFrame(
                    header = { Box(Modifier.height(48.dp)) { Text("Light") } },
                    media = { Box(Modifier.fillMaxSize()) },
                    tools = { Column { repeat(30) { Text("Tool $it", Modifier.height(48.dp)) } } },
                    navigation = { Box(Modifier.height(56.dp)) { Text("Main tabs") } }
                )
            }
        }
        compose.waitForIdle()
    }

    @Test fun landscapeKeepsFullHeightMediaOnLeftEvenWithManyTools() {
        show(800, 360)
        val media = compose.onNodeWithTag("experiment-media").fetchSemanticsNode().size
        val tools = compose.onNodeWithTag("experiment-tools").fetchSemanticsNode().size
        assertEquals(tools.height, media.height)
        assertEquals(tools.width * (0.68 / 0.32), media.width.toDouble(), 3.0)
    }

    @Test fun portraitToolsCannotConsumeTheMediaStage() {
        show(360, 640)
        val root = compose.onNodeWithTag("workspace-root").fetchSemanticsNode().size
        val media = compose.onNodeWithTag("experiment-media").fetchSemanticsNode().size
        val tools = compose.onNodeWithTag("experiment-tools").fetchSemanticsNode().size
        assertEquals(tools.width, media.width)
        assertTrue(media.height > root.height * 0.4f)
        assertTrue(tools.height <= root.height * 0.33f)
    }

    @Test fun landscapeResizeButtonExpandsAndRestoresToolsWithoutHidingThem() {
        show(800, 360)
        val initial = compose.onNodeWithTag("experiment-tools").fetchSemanticsNode().size.width
        compose.onNodeWithTag("experiment-resize-toggle").performClick()
        compose.waitForIdle()
        val expanded = compose.onNodeWithTag("experiment-tools").fetchSemanticsNode().size.width
        assertTrue(expanded > initial)
        compose.onNodeWithTag("experiment-resize-toggle").performClick()
        compose.waitForIdle()
        assertEquals(initial, compose.onNodeWithTag("experiment-tools").fetchSemanticsNode().size.width)
    }

    private fun showNav(mode: WorkspaceAnalysisMode) {
        compose.setContent {
            MainBottomNavRow(
                activeBottomTab = null, workspaceMode = mode, dataShortcutSelected = false,
                playWhenReady = false, onToggleCal = {}, onToggleTrack = {}, onOpenData = {},
                onTogglePlayPause = {}, onOpenView = {}, onToggleLight = {}, onToggleMore = {}
            )
        }
    }

    @Test fun motionNavigationHidesLightControls() {
        showNav(WorkspaceAnalysisMode.MOTION)
        compose.onNodeWithText("Calibrate").assertExists()
        compose.onNodeWithText("Tracking").assertExists()
        compose.onNodeWithText("Light").assertDoesNotExist()
    }

    @Test fun lightNavigationHidesMotionControls() {
        showNav(WorkspaceAnalysisMode.LIGHT)
        compose.onNodeWithText("Light").assertExists()
        compose.onNodeWithText("Calibrate").assertDoesNotExist()
        compose.onNodeWithText("Tracking").assertDoesNotExist()
    }
}
