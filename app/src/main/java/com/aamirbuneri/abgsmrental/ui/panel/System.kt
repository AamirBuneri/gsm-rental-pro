package com.aamirbuneri.abgsmrental.ui.panel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.bool
import com.aamirbuneri.abgsmrental.data.dbl
import com.aamirbuneri.abgsmrental.data.form
import com.aamirbuneri.abgsmrental.data.int
import com.aamirbuneri.abgsmrental.data.list
import com.aamirbuneri.abgsmrental.data.obj
import com.aamirbuneri.abgsmrental.data.str
import com.aamirbuneri.abgsmrental.data.text
import com.aamirbuneri.abgsmrental.ui.admin.PersonAvatar
import com.aamirbuneri.abgsmrental.ui.admin.SearchBox
import com.aamirbuneri.abgsmrental.ui.admin.rememberToast
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.InfoRow
import com.aamirbuneri.abgsmrental.ui.components.SecondaryButton
import com.aamirbuneri.abgsmrental.ui.components.Tone
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.ago
import com.aamirbuneri.abgsmrental.ui.util.money
import kotlinx.serialization.json.JsonElement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── staff ──────────────────────────────────────────────────────────────────

val STAFF_PERMS = listOf(
    "rentals" to "Rentals", "tools" to "Tools & slots", "services" to "Service orders", "catalog" to "Service catalog",
    "resellers" to "Resellers", "money" to "Money", "chat" to "Chat & messages", "website" to "Website",
)

@Composable
fun StaffPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_staff") { PageVM(api, "/admin/staff") }
    val toast = rememberToast()
    var editing by remember { mutableStateOf<JsonElement?>(null) }
    var open by remember { mutableStateOf(false) }
    var perms by remember { mutableStateOf(setOf<String>()) }
    val f = remember { FormState() }
    Panel("Staff", ctx.nav, vm, actions = {
        IconButton(onClick = { f.values.clear(); f["is_active"] = "1"; perms = emptySet(); editing = null; open = true }) { Icon(Icons.Outlined.PersonAdd, "Add staff") }
    }) { d ->
        item {
            Text("Team members sign in like you (website or this app) and only see the parts you allow. Settings, backups, staff and the activity log stay owner-only.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val rows = d.list("staff")
        if (rows.isEmpty()) item { EmptyState(Icons.Outlined.Badge, "No staff yet", "Tap + to add a team member.") }
        items(rows, key = { "st" + it.int("id") }) { s ->
            val on = s.bool("is_active")
            val p = s.str("perms").split(',').filter { it.isNotBlank() }
            Line(
                s.str("full_name").ifBlank { s.str("username") },
                "@${s.str("username")} · " + (if (p.isEmpty()) "no access" else p.joinToString { k -> STAFF_PERMS.firstOrNull { it.first == k }?.second ?: k }) +
                    (s.str("last_login").ifBlank { null }?.let { " · last in " + ago(it) } ?: ""),
                leading = { PersonAvatar(s.str("full_name").ifBlank { s.str("username") }, 40.dp) },
                pill = if (on) "Active" to Tone.Success else "Off" to Tone.Neutral,
                menu = listOf<Pair<String, () -> Unit>>(
                    "Edit" to {
                        f.values.clear(); listOf("username", "email", "full_name", "phone").forEach { k -> f[k] = s.str(k) }
                        f["is_active"] = if (on) "1" else "0"; perms = p.toSet(); editing = s; open = true
                    },
                    "Delete" to { vm.submit("/admin/staff/${s.int("id")}/delete", form(), toast) },
                ),
            )
        }
    }
    val e = editing
    Sheet(open, if (e != null) "Edit staff" else "New staff", { open = false }, action = "Save", busy = vm.busy, onAction = {
        vm.submit(if (e != null) "/admin/staff/${e.int("id")}" else "/admin/staff", f.json(lists = mapOf("perms" to perms.toList())), toast) { if (it.ok) open = false }
    }) {
        TextIn(f, "full_name", "Name")
        TextIn(f, "username", "Username")
        TextIn(f, "email", "Email", keyboard = androidx.compose.ui.text.input.KeyboardType.Email)
        TextIn(f, "phone", "Phone", keyboard = androidx.compose.ui.text.input.KeyboardType.Phone)
        TextIn(f, "password", if (e != null) "New password (leave empty to keep)" else "Password", password = true, hint = "At least 8 characters")
        if (e != null) SwitchIn(f, "is_active", "Account active", "Turning it off signs them out everywhere")
        Group("Can open") {
            STAFF_PERMS.forEach { (k, label) ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { perms = if (k in perms) perms - k else perms + k }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = k in perms, onCheckedChange = { perms = if (it) perms + k else perms - k })
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

// ── settings ───────────────────────────────────────────────────────────────

private data class SectionInfo(val key: String, val title: String, val sub: String, val icon: ImageVector)

private val SECTIONS = listOf(
    SectionInfo("general", "General", "Name, time zone, currency, contact, exchange rates", Icons.Outlined.Settings),
    SectionInfo("registration", "Registration", "Sign-ups, approval, default credit limit", Icons.Outlined.PersonAdd),
    SectionInfo("rentals", "Rentals", "Limits, refunds, expiry alerts", Icons.Outlined.Timer),
    SectionInfo("services", "Remote services", "Turn services on or off", Icons.Outlined.SupportAgent),
    SectionInfo("maintenance", "Maintenance", "Close the panel for a while", Icons.Outlined.Build),
    SectionInfo("email", "Email", "SMTP and which emails are sent", Icons.Outlined.Email),
    SectionInfo("whatsapp", "WhatsApp gateway", "Messages through your gateway", Icons.AutoMirrored.Outlined.Chat),
    SectionInfo("security", "Security", "Two-step sign-in rules", Icons.Outlined.Security),
    SectionInfo("api", "API & mobile app", "Reseller API, app sign-in and sign-up", Icons.Outlined.PhoneAndroid),
    SectionInfo("topbar", "Top bar", "Promo bar on the site and panels", Icons.Outlined.Campaign),
    SectionInfo("website", "Website", "Public home page texts", Icons.Outlined.Language),
)

private val LABELS = mapOf(
    "system_name" to "System name", "system_tagline" to "Tagline", "timezone" to "Time zone", "base_currency" to "Base currency",
    "support_phone" to "Support phone", "support_email" to "Support email", "support_whatsapp" to "Support WhatsApp",
    "fx_USD" to "1 USD = (base)", "fx_EUR" to "1 EUR = (base)", "fx_GBP" to "1 GBP = (base)", "fx_AED" to "1 AED = (base)", "fx_PKR" to "1 PKR = (base)",
    "registration_enabled" to "Allow sign-ups", "require_verification" to "New accounts need my approval", "registration_message" to "Message after sign-up",
    "credit_limit_default" to "Default credit limit", "max_active_rentals" to "Max running rentals per reseller (0 = no limit)", "show_slot_count" to "Show free slot count to resellers",
    "refund_mode" to "Refund on approved returns", "account_alert_days" to "Warn days before a tool account expires", "alert_15min" to "Alert 15 min before a rental ends",
    "alert_10min" to "Alert 10 min before", "alert_5min" to "Alert 5 min before", "maintenance_mode" to "Maintenance mode", "maintenance_message" to "Message",
    "maintenance_until" to "Back at (optional)", "maintenance_site" to "Also close the public website", "services_enabled" to "Remote services on", "services_note" to "Note on the services page",
    "mail_enabled" to "Send emails", "mail_method" to "Send with", "mail_from_email" to "From email", "mail_from_name" to "From name", "smtp_host" to "SMTP host",
    "smtp_port" to "SMTP port", "smtp_secure" to "Encryption", "smtp_user" to "SMTP username", "smtp_verify_ssl" to "Check the server certificate", "mail_admin_to" to "Admin alerts to",
    "site_url" to "Site address for links", "mail_verify_signup" to "Confirm email on sign-up", "mail_on_account" to "Account emails", "mail_on_topup" to "Top-up emails",
    "mail_on_rental" to "Rental emails", "mail_on_expiry" to "Expiry emails", "mail_on_service" to "Service order emails", "mail_admin_on_register" to "Tell me about sign-ups",
    "mail_admin_on_service" to "Tell me about new orders", "mail_admin_login_alert" to "Email me on admin sign-in", "mail_rental_receipt" to "Rental receipts",
    "mail_service_receipt" to "Service receipts", "mail_receipt_login" to "Put the login in rental receipts", "mail_admin_on_rental" to "Tell me about rentals",
    "twofa_admin" to "Two-step for owner & staff", "twofa_reseller" to "Two-step for resellers", "api_enabled" to "Reseller API", "api_rate" to "API calls per minute",
    "mobile_app_enabled" to "Resellers can use the app", "mobile_admin_enabled" to "Owner & staff can use the app", "mobile_register_enabled" to "Sign up from the app",
    "promo_enabled" to "Show the top bar", "promo_on_site" to "On the website", "promo_on_auth" to "On sign-in pages", "promo_on_panel" to "In the reseller panel", "promo_on_admin" to "In the admin panel",
    "promo_mode" to "Content", "promo_messages" to "Messages (one per line)", "promo_html" to "HTML", "promo_effect" to "Effect", "promo_speed" to "Seconds per message",
    "promo_style" to "Style", "promo_bg" to "Background color", "promo_fg" to "Text color", "promo_icon" to "Icon", "promo_link_url" to "Button link", "promo_link_label" to "Button text",
    "promo_starts_at" to "Start", "promo_ends_at" to "End", "promo_countdown" to "Show a countdown to the end", "promo_dismissible" to "Visitors can close it",
    "site_enabled" to "Public website on", "site_headline" to "Headline", "site_subheadline" to "Sub-headline", "site_about" to "About text", "site_show_stats" to "Show stats",
    "site_hero_visual" to "Hero picture", "site_scroll_fx" to "Scroll animations", "site_tools_title" to "Tools title", "site_tools_text" to "Tools text",
    "site_services_title" to "Services title", "site_services_text" to "Services text", "wa_enabled" to "Use the WhatsApp gateway", "wa_method" to "Request method",
    "wa_url" to "Gateway URL", "wa_body" to "Request body", "wa_admin_phone" to "Admin phone", "wa_on_rental" to "On new rentals", "wa_on_expiry" to "On expiry",
    "wa_on_payment" to "On payments", "wa_on_return" to "On returns",
)

private val CHOICES = mapOf(
    "refund" to listOf("full" to "Full price", "prorata" to "Unused time only"),
    "method" to listOf("GET" to "GET", "POST" to "POST"),
    "mailmethod" to listOf("smtp" to "SMTP", "mail" to "PHP mail()"),
    "secure" to listOf("tls" to "TLS", "ssl" to "SSL", "none" to "None"),
    "twofa" to listOf("off" to "Off", "optional" to "Optional", "required" to "Required"),
    "promomode" to listOf("text" to "Text", "html" to "HTML"),
    "herovisual" to listOf("auto" to "Show", "off" to "Hide"),
    "scrollfx" to listOf("always" to "Always", "once" to "Once", "off" to "Off"),
    "cur" to listOf("PKR" to "PKR", "USD" to "USD", "EUR" to "EUR", "GBP" to "GBP", "AED" to "AED"),
)

private val LONG = setOf("lines", "promohtml")
private val NUMBER = setOf("int", "rate", "limit", "port", "apirate", "speed")

@Composable
fun SettingsPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_settings") { PageVM(api, "/admin/settings") }
    val toast = rememberToast()
    var mailTest by remember { mutableStateOf(false) }
    val f = remember { FormState() }
    Panel("Settings", ctx.nav, vm) { d ->
        items(SECTIONS, key = { it.key }) { s ->
            Line(s.title, s.sub, leading = { Icon(s.icon, null, tint = MaterialTheme.colorScheme.secondary) }, onClick = { ctx.go("setting", 0, s.key) })
        }
        item { Section("Tools") }
        item {
            Line("Run the expiry check now", "Ends finished rentals and sends alerts",
                leading = { Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.secondary) },
                onClick = { vm.submit("/admin/settings/run-expiry", form(), toast) })
        }
        item {
            Line("Send a test email", "Check your email settings", leading = { Icon(Icons.Outlined.Email, null, tint = MaterialTheme.colorScheme.secondary) },
                onClick = { f.values.clear(); mailTest = true })
        }
        item {
            Line("Sign every phone out of the app", "Everyone has to sign in again (you too)", leading = { Icon(Icons.Outlined.PhoneAndroid, null, tint = AB.brand.danger) },
                onClick = { vm.submit("/admin/settings/mobile-signout", form(), toast) })
        }
        val info = d.obj("info")
        if (info != null) item {
            GlassCard(Modifier.fillMaxWidth()) {
                InfoRow("Site", info.str("url"))
                InfoRow("PHP", info.str("php"))
                InfoRow("Database", info.str("db"))
                InfoRow("Last expiry check", info.int("last").takeIf { it > 0 }?.let { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(it * 1000L)) } ?: "never")
            }
        }
    }
    Sheet(mailTest, "Test email", { mailTest = false }, action = "Send", busy = vm.busy, onAction = {
        vm.submit("/admin/settings/mail-test", f.json(), toast) { mailTest = false }
    }) {
        TextIn(f, "to", "Send to", hint = "Empty = your own email", keyboard = androidx.compose.ui.text.input.KeyboardType.Email)
    }
}

@Composable
fun SettingsSectionPanel(ctx: PanelCtx, section: String) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_settings_$section") { PageVM(api, "/admin/settings") }
    val toast = rememberToast()
    val f = remember { FormState() }
    var filled by remember { mutableStateOf(false) }
    val fields = vm.data.obj("sections").list(section)
    LaunchedEffect(fields.size) {
        if (fields.isNotEmpty() && !filled) {
            filled = true
            fields.forEach { fl -> f[fl.str("key")] = fl.str("value") }
        }
    }
    val info = SECTIONS.firstOrNull { it.key == section }
    Panel(info?.title ?: "Settings", ctx.nav, vm, bottom = {
        GradientButton("Save", {
            val extra = buildMap {
                put("section", section)
                fields.filter { it.str("type") == "localdt" }.forEach { fl -> val v = f[fl.str("key")]; if (v.length == 10) put(fl.str("key"), v + "T00:00") }
            }
            vm.submit("/admin/settings", f.json(extra = extra), toast) { if (it.ok) ctx.nav.popBackStack() }
        }, Modifier.fillMaxWidth(), loading = vm.busy)
    }) { _ ->
        if (info != null) item { Text(info.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Group {
                fields.forEach { fl ->
                    val key = fl.str("key")
                    val type = fl.str("type")
                    val label = LABELS[key] ?: key.replace('_', ' ').replaceFirstChar { it.uppercase() }
                    when {
                        type == "bool" -> SwitchIn(f, key, label)
                        CHOICES.containsKey(type) -> ChoiceIn(f, key, label, CHOICES.getValue(type))
                        type == "localdt" -> DateIn(f, key, label)
                        type in LONG || key.endsWith("_message") || key.endsWith("_about") || key == "wa_body" || key.endsWith("_text") -> TextIn(f, key, label, lines = 3)
                        type in NUMBER -> TextIn(f, key, label, number = true)
                        type == "email" -> TextIn(f, key, label, keyboard = androidx.compose.ui.text.input.KeyboardType.Email)
                        else -> TextIn(f, key, label)
                    }
                }
                if (section == "email") {
                    TextIn(f, "smtp_pass", "SMTP password (empty = keep the saved one)", password = true)
                    SwitchIn(f, "smtp_pass_clear", "Remove the saved password")
                }
            }
        }
    }
}

// ── activity log ───────────────────────────────────────────────────────────

@Composable
fun ActivityPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_activity") { PageVM(api, "/admin/activity") }
    Panel("Activity log", ctx.nav, vm) { d ->
        item { SearchBox(vm.query["q"].orEmpty(), "Action, detail or user", { vm.set("q", it) }) }
        val rows = d.list("rows")
        if (rows.isEmpty()) item { EmptyState(Icons.AutoMirrored.Outlined.List, "Nothing here", "Changes made in the panel are listed here.") }
        items(rows, key = { "ac" + it.int("id") }) { a ->
            Line(
                a.str("detail").ifBlank { a.str("action") },
                listOfNotNull(a.str("full_name").ifBlank { a.str("username") }.ifBlank { "system" }, a.str("action"), a.str("ip_address").ifBlank { null }, ago(a.str("created_at"))).joinToString(" · "),
            )
        }
        pager(d, vm)
    }
}

// ── backups ────────────────────────────────────────────────────────────────

@Composable
fun BackupsPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_backups") { PageVM(api, "/admin/system") }
    val toast = rememberToast()
    var auto by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(false) }
    val f = remember { FormState() }
    val s = remember { FormState() }
    Panel("Backups", ctx.nav, vm, actions = {
        IconButton(onClick = { s.values.clear(); vm.data.obj("settings")?.let { st -> st.keys.forEach { k -> s[k] = st.str(k) } }; auto = true }) { Icon(Icons.Outlined.Tune, "Automatic backups") }
    }, bottom = {
        GradientButton("Back up now", { f.values.clear(); f["with_uploads"] = "1"; now = true }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Backup)
    }) { d ->
        item {
            Text("Backups stay on your server (database + settings, optionally pictures). Download or restore them from the website: Admin → Backups & update.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!d.bool("zipOk")) item { Text("This server can’t make zip files (PHP zip extension missing).", color = AB.brand.danger, style = MaterialTheme.typography.bodySmall) }
        val rows = d.list("backups")
        if (rows.isEmpty()) item { EmptyState(Icons.Outlined.Backup, "No backups yet", "Tap “Back up now”.") }
        items(rows, key = { it.str("name") }) { b ->
            Line(
                SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(b.int("time") * 1000L)),
                "${b.str("reason")} · ${"%.1f".format(b.dbl("size") / 1048576.0)} MB",
                leading = { Icon(Icons.Outlined.Backup, null, tint = MaterialTheme.colorScheme.secondary) },
                menu = listOf<Pair<String, () -> Unit>>("Delete" to { vm.submit("/admin/system/backup-delete", form("f" to b.str("name")), toast) }),
            )
        }
    }
    Sheet(now, "Back up now", { now = false }, action = "Start", busy = vm.busy, onAction = {
        vm.submit("/admin/system/backup", f.json(), toast) { now = false }
    }) {
        SwitchIn(f, "with_uploads", "Include pictures", "Tool, service, website and message pictures")
    }
    Sheet(auto, "Automatic backups", { auto = false }, action = "Save", busy = vm.busy, onAction = {
        vm.submit("/admin/system/backup-settings", s.json(), toast) { if (it.ok) auto = false }
    }) {
        SwitchIn(s, "backup_auto", "Automatic backup every day")
        TextIn(s, "backup_keep", "Keep this many", number = true)
        SwitchIn(s, "backup_uploads", "Include pictures")
    }
}

// ── reseller API access ────────────────────────────────────────────────────

@Composable
fun ApiAccessPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_apiaccess") { PageVM(api, "/admin/api-access") }
    val toast = rememberToast()
    var rejecting by remember { mutableStateOf<JsonElement?>(null) }
    val f = remember { FormState() }
    Panel("API access", ctx.nav, vm) { d ->
        val rows = d.list("rows")
        if (rows.isEmpty()) item { EmptyState(Icons.Outlined.Api, "No requests", "Resellers ask for API access from their panel or app.") }
        items(rows, key = { "api" + it.int("id") }) { r ->
            val st = r.str("api_status")
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.str("full_name").ifBlank { r.str("username") }, style = MaterialTheme.typography.titleSmall)
                        Text("@${r.str("username")} · ${r.int("calls_7d")} calls in 7 days" + (r.str("api_key_hint").ifBlank { null }?.let { " · key …$it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    com.aamirbuneri.abgsmrental.ui.components.StatusPill(st.replaceFirstChar { it.uppercase() }, tone(st))
                }
                if (r.str("api_note").isNotBlank()) { Spacer(Modifier.height(6.dp)); Text("“${r.str("api_note")}”", style = MaterialTheme.typography.bodyMedium) }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (st != "approved") GradientButton("Approve", { vm.submit("/admin/api-access/${r.int("id")}/approve", form(), toast) }, Modifier.weight(1f), height = 46.dp)
                    if (st != "rejected") SecondaryButton(if (st == "approved") "Turn off" else "Reject", { f.values.clear(); rejecting = r }, Modifier.weight(1f))
                }
            }
        }
    }
    val rj = rejecting
    Sheet(rj != null, "Reject / turn off", { rejecting = null }, action = "Confirm", busy = vm.busy, onAction = {
        if (rj != null) vm.submit("/admin/api-access/${rj.int("id")}/reject", f.json(), toast) { rejecting = null }
    }) {
        Text("Their API key stops working right away.", style = MaterialTheme.typography.bodyMedium)
        TextIn(f, "note", "Note for the reseller (optional)", lines = 3)
    }
}

// ── reseller: create / edit / delete ───────────────────────────────────────

private val TIERS = listOf("standard" to "Standard", "silver" to "Silver", "gold" to "Gold", "platinum" to "Platinum", "vip" to "VIP")

@Composable
fun ResellerFormPanel(ctx: PanelCtx, id: Int) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_rform$id") { PageVM(api, if (id > 0) "/admin/resellers/$id/edit" else "/admin/resellers/new") }
    val toast = rememberToast()
    val f = remember { FormState(mapOf("currency" to ctx.currency.ifBlank { "PKR" }, "tier" to "standard")) }
    var filled by remember { mutableStateOf(id == 0) }
    val d = vm.data
    LaunchedEffect(d) {
        val r = d.obj("r")
        if (r != null && !filled) {
            filled = true
            listOf("username", "email", "full_name", "phone", "company_name", "address", "currency", "tier", "notes").forEach { k -> f[k] = r.str(k) }
            f["discount_percent"] = num(r.dbl("discount_percent"))
            f["credit_limit"] = r.str("credit_limit")
            f["allow_negative"] = if (r.bool("allow_negative")) "1" else "0"
        }
    }
    Panel(if (id > 0) "Edit reseller" else "New reseller", ctx.nav, vm, bottom = {
        GradientButton(if (id > 0) "Save" else "Create reseller", {
            vm.submit(if (id > 0) "/admin/resellers/$id/edit" else "/admin/resellers/new", f.json(), toast, reload = false) { r ->
                if (r.ok) {
                    val newId = r.redirectId("resellers")
                    ctx.nav.popBackStack()
                    if (id == 0 && newId != null) ctx.nav.navigate(com.aamirbuneri.abgsmrental.ui.admin.AdminRoutes.reseller(newId))
                }
            }
        }, Modifier.fillMaxWidth(), loading = vm.busy)
    }) { _ ->
        item {
            Group("Account") {
                TextIn(f, "full_name", "Full name")
                TextIn(f, "username", "Username")
                TextIn(f, "email", "Email", keyboard = androidx.compose.ui.text.input.KeyboardType.Email)
                TextIn(f, "phone", "Phone / WhatsApp", keyboard = androidx.compose.ui.text.input.KeyboardType.Phone)
                TextIn(f, "password", if (id > 0) "New password (empty = keep)" else "Password", password = true, hint = "At least 8 characters")
            }
        }
        item {
            Group("Business") {
                TextIn(f, "company_name", "Shop / company")
                TextIn(f, "address", "Address", lines = 2)
                ChoiceIn(f, "currency", "Pays in", CHOICES.getValue("cur"))
                ChoiceIn(f, "tier", "Tier", TIERS)
                TextIn(f, "discount_percent", "Discount %", number = true)
            }
        }
        item {
            Group("Credit") {
                if (id == 0) TextIn(f, "opening_balance", "Opening balance", number = true)
                SwitchIn(f, "allow_negative", "May go below zero", "Rent on credit up to the limit")
                if (f.on("allow_negative")) TextIn(f, "credit_limit", "Credit limit (empty = default)", number = true)
                TextIn(f, "notes", "Private notes", lines = 3)
            }
        }
    }
}

@Composable
fun ResellerDeletePanel(ctx: PanelCtx, id: Int) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_rdel$id") { PageVM(api, "/admin/resellers/$id/delete") }
    val toast = rememberToast()
    val f = remember { FormState(mapOf("mode" to "keep")) }
    Panel("Delete reseller", ctx.nav, vm) { d ->
        val r = d.obj("r")
        val s = d.obj("sum")
        val blockers = d.list("blockers")
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(r.str("full_name").ifBlank { r.str("username") }, style = MaterialTheme.typography.titleMedium)
                InfoRow("Balance", money(s.dbl("balance"), d.str("cur")))
                InfoRow("Rentals", "${s.int("rentals")} (${s.int("active")} running)")
                InfoRow("Invoices", "${s.int("invoices")}")
                InfoRow("Service orders", "${s.int("orders")}")
                InfoRow("Chat messages", "${s.int("messages")}")
            }
        }
        if (blockers.isNotEmpty()) {
            item { Text("Can’t delete yet:", color = AB.brand.danger, style = MaterialTheme.typography.titleSmall) }
            items(blockers.size) { i -> Text("• " + (blockers[i].text() ?: ""), style = MaterialTheme.typography.bodyMedium) }
        } else {
            item {
                Group("How") {
                    ChoiceIn(f, "mode", null, listOf("keep" to "Keep history", "erase" to "Erase everything"))
                    Text(if (f["mode"] == "erase") "Rentals, invoices, orders, wallet history and chat are removed too. Reports change." else "The account goes; rentals, invoices and orders stay in your reports.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (f["mode"] == "erase") TextIn(f, "confirm_word", "Type DELETE")
                    TextIn(f, "current_password", "Your password", password = true)
                }
            }
            item {
                GradientButton("Delete reseller", {
                    vm.submit("/admin/resellers/$id/delete", f.json(), toast, reload = false) { if (it.ok) { ctx.nav.popBackStack(); ctx.nav.popBackStack() } }
                }, Modifier.fillMaxWidth(), loading = vm.busy)
            }
        }
    }
}
