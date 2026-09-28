package com.physicstracker.app.ui.light

import androidx.compose.ui.geometry.Offset
import com.physicstracker.app.ui.LightChannel
import com.physicstracker.app.ui.LightGeometry
import com.physicstracker.app.ui.LightTool
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.nio.file.Files
import java.util.UUID

class LightProjectStoreTest {
    private fun project(): SavedLightProject {
        val geometry = LightGeometry(LightTool.LINE, Offset(1f, 2f), Offset(30f, 40f))
        val datasets = lightRegionLabels(4).associateWith { slot ->
            LightDataset("Experiment", "content://video", 640, 480, slot, LightChannel.G, geometry, true,
                listOf(LightObservation(0.123456, 18.5, 123456), LightObservation(0.234567, 42.5, 234567, true)), "Actual PTS", false)
        }
        return SavedLightProject(UUID.randomUUID().toString(), "content://video", "Saved light", true,
            640, 480, 123456, "BT.709", "C", 4, LightChannel.G, lightRegionLabels(4).associateWith { geometry }, datasets,
            timeMode = true, smooth = true, baseline = true, peaks = true, compare = true, updatedAt = 123456789L)
    }
    @Test fun roundTripRestoresAllRegionsSettingsAndExactTimes() {
        val p = project()
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { LightProjectStore.write(it, p) }
        val result = DataInputStream(bytes.toByteArray().inputStream()).use(LightProjectStore::read)
        assertEquals(p, result)
        assertEquals(8, result.summary.samples)
        assertTrue(result.datasets.values.all { !it.complete && it.observations.last().gapBefore })
    }
    @Test fun roundTripRestoresMoreThanFourIndependentPoints() {
        val original = project()
        val point = LightGeometry(LightTool.POINT, Offset(8f, 9f), Offset(8f, 9f))
        val extended = original.copy(regionCount = 5, active = "E",
            geometries = original.geometries + ("E" to point),
            datasets = original.datasets + ("E" to original.datasets.getValue("A").copy(slot = "E", geometry = point)))
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { LightProjectStore.write(it, extended) }
        val result = DataInputStream(bytes.toByteArray().inputStream()).use(LightProjectStore::read)
        assertEquals(extended, result)
        assertNotEquals(result.datasets.getValue("A").slot, result.datasets.getValue("E").slot)
    }
    @Test fun separateStoreInstanceReopensSavedHistoryAndReplacesWithoutDuplication() {
        val directory = Files.createTempDirectory("light-history-test").toFile()
        try {
            val original = project()
            LightProjectStore(directory).save(original)
            val reopened = LightProjectStore(directory)
            assertEquals(original, reopened.load(original.id))
            assertEquals(listOf(original.summary), reopened.history())
            val changed = original.copy(name = "Renamed", updatedAt = 999999999L, datasets = original.datasets - "C")
            reopened.save(changed)
            assertEquals(listOf(changed.summary), LightProjectStore(directory).history())
            assertEquals(changed, reopened.load(original.id))
        } finally { directory.deleteRecursively() }
    }
    @Test fun interruptedCommitRecoversBackup() {
        val directory = Files.createTempDirectory("light-backup-test").toFile()
        try {
            val p = project(); val store = LightProjectStore(directory); store.save(p)
            val target = File(directory, "${p.id}.light")
            assertTrue(target.renameTo(File(target.path + ".bak")))
            File(target.path + ".new").writeBytes(byteArrayOf(0, 1))
            assertEquals(listOf(p.summary), LightProjectStore(directory).history())
            assertEquals(p, store.load(p.id))
        } finally { directory.deleteRecursively() }
    }
    @Test fun failedWriteKeepsPriorSnapshot() {
        val directory = Files.createTempDirectory("light-failed-save-test").toFile()
        try {
            val p = project(); val store = LightProjectStore(directory); store.save(p)
            try { store.save(p.copy(name = "x".repeat(70_000))); fail("Expected rejected string") } catch (_: UTFDataFormatException) { }
            assertEquals(p, store.load(p.id))
        } finally { directory.deleteRecursively() }
    }
    @Test fun unknownVersionAndTruncatedInputAreRejected() {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { LightProjectStore.write(it, project()) }
        try { LightProjectStore.read(DataInputStream(bytes.toByteArray().take(16).toByteArray().inputStream())); fail("Expected truncated snapshot rejection") }
        catch (_: EOFException) { }
        try { LightProjectStore.read(DataInputStream(byteArrayOf(0, 0, 0, 0).inputStream())); fail("Expected invalid version rejection") }
        catch (_: IllegalArgumentException) { }
    }
}
