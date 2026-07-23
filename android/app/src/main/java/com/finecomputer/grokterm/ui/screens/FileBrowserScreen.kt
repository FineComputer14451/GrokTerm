package com.finecomputer.grokterm.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.finecomputer.grokterm.data.ProjectStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

data class BrowserEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val document: DocumentFile
)

private val TEXT_EXTENSIONS = setOf(
    "md", "txt", "json", "yaml", "yml", "toml", "xml", "html", "css", "js",
    "ts", "tsx", "jsx", "kt", "kts", "java", "py", "rs", "go", "c", "h",
    "cpp", "hpp", "sh", "bash", "zsh", "fish", "gradle", "properties",
    "csv", "tsv", "log", "ini", "cfg", "conf", "env", "gitignore",
    "dockerfile", "makefile", "cmake", "sql", "r", "rb", "php", "swift"
)

private const val MAX_PREVIEW_BYTES = 512 * 1024 // 512 KB

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val projectStore = remember { ProjectStore(context) }
    val scope = rememberCoroutineScope()

    var stack by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var entries by remember { mutableStateOf<List<BrowserEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("Browser") }

    // Preview state
    var previewName by remember { mutableStateOf<String?>(null) }
    var previewContent by remember { mutableStateOf<String?>(null) }
    var previewLoading by remember { mutableStateOf(false) }
    var previewTruncated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        val uri = projectStore.getProjectUri()
        if (uri == null) {
            error = "No project folder selected. Go back and pick one on Home."
            loading = false
            return@LaunchedEffect
        }
        val root = DocumentFile.fromTreeUri(context, uri)
        if (root == null || !root.exists()) {
            error = "Cannot open the selected folder. Re-select it on Home."
            loading = false
            return@LaunchedEffect
        }
        stack = listOf(root)
        title = root.name ?: projectStore.getProjectName() ?: "Project"
        entries = withContext(Dispatchers.IO) { loadChildren(root) }
        loading = false
    }

    LaunchedEffect(stack) {
        if (stack.isEmpty()) return@LaunchedEffect
        loading = true
        val current = stack.last()
        title = current.name ?: "Folder"
        entries = withContext(Dispatchers.IO) { loadChildren(current) }
        loading = false
    }

    fun navigateUp(): Boolean {
        if (previewName != null) {
            previewName = null
            previewContent = null
            previewTruncated = false
            return true
        }
        if (stack.size <= 1) return false
        stack = stack.dropLast(1)
        return true
    }

    fun openFile(entry: BrowserEntry) {
        if (entry.isDirectory) {
            stack = stack + entry.document
            return
        }

        val ext = entry.name.substringAfterLast('.', "").lowercase()
        val looksText = ext in TEXT_EXTENSIONS ||
                entry.name.equals("Dockerfile", true) ||
                entry.name.equals("Makefile", true) ||
                entry.name.equals("LICENSE", true) ||
                entry.name.equals("README", true)

        if (!looksText) {
            Toast.makeText(context, "Binary or unknown type — cannot preview", Toast.LENGTH_SHORT).show()
            return
        }

        if (entry.size > 2_000_000) {
            Toast.makeText(context, "File too large to preview (>2 MB)", Toast.LENGTH_SHORT).show()
            return
        }

        previewLoading = true
        previewName = entry.name
        previewContent = null
        previewTruncated = false

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                readTextPreview(context, entry.document)
            }
            previewLoading = false
            if (result == null) {
                previewName = null
                Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
            } else {
                previewContent = result.first
                previewTruncated = result.second
            }
        }
    }

    // Full-screen preview
    if (previewName != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                previewName!!,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (previewTruncated) {
                                Text(
                                    "Preview truncated to ${MAX_PREVIEW_BYTES / 1024} KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            previewName = null
                            previewContent = null
                            previewTruncated = false
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0A0A0A)
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF0A0A0A))
            ) {
                when {
                    previewLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFF00E5FF)
                        )
                    }
                    previewContent != null -> {
                        val vScroll = rememberScrollState()
                        val hScroll = rememberScrollState()
                        Text(
                            text = previewContent!!,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(vScroll)
                                .horizontalScroll(hScroll)
                                .padding(12.dp),
                            color = Color(0xFFE0E0E0),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
        return
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
                                            isTextLike(entry.name) -> Icons.Default.Description
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        tint = if (entry.isDirectory)
                                            MaterialTheme.colorScheme.primary
                                        else if (isTextLike(entry.name))
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                },
                                modifier = Modifier.clickable { openFile(entry) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

private fun isTextLike(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in TEXT_EXTENSIONS ||
            name.equals("Dockerfile", true) ||
            name.equals("Makefile", true) ||
            name.equals("LICENSE", true) ||
            name.equals("README", true)
}

/**
 * @return Pair(content, truncated) or null on failure
 */
private fun readTextPreview(
    context: android.content.Context,
    doc: DocumentFile
): Pair<String, Boolean>? {
    return try {
        context.contentResolver.openInputStream(doc.uri)?.use { input ->
            val bytes = input.readNBytes(MAX_PREVIEW_BYTES + 1)
            val truncated = bytes.size > MAX_PREVIEW_BYTES
            val slice = if (truncated) bytes.copyOf(MAX_PREVIEW_BYTES) else bytes
            // Reject if too many null bytes (likely binary)
            val nulls = slice.count { it == 0.toByte() }
            if (nulls > slice.size / 50) return null
            String(slice, Charset.forName("UTF-8")) to truncated
        }
    } catch (_: Exception) {
        null
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
