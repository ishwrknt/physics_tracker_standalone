package com.physicstracker.app.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.physicstracker.app.R

@Composable
internal fun PhysicsTrackerLogo(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.logonew), contentDescription = "PhysicsTracker logo", modifier = modifier)
}
