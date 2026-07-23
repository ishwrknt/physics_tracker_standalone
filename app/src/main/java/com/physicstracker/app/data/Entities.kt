package com.physicstracker.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val videoUri: String, // Path to local video file
    val videoWidth: Int,
    val videoHeight: Int,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

@Entity(
    tableName = "calibration",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class CalibrationEntity(
    @PrimaryKey val projectId: Long,
    // Calibration Stick (2 points)
    val stickP1x: Double,
    val stickP1y: Double,
    val stickP2x: Double,
    val stickP2y: Double,
    val knownDistanceMeters: Double, // e.g., 1.0
    val calibrationUnit: String = "m",
    
    // Coordinate System Origin
    val originX: Double,
    val originY: Double,
    val rotationDegrees: Double = 0.0,
    val coordinateSpace: String = "LEGACY_DISPLAY",
    val sourceWidth: Int? = null,
    val sourceHeight: Int? = null
)

@Entity(
    tableName = "track_points",
    indices = [Index(value = ["projectId", "frameNumber", "trackLabel"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val frameNumber: Int,
    val timestampUs: Long, // Precise presentation time from MediaExtractor
    val trackLabel: String = "A",
    val pixelX: Double,
    val pixelY: Double,
    val massKg: Double = 1.0, // Default for P2 dynamics
    val trackerId: String = trackLabel,
    val requestedTimestampUs: Long? = null,
    val actualTimestampUs: Long? = null,
    val sourceWidth: Int? = null,
    val sourceHeight: Int? = null,
    val coordinateSpace: String = "SOURCE_VIDEO",
    val observationKind: String = "MANUAL",
    val sourceAlgorithm: String = "manual",
    val confidence: Double? = null,
    val matchScore: Double? = null,
    val ambiguity: Double? = null,
    val residualErrorPx: Double? = null,
    val refinementMethod: String? = null,
    val recoveryStatus: String? = null,
    val isManualCorrection: Boolean = false,
    val isProtectedKeyframe: Boolean = false
)

@Entity(
    tableName = "trackers",
    indices = [Index(value = ["projectId", "displayOrder"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TrackerEntity(
    @PrimaryKey val id: String,
    val projectId: Long,
    val displayName: String,
    val colorArgb: Long,
    val displayOrder: Int,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
