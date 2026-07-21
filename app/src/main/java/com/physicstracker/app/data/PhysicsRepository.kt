package com.physicstracker.app.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhysicsRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val trackPointDao: TrackPointDao,
    private val calibrationDao: CalibrationDao
) {
    fun getProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    
    suspend fun insertProject(project: ProjectEntity): Long = projectDao.insertProject(project)

    suspend fun touchProject(projectId: Long, updatedAt: Long) = projectDao.touchProject(projectId, updatedAt)

    suspend fun updateProjectTrim(projectId: Long, trimStartMs: Long, trimEndMs: Long?, updatedAt: Long) =
        projectDao.updateProjectTrim(projectId, trimStartMs, trimEndMs, updatedAt)

    suspend fun renameProject(projectId: Long, name: String, updatedAt: Long) =
        projectDao.renameProject(projectId, name, updatedAt)

    suspend fun deleteProject(project: ProjectEntity) =
        projectDao.deleteProject(project)

    fun getPointsForProject(projectId: Long): Flow<List<TrackPointEntity>> = 
        trackPointDao.getPointsForProject(projectId)

    suspend fun insertPoint(point: TrackPointEntity): Long = trackPointDao.insertPoint(point)
    
    suspend fun deletePoint(pointId: Long) = trackPointDao.deletePointById(pointId)

    suspend fun deleteAllPoints(projectId: Long) = trackPointDao.deleteAllPointsForProject(projectId)

    suspend fun updateMassForTrack(projectId: Long, trackLabel: String, massKg: Double) =
        trackPointDao.updateMassForTrack(projectId, trackLabel, massKg)

    fun getCalibrationForProject(projectId: Long): Flow<CalibrationEntity?> =
        calibrationDao.getCalibrationForProject(projectId)

    suspend fun upsertCalibration(calibration: CalibrationEntity) =
        calibrationDao.upsertCalibration(calibration)

    suspend fun deleteCalibration(projectId: Long) =
        calibrationDao.deleteCalibrationForProject(projectId)
}
