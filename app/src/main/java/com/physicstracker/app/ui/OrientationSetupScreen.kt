package com.physicstracker.app.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun OrientationSetupScreen(
    navController: NavController,
    onSetupComplete: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val orientationPreferences = remember { OrientationPreferences(context) }
    
    var landscapeMode by remember { mutableStateOf(false) }
    var autoRotate by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    
    BackHandler {
        // Allow going back without completing setup
        onSetupComplete(false)
    }
    
    LaunchedEffect(Unit) {
        // Load current preferences if they exist
        landscapeMode = orientationPreferences.landscapeMode
        autoRotate = orientationPreferences.autoRotate
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Display Settings",
            tint = PhysicsColorPrimary,
            modifier = Modifier.size(64.dp)
        )
        
        Text(
            text = "Choose Your Display Mode",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "This setting will be saved and used every time you use PhyZX for consistent measurements.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Orientation options
        Text(
            text = "Video Analysis Mode",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrientationOptionCard(
                icon = Icons.Default.PhoneAndroid,
                title = "Portrait",
                description = "Better for handheld use",
                isSelected = !landscapeMode,
                onClick = { landscapeMode = false },
                modifier = Modifier.weight(1f)
            )
            
            OrientationOptionCard(
                icon = Icons.Default.TabletAndroid,
                title = "Landscape",
                description = "Better for video analysis",
                isSelected = landscapeMode,
                onClick = { landscapeMode = true },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Additional settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = PhysicsColorSurface.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Additional Settings",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-rotate videos",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Automatically match video orientation",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    
                    Switch(
                        checked = autoRotate,
                        onCheckedChange = { autoRotate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PhysicsColorPrimary,
                            checkedTrackColor = PhysicsColorPrimary.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { 
                    onSetupComplete(false)
                },
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    Color.White.copy(alpha = 0.3f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("Maybe Later", fontSize = 16.sp)
            }
            
            Button(
                onClick = {
                    isLoading = true
                    // Save preferences
                    orientationPreferences.apply {
                        landscapeMode = landscapeMode
                        autoRotate = autoRotate
                        shouldShowOrientationSetup = false
                    }
                    onSetupComplete(true)
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhysicsColorPrimary,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save & Continue", fontSize = 16.sp)
                }
            }
        }
        
        // Info section
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Info",
                    tint = PhysicsColorPrimary,
                    size = 24.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Why this matters",
                        color = PhysicsColorPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Consistent orientation ensures accurate measurements and reliable data analysis across all your experiments.",
                        color = PhysicsColorPrimary.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OrientationOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = if (isSelected) PhysicsColorPrimary else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                PhysicsColorPrimary.copy(alpha = 0.2f) 
            else 
                Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) PhysicsColorPrimary else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = title,
                color = if (isSelected) PhysicsColorPrimary else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
