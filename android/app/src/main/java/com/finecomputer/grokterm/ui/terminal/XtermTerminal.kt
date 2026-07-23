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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * xterm.js powered terminal surface — fully offline capable.
 *
 * Loads from `file:///android_asset/xterm/index.html`.
 * Vendor the JS/CSS files into `app/src/main/assets/xterm/` (see assets/xterm/README.md).
 *
 * Communication:
 * - Kotlin → JS  : term.write(data)
 * - JS → Kotlin  : AndroidBridge.onData(data)
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
                // Escape for safe JS string injection
                val escaped = data
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\u001b", "\\x1b")
                webViewRef?.evaluateJavascript(
                    "window.term && window.term.write('$escaped');",
                    null
                )
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
                settings.allowContentAccess = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                // Bridge
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
                        view?.evaluateJavascript("window.term && window.term.focus();", null)
                    }
                }

                // Fully offline — load from packaged assets
                loadUrl("file:///android_asset/xterm/index.html")

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
