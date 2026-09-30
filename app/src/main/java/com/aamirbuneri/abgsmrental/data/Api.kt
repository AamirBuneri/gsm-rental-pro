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

/**
 * An error to show the user. [code] is the HTTP status (0 = no connection).
 * [state] is the site's reason when it has one: verify_email, pending, disabled, maintenance, team_off, no_permission …
 */
class ApiException(val code: Int, message: String, val state: String? = null) : Exception(message) {
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

    suspend fun register(site: String, form: Map<String, String>): RegisterResult =
        call("POST", "/auth/register", RegisterResult.serializer(), site = site, auth = false, body = buildJsonObject {
            form.forEach { (k, v) -> put(k, if (k == "password") v else v.trim()) }
            put("device", deviceName())
            put("app_version", BuildConfig.VERSION_NAME)
        })

    suspend fun resendVerification(site: String, username: String, password: String): Message =
        call("POST", "/auth/resend", Message.serializer(), site = site, auth = false, body = buildJsonObject {
            put("username", username.trim())
            put("password", password)
        })

    suspend fun forgotPassword(site: String, email: String): Message =
        call("POST", "/auth/forgot", Message.serializer(), site = site, auth = false, body = buildJsonObject { put("email", email.trim()) })

    /** Who is signed in — role and staff permissions (3.4+). */
    suspend fun me(): UserBrief = call("GET", "/auth/me", UserBrief.serializer())

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

    // ── owner / staff (3.4+) ───────────────────────────────────────────────

    suspend fun adminDashboard(): AdminDashboard = call("GET", "/admin/dashboard", AdminDashboard.serializer())

    suspend fun adminNotifications(beforeId: Int = 0, limit: Int = 30): NoticePage =
        call("GET", "/admin/notifications", NoticePage.serializer(), query = buildMap {
            put("limit", "$limit")
            if (beforeId > 0) put("before_id", "$beforeId")
        })

    suspend fun adminMarkRead(id: Int? = null): Unread =
        call("POST", "/admin/notifications/read", Unread.serializer(), body = buildJsonObject { if (id != null) put("id", id) })

    suspend fun adminRentals(status: String, q: String = "", page: Int = 1): Page<AdminRental> =
        call("GET", "/admin/rentals", Page.serializer(AdminRental.serializer()), query = mapOf("status" to status, "q" to q, "page" to "$page"))

    suspend fun adminRental(id: Int): AdminRental = call("GET", "/admin/rentals/$id", AdminRental.serializer())

    suspend fun adminExtend(id: Int, minutes: Int): AdminRental =
        call("POST", "/admin/rentals/$id/extend", AdminRental.serializer(), body = buildJsonObject { put("minutes", minutes) })

    suspend fun adminCloseRental(id: Int, reason: String): AdminRental =
        call("POST", "/admin/rentals/$id/close", AdminRental.serializer(), body = buildJsonObject { put("reason", reason) })

    suspend fun adminOrders(status: String, q: String = "", page: Int = 1): Page<AdminOrder> =
        call("GET", "/admin/orders", Page.serializer(AdminOrder.serializer()), query = mapOf("status" to status, "q" to q, "page" to "$page"))

    suspend fun adminOrder(id: Int): AdminOrder = call("GET", "/admin/orders/$id", AdminOrder.serializer())

    suspend fun adminOrderSecret(id: Int): RemoteLogin = call("GET", "/admin/orders/$id/secret", RemoteLogin.serializer())

    suspend fun adminOrderStart(id: Int): AdminOrder = call("POST", "/admin/orders/$id/start", AdminOrder.serializer(), body = JsonObject(emptyMap()))

    suspend fun adminOrderComplete(id: Int, result: String): AdminOrder =
        call("POST", "/admin/orders/$id/complete", AdminOrder.serializer(), body = buildJsonObject { put("result", result) })

    suspend fun adminOrderQuote(id: Int, amount: Double, note: String): AdminOrder =
        call("POST", "/admin/orders/$id/quote", AdminOrder.serializer(), body = buildJsonObject { put("amount", amount); put("note", note) })

    suspend fun adminOrderClose(id: Int, status: String, refund: Double, reason: String): AdminOrder =
        call("POST", "/admin/orders/$id/close", AdminOrder.serializer(), body = buildJsonObject {
            put("status", status); put("refund", refund); put("reason", reason)
        })

    suspend fun adminResellers(filter: String, q: String = "", page: Int = 1): Page<AdminReseller> =
        call("GET", "/admin/resellers", Page.serializer(AdminReseller.serializer()), query = mapOf("f" to filter, "q" to q, "page" to "$page"))

    suspend fun adminReseller(id: Int): AdminReseller = call("GET", "/admin/resellers/$id", AdminReseller.serializer())

    suspend fun adminCredit(id: Int, type: String, amount: Double, note: String): AdminReseller =
        call("POST", "/admin/resellers/$id/credit", AdminReseller.serializer(), body = buildJsonObject {
            put("type", type); put("amount", amount); put("note", note)
        })

    /** toggle | verify | reject | confirm-email */
    suspend fun adminResellerAction(id: Int, action: String, reason: String = ""): AdminReseller =
        call("POST", "/admin/resellers/$id/$action", AdminReseller.serializer(), body = buildJsonObject { if (reason.isNotBlank()) put("reason", reason) })

    suspend fun adminTools(): List<AdminTool> = call("GET", "/admin/tools", ListSerializer(AdminTool.serializer()))

    suspend fun adminSlots(toolId: Int): ToolSlots = call("GET", "/admin/tools/$toolId/slots", ToolSlots.serializer())

    suspend fun adminSlotSecret(id: Int): ToolLogin = call("GET", "/admin/slots/$id/secret", ToolLogin.serializer())

    suspend fun adminSlotPassword(id: Int, password: String): AdminSlot =
        call("POST", "/admin/slots/$id/password", AdminSlot.serializer(), body = buildJsonObject { put("password", password) })

    suspend fun adminSlotToggle(id: Int): AdminSlot = call("POST", "/admin/slots/$id/toggle", AdminSlot.serializer(), body = JsonObject(emptyMap()))

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
            var msg = (root["error"] as? JsonPrimitive)?.contentOrNull ?: "Something went wrong ($code)."
            val state = (root["state"] as? JsonPrimitive)?.contentOrNull
            // an older site doesn't know the newer endpoints
            if (code == 404 && root["ok"] == null) msg = "Your site needs an update for this (GSM Rental Pro 3.4 or newer)."
            if (code == 401 && auth && signOutOn401) onSignedOut()
            throw ApiException(if (code in 200..299) 400 else code, msg, state)
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
