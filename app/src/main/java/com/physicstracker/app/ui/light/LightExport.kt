package com.physicstracker.app.ui.light

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale
import kotlin.math.abs

private fun lightCsvCell(value: String): String {
    val safe = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'$value" else value
    return "\"${safe.replace("\"", "\"\"")}\""
}

fun lightCsv(datasets: List<LightDataset>): String = buildString {
    append("media,region,mode,channel,axis_label,axis_value,intensity_0_255,timestamp_us,source_width,source_height,geometry,method,complete,provenance,gap_before,raw_intensity_0_255\r\n")
    datasets.forEach { data -> data.observations.forEach { sample ->
        append(listOf(lightCsvCell(data.mediaName), lightCsvCell(data.slot),
            if (data.timeSeries) "TIME" else "PROFILE", data.channel.name, lightCsvCell(data.axisLabel),
            sample.axis.toString(), sample.value.toString(), sample.timestampUs?.toString().orEmpty(),
            data.width.toString(), data.height.toString(), lightCsvCell(data.geometry.toString()),
            lightCsvCell(data.method), data.complete.toString(), lightCsvCell(data.provenance), sample.gapBefore.toString(), (sample.rawValue ?: sample.value).toString()).joinToString(","))
        append("\r\n")
    } }
}

/** Screen/PDF display reduction keeps extrema within consecutive buckets; raw data stays intact. */
fun lightPlotSamples(samples: List<LightObservation>, limit: Int = 1200): List<LightObservation> {
    require(limit >= 4)
    if (samples.size <= limit) return samples
    // A gap is a data boundary: never reduce across it or connect it in either renderer.
    if (samples.drop(1).any { it.gapBefore }) {
        val segments = mutableListOf<MutableList<LightObservation>>()
        samples.forEach { sample ->
            if (segments.isEmpty() || sample.gapBefore) segments.add(mutableListOf())
            segments.last().add(sample)
        }
        return segments.flatMap { segment -> lightPlotSamples(segment, (limit * segment.size / samples.size).coerceAtLeast(4)) }
    }
    val size = ((samples.size + limit / 4 - 1) / (limit / 4)).coerceAtLeast(1)
    return samples.chunked(size).flatMap { bucket ->
        val min = bucket.indices.minBy { bucket[it].value }
        val max = bucket.indices.maxBy { bucket[it].value }
        listOf(0, min, max, bucket.lastIndex).distinct().sorted().map { bucket[it] }
    }
}

fun createLightExport(context: Context, datasets: List<LightDataset>, pdf: Boolean): File {
    require(datasets.isNotEmpty() && datasets.any { it.observations.isNotEmpty() }) { "Analyze a selection before exporting." }
    val directory = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS))
    check(directory.exists() || directory.mkdirs()) { "Export folder is unavailable." }
    val file = File.createTempFile("PhysicsTracker-Light-", if (pdf) ".pdf" else ".csv", directory)
    try {
        if (!pdf) file.writeText(lightCsv(datasets), Charsets.UTF_8) else writeLightPdf(file, datasets)
        return file
    } catch (error: Exception) {
        file.delete() // Only this newly generated, incomplete export.
        throw error
    }
}

private fun writeLightPdf(file: File, datasets: List<LightDataset>) {
    val document = PdfDocument()
    try {
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.BLACK }
        val heading = Paint(text).apply { textSize = 17f; isFakeBoldText = true }
        var number = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun newPage() {
            page?.let(document::finishPage)
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, ++number).create())
            y = 40f
            page!!.canvas.drawText("PhysicsTracker · Light Analysis", 36f, y, heading)
            page!!.canvas.drawText("Page $number", 505f, 817f, text)
            y += 25f
        }
        fun line(value: String) {
            var rest = value.replace('\n', ' ')
            while (rest.isNotEmpty()) {
                if (y > 785) newPage()
                val length = text.breakText(rest, true, 523f, null).coerceAtLeast(1)
                page!!.canvas.drawText(rest.take(length), 36f, y, text)
                rest = rest.drop(length)
                y += 15f
            }
        }
        datasets.forEach { data ->
            newPage()
            line("${data.mediaName} · Region ${data.slot} · ${if (data.timeSeries) "Time series" else "Spatial profile"}")
            line("${data.width} × ${data.height} source pixels (orientation applied)")
            line("${data.intensityLabel}; image brightness is not calibrated illuminance.")
            line("${data.geometry}; ${data.method}")
            line("Provenance: ${data.provenance}")
            line("Status: ${if (data.complete) "Complete" else "Partial / interrupted"}; ${data.observations.size} samples")
            val values = data.observations.map { it.value }
            if (values.isNotEmpty()) line("Minimum ${values.minOrNull()}; mean ${values.average()}; maximum ${values.maxOrNull()}")
            if (y + 240 > 785) newPage()
            drawLightPdfPlot(page!!.canvas, data, 65f, y + 10f, 475f, 160f)
            y += 225f
            line("${data.axisLabel} | ${data.intensityLabel} | decoder timestamp (µs, video only)")
            data.observations.take(200).forEach { sample ->
                line(String.format(Locale.US, "%.6f | %.6f | %s", sample.axis, sample.value, sample.timestampUs?.toString() ?: "—"))
            }
            if (data.observations.size > 200) line("Table preview: first 200 of ${data.observations.size} samples. Export CSV for every row. Graph includes the full range with an extrema-preserving display reduction.")
        }
        page?.let(document::finishPage)
        file.outputStream().use(document::writeTo)
    } finally {
        document.close()
    }
}

private fun drawLightPdfPlot(canvas: Canvas, data: LightDataset, left: Float, top: Float, width: Float, height: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; strokeWidth = 1f; textSize = 10f }
    canvas.drawLine(left, top, left, top + height, paint)
    canvas.drawLine(left, top + height, left + width, top + height, paint)
    val bounds = LightPlotBounds.of(data.observations)
    canvas.drawText("255", left - 25, top + 4, paint)
    canvas.drawText("0", left - 15, top + height, paint)
    canvas.drawText(String.format(Locale.US, "%.3f", bounds.minX), left, top + height + 15, paint)
    canvas.drawText(String.format(Locale.US, "%.3f", bounds.maxX), left + width - 45, top + height + 15, paint)
    canvas.drawText(data.axisLabel, left, top + height + 33, paint)
    canvas.drawText(data.intensityLabel, left, top - 2, paint)
    paint.color = if (data.slot == "A") Color.rgb(0, 100, 155) else Color.rgb(145, 70, 0)
    val samples = lightPlotSamples(data.observations)
    val points = samples.map(bounds::normalized)
    points.zipWithNext().forEachIndexed { index, (a, b) ->
        if (!samples[index + 1].gapBefore) canvas.drawLine(left + a.x * width, top + a.y * height, left + b.x * width, top + b.y * height, paint)
    }
    if (points.size == 1) canvas.drawCircle(left + points[0].x * width, top + points[0].y * height, 3f, paint)
}

fun lightShareIntent(context: Context, file: File, pdf: Boolean): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return Intent(Intent.ACTION_SEND).apply {
        type = if (file.extension == "xlsx") LIGHT_XLSX_MIME else if (pdf) "application/pdf" else "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("Light Analysis", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
