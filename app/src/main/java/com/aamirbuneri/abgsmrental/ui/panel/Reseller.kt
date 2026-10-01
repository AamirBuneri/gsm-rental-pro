package com.aamirbuneri.abgsmrental.ui.panel

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.BridgeResult
import com.aamirbuneri.abgsmrental.data.bool
import com.aamirbuneri.abgsmrental.data.form
import com.aamirbuneri.abgsmrental.data.int
import com.aamirbuneri.abgsmrental.data.list
import com.aamirbuneri.abgsmrental.data.obj
import com.aamirbuneri.abgsmrental.data.str
import com.aamirbuneri.abgsmrental.data.text
import com.aamirbuneri.abgsmrental.ui.admin.rememberToast
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.InfoRow
import com.aamirbuneri.abgsmrental.ui.components.SecondaryButton
import com.aamirbuneri.abgsmrental.ui.components.StatusPill
import com.aamirbuneri.abgsmrental.ui.components.Tone
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.ago
import com.aamirbuneri.abgsmrental.ui.util.copy
import com.aamirbuneri.abgsmrental.ui.util.dateTime
import com.aamirbuneri.abgsmrental.ui.util.money
import kotlinx.serialization.json.JsonElement

// ── returns ────────────────────────────────────────────────────────────────

@Composable
fun MyReturnsPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_myreturns") { PageVM(api, "/reseller/returns") }
    Panel("Returns", ctx.nav, vm) { d ->
        item {
            Text("Tool didn’t work? Open the running rental and tap “Didn’t work — ask for a refund”. The team checks it and refunds your wallet.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val rows = d.list("rows")
        if (rows.isEmpty()) item { EmptyState(Icons.AutoMirrored.Outlined.Undo, "No returns", "Your refund requests show up here.") }
        items(rows, key = { "rt" + it.int("id") }) { t ->
            val st = t.str("status")
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${t.str("tool_name")} · ${t.str("rental_number")}", style = MaterialTheme.typography.titleSmall)
                        Text("Asked ${ago(t.str("requested_at").ifBlank { t.str("created_at") })}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusPill(when (st) { "approved" -> "Refunded"; "rejected" -> "Rejected"; else -> "Waiting" }, tone(if (st == "pending") "pending" else st))
                }
                Spacer(Modifier.height(6.dp))
                Text("“${t.str("reason")}”", style = MaterialTheme.typography.bodyMedium)
                if (st == "approved" && t.str("refund_amount").isNotBlank()) InfoRow("Refund", money(t.str("refund_amount").toDoubleOrNull() ?: 0.0, t.str("currency")), AB.brand.success)
                if (t.str("admin_note").isNotBlank()) InfoRow("Note", t.str("admin_note"))
            }
        }
    }
}

/** "Didn’t work" on a running rental. */
@Composable
fun RequestReturnPanel(ctx: PanelCtx, rentalId: Int, title: String) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_retreq$rentalId") { PageVM(api, "/reseller/returns") }
    val toast = rememberToast()
    val f = remember { FormState() }
    Panel("Ask for a refund", ctx.nav, vm) { _ ->
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(title.ifBlank { "Rental #$rentalId" }, style = MaterialTheme.typography.titleSmall)
                Text("Tell the team what went wrong. If they approve, the rental ends and the refund goes back to your wallet.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { TextIn(f, "reason", "What happened?", lines = 4, hint = "e.g. Login says “account expired”") }
        item {
            GradientButton("Send request", {
                if (f["reason"].trim().length < 5) toast("Write a few words about the problem.")
                else vm.submit("/reseller/rentals/$rentalId/return", f.json(), toast, reload = false) { if (it.ok) ctx.replace("myreturns") }
            }, Modifier.fillMaxWidth(), loading = vm.busy)
        }
    }
}

// ── profile, password, 2FA (resellers and the team) ────────────────────────

@Composable
fun ProfilePanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_profile") { PageVM(api, "/profile") }
    val toast = rememberToast()
    val f = remember { FormState() }
    val pw = remember { FormState() }
    var editing by remember { mutableStateOf(false) }
    var changing by remember { mutableStateOf(false) }
    Panel("Profile & security", ctx.nav, vm) { d ->
        val me = d.obj("me")
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.aamirbuneri.abgsmrental.ui.admin.PersonAvatar(me.str("full_name").ifBlank { me.str("username") }, 52.dp)
                    Spacer(Modifier.padding(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(me.str("full_name").ifBlank { me.str("username") }, style = MaterialTheme.typography.titleMedium)
                        Text("@${me.str("username")} · ${me.str("role")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                InfoRow("Email", me.str("email") + if (!me.bool("email_verified")) " (not confirmed)" else "")
                InfoRow("Phone", me.str("phone").ifBlank { "—" })
                if (me.str("company_name").isNotBlank()) InfoRow("Shop", me.str("company_name"))
                if (me.str("created_at").isNotBlank()) InfoRow("Member since", dateTime(me.str("created_at")))
            }
        }
        item {
            IconLine(Icons.Outlined.Person, "Edit details", "Name, phone, email") {
                f.values.clear(); listOf("full_name", "phone", "email").forEach { k -> f[k] = me.str(k) }; editing = true
            }
        }
        item { IconLine(Icons.Outlined.Lock, "Change password", "Other devices are signed out") { pw.values.clear(); changing = true } }
        item {
            IconLine(Icons.Outlined.Shield, "Two-step sign-in", if (me.bool("totp_enabled")) "On — authenticator app" else "Off — add a code from your phone",
                tint = if (me.bool("totp_enabled")) AB.brand.success else AB.brand.warning) { ctx.go("twofa") }
        }
        val logins = d.list("logins")
        if (logins.isNotEmpty()) {
            item { Section("Recent sign-ins") }
            items(logins.take(8), key = { "lg" + it.int("id") }) { l ->
                Line(
                    if (l.bool("success") || l.str("success").isBlank()) "Signed in" else "Failed attempt",
                    listOfNotNull(l.str("ip_address").ifBlank { null }, l.str("user_agent").take(60).ifBlank { null }, ago(l.str("created_at"))).joinToString(" · "),
                    leading = { Icon(Icons.Outlined.Devices, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                )
            }
        }
    }
    Sheet(editing, "Edit details", { editing = false }, action = "Save", busy = vm.busy, onAction = {
        vm.submit("/profile", f.json(extra = mapOf("action" to "profile")), toast) { if (it.ok) editing = false }
    }) {
        TextIn(f, "full_name", "Full name")
        TextIn(f, "phone", "Phone / WhatsApp", keyboard = KeyboardType.Phone)
        TextIn(f, "email", "Email", keyboard = KeyboardType.Email)
        if (f["email"].trim().lowercase() != vm.data.obj("me").str("email").lowercase()) TextIn(f, "email_password", "Your password (to change the email)", password = true)
    }
    Sheet(changing, "Change password", { changing = false }, action = "Change", busy = vm.busy, onAction = {
        when {
            pw["new_password"].length < 8 -> toast("The new password needs at least 8 characters.")
            pw["new_password"] != pw["confirm_password"] -> toast("The two new passwords are not the same.")
            else -> vm.submit("/profile", pw.json(extra = mapOf("action" to "password")), toast) { if (it.ok) changing = false }
        }
    }) {
        TextIn(pw, "current_password", "Current password", password = true)
        TextIn(pw, "new_password", "New password", password = true, hint = "At least 8 characters")
        TextIn(pw, "confirm_password", "New password again", password = true)
    }
}

@Composable
fun TwoFaPanel(ctx: PanelCtx) {
    val context = LocalContext.current
    val api = context.container.api
    val vm: PageVM = viewModel(key = "p_twofa") { PageVM(api, "/profile/2fa") }
    val toast = rememberToast()
    val f = remember { FormState() }
    var codes by remember { mutableStateOf<List<String>>(emptyList()) }
    var off by remember { mutableStateOf(false) }
    var newCodes by remember { mutableStateOf(false) }
    fun grabCodes(r: BridgeResult) {
        val c = (r.page?.data ?: r.data).list("codes").mapNotNull { it.text() }
        if (c.isNotEmpty()) codes = c
    }
    LaunchedEffect(vm.data) { vm.data.list("codes").mapNotNull { it.text() }.takeIf { it.isNotEmpty() }?.let { codes = it } }
    Panel("Two-step sign-in", ctx.nav, vm) { d ->
        val on = d.bool("enabled")
        if (codes.isNotEmpty()) item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Your recovery codes", style = MaterialTheme.typography.titleSmall, color = AB.brand.warning)
                Text("Each works once if you lose your phone. Save them somewhere safe — they won’t be shown again.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                codes.chunked(2).forEach { row ->
                    Row { row.forEach { c -> Text(c, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f).padding(vertical = 2.dp)) } }
                }
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Copy all", { copy(context, "Recovery codes", codes.joinToString("\n"), sensitive = true) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.ContentCopy)
            }
        }
        if (on) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Shield, null, tint = AB.brand.success)
                        Spacer(Modifier.padding(4.dp))
                        Text("Two-step sign-in is ON", style = MaterialTheme.typography.titleSmall, color = AB.brand.success)
                    }
                    Text("${d.int("left")} recovery codes left.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { IconLine(Icons.Outlined.Key, "New recovery codes", "The old ones stop working") { f.values.clear(); newCodes = true } }
            if (d.str("policy") != "required") item { IconLine(Icons.Outlined.Lock, "Turn off", null, tint = AB.brand.danger) { f.values.clear(); off = true } }
        } else {
            item {
                Text(if (d.bool("required")) "Your account needs two-step sign-in. Set it up now." else "Add a 6-digit code from an authenticator app (Google Authenticator, Microsoft Authenticator, Authy…) to your password.",
                    style = MaterialTheme.typography.bodyMedium)
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("1. Add this key to your authenticator", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(d.str("secret").chunked(4).joinToString(" "), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable { copy(context, "Setup key", d.str("secret")) })
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("Copy key", { copy(context, "Setup key", d.str("secret")) }, Modifier.weight(1f))
                        SecondaryButton("Open app", {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(d.str("uri"))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                .onFailure { toast("No authenticator app found. Install one, or type the key.") }
                        }, Modifier.weight(1f))
                    }
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("2. Type the 6-digit code it shows", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    TextIn(f, "code", "Code", keyboard = KeyboardType.Number, mono = true)
                    Spacer(Modifier.height(8.dp))
                    GradientButton("Turn on", { vm.submit("/profile/2fa/enable", f.json(), toast) { r -> grabCodes(r); f["code"] = "" } }, Modifier.fillMaxWidth(), loading = vm.busy)
                }
            }
        }
    }
    Sheet(off, "Turn off two-step", { off = false }, action = "Turn off", busy = vm.busy, onAction = {
        vm.submit("/profile/2fa/disable", f.json(), toast) { if (it.ok) { off = false; codes = emptyList() } }
    }) {
        TextIn(f, "password", "Your password", password = true)
        TextIn(f, "code", "Code from the app or a recovery code", mono = true)
    }
    Sheet(newCodes, "New recovery codes", { newCodes = false }, action = "Make new codes", busy = vm.busy, onAction = {
        vm.submit("/profile/2fa/recovery", f.json(), toast) { r -> if (r.ok) { newCodes = false; grabCodes(r) } }
    }) {
        TextIn(f, "code", "Code from the authenticator app", keyboard = KeyboardType.Number, mono = true)
    }
}

// ── reseller API ───────────────────────────────────────────────────────────

@Composable
fun MyApiPanel(ctx: PanelCtx) {
    val context = LocalContext.current
    val api = context.container.api
    val vm: PageVM = viewModel(key = "p_myapi") { PageVM(api, "/reseller/api") }
    val toast = rememberToast()
    val f = remember { FormState() }
    var newKey by remember { mutableStateOf("") }
    var asking by remember { mutableStateOf(false) }
    var keyFor by remember { mutableStateOf<String?>(null) }
    var ips by remember { mutableStateOf(false) }
    fun grab(r: BridgeResult) { (r.page?.data ?: r.data).str("newKey").takeIf { it.isNotBlank() }?.let { newKey = it } }
    Panel("API", ctx.nav, vm) { d ->
        val p = d.obj("profile")
        val st = p.str("api_status", "none")
        item {
            Text("Rent tools and check your balance from your own website or software. Calls use your wallet like the app does.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (newKey.isNotBlank()) item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Your new API key — copy it now", style = MaterialTheme.typography.titleSmall, color = AB.brand.warning)
                Text("It is shown once. If you lose it, make a new one.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text(newKey, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                GradientButton("Copy key", { copy(context, "API key", newKey, sensitive = true) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.ContentCopy)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Api, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.padding(4.dp))
                    Text("Access", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    StatusPill(when (st) { "approved" -> if (p.bool("api_enabled") || p.str("api_enabled").isBlank()) "Approved" else "Turned off"; "pending" -> "Waiting"; "rejected" -> "Not approved"; else -> "Not requested" }, tone(if (st == "none") "neutral" else st))
                }
                if (p.str("api_admin_note").isNotBlank()) InfoRow("Admin note", p.str("api_admin_note"))
                if (st == "approved") {
                    InfoRow("Base address", d.str("base"))
                    InfoRow("Key", if (p.str("api_key_hint").isNotBlank()) "…" + p.str("api_key_hint") else "none yet")
                    InfoRow("Allowed IPs", p.str("api_ips").ifBlank { "any" })
                    if (p.str("api_last_used").isNotBlank()) InfoRow("Last used", ago(p.str("api_last_used")))
                }
            }
        }
        when (st) {
            "approved" -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientButton(if (p.str("api_key_hint").isNotBlank()) "New key" else "Make key", { f.values.clear(); keyFor = "key" }, Modifier.weight(1f), height = 46.dp)
                        if (p.str("api_key_hint").isNotBlank()) SecondaryButton("Revoke", { f.values.clear(); keyFor = "revoke" }, Modifier.weight(1f))
                    }
                }
                item { IconLine(Icons.Outlined.Lock, "Allowed IP addresses", "Only these servers may use your key") { f.values.clear(); f["api_ips"] = p.str("api_ips"); ips = true } }
                item { SecondaryButton("Copy base address", { copy(context, "API address", d.str("base")) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.ContentCopy) }
            }
            "pending" -> item { Text("The team will look at your request soon.", style = MaterialTheme.typography.bodyMedium) }
            else -> item { GradientButton("Ask for API access", { f.values.clear(); asking = true }, Modifier.fillMaxWidth()) }
        }
        val calls = d.list("calls")
        if (calls.isNotEmpty()) {
            item { Section("Recent calls") }
            items(calls, key = { "call" + it.int("id") }) { c ->
                Line("${c.str("method")} ${c.str("endpoint").removePrefix("/api/v1")}", listOfNotNull(c.str("status").ifBlank { null }?.let { "HTTP $it" }, c.str("ip").ifBlank { null }, ago(c.str("created_at"))).joinToString(" · "))
            }
        }
    }
    Sheet(asking, "Ask for API access", { asking = false }, action = "Send", busy = vm.busy, onAction = {
        vm.submit("/reseller/api/request", f.json(), toast) { if (it.ok) asking = false }
    }) {
        TextIn(f, "api_note", "What will you use it for?", lines = 4, hint = "e.g. My own shop website rents tools for customers")
    }
    val k = keyFor
    Sheet(k != null, if (k == "revoke") "Revoke key" else "New API key", { keyFor = null }, action = if (k == "revoke") "Revoke" else "Make key", busy = vm.busy, onAction = {
        vm.submit(if (k == "revoke") "/reseller/api/revoke" else "/reseller/api/key", f.json(), toast) { r -> if (r.ok) { keyFor = null; grab(r); if (k == "revoke") newKey = "" } }
    }) {
        Text(if (k == "revoke") "Your key stops working right away." else "Any old key stops working. Your software needs the new one.", style = MaterialTheme.typography.bodyMedium)
        TextIn(f, "current_password", "Your password", password = true)
    }
    Sheet(ips, "Allowed IP addresses", { ips = false }, action = "Save", busy = vm.busy, onAction = {
        vm.submit("/reseller/api/ips", f.json(), toast) { if (it.ok) ips = false }
    }) {
        Text("Your server’s IP addresses, separated by commas or spaces. Empty = any address.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextIn(f, "api_ips", "IP addresses", lines = 3, mono = true)
    }
}

// ── reviews (reseller) ─────────────────────────────────────────────────────

@Composable
fun MyReviewsPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_myreviews") { PageVM(api, "/reseller/reviews") }
    val toast = rememberToast()
    var rating by remember { mutableStateOf<JsonElement?>(null) }
    val f = remember { FormState(mapOf("rating" to "5", "show_name" to "1")) }
    Panel("Reviews", ctx.nav, vm) { d ->
        val toRate = d.list("toRate")
        if (toRate.isNotEmpty()) {
            item { Section("Rate your orders") }
            items(toRate, key = { "tr" + it.str("kind") + it.int("id") }) { t ->
                Line(t.str("name"), "${t.str("number")} · ${ago(t.str("done"))}",
                    leading = { Icon(Icons.Outlined.StarBorder, null, tint = AB.brand.warning) },
                    onClick = { f.values.clear(); f["rating"] = "5"; f["show_name"] = "1"; rating = t })
            }
        }
        val mine = d.list("mine")
        item { Section("Your reviews") }
        if (mine.isEmpty()) item { EmptyState(Icons.Outlined.Star, "No reviews yet", "Rate finished rentals and services.") }
        items(mine, key = { "my" + it.int("id") }) { r ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★".repeat(r.int("rating")).padEnd(5, '☆'), color = AB.brand.warning, modifier = Modifier.weight(1f))
                    StatusPill(when (r.str("status")) { "approved" -> "On website"; "hidden" -> "Private"; else -> "Waiting" }, when (r.str("status")) { "approved" -> Tone.Success; "hidden" -> Tone.Neutral; else -> Tone.Warning })
                }
                if (r.str("comment").isNotBlank()) Text(r.str("comment"), style = MaterialTheme.typography.bodyMedium)
                if (r.str("reply").isNotBlank()) { Spacer(Modifier.height(4.dp)); Text("Reply: ${r.str("reply")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
            }
        }
    }
    val t = rating
    Sheet(t != null, "Rate ${t?.str("name") ?: ""}", { rating = null }, action = "Send", busy = vm.busy, onAction = {
        if (t != null) vm.submit("/reseller/reviews", f.json(extra = mapOf("kind" to t.str("kind"), "id" to t.str("id"))), toast) { if (it.ok) rating = null }
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { n ->
                IconButton(onClick = { f["rating"] = "$n" }) {
                    Icon(if (n <= (f["rating"].toIntOrNull() ?: 0)) Icons.Outlined.Star else Icons.Outlined.StarBorder, "$n stars", tint = AB.brand.warning)
                }
            }
        }
        TextIn(f, "comment", "Comment (optional)", lines = 4)
        SwitchIn(f, "show_name", "Show my name with the review")
    }
}
