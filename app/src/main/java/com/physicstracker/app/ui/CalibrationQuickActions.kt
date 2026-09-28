package com.physicstracker.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal fun calibrationEditAllowed(locked: Boolean, measurementOnly: Boolean) = !locked || measurementOnly

@Composable
internal fun CalibrationQuickActions(
    locked: Boolean,
    available: Boolean,
    showAxisActions: Boolean,
    onCentre: () -> Unit,
    onReset: () -> Unit,
    onToggleLock: () -> Unit
) {
    Column {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onToggleLock, enabled = available, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (locked) "Unlock calibration" else "Lock calibration")
            }
            if (showAxisActions) {
                OutlinedButton(onClick = onCentre, enabled = available && !locked, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Centre zero")
                }
                OutlinedButton(onClick = onReset, enabled = available && !locked, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Reset direction")
                }
            }
        }
        Text(
            if (locked) "Calibration locked. Measurements still work."
            else if (showAxisActions) "Place zero, then rotate X/Y. Reset puts +X right and +Y up."
            else "Set the known length, then lock to prevent accidental changes.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
