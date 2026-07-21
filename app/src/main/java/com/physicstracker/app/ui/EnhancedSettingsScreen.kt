package com.physicstracker.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun EnhancedSettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val orientationPreferences = remember { OrientationPreferences(context) }
    
    var landscapeMode by remember { mutableStateOf(orientationPreferences.landscapeMode) }
    var autoRotate by remember { mutableStateOf(orientationPreferences.autoRotate) }
    var showFrameNumbers by remember { mutableStateOf(orientationPreferences.showFrameNumbers) }
    var hapticFeedback by remember { mutableStateOf(orientationPreferences.hapticFeedback) }
    var buttonSize by remember { mutableStateOf(orientationPreferences.buttonSize) }
    
    var showSaveConfirmation by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize your PhysicsTracker experience",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            
            IconButton(
                onClick = {
                    // Save all settings
                    orientationPreferences.apply {
                        landscapeMode = this@EnhancedSettingsScreen.landscapeMode
                        autoRotate = this@EnhancedSettingsScreen.autoRotate
                        showFrameNumbers = this@EnhancedSettingsScreen.showFrameNumbers
                        hapticFeedback = this@EnhancedSettingsScreen.hapticFeedback
                        buttonSize = this@EnhancedSettingsScreen.buttonSize
                    }
                    showSaveConfirmation = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save",
                    tint = PhysicsColorPrimary
                )
            }
        }
        
        // Display Settings Section
        SettingsSection(
            title = "Display Settings",
            icon = Icons.Filled.DisplaySettings
        ) {
            SettingsToggleItem(
                title = "Landscape Mode",
                description = "Use landscape orientation for video analysis",
                value = landscapeMode,
                onValueChanged = { landscapeMode = it }
            )
            
            SettingsToggleItem(
                title = "Auto-rotate Videos",
                description = "Automatically match video orientation",
                value = autoRotate,
                onValueChanged = { autoRotate = it }
            )
            
            SettingsToggleItem(
                title = "Show Frame Numbers",
                description = "Display frame numbers in controls",
                value = showFrameNumbers,
                onValueChanged = { showFrameNumbers = it }
            )
        }
        
        // Interaction Settings Section
        SettingsSection(
            title = "Interaction",
            icon = Icons.Filled.TouchApp
        ) {
            SettingsToggleItem(
                title = "Haptic Feedback",
                description = "Vibrate on button interactions",
                value = hapticFeedback,
                onValueChanged = { hapticFeedback = it }
            )
            
            SettingsSliderItem(
                title = "Button Size",
                description = "Adjust control button sizes",
                value = buttonSize,
                valueRange = 0.8f..1.5f,
                onValueChanged = { buttonSize = it },
                formatter = { "${(it * 100).roundToInt()}%" }
            )
        }
        
        // Data Management Section
        SettingsSection(
            title = "Data Management",
            icon = Icons.Filled.Storage
        ) {
            SettingsActionItem(
                title = "Export All Projects",
                description = "Export all tracking data as CSV",
                icon = Icons.Filled.FileDownload,
                onClick = {
                    // TODO: Implement export all projects
                }
            )
            
            SettingsActionItem(
                title = "Clear Cache",
                description = "Clear temporary files and cache",
                icon = Icons.Filled.Clear,
                onClick = {
                    // TODO: Implement clear cache
                }
            )
        }
        
        // Advanced Section
        SettingsSection(
            title = "Advanced",
            icon = Icons.Filled.Settings
        ) {
            SettingsActionItem(
                title = "Reset Orientation Setup",
                description = "Show orientation setup on next launch",
                icon = Icons.Filled.Refresh,
                onClick = {
                    orientationPreferences.resetOrientationSetup()
                }
            )
            
            SettingsActionItem(
                title = "Debug Mode",
                description = "Enable debug information and overlays",
                icon = Icons.Filled.BugReport,
                onClick = {
                    // TODO: Implement debug mode
                }
            )
        }
        
        // About Section
        SettingsSection(
            title = "About",
            icon = Icons.Filled.Info
        ) {
            SettingsInfoItem(
                title = "Version",
                value = "1.0.0",
                icon = Icons.Filled.Info
            )
            
            SettingsInfoItem(
                title = "Build",
                value = "Debug",
                icon = Icons.Filled.Build
            )
            
            SettingsActionItem(
                title = "Help & Support",
                description = "Get help with PhysicsTracker",
                icon = Icons.Filled.Help,
                onClick = {
                    // TODO: Implement help/support
                }
            )
        }
    }
    
    // Save confirmation snackbar
    if (showSaveConfirmation) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSaveConfirmation = false
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = PhysicsColorPrimary.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Saved",
                    tint = PhysicsColorPrimary,
                    size = 24.dp
                )
                Text(
                    text = "Settings saved successfully",
                    color = PhysicsColorPrimary,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PhysicsColorPrimary,
                size = 24.dp
            )
            Text(
                text = title,
                color = PhysicsColorPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = PhysicsColorSurface.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        
        Switch(
            checked = value,
            onCheckedChange = onValueChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PhysicsColorPrimary,
                checkedTrackColor = PhysicsColorPrimary.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SettingsSliderItem(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChanged: (Float) -> Unit,
    formatter: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            
            Text(
                text = formatter(value),
                color = PhysicsColorPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChanged,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = PhysicsColorPrimary,
                activeTrackColor = PhysicsColorPrimary,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = PhysicsColorPrimary,
            size = 24.dp
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = Color.White.copy(alpha = 0.5f),
            size = 20.dp
        )
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White.copy(alpha = 0.7f),
            size = 24.dp
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
