package com.physicstracker.app.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.physicstracker.app.ui.VideoPlayerViewModel

// Simple enhanced controls that will work
@Composable
fun SimpleEnhancedControls(
    viewModel: VideoPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Time display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = PhysicsColorOnSurface.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = formatTime(duration),
                color = PhysicsColorOnSurface.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace
            )
        }
        
        // Video controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.stepFrames(-10) },
                enabled = currentPosition > 0
            ) {
                Icon(Icons.Default.SkipPrevious, "Back 10")
            }
            
            IconButton(
                onClick = { viewModel.stepFrames(-1) },
                enabled = currentPosition > 0
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, "Back 1")
            }
            
            Button(
                onClick = { viewModel.togglePlayPause() }
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (isPlaying) "Pause" else "Play"
                )
            }
            
            IconButton(
                onClick = { viewModel.stepFrames(1) },
                enabled = currentPosition < duration
            ) {
                Icon(Icons.Default.KeyboardArrowRight, "Forward 1")
            }
            
            IconButton(
                onClick = { viewModel.stepFrames(10) },
                enabled = currentPosition < duration
            ) {
                Icon(Icons.Default.SkipNext, "Forward 10")
            }
        }
        
        // Scrubber
        Slider(
            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
            onValueChange = { progress ->
                viewModel.seekTo((progress * duration).toLong())
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
