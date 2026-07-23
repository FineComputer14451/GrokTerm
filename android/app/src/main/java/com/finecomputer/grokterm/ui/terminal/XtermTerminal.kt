package com.finecomputer.grokterm.ui.terminal

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * xterm.js powered terminal surface.
 *
 * Provides proper VT / ANSI rendering, colors, mouse support and resize handling
 * so that Grok Build's full TUI works correctly.
 *
 * Communication:
 * - Kotlin → JS  : term.write(data)
 * - JS → Kotlin  : AndroidBridge.onData(data)  (user keystrokes)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun XtermTerminal(
    modifier: Modifier = Modifier,
    onReady: (XtermController) -> Unit = {},
    onUserInput: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val controller = remember {
        object : XtermController {
            override fun write(data: String) {
                val escaped = data
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\u001b", "\\x1b")
                webViewRef?.evaluateJavascript("window.term && window.term.write('$escaped');", null)
            }

            override fun clear() {
                webViewRef?.evaluateJavascript("window.term && window.term.clear();", null)
            }

            override fun focus() {
                webViewRef?.evaluateJavascript("window.term && window.term.focus();", null)
                webViewRef?.requestFocus()
            }
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(AndroidColor.BLACK)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // Bridge for keystrokes coming from xterm.js
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onData(data: String) {
                        scope.launch(Dispatchers.Main) {
                            onUserInput(data)
                        }
                    }

                    @JavascriptInterface
                    fun onReady() {
                        scope.launch(Dispatchers.Main) {
                            onReady(controller)
                        }
                    }
                }, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Safety: ensure focus after load
                        view?.evaluateJavascript("window.term && window.term.focus();", null)
                    }
                }

                // Load the terminal page (CDN for scaffold; vendor later for offline)
                loadDataWithBaseURL(
                    "https://cdn.jsdelivr.net",
                    XTERM_HTML,
                    "text/html",
                    "UTF-8",
                    null
                )

                webViewRef = this
            }
        },
        modifier = modifier,
        update = { webView ->
            webViewRef = webView
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}

interface XtermController {
    fun write(data: String)
    fun clear()
    fun focus()
}

/**
 * Minimal self-contained xterm.js page.
 * Uses jsDelivr CDN for the scaffold. For production / offline, vendor the assets
 * into app/src/main/assets/ and change the load method.
 */
private const val XTERM_HTML = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/css/xterm.min.css" />
  <style>
    html, body, #terminal {
      margin: 0;
      padding: 0;
      width: 100%;
      height: 100%;
      background: #0a0a0a;
      overflow: hidden;
    }
    .xterm {
      height: 100%;
    }
    .xterm-viewport {
      overflow-y: auto !important;
    }
  </style>
</head>
<body>
  <div id="terminal"></div>

  <script src="https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/lib/xterm.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/@xterm/addon-fit@0.10.0/lib/addon-fit.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/@xterm/addon-web-links@0.11.0/lib/addon-web-links.min.js"></script>

  <script>
    const term = new Terminal({
      cursorBlink: true,
      cursorStyle: 'block',
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      fontSize: 13,
      lineHeight: 1.2,
      theme: {
        background: '#0a0a0a',
        foreground: '#00e5ff',
        cursor: '#00e5ff',
        selectionBackground: '#00363f',
        black: '#000000',
        red: '#ef5350',
        green: '#66bb6a',
        yellow: '#ffee58',
        blue: '#42a5f5',
        magenta: '#ab47bc',
        cyan: '#26c6da',
        white: '#e0e0e0',
        brightBlack: '#546e7a',
        brightRed: '#ef9a9a',
        brightGreen: '#a5d6a7',
        brightYellow: '#fff59d',
        brightBlue: '#90caf9',
        brightMagenta: '#ce93d8',
        brightCyan: '#80deea',
        brightWhite: '#fafafa'
      },
      allowProposedApi: true,
      scrollback: 5000
    });

    const fitAddon = new FitAddon.FitAddon();
    term.loadAddon(fitAddon);
    term.loadAddon(new WebLinksAddon.WebLinksAddon());

    term.open(document.getElementById('terminal'));
    fitAddon.fit();

    // Forward user input to Android
    term.onData(data => {
      if (window.AndroidBridge) {
        AndroidBridge.onData(data);
      }
    });

    // Expose for Kotlin
    window.term = term;
    window.fitAddon = fitAddon;

    // Notify Android that terminal is ready
    if (window.AndroidBridge) {
      AndroidBridge.onReady();
    }

    // Handle resize
    window.addEventListener('resize', () => {
      fitAddon.fit();
    });

    // Initial fit after a short delay (layout settle)
    setTimeout(() => fitAddon.fit(), 100);
    setTimeout(() => fitAddon.fit(), 400);
  </script>
</body>
</html>
"""
