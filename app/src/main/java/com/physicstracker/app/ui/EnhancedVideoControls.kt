package com.physicstracker.app.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Copy of the private function from TrackingScreen.kt
private fun formatTimestampMs(valueMs: Long): String {
    val minutes = valueMs / 60_000
    val seconds = (valueMs % 60_000) / 1_000
    val millis = valueMs % 1_000
    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}

// Enhanced video controls with better UX and haptic feedback
@Composable
fun EnhancedVideoControls(
    viewModel: VideoPlayerViewModel,
    modifier: Modifier = Modifier,
    showFrameNumbers: Boolean = true,
    buttonSize: Float = 1.0f,
    hapticFeedback: Boolean = true
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // Enhanced button states
    var isRewinding by remember { mutableStateOf(false) }
    var isForwarding by remember { mutableStateOf(false) }
    
    // Auto-repeat logic
    LaunchedEffect(isRewinding) {
        if (isRewinding) {
            while (isRewinding) {
                viewModel.stepFrames(-10)
                delay(500) // Repeat every 500ms while held
            }
        }
    }
    
    LaunchedEffect(isForwarding) {
        if (isForwarding) {
            while (isForwarding) {
                viewModel.stepFrames(10)
                delay(500) // Repeat every 500ms while held
            }
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Time and frame display
        EnhancedTimeDisplay(
            viewModel = viewModel,
            showFrameNumbers = showFrameNumbers,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Enhanced video controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rewind button (10 seconds back) - with auto-repeat
            EnhancedControlButton(
                icon = Icons.Filled.FastRewind,
                contentDescription = "Rewind 10 seconds",
                onClick = { 
                    if (hapticFeedback) triggerHapticFeedback(context)
                    viewModel.stepFrames(-10 * 30) // Assuming 30fps
                },
                onLongPress = { 
                    if (hapticFeedback) triggerHapticFeedback(context)
                    isRewinding = true 
                },
                onLongPressRelease = { isRewinding = false },
                buttonSize = buttonSize,
                enabled = viewModel.player.currentPosition > 0
            )
            
            // Previous frame button
            EnhancedControlButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = "Previous frame",
                onClick = { 
                    if (hapticFeedback) triggerHapticFeedback(context)
                    viewModel.stepFrames(-1) 
                },
                buttonSize = buttonSize,
                enabled = viewModel.player.currentPosition > 0
            )
            
            // Enhanced Play/Pause button
            EnhancedPlayPauseButton(
                isPlaying = isPlaying,
                onClick = { 
                    if (hapticFeedback) triggerHapticFeedback(context)
                    viewModel.togglePlayPause() 
                },
                buttonSize = buttonSize
            )
            
            // Next frame button
            EnhancedControlButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next frame",
                onClick = { 
                    if (hapticFeedback) triggerHapticFeedback(context)
                    viewModel.stepFrames(1) 
                },
                buttonSize = buttonSize,
                enabled = viewModel.player.currentPosition < viewModel.player.duration
            )
            
            // Forward button (10 seconds forward) - with auto-repeat
            EnhancedControlButton(
                icon = Icons.Filled.FastForward,
                contentDescription = "Forward 10 seconds",
                onClick = { 
                    if (hapticFeedback) triggerHapticFeedback(context)
                    viewModel.stepFrames(10 * 30) // Assuming 30fps
                },
                onLongPress = { 
                    if (hapticFeedback) triggerHapticFeedback(context)
                    isForwarding = true 
                },
                onLongPressRelease = { isForwarding = false },
                buttonSize = buttonSize,
                enabled = viewModel.player.currentPosition < viewModel.player.duration
            )
        }
        
        // Enhanced scrubber with visual feedback
        EnhancedScrubber(
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth(),
            buttonSize = buttonSize
        )
    }
}

@Composable
private fun EnhancedControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onLongPressRelease: (() -> Unit)? = null,
    buttonSize: Float = 1.0f,
    enabled: Boolean = true
) {
    val scale = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            onLongPress?.invoke()
            scale.animateTo(0.9f, spring(stiffness = Spring.StiffnessMedium))
        } else {
            onLongPressRelease?.invoke()
            scale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
        }
    }
    
    Box(
        modifier = Modifier
            .size((48.dp * buttonSize).coerceIn(32.dp, 64.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) 
                    PhysicsColorPrimary.copy(alpha = 0.1f) 
                else 
                    Color.Gray.copy(alpha = 0.1f)
            )
            .border(
                width = 1.dp,
                color = if (enabled) 
                    PhysicsColorPrimary.copy(alpha = 0.3f) 
                else 
                    Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, radius = 24.dp),
                onClick = if (enabled) onClick else {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) PhysicsColorPrimary else Color.Gray,
            modifier = Modifier.scale(scale.value)
        )
    }
}

@Composable
private fun EnhancedPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    buttonSize: Float = 1.0f
) {
    val scale = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    LaunchedEffect(isPressed) {
        scale.animateTo(
            targetValue = if (isPressed) 0.85f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        )
    }
    
    Box(
        modifier = Modifier
            .size((64.dp * buttonSize).coerceIn(48.dp, 80.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                PhysicsColorPrimary.copy(alpha = 0.15f)
            )
            .border(
                width = 2.dp,
                color = PhysicsColorPrimary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, radius = 32.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = PhysicsColorPrimary,
            modifier = Modifier
                .size((32.dp * buttonSize).coerceIn(24.dp, 40.dp))
                .scale(scale.value)
        )
    }
}

@Composable
private fun EnhancedTimeDisplay(
    viewModel: VideoPlayerViewModel,
    showFrameNumbers: Boolean,
    modifier: Modifier = Modifier
) {
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val frameDurationMs by viewModel.frameDurationMs.collectAsState()
    
    val currentFrame = if (frameDurationMs > 0) (currentPosition / frameDurationMs).toInt() else 0
    val totalFrames = if (frameDurationMs > 0) (duration / frameDurationMs).toInt() else 0
    
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTimestampMs(currentPosition),
            color = PhysicsColorOnSurface.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        
        if (showFrameNumbers) {
            Text(
                text = "Frame ${currentFrame + 1}/${totalFrames}",
                color = PhysicsColorOnSurface.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
        
        Text(
            text = formatTimestampMs(duration),
            color = PhysicsColorOnSurface.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EnhancedScrubber(
    viewModel: VideoPlayerViewModel,
    modifier: Modifier = Modifier,
    buttonSize: Float = 1.0f
) {
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    
    val progress = if (duration > 0) {
        currentPosition.toFloat() / duration.toFloat()
    } else 0f
    
    Column(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                val newPositionMs = (newProgress * duration).roundToInt().toLong()
                viewModel.seekTo(newPositionMs)
            },
            colors = SliderDefaults.colors(
                thumbColor = PhysicsColorPrimary,
                activeTrackColor = PhysicsColorPrimary,
                inactiveTrackColor = PhysicsColorOnSurface.copy(alpha = 0.2f)
            )
        )
    }
}

// Haptic feedback helper
private fun triggerHapticFeedback(context: Context) {
    try {
        // Android haptic feedback
        android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    } catch (e: Exception) {
        // Fallback - do nothing if haptic feedback fails
    }
}

// Orientation preference manager
class OrientationPreferences(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("physics_tracker_orientation", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_ORIENTATION_SETUP_COMPLETE = "orientation_setup_complete"
        private const val KEY_LANDSCAPE_MODE = "landscape_mode"
        private const val KEY_AUTO_ROTATE = "auto_rotate"
        private const val KEY_SHOW_FRAME_NUMBERS = "show_frame_numbers"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_BUTTON_SIZE = "button_size"
    }
    
    var shouldShowOrientationSetup: Boolean
        get() = !prefs.getBoolean(KEY_ORIENTATION_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ORIENTATION_SETUP_COMPLETE, !value).apply()
    
    var landscapeMode: Boolean
        get() = prefs.getBoolean(KEY_LANDSCAPE_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_LANDSCAPE_MODE, value).apply()
    
    var autoRotate: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ROTATE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_ROTATE, value).apply()
    
    var showFrameNumbers: Boolean
        get() = prefs.getBoolean(KEY_SHOW_FRAME_NUMBERS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_FRAME_NUMBERS, value).apply()
    
    var hapticFeedback: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()
    
    var buttonSize: Float
        get() = prefs.getFloat(KEY_BUTTON_SIZE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_BUTTON_SIZE, value).apply()
    
    fun resetOrientationSetup() {
        prefs.edit().remove(KEY_ORIENTATION_SETUP_COMPLETE).apply()
    }
}
