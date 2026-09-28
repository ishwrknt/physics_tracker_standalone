package com.physicstracker.app.ui.light

import androidx.compose.ui.geometry.Offset
import com.physicstracker.app.ui.LightChannel
import com.physicstracker.app.ui.LightGeometry
import com.physicstracker.app.ui.LightTool
import java.io.*
import java.util.UUID

data class LightProjectSummary(val id: String, val name: String, val video: Boolean, val updatedAt: Long, val samples: Int, val uri: String)
data class SavedLightProject(
    val id: String, val uri: String, val name: String, val video: Boolean, val width: Int, val height: Int,
    val timestampUs: Long?, val provenance: String, val active: String, val regionCount: Int,
    val channel: LightChannel, val geometries: Map<String, LightGeometry>, val datasets: Map<String, LightDataset>,
    val timeMode: Boolean = false, val smooth: Boolean = false, val baseline: Boolean = false,
    val peaks: Boolean = false, val compare: Boolean = false, val updatedAt: Long = System.currentTimeMillis()
) {
    val summary get() = LightProjectSummary(id, name, video, updatedAt, datasets.values.sumOf { it.observations.size }, uri)
}

/** Versioned streaming snapshots avoid placing hundreds of thousands of samples in a Room cursor.
 * Writes are fsynced and replaced with backup recovery. Original media is referenced, never deleted.
 */
class LightProjectStore(private val directory: File) {
    var unreadableProjects: Int = 0
        private set
    private fun file(id: String): File {
        require(UUID.fromString(id).toString() == id) { "Invalid light project identity" }
        check(directory.exists() || directory.mkdirs()) { "Cannot create project storage" }
        return File(directory, "$id.light")
    }
    @Synchronized fun save(project: SavedLightProject) {
        val target = file(project.id)
        val pending = File(target.path + ".new")
        val backup = File(target.path + ".bak")
        recover(target)
        try {
            FileOutputStream(pending).use { stream ->
                val output = DataOutputStream(BufferedOutputStream(stream))
                write(output, project); output.flush(); stream.fd.sync()
            }
            if (target.exists()) check(target.renameTo(backup)) { "Cannot preserve previous save" }
            check(pending.renameTo(target)) { "Cannot commit light project" }
            backup.delete()
        } catch (error: Exception) {
            pending.delete()
            if (!target.exists() && backup.exists()) backup.renameTo(target)
            throw error
        }
    }
    private fun recover(target: File) {
        val backup = File(target.path + ".bak")
        if (!target.exists() && backup.exists()) check(backup.renameTo(target)) { "Cannot recover previous save" }
    }
    @Synchronized fun load(id: String): SavedLightProject {
        val target = file(id); recover(target)
        return DataInputStream(BufferedInputStream(FileInputStream(target))).use(::read)
    }
    @Synchronized fun history(): List<LightProjectSummary> {
        unreadableProjects = 0
        if (!directory.exists()) return emptyList()
        directory.listFiles()?.filter { it.name.endsWith(".light.bak") }?.forEach { recover(File(it.path.removeSuffix(".bak"))) }
        return directory.listFiles().orEmpty().filter { it.extension == "light" }.mapNotNull { target ->
            try { DataInputStream(BufferedInputStream(FileInputStream(target))).use { input ->
                require(input.readInt() == MAGIC && input.readInt() == VERSION) { "Unsupported light project: ${target.name}" }
                LightProjectSummary(input.readUTF(), input.readUTF(), input.readBoolean(), input.readLong(), input.readInt(), input.readUTF())
            } } catch (_: Exception) { unreadableProjects++; null }
        }.sortedByDescending { it.updatedAt }
    }
    companion object {
        private const val MAGIC = 0x504C4954
        private const val VERSION = 1
        private fun DataOutputStream.nullLong(value: Long?) { writeBoolean(value != null); if (value != null) writeLong(value) }
        private fun DataInputStream.nullLong(): Long? = if (readBoolean()) readLong() else null
        private fun DataOutputStream.geometry(value: LightGeometry) {
            writeUTF(value.tool.name); writeFloat(value.start.x); writeFloat(value.start.y); writeFloat(value.end.x); writeFloat(value.end.y)
        }
        private fun DataInputStream.geometry() = LightGeometry(LightTool.valueOf(readUTF()), Offset(readFloat(), readFloat()), Offset(readFloat(), readFloat()))
        private fun DataInputStream.count(max: Int): Int = readInt().also { require(it in 0..max) { "Invalid snapshot count" } }
        fun write(out: DataOutputStream, p: SavedLightProject) = with(out) {
            require(p.regionCount in 1..MAX_LIGHT_REGIONS && p.active in lightRegionLabels(p.regionCount) &&
                p.geometries.size <= MAX_LIGHT_REGIONS && p.datasets.size <= MAX_LIGHT_REGIONS &&
                p.datasets.values.all { it.observations.size <= 200_000 }) { "Snapshot exceeds supported sample limits" }
            writeInt(MAGIC); writeInt(VERSION); writeUTF(p.id); writeUTF(p.name); writeBoolean(p.video); writeLong(p.updatedAt); writeInt(p.summary.samples)
            writeUTF(p.uri); writeInt(p.width); writeInt(p.height); nullLong(p.timestampUs); writeUTF(p.provenance)
            writeUTF(p.active); writeInt(p.regionCount); writeUTF(p.channel.name)
            writeBoolean(p.timeMode); writeBoolean(p.smooth); writeBoolean(p.baseline); writeBoolean(p.peaks); writeBoolean(p.compare)
            writeInt(p.geometries.size); p.geometries.forEach { (slot, g) -> writeUTF(slot); geometry(g) }
            writeInt(p.datasets.size); p.datasets.forEach { (slot, d) ->
                writeUTF(slot); writeUTF(d.mediaName); writeUTF(d.mediaUri); writeInt(d.width); writeInt(d.height)
                writeUTF(d.channel.name); geometry(d.geometry); writeBoolean(d.timeSeries); writeUTF(d.provenance); writeBoolean(d.complete)
                writeUTF(d.processing.orEmpty()); writeInt(d.observations.size)
                d.observations.forEach { s -> writeDouble(s.axis); writeDouble(s.value); nullLong(s.timestampUs); writeBoolean(s.gapBefore)
                    writeBoolean(s.rawValue != null); s.rawValue?.let(::writeDouble) }
            }
        }
        fun read(input: DataInputStream): SavedLightProject = with(input) {
            require(readInt() == MAGIC && readInt() == VERSION) { "Unsupported light project version" }
            val id = readUTF(); val name = readUTF(); val video = readBoolean(); val updated = readLong(); readInt()
            val uri = readUTF(); val width = readInt(); val height = readInt(); val pts = nullLong(); val provenance = readUTF()
            require(width > 0 && height > 0)
            val active = readUTF(); val regions = readInt(); val channel = LightChannel.valueOf(readUTF())
            require(regions in 1..MAX_LIGHT_REGIONS && active in lightRegionLabels(regions))
            val time = readBoolean(); val smooth = readBoolean(); val baseline = readBoolean(); val peaks = readBoolean(); val compare = readBoolean()
            val geometry = buildMap { repeat(count(MAX_LIGHT_REGIONS)) { val slot = readUTF(); require(slot in lightRegionLabels(regions)); put(slot, geometry()) } }
            val datasets = buildMap { repeat(count(MAX_LIGHT_REGIONS)) {
                val slot = readUTF(); require(slot in lightRegionLabels(regions))
                val mediaName = readUTF(); val mediaUri = readUTF(); val w = readInt(); val h = readInt()
                val ch = LightChannel.valueOf(readUTF()); val g = geometry(); val timeSeries = readBoolean(); val prov = readUTF(); val complete = readBoolean()
                val processing = readUTF().ifEmpty { null }
                val samples = List(count(200_000)) {
                    val axis = readDouble(); val value = readDouble(); val stamp = nullLong(); val gap = readBoolean()
                    val raw = if (readBoolean()) readDouble() else null
                    require(axis.isFinite() && value.isFinite() && (raw == null || raw.isFinite()))
                    LightObservation(axis, value, stamp, gap, raw)
                }
                put(slot, LightDataset(mediaName, mediaUri, w, h, slot, ch, g, timeSeries, samples, prov, complete, processing))
            } }
            SavedLightProject(id, uri, name, video, width, height, pts, provenance, active, regions, channel, geometry, datasets,
                time, smooth, baseline, peaks, compare, updated)
        }
    }
}
