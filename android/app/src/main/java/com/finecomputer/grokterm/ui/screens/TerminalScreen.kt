package com.finecomputer.grokterm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finecomputer.grokterm.data.ApiKeyStore
import com.finecomputer.grokterm.data.GrokBinaryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

enum class TerminalMode {
    SHELL, GROK
}

/**
 * Upgraded interactive terminal.
 *
 * Modes:
 * - SHELL  : /system/bin/sh (always available)
 * - GROK   : launches the patched Grok Build binary when present
 *
 * Improvements over Phase 2 scaffold:
 * - Dual mode with one-tap switch / launch
 * - Injects XAI_API_KEY into the process environment
 * - Better concurrent stdout reader
 * - Cleaner process lifecycle
 * - Ready for future PTY / full VT emulator swap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val binaryManager = remember { GrokBinaryManager(context) }
    val apiKeyStore = remember { ApiKeyStore(context) }

    var mode by remember { mutableStateOf(TerminalMode.SHELL) }
    var lines by remember {
        mutableStateOf(
            listOf(
                "GrokTerm Terminal",
                "Modes: Shell (always) · Grok (when binary ready)",
                "Tip: set API key in Settings for full Grok auth."
            )
        )
    }
    var input by remember { mutableStateOf("") }
    var process by remember { mutableStateOf<Process?>(null) }
    var writer by remember { mutableStateOf<OutputStreamWriter?>(null) }
    var readerJob by remember { mutableStateOf<Job?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Idle") }

    fun append(line: String) {
        lines = lines + line
    }

    fun stopSession() {
        readerJob?.cancel()
        readerJob = null
        try {
            writer?.close()
        } catch (_: Exception) {}
        writer = null
        process?.destroy()
        process = null
        isRunning = false
        status = "Stopped"
    }

    fun startSession(targetMode: TerminalMode) {
        stopSession()
        mode = targetMode
        isRunning = true
        status = if (targetMode == TerminalMode.GROK) "Starting Grok…" else "Starting shell…"

        scope.launch(Dispatchers.IO) {
            try {
                val pb = if (targetMode == TerminalMode.GROK && binaryManager.isReady) {
                    // Direct launch of patched Grok binary
                    ProcessBuilder(binaryManager.binaryPath).also {
                        append("[GrokTerm] Launching patched Grok binary…")
                    }
                } else {
                    if (targetMode == TerminalMode.GROK) {
                        append("[GrokTerm] Grok binary not ready — falling back to shell.")
                        append("Place aarch64 musl binary or use Phase 1 Termux installer.")
                    }
                    ProcessBuilder("/system/bin/sh", "-").also {
                        append("[GrokTerm] Shell session started.")
                    }
                }

                pb.redirectErrorStream(true)
                val env = pb.environment()
                env["HOME"] = context.filesDir.absolutePath
                env["TERM"] = "xterm-256color"
                env["COLORTERM"] = "truecolor"

                // Inject API key if present
                apiKeyStore.getApiKey()?.let { key ->
                    env["XAI_API_KEY"] = key
                    append("[GrokTerm] XAI_API_KEY injected into environment.")
                }

                // Working directory
                pb.directory(context.filesDir)

                val p = pb.start()
                process = p
                writer = OutputStreamWriter(p.outputStream, Charsets.UTF_8)

                withContext(Dispatchers.Main) {
                    status = if (targetMode == TerminalMode.GROK) "Grok running" else "Shell running"
                }

                // Concurrent reader
                val reader = BufferedReader(InputStreamReader(p.inputStream, Charsets.UTF_8))
                readerJob = scope.launch(Dispatchers.IO) {
                    try {
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            withContext(Dispatchers.Main) {
                                append(line)
                            }
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            withContext(Dispatchers.Main) {
                                append("[reader] ${e.message}")
                            }
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            isRunning = false
                            status = "Session ended"
                        }
                    }
                }

                // Wait for process (optional, reader will detect EOF)
                p.waitFor()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    append("Failed to start session: ${e.message}")
                    isRunning = false
                    status = "Error"
                }
            }
        }
    }

    // Auto-start shell on first entry
    LaunchedEffect(Unit) {
        startSession(TerminalMode.SHELL)
    }

    fun sendCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        append("> $trimmed")
        scope.launch(Dispatchers.IO) {
            try {
                writer?.apply {
                    write(trimmed + "\n")
                    flush()
                } ?: withContext(Dispatchers.Main) {
                    append("[no active session]")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    append("Write error: ${e.message}")
                }
            }
        }
        input = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (mode == TerminalMode.GROK) "Grok" else "Shell",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            status,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        stopSession()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Launch / switch to Grok
                    IconButton(
                        onClick = { startSession(TerminalMode.GROK) },
                        enabled = !isRunning || mode != TerminalMode.GROK
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start Grok",
                            tint = if (binaryManager.isReady) Color(0xFF00E5FF) else Color.Gray
                        )
                    }
                    // Restart shell
                    IconButton(onClick = { startSession(TerminalMode.SHELL) }) {
                        Icon(Icons.Default.Terminal, contentDescription = "Shell")
                    }
                    IconButton(onClick = { lines = emptyList() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0A0A0A))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(lines) { line ->
                    Text(
                        text = line,
                        color = when {
                            line.startsWith("> ") -> Color(0xFF80CBC4)
                            line.startsWith("[GrokTerm]") -> Color(0xFF90CAF9)
                            line.startsWith("Failed") || line.startsWith("Error") -> Color(0xFFEF9A9A)
                            else -> Color(0xFF00E5FF).copy(alpha = 0.9f)
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            LaunchedEffect(lines.size) {
                if (lines.isNotEmpty()) {
                    listState.animateScrollToItem(lines.lastIndex)
                }
            }

            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == TerminalMode.GROK) "grok> " else "$ ",
                    color = Color(0xFF00E5FF),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF00E5FF)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { sendCommand(input) }
                    ),
                    singleLine = true,
                    enabled = isRunning
                )
                IconButton(
                    onClick = { sendCommand(input) },
                    enabled = isRunning
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (isRunning) Color(0xFF00E5FF) else Color.Gray
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopSession()
        }
    }
}
