package com.physicstracker.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.physicstracker.app.di.DatabaseModule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration5To6Test {
    private val databaseName = "migration-5-6-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun preservesPointsAndClassifiesLegacyCoordinates() {
        helper.createDatabase(databaseName, 5).apply {
            execSQL("INSERT INTO projects (id,name,videoUri,videoWidth,videoHeight,trimStartMs,trimEndMs,createdAt,updatedAt) VALUES (1,'p','file://v',848,478,0,NULL,1,1)")
            execSQL("INSERT INTO calibration (projectId,stickP1x,stickP1y,stickP2x,stickP2y,knownDistanceMeters,calibrationUnit,originX,originY,rotationDegrees) VALUES (1,0,0,100,0,1,'m',0,0,0)")
            execSQL("INSERT INTO track_points (id,projectId,frameNumber,timestampUs,trackLabel,pixelX,pixelY,massKg) VALUES (1,1,3,49989,'B',12.5,20.25,1)")
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 6, true, DatabaseModule.MIGRATION_5_6).use { db ->
            db.query("SELECT trackerId, coordinateSpace, pixelX, pixelY FROM track_points WHERE id=1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("1:B", cursor.getString(0))
                assertEquals("LEGACY_DISPLAY", cursor.getString(1))
                assertEquals(12.5, cursor.getDouble(2), 0.0)
                assertEquals(20.25, cursor.getDouble(3), 0.0)
            }
        }
    }
}
