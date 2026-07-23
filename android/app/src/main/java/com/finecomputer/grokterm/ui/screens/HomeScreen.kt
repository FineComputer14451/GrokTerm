package com.finecomputer.grokterm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Download
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTerminal: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val binaryManager = remember { GrokBinaryManager(context) }
    val apiKeyStore = remember { ApiKeyStore(context) }
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("Checking Grok binary…") }
    var isReady by remember { mutableStateOf(false) }
    var hasKey by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }

    fun refreshStatus() {
        isReady = binaryManager.isReady
        hasKey = apiKeyStore.hasApiKey()
        status = when {
            isReady && hasKey -> "Grok binary ready · API key set"
            isReady -> "Grok binary ready · set API key in Settings"
            else -> "Grok binary not present"
        }
    }

    LaunchedEffect(Unit) {
        refreshStatus()
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Grok Build\non Android",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier = Modifier.padding(16.dp)) {
                    Text("Status", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(status, style = MaterialTheme.typography.bodyLarge)
                    if (progressText.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            progressText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Button(
                onClick = onOpenTerminal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Open Terminal")
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
                                if (p.version != null) append(" (")
                                    .append(p.version).append(")")
                                if (p.totalBytes > 0) {
                                    val pct = (p.bytesDownloaded * 100 / p.totalBytes).toInt()
                                    append(" · ").append(pct).append("%")
                                } else if (p.bytesDownloaded > 0) {
                                    append(" · ")
                                        .append(p.bytesDownloaded / 1024).append(" KB")
                                }
                            }
                        }
                        isDownloading = false
                        progressText = ""
                        refreshStatus()
                        status = if (result.isSuccess) {
                            result.getOrNull() ?: "Ready"
                        } else {
                            "Download failed: ${result.exceptionOrNull()?.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDownloading
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Downloading…")
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(if (isReady) "Update Grok Binary" else "Download Grok Binary")
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Downloads the official aarch64 musl binary from x.ai/cli\n" +
                        "and applies the DNS patch automatically.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
