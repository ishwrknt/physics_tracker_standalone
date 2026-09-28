package com.physicstracker.app.ui.light

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun LightDemoGuide(step: Int, busy: Boolean, onNext: () -> Unit, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(when (step) {
                0 -> "1/4 · Synthetic demo: a gradient that changes brightness. Start by selecting a line."
                1 -> "2/4 · Drag the line endpoints if you wish. Measure to see brightness along it."
                2 -> "3/4 · Graph and Table use the same samples. Now measure the changing brightness over time."
                else -> "4/4 · Results autosave on this device. Return to History and reopen this demo to try again."
            }, modifier = Modifier.weight(1f).padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onNext, enabled = !busy) { Text(listOf("Select line", "Measure", "Measure time", "History")[step.coerceIn(0, 3)]) }
            TextButton(onClick = onDismiss) { Text("Close guide") }
        }
    }
}
