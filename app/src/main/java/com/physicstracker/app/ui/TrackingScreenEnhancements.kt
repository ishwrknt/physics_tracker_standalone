package com.physicstracker.app.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.physicstracker.app.ui.VideoPlayerViewModel

// Enhanced PLAY tab content to replace the existing scrubber buttons
@Composable
fun EnhancedPlayTabContent(
    viewModel: VideoPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orientationPreferences = remember { OrientationPreferences(context) }
    
    EnhancedVideoControls(
        viewModel = viewModel,
        modifier = modifier,
        showFrameNumbers = orientationPreferences.showFrameNumbers,
        buttonSize = orientationPreferences.buttonSize,
        hapticFeedback = orientationPreferences.hapticFeedback
    )
}

// Integration helper to add orientation setup check to MainActivity
fun shouldShowOrientationSetup(context: Context): Boolean {
    return OrientationPreferences(context).shouldShowOrientationSetup
}
