package com.cookbookanything.app.data.text

import android.content.Context
import android.content.pm.PackageManager
import com.cookbookanything.app.BuildConfig
import com.google.firebase.FirebaseApp
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Calls the Gemini models with the Gemini API key the build was given (from Google AI Studio), or else
 * the project's Firebase API key. The key should be restricted to this app (package name and signing
 * certificate), which the two X-Android headers make possible.
 */
@Singleton
class GoogleApi @Inject constructor(@ApplicationContext private val context: Context) {

    /** False in a build with neither a Gemini key nor the Firebase config file. */
    val available: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotEmpty() || FirebaseApp.getApps(context).isNotEmpty()

    /** Google's refusal, with the HTTP status, so a caller can tell a retired model (404) from the rest. */
    class Refused(val code: Int, message: String) : IOException(message)

    /** POSTs [body] as JSON and returns the JSON answer. Throws [Refused] with the API's message when it refuses. */
    fun post(endpoint: String, body: JsonObject, service: String, timeoutMs: Int = TIMEOUT_MS): JsonObject {
        // Google refuses the Firebase project for Gemini (403 "Permission denied on the service"), so a key from AI Studio comes first.
        val apiKey = BuildConfig.GEMINI_API_KEY.ifEmpty { FirebaseApp.getInstance().options.apiKey }
        val connection = URL("$endpoint?key=$apiKey").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("X-Android-Package", context.packageName)
            connection.setRequestProperty("X-Android-Cert", signingCertificateSha1())
            // Streamed, so the write lasts as long as the upload and [timeoutMs] is all Google's own time.
            val bytes = body.toString().toByteArray()
            connection.setFixedLengthStreamingMode(bytes.size)
            val started = System.currentTimeMillis()
            connection.outputStream.use { it.write(bytes) }
            val sent = (System.currentTimeMillis() - started) / 1000
            val code = try {
                connection.responseCode
            } catch (e: SocketTimeoutException) {
                // Says which side was slow, the phone's upload or the model.
                throw IOException("$service did not answer within ${timeoutMs / 1000} s (${bytes.size / 1024} KB sent in $sent s)", e)
            }
            val text = (if (code < 400) connection.inputStream else connection.errorStream)?.bufferedReader()?.readText().orEmpty()
            // An HTML error page from a proxy or captive portal is a failed call, not a crash.
            val json = runCatching { Json.parseToJsonElement(text.ifBlank { "{}" }).jsonObject }
                .getOrElse { throw Refused(code, "$service answered $code: ${text.take(200)}") }
            if (code >= 400) {
                val message = json["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                throw Refused(code, "$service answered $code: ${message ?: text.take(200)}")
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
