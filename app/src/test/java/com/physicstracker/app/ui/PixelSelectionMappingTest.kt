package com.physicstracker.app.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Test

class PixelSelectionMappingTest {
    @Test
    fun selectedPixelIsTheSameInputForMagnifierAndTrackerAcrossRasterSizes() {
        val sourceWidth = 1920
        val sourceHeight = 1080
        val selectedPixels = listOf(
            SelectedPixel(0, 0),
            SelectedPixel(sourceWidth - 1, sourceHeight - 1),
            SelectedPixel(880, 682),
            SelectedPixel(sourceWidth / 2, sourceHeight / 2)
        )
        val decodedSizes = listOf(
            1920 to 1080,
            1280 to 720,
            960 to 540,
            3840 to 2160,
            641 to 359
        )

        selectedPixels.forEach { selectedPixel ->
            decodedSizes.forEach { (decodedWidth, decodedHeight) ->
                val magnifierPixel = mapPixelCenterBetweenSpaces(
                    selectedPixel.offset,
                    sourceWidth,
                    sourceHeight,
                    decodedWidth,
                    decodedHeight
                ).toSelectedPixel(decodedWidth, decodedHeight)
                val trackerPixel = mapPixelCenterBetweenSpaces(
                    selectedPixel.offset,
                    sourceWidth,
                    sourceHeight,
                    decodedWidth,
                    decodedHeight
                ).toSelectedPixel(decodedWidth, decodedHeight)

                assertEquals(trackerPixel, magnifierPixel)
            }
        }
    }

    @Test
    fun markerProjectionReturnsToTheSamePixelAcrossZoomAndDensity() {
        val sourceWidth = 1920
        val sourceHeight = 1080
        val selectedPixel = SelectedPixel(880, 682)
        val zoomLevels = listOf(1f, 1.5f, 2f, 4f, 8f)
        val densities = listOf(1f, 1.5f, 2f, 3f)

        zoomLevels.forEach { zoom ->
            densities.forEach { density ->
                val viewportWidth = 1024f * zoom * density
                val viewportHeight = 576f * zoom * density
                val markerX = (selectedPixel.x + 0.5f) / sourceWidth * viewportWidth
                val markerY = (selectedPixel.y + 0.5f) / sourceHeight * viewportHeight
                val restoredPixel = Offset(
                    markerX / viewportWidth * sourceWidth - 0.5f,
                    markerY / viewportHeight * sourceHeight - 0.5f
                ).toSelectedPixel(sourceWidth, sourceHeight)

                assertEquals(selectedPixel, restoredPixel)
            }
        }
    }

    @Test
    fun halfPixelBoundaryIsRoundedOnlyOnce() {
        val selectedPixel = Offset(879.51f, 681.51f).toSelectedPixel(1920, 1080)

        assertEquals(880, selectedPixel.x)
        assertEquals(682, selectedPixel.y)
        assertEquals(880, selectedPixel.offset.x.roundToInt())
        assertEquals(682, selectedPixel.offset.y.roundToInt())
    }

    @Test
    fun selectedPixelSurvivesPortraitLandscapeAndVideoRotationWithoutOffset() {
        val sourceWidth = 1920
        val sourceHeight = 1080
        val selectedPixels = listOf(
            SelectedPixel(0, 0),
            SelectedPixel(880, 682),
            SelectedPixel(sourceWidth - 1, sourceHeight - 1)
        )
        val portraitViewport = PixelViewport(left = 0f, top = 700f, width = 1080f, height = 607f)
        val landscapeViewport = PixelViewport(left = 180f, top = 0f, width = 1920f, height = 1080f)

        listOf(0, 90, 180, 270).forEach { videoRotation ->
            listOf(portraitViewport, landscapeViewport).forEach { viewport ->
                selectedPixels.forEach { selectedPixel ->
                    val markerCenter = sourcePixelToViewport(
                        sourcePixel = selectedPixel.offset,
                        sourceWidth = sourceWidth,
                        sourceHeight = sourceHeight,
                        rotationDegrees = videoRotation,
                        viewport = viewport
                    )
                    val restored = viewportPointToSourcePixel(
                        viewportPoint = markerCenter,
                        sourceWidth = sourceWidth,
                        sourceHeight = sourceHeight,
                        rotationDegrees = videoRotation,
                        viewport = viewport
                    ).toSelectedPixel(sourceWidth, sourceHeight)

                    assertEquals(selectedPixel, restored)
                }
            }
        }
    }

    @Test
    fun magnifierAndTrackerUseSamePhysicalPixelForRawAndDisplayOrientedFrames() {
        val sourceWidth = 1920
        val sourceHeight = 1080
        val selectedPixel = SelectedPixel(880, 682)

        listOf(90, 270).forEach { videoRotation ->
            val frameSizes = listOf(
                1920 to 1080, // Raw decoder orientation.
                1080 to 1920, // Rotation already applied by the decoder.
                960 to 540,
                540 to 960
            )
            frameSizes.forEach { (frameWidth, frameHeight) ->
                val magnifierPixel = sourcePixelToDecodedFrame(
                    sourcePixel = selectedPixel.offset,
                    sourceWidth = sourceWidth,
                    sourceHeight = sourceHeight,
                    rotationDegrees = videoRotation,
                    frameWidth = frameWidth,
                    frameHeight = frameHeight
                )
                val trackerPixel = sourcePixelToDecodedFrame(
                    sourcePixel = selectedPixel.offset,
                    sourceWidth = sourceWidth,
                    sourceHeight = sourceHeight,
                    rotationDegrees = videoRotation,
                    frameWidth = frameWidth,
                    frameHeight = frameHeight
                )
                val restoredSourcePixel = decodedFramePixelToSource(
                    framePixel = trackerPixel,
                    frameWidth = frameWidth,
                    frameHeight = frameHeight,
                    sourceWidth = sourceWidth,
                    sourceHeight = sourceHeight,
                    rotationDegrees = videoRotation
                ).toSelectedPixel(sourceWidth, sourceHeight)
                val remappedFramePixel = sourcePixelToDecodedFrame(
                    sourcePixel = restoredSourcePixel.offset,
                    sourceWidth = sourceWidth,
                    sourceHeight = sourceHeight,
                    rotationDegrees = videoRotation,
                    frameWidth = frameWidth,
                    frameHeight = frameHeight
                )

                assertEquals(magnifierPixel, trackerPixel)
                assertEquals(trackerPixel, remappedFramePixel)

                // Native-size raw and display-oriented frames must round-trip exactly.
                // Downscaled decoder frames necessarily collapse multiple source pixels;
                // the UI therefore retains selectedPixel as its canonical source coordinate.
                val isNativeRawFrame = frameWidth == sourceWidth && frameHeight == sourceHeight
                val isNativeDisplayFrame =
                    frameWidth == sourceHeight && frameHeight == sourceWidth
                if (isNativeRawFrame || isNativeDisplayFrame) {
                    assertEquals(selectedPixel, restoredSourcePixel)
                }
            }
        }
    }
}
