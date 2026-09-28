package com.physicstracker.app.tracking

import android.graphics.Bitmap

/** One bulk read instead of JNI getPixel calls inside the search loop. Caller owns bitmap lifetime. */
fun Bitmap.toTrackingFrame(): ArgbTrackingFrame {
    require(!isRecycled)
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return ArgbTrackingFrame(width, height, pixels)
}
