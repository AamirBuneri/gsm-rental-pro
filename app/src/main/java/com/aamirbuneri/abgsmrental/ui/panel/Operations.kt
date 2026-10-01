package com.aamirbuneri.abgsmrental.ui.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Undo
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.bool
import com.aamirbuneri.abgsmrental.data.dbl
import com.aamirbuneri.abgsmrental.data.form
import com.aamirbuneri.abgsmrental.data.int
import com.aamirbuneri.abgsmrental.data.items
import com.aamirbuneri.abgsmrental.data.list
import com.aamirbuneri.abgsmrental.data.obj
import com.aamirbuneri.abgsmrental.data.str
import com.aamirbuneri.abgsmrental.data.text
import com.aamirbuneri.abgsmrental.ui.admin.AdminRoutes
import com.aamirbuneri.abgsmrental.ui.admin.FilterOption
import com.aamirbuneri.abgsmrental.ui.admin.FilterRow
import com.aamirbuneri.abgsmrental.ui.admin.SearchBox
import com.aamirbuneri.abgsmrental.ui.admin.WeekBars
import com.aamirbuneri.abgsmrental.ui.admin.rememberToast
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.InfoRow
import com.aamirbuneri.abgsmrental.ui.components.SecondaryButton
import com.aamirbuneri.abgsmrental.ui.components.Tone
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.ago
import com.aamirbuneri.abgsmrental.ui.util.dateTime
import com.aamirbuneri.abgsmrental.ui.util.money
import com.aamirbuneri.abgsmrental.ui.util.shortDate
import kotlinx.serialization.json.JsonElement

val PAY_METHODS = listOf("cash" to "Cash", "bank" to "Bank", "easypaisa" to "EasyPaisa", "jazzcash" to "JazzCash", "card" to "Card", "other" to "Other")

// ── assign a rental ────────────────────────────────────────────────────────

@Composable
fun AssignPanel(ctx: PanelCtx, presetReseller: Int = 0) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_assign") { PageVM(api, "/admin/assign") }
    val toast = rememberToast()
    val f = remember { FormState(mapOf("renter" to "reseller", "charge_wallet" to "1", "paid_now" to "1", "pay_method" to "cash", "user_id" to if (presetReseller > 0) "$presetReseller" else "")) }
    var opts by remember { mutableStateOf<JsonElement?>(null) }
    fun loadOptions() {
        val tool = f["tool_id"]
        if (tool.isBlank()) return
        val q = if (f["renter"] == "reseller" && f["user_id"].isNotBlank()) mapOf("reseller" to f["user_id"]) else emptyMap()
        vm.fetch(toast, { a -> a.page("/admin/api/tool/$tool", q) }) { r -> opts = r.json; f["duration_id"] = ""; f["slot_id"] = "" }
    }
    Panel("Assign a tool", ctx.nav, vm, bottom = {
        GradientButton("Assign rental", {
            val renter = f["renter"]
            val keep = setOf("renter", "tool_id", "duration_id", "slot_id", "notes", "use_custom_price", "custom_price") + when (renter) {
                "reseller" -> setOf("user_id", "charge_wallet")
                "client" -> setOf("client_id", "paid_now", "pay_method")
                else -> setOf("new_client_name", "new_client_phone", "new_client_email", "new_client_idn", "paid_now", "pay_method")
            }
            val data = kotlinx.serialization.json.JsonObject(f.json().filterKeys { it in keep && f[it].isNotBlank() && !((it == "charge_wallet" || it == "paid_now" || it == "use_custom_price") && f[it] != "1") })
            vm.submit("/admin/assign", data, toast, reload = false) { r ->
                r.redirectId("rentals")?.let { id -> ctx.nav.popBackStack(); ctx.nav.navigate(AdminRoutes.rental(id)) }
            }
        }, Modifier.fillMaxWidth(), loading = vm.busy)
    }) { d ->
        item {
            Group("Who rents it") {
                ChoiceIn(f, "renter", null, listOf("reseller" to "Reseller", "client" to "Walk-in client", "new" to "New client")) { opts = null; loadOptions() }
                when (f["renter"]) {
                    "reseller" -> {
                        PickIn(f, "user_id", "Reseller", d.list("resellers").map { r ->
                            r.str("id") to "${r.str("full_name").ifBlank { r.str("username") }} (@${r.str("username")}) · ${money(r.dbl("credit_balance"), r.str("currency"))}"
                        }) { loadOptions() }
                        SwitchIn(f, "charge_wallet", "Charge their wallet", "Off = free (e.g. a replacement)")
                    }
                    "client" -> {
                        PickIn(f, "client_id", "Client", d.list("clients").map { c -> c.str("id") to (c.str("full_name") + c.str("phone").let { if (it.isBlank()) "" else " · $it" }) })
                        SwitchIn(f, "paid_now", "Paid now")
                        if (f.on("paid_now")) ChoiceIn(f, "pay_method", "Paid by", PAY_METHODS)
                    }
                    else -> {
                        TextIn(f, "new_client_name", "Name")
                        TextIn(f, "new_client_phone", "Phone", keyboard = androidx.compose.ui.text.input.KeyboardType.Phone)
                        TextIn(f, "new_client_email", "Email (optional)", keyboard = androidx.compose.ui.text.input.KeyboardType.Email)
                        TextIn(f, "new_client_idn", "ID card number (optional)")
                        SwitchIn(f, "paid_now", "Paid now")
                        if (f.on("paid_now")) ChoiceIn(f, "pay_method", "Paid by", PAY_METHODS)
                    }
                }
            }
        }
        item {
            Group("Tool & plan") {
                PickIn(f, "tool_id", "Tool", d.list("tools").map { t -> t.str("id") to "${t.str("name")} · ${t.int("free")} free" + if (!t.bool("is_active")) " (hidden)" else "" }) { loadOptions() }
                val o = opts
                if (o != null) {
                    ChoiceIn(f, "duration_id", "Plan", o.list("durations").map { p -> p.str("id") to "${p.str("label")} · ${p.str("text")}" })
                    PickIn(f, "slot_id", "Slot", listOf("" to "Any free slot (automatic)") + o.list("slots").map { s -> s.str("id") to "${s.str("name")} · ${s.str("status")}" + (s.str("until").ifBlank { null }?.let { " · to $it" } ?: "") })
                    o.str("next_free").ifBlank { null }?.let { Text("All busy? Next one frees at $it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else if (f["tool_id"].isNotBlank()) Text("Loading plans…", style = MaterialTheme.typography.bodySmall)
                SwitchIn(f, "use_custom_price", "Custom price", "Instead of the plan price")
                if (f.on("use_custom_price")) TextIn(f, "custom_price", "Price", number = true)
                TextIn(f, "notes", "Note (optional)", lines = 2)
            }
        }
    }
}

// ── walk-in clients ────────────────────────────────────────────────────────

@Composable
fun ClientsPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_clients") { PageVM(api, "/admin/clients") }
    val toast = rememberToast()
    var editing by remember { mutableStateOf<JsonElement?>(null) }
    var open by remember { mutableStateOf(false) }
    val f = remember { FormState() }
    Panel("Walk-in clients", ctx.nav, vm, actions = {
        IconButton(onClick = { f.values.clear(); editing = null; open = true }) { Icon(Icons.Outlined.Add, "Add client") }
    }) { d ->
        item { SearchBox(vm.query["q"].orEmpty(), "Name, phone, email or ID", { vm.set("q", it) }) }
        val rows = d.list("rows")
        if (rows.isEmpty()) item { EmptyState(Icons.Outlined.People, "No clients", "Customers who rent at your shop without an account.") }
        items(rows, key = { it.int("id") }) { c ->
            Line(
                c.str("full_name"),
                listOfNotNull(c.str("phone").ifBlank { null }, c.str("email").ifBlank { null }, "${c.int("rentals")} rentals" + if (c.int("active") > 0) " · ${c.int("active")} running" else "").joinToString(" · "),
                leading = { com.aamirbuneri.abgsmrental.ui.admin.PersonAvatar(c.str("full_name"), 40.dp) },
                value = if (c.dbl("due") > 0) "Due ${money(c.dbl("due"), ctx.currency)}" else null,
                valueColor = AB.brand.danger,
                menu = listOf<Pair<String, () -> Unit>>(
                    "Edit" to { f.values.clear(); listOf("full_name", "phone", "email", "address", "id_number", "notes").forEach { k -> f[k] = c.str(k) }; editing = c; open = true },
                    "Assign a tool" to { ctx.go("assign") },
                    "Delete" to { vm.submit("/admin/clients/${c.int("id")}/delete", form(), toast) },
                ),
            )
        }
        pager(d, vm)
    }
    Sheet(open, if (editing != null) "Edit client" else "New client", { open = false }, action = "Save", busy = vm.busy, onAction = {
        vm.submit("/admin/clients", f.json(extra = editing?.let { mapOf("id" to it.str("id")) } ?: emptyMap()), toast) { open = false }
    }) {
        TextIn(f, "full_name", "Name")
        TextIn(f, "phone", "Phone", keyboard = androidx.compose.ui.text.input.KeyboardType.Phone)
        TextIn(f, "email", "Email", keyboard = androidx.compose.ui.text.input.KeyboardType.Email)
        TextIn(f, "id_number", "ID card number")
        TextIn(f, "address", "Address", lines = 2)
        TextIn(f, "notes", "Notes", lines = 2)
    }
}

// ── returns ────────────────────────────────────────────────────────────────

@Composable
fun ReturnsPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_returns") { PageVM(api, "/admin/returns", mapOf("status" to "pending")) }
    val toast = rememberToast()
    var acting by remember { mutableStateOf<Pair<JsonElement, Boolean>?>(null) }
    val f = remember { FormState() }
    Panel("Returns", ctx.nav, vm) { d ->
        val c = d.obj("counts")
        item {
            FilterRow(listOf(
                FilterOption("pending", "Waiting", c.int("pending")), FilterOption("approved", "Approved", c.int("approved")), FilterOption("rejected", "Rejected", c.int("rejected")),
            ), vm.query["status"] ?: "pending", { vm.set("status", it) }, Modifier.padding(horizontal = 0.dp))
        }
        val rows = d.list("rows")
        if (rows.isEmpty()) item { EmptyState(Icons.AutoMirrored.Outlined.Undo, "No returns", "When a reseller says a tool didn’t work, the request shows up here.") }
        items(rows, key = { it.int("id") }) { t ->
            GlassCard(Modifier.fillMaxWidth()) {
                Text("${t.str("tool_name")} · ${t.str("slot_name")}", style = MaterialTheme.typography.titleSmall)
                Text("${t.str("requester")} · ${t.str("rental_number")} · ${money(t.dbl("price"), t.str("currency"))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("“${t.str("reason")}”", style = MaterialTheme.typography.bodyMedium)
                Text("Asked ${ago(t.str("requested_at"))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (t.str("status") == "pending") {
                    Spacer(Modifier.height(10.dp))
                    Text("Suggested refund ${money(t.dbl("suggested"), t.str("currency"))} (up to ${money(minOf(t.dbl("price"), t.dbl("refundable")), t.str("currency"))})", style = MaterialTheme.typography.labelMedium, color = AB.brand.info)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientButton("Approve", { f.values.clear(); f["refund"] = num(t.dbl("suggested")); acting = t to true }, Modifier.weight(1f), height = 46.dp)
                        SecondaryButton("Reject", { f.values.clear(); acting = t to false }, Modifier.weight(1f))
                    }
                } else {
                    Spacer(Modifier.height(6.dp))
                    InfoRow("Result", (if (t.str("status") == "approved") "Approved" else "Rejected") + (t.str("refund_amount").ifBlank { null }?.let { " · refunded ${money(t.dbl("refund_amount"), t.str("currency"))}" } ?: ""))
                    if (t.str("admin_note").isNotBlank()) InfoRow("Note", t.str("admin_note"))
                }
            }
        }
    }
    val a = acting
    Sheet(a != null, if (a?.second == true) "Approve return" else "Reject return", { acting = null }, action = if (a?.second == true) "Approve" else "Reject", busy = vm.busy, onAction = {
        if (a != null) vm.submit("/admin/returns/${a.first.int("id")}/process", f.json(extra = mapOf("action" to if (a.second) "approve" else "reject")), toast) { acting = null }
    }) {
        if (a?.second == true) {
            Text("The rental closes, the slot is freed, and the refund goes back to the reseller’s wallet. Change the slot password afterwards.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextIn(f, "refund", "Refund (${a.first.str("currency")})", number = true)
        }
        TextIn(f, "note", "Note for the reseller", lines = 3)
    }
}

// ── invoices ───────────────────────────────────────────────────────────────

@Composable
fun InvoicesPanel(ctx: PanelCtx, reseller: Boolean = false) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_invoices$reseller") { PageVM(api, if (reseller) "/reseller/invoices" else "/admin/invoices") }
    Panel("Invoices", ctx.nav, vm) { d ->
        if (!reseller) {
            val c = d.obj("counts")
            item { SearchBox(vm.query["q"].orEmpty(), "Invoice, tool or renter", { vm.set("q", it) }) }
            item {
                FilterRow(listOf(FilterOption("", "All"), FilterOption("unpaid", "Unpaid", c.int("unpaid")), FilterOption("partial", "Part paid", c.int("partial")), FilterOption("paid", "Paid", c.int("paid")), FilterOption("void", "Void")),
                    vm.query["status"] ?: "", { vm.set("status", it) }, Modifier.padding(horizontal = 0.dp))
            }
            d.list("totals").firstOrNull()?.let { t ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stat("Billed", money(t.dbl("billed"), t.str("currency")), Modifier.weight(1f))
                        Stat("Due", money(t.dbl("due"), t.str("currency")), Modifier.weight(1f), tint = AB.brand.danger)
                    }
                }
            }
        }
        val rows = d.list("rows")
        if (rows.isEmpty()) item { EmptyState(Icons.AutoMirrored.Outlined.ReceiptLong, "No invoices", "Every rental gets an invoice.") }
        items(rows, key = { it.int("id") }) { i ->
            val st = i.str("payment_status")
            Line(
                i.str("invoice_number"),
                listOfNotNull(if (!reseller) i.str("renter_name") else null, i.str("tool_name"), shortDate(i.str("created_at"))).joinToString(" · "),
                pill = st.replaceFirstChar { it.uppercase() } to tone(st),
                value = money(i.dbl("total_amount"), i.str("currency")),
                onClick = { ctx.go(if (reseller) "rinvoice" else "invoice", i.int("id")) },
            )
        }
        pager(d, vm)
    }
}

@Composable
fun InvoicePanel(ctx: PanelCtx, id: Int, reseller: Boolean = false) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_invoice$id") { PageVM(api, if (reseller) "/reseller/invoices/$id" else "/admin/invoices/$id") }
    val toast = rememberToast()
    var pay by remember { mutableStateOf(false) }
    val f = remember { FormState(mapOf("method" to "cash")) }
    val inv = vm.data.obj("inv")
    val due = inv.dbl("total_amount") - inv.dbl("paid_amount")
    val payBar: (@Composable () -> Unit)? = if (!reseller && due > 0.004 && inv.str("payment_status") != "void") {
        { GradientButton("Record payment", { f["amount"] = num(due); pay = true }, Modifier.fillMaxWidth()) }
    } else null
    Panel(inv.str("invoice_number").ifBlank { "Invoice" }, ctx.nav, vm, bottom = payBar) { d ->
        val i = d.obj("inv")
        val cur = i.str("currency")
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(i.str("tool_name"), style = MaterialTheme.typography.titleMedium)
                        Text("${i.str("slot_name")} · ${i.str("duration_label")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    com.aamirbuneri.abgsmrental.ui.components.StatusPill(i.str("payment_status").replaceFirstChar { it.uppercase() }, tone(i.str("payment_status")))
                }
                Spacer(Modifier.height(10.dp))
                InfoRow("Total", money(i.dbl("total_amount"), cur), MaterialTheme.colorScheme.primary)
                InfoRow("Paid", money(i.dbl("paid_amount"), cur))
                if (due > 0.004) InfoRow("Due", money(due, cur), AB.brand.danger)
                InfoRow("Renter", i.str("renter_name") + i.str("company_name").let { if (it.isBlank()) "" else " · $it" })
                if (i.str("renter_phone").isNotBlank()) InfoRow("Phone", i.str("renter_phone"))
                InfoRow("Rental", i.str("rental_number"))
                InfoRow("Issued", dateTime(i.str("created_at")))
                InfoRow("Period", "${shortDate(i.str("start_time"))} → ${shortDate(i.str("expiry_time"))}")
            }
        }
        val pays = d.list("payments")
        if (pays.isNotEmpty()) {
            item { Section("Payments") }
            items(pays.size) { k ->
                val p = pays[k]
                Line(money(p.dbl("amount"), cur), listOfNotNull(PAY_METHODS.firstOrNull { it.first == p.str("method") }?.second ?: p.str("method"), p.str("reference").ifBlank { null }, shortDate(p.str("created_at"))).joinToString(" · "))
            }
        }
    }
    Sheet(pay, "Record payment", { pay = false }, action = "Save", busy = vm.busy, onAction = {
        vm.submit("/admin/invoices/$id/payment", f.json(), toast) { pay = false }
    }) {
        TextIn(f, "amount", "Amount (${inv.str("currency")})", number = true)
        ChoiceIn(f, "method", "Paid by", PAY_METHODS)
        TextIn(f, "reference", "Reference (optional)")
        TextIn(f, "notes", "Note (optional)", lines = 2)
    }
}

// ── reports, profit, account costs ─────────────────────────────────────────

@Composable
fun ReportsPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_reports") { PageVM(api, "/admin/reports", mapOf("days" to "30")) }
    Panel("Reports", ctx.nav, vm) { d ->
        val cur = d.str("currency", ctx.currency)
        item {
            FilterRow(listOf(FilterOption("7", "7 days"), FilterOption("30", "30 days"), FilterOption("90", "90 days"), FilterOption("365", "1 year")), vm.query["days"] ?: "30", { vm.set("days", it) }, Modifier.padding(horizontal = 0.dp))
        }
        val curs = d.list("currencies").mapNotNull { it.text() }
        if (curs.size > 1) item { FilterRow(curs.map { FilterOption(it, it) }, cur, { vm.set("cur", it) }, Modifier.padding(horizontal = 0.dp)) }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Sales by day", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                WeekBars(d.list("chart").map { it.str("label") to it.dbl("value") }, cur)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Rental sales", money(d.obj("sales").dbl("total"), cur), Modifier.weight(1f), sub = "${d.obj("sales").int("n")} rentals")
                Stat("Refunds", money(d.dbl("refunds"), cur), Modifier.weight(1f), tint = AB.brand.danger)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Wallet top-ups", money(d.dbl("topups"), cur), Modifier.weight(1f), tint = AB.brand.success)
                Stat("Cash received", money(d.dbl("cash"), cur), Modifier.weight(1f), tint = AB.brand.info)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Unpaid invoices", money(d.dbl("outstanding"), cur), Modifier.weight(1f), tint = AB.brand.warning)
                Stat("Walk-in", money(d.obj("walkin").dbl("total"), cur), Modifier.weight(1f), sub = "${d.obj("walkin").int("n")} rentals")
            }
        }
        val svc = d.obj("svc")
        if (svc != null && (svc.int("n") > 0 || svc.dbl("gross") > 0)) item {
            Stat("Remote services (net)", money(svc.dbl("net"), cur), Modifier.fillMaxWidth(), sub = "${svc.int("n")} paid orders · ${money(svc.dbl("refunds"), cur)} refunded")
        }
        rank("Top tools", d.list("byTool"), cur) { it.str("name") }
        rank("Top resellers", d.list("byReseller"), cur) { it.str("full_name").ifBlank { it.str("username") } }
        if (svc != null) rank("Top services", svc.list("by"), cur) { it.str("name") }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.rank(title: String, rows: List<JsonElement>, cur: String, name: (JsonElement) -> String) {
    if (rows.isEmpty()) return
    item { Section(title) }
    item {
        GlassCard(Modifier.fillMaxWidth()) {
            val max = rows.maxOf { it.dbl("total") }.coerceAtLeast(1.0)
            rows.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(name(r), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("${r.int("n")} · ${money(r.dbl("total"), cur)}", style = MaterialTheme.typography.labelLarge)
                }
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { (r.dbl("total") / max).toFloat() }, modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
fun ProfitPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_profit") { PageVM(api, "/admin/profit", mapOf("range" to "30")) }
    Panel("Profit & ROI", ctx.nav, vm) { d ->
        val cur = d.str("base", ctx.currency)
        val t = d.obj("rep").obj("totals")
        item {
            FilterRow(listOf(FilterOption("7", "7 days"), FilterOption("30", "30 days"), FilterOption("month", "This month"), FilterOption("last_month", "Last month"), FilterOption("90", "90 days"), FilterOption("365", "12 months")),
                vm.query["range"] ?: "30", { vm.set("range", it) }, Modifier.padding(horizontal = 0.dp))
        }
        item { Text(d.str("label"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Revenue", money(t.dbl("grand_revenue"), cur), Modifier.weight(1f))
                Stat("Profit", money(t.dbl("grand_profit"), cur), Modifier.weight(1f), tint = if (t.dbl("grand_profit") >= 0) AB.brand.success else AB.brand.danger,
                    sub = t.str("grand_margin").ifBlank { null }?.let { "${it.toDoubleOrNull()?.let { m -> "%.0f".format(m) } ?: it}% margin" })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Account costs", money(t.dbl("cost"), cur), Modifier.weight(1f), tint = AB.brand.warning)
                Stat("Slot use", "%.0f%%".format(t.dbl("util")), Modifier.weight(1f), tint = AB.brand.info, sub = "${t.int("slots")} slots")
            }
        }
        val tools = d.obj("rep").list("tools")
        if (tools.isNotEmpty()) item { Section("By tool") }
        items(tools.size) { k ->
            val x = tools[k]
            GlassCard(Modifier.fillMaxWidth()) {
                Row {
                    Text(x.str("name"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(money(x.dbl("profit"), cur), style = MaterialTheme.typography.titleSmall, color = if (x.dbl("profit") >= 0) AB.brand.success else AB.brand.danger, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Revenue ${money(x.dbl("revenue"), cur)} · costs ${money(x.dbl("cost"), cur)} · ${x.int("rentals")} rentals · use %.0f%%".format(x.dbl("util")) +
                        (x.str("roi").ifBlank { null }?.toDoubleOrNull()?.let { " · lifetime ROI %.0f%%".format(it) } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun AccountsPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_accounts") { PageVM(api, "/admin/accounts") }
    val toast = rememberToast()
    var addCost by remember { mutableStateOf(false) }
    var renew by remember { mutableStateOf<JsonElement?>(null) }
    val f = remember { FormState() }
    Panel("Tool accounts & costs", ctx.nav, vm, actions = {
        IconButton(onClick = { f.values.clear(); f["kind"] = "expense"; addCost = true }) { Icon(Icons.Outlined.Add, "Add a cost") }
    }) { d ->
        val c = d.obj("counts")
        item {
            FilterRow(listOf(FilterOption("all", "All", c.int("all")), FilterOption("expired", "Expired", c.int("expired")), FilterOption("soon", "Ending soon", c.int("soon")), FilterOption("ok", "OK", c.int("ok")), FilterOption("unset", "No date", c.int("unset"))),
                vm.query["f"] ?: "all", { vm.set("f", it) }, Modifier.padding(horizontal = 0.dp))
        }
        items(d.list("rows"), key = { "s" + it.int("id") }) { s ->
            val st = s.obj("st")
            Line(
                "${s.str("tool_name")} · ${s.str("slot_name")}",
                listOfNotNull(st.str("label"), "Paid ${money(s.dbl("total_paid"), ctx.currency)}" + (s.str("last_paid").ifBlank { null }?.let { " · last ${shortDate(it)}" } ?: "")).joinToString("\n"),
                leading = { Dot(s.str("tool_name"), s.str("color")) },
                pill = when (st.str("tone")) { "red" -> "Expired" to Tone.Danger; "amber" -> "Soon" to Tone.Warning; "green" -> "OK" to Tone.Success; else -> null },
                menu = listOf<Pair<String, () -> Unit>>("Renew / paid" to { f.values.clear(); f["amount"] = num(s.dbl("renew_cost").takeIf { it > 0 }); renew = s }),
            )
        }
        val costs = d.list("costs")
        if (costs.isNotEmpty()) item { Section("Recent costs") }
        items(costs, key = { "c" + it.int("id") }) { x ->
            Line(
                money(x.dbl("amount"), ctx.currency),
                listOfNotNull(x.str("kind").replaceFirstChar { it.uppercase() }, x.str("tool_name").ifBlank { null }, x.str("slot_name").ifBlank { null }, x.str("note").ifBlank { null }, shortDate(x.str("created_at"))).joinToString(" · "),
                menu = listOf<Pair<String, () -> Unit>>("Delete" to { vm.submit("/admin/costs/${x.int("id")}/delete", form(), toast) }),
            )
        }
    }
    val r = renew
    Sheet(r != null, "Renew ${r.str("slot_name")}", { renew = null }, action = "Save", busy = vm.busy, onAction = {
        if (r != null) vm.submit("/admin/slots/${r.int("id")}/renew", f.json(extra = mapOf("back" to "/admin/accounts")), toast) { renew = null }
    }) {
        DateIn(f, "expires_at", "Valid until")
        TextIn(f, "amount", "Amount paid (${ctx.currency})", number = true)
        TextIn(f, "note", "Note (optional)")
    }
    Sheet(addCost, "Add a cost", { addCost = false }, action = "Save", busy = vm.busy, onAction = {
        vm.submit("/admin/costs", f.json(extra = mapOf("back" to "/admin/accounts")), toast) { addCost = false }
    }) {
        val d = vm.data
        ChoiceIn(f, "kind", "Type", listOf("purchase" to "Purchase", "renewal" to "Renewal", "credits" to "Credits top-up", "expense" to "Other expense"))
        PickIn(f, "target", "For", d.list("tools").map { t -> "t:${t.str("id")}" to "${t.str("name")} (whole tool)" } +
            d.list("allSlots").map { s -> "s:${s.str("id")}" to (d.list("tools").firstOrNull { it.str("id") == s.str("tool_id") }?.str("name").orEmpty() + " · " + s.str("slot_name")) })
        TextIn(f, "amount", "Amount (${ctx.currency})", number = true)
        DateIn(f, "period_start", "From (optional)")
        DateIn(f, "period_end", "Until (optional)", hint = "The cost is spread over this period in profit reports")
        TextIn(f, "note", "Note (optional)")
    }
}

