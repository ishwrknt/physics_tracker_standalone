package com.physicstracker.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, CalibrationEntity::class, TrackPointEntity::class, TrackerEntity::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun trackerDao(): TrackerDao
}
