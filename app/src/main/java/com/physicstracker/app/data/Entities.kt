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
    val rotationDegrees: Double = 0.0
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
    val massKg: Double = 1.0 // Default for P2 dynamics
)
