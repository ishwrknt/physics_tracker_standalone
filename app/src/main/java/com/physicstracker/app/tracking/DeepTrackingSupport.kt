package com.physicstracker.app.tracking

import android.content.Context
import android.net.Uri
import java.io.File

private const val DEEP_TRACKER_MODEL_ASSET = "models/deeplab_mobile.tflite"
private const val DEEP_TRACKER_MODEL_FILE = "deeplab_mobile.tflite"
private const val DEEP_TRACKER_MODEL_DIR = "models"

enum class DeepTrackingSource {
    NONE,
    APP_FILE,
    ASSET
}

data class DeepTrackingSupport(
    val modelAssetPath: String = DEEP_TRACKER_MODEL_ASSET,
    val modelFilePath: String?,
    val source: DeepTrackingSource
) {
    val isModelReady: Boolean get() = source != DeepTrackingSource.NONE

    fun displayLabel(): String = when (source) {
        DeepTrackingSource.APP_FILE -> "Deep model loaded"
        DeepTrackingSource.ASSET -> "Bundled deep model"
        DeepTrackingSource.NONE -> "No deep model"
    }

    fun unavailableMessage(): String =
        when (source) {
            DeepTrackingSource.APP_FILE -> "Deep accurate mode is ready"
            DeepTrackingSource.ASSET -> "Deep accurate mode is ready"
            DeepTrackingSource.NONE -> "Deep accurate mode needs model asset $modelAssetPath or imported .tflite file"
        }

    companion object {
        fun from(context: Context): DeepTrackingSupport {
            val modelDir = File(context.filesDir, DEEP_TRACKER_MODEL_DIR).apply { mkdirs() }
            val fileModel = File(modelDir, DEEP_TRACKER_MODEL_FILE)
            if (fileModel.exists() && fileModel.length() > 0L) {
                return DeepTrackingSupport(
                    modelFilePath = fileModel.absolutePath,
                    source = DeepTrackingSource.APP_FILE
                )
            }
            val isBundled = runCatching {
                context.assets.open(DEEP_TRACKER_MODEL_ASSET).close()
                true
            }.getOrDefault(false)
            return DeepTrackingSupport(
                modelFilePath = null,
                source = if (isBundled) DeepTrackingSource.ASSET else DeepTrackingSource.NONE
            )
        }

        fun installModelFromUri(context: Context, uri: Uri): DeepTrackingSupport {
            val modelDir = File(context.filesDir, DEEP_TRACKER_MODEL_DIR).apply { mkdirs() }
            val fileModel = File(modelDir, DEEP_TRACKER_MODEL_FILE)
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to read selected model" }
                fileModel.outputStream().use { output -> input.copyTo(output) }
            }
            return from(context)
        }

        fun clearImportedModel(context: Context): DeepTrackingSupport {
            val fileModel = File(File(context.filesDir, DEEP_TRACKER_MODEL_DIR), DEEP_TRACKER_MODEL_FILE)
            if (fileModel.exists()) {
                fileModel.delete()
            }
            return from(context)
        }
    }
}
