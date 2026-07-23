package com.finecomputer.grokterm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
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

    var status by remember { mutableStateOf("Checking Grok binary...") }
    var isReady by remember { mutableStateOf(false) }
    var hasKey by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isReady = binaryManager.isReady
        hasKey = apiKeyStore.hasApiKey()
        status = when {
            isReady && hasKey -> "Grok binary ready · API key set"
            isReady -> "Grok binary ready · API key missing (Settings)"
            else -> "Grok binary not found — use Phase 1 Termux install or place binary"
        }
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
                }
            }

            Button(
                onClick = onOpenTerminal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = true // always allow terminal; binary optional for plain shell
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Open Terminal")
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        status = "Checking / patching..."
                        val result = binaryManager.downloadOrUpdate()
                        isReady = binaryManager.isReady
                        status = if (result.isSuccess) {
                            "Ready"
                        } else {
                            result.exceptionOrNull()?.message ?: "Failed"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Check / Patch Binary")
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Phase 2 scaffold · Use Phase 1 Termux installer for full native Grok\n" +
                        "or place aarch64 musl binary in app files.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
