package com.cookbookanything.app.data.text

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

/**
 * Loads a page in the phone's own browser engine, for sites that refuse plain requests from apps or
 * draw the recipe with JavaScript. Nothing is shown; the page's HTML is handed back once [accept]
 * likes it, or the last version of it when the time runs out. It is looked at once a second.
 */
@Singleton
class BrowserPageLoader @Inject constructor(@ApplicationContext private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun load(url: String, accept: (String) -> Boolean): String? = withContext(Dispatchers.Main) {
        var finished = false
        var last: String? = null
        val webView = WebView(context)
        try {
            // A bot check or a single-page site needs JavaScript to produce the page at all.
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    finished = true
                }
            }
            webView.loadUrl(url)
            withTimeoutOrNull(TIMEOUT_MS) {
                // Looked at again every second: Instagram and other pages that draw themselves are still empty when they "finish".
                while (true) {
                    delay(POLL_MS)
                    if (!finished) continue
                    val html = webView.html() ?: continue
                    last = html
                    if (withContext(Dispatchers.Default) { accept(html) }) break
                }
            }
            last
        } finally {
            webView.destroy()
        }
    }

    // The address goes in front as a comment, so a report shows where the page ended up (a login wall, say).
    private suspend fun WebView.html(): String? = suspendCancellableCoroutine { continuation ->
        evaluateJavascript("'<!-- ' + location.href + ' -->\\n' + document.documentElement.outerHTML") { json ->
            continuation.resume(runCatching { Json.parseToJsonElement(json).jsonPrimitive.content }.getOrNull())
        }
    }

    private companion object {
        const val TIMEOUT_MS = 25_000L
        const val POLL_MS = 1_000L
    }
}
