package com.aamirbuneri.abgsmrental.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// Shapes of the site's /api/v1 answers (GSM Rental Pro 3.3+). Every field has a default so a newer
// or older site never crashes the app.

@Serializable
data class Support(
    val whatsapp: String = "",
    val phone: String = "",
    val email: String = "",
)

@Serializable
data class CurrencyOption(
    val code: String = "",
    val name: String = "",
    val symbol: String = "",
)

@Serializable
data class AppInfo(
    val app: String = "",
    @SerialName("api_version") val apiVersion: Int = 0,
    val version: String = "",
    val name: String = "",
    val tagline: String = "",
    val currency: String = "PKR",
    @SerialName("app_enabled") val appEnabled: Boolean = true,
    val maintenance: Boolean = false,
    val services: Boolean = false,
    val registration: Boolean = false,
    val website: String = "",
    @SerialName("register_url") val registerUrl: String = "",
    @SerialName("forgot_url") val forgotUrl: String = "",
    val support: Support = Support(),
    /** 3.4+: sign-up inside the app (null = older site → open the website). */
    @SerialName("app_signup") val appSignup: Boolean? = null,
    @SerialName("password_reset") val passwordReset: Boolean? = null,
    @SerialName("team_app") val teamApp: Boolean = false,
    val currencies: List<CurrencyOption> = emptyList(),
)

@Serializable
data class UserBrief(
    val username: String = "",
    val name: String = "",
    val email: String = "",
    /** reseller | admin | staff (3.4+; older sites only let resellers in). */
    val role: String = "reseller",
    val team: Boolean = false,
    val owner: Boolean = false,
    val perms: List<String> = emptyList(),
)

@Serializable
data class RegisterResult(
    /** ready (signed in) | verify_email | pending */
    val state: String = "",
    val token: String? = null,
    val message: String = "",
    val user: UserBrief? = null,
)

@Serializable
data class Message(val message: String = "")

@Serializable
data class LoginResult(
    @SerialName("need_2fa") val need2fa: Boolean = false,
    val token: String? = null,
    val challenge: String? = null,
    val user: UserBrief? = null,
)

@Serializable
data class Account(
    val username: String = "",
    val name: String = "",
    val email: String = "",
    val balance: Double = 0.0,
    val currency: String = "PKR",
    @SerialName("credit_allowed") val creditAllowed: Boolean = false,
    @SerialName("credit_limit") val creditLimit: Double? = null,
    @SerialName("can_spend") val canSpend: Double? = null,
    @SerialName("discount_percent") val discountPercent: Double = 0.0,
) {
    val displayName: String get() = name.ifBlank { username }
}

@Serializable
data class Plan(
    val id: Int = 0,
    val label: String = "",
    val minutes: Int = 0,
    val price: Double = 0.0,
    val currency: String = "PKR",
)

@Serializable
data class Tool(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val color: String = "",
    val image: String? = null,
    val available: Boolean = false,
    val plans: List<Plan> = emptyList(),
    @SerialName("free_slots") val freeSlots: Int? = null,
    @SerialName("next_free_at") val nextFreeAt: String? = null,
)

@Serializable
data class ToolLogin(
    val username: String = "",
    val email: String = "",
    val password: String = "",
)

@Serializable
data class Rental(
    val id: Int = 0,
    val number: String = "",
    val tool: String = "",
    val plan: String = "",
    val minutes: Int = 0,
    val price: Double = 0.0,
    val currency: String = "PKR",
    val status: String = "",
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("seconds_left") val secondsLeft: Long = 0,
    val login: ToolLogin? = null,
    val balance: Double? = null,
    @SerialName("tool_id") val toolId: Int = 0,
    val color: String = "",
) {
    /** Wall-clock moment this rental ends, fixed when the answer arrived (no java.time needed). */
    @Transient
    val endsAtMs: Long = System.currentTimeMillis() + secondsLeft * 1000L

    val running: Boolean get() = status == "active" && endsAtMs > System.currentTimeMillis()
}

@Serializable
data class ServiceField(
    val label: String = "",
    val required: Boolean = false,
)

@Serializable
data class Service(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val color: String = "",
    val image: String? = null,
    val price: Double? = null,
    val currency: String = "PKR",
    val quote: Boolean = false,
    @SerialName("eta_minutes") val etaMinutes: Int = 0,
    @SerialName("needs_remote") val needsRemote: Boolean = false,
    val fields: List<ServiceField> = emptyList(),
)

@Serializable
data class Order(
    val id: Int = 0,
    val number: String = "",
    val service: String = "",
    val status: String = "",
    @SerialName("status_label") val statusLabel: String = "",
    val price: Double? = null,
    val currency: String = "PKR",
    val paid: Boolean = false,
    val device: String = "",
    val result: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("service_id") val serviceId: Int = 0,
    val quote: Boolean = false,
    @SerialName("awaiting_acceptance") val awaitingAcceptance: Boolean = false,
    @SerialName("can_cancel") val canCancel: Boolean = false,
    val note: String = "",
    @SerialName("remote_app") val remoteApp: String = "",
    val details: JsonElement? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("quoted_at") val quotedAt: String? = null,
    val balance: Double? = null,
) {
    /** The filled-in custom fields as label → value. */
    val detailPairs: List<Pair<String, String>>
        get() = (details as? JsonObject)?.entries
            ?.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }?.let { k to it } }
            ?: emptyList()

    val open: Boolean get() = status in setOf("pending", "quoted", "in_progress")
}

@Serializable
data class Stats(
    @SerialName("rentals_total") val rentalsTotal: Int = 0,
    @SerialName("month_rentals") val monthRentals: Int = 0,
    @SerialName("month_spent") val monthSpent: Double = 0.0,
)

@Serializable
data class Dashboard(
    val account: Account = Account(),
    @SerialName("active_rentals") val activeRentals: List<Rental> = emptyList(),
    @SerialName("open_orders") val openOrders: List<Order> = emptyList(),
    val unread: Int = 0,
    val stats: Stats = Stats(),
    val services: Boolean = false,
)

@Serializable
data class Notice(
    val id: Int = 0,
    val title: String = "",
    val message: String = "",
    val type: String = "info",
    val read: Boolean = false,
    @SerialName("rental_id") val rentalId: Int? = null,
    val link: String = "",
    @SerialName("created_at") val createdAt: String = "",
    /** Rich messages (3.5+): picture, a button with a link. */
    val image: String? = null,
    @SerialName("action_label") val actionLabel: String? = null,
    @SerialName("action_url") val actionUrl: String? = null,
    val broadcast: Boolean = false,
) {
    /** Order id when the notification points at a service order. */
    val orderId: Int?
        get() = Regex("""/(?:services/)?orders?/(\d+)""").find(link)?.groupValues?.get(1)?.toIntOrNull()
}

@Serializable
data class NoticePage(
    val unread: Int = 0,
    val items: List<Notice> = emptyList(),
)

@Serializable
data class Unread(val unread: Int = 0)

@Serializable
data class LedgerEntry(
    val id: Int = 0,
    val type: String = "",
    val amount: Double = 0.0,
    @SerialName("balance_after") val balanceAfter: Double = 0.0,
    val currency: String = "PKR",
    val description: String = "",
    @SerialName("reference_type") val referenceType: String = "",
    @SerialName("reference_id") val referenceId: Int? = null,
    @SerialName("created_at") val createdAt: String = "",
) {
    val isCredit: Boolean get() = type == "credit" || type == "refund" || type == "topup"
}

@Serializable
data class WalletPage(
    val balance: Double = 0.0,
    val currency: String = "PKR",
    @SerialName("credit_allowed") val creditAllowed: Boolean = false,
    @SerialName("credit_limit") val creditLimit: Double? = null,
    @SerialName("can_spend") val canSpend: Double? = null,
    @SerialName("topup_whatsapp") val topupWhatsapp: String = "",
    val items: List<LedgerEntry> = emptyList(),
)

@Serializable
data class PingResult(
    @SerialName("latest_id") val latestId: Int = 0,
    val unread: Int = 0,
    val items: List<Notice> = emptyList(),
)
