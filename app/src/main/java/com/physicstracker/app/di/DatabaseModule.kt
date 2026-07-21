package com.physicstracker.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.physicstracker.app.data.AppDatabase
import com.physicstracker.app.data.CalibrationDao
import com.physicstracker.app.data.ProjectDao
import com.physicstracker.app.data.TrackPointDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE projects ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "UPDATE projects SET updatedAt = createdAt WHERE updatedAt = 0"
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE calibration ADD COLUMN calibrationUnit TEXT NOT NULL DEFAULT 'm'"
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE projects ADD COLUMN trimStartMs INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE projects ADD COLUMN trimEndMs INTEGER"
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS track_points_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    frameNumber INTEGER NOT NULL,
                    timestampUs INTEGER NOT NULL,
                    trackLabel TEXT NOT NULL DEFAULT 'A',
                    pixelX REAL NOT NULL,
                    pixelY REAL NOT NULL,
                    massKg REAL NOT NULL,
                    FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO track_points_new (id, projectId, frameNumber, timestampUs, trackLabel, pixelX, pixelY, massKg)
                SELECT id, projectId, frameNumber, timestampUs, 'A', pixelX, pixelY, massKg
                FROM track_points
                """.trimIndent()
            )
            db.execSQL("DROP TABLE track_points")
            db.execSQL("ALTER TABLE track_points_new RENAME TO track_points")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_track_points_projectId_frameNumber_trackLabel ON track_points(projectId, frameNumber, trackLabel)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "physics_tracker_db"
        )
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .addMigrations(MIGRATION_4_5)
            .build()
    }

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    fun provideTrackPointDao(database: AppDatabase): TrackPointDao {
        return database.trackPointDao()
    }

    @Provides
    fun provideCalibrationDao(database: AppDatabase): CalibrationDao {
        return database.calibrationDao()
    }
}
