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
import androidx.compose.material.icons.filled.Send
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
import com.finecomputer.grokterm.data.GrokBinaryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Basic interactive terminal screen.
 * Uses a local shell process for now. Can later be swapped for a full VT100 emulator
 * or direct launch of the patched Grok binary as the shell.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var lines by remember { mutableStateOf(listOf("GrokTerm · basic shell ready", "Type commands below. `grok` available if binary is patched.")) }
    var input by remember { mutableStateOf("") }
    var process by remember { mutableStateOf<Process?>(null) }
    var writer by remember { mutableStateOf<OutputStreamWriter?>(null) }

    // Start a simple shell on first composition
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val pb = ProcessBuilder("/system/bin/sh", "-")
                pb.redirectErrorStream(true)
                pb.environment()["HOME"] = context.filesDir.absolutePath
                pb.environment()["TERM"] = "xterm-256color"
                val p = pb.start()
                process = p
                writer = OutputStreamWriter(p.outputStream)

                val reader = BufferedReader(InputStreamReader(p.inputStream))
                // Simple line reader loop (production would use a proper PTY + emulator)
                while (true) {
                    val line = reader.readLine() ?: break
                    withContext(Dispatchers.Main) {
                        lines = lines + line
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lines = lines + "Shell start failed: ${e.message}"
                }
            }
        }
    }

    fun sendCommand(cmd: String) {
        if (cmd.isBlank()) return
        lines = lines + "> $cmd"
        scope.launch(Dispatchers.IO) {
            try {
                writer?.apply {
                    write(cmd + "\n")
                    flush()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lines = lines + "Error: ${e.message}"
                }
            }
        }
        input = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                        color = Color(0xFF00E5FF).copy(alpha = 0.9f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Auto-scroll
            LaunchedEffect(lines.size) {
                if (lines.isNotEmpty()) {
                    listState.animateScrollToItem(lines.lastIndex)
                }
            }

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
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
                    singleLine = true
                )
                IconButton(onClick = { sendCommand(input) }) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            process?.destroy()
        }
    }
}
