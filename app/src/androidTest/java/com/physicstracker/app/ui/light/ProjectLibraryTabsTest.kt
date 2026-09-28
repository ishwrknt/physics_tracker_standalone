package com.physicstracker.app.ui.light

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.physicstracker.app.ui.ProjectLibraryScreen
import com.physicstracker.app.data.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectLibraryTabsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeShowsOnlyRelevantActionsAndKeepsCallbacks() {
        var motion = 0
        var light = 0
        var demos = 0
        compose.setContent {
            ProjectLibraryScreen(projects = emptyList(), appVersion = "test",
                onNewExperiment = { motion++ }, onOpenProject = {}, onProjectActions = {}, onOpenLearning = {},
                onOpenLight = { light++ }, onOpenLightDemo = { demos++ })
        }
        compose.onNodeWithText("Browse demos").assertDoesNotExist()
        compose.onNodeWithText("Open video").performScrollTo().performClick()
        compose.onNodeWithText("Open photo or video").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, motion); assertEquals(1, light) }
        compose.onNodeWithText("Projects").performClick()
        compose.onNodeWithText("Open video").assertDoesNotExist()
        compose.onNodeWithText("No saved projects here yet.").assertExists()
        compose.onNodeWithText("Learn", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Browse demos").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, demos) }
        compose.onNodeWithText("Start").performClick()
        compose.onNodeWithText("Browse demos").assertDoesNotExist()
        compose.onNodeWithText("Open video").assertExists()
    }

    @Test fun cardsOpenFocusedSavedProjectsAndBackReturnsToStart() {
        var openedMotion = 0
        var openedLight = 0
        val motion = ProjectEntity(id = 1, name = "Pendulum", videoUri = "content://motion", videoWidth = 320, videoHeight = 240)
        val light = LightProjectSummary("light-id", "Lamp", true, 123L, 12, "content://light")
        compose.setContent {
            ProjectLibraryScreen(projects = listOf(motion), lightProjects = listOf(light), appVersion = "test",
                onNewExperiment = {}, onOpenProject = { openedMotion++ }, onProjectActions = {}, onOpenLearning = {},
                onOpenLightProject = { openedLight++ })
        }
        compose.onNodeWithTag("open-saved-Motion").performClick()
        compose.onNodeWithText("Saved Motion projects").assertExists()
        compose.onNodeWithText("Pendulum").performClick()
        compose.runOnIdle { assertEquals(1, openedMotion) }
        compose.onNodeWithText("‹ Back to Start").performClick()
        compose.onNodeWithTag("open-saved-Light intensity").performClick()
        compose.onNodeWithText("Saved Light projects").assertExists()
        compose.onNodeWithText("Lamp").performClick()
        compose.runOnIdle { assertEquals(1, openedLight) }
        compose.onNodeWithText("‹ Back to Start").performClick()
        compose.onNodeWithTag("home-page-content").performTouchInput { swipeLeft() }
        compose.onNodeWithText("All").assertExists()
    }
}
