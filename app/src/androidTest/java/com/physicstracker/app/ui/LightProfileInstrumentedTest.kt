package com.physicstracker.app.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LightProfileInstrumentedTest {
    private fun horizontalGradient(size: Int): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val value = x.coerceIn(0, 255)
                    bitmap.setPixel(x, y, Color.rgb(value, value, value))
                }
            }
        }

    @Test
    fun lineProfileTracksImageIntensityAndScreenDistance() {
        val bitmap = horizontalGradient(100)
        val profile = buildLightProfile(
            bitmap = bitmap,
            geometry = LightGeometry(LightTool.LINE, Offset(0f, 50f), Offset(99f, 50f)),
            channel = LightChannel.R,
            canvasSize = IntSize(100, 100),
            calibration = null,
            backgroundSubtract = false,
            smoothing = false
        )

        assertEquals(100, profile.size)
        assertEquals(0.0, profile.first().axisValue, 1e-9)
        assertEquals(99.0, profile.last().axisValue, 1e-9)
        assertTrue(profile.first().intensity < profile.last().intensity)
        assertEquals(50.0, profile[50].intensity, 1.0)
    }

    @Test
    fun boxProfileUsesBoxWidthForAxisAndAveragesItsDepth() {
        val bitmap = horizontalGradient(100)
        val profile = buildLightProfile(
            bitmap = bitmap,
            geometry = LightGeometry(LightTool.BOX, Offset(20f, 20f), Offset(80f, 80f)),
            channel = LightChannel.R,
            canvasSize = IntSize(100, 100),
            calibration = null,
            backgroundSubtract = false,
            smoothing = false
        )

        assertEquals(61, profile.size)
        assertEquals(60.0, profile.last().axisValue, 1e-9)
        assertEquals(20.0, profile.first().intensity, 1e-9)
        assertEquals(80.0, profile.last().intensity, 1e-9)
    }
}
