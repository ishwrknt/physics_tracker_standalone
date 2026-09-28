package com.physicstracker.app.live

/**
 * Automatic live calibration: estimate meters-per-analysis-pixel from camera optics
 * (pinhole model) using the reported focal length, physical sensor size, the active
 * crop, and the current lens focus distance.
 *
 * Honesty contract: this is an ESTIMATE. It depends on the accuracy of the device's
 * reported focus distance ([focusCalibration]). The UI must always present it as an
 * estimate to be confirmed, never as validated truth.
 */
object LiveScaleEstimator {

    enum class FocusQuality { UNKNOWN, UNCALIBRATED, APPROXIMATE, CALIBRATED }

    data class Estimate(
        val metersPerPixel: Double,
        val distanceMeters: Double,
        val focusQuality: FocusQuality
    )

    /**
     * @param focalLengthMm reported focal length
     * @param sensorWidthMm physical sensor width (mm)
     * @param sensorFullWidthPx full sensor resolution width (px)
     * @param cropWidthSensorPx width of the captured crop region in sensor px
     *   (from ImageProxy.cropRect — reflects the shared ViewPort crop)
     * @param analysisWidthPx our downscaled analysis frame width
     * @param focusDistanceDiopters LENS_FOCUS_DISTANCE (diopters) from the capture result;
     *   null/0 means unavailable or fixed-focus infinity.
     */
    fun estimate(
        focalLengthMm: Double,
        sensorWidthMm: Double,
        sensorFullWidthPx: Int,
        cropWidthSensorPx: Int,
        analysisWidthPx: Int,
        focusDistanceDiopters: Double?,
        focusQuality: FocusQuality = FocusQuality.UNKNOWN,
        hyperfocalDistanceMeters: Double? = null
    ): Estimate? {
        if (focalLengthMm <= 0.0 || sensorWidthMm <= 0.0 || sensorFullWidthPx <= 0 ||
            cropWidthSensorPx <= 0 || analysisWidthPx <= 0
        ) return null
        val diopter = focusDistanceDiopters ?: return null
        if (diopter <= 0.0) return null // fixed-focus / infinity: distance unknown
        val distanceMeters = 1.0 / diopter
        // Degenerate readings: horribly close, or at infinity on a fixed-focus device.
        if (distanceMeters <= 0.05 || distanceMeters > 1000.0) return null
        val pixelPitchMm = sensorWidthMm / sensorFullWidthPx
        val sensorPxPerAnalysisPx = cropWidthSensorPx.toDouble() / analysisWidthPx
        // X[m] = D[m] * (px on sensor) * pitch[mm] / f[mm]  (pinhole)
        val mpp = distanceMeters * (sensorPxPerAnalysisPx * pixelPitchMm) / focalLengthMm
        if (mpp <= 0.0) return null
        return Estimate(metersPerPixel = mpp, distanceMeters = distanceMeters, focusQuality = focusQuality)
    }

    fun mapFocusCalibration(value: Int?): FocusQuality = when (value) {
        2 -> FocusQuality.APPROXIMATE
        3 -> FocusQuality.UNCALIBRATED
        1 -> FocusQuality.CALIBRATED
        else -> FocusQuality.UNKNOWN
    }
}
