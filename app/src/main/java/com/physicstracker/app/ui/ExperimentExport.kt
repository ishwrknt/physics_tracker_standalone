package com.physicstracker.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.os.Build
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.physicstracker.app.data.CalibrationEntity
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.min

data class ExportArtifacts(
    val csvFile: File,
    val excelFile: File,
    val pdfFile: File,
    val auditFile: File,
    val folder: File
)

data class ExportPhotogateSummary(
    val gateATimestampUs: Long?,
    val gateAFrame: Int?,
    val gateBTimestampUs: Long?,
    val gateBFrame: Int?,
    val deltaUs: Long?
)

data class ExportTwoBodySummary(
    val currentSeparation: Double?,
    val minimumSeparation: Double?,
    val maximumSeparation: Double?,
    val averageSeparation: Double?,
    val currentRelativeSpeed: Double?,
    val minimumRelativeSpeed: Double?,
    val maximumRelativeSpeed: Double?,
    val averageRelativeSpeed: Double?,
    val frameCount: Int,
    val unit: String,
    val trackAMassKg: Double,
    val trackBMassKg: Double,
    val trackAMomentum: Double?,
    val trackBMomentum: Double?,
    val totalMomentum: Double?,
    val relativeMomentum: Double?,
    val closestApproachTimeUs: Long?,
    val closestApproachFrame: Int?,
    val closestApproachSeparation: Double?,
    val closestApproachRelativeSpeed: Double?,
    val closestApproachMomentumA: Double?,
    val closestApproachMomentumB: Double?,
    val closestApproachTotalMomentum: Double?,
    val closestApproachRelativeMomentum: Double?,
    val interactionClass: String?,
    val preInteractionTimeUs: Long?,
    val preInteractionFrame: Int?,
    val preInteractionSeparation: Double?,
    val preInteractionRelativeSpeed: Double?,
    val preInteractionMomentumA: Double?,
    val preInteractionMomentumB: Double?,
    val preInteractionTotalMomentum: Double?,
    val preInteractionRelativeMomentum: Double?,
    val preInteractionDeltaUs: Long?,
    val postInteractionTimeUs: Long?,
    val postInteractionFrame: Int?,
    val postInteractionSeparation: Double?,
    val postInteractionRelativeSpeed: Double?,
    val postInteractionMomentumA: Double?,
    val postInteractionMomentumB: Double?,
    val postInteractionTotalMomentum: Double?,
    val postInteractionRelativeMomentum: Double?,
    val postInteractionDeltaUs: Long?,
    val gateAToClosestUs: Long?,
    val closestToGateBUs: Long?,
    val gateAToGateBUs: Long?,
    val momentumUnit: String
)

private data class ExportTrackSummary(
    val trackLabel: String,
    val sampleCount: Int,
    val frameStart: Int?,
    val frameEnd: Int?,
    val timeStartUs: Long?,
    val timeEndUs: Long?,
    val minX: Double?,
    val maxX: Double?,
    val minY: Double?,
    val maxY: Double?,
    val minSpeed: Double?,
    val maxSpeed: Double?,
    val avgSpeed: Double?,
    val maxAcceleration: Double?
)

private data class ExportChartSeries(
    val label: String,
    val samples: List<GraphSample>,
    val color: Int
)

private data class ExportChartLabels(
    val title: String,
    val xAxis: String,
    val yAxis: String
)

private fun exportAxisTitle(series: GraphSeries, unit: String): String = when (series) {
    GraphSeries.TIME -> "Time (s)"
    GraphSeries.X -> "Horizontal position ($unit)"
    GraphSeries.Y -> "Vertical position ($unit)"
    GraphSeries.VX -> "Horizontal velocity ($unit/s)"
    GraphSeries.VY -> "Vertical velocity ($unit/s)"
    GraphSeries.SPEED -> "Speed ($unit/s)"
    GraphSeries.AX -> "Horizontal acceleration ($unit/s²)"
    GraphSeries.AY -> "Vertical acceleration ($unit/s²)"
    GraphSeries.ACCEL -> "Acceleration magnitude ($unit/s²)"
    GraphSeries.SEPARATION -> "Separation between tracks ($unit)"
    GraphSeries.RELATIVE_SPEED -> "Relative speed between tracks ($unit/s)"
}

private fun exportGraphLabels(series: GraphSeries, unit: String, titlePrefix: String = "Motion graph") =
    ExportChartLabels(
        title = "$titlePrefix: ${exportAxisTitle(series, unit)} versus time",
        xAxis = "Time (s)",
        yAxis = exportAxisTitle(series, unit)
    )

private fun sanitizeFileName(name: String): String =
    name.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "physics_tracker" }

private fun exportTimestamp(): String =
    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

private fun exportGraphLimit(value: Float?): String =
    value?.let { String.format(Locale.US, "%.3f", it) } ?: "auto"

private fun escapeXml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

private fun xlsxCell(value: Any?): String = when (value) {
    null -> "<c/>"
    is Number -> "<c><v>$value</v></c>"
    is Boolean -> "<c t=\"b\"><v>${if (value) 1 else 0}</v></c>"
    else -> "<c t=\"inlineStr\"><is><t>${escapeXml(value.toString())}</t></is></c>"
}

private fun xlsxSheet(rows: List<List<Any?>>): String {
    val body = rows.mapIndexed { rowIndex, row ->
        "<row r=\"${rowIndex + 1}\">${row.joinToString(separator = "", transform = ::xlsxCell)}</row>"
    }.joinToString("")
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
          <sheetData>$body</sheetData>
        </worksheet>""".trimIndent()
}

private fun xlsxObservationRows(
    points: List<DisplayKinematicsPoint>,
    unit: String
): List<List<Any?>> = buildList {
    add(
        listOf(
            "Tracker ID", "Tracker", "Frame", "Time (s)", "Requested timestamp (µs)",
            "Actual timestamp (µs)", "X ($unit)", "Y ($unit)", "Vx ($unit/s)",
            "Vy ($unit/s)", "Speed ($unit/s)", "Ax ($unit/s²)", "Ay ($unit/s²)",
            "Acceleration ($unit/s²)", "Representation", "Observation", "Algorithm",
            "Confidence", "Ambiguity", "Residual error (px)", "Manual correction",
            "Coordinate space", "Gap"
        )
    )
    points.forEach { point ->
        add(
            listOf(
                point.trackerId,
                point.trackLabel,
                point.frameNumber,
                point.timestampUs / 1_000_000.0,
                point.requestedTimestampUs,
                point.actualTimestampUs,
                point.xValue,
                point.yValue,
                point.vxValue,
                point.vyValue,
                point.speedValue,
                point.axValue,
                point.ayValue,
                point.accelerationValue,
                point.dataRepresentation,
                point.observationKind,
                point.sourceAlgorithm,
                point.confidence,
                point.ambiguity,
                point.residualErrorPx,
                point.manualCorrection,
                point.coordinateSpace,
                point.gapLabel
            )
        )
    }
}

private fun writeScientificXlsx(
    file: File,
    projectName: String,
    displayedUnit: String,
    rawPoints: List<DisplayKinematicsPoint>,
    selectedPoints: List<DisplayKinematicsPoint>,
    selectedSeries: GraphSeries,
    activeViewLabel: String,
    processingStrength: Float,
    averagingWindow: Int,
    calibration: CalibrationEntity?
) {
    val metadata = listOf(
        listOf("Field", "Value"),
        listOf("Project", projectName),
        listOf("Exported at", formatAuditTimestamp(System.currentTimeMillis())),
        listOf("Selected representation", selectedPoints.firstOrNull()?.dataRepresentation ?: "RAW"),
        listOf("Processing strength", processingStrength),
        listOf("Averaging window (samples)", averagingWindow),
        listOf("Graph series", graphSeriesLabel(selectedSeries, displayedUnit)),
        listOf("Active view", activeViewLabel),
        listOf("Time axis", "Time (s)"),
        listOf("Position unit", displayedUnit),
        listOf("Velocity unit", "$displayedUnit/s"),
        listOf("Acceleration unit", "$displayedUnit/s²"),
        listOf("Calibration distance", calibration?.let { "${displayCalibrationDistance(it)} ${it.calibrationUnit}" } ?: "None"),
        listOf("Calibration coordinate space", calibration?.coordinateSpace ?: "None")
    )
    val statistics = buildList {
        add(listOf("Tracker", "Samples", "First frame", "Last frame", "Mean speed ($displayedUnit/s)", "Maximum acceleration ($displayedUnit/s²)"))
        trackSummaries(selectedPoints).forEach { summary ->
            add(
                listOf(
                    summary.trackLabel,
                    summary.sampleCount,
                    summary.frameStart,
                    summary.frameEnd,
                    summary.avgSpeed,
                    summary.maxAcceleration
                )
            )
        }
    }
    val quality = buildList {
        add(listOf("Tracker", "Frame", "Status", "Confidence", "Algorithm", "Manual correction", "Gap"))
        selectedPoints.forEach { point ->
            add(
                listOf(
                    point.trackLabel,
                    point.frameNumber,
                    point.observationKind,
                    point.confidence,
                    point.sourceAlgorithm,
                    point.manualCorrection,
                    point.gapLabel
                )
            )
        }
    }
    val sheets = listOf(
        "Metadata" to xlsxSheet(metadata),
        "Raw observations" to xlsxSheet(xlsxObservationRows(rawPoints, displayedUnit)),
        "Selected representation" to xlsxSheet(xlsxObservationRows(selectedPoints, displayedUnit)),
        "Statistics" to xlsxSheet(statistics),
        "Quality information" to xlsxSheet(quality)
    )
    ZipOutputStream(FileOutputStream(file)).use { zip ->
        fun entry(path: String, content: String) {
            zip.putNextEntry(ZipEntry(path))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        entry(
            "[Content_Types].xml",
            """<?xml version="1.0" encoding="UTF-8"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              ${sheets.indices.joinToString("") { "<Override PartName=\"/xl/worksheets/sheet${it + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" }}
            </Types>""".trimIndent()
        )
        entry(
            "_rels/.rels",
            """<?xml version="1.0" encoding="UTF-8"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>""".trimIndent()
        )
        entry(
            "xl/workbook.xml",
            """<?xml version="1.0" encoding="UTF-8"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>${sheets.mapIndexed { index, sheet -> "<sheet name=\"${escapeXml(sheet.first)}\" sheetId=\"${index + 1}\" r:id=\"rId${index + 1}\"/>" }.joinToString("")}</sheets>
            </workbook>""".trimIndent()
        )
        entry(
            "xl/_rels/workbook.xml.rels",
            """<?xml version="1.0" encoding="UTF-8"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              ${sheets.indices.joinToString("") { "<Relationship Id=\"rId${it + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${it + 1}.xml\"/>" }}
            </Relationships>""".trimIndent()
        )
        sheets.forEachIndexed { index, sheet -> entry("xl/worksheets/sheet${index + 1}.xml", sheet.second) }
    }
}

private fun trackSummaries(points: List<DisplayKinematicsPoint>): List<ExportTrackSummary> =
    points.groupBy { it.trackLabel }
        .toSortedMap()
        .map { (trackLabel, samples) ->
            val ordered = samples.sortedBy { it.timestampUs }
            val speeds = ordered.mapNotNull { it.speedValue }
            val accelerations = ordered.mapNotNull { it.accelerationValue }
            ExportTrackSummary(
                trackLabel = trackLabel,
                sampleCount = ordered.size,
                frameStart = ordered.firstOrNull()?.frameNumber,
                frameEnd = ordered.lastOrNull()?.frameNumber,
                timeStartUs = ordered.firstOrNull()?.timestampUs,
                timeEndUs = ordered.lastOrNull()?.timestampUs,
                minX = ordered.minOfOrNull { it.xValue },
                maxX = ordered.maxOfOrNull { it.xValue },
                minY = ordered.minOfOrNull { it.yValue },
                maxY = ordered.maxOfOrNull { it.yValue },
                minSpeed = speeds.minOrNull(),
                maxSpeed = speeds.maxOrNull(),
                avgSpeed = speeds.takeIf { it.isNotEmpty() }?.average(),
                maxAcceleration = accelerations.maxOrNull()
            )
        }

private fun trendDescription(values: List<Double>): String {
    if (values.size < 2) return "insufficient trend data"
    val start = values.first()
    val end = values.last()
    val delta = end - start
    return when {
        kotlin.math.abs(delta) < 1e-6 -> "stays nearly constant"
        delta > 0 -> "rises overall"
        else -> "falls overall"
    }
}

private fun selectedSeriesSamples(points: List<DisplayKinematicsPoint>, selectedSeries: GraphSeries): List<Double> =
    points.mapNotNull { point ->
        when (selectedSeries) {
            GraphSeries.TIME -> point.timestampUs / 1_000_000.0
            GraphSeries.X -> point.xValue
            GraphSeries.Y -> point.yValue
            GraphSeries.VX -> point.vxValue
            GraphSeries.VY -> point.vyValue
            GraphSeries.SPEED -> point.speedValue
            GraphSeries.AX -> point.axValue
            GraphSeries.AY -> point.ayValue
            GraphSeries.ACCEL -> point.accelerationValue
            GraphSeries.SEPARATION, GraphSeries.RELATIVE_SPEED -> null
        }
    }

private fun buildAnalysisNarrative(
    points: List<DisplayKinematicsPoint>,
    selectedSeries: GraphSeries,
    displayedUnit: String,
    profileResult: ProfileResult?,
    twoBodySummary: ExportTwoBodySummary?
): List<String> {
    val summaries = trackSummaries(points)
    val lines = mutableListOf<String>()
    summaries.forEach { summary ->
        lines += buildString {
            append("Track ${summary.trackLabel}: ")
            append("${summary.sampleCount} samples")
            summary.timeStartUs?.let { append(", from ${formatSeconds(it)} s") }
            summary.timeEndUs?.let { append(" to ${formatSeconds(it)} s") }
            summary.avgSpeed?.let { append(", avg speed ${formatCalibratedValue(it)} $displayedUnit/s") }
            summary.maxSpeed?.let { append(", max speed ${formatCalibratedValue(it)} $displayedUnit/s") }
            summary.maxAcceleration?.let { append(", max accel ${formatCalibratedValue(it)} $displayedUnit/s²") }
        }
    }
    val selectedValues = selectedSeriesSamples(points, selectedSeries)
    if (selectedValues.isNotEmpty()) {
        lines += "Selected graph series ${graphSeriesLabel(selectedSeries, displayedUnit)} ${trendDescription(selectedValues)} over the exported time window."
        lines += "Observed range: ${formatCalibratedValue(selectedValues.minOrNull() ?: 0.0)} to ${formatCalibratedValue(selectedValues.maxOrNull() ?: 0.0)}."
    }
    profileResult?.let {
        lines += "Profile fit ${it.label.lowercase(Locale.US)} reports RMSE ${formatCalibratedValue(it.rmse)} and R² ${formatCalibratedValue(it.rSquared)}."
        lines += "Equation: ${it.equation}."
    }
    twoBodySummary?.takeIf { it.frameCount > 0 }?.let {
        lines += "Two-track comparison keeps both objects separate and reports current separation ${it.currentSeparation?.let(::formatCalibratedValue) ?: "--"} ${it.unit}."
        lines += "Relative speed spans ${it.minimumRelativeSpeed?.let(::formatCalibratedValue) ?: "--"} to ${it.maximumRelativeSpeed?.let(::formatCalibratedValue) ?: "--"} ${it.unit}/s."
    }
    return lines
}

private fun graphSamplesForSeries(points: List<DisplayKinematicsPoint>, selectedSeries: GraphSeries): List<GraphSample> =
    points.mapNotNull { point ->
        val value = when (selectedSeries) {
            GraphSeries.TIME -> point.timestampUs / 1_000_000.0
            GraphSeries.X -> point.xValue
            GraphSeries.Y -> point.yValue
            GraphSeries.VX -> point.vxValue
            GraphSeries.VY -> point.vyValue
            GraphSeries.SPEED -> point.speedValue
            GraphSeries.AX -> point.axValue
            GraphSeries.AY -> point.ayValue
            GraphSeries.ACCEL -> point.accelerationValue
            GraphSeries.SEPARATION, GraphSeries.RELATIVE_SPEED -> null
        } ?: return@mapNotNull null
        GraphSample(point.timestampUs / 1_000_000.0, value)
    }

private fun deltaSamplesForSeries(
    trackA: List<DisplayKinematicsPoint>,
    trackB: List<DisplayKinematicsPoint>,
    selectedSeries: GraphSeries
): List<GraphSample> {
    if (selectedSeries == GraphSeries.SEPARATION || selectedSeries == GraphSeries.RELATIVE_SPEED) return emptyList()
    val byFrameA = trackA.associateBy { it.frameNumber }
    return trackB.mapNotNull { pointB ->
        val pointA = byFrameA[pointB.frameNumber] ?: return@mapNotNull null
        val valueA = when (selectedSeries) {
            GraphSeries.TIME -> pointA.timestampUs / 1_000_000.0
            GraphSeries.X -> pointA.xValue
            GraphSeries.Y -> pointA.yValue
            GraphSeries.VX -> pointA.vxValue
            GraphSeries.VY -> pointA.vyValue
            GraphSeries.SPEED -> pointA.speedValue
            GraphSeries.AX -> pointA.axValue
            GraphSeries.AY -> pointA.ayValue
            GraphSeries.ACCEL -> pointA.accelerationValue
            GraphSeries.SEPARATION, GraphSeries.RELATIVE_SPEED -> null
        } ?: return@mapNotNull null
        val valueB = when (selectedSeries) {
            GraphSeries.TIME -> pointB.timestampUs / 1_000_000.0
            GraphSeries.X -> pointB.xValue
            GraphSeries.Y -> pointB.yValue
            GraphSeries.VX -> pointB.vxValue
            GraphSeries.VY -> pointB.vyValue
            GraphSeries.SPEED -> pointB.speedValue
            GraphSeries.AX -> pointB.axValue
            GraphSeries.AY -> pointB.ayValue
            GraphSeries.ACCEL -> pointB.accelerationValue
            GraphSeries.SEPARATION, GraphSeries.RELATIVE_SPEED -> null
        } ?: return@mapNotNull null
        GraphSample(pointB.timestampUs / 1_000_000.0, valueB - valueA)
    }
}

private fun formatAuditTimestamp(timestampMs: Long?): String {
    if (timestampMs == null) return "unknown"
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestampMs))
}

private fun sha256(text: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

private data class AuditLogPayload(
    val content: String,
    val checksum: String
)

private fun buildAuditLog(
    context: Context,
    projectName: String,
    displayedUnit: String,
    points: List<DisplayKinematicsPoint>,
    calibration: CalibrationEntity?,
    selectedSeries: GraphSeries,
    graphViewport: GraphViewportSummary,
    showTrackA: Boolean,
    showTrackB: Boolean,
    showDelta: Boolean,
    profileResult: ProfileResult?,
    photogateSummary: ExportPhotogateSummary?,
    twoBodySummary: ExportTwoBodySummary?,
    trimStartMs: Long,
    lastCheckpointSavedAt: Long?
): AuditLogPayload {
    val appVersion = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }.getOrDefault("unknown")
    val lines = buildList {
        add("PhyZX Measurement Audit Log")
        add("exported_at=${formatAuditTimestamp(System.currentTimeMillis())}")
        add("project_name=${sanitizeFileName(projectName)}")
        add("app_version=$appVersion")
        add("device_manufacturer=${Build.MANUFACTURER}")
        add("device_model=${Build.MODEL}")
        add("android_version=${Build.VERSION.RELEASE}")
        add("display_unit=$displayedUnit")
        add("sample_count=${points.size}")
        add("data_representation=${points.firstOrNull()?.dataRepresentation ?: "RAW"}")
        add("trim_start_ms=$trimStartMs")
        add("last_checkpoint_saved_at=${formatAuditTimestamp(lastCheckpointSavedAt)}")
        add("selected_series=${selectedSeries.name}")
        add("graph_x_min=${exportGraphLimit(graphViewport.xMin)}")
        add("graph_x_max=${exportGraphLimit(graphViewport.xMax)}")
        add("graph_y_min=${exportGraphLimit(graphViewport.yMin)}")
        add("graph_y_max=${exportGraphLimit(graphViewport.yMax)}")
        add("compare_track_a=$showTrackA")
        add("compare_track_b=$showTrackB")
        add("compare_delta=$showDelta")
        add("profile_mode=${profileResult?.mode?.name ?: "NONE"}")
        add("profile_equation=${profileResult?.equation ?: "none"}")
        add("profile_rmse=${profileResult?.rmse?.let { "%.6f".format(it) } ?: "n/a"}")
        add("profile_r_squared=${profileResult?.rSquared?.let { "%.6f".format(it) } ?: "n/a"}")
        add("first_sample_time_s=${points.firstOrNull()?.let { formatSeconds(it.timestampUs) } ?: "n/a"}")
        add("last_sample_time_s=${points.lastOrNull()?.let { formatSeconds(it.timestampUs) } ?: "n/a"}")
        add("frame_span=${points.firstOrNull()?.frameNumber?.let { start -> points.lastOrNull()?.frameNumber?.minus(start) ?: 0 } ?: 0}")
        add("photogate_a_time_s=${photogateSummary?.gateATimestampUs?.let(::formatSeconds) ?: "n/a"}")
        add("photogate_a_frame=${photogateSummary?.gateAFrame ?: "n/a"}")
        add("photogate_b_time_s=${photogateSummary?.gateBTimestampUs?.let(::formatSeconds) ?: "n/a"}")
        add("photogate_b_frame=${photogateSummary?.gateBFrame ?: "n/a"}")
        add("photogate_delta_t_s=${photogateSummary?.deltaUs?.let(::formatDeltaSeconds) ?: "n/a"}")
        add("two_body_frame_count=${twoBodySummary?.frameCount ?: 0}")
        add("two_body_unit=${twoBodySummary?.unit ?: displayedUnit}")
        add("two_body_current_separation=${twoBodySummary?.currentSeparation?.let(::formatCalibratedValue) ?: "n/a"}")
        add("two_body_min_separation=${twoBodySummary?.minimumSeparation?.let(::formatCalibratedValue) ?: "n/a"}")
        add("two_body_max_separation=${twoBodySummary?.maximumSeparation?.let(::formatCalibratedValue) ?: "n/a"}")
        add("two_body_avg_separation=${twoBodySummary?.averageSeparation?.let(::formatCalibratedValue) ?: "n/a"}")
        add("two_body_current_relative_speed=${twoBodySummary?.currentRelativeSpeed?.let(::formatCalibratedValue) ?: "n/a"}")
        add("two_body_min_relative_speed=${twoBodySummary?.minimumRelativeSpeed?.let(::formatCalibratedValue) ?: "n/a"}")
        add("two_body_max_relative_speed=${twoBodySummary?.maximumRelativeSpeed?.let(::formatCalibratedValue) ?: "n/a"}")
        add("two_body_avg_relative_speed=${twoBodySummary?.averageRelativeSpeed?.let(::formatCalibratedValue) ?: "n/a"}")
        add("track_a_mass_kg=${formatCalibratedValue(twoBodySummary?.trackAMassKg ?: 0.0)}")
        add("track_b_mass_kg=${formatCalibratedValue(twoBodySummary?.trackBMassKg ?: 0.0)}")
        add("track_a_momentum=${twoBodySummary?.trackAMomentum?.let(::formatCalibratedValue) ?: "n/a"}")
        add("track_b_momentum=${twoBodySummary?.trackBMomentum?.let(::formatCalibratedValue) ?: "n/a"}")
        add("total_momentum=${twoBodySummary?.totalMomentum?.let(::formatCalibratedValue) ?: "n/a"}")
        add("relative_momentum=${twoBodySummary?.relativeMomentum?.let(::formatCalibratedValue) ?: "n/a"}")
        add("closest_approach_time_s=${twoBodySummary?.closestApproachTimeUs?.let(::formatSeconds) ?: "n/a"}")
        add("closest_approach_frame=${twoBodySummary?.closestApproachFrame ?: "n/a"}")
        add("closest_approach_separation=${twoBodySummary?.closestApproachSeparation?.let(::formatCalibratedValue) ?: "n/a"}")
        add("closest_approach_relative_speed=${twoBodySummary?.closestApproachRelativeSpeed?.let(::formatCalibratedValue) ?: "n/a"}")
        add("closest_approach_track_a_momentum=${twoBodySummary?.closestApproachMomentumA?.let(::formatCalibratedValue) ?: "n/a"}")
        add("closest_approach_track_b_momentum=${twoBodySummary?.closestApproachMomentumB?.let(::formatCalibratedValue) ?: "n/a"}")
        add("closest_approach_total_momentum=${twoBodySummary?.closestApproachTotalMomentum?.let(::formatCalibratedValue) ?: "n/a"}")
        add("closest_approach_relative_momentum=${twoBodySummary?.closestApproachRelativeMomentum?.let(::formatCalibratedValue) ?: "n/a"}")
        add("interaction_class=${twoBodySummary?.interactionClass ?: "n/a"}")
        add("pre_interaction_time_s=${twoBodySummary?.preInteractionTimeUs?.let(::formatSeconds) ?: "n/a"}")
        add("pre_interaction_frame=${twoBodySummary?.preInteractionFrame ?: "n/a"}")
        add("pre_interaction_separation=${twoBodySummary?.preInteractionSeparation?.let(::formatCalibratedValue) ?: "n/a"}")
        add("pre_interaction_relative_speed=${twoBodySummary?.preInteractionRelativeSpeed?.let(::formatCalibratedValue) ?: "n/a"}")
        add("pre_interaction_track_a_momentum=${twoBodySummary?.preInteractionMomentumA?.let(::formatCalibratedValue) ?: "n/a"}")
        add("pre_interaction_track_b_momentum=${twoBodySummary?.preInteractionMomentumB?.let(::formatCalibratedValue) ?: "n/a"}")
        add("pre_interaction_total_momentum=${twoBodySummary?.preInteractionTotalMomentum?.let(::formatCalibratedValue) ?: "n/a"}")
        add("pre_interaction_relative_momentum=${twoBodySummary?.preInteractionRelativeMomentum?.let(::formatCalibratedValue) ?: "n/a"}")
        add("pre_interaction_delta_s=${twoBodySummary?.preInteractionDeltaUs?.let(::formatDeltaSeconds) ?: "n/a"}")
        add("post_interaction_time_s=${twoBodySummary?.postInteractionTimeUs?.let(::formatSeconds) ?: "n/a"}")
        add("post_interaction_frame=${twoBodySummary?.postInteractionFrame ?: "n/a"}")
        add("post_interaction_separation=${twoBodySummary?.postInteractionSeparation?.let(::formatCalibratedValue) ?: "n/a"}")
        add("post_interaction_relative_speed=${twoBodySummary?.postInteractionRelativeSpeed?.let(::formatCalibratedValue) ?: "n/a"}")
        add("post_interaction_track_a_momentum=${twoBodySummary?.postInteractionMomentumA?.let(::formatCalibratedValue) ?: "n/a"}")
        add("post_interaction_track_b_momentum=${twoBodySummary?.postInteractionMomentumB?.let(::formatCalibratedValue) ?: "n/a"}")
        add("post_interaction_total_momentum=${twoBodySummary?.postInteractionTotalMomentum?.let(::formatCalibratedValue) ?: "n/a"}")
        add("post_interaction_relative_momentum=${twoBodySummary?.postInteractionRelativeMomentum?.let(::formatCalibratedValue) ?: "n/a"}")
        add("post_interaction_delta_s=${twoBodySummary?.postInteractionDeltaUs?.let(::formatDeltaSeconds) ?: "n/a"}")
        add("gate_a_to_closest_s=${twoBodySummary?.gateAToClosestUs?.let(::formatDeltaSeconds) ?: "n/a"}")
        add("closest_to_gate_b_s=${twoBodySummary?.closestToGateBUs?.let(::formatDeltaSeconds) ?: "n/a"}")
        add("gate_a_to_gate_b_s=${twoBodySummary?.gateAToGateBUs?.let(::formatDeltaSeconds) ?: "n/a"}")
        add("momentum_unit=${twoBodySummary?.momentumUnit ?: "${displayedUnit}·kg/s"}")
        if (calibration != null) {
            add("calibration_distance=${displayCalibrationDistance(calibration)} ${calibration.calibrationUnit}")
            add("calibration_rotation_deg=${"%.4f".format(calibration.rotationDegrees)}")
            add("calibration_origin=${"%.3f".format(calibration.originX)},${"%.3f".format(calibration.originY)}")
            add("calibration_stick_p1=${"%.3f".format(calibration.stickP1x)},${"%.3f".format(calibration.stickP1y)}")
            add("calibration_stick_p2=${"%.3f".format(calibration.stickP2x)},${"%.3f".format(calibration.stickP2y)}")
        } else {
            add("calibration=none")
        }
    }
    val content = lines.joinToString("\n", postfix = "\n")
    return AuditLogPayload(content = content, checksum = sha256(content))
}

private fun buildExcelXml(
    projectName: String,
    displayedUnit: String,
    points: List<DisplayKinematicsPoint>,
    photogateSummary: ExportPhotogateSummary?,
    twoBodySummary: ExportTwoBodySummary?,
    lightAnalysis: LightAnalysisExport? = null
): String {
    val summaries = trackSummaries(points)
    val headers = listOf(
        "track",
        "frame",
        "time_s",
        "tracker_id",
        "data_representation",
        "observation_kind",
        "source_algorithm",
        "confidence",
        "requested_timestamp_us",
        "actual_timestamp_us",
        "coordinate_space",
        "ambiguity",
        "residual_error_px",
        "manual_correction",
        "gap",
        "x_$displayedUnit",
        "y_$displayedUnit",
        "vx_${displayedUnit}_per_s",
        "vy_${displayedUnit}_per_s",
        "v_${displayedUnit}_per_s",
        "ax_${displayedUnit}_per_s2",
        "ay_${displayedUnit}_per_s2",
        "a_${displayedUnit}_per_s2",
        "gate_a_time_s",
        "gate_b_time_s",
        "gate_delta_t_s",
        "gate_a_frame",
        "gate_b_frame",
        "two_body_current_separation",
        "two_body_min_separation",
        "two_body_max_separation",
        "two_body_avg_separation",
        "two_body_current_relative_speed",
        "two_body_min_relative_speed",
        "two_body_max_relative_speed",
        "two_body_avg_relative_speed",
        "two_body_frame_count",
        "two_body_unit",
        "track_a_mass_kg",
        "track_b_mass_kg",
        "track_a_momentum",
        "track_b_momentum",
        "total_momentum",
        "relative_momentum",
        "closest_approach_time_s",
        "closest_approach_frame",
        "closest_approach_separation",
        "closest_approach_relative_speed",
        "closest_approach_track_a_momentum",
        "closest_approach_track_b_momentum",
        "closest_approach_total_momentum",
        "closest_approach_relative_momentum",
        "interaction_class",
        "pre_interaction_time_s",
        "pre_interaction_frame",
        "pre_interaction_separation",
        "pre_interaction_relative_speed",
        "pre_interaction_track_a_momentum",
        "pre_interaction_track_b_momentum",
        "pre_interaction_total_momentum",
        "pre_interaction_relative_momentum",
        "pre_interaction_delta_s",
        "post_interaction_time_s",
        "post_interaction_frame",
        "post_interaction_separation",
        "post_interaction_relative_speed",
        "post_interaction_track_a_momentum",
        "post_interaction_track_b_momentum",
        "post_interaction_total_momentum",
        "post_interaction_relative_momentum",
        "post_interaction_delta_s",
        "gate_a_to_closest_s",
        "closest_to_gate_b_s",
        "gate_a_to_gate_b_s",
        "momentum_unit"
    )
    val rows = points.joinToString("") { point ->
        listOf(
            point.trackLabel,
            point.frameNumber.toString(),
            formatSeconds(point.timestampUs),
            point.trackerId,
            point.dataRepresentation,
            point.observationKind,
            point.sourceAlgorithm,
            point.confidence?.let(::formatCalibratedValue) ?: "",
            point.requestedTimestampUs?.toString() ?: "",
            point.actualTimestampUs?.toString() ?: "",
            point.coordinateSpace,
            point.ambiguity?.let(::formatCalibratedValue) ?: "",
            point.residualErrorPx?.let(::formatCalibratedValue) ?: "",
            point.manualCorrection.toString(),
            point.gapLabel ?: "",
            formatCalibratedValue(point.xValue),
            formatCalibratedValue(point.yValue),
            formatOptionalCalibratedValue(point.vxValue),
            formatOptionalCalibratedValue(point.vyValue),
            formatOptionalCalibratedValue(point.speedValue),
            formatOptionalCalibratedValue(point.axValue),
            formatOptionalCalibratedValue(point.ayValue),
            formatOptionalCalibratedValue(point.accelerationValue),
            photogateSummary?.gateATimestampUs?.let(::formatSeconds) ?: "",
            photogateSummary?.gateBTimestampUs?.let(::formatSeconds) ?: "",
            photogateSummary?.deltaUs?.let(::formatDeltaSeconds) ?: "",
            photogateSummary?.gateAFrame?.toString() ?: "",
            photogateSummary?.gateBFrame?.toString() ?: "",
            twoBodySummary?.currentSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.minimumSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.maximumSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.averageSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.currentRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.minimumRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.maximumRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.averageRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.frameCount?.toString() ?: "",
            twoBodySummary?.unit ?: "",
            twoBodySummary?.trackAMassKg?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.trackBMassKg?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.trackAMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.trackBMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.totalMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.relativeMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachTimeUs?.let(::formatSeconds) ?: "",
            twoBodySummary?.closestApproachFrame?.toString() ?: "",
            twoBodySummary?.closestApproachSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachMomentumA?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachMomentumB?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachTotalMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.closestApproachRelativeMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.interactionClass ?: "",
            twoBodySummary?.preInteractionTimeUs?.let(::formatSeconds) ?: "",
            twoBodySummary?.preInteractionFrame?.toString() ?: "",
            twoBodySummary?.preInteractionSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionMomentumA?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionMomentumB?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionTotalMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionRelativeMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.preInteractionDeltaUs?.let(::formatDeltaSeconds) ?: "",
            twoBodySummary?.postInteractionTimeUs?.let(::formatSeconds) ?: "",
            twoBodySummary?.postInteractionFrame?.toString() ?: "",
            twoBodySummary?.postInteractionSeparation?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionRelativeSpeed?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionMomentumA?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionMomentumB?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionTotalMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionRelativeMomentum?.let(::formatCalibratedValue) ?: "",
            twoBodySummary?.postInteractionDeltaUs?.let(::formatDeltaSeconds) ?: "",
            twoBodySummary?.gateAToClosestUs?.let(::formatDeltaSeconds) ?: "",
            twoBodySummary?.closestToGateBUs?.let(::formatDeltaSeconds) ?: "",
            twoBodySummary?.gateAToGateBUs?.let(::formatDeltaSeconds) ?: "",
            twoBodySummary?.momentumUnit ?: ""
        ).joinToString("") { value ->
            "<Cell><Data ss:Type=\"String\">${escapeXml(value)}</Data></Cell>"
        }.let { "<Row>$it</Row>" }
    }
    val headerRow = headers.joinToString("") { "<Cell><Data ss:Type=\"String\">$it</Data></Cell>" }
    val summaryRows = buildList {
        add(
            listOf("Project", projectName).joinToString("") { value ->
                "<Cell><Data ss:Type=\"String\">${escapeXml(value)}</Data></Cell>"
            }.let { "<Row>$it</Row>" }
        )
        add(
            listOf("Tracks exported", summaries.joinToString(", ") { it.trackLabel.ifBlank { "A" } }).joinToString("") { value ->
                "<Cell><Data ss:Type=\"String\">${escapeXml(value)}</Data></Cell>"
            }.let { "<Row>$it</Row>" }
        )
        add(
            listOf("Total samples", points.size.toString()).joinToString("") { value ->
                "<Cell><Data ss:Type=\"String\">${escapeXml(value)}</Data></Cell>"
            }.let { "<Row>$it</Row>" }
        )
        summaries.forEach { summary ->
            add(
                listOf(
                    "Track ${summary.trackLabel}",
                    "${summary.sampleCount} samples, frames ${summary.frameStart ?: "-"} to ${summary.frameEnd ?: "-"}, avg speed ${summary.avgSpeed?.let(::formatCalibratedValue) ?: "-"}"
                ).joinToString("") { value ->
                    "<Cell><Data ss:Type=\"String\">${escapeXml(value)}</Data></Cell>"
                }.let { "<Row>$it</Row>" }
            )
        }
        twoBodySummary?.takeIf { it.frameCount > 0 }?.let {
            add(
                listOf(
                    "Two-track analysis",
                    "Separation ${it.minimumSeparation?.let(::formatCalibratedValue) ?: "-"} to ${it.maximumSeparation?.let(::formatCalibratedValue) ?: "-"} ${it.unit}, relative speed ${it.minimumRelativeSpeed?.let(::formatCalibratedValue) ?: "-"} to ${it.maximumRelativeSpeed?.let(::formatCalibratedValue) ?: "-"} ${it.unit}/s"
                ).joinToString("") { value ->
                    "<Cell><Data ss:Type=\"String\">${escapeXml(value)}</Data></Cell>"
                }.let { "<Row>$it</Row>" }
            )
        }
    }.joinToString("")
    val lightWorksheet = lightAnalysis?.let { light ->
        val rows = buildList {
            add(listOf("Mode", light.modeLabel))
            add(listOf("Channel", light.channelLabel))
            add(listOf("Geometry", light.geometrySummary))
            light.statsLines.forEach { add(listOf("Stat", it)) }
            add(listOf("Table", "axis,intensity"))
            light.tableRows.forEach { row -> add(listOf("Row", row)) }
        }.joinToString("") { values ->
            values.joinToString("") { value ->
                "<Cell><Data ss:Type=\"String\">${escapeXml(value)}</Data></Cell>"
            }.let { "<Row>$it</Row>" }
        }
        """
          <Worksheet ss:Name="Light">
            <Table>
              $rows
            </Table>
          </Worksheet>
        """.trimIndent()
    } ?: ""
    return """
        <?xml version="1.0"?>
        <?mso-application progid="Excel.Sheet"?>
        <Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
         xmlns:o="urn:schemas-microsoft-com:office:office"
         xmlns:x="urn:schemas-microsoft-com:office:excel"
         xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
          <Worksheet ss:Name="Summary">
            <Table>
              $summaryRows
            </Table>
          </Worksheet>
          <Worksheet ss:Name="${sanitizeFileName(projectName).take(24)}_data">
            <Table>
              <Row>$headerRow</Row>
              $rows
            </Table>
          </Worksheet>
          $lightWorksheet
        </Workbook>
    """.trimIndent()
}

private fun appendLightCsvSection(base: String, lightAnalysis: LightAnalysisExport?): String {
    lightAnalysis ?: return base
    val header = buildString {
        appendLine()
        appendLine("light_mode,${lightAnalysis.modeLabel}")
        appendLine("light_channel,${lightAnalysis.channelLabel}")
        appendLine("light_geometry,\"${lightAnalysis.geometrySummary}\"")
        appendLine("light_series_axis,light_series_intensity")
        lightAnalysis.tableRows.forEach { appendLine(it) }
    }
    return base.trimEnd() + "\n" + header
}

private fun appendLightPages(
    document: PdfDocument,
    lightAnalysis: LightAnalysisExport
) {
    val titlePaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val bodyPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 12f
        isAntiAlias = true
    }
    val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, 80).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    canvas.drawColor(AndroidColor.rgb(18, 18, 18))
    drawWatermark(canvas, pageInfo.pageWidth, pageInfo.pageHeight)
    var y = 50f
    canvas.drawText("Light Analysis", 40f, y, titlePaint)
    y += 28f
    canvas.drawText("Mode: ${lightAnalysis.modeLabel}", 40f, y, bodyPaint)
    y += 20f
    canvas.drawText("Channel: ${lightAnalysis.channelLabel}", 40f, y, bodyPaint)
    y += 20f
    y = drawWrappedLines(canvas, bodyPaint, lightAnalysis.geometrySummary, 40f, y, 1120f)
    y += 16f
    val lightXAxis = if (lightAnalysis.modeLabel.contains("Time", ignoreCase = true)) {
        "Time (s)"
    } else {
        "Position / distance (${lightAnalysis.unitLabel})"
    }
    val lightLabels = ExportChartLabels(
        title = "Light intensity: ${lightAnalysis.modeLabel}",
        xAxis = lightXAxis,
        yAxis = "Intensity (0–255, ${lightAnalysis.channelLabel})"
    )
    if (lightAnalysis.focusedSeries.size >= 2) {
        drawSeriesChart(canvas, bodyPaint, 40f, y + 16f, 1120f, 300f, lightAnalysis.focusedSeries, AndroidColor.CYAN, lightLabels)
        y += 340f
    }
    if (lightAnalysis.compareSeries.any { it.samples.size >= 2 }) {
        drawMultiSeriesChart(canvas, bodyPaint, 40f, y + 16f, 1120f, 260f, lightAnalysis.compareSeries.map {
            ExportChartSeries(it.label, it.samples, it.color)
        }, lightLabels.copy(title = "Light intensity comparison: ${lightAnalysis.modeLabel}"))
        y += 300f
    }
    canvas.drawText("Stats", 40f, y, titlePaint)
    y += 24f
    lightAnalysis.statsLines.forEach { line ->
        canvas.drawText(line.take(130), 54f, y, bodyPaint)
        y += 18f
    }
    document.finishPage(page)
}

private fun drawProfileSummary(
    canvas: android.graphics.Canvas,
    paint: Paint,
    y: Float,
    title: String,
    result: ProfileResult?
): Float {
    var cursor = y
    canvas.drawText(title, 40f, cursor, paint)
    cursor += 24f
    if (result == null) {
        canvas.drawText("No profile available", 54f, cursor, paint)
        return cursor + 24f
    }
    canvas.drawText("${result.label} fit", 54f, cursor, paint)
    cursor += 20f
    canvas.drawText(result.equation.take(90), 54f, cursor, paint)
    cursor += 20f
    canvas.drawText("RMSE ${"%.4f".format(result.rmse)}   R² ${"%.4f".format(result.rSquared)}", 54f, cursor, paint)
    cursor += 20f
    result.parameters.take(4).forEach { (name, value) ->
        canvas.drawText("$name = ${"%.4f".format(value)}", 72f, cursor, paint)
        cursor += 18f
    }
    return cursor + 16f
}

private fun drawWrappedLines(
    canvas: android.graphics.Canvas,
    paint: Paint,
    text: String,
    left: Float,
    top: Float,
    maxWidth: Float,
    lineHeight: Float = 18f
): Float {
    if (text.isBlank()) return top
    val words = text.split(Regex("\\s+"))
    var line = ""
    var y = top
    words.forEach { word ->
        val candidate = if (line.isBlank()) word else "$line $word"
        if (paint.measureText(candidate) <= maxWidth || line.isBlank()) {
            line = candidate
        } else {
            canvas.drawText(line, left, y, paint)
            y += lineHeight
            line = word
        }
    }
    if (line.isNotBlank()) {
        canvas.drawText(line, left, y, paint)
        y += lineHeight
    }
    return y
}

private fun drawTrackSummaryBlock(
    canvas: android.graphics.Canvas,
    titlePaint: Paint,
    bodyPaint: Paint,
    left: Float,
    top: Float,
    width: Float,
    summary: ExportTrackSummary,
    displayedUnit: String
): Float {
    val cardPaint = Paint().apply {
        color = AndroidColor.argb(46, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val bottom = top + 124f
    canvas.drawRoundRect(left, top, left + width, bottom, 18f, 18f, cardPaint)
    var y = top + 24f
    canvas.drawText("Track ${summary.trackLabel}", left + 16f, y, titlePaint)
    y += 22f
    canvas.drawText("Samples ${summary.sampleCount}   Frames ${summary.frameStart ?: "-"} to ${summary.frameEnd ?: "-"}", left + 16f, y, bodyPaint)
    y += 18f
    canvas.drawText("Time ${summary.timeStartUs?.let(::formatSeconds) ?: "-"} s to ${summary.timeEndUs?.let(::formatSeconds) ?: "-"} s", left + 16f, y, bodyPaint)
    y += 18f
    canvas.drawText("X ${summary.minX?.let(::formatCalibratedValue) ?: "-"} to ${summary.maxX?.let(::formatCalibratedValue) ?: "-"} $displayedUnit", left + 16f, y, bodyPaint)
    y += 18f
    canvas.drawText("Y ${summary.minY?.let(::formatCalibratedValue) ?: "-"} to ${summary.maxY?.let(::formatCalibratedValue) ?: "-"} $displayedUnit", left + 16f, y, bodyPaint)
    y += 18f
    canvas.drawText("Speed avg ${summary.avgSpeed?.let(::formatCalibratedValue) ?: "-"}   max ${summary.maxSpeed?.let(::formatCalibratedValue) ?: "-"}", left + 16f, y, bodyPaint)
    return bottom + 12f
}

private fun drawSeriesChart(
    canvas: android.graphics.Canvas,
    paint: Paint,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    samples: List<GraphSample>,
    color: Int,
    labels: ExportChartLabels
) {
    if (samples.size < 2) return
    val minX = samples.minOf { it.timeSeconds }
    val maxX = samples.maxOf { it.timeSeconds }
    val minY = samples.minOf { it.value }
    val maxY = samples.maxOf { it.value }
    val dx = max(maxX - minX, 1e-6)
    val dy = max(maxY - minY, 1e-6)
    val plotLeft = left + 72f
    val plotTop = top + 34f
    val plotRight = left + width - 18f
    val plotBottom = top + height - 42f
    val plotWidth = plotRight - plotLeft
    val plotHeight = plotBottom - plotTop
    val axis = Paint(paint).apply { this.color = AndroidColor.argb(100, 255, 255, 255) }
    val labelPaint = Paint(paint).apply {
        this.color = AndroidColor.WHITE
        textSize = 14f
        isFakeBoldText = true
    }
    val tickPaint = Paint(paint).apply {
        this.color = AndroidColor.LTGRAY
        textSize = 11f
    }
    canvas.drawText(labels.title, left, top + 16f, labelPaint)
    canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, axis)
    canvas.drawLine(plotLeft, plotTop, plotLeft, plotBottom, axis)
    canvas.drawText(formatCalibratedValue(minX), plotLeft, plotBottom + 16f, tickPaint)
    canvas.drawText(formatCalibratedValue(maxX), plotRight - tickPaint.measureText(formatCalibratedValue(maxX)), plotBottom + 16f, tickPaint)
    canvas.drawText(formatCalibratedValue(maxY), 4f + left, plotTop + 4f, tickPaint)
    canvas.drawText(formatCalibratedValue(minY), 4f + left, plotBottom, tickPaint)
    canvas.drawText(labels.xAxis, plotLeft + (plotWidth - labelPaint.measureText(labels.xAxis)) / 2f, top + height - 4f, labelPaint)
    canvas.save()
    canvas.rotate(-90f, left + 16f, plotTop + plotHeight / 2f)
    canvas.drawText(labels.yAxis, left + 16f - labelPaint.measureText(labels.yAxis) / 2f, plotTop + plotHeight / 2f, labelPaint)
    canvas.restore()
    val linePaint = Paint(paint).apply {
        this.color = color
        strokeWidth = 3f
        style = android.graphics.Paint.Style.STROKE
        isAntiAlias = true
    }
    val path = android.graphics.Path()
    samples.forEachIndexed { index, sample ->
        val x = plotLeft + (((sample.timeSeconds - minX) / dx) * plotWidth).toFloat()
        val y = plotBottom - (((sample.value - minY) / dy) * plotHeight).toFloat()
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    canvas.drawPath(path, linePaint)
}

private fun drawMultiSeriesChart(
    canvas: android.graphics.Canvas,
    paint: Paint,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    series: List<ExportChartSeries>,
    labels: ExportChartLabels
) {
    val nonEmpty = series.filter { it.samples.size >= 2 }
    if (nonEmpty.isEmpty()) return
    val allSamples = nonEmpty.flatMap { it.samples }
    val minX = allSamples.minOf { it.timeSeconds }
    val maxX = allSamples.maxOf { it.timeSeconds }
    val minY = allSamples.minOf { it.value }
    val maxY = allSamples.maxOf { it.value }
    val dx = max(maxX - minX, 1e-6)
    val dy = max(maxY - minY, 1e-6)
    val plotLeft = left + 72f
    val plotTop = top + 34f
    val plotRight = left + width - 18f
    val plotBottom = top + height - 42f
    val plotWidth = plotRight - plotLeft
    val plotHeight = plotBottom - plotTop
    val axis = Paint(paint).apply { color = AndroidColor.argb(100, 255, 255, 255) }
    val labelPaint = Paint(paint).apply {
        color = AndroidColor.WHITE
        textSize = 14f
        isFakeBoldText = true
    }
    val tickPaint = Paint(paint).apply { color = AndroidColor.LTGRAY; textSize = 11f }
    canvas.drawText(labels.title, left, top + 16f, labelPaint)
    canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, axis)
    canvas.drawLine(plotLeft, plotTop, plotLeft, plotBottom, axis)
    canvas.drawText(formatCalibratedValue(minX), plotLeft, plotBottom + 16f, tickPaint)
    canvas.drawText(formatCalibratedValue(maxX), plotRight - tickPaint.measureText(formatCalibratedValue(maxX)), plotBottom + 16f, tickPaint)
    canvas.drawText(formatCalibratedValue(maxY), 4f + left, plotTop + 4f, tickPaint)
    canvas.drawText(formatCalibratedValue(minY), 4f + left, plotBottom, tickPaint)
    canvas.drawText(labels.xAxis, plotLeft + (plotWidth - labelPaint.measureText(labels.xAxis)) / 2f, top + height - 4f, labelPaint)
    canvas.save()
    canvas.rotate(-90f, left + 16f, plotTop + plotHeight / 2f)
    canvas.drawText(labels.yAxis, left + 16f - labelPaint.measureText(labels.yAxis) / 2f, plotTop + plotHeight / 2f, labelPaint)
    canvas.restore()
    nonEmpty.forEachIndexed { index, chart ->
        val linePaint = Paint(paint).apply {
            color = chart.color
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val path = android.graphics.Path()
        chart.samples.forEachIndexed { sampleIndex, sample ->
            val x = plotLeft + (((sample.timeSeconds - minX) / dx) * plotWidth).toFloat()
            val y = plotBottom - (((sample.value - minY) / dy) * plotHeight).toFloat()
            if (sampleIndex == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, linePaint)
        val legendPaint = Paint(paint).apply {
            color = chart.color
            textSize = 14f
            isFakeBoldText = true
        }
        canvas.drawText(chart.label, plotLeft + index * 150f, plotTop + 16f, legendPaint)
    }
}

private fun drawEventTimeline(
    canvas: android.graphics.Canvas,
    left: Float,
    top: Float,
    width: Float,
    events: List<Pair<String, Long>>,
    currentLabel: String? = null
): Float {
    if (events.isEmpty()) return top
    val axisPaint = Paint().apply {
        color = AndroidColor.argb(110, 255, 255, 255)
        strokeWidth = 2f
        isAntiAlias = true
    }
    val labelPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 14f
        isAntiAlias = true
    }
    val highlightPaint = Paint().apply {
        color = AndroidColor.argb(215, 255, 241, 118)
        textSize = 14f
        isAntiAlias = true
        isFakeBoldText = true
    }
    val markerPaint = Paint().apply {
        strokeWidth = 2f
        isAntiAlias = true
    }
    val radius = 7f
    val centerY = top + 20f
    val spanStartUs = events.minOf { it.second }
    val spanEndUs = events.maxOf { it.second }
    val spanUs = (spanEndUs - spanStartUs).coerceAtLeast(1L).toFloat()
    canvas.drawLine(left, centerY, left + width, centerY, axisPaint)
    events.forEach { (label, timestampUs) ->
        val fraction = ((timestampUs - spanStartUs) / spanUs).coerceIn(0f, 1f)
        val x = left + width * fraction
        markerPaint.color = when (label) {
            "Gate A" -> AndroidColor.rgb(129, 199, 132)
            "Gate B" -> AndroidColor.rgb(255, 183, 77)
            else -> AndroidColor.rgb(255, 241, 118)
        }
        canvas.drawLine(x, centerY - 12f, x, centerY + 12f, markerPaint)
        canvas.drawCircle(x, centerY, radius, markerPaint)
        val paint = if (label == currentLabel) highlightPaint else labelPaint
        canvas.drawText(label, (x - 22f).coerceAtLeast(left), centerY - 18f, paint)
        canvas.drawText("${formatSeconds(timestampUs)}s", (x - 22f).coerceAtLeast(left), centerY + 28f, paint)
    }
    return top + 48f
}

private fun drawWatermark(
    canvas: android.graphics.Canvas,
    width: Int,
    height: Int
) {
    val watermarkPaint = Paint().apply {
        color = AndroidColor.argb(30, 255, 255, 255)
        textSize = 72f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.save()
    canvas.rotate(-24f, width / 2f, height / 2f)
    canvas.drawText("PhyZX Verified Export", width / 2f, height / 2f, watermarkPaint)
    canvas.restore()
}

private fun appendAuditPages(
    document: PdfDocument,
    auditLogContent: String,
    auditChecksum: String
) {
    val lines = buildList {
        add("Measurement Audit Appendix")
        add("SHA-256: $auditChecksum")
        add("")
        addAll(auditLogContent.trim().lines())
    }
    val pageWidth = 1240
    val pageHeight = 1754
    val maxLinesPerPage = 78
    val bodyPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 12f
        isAntiAlias = true
    }
    val titlePaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }
    lines.chunked(maxLinesPerPage).forEachIndexed { index, chunk ->
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 2).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(AndroidColor.rgb(18, 18, 18))
        drawWatermark(canvas, pageWidth, pageHeight)
        var y = 50f
        canvas.drawText("PhyZX Experiment Report", 40f, y, titlePaint)
        y += 30f
        canvas.drawText("Audit Appendix ${index + 1}", 40f, y, bodyPaint)
        y += 28f
        chunk.forEach { line ->
            canvas.drawText(line.take(145), 40f, y, bodyPaint)
            y += 20f
        }
        document.finishPage(page)
    }
}

private fun appendDataPages(
    document: PdfDocument,
    projectName: String,
    displayedUnit: String,
    points: List<DisplayKinematicsPoint>
) {
    val bodyPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 12f
        isAntiAlias = true
    }
    val titlePaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val headers = "Track   Frame   Time(s)   X($displayedUnit)   Y($displayedUnit)   Speed   Kind/Confidence"
    points.chunked(60).forEachIndexed { index, chunk ->
        val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, 100 + index).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(AndroidColor.rgb(18, 18, 18))
        drawWatermark(canvas, pageInfo.pageWidth, pageInfo.pageHeight)
        var y = 50f
        canvas.drawText("PhyZX Data Appendix", 40f, y, titlePaint)
        y += 28f
        canvas.drawText(projectName, 40f, y, bodyPaint)
        y += 24f
        canvas.drawText("Detailed table page ${index + 1}", 40f, y, bodyPaint)
        y += 24f
        canvas.drawText(headers, 40f, y, bodyPaint)
        y += 18f
        chunk.forEach { point ->
            val row = buildString {
                append(point.trackLabel.padEnd(5))
                append(" ")
                append(point.frameNumber.toString().padEnd(7))
                append(" ")
                append(formatSeconds(point.timestampUs).padEnd(9))
                append(" ")
                append(formatCalibratedValue(point.xValue).padEnd(10))
                append(" ")
                append(formatCalibratedValue(point.yValue).padEnd(10))
                append(" ")
                append(formatOptionalCalibratedValue(point.speedValue).padEnd(8))
                append(" ")
                append(point.observationKind.take(6))
                append("/")
                append(point.confidence?.let { String.format(Locale.US, "%.2f", it) } ?: "--")
            }
            canvas.drawText(row.take(130), 40f, y, bodyPaint)
            y += 18f
        }
        document.finishPage(page)
    }
}

private fun appendGraphPages(
    document: PdfDocument,
    projectName: String,
    displayedUnit: String,
    selectedSeries: GraphSeries,
    activeViewLabel: String,
    focusedCharts: List<ExportChartSeries>,
    combinedCharts: List<ExportChartSeries>,
    appendixCharts: List<ExportChartSeries>,
    profileResult: ProfileResult?
) {
    val titlePaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val bodyPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 12f
        isAntiAlias = true
    }
    val focusedInfo = PdfDocument.PageInfo.Builder(1240, 1754, 40).create()
    val focusedPage = document.startPage(focusedInfo)
    val focusedCanvas = focusedPage.canvas
    focusedCanvas.drawColor(AndroidColor.rgb(18, 18, 18))
    drawWatermark(focusedCanvas, focusedInfo.pageWidth, focusedInfo.pageHeight)
    var y = 50f
    focusedCanvas.drawText("Graph Export View", 40f, y, titlePaint)
    y += 28f
    focusedCanvas.drawText(projectName, 40f, y, bodyPaint)
    y += 22f
    focusedCanvas.drawText("Current view: $activeViewLabel", 40f, y, bodyPaint)
    y += 18f
    focusedCanvas.drawText("Selected series: ${graphSeriesLabel(selectedSeries, displayedUnit)}", 40f, y, bodyPaint)
    y += 28f
    drawMultiSeriesChart(
        focusedCanvas, bodyPaint, 50f, y + 20f, 1120f, 360f, focusedCharts,
        exportGraphLabels(selectedSeries, displayedUnit, "Focused motion graph")
    )
    y += 420f
    profileResult?.let {
        y = drawProfileSummary(focusedCanvas, bodyPaint, y, "Focused Graph Fit", it)
    }
    document.finishPage(focusedPage)

    if (combinedCharts.count { it.samples.size >= 2 } >= 2) {
        val combinedInfo = PdfDocument.PageInfo.Builder(1240, 1754, 41).create()
        val combinedPage = document.startPage(combinedInfo)
        val combinedCanvas = combinedPage.canvas
        combinedCanvas.drawColor(AndroidColor.rgb(18, 18, 18))
        drawWatermark(combinedCanvas, combinedInfo.pageWidth, combinedInfo.pageHeight)
        var cy = 50f
        combinedCanvas.drawText("Combined Track Graph", 40f, cy, titlePaint)
        cy += 28f
        combinedCanvas.drawText("This page overlays the currently selected series for the visible tracks exactly as a combined comparison view.", 40f, cy, bodyPaint)
        cy += 26f
        combinedCanvas.drawText("${graphSeriesLabel(selectedSeries, displayedUnit)}   $activeViewLabel", 40f, cy, bodyPaint)
        cy += 32f
        drawMultiSeriesChart(
            combinedCanvas, bodyPaint, 50f, cy + 24f, 1120f, 430f, combinedCharts,
            exportGraphLabels(selectedSeries, displayedUnit, "Track comparison")
        )
        cy += 500f
        profileResult?.let {
            cy = drawProfileSummary(combinedCanvas, bodyPaint, cy, "Comparison Fit Context", it)
        }
        document.finishPage(combinedPage)
    }

    if (appendixCharts.any { it.samples.size >= 2 }) {
        val appendixInfo = PdfDocument.PageInfo.Builder(1240, 1754, 42).create()
        val appendixPage = document.startPage(appendixInfo)
        val appendixCanvas = appendixPage.canvas
        appendixCanvas.drawColor(AndroidColor.rgb(18, 18, 18))
        drawWatermark(appendixCanvas, appendixInfo.pageWidth, appendixInfo.pageHeight)
        var ay = 50f
        appendixCanvas.drawText("Graph Appendix", 40f, ay, titlePaint)
        ay += 28f
        appendixCanvas.drawText("Separate and combined plots for export reference", 40f, ay, bodyPaint)
        ay += 30f
        appendixCharts.chunked(2).forEach { rowCharts ->
            rowCharts.forEachIndexed { index, chart ->
                val left = if (index == 0) 50f else 625f
                appendixCanvas.drawText(chart.label, left, ay, bodyPaint)
                drawMultiSeriesChart(
                    appendixCanvas, bodyPaint, left, ay + 28f, 520f, 250f, listOf(chart),
                    exportGraphLabels(selectedSeries, displayedUnit, "${chart.label} motion graph")
                )
            }
            ay += 330f
        }
        document.finishPage(appendixPage)
    }
}

private fun buildPdf(
    context: Context,
    projectName: String,
    displayedUnit: String,
    points: List<DisplayKinematicsPoint>,
    trackAPoints: List<DisplayKinematicsPoint>,
    trackBPoints: List<DisplayKinematicsPoint>,
    calibration: CalibrationEntity?,
    selectedSeries: GraphSeries,
    graphViewport: GraphViewportSummary,
    showTrackA: Boolean,
    showTrackB: Boolean,
    showDelta: Boolean,
    activeViewLabel: String,
    trackAColor: Int,
    trackBColor: Int,
    profileResult: ProfileResult?,
    photogateSummary: ExportPhotogateSummary?,
    twoBodySummary: ExportTwoBodySummary?,
    lightAnalysis: LightAnalysisExport?,
    auditChecksum: String,
    auditLogContent: String
): PdfDocument {
    val document = PdfDocument()
    val summaries = trackSummaries(points)
    val narrative = buildAnalysisNarrative(points, selectedSeries, displayedUnit, profileResult, twoBodySummary)
    val titlePaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val bodyPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 12f
        isAntiAlias = true
    }
    val sectionPaint = Paint(bodyPaint).apply {
        textSize = 16f
        isFakeBoldText = true
    }

    val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    canvas.drawColor(AndroidColor.rgb(18, 18, 18))
    drawWatermark(canvas, pageInfo.pageWidth, pageInfo.pageHeight)
    val appVersion = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }.getOrDefault("unknown")
    var y = 50f
    canvas.drawText("PhyZX Experiment Report", 40f, y, titlePaint)
    y += 30f
    canvas.drawText("Project: $projectName", 40f, y, bodyPaint)
    y += 20f
    canvas.drawText("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}", 40f, y, bodyPaint)
    y += 20f
    canvas.drawText("Samples: ${points.size}   Unit: $displayedUnit", 40f, y, bodyPaint)
    y += 20f
    canvas.drawText(
        "Graph window: X ${exportGraphLimit(graphViewport.xMin)} to ${exportGraphLimit(graphViewport.xMax)} s   Y ${exportGraphLimit(graphViewport.yMin)} to ${exportGraphLimit(graphViewport.yMax)}",
        40f,
        y,
        bodyPaint
    )
    y += 20f
    canvas.drawText("Visible tracks: A $showTrackA   B $showTrackB   Δ $showDelta", 40f, y, bodyPaint)
    y += 20f
    canvas.drawText("Device: ${Build.MANUFACTURER} ${Build.MODEL}   Android ${Build.VERSION.RELEASE}", 40f, y, bodyPaint)
    y += 20f
    canvas.drawText("App: PhyZX $appVersion   Export Integrity: Watermarked local report", 40f, y, bodyPaint)
    y += 20f
    canvas.drawText("Audit Log SHA-256: ${auditChecksum.take(24)}...", 40f, y, bodyPaint)
    y += 28f
    canvas.drawText("Executive Summary", 40f, y, titlePaint)
    y += 24f
    narrative.forEach { line ->
        y = drawWrappedLines(canvas, bodyPaint, line, 54f, y, 1120f)
        y += 6f
    }
    y += 8f
    photogateSummary?.let {
        canvas.drawText("Timing", 40f, y, sectionPaint)
        y += 20f
        canvas.drawText(
            "Photogates: A ${it.gateATimestampUs?.let(::formatSeconds) ?: "--"} s   B ${it.gateBTimestampUs?.let(::formatSeconds) ?: "--"} s   Δt ${it.deltaUs?.let(::formatDeltaSeconds) ?: "--"} s",
            40f,
            y,
            bodyPaint
        )
        y += 20f
    }
    if (summaries.isNotEmpty()) {
        canvas.drawText("Track Summaries", 40f, y, titlePaint)
        y += 22f
        summaries.forEachIndexed { index, summary ->
            val left = if (index % 2 == 0) 40f else 635f
            val cardTop = if (index % 2 == 0) y else y - 136f
            drawTrackSummaryBlock(canvas, sectionPaint, bodyPaint, left, cardTop, 565f, summary, displayedUnit)
            if (index % 2 == 1 || index == summaries.lastIndex) {
                y = cardTop + 140f
            }
        }
    }
    twoBodySummary?.takeIf { it.frameCount > 0 }?.let {
        canvas.drawText("Two-Track Analysis", 40f, y, titlePaint)
        y += 22f
        canvas.drawText(
            "Two-body separation: current ${it.currentSeparation?.let(::formatCalibratedValue) ?: "--"} ${it.unit}   avg ${it.averageSeparation?.let(::formatCalibratedValue) ?: "--"} ${it.unit}   frames ${it.frameCount}",
            40f,
            y,
            bodyPaint
        )
        y += 20f
        canvas.drawText(
            "Relative speed: current ${it.currentRelativeSpeed?.let(::formatCalibratedValue) ?: "--"} ${it.unit}/s   avg ${it.averageRelativeSpeed?.let(::formatCalibratedValue) ?: "--"} ${it.unit}/s",
            40f,
            y,
            bodyPaint
        )
        y += 20f
        canvas.drawText(
            "Momentum: pA ${it.trackAMomentum?.let(::formatCalibratedValue) ?: "--"}   pB ${it.trackBMomentum?.let(::formatCalibratedValue) ?: "--"}   total ${it.totalMomentum?.let(::formatCalibratedValue) ?: "--"} ${it.momentumUnit}",
            40f,
            y,
            bodyPaint
        )
        y += 20f
        canvas.drawText(
            "Closest approach: t ${it.closestApproachTimeUs?.let(::formatSeconds) ?: "--"} s   frame ${it.closestApproachFrame ?: "--"}   sep ${it.closestApproachSeparation?.let(::formatCalibratedValue) ?: "--"} ${it.unit}",
            40f,
            y,
            bodyPaint
        )
        y += 20f
        canvas.drawText(
            "Closest momenta: pA ${it.closestApproachMomentumA?.let(::formatCalibratedValue) ?: "--"}   pB ${it.closestApproachMomentumB?.let(::formatCalibratedValue) ?: "--"}   total ${it.closestApproachTotalMomentum?.let(::formatCalibratedValue) ?: "--"} ${it.momentumUnit}",
            40f,
            y,
            bodyPaint
        )
        y += 20f
        canvas.drawText(
            "Interaction state: ${it.interactionClass ?: "--"}",
            40f,
            y,
            bodyPaint
        )
        y += 20f
        canvas.drawText(
            "Before event: t ${it.preInteractionTimeUs?.let(::formatSeconds) ?: "--"} s   sep ${it.preInteractionSeparation?.let(::formatCalibratedValue) ?: "--"} ${it.unit}   rel v ${it.preInteractionRelativeSpeed?.let(::formatCalibratedValue) ?: "--"} ${it.unit}/s",
            40f,
            y,
            bodyPaint
        )
        y += 20f
        canvas.drawText(
            "Before momenta: pA ${it.preInteractionMomentumA?.let(::formatCalibratedValue) ?: "--"}   pB ${it.preInteractionMomentumB?.let(::formatCalibratedValue) ?: "--"}   total ${it.preInteractionTotalMomentum?.let(::formatCalibratedValue) ?: "--"} ${it.momentumUnit}",
            40f,
            y,
            bodyPaint
        )
        y += 20f
        canvas.drawText(
            "After event: t ${it.postInteractionTimeUs?.let(::formatSeconds) ?: "--"} s   sep ${it.postInteractionSeparation?.let(::formatCalibratedValue) ?: "--"} ${it.unit}   rel v ${it.postInteractionRelativeSpeed?.let(::formatCalibratedValue) ?: "--"} ${it.unit}/s",
            40f,
            y,
            bodyPaint
        )
        y += 20f
        canvas.drawText(
            "After momenta: pA ${it.postInteractionMomentumA?.let(::formatCalibratedValue) ?: "--"}   pB ${it.postInteractionMomentumB?.let(::formatCalibratedValue) ?: "--"}   total ${it.postInteractionTotalMomentum?.let(::formatCalibratedValue) ?: "--"} ${it.momentumUnit}",
            40f,
            y,
            bodyPaint
        )
        y += 20f
        canvas.drawText(
            "Event deltas: A->Closest ${it.gateAToClosestUs?.let(::formatDeltaSeconds) ?: "--"} s   Closest->B ${it.closestToGateBUs?.let(::formatDeltaSeconds) ?: "--"} s   A->B ${it.gateAToGateBUs?.let(::formatDeltaSeconds) ?: "--"} s",
            40f,
            y,
            bodyPaint
        )
        y += 20f
    }
    val eventTimeline = buildList {
        photogateSummary?.gateATimestampUs?.let { add("Gate A" to it) }
        photogateSummary?.gateBTimestampUs?.let { add("Gate B" to it) }
        twoBodySummary?.closestApproachTimeUs?.let { add("Closest" to it) }
    }
    if (eventTimeline.isNotEmpty()) {
        canvas.drawText("Event Timeline", 40f, y, titlePaint)
        y += 8f
        y = drawEventTimeline(
            canvas = canvas,
            left = 54f,
            top = y,
            width = 1080f,
            events = eventTimeline,
            currentLabel = if (twoBodySummary?.closestApproachTimeUs != null) "Closest" else null
        )
        y += 18f
        canvas.drawText(
            eventTimeline.joinToString("   ") { "${it.first}: ${formatSeconds(it.second)}s" },
            54f,
            y,
            bodyPaint
        )
        y += 24f
    }
    calibration?.let {
        canvas.drawText("Calibration", 40f, y, sectionPaint)
        y += 20f
        canvas.drawText(
            "Calibration: ${displayCalibrationDistance(it)} ${it.calibrationUnit}   Rotation: ${"%.2f".format(it.rotationDegrees)}°",
            40f,
            y,
            bodyPaint
        )
        y += 20f
    }
    canvas.drawText("Graph Analysis", 40f, y, titlePaint)
    y += 24f
    val selectedSeriesSamples = graphSamplesForSeries(points, selectedSeries)
    val speedSamples = points.mapNotNull { it.speedValue?.let { speed -> GraphSample(it.timestampUs / 1_000_000.0, speed) } }
    drawSeriesChart(
        canvas, bodyPaint, 40f, y + 10f, 520f, 220f, selectedSeriesSamples, AndroidColor.CYAN,
        exportGraphLabels(selectedSeries, displayedUnit, "Selected motion graph")
    )
    drawSeriesChart(
        canvas, bodyPaint, 640f, y + 10f, 520f, 220f, speedSamples, AndroidColor.YELLOW,
        exportGraphLabels(GraphSeries.SPEED, displayedUnit, "Speed graph")
    )
    y += 252f
    canvas.drawText("Selected Graph Series: ${graphSeriesLabel(selectedSeries, displayedUnit)}", 40f, y, bodyPaint)
    y += 24f
    y = drawProfileSummary(canvas, bodyPaint, y, "Profile", profileResult)
    canvas.drawText("Graph Interpretation", 40f, y, sectionPaint)
    y += 24f
    y = drawWrappedLines(
        canvas,
        bodyPaint,
        "Frame timing follows the imported video. The selected series plot and speed plot above summarize the exported window, while the profile fit quantifies how well the chosen mathematical model explains the measured motion.",
        54f,
        y,
        1120f
    )
    y += 12f
    canvas.drawText("Data Snapshot", 40f, y, titlePaint)
    y += 22f
    canvas.drawText("Track   Frame   Time(s)   X   Y   Speed   Accel", 54f, y, bodyPaint)
    y += 18f
    points.take(18).forEach { point ->
        val line = buildString {
            append(point.trackLabel.padEnd(5))
            append(" ")
            append(point.frameNumber.toString().padEnd(7))
            append(" ")
            append(formatSeconds(point.timestampUs).padEnd(9))
            append(" ")
            append(formatCalibratedValue(point.xValue).padEnd(8))
            append(" ")
            append(formatCalibratedValue(point.yValue).padEnd(8))
            append(" ")
            append(formatOptionalCalibratedValue(point.speedValue).padEnd(8))
            append(" ")
            append(formatOptionalCalibratedValue(point.accelerationValue))
        }
        canvas.drawText(line.take(130), 54f, y, bodyPaint)
        y += 16f
    }
    document.finishPage(page)
    val focusedCharts = buildList {
        if (showTrackA && trackAPoints.isNotEmpty()) add(
            ExportChartSeries("Track A", graphSamplesForSeries(trackAPoints, selectedSeries), trackAColor)
        )
        if (showTrackB && trackBPoints.isNotEmpty()) add(
            ExportChartSeries("Track B", graphSamplesForSeries(trackBPoints, selectedSeries), trackBColor)
        )
        if (showDelta && trackAPoints.isNotEmpty() && trackBPoints.isNotEmpty()) add(
            ExportChartSeries("Δ (B-A)", deltaSamplesForSeries(trackAPoints, trackBPoints, selectedSeries), AndroidColor.rgb(255, 241, 118))
        )
        if (isEmpty()) add(
            ExportChartSeries(graphSeriesLabel(selectedSeries, displayedUnit), selectedSeriesSamples, AndroidColor.CYAN)
        )
    }
    val combinedCharts = buildList {
        if (showTrackA && trackAPoints.isNotEmpty()) add(
            ExportChartSeries("Track A", graphSamplesForSeries(trackAPoints, selectedSeries), trackAColor)
        )
        if (showTrackB && trackBPoints.isNotEmpty()) add(
            ExportChartSeries("Track B", graphSamplesForSeries(trackBPoints, selectedSeries), trackBColor)
        )
        if (showDelta && trackAPoints.isNotEmpty() && trackBPoints.isNotEmpty()) add(
            ExportChartSeries("Δ (B-A)", deltaSamplesForSeries(trackAPoints, trackBPoints, selectedSeries), AndroidColor.rgb(255, 241, 118))
        )
    }
    val appendixCharts = buildList {
        if (trackAPoints.isNotEmpty()) add(ExportChartSeries("Track A", graphSamplesForSeries(trackAPoints, selectedSeries), trackAColor))
        if (trackBPoints.isNotEmpty()) add(ExportChartSeries("Track B", graphSamplesForSeries(trackBPoints, selectedSeries), trackBColor))
        if (trackAPoints.isNotEmpty() && trackBPoints.isNotEmpty()) {
            add(ExportChartSeries("Δ (B-A)", deltaSamplesForSeries(trackAPoints, trackBPoints, selectedSeries), AndroidColor.rgb(255, 241, 118)))
        }
    }
    appendGraphPages(
        document = document,
        projectName = projectName,
        displayedUnit = displayedUnit,
        selectedSeries = selectedSeries,
        activeViewLabel = activeViewLabel,
        focusedCharts = focusedCharts,
        combinedCharts = combinedCharts,
        appendixCharts = appendixCharts,
        profileResult = profileResult
    )
    lightAnalysis?.let { appendLightPages(document, it) }
    appendDataPages(document, projectName, displayedUnit, points)
    appendAuditPages(document, auditLogContent, auditChecksum)
    return document
}

fun exportExperimentArtifacts(
    context: Context,
    projectName: String,
    displayedUnit: String,
    points: List<DisplayKinematicsPoint>,
    rawPoints: List<DisplayKinematicsPoint>,
    trackAPoints: List<DisplayKinematicsPoint>,
    trackBPoints: List<DisplayKinematicsPoint>,
    calibration: CalibrationEntity?,
    selectedSeries: GraphSeries,
    graphViewport: GraphViewportSummary,
    showTrackA: Boolean,
    showTrackB: Boolean,
    showDelta: Boolean,
    activeViewLabel: String,
    trackAColor: Int,
    trackBColor: Int,
    profileResult: ProfileResult?,
    photogateSummary: ExportPhotogateSummary?,
    twoBodySummary: ExportTwoBodySummary?,
    lightAnalysis: LightAnalysisExport? = null,
    processingStrength: Float,
    averagingWindow: Int,
    trimStartMs: Long,
    lastCheckpointSavedAt: Long?
): ExportArtifacts {
    val baseDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), "exports/${sanitizeFileName(projectName)}")
    if (!baseDir.exists()) baseDir.mkdirs()
    val stamp = exportTimestamp()
    val csvFile = File(baseDir, "${sanitizeFileName(projectName)}_$stamp.csv")
    val excelFile = File(baseDir, "${sanitizeFileName(projectName)}_$stamp.xlsx")
    val pdfFile = File(baseDir, "${sanitizeFileName(projectName)}_$stamp.pdf")
    val auditFile = File(baseDir, "${sanitizeFileName(projectName)}_$stamp.audit.txt")
    val auditLog = buildAuditLog(
        context = context,
        projectName = projectName,
        displayedUnit = displayedUnit,
        points = points,
        calibration = calibration,
        selectedSeries = selectedSeries,
        graphViewport = graphViewport,
        showTrackA = showTrackA,
        showTrackB = showTrackB,
        showDelta = showDelta,
        profileResult = profileResult,
        photogateSummary = photogateSummary,
        twoBodySummary = twoBodySummary,
        trimStartMs = trimStartMs,
        lastCheckpointSavedAt = lastCheckpointSavedAt
    )

    csvFile.writeText(
        appendLightCsvSection(
            buildCsvContent(
                projectName,
                displayedUnit,
                points,
                photogateSummary,
                twoBodySummary,
                processingStrength,
                averagingWindow
            ),
            lightAnalysis
        )
    )
    writeScientificXlsx(
        file = excelFile,
        projectName = projectName,
        displayedUnit = displayedUnit,
        rawPoints = rawPoints,
        selectedPoints = points,
        selectedSeries = selectedSeries,
        activeViewLabel = activeViewLabel,
        processingStrength = processingStrength,
        averagingWindow = averagingWindow,
        calibration = calibration
    )
    auditFile.writeText(auditLog.content)
    val pdf = buildPdf(
        context,
        projectName,
        displayedUnit,
        points,
        trackAPoints,
        trackBPoints,
        calibration,
        selectedSeries,
        graphViewport,
        showTrackA,
        showTrackB,
        showDelta,
        activeViewLabel,
        trackAColor,
        trackBColor,
        profileResult,
        photogateSummary,
        twoBodySummary,
        lightAnalysis,
        auditLog.checksum,
        auditLog.content
    )
    FileOutputStream(pdfFile).use { output -> pdf.writeTo(output) }
    pdf.close()
    return ExportArtifacts(csvFile, excelFile, pdfFile, auditFile, baseDir)
}

fun shareExportFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share export"))
}
