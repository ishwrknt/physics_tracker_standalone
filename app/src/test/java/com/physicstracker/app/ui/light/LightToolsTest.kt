package com.physicstracker.app.ui.light

import androidx.compose.ui.geometry.Offset
import com.physicstracker.app.ui.LightChannel
import com.physicstracker.app.ui.LightGeometry
import com.physicstracker.app.ui.LightTool
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class LightToolsTest {
    private fun data(samples: List<LightObservation>) = LightDataset("Test & <image>", "test", 100, 100, "C", LightChannel.R,
        LightGeometry(LightTool.LINE, Offset.Zero, Offset(99f, 0f)), false, samples, "Fixture")

    @Test fun lineAndAreaStartWithVisibleExtentEvenAtEdges() {
        for (point in listOf(Offset.Zero, Offset(50f, 50f), Offset(99f, 99f))) {
            val geometry = LightGeometry(LightTool.POINT, point, point)
            for (tool in listOf(LightTool.LINE, LightTool.BOX)) {
                val result = lightGeometryForTool(geometry, tool, 100, 100)
                assertTrue(result.start.x < result.end.x)
                if (tool == LightTool.BOX) assertTrue(result.start.y < result.end.y)
            }
        }
    }
    @Test fun newRegionInheritsSelectedToolWithIndependentGeometry() {
        val anchor = Offset(40f, 30f)
        LightTool.entries.forEach { tool ->
            val active = LightGeometry(tool, anchor, Offset(60f, 50f))
            val added = lightNewRegionGeometry(active, 100, 80)
            assertEquals(tool, added.tool)
            assertEquals(anchor, active.start)
            assertEquals(Offset(60f, 50f), active.end)
            if (tool == LightTool.POINT) assertEquals(added.start, added.end)
            else {
                assertTrue(added.start.x < added.end.x)
                if (tool == LightTool.BOX) assertTrue(added.start.y < added.end.y)
            }
            assertNotSame(active, added)
        }
    }
    @Test fun processingPreservesRawValuesTimingAndGaps() {
        val raw = data(listOf(LightObservation(0.0, 10.0, 0), LightObservation(1.0, 30.0, 1000),
            LightObservation(2.0, 100.0, 2000, true)))
        val result = processedLightData(raw, true, true)
        assertEquals(listOf(10.0, 10.0, 90.0), result.observations.map { it.value })
        assertEquals(listOf(10.0, 30.0, 100.0), result.observations.map { it.rawValue })
        assertEquals(listOf(10.0, 30.0, 100.0), raw.observations.map { it.value })
        assertEquals(raw.observations.map { it.timestampUs }, result.observations.map { it.timestampUs })
        assertTrue(result.observations.last().gapBefore)
        assertTrue(result.provenance.contains("DERIVED"))
    }
    @Test fun peaksDoNotCrossGaps() {
        val samples = listOf(LightObservation(0.0, 1.0), LightObservation(1.0, 9.0), LightObservation(2.0, 1.0))
        assertEquals(listOf(1), lightPeakIndices(samples))
        assertTrue(lightPeakIndices(samples.dropLast(1) + samples.last().copy(gapBefore = true)).isEmpty())
    }
    @Test fun spreadsheetContainsIdentityProvenanceAndRawDataAsSafeText() {
        val output = ByteArrayOutputStream()
        writeLightSpreadsheet(output, listOf(processedLightData(data(listOf(LightObservation(0.0, 25.0))), true, false)))
        val entries = mutableMapOf<String, String>()
        ZipInputStream(output.toByteArray().inputStream()).use { zip ->
            while (true) { val entry = zip.nextEntry ?: break; entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8) }
        }
        assertTrue(entries.containsKey("[Content_Types].xml"))
        val sheet = entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("Test &amp; &lt;image&gt;"))
        assertTrue(sheet.contains("Raw intensity"))
        assertTrue(sheet.contains("DERIVED"))
        assertTrue(sheet.contains("<t>C</t>"))
        assertTrue(sheet.contains("<v>25.0</v>"))
    }
    @Test fun allFourRegionsAreAvailable() { assertEquals(listOf("A", "B", "C", "D"), lightRegionLabels(4)) }
    @Test fun additionalRegionsKeepStableUniqueLabels() {
        val labels = lightRegionLabels(30)
        assertEquals("Z", labels[25])
        assertEquals("AA", labels[26])
        assertEquals("AD", labels[29])
        assertEquals(30, labels.distinct().size)
    }
    @Test fun comparisonAlwaysSelectsTwoDistinctAvailableRegions() {
        assertEquals(listOf("A", "B"), lightComparisonSlots("A", "B", 2))
        assertEquals(listOf("B", "A"), lightComparisonSlots("B", "B", 4))
        assertEquals(listOf("A"), lightComparisonSlots("A", "B", 1))
    }
}
