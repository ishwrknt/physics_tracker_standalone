package com.physicstracker.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.physicstracker.app.live.data.LiveSessionDao
import com.physicstracker.app.live.data.LiveSessionEntity

@Database(
    entities = [
        ProjectEntity::class,
        CalibrationEntity::class,
        TrackPointEntity::class,
        TrackerEntity::class,
        LiveSessionEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun trackerDao(): TrackerDao
    abstract fun liveSessionDao(): LiveSessionDao
}
