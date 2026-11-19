package com.cookbookanything.app.data.text

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

/**
 * Loads a page in the phone's own browser engine, for sites that refuse plain requests from apps or
 * draw the recipe with JavaScript. Nothing is shown; the page's HTML is handed back once [accept]
 * likes it, or the last version of it when the time runs out.
 */
@Singleton
class BrowserPageLoader @Inject constructor(@ApplicationContext private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun load(url: String, accept: (String) -> Boolean): String? = withContext(Dispatchers.Main) {
        val done = CompletableDeferred<String>()
        var last: String? = null
        val webView = WebView(context)
        try {
            // A bot check or a single-page site needs JavaScript to produce the page at all.
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    view.evaluateJavascript("document.documentElement.outerHTML") { json ->
                        val html = runCatching { Json.parseToJsonElement(json).jsonPrimitive.content }.getOrNull() ?: return@evaluateJavascript
                        last = html
                        if (accept(html)) done.complete(html)
                    }
                }
            }
            webView.loadUrl(url)
            withTimeoutOrNull(TIMEOUT_MS) { done.await() } ?: last
        } finally {
            webView.destroy()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 25_000L
    }
}
