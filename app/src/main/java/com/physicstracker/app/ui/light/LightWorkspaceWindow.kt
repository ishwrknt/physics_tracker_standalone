package com.physicstracker.app.ui.light

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.physicstracker.app.ui.FloatingOverlayChrome

/** Same draggable header and resize grip as the tracking workspace, bounded to the media stage. */
@Composable
internal fun LightWorkspaceWindow(title: String, widthDp: Float, heightDp: Float, index: Int,
    onClose: () -> Unit, content: @Composable () -> Unit) {
    val landscape = widthDp > heightDp
    var x by rememberSaveable { mutableFloatStateOf(0.03f + index * 0.025f) }
    var y by rememberSaveable { mutableFloatStateOf(0.03f + index * 0.025f) }
    var width by rememberSaveable { mutableFloatStateOf(if (landscape) 0.58f else 0.90f) }
    var height by rememberSaveable { mutableFloatStateOf(0.58f) }
    val left = x.coerceIn(0f, (1 - width).coerceAtLeast(0f))
    val top = y.coerceIn(0f, (1 - height).coerceAtLeast(0f))
    Box(Modifier.fillMaxSize()) {
        Surface(color = Color(0xF018222D), modifier = Modifier.offset((widthDp * left).dp, (heightDp * top).dp)
            .size((widthDp * width).dp, (heightDp * height).dp)) {
            Box(Modifier.padding(top = 34.dp, start = 8.dp, end = 8.dp, bottom = 34.dp)) { content() }
        }
        FloatingOverlayChrome(title, false, left, top, width, height, landscape,
            onMove = { a, b -> x = a; y = b }, onResize = { a, b -> width = a; height = b },
            onReset = { x = 0.03f; y = 0.03f; width = if (landscape) 0.58f else 0.90f; height = 0.58f },
            onClose = onClose, minimumWidthFraction = if (landscape) 0.35f else 0.65f,
            minimumHeightFraction = 0.5f, areaWidthDp = widthDp, areaHeightDp = heightDp)
    }
}
