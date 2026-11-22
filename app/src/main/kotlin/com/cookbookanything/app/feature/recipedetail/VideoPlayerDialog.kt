package com.cookbookanything.app.feature.recipedetail

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cookbookanything.app.R
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.core.domain.video.VideoPlayer
import com.cookbookanything.core.domain.video.videoPlayer

/**
 * The recipe's video over the whole screen: the embed player of YouTube, Vimeo, Instagram, TikTok or
 * Facebook, a video file in the browser engine's own player, or any other web page as it is.
 * [link] is a saved video link, so it is already an http(s) address.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerDialog(link: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Color.Black, contentColor = Color.White, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.safeDrawingPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIconButton(R.drawable.ic_close, stringResource(R.string.close), onClick = onDismiss, label = stringResource(R.string.close))
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        // The site's own app (YouTube, Instagram) when it is installed, for full screen and sound controls.
                        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) } },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    ) {
                        Text(stringResource(R.string.video_open))
                    }
                }
                AndroidView(
                    factory = { viewContext ->
                        WebView(viewContext).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.allowFileAccess = false
                            // shortcut: the player's own full-screen button does nothing (no onShowCustomView), the dialog already fills the screen
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                // Players try to jump into their apps (intent:, instagram://); only web pages load here.
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                    request.url.scheme != "http" && request.url.scheme != "https"
                            }
                            val player = videoPlayer(link)
                            // YouTube refuses embeds without a referrer, and asks apps to name themselves with their package as the address.
                            if (player != null) loadDataWithBaseURL("https://${viewContext.packageName}/", playerPage(player), "text/html", "UTF-8", null) else loadUrl(link)
                        }
                    },
                    onRelease = WebView::destroy,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun playerPage(player: VideoPlayer): String {
    val src = player.url.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
    val element = if (player.file) {
        """<video src="$src" controls autoplay playsinline></video>"""
    } else {
        """<iframe src="$src" allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen referrerpolicy="strict-origin-when-cross-origin"></iframe>"""
    }
    return """<!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">""" +
        """<style>html,body{height:100%;margin:0;background:#000}iframe,video{display:block;width:100%;height:100%;border:0}</style>""" +
        """</head><body>$element</body></html>"""
}
