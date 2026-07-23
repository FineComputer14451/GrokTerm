package com.finecomputer.grokterm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finecomputer.grokterm.data.OnboardingStore
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val store = remember { OnboardingStore(context) }
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            Icons.Default.Terminal,
            "GrokTerm",
            "A Termux-like terminal built for xAI Grok Build on Android.\nFull TUI, offline xterm.js, and one-tap workflows."
        ),
        OnboardingPage(
            Icons.Default.Download,
            "Download the binary",
            "On the home screen, tap Download Grok Binary.\nWe fetch the official aarch64 musl build and apply the DNS patch automatically."
        ),
        OnboardingPage(
            Icons.Default.Key,
            "Set your API key",
            "Open Settings and paste your XAI_API_KEY.\nIt is stored encrypted and injected into every Grok session."
        ),
        OnboardingPage(
            Icons.Default.FolderOpen,
            "Pick a project",
            "Select a Production Bible or skills folder.\nGrok will start in that directory when possible."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val p = pages[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        p.icon,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(32.dp))
                    Text(
                        p.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        p.body,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }

            // Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(pages.size) { i ->
                    val selected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            val isLast = pagerState.currentPage == pages.lastIndex

            Button(
                onClick = {
                    if (isLast) {
                        scope.launch {
                            store.setComplete(true)
                            onFinished()
                        }
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (isLast) "Get started" else "Next")
            }

            if (!isLast) {
                TextButton(
                    onClick = {
                        scope.launch {
                            store.setComplete(true)
                            onFinished()
                        }
                    }
                ) {
                    Text("Skip")
                }
            }
        }
    }
}
