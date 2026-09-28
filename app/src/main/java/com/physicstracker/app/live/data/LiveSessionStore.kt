package com.physicstracker.app.live.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Persisted live-tracking session. Independent from the video-analysis project schema:
 * live runs have no video URI and store their own self-contained sample series.
 *
 * [samplesCsv] holds one sample per line:
 * t_us,x_px,y_px,confidence,x_m,y_m,vx,vy,ax,ay,speed,accel
 * (blank fields for values not yet available, e.g. the first derivative columns).
 */
@Entity(tableName = "live_sessions")
data class LiveSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtEpochMs: Long,
    val durationUs: Long,
    val sampleCount: Int,
    val frameWidth: Int,
    val frameHeight: Int,
    val metersPerPixel: Double?,
    val stickPixelLength: Double?,
    val knownDistanceMeters: Double?,
    val calibrationUnit: String?,
    val physicsMode: String,
    val trackerAlgorithm: String,
    val samplesCsv: String,
    /** JSON metadata: calibration history, device info, segments. */
    val metaJson: String = "{}"
)

@Dao
interface LiveSessionDao {
    @Query("SELECT * FROM live_sessions ORDER BY createdAtEpochMs DESC")
    fun getSessions(): Flow<List<LiveSessionEntity>>

    @Insert
    suspend fun insert(session: LiveSessionEntity): Long

    @Delete
    suspend fun delete(session: LiveSessionEntity)
}
