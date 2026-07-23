package com.finecomputer.grokterm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.finecomputer.grokterm.data.ApiKeyStore
import com.finecomputer.grokterm.data.GrokBinaryManager
import com.finecomputer.grokterm.ui.terminal.XtermController
import com.finecomputer.grokterm.ui.terminal.XtermTerminal
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
 * Full TUI-capable terminal screen powered by xterm.js.
 *
 * - Dual mode: Shell / Grok
 * - Direct launch of patched Grok binary
 * - XAI_API_KEY injection
 * - Proper ANSI / color / mouse / resize support via xterm.js
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val binaryManager = remember { GrokBinaryManager(context) }
    val apiKeyStore = remember { ApiKeyStore(context) }

    var mode by remember { mutableStateOf(TerminalMode.SHELL) }
    var process by remember { mutableStateOf<Process?>(null) }
    var writer by remember { mutableStateOf<OutputStreamWriter?>(null) }
    var readerJob by remember { mutableStateOf<Job?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Idle") }
    var controller by remember { mutableStateOf<XtermController?>(null) }

    fun writeToTerm(data: String) {
        controller?.write(data)
    }

    fun stopSession() {
        readerJob?.cancel()
        readerJob = null
        try { writer?.close() } catch (_: Exception) {}
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
                    writeToTerm("\r\n\u001b[36m[GrokTerm]\u001b[0m Launching patched Grok binary…\r\n")
                    ProcessBuilder(binaryManager.binaryPath)
                } else {
                    if (targetMode == TerminalMode.GROK) {
                        writeToTerm("\r\n\u001b[33m[GrokTerm]\u001b[0m Grok binary not ready — falling back to shell.\r\n")
                        writeToTerm("Place aarch64 musl binary or use Phase 1 Termux installer.\r\n")
                    }
                    writeToTerm("\r\n\u001b[36m[GrokTerm]\u001b[0m Shell session started.\r\n")
                    ProcessBuilder("/system/bin/sh", "-")
                }

                pb.redirectErrorStream(true)
                val env = pb.environment()
                env["HOME"] = context.filesDir.absolutePath
                env["TERM"] = "xterm-256color"
                env["COLORTERM"] = "truecolor"
                env["LANG"] = "en_US.UTF-8"

                apiKeyStore.getApiKey()?.let { key ->
                    env["XAI_API_KEY"] = key
                    writeToTerm("\u001b[36m[GrokTerm]\u001b[0m XAI_API_KEY injected.\r\n")
                }

                pb.directory(context.filesDir)

                val p = pb.start()
                process = p
                writer = OutputStreamWriter(p.outputStream, Charsets.UTF_8)

                withContext(Dispatchers.Main) {
                    status = if (targetMode == TerminalMode.GROK) "Grok running" else "Shell running"
                    controller?.focus()
                }

                // Stream raw output to xterm (preserves ANSI)
                val reader = BufferedReader(InputStreamReader(p.inputStream, Charsets.UTF_8))
                val buffer = CharArray(4096)

                readerJob = scope.launch(Dispatchers.IO) {
                    try {
                        while (isActive) {
                            val n = reader.read(buffer)
                            if (n == -1) break
                            if (n > 0) {
                                val chunk = String(buffer, 0, n)
                                withContext(Dispatchers.Main) {
                                    writeToTerm(chunk)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            withContext(Dispatchers.Main) {
                                writeToTerm("\r\n\u001b[31m[reader] ${e.message}\u001b[0m\r\n")
                            }
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            isRunning = false
                            status = "Session ended"
                        }
                    }
                }

                p.waitFor()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    writeToTerm("\r\n\u001b[31mFailed to start session: ${e.message}\u001b[0m\r\n")
                    isRunning = false
                    status = "Error"
                }
            }
        }
    }

    fun onUserInput(data: String) {
        scope.launch(Dispatchers.IO) {
            try {
                writer?.apply {
                    write(data)
                    flush()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    writeToTerm("\r\n\u001b[31mWrite error: ${e.message}\u001b[0m\r\n")
                }
            }
        }
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
                    IconButton(
                        onClick = { startSession(TerminalMode.GROK) }
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start Grok",
                            tint = if (binaryManager.isReady) Color(0xFF00E5FF) else Color.Gray
                        )
                    }
                    IconButton(onClick = { startSession(TerminalMode.SHELL) }) {
                        Icon(Icons.Default.Terminal, contentDescription = "Shell")
                    }
                    IconButton(onClick = { controller?.clear() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
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
            XtermTerminal(
                modifier = Modifier.fillMaxSize(),
                onReady = { ctrl ->
                    controller = ctrl
                    // Auto-start shell once the terminal is ready
                    startSession(TerminalMode.SHELL)
                },
                onUserInput = { data -> onUserInput(data) }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopSession()
        }
    }
}
