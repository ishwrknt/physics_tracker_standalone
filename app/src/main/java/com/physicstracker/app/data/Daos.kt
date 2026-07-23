package com.physicstracker.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC, createdAt DESC, id DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Query("UPDATE projects SET updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun touchProject(projectId: Long, updatedAt: Long)

    @Query("UPDATE projects SET trimStartMs = :trimStartMs, trimEndMs = :trimEndMs, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun updateProjectTrim(projectId: Long, trimStartMs: Long, trimEndMs: Long?, updatedAt: Long)

    @Query("UPDATE projects SET name = :name, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun renameProject(projectId: Long, name: String, updatedAt: Long)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface TrackPointDao {
    @Query("SELECT * FROM track_points WHERE projectId = :projectId ORDER BY timestampUs ASC, trackLabel ASC, id ASC")
    fun getPointsForProject(projectId: Long): Flow<List<TrackPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TrackPointEntity): Long

    @Delete
    suspend fun deletePoint(point: TrackPointEntity)

    @Query("DELETE FROM track_points WHERE id = :pointId")
    suspend fun deletePointById(pointId: Long)

    @Query("DELETE FROM track_points WHERE projectId = :projectId")
    suspend fun deleteAllPointsForProject(projectId: Long)

    @Query("UPDATE track_points SET massKg = :massKg WHERE projectId = :projectId AND trackLabel = :trackLabel")
    suspend fun updateMassForTrack(projectId: Long, trackLabel: String, massKg: Double)

    @Query("DELETE FROM track_points WHERE projectId = :projectId AND trackerId = :trackerId AND frameNumber BETWEEN :startFrame AND :endFrame")
    suspend fun deleteTrackerRange(projectId: Long, trackerId: String, startFrame: Int, endFrame: Int)
}

@Dao
interface CalibrationDao {
    @Query("SELECT * FROM calibration WHERE projectId = :projectId LIMIT 1")
    fun getCalibrationForProject(projectId: Long): Flow<CalibrationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalibration(calibration: CalibrationEntity)

    @Query("DELETE FROM calibration WHERE projectId = :projectId")
    suspend fun deleteCalibrationForProject(projectId: Long)
}

@Dao
interface TrackerDao {
    @Query("SELECT * FROM trackers WHERE projectId = :projectId ORDER BY displayOrder, createdAt, id")
    fun getTrackersForProject(projectId: Long): Flow<List<TrackerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracker(tracker: TrackerEntity)

    @Delete
    suspend fun deleteTracker(tracker: TrackerEntity)
}
