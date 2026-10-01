package com.aamirbuneri.abgsmrental.ui.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.FilePart
import com.aamirbuneri.abgsmrental.data.ToolLogin
import com.aamirbuneri.abgsmrental.data.bool
import com.aamirbuneri.abgsmrental.data.dbl
import com.aamirbuneri.abgsmrental.data.dblOrNull
import com.aamirbuneri.abgsmrental.data.form
import com.aamirbuneri.abgsmrental.data.int
import com.aamirbuneri.abgsmrental.data.items
import com.aamirbuneri.abgsmrental.data.list
import com.aamirbuneri.abgsmrental.data.obj
import com.aamirbuneri.abgsmrental.data.str
import com.aamirbuneri.abgsmrental.ui.admin.ConfirmDialog
import com.aamirbuneri.abgsmrental.ui.admin.rememberToast
import com.aamirbuneri.abgsmrental.ui.admin.strongPassword
import com.aamirbuneri.abgsmrental.ui.components.CredentialRow
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.SecondaryButton
import com.aamirbuneri.abgsmrental.ui.components.Tone
import com.aamirbuneri.abgsmrental.ui.util.clock
import com.aamirbuneri.abgsmrental.ui.util.money
import com.aamirbuneri.abgsmrental.ui.util.parseIso
import com.aamirbuneri.abgsmrental.ui.util.shortDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

val TOOL_COLORS = listOf("#22d3ee", "#6366f1", "#a855f7", "#ec4899", "#f43f5e", "#f97316", "#f59e0b", "#10b981", "#14b8a6", "#3b82f6")
val SERVICE_COLORS = listOf("#8b5cf6", "#22d3ee", "#6366f1", "#ec4899", "#f43f5e", "#f97316", "#f59e0b", "#10b981", "#14b8a6", "#3b82f6")

/** "tools/ab12.webp" → full address on the site. */
fun siteFile(site: String, path: String?): String? = path?.takeIf { it.isNotBlank() }?.let { site.trimEnd('/') + "/uploads/" + it }

fun minutesText(m: Int): String = when {
    m <= 0 -> ""
    m % 1440 == 0 -> "${m / 1440} day" + if (m / 1440 == 1) "" else "s"
    m % 60 == 0 -> "${m / 60} h"
    else -> "${m / 60} h ${m % 60} min"
}

/** 2.5 → "2.5", 3.0 → "3", null → "". */
fun num(v: Double?): String = when {
    v == null -> ""
    v % 1.0 == 0.0 -> v.toLong().toString()
    else -> v.toString()
}

// ── tools ──────────────────────────────────────────────────────────────────

@Composable
fun ToolsPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_tools") { PageVM(api, "/admin/tools") }
    val toast = rememberToast()
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    Panel("Tools & slots", ctx.nav, vm, actions = {
        IconButton(onClick = { ctx.go("tool", 0) }) { Icon(Icons.Outlined.Add, "New tool") }
    }) { d ->
        val tools = d.list("tools")
        if (tools.isEmpty()) item { EmptyState(Icons.Outlined.Key, "No tools yet", "Add your first tool, its plans and prices, then its slots (accounts).", action = "Add a tool") { ctx.go("tool", 0) } }
        items(tools, key = { it.int("id") }) { t ->
            val id = t.int("id")
            Line(
                title = t.str("name"),
                sub = listOfNotNull(
                    "${t.int("free_slots")} free · ${t.int("busy_slots")} busy of ${t.int("total_slots")} slots",
                    "${t.int("dur_count")} plan${if (t.int("dur_count") == 1) "" else "s"}" + (t.dblOrNull("min_price")?.let { " · from ${money(it, ctx.currency)}" } ?: ""),
                    "${t.int("rental_count")} rentals",
                ).joinToString("\n"),
                leading = { Dot(t.str("name"), t.str("color")) },
                pill = if (t.bool("is_active")) null else "Hidden" to Tone.Neutral,
                menu = listOf<Pair<String, () -> Unit>>(
                    "Slots" to { ctx.go("slots", id) },
                    "Edit tool & prices" to { ctx.go("tool", id) },
                    (if (t.bool("is_active")) "Hide from resellers" else "Show to resellers") to { vm.submit("/admin/tools/$id/toggle", form(), toast) },
                    "Delete" to { confirm = "Delete ${t.str("name")}? Its rental history is kept." to { vm.submit("/admin/tools/$id/delete", form(), toast) } },
                ),
                onClick = { ctx.go("slots", id) },
            )
        }
    }
    confirm?.let { (text, run) ->
        ConfirmDialog("Are you sure?", text, "Delete", danger = true, onDismiss = { confirm = null }) { confirm = null; run() }
    }
}

private class PlanRow(val id: String, label: String, hours: String, price: String, usd: String, eur: String, gbp: String, aed: String) {
    var label by mutableStateOf(label); var hours by mutableStateOf(hours); var price by mutableStateOf(price)
    var usd by mutableStateOf(usd); var eur by mutableStateOf(eur); var gbp by mutableStateOf(gbp); var aed by mutableStateOf(aed)
}

/** New tool (id 0) or edit: name, look, plans with prices in every currency. */
@Composable
fun ToolFormPanel(ctx: PanelCtx, id: Int) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_tool$id") { PageVM(api, if (id > 0) "/admin/tools/$id/edit" else "/admin/tools/new") }
    val toast = rememberToast()
    val f = remember { FormState(mapOf("color" to TOOL_COLORS[0], "is_active" to "1", "sort_order" to "0")) }
    val plans = remember { mutableStateListOf<PlanRow>() }
    var picture by remember { mutableStateOf<FilePart?>(null) }
    var filled by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    val data = vm.data
    LaunchedEffect(data) {
        if (data != null && !filled) {
            filled = true
            data.obj("tool")?.let { t ->
                listOf("name", "category", "description", "color", "sort_order").forEach { k -> f[k] = t.str(k) }
                f["is_active"] = if (t.bool("is_active")) "1" else "0"
            }
            plans.clear()
            data.list("durations").forEach { p ->
                plans += PlanRow(p.str("id"), p.str("label"), num(p.int("minutes") / 60.0), num(p.dblOrNull("price")), num(p.dblOrNull("price_usd")), num(p.dblOrNull("price_eur")), num(p.dblOrNull("price_gbp")), num(p.dblOrNull("price_aed")))
            }
            if (plans.isEmpty()) plans += PlanRow("", "2 Hours", "2", "", "", "", "", "")
        }
    }
    fun save() {
        val fields = f.pairs().filter { it.first != "is_active" || it.second == "1" }.toMutableList()
        plans.filter { it.label.isNotBlank() }.forEach { p ->
            fields += "d_id[]" to p.id; fields += "d_label[]" to p.label; fields += "d_hours[]" to p.hours; fields += "d_price[]" to p.price
            fields += "d_usd[]" to p.usd; fields += "d_eur[]" to p.eur; fields += "d_gbp[]" to p.gbp; fields += "d_aed[]" to p.aed
        }
        vm.submitFiles(if (id > 0) "/admin/tools/$id/edit" else "/admin/tools/new", fields, listOfNotNull(picture), toast, reload = false) { r ->
            if (id == 0) r.redirectId("tools")?.let { ctx.replace("slots", it) } ?: ctx.nav.popBackStack()
            else ctx.nav.popBackStack()
        }
    }
    Panel(if (id > 0) "Edit tool" else "New tool", ctx.nav, vm, bottom = {
        GradientButton(if (id > 0) "Save tool" else "Create tool", { save() }, Modifier.fillMaxWidth(), loading = vm.busy)
    }) { d ->
        item {
            Group("Tool") {
                TextIn(f, "name", "Name", hint = "e.g. UnlockTool")
                TextIn(f, "category", "Category (optional)", hint = "e.g. Samsung, iPhone")
                TextIn(f, "description", "What resellers see", lines = 3)
                ColorIn(f, "color", TOOL_COLORS)
                ImageIn("Logo / picture", picture, siteFile(ctx.site, d.obj("tool").str("image").ifBlank { null })) { picture = it }
                SwitchIn(f, "is_active", "Visible to resellers", "Hidden tools can still be assigned by you")
                TextIn(f, "sort_order", "Order in lists", number = true, hint = "Lower comes first")
            }
        }
        item {
            Group("Plans & prices") {
                Text("Each plan is a rental length and its price. Prices in other currencies are optional — resellers billed in that currency only see plans with a price.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                plans.forEachIndexed { i, p ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Plan ${i + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = { plans.removeAt(i) }) { Icon(Icons.Outlined.Delete, "Remove plan", tint = MaterialTheme.colorScheme.error) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PlainField(p.label, { p.label = it }, "Label", Modifier.weight(1.4f))
                            PlainField(p.hours, { p.hours = it }, "Hours", Modifier.weight(0.8f), number = true)
                        }
                        PlainField(p.price, { p.price = it }, "Price (${ctx.currency})", number = true)
                        if (more) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PlainField(p.usd, { p.usd = it }, "USD", Modifier.weight(1f), number = true)
                            PlainField(p.eur, { p.eur = it }, "EUR", Modifier.weight(1f), number = true)
                            PlainField(p.gbp, { p.gbp = it }, "GBP", Modifier.weight(1f), number = true)
                            PlainField(p.aed, { p.aed = it }, "AED", Modifier.weight(1f), number = true)
                        }
                        if (i < plans.lastIndex) Divider()
                    }
                }
                Row {
                    TextButton(onClick = { plans += PlanRow("", "", "", "", "", "", "", "") }) { Text("+ Add plan") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { more = !more }) { Text(if (more) "Hide other currencies" else "Other currencies") }
                }
            }
        }
    }
}

@Composable
fun PlainField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth(), number: Boolean = false, lines: Int = 1) {
    androidx.compose.material3.OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = lines == 1, minLines = lines,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (number) androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Text),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = modifier,
    )
}

// ── slots ──────────────────────────────────────────────────────────────────

private sealed interface SlotEdit {
    data object Add : SlotEdit
    data object Bulk : SlotEdit
    data class Edit(val s: JsonElement) : SlotEdit
    data class Renew(val s: JsonElement) : SlotEdit
}

@Composable
fun SlotsPanel(ctx: PanelCtx, toolId: Int) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_slots$toolId") { PageVM(api, "/admin/tools/$toolId/slots") }
    val toast = rememberToast()
    var edit by remember { mutableStateOf<SlotEdit?>(null) }
    var shown by remember { mutableStateOf<Map<Int, ToolLogin>>(emptyMap()) }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val f = remember { FormState() }
    val tool = vm.data.obj("tool")
    Panel(tool.str("name").ifBlank { "Slots" } + " · slots", ctx.nav, vm, actions = {
        TextButton(onClick = { ctx.go("tool", toolId) }) { Text("Edit tool") }
    }) { d ->
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientButton("Add slot", { f.values.clear(); edit = SlotEdit.Add }, Modifier.weight(1f), icon = Icons.Outlined.Add, height = 48.dp)
                SecondaryButton("Add many", { f.values.clear(); edit = SlotEdit.Bulk }, Modifier.weight(1f))
            }
        }
        val slots = d.list("slots")
        if (slots.isEmpty()) item { EmptyState(Icons.Outlined.Key, "No slots yet", "A slot is one account of this tool that can be rented. Add one for each login you own.") }
        items(slots, key = { it.int("id") }) { s ->
            val sid = s.int("id")
            val status = s.str("status")
            val renter = s.str("renter_name")
            val ends = parseIso(s.str("expiry_time"))?.time
            val acct = s.str("account_expires_at")
            val login = shown[sid]
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Line(
                    title = s.str("slot_name"),
                    sub = listOfNotNull(
                        s.str("username").ifBlank { s.str("email") }.ifBlank { null },
                        if (status == "busy" && renter.isNotBlank()) "In use by $renter" + (ends?.let { " · " + clock(((it - System.currentTimeMillis()) / 1000).coerceAtLeast(0)) + " left" } ?: "") else null,
                        if (acct.isNotBlank()) "Account valid until ${shortDate(acct)}" else null,
                        "Paid ${money(s.dbl("total_paid"), ctx.currency)} · earned ${money(s.dbl("total_earned"), ctx.currency)}",
                    ).joinToString("\n"),
                    pill = when (status) { "available" -> "Free" to Tone.Success; "busy" -> "In use" to Tone.Warning; else -> "Off" to Tone.Neutral },
                    menu = listOfNotNull<Pair<String, () -> Unit>>(
                        (if (login == null) "Show login" else "Hide login") to {
                            if (login != null) {
                                shown = shown - sid
                            } else {
                                vm.fetch(toast, { a -> a.page("/admin/slots/$sid/secret") }) { r ->
                                    val j = r.json
                                    shown = shown + (sid to ToolLogin(j.str("username"), j.str("email"), j.str("password")))
                                }
                            }
                        },
                        "Edit / new password" to {
                            f.values.clear()
                            listOf("slot_name", "username", "email", "notes").forEach { k -> f[k] = s.str(k) }
                            f["account_expires_at"] = acct.takeIf { it.isNotBlank() }?.let { com.aamirbuneri.abgsmrental.ui.util.parseIso(it) }?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(it) } ?: ""
                            f["renew_cost"] = num(s.dblOrNull("renew_cost")); f["renew_days"] = s.str("renew_days").ifBlank { "30" }
                            edit = SlotEdit.Edit(s)
                        },
                        "Renew account" to { f.values.clear(); f["amount"] = num(s.dblOrNull("renew_cost")); edit = SlotEdit.Renew(s) },
                        if (status != "busy") (if (status == "disabled") "Enable" else "Disable") to { vm.submit("/admin/slots/$sid/toggle", form(), toast) } else null,
                        if (status != "busy") "Delete" to { confirm = "Delete slot ${s.str("slot_name")}?" to { vm.submit("/admin/slots/$sid/delete", form(), toast) } } else null,
                        if (status == "busy" && s.int("rental_id") > 0) "Open rental" to { ctx.nav.navigate(com.aamirbuneri.abgsmrental.ui.admin.AdminRoutes.rental(s.int("rental_id"))) } else null,
                    ),
                )
                if (login != null) {
                    Group {
                        if (login.username.isNotBlank()) CredentialRow("Username", login.username)
                        if (login.email.isNotBlank()) CredentialRow("Email", login.email)
                        CredentialRow("Password", login.password.ifBlank { "—" }, secret = true, initiallyShown = true)
                    }
                }
            }
        }
    }
    val e = edit
    Sheet(
        visible = e != null,
        title = when (e) { SlotEdit.Add -> "Add slot"; SlotEdit.Bulk -> "Add many slots"; is SlotEdit.Edit -> "Edit ${e.s.str("slot_name")}"; is SlotEdit.Renew -> "Renew ${e.s.str("slot_name")}"; null -> "" },
        onClose = { edit = null },
        action = "Save",
        busy = vm.busy,
        onAction = {
            val target = when (e) {
                SlotEdit.Add -> "/admin/tools/$toolId/slots" to f.json()
                SlotEdit.Bulk -> "/admin/tools/$toolId/slots" to form("bulk" to f["bulk"])
                is SlotEdit.Edit -> "/admin/slots/${e.s.int("id")}/edit" to f.json()
                is SlotEdit.Renew -> "/admin/slots/${e.s.int("id")}/renew" to f.json(extra = mapOf("back" to "/admin/tools/$toolId/slots"))
                null -> null
            }
            if (target != null) vm.submit(target.first, target.second, toast) { edit = null }
        },
    ) {
        when (e) {
            SlotEdit.Bulk -> {
                Text("One slot per line:\nname | username or email | password | notes | expiry (YYYY-MM-DD) | cost", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextIn(f, "bulk", "Slots", lines = 8, mono = true)
            }
            is SlotEdit.Renew -> {
                Text("Paid for another period of this tool account? Record it here — profit reports use it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DateIn(f, "expires_at", "Valid until")
                TextIn(f, "amount", "Amount paid (${ctx.currency})", number = true)
                TextIn(f, "note", "Note (optional)")
            }
            SlotEdit.Add, is SlotEdit.Edit -> {
                TextIn(f, "slot_name", "Slot name", hint = "e.g. Slot A")
                TextIn(f, "username", "Username")
                TextIn(f, "email", "Email", keyboard = androidx.compose.ui.text.input.KeyboardType.Email)
                TextIn(f, "password", if (e is SlotEdit.Edit) "New password (leave empty to keep)" else "Password", password = true, mono = true)
                TextButton(onClick = { f["password"] = strongPassword() }) { Text("Make a strong password") }
                if (e is SlotEdit.Edit && e.s.str("status") == "busy") {
                    Text("This slot is rented now — the renter keeps seeing the login they got until their rental ends.", style = MaterialTheme.typography.bodySmall, color = com.aamirbuneri.abgsmrental.ui.theme.AB.brand.warning)
                }
                TextIn(f, "notes", "Notes (only you see these)", lines = 2)
                DateIn(f, "account_expires_at", "Tool account valid until (optional)")
                if (e == SlotEdit.Add) TextIn(f, "cost_paid", "What you paid for it (${ctx.currency}, optional)", number = true)
                else {
                    TextIn(f, "renew_cost", "Renewal price (${ctx.currency}, optional)", number = true)
                    TextIn(f, "renew_days", "Renews every … days", number = true)
                }
            }
            null -> {}
        }
    }
    confirm?.let { (text, run) ->
        ConfirmDialog("Are you sure?", text, "Delete", danger = true, onDismiss = { confirm = null }) { confirm = null; run() }
    }
}

// ── services catalog ───────────────────────────────────────────────────────

@Composable
fun ServicesPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_services") { PageVM(api, "/admin/services") }
    val toast = rememberToast()
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    Panel("Service catalog", ctx.nav, vm, actions = {
        IconButton(onClick = { ctx.go("service", 0) }) { Icon(Icons.Outlined.Add, "New service") }
    }) { d ->
        val list = d.list("services")
        if (list.isEmpty()) item { EmptyState(Icons.Outlined.Key, "No services yet", "Add the remote services you offer (FRP, unlocks, checks…).", action = "Add a service") { ctx.go("service", 0) } }
        items(list, key = { it.int("id") }) { s ->
            val id = s.int("id")
            val quote = s.str("price_mode") == "quote"
            Line(
                title = s.str("name"),
                sub = listOfNotNull(
                    s.str("category").ifBlank { null },
                    if (quote) "Price on request" else money(s.dbl("price"), ctx.currency),
                    "${s.int("order_count")} orders" + if (s.int("open_count") > 0) " · ${s.int("open_count")} open" else "",
                ).joinToString(" · "),
                leading = { Dot(s.str("name"), s.str("color")) },
                pill = if (s.bool("is_active")) null else "Hidden" to Tone.Neutral,
                menu = listOf<Pair<String, () -> Unit>>(
                    "Edit" to { ctx.go("service", id) },
                    (if (s.bool("is_active")) "Hide" else "Show") to { vm.submit("/admin/services/$id/toggle", form(), toast) },
                    "Delete" to { confirm = "Delete ${s.str("name")}? Its order history is kept." to { vm.submit("/admin/services/$id/delete", form(), toast) } },
                ),
                onClick = { ctx.go("service", id) },
            )
        }
    }
    confirm?.let { (text, run) ->
        ConfirmDialog("Are you sure?", text, "Delete", danger = true, onDismiss = { confirm = null }) { confirm = null; run() }
    }
}

private class FieldRow(label: String, required: Boolean) {
    var label by mutableStateOf(label)
    var required by mutableStateOf(required)
}

@Composable
fun ServiceFormPanel(ctx: PanelCtx, id: Int) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_service$id") { PageVM(api, if (id > 0) "/admin/services/$id/edit" else "/admin/services/new") }
    val toast = rememberToast()
    val f = remember { FormState(mapOf("color" to SERVICE_COLORS[0], "is_active" to "1", "price_mode" to "fixed", "needs_remote" to "1", "eta_minutes" to "30", "sort_order" to "0")) }
    val fields = remember { mutableStateListOf<FieldRow>() }
    var picture by remember { mutableStateOf<FilePart?>(null) }
    var filled by remember { mutableStateOf(false) }
    val data = vm.data
    LaunchedEffect(data) {
        if (data != null && !filled) {
            filled = true
            data.obj("service")?.let { s ->
                listOf("name", "category", "description", "color", "price_mode", "eta_minutes", "sort_order").forEach { k -> f[k] = s.str(k) }
                listOf("price", "price_usd", "price_eur", "price_gbp", "price_aed").forEach { k -> f[k] = num(s.dblOrNull(k)) }
                f["is_active"] = if (s.bool("is_active")) "1" else "0"
                f["needs_remote"] = if (s.bool("needs_remote")) "1" else "0"
                runCatching { Json.parseToJsonElement(s.str("fields_json", "[]")).items() }.getOrDefault(emptyList()).forEach { x ->
                    fields += FieldRow(x.str("label"), x.bool("required"))
                }
            }
        }
    }
    fun save() {
        val out = f.pairs().filter { (k, v) -> (k != "is_active" && k != "needs_remote") || v == "1" }.toMutableList()
        fields.filter { it.label.isNotBlank() }.forEach { r -> out += "field_label[]" to r.label; out += "field_req[]" to if (r.required) "1" else "0" }
        vm.submitFiles(if (id > 0) "/admin/services/$id/edit" else "/admin/services/new", out, listOfNotNull(picture), toast, reload = false) { ctx.nav.popBackStack() }
    }
    Panel(if (id > 0) "Edit service" else "New service", ctx.nav, vm, bottom = {
        GradientButton(if (id > 0) "Save service" else "Create service", { save() }, Modifier.fillMaxWidth(), loading = vm.busy)
    }) { d ->
        item {
            Group("Service") {
                TextIn(f, "name", "Name", hint = "e.g. Xiaomi FRP Remove")
                TextIn(f, "category", "Category (optional)", hint = "e.g. Xiaomi, Samsung, iPhone")
                TextIn(f, "description", "What resellers see", lines = 3)
                ColorIn(f, "color", SERVICE_COLORS)
                ImageIn("Picture", picture, siteFile(ctx.site, d.obj("service").str("image").ifBlank { null })) { picture = it }
                SwitchIn(f, "is_active", "Visible to resellers")
            }
        }
        item {
            Group("Price") {
                ChoiceIn(f, "price_mode", null, listOf("fixed" to "Fixed price (paid when ordering)", "quote" to "Price on request (you send a quote)"))
                if (f["price_mode"] == "fixed") {
                    TextIn(f, "price", "Price (${ctx.currency})", number = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextIn(f, "price_usd", "USD", number = true, modifier = Modifier.weight(1f))
                        TextIn(f, "price_eur", "EUR", number = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextIn(f, "price_gbp", "GBP", number = true, modifier = Modifier.weight(1f))
                        TextIn(f, "price_aed", "AED", number = true, modifier = Modifier.weight(1f))
                    }
                }
                TextIn(f, "eta_minutes", "Usual time (minutes)", number = true)
            }
        }
        item {
            Group("Order form") {
                SwitchIn(f, "needs_remote", "Needs remote access", "Resellers give their AnyDesk / UltraViewer login")
                Text("Extra fields resellers fill in (model, IMEI …):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                fields.forEachIndexed { i, r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlainField(r.label, { r.label = it }, "Field ${i + 1}", Modifier.weight(1f))
                        Spacer(Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.Checkbox(checked = r.required, onCheckedChange = { r.required = it })
                            Text("Required", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { fields.removeAt(i) }) { Icon(Icons.Outlined.Delete, "Remove", tint = MaterialTheme.colorScheme.error) }
                    }
                }
                TextButton(onClick = { fields += FieldRow("", false) }) { Text("+ Add field") }
                TextIn(f, "sort_order", "Order in lists", number = true)
            }
        }
    }
}
