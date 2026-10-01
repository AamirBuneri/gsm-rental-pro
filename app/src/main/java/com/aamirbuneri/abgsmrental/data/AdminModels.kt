package com.aamirbuneri.abgsmrental.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// Answers of the site's /api/v1/admin/… (GSM Rental Pro 3.4+), used by the owner and staff.

object Perm {
    const val RENTALS = "rentals"
    const val TOOLS = "tools"
    const val SERVICES = "services"
    const val RESELLERS = "resellers"
    const val MONEY = "money"
    const val CATALOG = "catalog"
    const val CHAT = "chat"
    const val WEBSITE = "website"
}

@Serializable
data class SlotSummary(val free: Int = 0, val busy: Int = 0, val total: Int = 0)

@Serializable
data class RentalCounts(val active: Int = 0, val today: Int = 0)

@Serializable
data class DayValue(val label: String = "", val value: Double = 0.0, val count: Int = 0)

@Serializable
data class Sales(
    val today: Double = 0.0,
    val yesterday: Double = 0.0,
    @SerialName("month_revenue") val monthRevenue: Double = 0.0,
    @SerialName("month_profit") val monthProfit: Double = 0.0,
    val outstanding: Double = 0.0,
    val week: List<DayValue> = emptyList(),
)

@Serializable
data class ServiceCounts(
    val ready: Int = 0,
    @SerialName("need_quote") val needQuote: Int = 0,
    val quoted: Int = 0,
    @SerialName("in_progress") val inProgress: Int = 0,
    val today: Int = 0,
) {
    val open: Int get() = ready + needQuote + quoted + inProgress
}

@Serializable
data class ExpiringAccount(
    @SerialName("slot_id") val slotId: Int = 0,
    val slot: String = "",
    val tool: String = "",
    @SerialName("tool_id") val toolId: Int = 0,
    @SerialName("expires_at") val expiresAt: String = "",
)

@Serializable
data class AdminDashboard(
    val me: UserBrief = UserBrief(),
    val currency: String = "PKR",
    val slots: SlotSummary = SlotSummary(),
    val rentals: RentalCounts = RentalCounts(),
    val resellers: Int = 0,
    val unread: Int = 0,
    @SerialName("services_enabled") val servicesEnabled: Boolean = false,
    val sales: Sales? = null,
    val services: ServiceCounts? = null,
    @SerialName("pending_registrations") val pendingRegistrations: Int? = null,
    @SerialName("pending_returns") val pendingReturns: Int? = null,
    val live: List<AdminRental> = emptyList(),
    @SerialName("expiring_accounts") val expiringAccounts: List<ExpiringAccount> = emptyList(),
    val queue: List<AdminOrder> = emptyList(),
)

@Serializable
data class HistoryNote(val body: String = "", val by: String = "", val at: String = "")

@Serializable
data class AdminRental(
    val id: Int = 0,
    val number: String = "",
    val tool: String = "",
    @SerialName("tool_id") val toolId: Int = 0,
    val slot: String = "",
    val plan: String = "",
    val minutes: Int = 0,
    val price: Double = 0.0,
    val currency: String = "PKR",
    val status: String = "",
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("seconds_left") val secondsLeft: Long = 0,
    val renter: String = "",
    val reseller: String = "",
    @SerialName("reseller_id") val resellerId: Int? = null,
    @SerialName("walk_in") val walkIn: Boolean = false,
    val color: String = "",
    val image: String? = null,
    // detail only
    val login: ToolLogin? = null,
    val notes: String = "",
    @SerialName("close_reason") val closeReason: String = "",
    val history: List<HistoryNote> = emptyList(),
    val message: String = "",
) {
    @Transient
    val endsAtMs: Long = System.currentTimeMillis() + secondsLeft * 1000L
    val running: Boolean get() = status == "active" && endsAtMs > System.currentTimeMillis()
}

@Serializable
data class AdminOrder(
    val id: Int = 0,
    val number: String = "",
    val service: String = "",
    val status: String = "",
    @SerialName("status_label") val statusLabel: String = "",
    val price: Double = 0.0,
    val currency: String = "PKR",
    val paid: Boolean = false,
    val quote: Boolean = false,
    @SerialName("needs_quote") val needsQuote: Boolean = false,
    val device: String = "",
    val reseller: String = "",
    @SerialName("reseller_username") val resellerUsername: String = "",
    @SerialName("reseller_id") val resellerId: Int? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val color: String = "",
    val image: String? = null,
    val unread: Int = 0,
    // detail only
    val details: JsonElement? = null,
    val note: String = "",
    val result: String? = null,
    @SerialName("remote_app") val remoteApp: String = "",
    @SerialName("has_remote") val hasRemote: Boolean = false,
    @SerialName("reseller_phone") val resellerPhone: String = "",
    @SerialName("eta_minutes") val etaMinutes: Int = 0,
    @SerialName("quoted_at") val quotedAt: String? = null,
    @SerialName("can_start") val canStart: Boolean = false,
    @SerialName("can_quote") val canQuote: Boolean = false,
    @SerialName("can_complete") val canComplete: Boolean = false,
    @SerialName("can_close") val canClose: Boolean = false,
    val message: String = "",
) {
    val detailPairs: List<Pair<String, String>>
        get() = (details as? JsonObject)?.entries
            ?.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }?.let { k to it } }
            ?: emptyList()
    val open: Boolean get() = status in setOf("pending", "quoted", "in_progress")
}

@Serializable
data class RemoteLogin(val app: String = "", val id: String = "", val password: String = "")

@Serializable
data class LedgerLine(
    val id: Int = 0,
    val type: String = "",
    val amount: Double = 0.0,
    @SerialName("balance_after") val balanceAfter: Double = 0.0,
    val currency: String = "PKR",
    val description: String = "",
    @SerialName("created_at") val createdAt: String = "",
) {
    val isCredit: Boolean get() = type == "credit" || type == "refund" || type == "topup"
}

@Serializable
data class AdminReseller(
    val id: Int = 0,
    val username: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val company: String = "",
    val balance: Double = 0.0,
    val currency: String = "PKR",
    val tier: String = "",
    @SerialName("credit_allowed") val creditAllowed: Boolean = false,
    val active: Boolean = true,
    val verified: Boolean = true,
    @SerialName("email_verified") val emailVerified: Boolean = true,
    /** active | pending | email | disabled */
    val state: String = "active",
    @SerialName("active_rentals") val activeRentals: Int = 0,
    val online: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    // detail only
    @SerialName("total_spent") val totalSpent: Double = 0.0,
    @SerialName("discount_percent") val discountPercent: Double = 0.0,
    @SerialName("credit_limit") val creditLimit: Double? = null,
    @SerialName("last_login_at") val lastLoginAt: String? = null,
    @SerialName("rentals_total") val rentalsTotal: Int = 0,
    @SerialName("orders_total") val ordersTotal: Int = 0,
    val twofa: Boolean = false,
    @SerialName("can_money") val canMoney: Boolean = false,
    @SerialName("recent_rentals") val recentRentals: List<AdminRental> = emptyList(),
    val ledger: List<LedgerLine> = emptyList(),
    val message: String = "",
) {
    val displayName: String get() = name.ifBlank { username }
}

@Serializable
data class Page<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val pages: Int = 1,
    val total: Int = 0,
    val counts: Map<String, Int> = emptyMap(),
)

@Serializable
data class AdminTool(
    val id: Int = 0,
    val name: String = "",
    val color: String = "",
    val image: String? = null,
    val active: Boolean = true,
    val total: Int = 0,
    val busy: Int = 0,
    val free: Int = 0,
    val disabled: Int = 0,
    val expiring: Int = 0,
)

@Serializable
data class AdminSlot(
    val id: Int = 0,
    val name: String = "",
    val tool: String = "",
    @SerialName("tool_id") val toolId: Int = 0,
    /** available | busy | disabled | expired */
    val status: String = "",
    val username: String = "",
    val email: String = "",
    @SerialName("account_expires_at") val accountExpiresAt: String? = null,
    val renter: String? = null,
    @SerialName("rental_id") val rentalId: Int? = null,
    @SerialName("rental_number") val rentalNumber: String? = null,
    @SerialName("rental_ends_at") val rentalEndsAt: String? = null,
    @SerialName("seconds_left") val secondsLeft: Long = 0,
    val notes: String = "",
    val message: String = "",
) {
    @Transient
    val endsAtMs: Long = System.currentTimeMillis() + secondsLeft * 1000L
}

@Serializable
data class ToolBrief(val id: Int = 0, val name: String = "", val color: String = "", val image: String? = null, val active: Boolean = true)

@Serializable
data class ToolSlots(val tool: ToolBrief = ToolBrief(), val slots: List<AdminSlot> = emptyList())
