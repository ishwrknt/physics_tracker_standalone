package com.physicstracker.app.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Copies once into durable app storage so demo projects can reopen from History. */
suspend fun prepareDemo(context: Context, demo: DemoMedia): Uri = withContext(Dispatchers.IO) {
    require(demo in DemoCatalog.items)
    val directory = File(context.filesDir, "bundled-demos-v1").apply { check(isDirectory || mkdirs()) }
    val target = File(directory, demo.file)
    if (!target.isFile || target.length() == 0L) {
        val pending = File.createTempFile("demo-", ".tmp", directory)
        try {
            context.assets.open(demo.asset).use { source -> pending.outputStream().use { source.copyTo(it) } }
            check(pending.renameTo(target)) { "Could not save demo media" }
        } finally { pending.delete() }
    }
    Uri.fromFile(target)
}

fun demoSourceNote(uri: Uri): String {
    if (uri.scheme != "file" || File(uri.path ?: "").parentFile?.name != "bundled-demos-v1") return ""
    return DemoCatalog.items.firstOrNull { it.file == uri.lastPathSegment }?.let { "; ${it.sourceNote}" }.orEmpty()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DemoPicker(busy: Boolean, error: String?, onClose: () -> Unit, onChoose: (DemoMedia) -> Unit) {
    var light by rememberSaveable { mutableStateOf(true) }
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text("Try a demo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Open a sample in the same workspace as your own media. Your saved projects stay unchanged.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = light, onClick = { light = true }, label = { Text("Light") })
                    FilterChip(selected = !light, onClick = { light = false }, label = { Text("Motion") })
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                LazyColumn(Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DemoCatalog.items.filter { it.light == light }, key = { it.asset }) { demo ->
                        OutlinedButton(enabled = !busy, onClick = { onChoose(demo) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(demo.title, style = MaterialTheme.typography.titleSmall)
                                Text(demo.instruction, style = MaterialTheme.typography.bodySmall)
                                Text(demo.sourceNote, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(enabled = !busy, onClick = onClose) { Text("Close") } })
}
