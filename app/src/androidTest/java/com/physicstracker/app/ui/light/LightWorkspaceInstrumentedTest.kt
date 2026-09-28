package com.physicstracker.app.ui.light

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.geometry.Offset
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import com.physicstracker.app.ui.LightChannel
import com.physicstracker.app.ui.LightGeometry
import com.physicstracker.app.ui.LightTool
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import org.junit.After
import java.util.UUID

class LightWorkspaceInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testStorage by lazy { File(context.cacheDir, "light-test-history-${UUID.randomUUID()}") }
    private val models = mutableListOf<LightAnalysisViewModel>()
    private fun model() = LightAnalysisViewModel(context.applicationContext as Application, testStorage).also { models += it }
    @After fun cleanupHistory() { models.forEach { it.viewModelScope.cancel() }; testStorage.deleteRecursively() }
    private fun photo(): File {
        val file = File.createTempFile("light-test-", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(8, 6, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)
        bitmap.setPixel(4, 3, Color.RED)
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
        return file
    }
    private fun await(model: LightAnalysisViewModel) = runBlocking {
        withTimeout(10_000) { while (model.state.value.busy) delay(10) }
    }

    @Test fun everyBundledDemoDecodesInTheRealLightPixelPipeline() = runBlocking {
        for (demo in com.physicstracker.app.ui.DemoCatalog.items) {
            val uri = com.physicstracker.app.ui.prepareDemo(context, demo)
            if (demo.video) {
                var frames = 0
                decodeLightVideo(context, uri, {}) { pixels, pts, _ ->
                    assertTrue("${demo.file}: dimensions", pixels.width > 0 && pixels.height > 0)
                    assertTrue("${demo.file}: PTS", pts >= 0)
                    frames++
                    false
                }
                assertEquals(demo.file, 1, frames)
            } else {
                val bitmap = decodeLightPhoto(context, uri)
                assertTrue(bitmap.width > 0)
                bitmap.recycle()
            }
        }
    }

    @Test fun convertedDemoKeepsProvenanceThroughTimeMeasurementAndReopen() = runBlocking {
        val demo = com.physicstracker.app.ui.DemoCatalog.items.first { it.light && it.converted }
        val uri = com.physicstracker.app.ui.prepareDemo(context, demo)
        val first = model()
        first.open(uri, true, displayName = demo.projectName)
        await(first)
        first.analyze(true)
        await(first)
        assertTrue(first.state.value.status, first.state.value.datasets["A"]!!.observations.isNotEmpty())
        assertTrue(first.state.value.provenance.contains("compression changes pixel values"))
        first.saveProjectNow()
        withTimeout(10_000) {
            while (runCatching { LightProjectStore(testStorage).load(first.state.value.projectId!!).datasets }
                    .getOrNull() != first.state.value.datasets) delay(20)
        }
        val second = model()
        second.reopen(first.state.value.projectId!!)
        withTimeout(10_000) { while (second.state.value.preview == null || second.state.value.busy) delay(20) }
        assertEquals(first.state.value.datasets, second.state.value.datasets)
        assertEquals(first.state.value.provenance, second.state.value.provenance)
    }

    @Test fun actualPhotoToGraphTableAndCsvShareUsesSameDataset() {
        val file = photo()
        var export: File? = null
        try {
            val model = model()
            model.open(Uri.fromFile(file), false)
            await(model)
            assertNotNull(model.state.value.preview)
            model.channel(LightChannel.R)
            model.analyze(false)
            await(model)
            val data = requireNotNull(model.state.value.datasets["A"])
            // Results appear beside the media without forcing a floating graph panel.
            assertEquals(LightPage.SELECT, model.state.value.page)
            assertEquals(255.0, data.observations.single().value, 1e-9)
            assertEquals(Offset(4f, 3f), data.geometry.start)
            model.page(LightPage.TABLE)
            assertSame(data, model.state.value.datasets["A"])
            export = createLightExport(context, listOf(data), false)
            assertEquals(lightCsv(listOf(data)), export.readText())
            val intent = lightShareIntent(context, export, false)
            assertEquals("text/csv", intent.type)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            val uri = requireNotNull(intent.clipData).getItemAt(0).uri
            context.contentResolver.openInputStream(uri)!!.use { assertTrue(it.readBytes().isNotEmpty()) }
            model.channel(LightChannel.B)
            assertTrue(model.state.value.datasets.isEmpty())
        } finally { file.delete(); export?.delete() }
    }

    @Test fun newMediaAndRegionChangesDoNotMixOldObservations() {
        val file = photo()
        try {
            val model = model()
            model.open(Uri.fromFile(file), false); await(model)
            model.analyze(false); await(model)
            val a = requireNotNull(model.state.value.datasets["A"])
            model.dual(true); model.selectSlot("B")
            model.geometry(LightGeometry(LightTool.POINT, Offset.Zero, Offset.Zero))
            model.analyze(false); await(model)
            assertSame(a, model.state.value.datasets["A"])
            assertEquals(0.0, model.state.value.datasets["B"]!!.observations.single().value, 1e-9)
            model.geometry(LightGeometry(LightTool.POINT, Offset(1f, 1f), Offset(1f, 1f)))
            assertFalse(model.state.value.datasets.containsKey("B"))
            assertTrue(model.state.value.datasets.containsKey("A"))
            model.open(Uri.fromFile(file), false); await(model)
            assertTrue(model.state.value.datasets.isEmpty())
        } finally { file.delete() }
    }

    @Test fun gestureCannotSwitchPointIntoBox() {
        val file = photo()
        try {
            val model = model()
            model.open(Uri.fromFile(file), false)
            await(model)
            assertEquals(LightTool.POINT, model.state.value.geometries["A"]?.tool)
            model.geometry(LightGeometry(LightTool.BOX, Offset.Zero, Offset(5f, 4f)))
            val selected = requireNotNull(model.state.value.geometries["A"])
            assertEquals(LightTool.POINT, selected.tool)
            assertEquals(selected.start, selected.end)
            model.tool(LightTool.BOX)
            assertEquals(LightTool.BOX, model.state.value.geometries["A"]?.tool)
        } finally { file.delete() }
    }

    @Test fun exportedPdfIsReadableAndPaginated() {
        val data = LightDataset("Known gradient", "file://test", 200, 10, "A", LightChannel.R,
            LightGeometry(LightTool.LINE, Offset.Zero, Offset(199f, 0f)), false,
            (0..199).map { LightObservation(it.toDouble(), it.toDouble()) }, "Generated fixture; no calibration")
        val file = createLightExport(context, listOf(data), true)
        try {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)
            try {
                assertTrue(renderer.pageCount > 1)
                val page = renderer.openPage(0)
                try {
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    try { page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                    finally { bitmap.recycle() }
                } finally { page.close() }
            } finally { renderer.close() }
        } finally { file.delete() }
    }

    @Test fun fourRegionsStayIndependentAndExcelCanBeShared() {
        val file = photo()
        var export: File? = null
        try {
            val model = model()
            model.open(Uri.fromFile(file), false); await(model)
            model.regions(4)
            lightRegionLabels(4).forEach { slot ->
                model.selectSlot(slot)
                model.analyze(false); await(model)
            }
            assertEquals(setOf("A", "B", "C", "D"), model.state.value.datasets.keys)
            model.selectSlot("C"); model.clear()
            assertEquals(setOf("A", "B", "D"), model.state.value.datasets.keys)
            export = createLightSpreadsheet(context, model.state.value.datasets.values.toList())
            val intent = lightShareIntent(context, export, false)
            assertEquals(LIGHT_XLSX_MIME, intent.type)
            context.contentResolver.openInputStream(requireNotNull(intent.clipData).getItemAt(0).uri)!!.use {
                assertEquals('P'.code, it.read()); assertEquals('K'.code, it.read())
            }
        } finally { file.delete(); export?.delete() }
    }

    @Test fun addedRegionsCanUseDifferentProfilesAndKeepTheirResults() {
        val file = photo()
        try {
            val model = model()
            model.open(Uri.fromFile(file), false); await(model)
            model.tool(LightTool.LINE)
            model.addRegion()
            assertEquals("B", model.state.value.active)
            assertEquals(LightTool.LINE, model.state.value.geometries["B"]?.tool)
            model.tool(LightTool.BOX)
            model.analyze(false); await(model)
            model.selectSlot("A")
            assertEquals(LightTool.LINE, model.state.value.geometries["A"]?.tool)
            model.analyze(false); await(model)
            assertEquals(LightTool.LINE, model.state.value.datasets["A"]?.geometry?.tool)
            assertEquals(LightTool.BOX, model.state.value.datasets["B"]?.geometry?.tool)
            assertTrue(model.state.value.datasets.getValue("A").observations.isNotEmpty())
            assertTrue(model.state.value.datasets.getValue("B").observations.isNotEmpty())
        } finally { file.delete() }
    }

    @Test fun exifRotationChangesSourceGridWithoutDownsampling() {
        val file = File.createTempFile("light-rotation-", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.RED)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            ExifInterface(file.path).apply { setAttribute(ExifInterface.TAG_ORIENTATION, "6"); saveAttributes() }
            val decoded = decodeLightPhoto(context, Uri.fromFile(file))
            try { assertEquals(8, decoded.width); assertEquals(12, decoded.height) }
            finally { decoded.recycle() }
        } finally { bitmap.recycle(); file.delete() }
    }

    @Test fun savedProjectReopensAcrossViewModelsWithExactResults() {
        val file = photo()
        try {
            val first = model()
            first.open(Uri.fromFile(file), false); await(first)
            first.channel(LightChannel.R); first.analyze(false); await(first)
            val expected = requireNotNull(first.state.value.datasets["A"])
            val id = requireNotNull(first.state.value.projectId)
            first.saveProjectNow()
            runBlocking { withTimeout(10_000) {
                while (runCatching { LightProjectStore(testStorage).load(id).datasets["A"] }.getOrNull() != expected) delay(20)
            } }
            first.viewModelScope.cancel()
            val second = model(); second.reopen(id)
            runBlocking { withTimeout(10_000) {
                while (second.state.value.projectId != id || second.state.value.busy || second.state.value.preview == null) delay(20)
            } }
            assertEquals(expected, second.state.value.datasets["A"])
            assertEquals(LightChannel.R, second.state.value.channel)
        } finally { file.delete() }
    }

    @Test fun bundledDemoMeasuresRealDecodedFrames() {
        val model = model(); model.openDemo(); await(model)
        assertNotNull(model.state.value.preview)
        model.tool(LightTool.LINE); model.analyze(false); await(model)
        assertTrue(model.state.value.datasets.getValue("A").observations.size > 1)
        model.analyze(true); await(model)
        val data = model.state.value.datasets.getValue("A")
        assertTrue(data.complete)
        assertEquals(96, data.observations.size)
        assertTrue(data.observations.zipWithNext().all { (a, b) -> a.timestampUs!! < b.timestampUs!! })
        assertTrue(data.observations.maxOf { it.value } - data.observations.minOf { it.value } > 20)
    }
}
