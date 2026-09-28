package com.physicstracker.app.ui.light

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

const val LIGHT_XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

fun writeLightSpreadsheet(output: OutputStream, datasets: List<LightDataset>) {
    fun xml(value: Any?) = value.toString().filter { it >= ' ' || it == '\n' || it == '\t' }
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    fun row(values: List<Any?>) = "<row>" + values.joinToString("") { value ->
        if (value is Number) "<c><v>$value</v></c>" else "<c t=\"inlineStr\"><is><t>${xml(value ?: "")}</t></is></c>"
    } + "</row>"
    ZipOutputStream(output).use { zip ->
        fun entry(name: String, text: String) { zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray(Charsets.UTF_8)); zip.closeEntry() }
        entry("[Content_Types].xml", """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
        entry("_rels/.rels", """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
        entry("xl/workbook.xml", """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Light measurements" sheetId="1" r:id="rId1"/></sheets></workbook>""")
        entry("xl/_rels/workbook.xml.rels", """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        fun append(text: String) { zip.write(text.toByteArray(Charsets.UTF_8)) }
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        append(row(listOf("Media", "Region", "Mode", "Channel", "Axis", "Position / time", "Displayed intensity (0-255)", "Raw intensity (0-255)", "Timestamp (us)", "Width", "Height", "Geometry", "Method", "Complete", "Provenance", "Gap before")))
        datasets.forEach { data -> data.observations.forEach { sample ->
            append(row(listOf(data.mediaName, data.slot, if (data.timeSeries) "TIME" else "PROFILE", data.channel.displayName(),
                data.axisLabel, sample.axis, sample.value, sample.rawValue ?: sample.value, sample.timestampUs?.toString() ?: "",
                data.width, data.height, data.geometry, data.method, data.complete, data.provenance, sample.gapBefore)))
        } }
        append("</sheetData></worksheet>")
        zip.closeEntry()
    }
}

fun createLightSpreadsheet(context: Context, datasets: List<LightDataset>): File {
    require(datasets.any { it.observations.isNotEmpty() })
    val directory = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS))
    check(directory.exists() || directory.mkdirs())
    val file = File.createTempFile("PhysicsTracker-Light-", ".xlsx", directory)
    try { file.outputStream().use { writeLightSpreadsheet(it, datasets) }; return file }
    catch (error: Exception) { file.delete(); throw error }
}
