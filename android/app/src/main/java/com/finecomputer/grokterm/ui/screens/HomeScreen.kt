package com.finecomputer.grokterm.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finecomputer.grokterm.data.ApiKeyStore
import com.finecomputer.grokterm.data.GrokBinaryManager
import com.finecomputer.grokterm.data.LaunchAction
import com.finecomputer.grokterm.data.PendingLaunch
import com.finecomputer.grokterm.data.ProjectStore
import com.finecomputer.grokterm.data.TermuxBridge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTerminal: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBrowser: () -> Unit = {}
) {
    val context = LocalContext.current
    val binaryManager = remember { GrokBinaryManager(context) }
    val apiKeyStore = remember { ApiKeyStore(context) }
    val projectStore = remember { ProjectStore(context) }
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("Checking Grok binary…") }
    var isReady by remember { mutableStateOf(false) }
    var hasKey by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf<String?>(null) }
    var showHeadlessDialog by remember { mutableStateOf(false) }
    var headlessPrompt by remember { mutableStateOf("") }
    var termuxInstalled by remember { mutableStateOf(false) }

    fun refreshStatus() {
        isReady = binaryManager.isReady
        hasKey = apiKeyStore.hasApiKey()
        termuxInstalled = TermuxBridge.isTermuxInstalled(context)
        status = when {
            isReady && hasKey -> "Grok binary ready · API key set"
            isReady -> "Grok binary ready · set API key in Settings"
            else -> "Grok binary not present"
        }
    }

    fun launch(action: LaunchAction) {
        PendingLaunch.set(action)
        onOpenTerminal()
    }

    fun openTermux() {
        scope.launch {
            val uri = projectStore.getProjectUri()
            val workDir = uri?.let { projectStore.tryResolveFilesystemPath(it) }
            val cmd = TermuxBridge.defaultGrokCommand(workDir)
            val ok = if (termuxInstalled) {
                TermuxBridge.runInTermux(context, cmd, workDir)
            } else {
                false
            }
            if (!ok) {
                if (!termuxInstalled) {
                    Toast.makeText(context, "Termux not installed — opening F-Droid", Toast.LENGTH_LONG).show()
                    TermuxBridge.openTermuxInstallPage(context)
                } else {
                    Toast.makeText(
                        context,
                        "Could not run command — launching Termux app",
                        Toast.LENGTH_SHORT
                    ).show()
                    TermuxBridge.launchTermux(context)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshStatus()
        projectName = projectStore.getProjectName()
    }

    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val name = uri.lastPathSegment
                    ?.substringAfterLast(":")
                    ?.replace("%2F", "/")
                    ?: "Selected folder"
                projectStore.setProject(uri, name)
                projectName = name
            }
        }
    }

    if (showHeadlessDialog) {
        AlertDialog(
            onDismissRequest = { showHeadlessDialog = false },
            title = { Text("Headless prompt") },
            text = {
                OutlinedTextField(
                    value = headlessPrompt,
                    onValueChange = { headlessPrompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("grok -p \"…\"") },
                    placeholder = { Text("Explain this repo") },
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val p = headlessPrompt.trim()
                        if (p.isNotEmpty()) {
                            showHeadlessDialog = false
                            launch(LaunchAction.Headless(p))
                        }
                    },
                    enabled = headlessPrompt.isNotBlank()
                ) { Text("Run") }
            },
            dismissButton = {
                TextButton(onClick = { showHeadlessDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GrokTerm", fontFamily = FontFamily.Monospace) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Grok Build\non Android",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier = Modifier.padding(16.dp)) {
                    Text("Status", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(status, style = MaterialTheme.typography.bodyLarge)
                    if (progressText.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(progressText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (termuxInstalled) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Termux detected (Phase 1 bridge available)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier = Modifier.padding(16.dp)) {
                    Text("Project / Production Bible", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(projectName ?: "No folder selected", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { openTreeLauncher.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (projectName == null) "Select" else "Change")
                        }
                        if (projectName != null) {
                            OutlinedButton(
                                onClick = onOpenBrowser,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.TravelExplore, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Browse")
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    projectStore.clearProject()
                                    projectName = null
                                }
                            }) { Text("Clear") }
                        }
                    }
                }
            }

            Text("Quick Actions", style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth())

            Row(Modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTile(Icons.Default.PlayArrow, "Grok", isReady, Modifier.weight(1f)) {
                    launch(LaunchAction.Interactive)
                }
                QuickTile(Icons.AutoMirrored.Filled.List, "Plan", isReady, Modifier.weight(1f)) {
                    launch(LaunchAction.Plan)
                }
            }

            Row(Modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTile(Icons.Default.Terminal, "Headless", isReady, Modifier.weight(1f)) {
                    showHeadlessDialog = true
                }
                QuickTile(Icons.Default.Refresh, "Resume", isReady, Modifier.weight(1f)) {
                    launch(LaunchAction.Resume)
                }
            }

            Button(onClick = onOpenTerminal, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Default.Terminal, null)
                Spacer(Modifier.width(12.dp))
                Text("Open Terminal (Shell)")
            }

            OutlinedButton(
                onClick = { openTermux() },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.OpenInNew, null)
                Spacer(Modifier.width(12.dp))
                Text(if (termuxInstalled) "Open in Termux (Phase 1)" else "Install Termux (F-Droid)")
            }

            Button(
                onClick = {
                    if (isDownloading) return@Button
                    scope.launch {
                        isDownloading = true
                        progressText = "Starting…"
                        val result = binaryManager.downloadOrUpdate { p ->
                            progressText = buildString {
                                append(p.stage)
                                if (p.version != null) append(" (").append(p.version).append(")")
                                if (p.totalBytes > 0) {
                                    val pct = (p.bytesDownloaded * 100 / p.totalBytes).toInt()
                                    append(" · ").append(pct).append("%")
                                } else if (p.bytesDownloaded > 0) {
                                    append(" · ").append(p.bytesDownloaded / 1024).append(" KB")
                                }
                            }
                        }
                        isDownloading = false
                        progressText = ""
                        refreshStatus()
                        status = if (result.isSuccess) result.getOrNull() ?: "Ready"
                        else "Download failed: ${result.exceptionOrNull()?.message}"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDownloading
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(12.dp))
                    Text("Downloading…")
                } else {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(12.dp))
                    Text(if (isReady) "Update Grok Binary" else "Download Grok Binary")
                }
            }

            Text(
                text = "Phase 1 Termux = full native grok. Phase 2 = in-app TUI.\n" +
                        "RUN_COMMAND must be allowed in Termux settings for auto-start.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun QuickTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
