package com.aamirbuneri.abgsmrental.data

import android.os.Build
import com.aamirbuneri.abgsmrental.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** An error to show the reseller. [code] is the HTTP status (0 = no connection). */
class ApiException(val code: Int, message: String) : Exception(message) {
    val offline: Boolean get() = code == 0
    val signedOut: Boolean get() = code == 401
}

/**
 * Talks to the site's /api/v1. Always uses the "index.php?r=/api/v1/…" address, which works whether
 * the site has clean URLs switched on or not.
 */
class Api(
    private val prefs: Prefs,
    private val onSignedOut: suspend () -> Unit,
) {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val userAgent = "ABGsmRental-Android/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"

    // ── endpoints ──────────────────────────────────────────────────────────

    suspend fun appInfo(site: String): AppInfo =
        call("GET", "/app/info", AppInfo.serializer(), site = site, auth = false)

    suspend fun login(site: String, username: String, password: String): LoginResult =
        call("POST", "/auth/login", LoginResult.serializer(), site = site, auth = false, body = buildJsonObject {
            put("username", username.trim())
            put("password", password)
            put("device", deviceName())
            put("app_version", BuildConfig.VERSION_NAME)
        })

    suspend fun verify2fa(site: String, challenge: String, code: String): LoginResult =
        call("POST", "/auth/2fa", LoginResult.serializer(), site = site, auth = false, body = buildJsonObject {
            put("challenge", challenge)
            put("code", code.trim())
            put("device", deviceName())
            put("app_version", BuildConfig.VERSION_NAME)
        })

    suspend fun logout() {
        runCatching { call("POST", "/auth/logout", JsonElement.serializer(), body = JsonObject(emptyMap()), signOutOn401 = false) }
    }

    suspend fun dashboard(): Dashboard = call("GET", "/dashboard", Dashboard.serializer())

    suspend fun tools(): List<Tool> = call("GET", "/tools", ListSerializer(Tool.serializer()))

    suspend fun rent(toolId: Int, planId: Int): Rental =
        call("POST", "/rentals", Rental.serializer(), body = buildJsonObject {
            put("tool_id", toolId)
            put("plan_id", planId)
            put("notes", "Rented in the Android app")
        })

    suspend fun rentals(status: String = "all", limit: Int = 50): List<Rental> =
        call("GET", "/rentals", ListSerializer(Rental.serializer()), query = mapOf("status" to status, "limit" to "$limit"))

    suspend fun rental(id: Int): Rental = call("GET", "/rentals/$id", Rental.serializer())

    suspend fun services(): List<Service> = call("GET", "/services", ListSerializer(Service.serializer()))

    suspend fun placeOrder(body: JsonObject): Order = call("POST", "/orders", Order.serializer(), body = body)

    suspend fun orders(limit: Int = 50): List<Order> =
        call("GET", "/orders", ListSerializer(Order.serializer()), query = mapOf("limit" to "$limit"))

    suspend fun order(id: Int): Order = call("GET", "/orders/$id", Order.serializer())

    suspend fun acceptQuote(id: Int, expectedPrice: Double?): Order =
        call("POST", "/orders/$id/accept", Order.serializer(), body = buildJsonObject {
            if (expectedPrice != null) put("expected_price", expectedPrice)
        })

    suspend fun cancelOrder(id: Int, reason: String): Order =
        call("POST", "/orders/$id/cancel", Order.serializer(), body = buildJsonObject { put("reason", reason) })

    suspend fun notifications(beforeId: Int = 0, limit: Int = 30): NoticePage =
        call("GET", "/notifications", NoticePage.serializer(), query = buildMap {
            put("limit", "$limit")
            if (beforeId > 0) put("before_id", "$beforeId")
        })

    suspend fun markRead(id: Int? = null): Unread =
        call("POST", "/notifications/read", Unread.serializer(), body = buildJsonObject { if (id != null) put("id", id) })

    suspend fun wallet(beforeId: Int = 0, limit: Int = 30): WalletPage =
        call("GET", "/wallet", WalletPage.serializer(), query = buildMap {
            put("limit", "$limit")
            if (beforeId > 0) put("before_id", "$beforeId")
        })

    // ── transport ──────────────────────────────────────────────────────────

    private suspend fun <T> call(
        method: String,
        path: String,
        serializer: KSerializer<T>,
        query: Map<String, String> = emptyMap(),
        body: JsonObject? = null,
        site: String? = null,
        auth: Boolean = true,
        signOutOn401: Boolean = true,
    ): T {
        val base = normalizeSite(site ?: prefs.site())
        if (base.isEmpty()) throw ApiException(0, "Set your website address first.")
        val token = if (auth) prefs.token() else null
        if (auth && token.isNullOrEmpty()) {
            if (signOutOn401) onSignedOut()
            throw ApiException(401, "Please sign in.")
        }

        val url = buildString {
            append(base).append("/index.php?r=").append(enc("/api/v1$path"))
            query.forEach { (k, v) -> append('&').append(enc(k)).append('=').append(enc(v)) }
        }
        val req = Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
            .apply {
                if (token != null) {
                    header("Authorization", "Bearer $token")
                    header("X-API-Key", token) // some hosts drop the Authorization header
                }
                val payload = (body ?: JsonObject(emptyMap())).toString()
                if (method == "POST") post(payload.toRequestBody(JSON)) else get()
            }
            .build()

        val (code, text) = withContext(Dispatchers.IO) {
            try {
                http.newCall(req).execute().use { r -> r.code to (r.body?.string().orEmpty()) }
            } catch (e: IOException) {
                throw ApiException(0, "Can’t reach the server. Check your internet connection.")
            }
        }

        val root: JsonObject = runCatching { json.parseToJsonElement(text).jsonObject }.getOrElse {
            throw ApiException(code, when (code) {
                404 -> "This site doesn’t have the app API. Update the site to GSM Rental Pro 3.3 or newer."
                in 500..599 -> "The server had a problem ($code). Try again in a moment."
                else -> "Unexpected answer from the server ($code)."
            })
        }
        val ok = (root["ok"] as? JsonPrimitive)?.booleanOrNull == true
        if (!ok || code >= 400) {
            val msg = (root["error"] as? JsonPrimitive)?.contentOrNull ?: "Something went wrong ($code)."
            if (code == 401 && auth && signOutOn401) onSignedOut()
            throw ApiException(if (code in 200..299) 400 else code, msg)
        }
        return json.decodeFromJsonElement(serializer, root["data"] ?: JsonNull)
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

        /** "aamirbuneri.com/", "https://x.com/index.php?r=/login" → "https://x.com". Keeps sub-folders. */
        fun normalizeSite(input: String): String {
            var s = input.trim()
            if (s.isEmpty()) return ""
            if (!s.startsWith("http://", true) && !s.startsWith("https://", true)) s = "https://$s"
            s = s.substringBefore('#').substringBefore('?')
            s = s.replace(Regex("/index\\.php.*$", RegexOption.IGNORE_CASE), "")
            s = s.replace(Regex("/(login|admin|reseller|install)(/.*)?$", RegexOption.IGNORE_CASE), "")
            return s.trimEnd('/')
        }

        fun deviceName(): String {
            val maker = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
            val model = Build.MODEL.orEmpty()
            return (if (model.startsWith(maker, ignoreCase = true)) model else "$maker $model").trim().take(120)
        }
    }
}
