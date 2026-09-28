package com.physicstracker.app.live.data

import com.physicstracker.app.data.CalibrationDao
import com.physicstracker.app.data.CalibrationEntity
import com.physicstracker.app.data.ProjectDao
import com.physicstracker.app.data.ProjectEntity
import com.physicstracker.app.data.TrackPointDao
import com.physicstracker.app.data.TrackPointEntity
import com.physicstracker.app.data.TrackerDao
import com.physicstracker.app.data.TrackerEntity
import com.physicstracker.app.live.LiveCalibration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts a finished live run into a first-class [[ProjectEntity]] experiment so it
 * appears in the Experiments library and opens in the existing analysis workspace
 * (graphs, tables, fitting) without any import/export step.
 *
 * Coordinate discipline follows [[TrackPointEntity]] conventions:
 * SOURCE_VIDEO space equals the live analysis frame; origin is the first recorded
 * sample so positions continue to match what the user saw live.
 */
@Singleton
class LiveExperimentConverter @Inject constructor(
    private val projectDao: ProjectDao,
    private val calibrationDao: CalibrationDao,
    private val trackPointDao: TrackPointDao,
    private val trackerDao: TrackerDao
) {
    data class LiveSampleRow(
        val elapsedUs: Long,
        val xPx: Double,
        val yPx: Double,
        val confidence: Double,
        val matchScore: Double?
    )

    suspend fun convert(
        name: String,
        samples: List<LiveSampleRow>,
        calibration: LiveCalibration?,
        frameWidth: Int,
        frameHeight: Int,
        trackerAlgorithm: String
    ): Long {
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = name,
                videoUri = "",
                videoWidth = frameWidth,
                videoHeight = frameHeight
            )
        )
        trackerDao.upsertTracker(
            TrackerEntity(
                id = "$projectId:A",
                projectId = projectId,
                displayName = "Object",
                colorArgb = 0xFF55D98D,
                displayOrder = 0
            )
        )
        val first = samples.firstOrNull()
        if (calibration != null && first != null) {
            calibrationDao.upsertCalibration(
                CalibrationEntity(
                    projectId = projectId,
                    stickP1x = calibration.stickP1x.toDouble(),
                    stickP1y = calibration.stickP1y.toDouble(),
                    stickP2x = calibration.stickP2x.toDouble(),
                    stickP2y = calibration.stickP2y.toDouble(),
                    knownDistanceMeters = calibration.knownDistanceMeters,
                    calibrationUnit = calibration.unit,
                    originX = first.xPx,
                    originY = first.yPx,
                    rotationDegrees = 0.0,
                    coordinateSpace = "SOURCE_VIDEO",
                    sourceWidth = frameWidth,
                    sourceHeight = frameHeight
                )
            )
        }
        samples.forEachIndexed { index, s ->
            trackPointDao.insertPoint(
                TrackPointEntity(
                    projectId = projectId,
                    frameNumber = index + 1,
                    timestampUs = s.elapsedUs,
                    trackLabel = "A",
                    trackerId = "$projectId:A",
                    pixelX = s.xPx,
                    pixelY = s.yPx,
                    requestedTimestampUs = s.elapsedUs,
                    actualTimestampUs = s.elapsedUs,
                    sourceWidth = frameWidth,
                    sourceHeight = frameHeight,
                    coordinateSpace = "SOURCE_VIDEO",
                    observationKind = "AUTO_MEASURED",
                    sourceAlgorithm = trackerAlgorithm,
                    confidence = s.confidence,
                    matchScore = s.matchScore
                )
            )
        }
        return projectId
    }
}
