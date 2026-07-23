package com.finecomputer.grokterm.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.finecomputer.grokterm.data.ProjectStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BrowserEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val document: DocumentFile
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val projectStore = remember { ProjectStore(context) }

    var rootUri by remember { mutableStateOf<Uri?>(null) }
    var stack by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var entries by remember { mutableStateOf<List<BrowserEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("Browser") }

    // Load root from ProjectStore
    LaunchedEffect(Unit) {
        loading = true
        error = null
        val uri = projectStore.getProjectUri()
        if (uri == null) {
            error = "No project folder selected. Go back and pick one on Home."
            loading = false
            return@LaunchedEffect
        }
        rootUri = uri
        val root = DocumentFile.fromTreeUri(context, uri)
        if (root == null || !root.exists()) {
            error = "Cannot open the selected folder. Re-select it on Home."
            loading = false
            return@LaunchedEffect
        }
        stack = listOf(root)
        title = root.name ?: projectStore.getProjectName() ?: "Project"
        entries = loadChildren(root)
        loading = false
    }

    fun navigateInto(doc: DocumentFile) {
        if (!doc.isDirectory) return
        loading = true
        stack = stack + doc
        title = doc.name ?: "Folder"
        // Load off main in effect-style
    }

    // Reload when stack changes
    LaunchedEffect(stack) {
        if (stack.isEmpty()) return@LaunchedEffect
        loading = true
        val current = stack.last()
        title = current.name ?: "Folder"
        entries = withContext(Dispatchers.IO) { loadChildren(current) }
        loading = false
    }

    fun navigateUp(): Boolean {
        if (stack.size <= 1) return false
        stack = stack.dropLast(1)
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            title,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (stack.size > 1) {
                            Text(
                                stack.dropLast(1).joinToString(" / ") { it.name ?: "…" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navigateUp()) onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(Modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(
                        error!!,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                entries.isEmpty() -> {
                    Text(
                        "Empty folder",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(entries, key = { it.document.uri.toString() }) { entry ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        entry.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                supportingContent = {
                                    if (!entry.isDirectory && entry.size >= 0) {
                                        Text(formatSize(entry.size))
                                    } else if (entry.isDirectory) {
                                        Text("Folder")
                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = when {
                                            entry.isDirectory -> Icons.Default.Folder
                                            entry.name.endsWith(".md", true) ||
                                                    entry.name.endsWith(".txt", true) ||
                                                    entry.name.endsWith(".json", true) ->
                                                Icons.Default.Description
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        tint = if (entry.isDirectory)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                },
                                modifier = Modifier.clickable {
                                    if (entry.isDirectory) {
                                        stack = stack + entry.document
                                    }
                                    // Files: future — preview / open
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

private fun loadChildren(dir: DocumentFile): List<BrowserEntry> {
    val children = dir.listFiles() ?: return emptyList()
    return children
        .filter { it.name != null }
        .map { doc ->
            BrowserEntry(
                name = doc.name ?: "?",
                isDirectory = doc.isDirectory,
                size = if (doc.isFile) doc.length() else -1L,
                document = doc
            )
        }
        .sortedWith(
            compareBy<BrowserEntry> { !it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}
