package com.physicstracker.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PhysicsColorBackground = Color(0xFF0D1F17)
val PhysicsColorSurface = Color(0xFF132B1E)
val PhysicsColorSurfaceVariant = Color(0xFF1A3828)
val PhysicsColorPrimary = Color(0xFF2ECC71)
val PhysicsColorPrimaryDim = Color(0xFF1A7A42)
val PhysicsColorAccent = Color(0xFFF0B429)
val PhysicsColorOnBackground = Color(0xFFE8F5EE)
val PhysicsColorOnSurface = Color(0xFFB2CFBE)
val PhysicsColorDivider = Color(0xFF1F4030)
val PhysicsColorError = Color(0xFFE74C3C)
val PhysicsColorTrackA = Color(0xFF2ECC71)
val PhysicsColorTrackB = Color(0xFFF0B429)

private val PhysicsTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 14.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
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
