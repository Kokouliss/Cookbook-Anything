package com.cookbookanything.app.data.text

import android.content.Context
import android.content.pm.PackageManager
import com.google.firebase.FirebaseApp
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Calls Google APIs (the Gemini models) with the project's Firebase API key. Each API
 * has to be switched on in that project; the key should be restricted to this app (package name and
 * signing certificate), which the two X-Android headers make possible.
 */
@Singleton
class GoogleApi @Inject constructor(@ApplicationContext private val context: Context) {

    /** False in a build without the Firebase config file. */
    val available: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    /** POSTs [body] as JSON and returns the JSON answer. Throws [IOException] with the API's message when it refuses. */
    fun post(endpoint: String, body: JsonObject, service: String, timeoutMs: Int = TIMEOUT_MS): JsonObject {
        val apiKey = FirebaseApp.getInstance().options.apiKey
        val connection = URL("$endpoint?key=$apiKey").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("X-Android-Package", context.packageName)
            connection.setRequestProperty("X-Android-Cert", signingCertificateSha1())
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            val text = (if (code < 400) connection.inputStream else connection.errorStream)?.bufferedReader()?.readText().orEmpty()
            // An HTML error page from a proxy or captive portal is a failed call, not a crash.
            val json = runCatching { Json.parseToJsonElement(text.ifBlank { "{}" }).jsonObject }
                .getOrElse { throw IOException("$service answered $code: ${text.take(200)}") }
            if (code >= 400) {
                val message = json["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                throw IOException("$service answered $code: ${message ?: text.take(200)}")
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    /** The SHA-1 of the app's signing certificate, as Google's Android key restriction wants it. */
    private fun signingCertificateSha1(): String {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val certificate = info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray() ?: return ""
        return MessageDigest.getInstance("SHA-1").digest(certificate).joinToString("") { "%02X".format(it) }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000
    }
}
