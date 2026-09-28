package com.physicstracker.app.ui

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DemoCatalogTest {
    @Test fun everyEntryHasPackagedMediaAndUniqueIdentity() {
        assertEquals(30, DemoCatalog.items.size)
        assertEquals(30, DemoCatalog.items.map { it.asset }.distinct().size)
        DemoCatalog.items.forEach {
            assertTrue("Missing ${it.asset}", File("src/main/assets", it.asset).isFile)
            assertTrue(it.projectName.startsWith("Demo — "))
            assertFalse(it.instruction.isBlank())
        }
    }

    @Test fun catalogueMatchesAllSuppliedAssetsWithoutOmissions() {
        val packaged = File("src/main/assets/demos/supplied").list()!!.toSet()
        assertEquals(packaged, DemoCatalog.items.filterNot { it.guided }.map { it.file }.toSet())
        assertEquals(2, DemoCatalog.items.count { !it.video })
        assertEquals(12, DemoCatalog.items.count { !it.light })
    }

    @Test fun convertedClipsCannotBePresentedAsOriginalIntensityReferences() {
        val converted = DemoCatalog.items.filter { it.converted }
        assertEquals(18, converted.size)
        converted.forEach {
            assertTrue(it.projectName.contains("converted"))
            assertTrue(it.sourceNote.contains("changes pixel values"))
        }
    }
}
