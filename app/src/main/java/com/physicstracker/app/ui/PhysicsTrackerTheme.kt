package com.physicstracker.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PhysicsColorCanvas = Color(0xFF070B10)
val PhysicsColorBackground = Color(0xFF0B1118)
val PhysicsColorSurface = Color(0xFF121A23)
val PhysicsColorSurfaceVariant = Color(0xFF192431)
val PhysicsColorPrimary = Color(0xFF52C7EA)
val PhysicsColorPrimaryDim = Color(0xFF246A80)
val PhysicsColorAccent = Color(0xFFF2B84B)
val PhysicsColorOnBackground = Color(0xFFF1F5F8)
val PhysicsColorOnSurface = Color(0xFFA9B7C5)
val PhysicsColorDivider = Color(0xFF2A3948)
val PhysicsColorError = Color(0xFFFF6B6B)
val PhysicsColorTrackA = Color(0xFF55D98D)
val PhysicsColorTrackB = Color(0xFFFFBE55)

private val PhysicsTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 14.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)

private val PhysicsColorScheme = darkColorScheme(
    primary = PhysicsColorPrimary,
    onPrimary = PhysicsColorBackground,
    secondary = PhysicsColorAccent,
    onSecondary = PhysicsColorBackground,
    background = PhysicsColorBackground,
    onBackground = PhysicsColorOnBackground,
    surface = PhysicsColorSurface,
    onSurface = PhysicsColorOnSurface,
    surfaceVariant = PhysicsColorSurfaceVariant,
    onSurfaceVariant = PhysicsColorOnSurface,
    outline = PhysicsColorDivider,
    error = PhysicsColorError
)

@Composable
fun PhysicsTrackerWorkspaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PhysicsColorScheme,
        typography = PhysicsTypography,
        content = content
    )
}
