package com.physicstracker.app.tracking

import android.content.Context
import android.net.Uri
import java.io.File

private const val MEDIAPIPE_TRACKER_MODEL_ASSET = "models/mediapipe_object_detector.tflite"
private const val MEDIAPIPE_TRACKER_MODEL_FILE = "mediapipe_object_detector.tflite"
private const val MEDIAPIPE_TRACKER_MODEL_DIR = "models"

enum class MediaPipeTrackingSource {
    NONE,
    APP_FILE,
    ASSET
}

data class MediaPipeTrackingSupport(
    val modelAssetPath: String = MEDIAPIPE_TRACKER_MODEL_ASSET,
    val modelFilePath: String?,
    val source: MediaPipeTrackingSource
) {
    val isModelReady: Boolean get() = source != MediaPipeTrackingSource.NONE

    fun displayLabel(): String = when (source) {
        MediaPipeTrackingSource.APP_FILE -> "MediaPipe model loaded"
        MediaPipeTrackingSource.ASSET -> "Bundled MediaPipe model"
        MediaPipeTrackingSource.NONE -> "No MediaPipe model"
    }

    fun unavailableMessage(): String = when (source) {
        MediaPipeTrackingSource.APP_FILE -> "MediaPipe detect mode is ready"
        MediaPipeTrackingSource.ASSET -> "MediaPipe detect mode is ready"
        MediaPipeTrackingSource.NONE ->
            "MediaPipe detect mode needs model asset $modelAssetPath or imported .tflite file"
    }

    companion object {
        fun from(context: Context): MediaPipeTrackingSupport {
            val modelDir = File(context.filesDir, MEDIAPIPE_TRACKER_MODEL_DIR).apply { mkdirs() }
            val fileModel = File(modelDir, MEDIAPIPE_TRACKER_MODEL_FILE)
            if (fileModel.exists() && fileModel.length() > 0L) {
                return MediaPipeTrackingSupport(
                    modelFilePath = fileModel.absolutePath,
                    source = MediaPipeTrackingSource.APP_FILE
                )
            }
            val isBundled = runCatching {
                context.assets.open(MEDIAPIPE_TRACKER_MODEL_ASSET).close()
                true
            }.getOrDefault(false)
            return MediaPipeTrackingSupport(
                modelFilePath = null,
                source = if (isBundled) MediaPipeTrackingSource.ASSET else MediaPipeTrackingSource.NONE
            )
        }

        fun installModelFromUri(context: Context, uri: Uri): MediaPipeTrackingSupport {
            val modelDir = File(context.filesDir, MEDIAPIPE_TRACKER_MODEL_DIR).apply { mkdirs() }
            val fileModel = File(modelDir, MEDIAPIPE_TRACKER_MODEL_FILE)
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to read selected MediaPipe model" }
                fileModel.outputStream().use { output -> input.copyTo(output) }
            }
            return from(context)
        }

        fun clearImportedModel(context: Context): MediaPipeTrackingSupport {
            val fileModel = File(File(context.filesDir, MEDIAPIPE_TRACKER_MODEL_DIR), MEDIAPIPE_TRACKER_MODEL_FILE)
            if (fileModel.exists()) {
                fileModel.delete()
            }
            return from(context)
        }
    }
}
