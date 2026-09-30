package com.aamirbuneri.abgsmrental.ui.admin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.AdminDashboard
import com.aamirbuneri.abgsmrental.data.AdminOrder
import com.aamirbuneri.abgsmrental.data.AdminRental
import com.aamirbuneri.abgsmrental.data.Perm
import com.aamirbuneri.abgsmrental.data.Settings
import com.aamirbuneri.abgsmrental.ui.DataVM
import com.aamirbuneri.abgsmrental.ui.Load
import com.aamirbuneri.abgsmrental.ui.Shell
import com.aamirbuneri.abgsmrental.ui.components.CountdownRing
import com.aamirbuneri.abgsmrental.ui.components.ErrorState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.IconBadge
import com.aamirbuneri.abgsmrental.ui.components.InlineError
import com.aamirbuneri.abgsmrental.ui.components.ItemAvatar
import com.aamirbuneri.abgsmrental.ui.components.LoadingCards
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.components.SectionTitle
import com.aamirbuneri.abgsmrental.ui.components.StatusPill
import com.aamirbuneri.abgsmrental.ui.components.orderTone
import com.aamirbuneri.abgsmrental.ui.goTab
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.ago
import com.aamirbuneri.abgsmrental.ui.util.money
import com.aamirbuneri.abgsmrental.ui.util.shortDate
import com.aamirbuneri.abgsmrental.ui.util.timeOnly
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(nav: NavHostController, shell: Shell, admin: AdminShellState, settings: Settings) {
    val context = LocalContext.current
    val api = context.container.api
    val vm: DataVM<AdminDashboard> = viewModel(key = "a_home") { DataVM { api.adminDashboard() } }
    AskNotificationPermission()
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); vm.refresh() }
    }
    val state = vm.state
    LaunchedEffect(state) {
        if (state is Load.Ok) {
            val d = state.data
            shell.unread = d.unread
            admin.servicesEnabled = d.servicesEnabled
            admin.openOrders = d.services?.let { it.ready + it.needQuote } ?: 0
            admin.pendingRegistrations = d.pendingRegistrations ?: 0
            // the owner changed this staff member's permissions → tabs follow
            if (d.me.role.isNotBlank() && (d.me.role != settings.role || d.me.perms.toSet() != settings.perms.toSet())) {
                context.container.prefs.setRole(d.me)
            }
        }
    }
    ScreenBackground {
        PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            when (state) {
                is Load.Loading -> Column(Modifier.statusBarsPadding()) {
                    Header(settings, shell.unread, null) {}
                    LoadingCards(4, 110.dp)
                }
                is Load.Err -> Column(Modifier.statusBarsPadding()) {
                    Header(settings, shell.unread, null) { nav.navigate(AdminRoutes.NOTIFICATIONS) }
                    ErrorState(state.error.message ?: "", state.error.offline) { vm.refresh() }
                }
                is Load.Ok -> Content(nav, admin, state.data, settings, shell, vm.refreshError)
            }
        }
    }
}

@Composable
private fun Content(nav: NavHostController, admin: AdminShellState, d: AdminDashboard, settings: Settings, shell: Shell, refreshError: String?) {
    val b = AB.brand
    val can = { p: String -> settings.can(p) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Column(Modifier.statusBarsPadding()) { Header(settings, d.unread, d) { nav.navigate(AdminRoutes.NOTIFICATIONS) } } }
        if (refreshError != null) item { InlineError(refreshError, Modifier.padding(horizontal = 16.dp)) }

        // hero: money for the owner / Money permission, otherwise the live rentals count
        item {
            val s = d.sales
            if (s != null) SalesHero(d, s.today, s.yesterday, s.monthRevenue, s.monthProfit, s.week.map { it.label to it.value })
            else StaffHero(d)
        }

        // what needs a decision now
        val attention = buildList<Triple<String, ImageVector, String>> {
            d.pendingRegistrations?.takeIf { it > 0 }?.let { add(Triple("$it new reseller${if (it == 1) "" else "s"} waiting for approval", Icons.Outlined.HowToReg, "reg")) }
            d.services?.takeIf { it.needQuote > 0 }?.let { add(Triple("${it.needQuote} ${if (it.needQuote == 1) "order needs" else "orders need"} a price", Icons.Outlined.RequestQuote, "quote")) }
            d.services?.takeIf { it.ready > 0 }?.let { add(Triple("${it.ready} paid order${if (it.ready == 1) "" else "s"} ready to start", Icons.Outlined.SupportAgent, "ready")) }
            d.pendingReturns?.takeIf { it > 0 }?.let { add(Triple("$it return request${if (it == 1) "" else "s"} (open on the website)", Icons.AutoMirrored.Outlined.Undo, "returns")) }
            if (d.expiringAccounts.isNotEmpty()) add(Triple("${d.expiringAccounts.size} tool account${if (d.expiringAccounts.size == 1) "" else "s"} expiring soon", Icons.Outlined.EventBusy, "expiring"))
        }
        if (attention.isNotEmpty()) {
            item { SectionTitle("Needs attention", Modifier.padding(horizontal = 16.dp)) }
            items(attention, key = { it.third }) { (text, icon, key) ->
                val tint = when (key) { "reg" -> b.info; "quote" -> b.warning; "ready" -> b.success; else -> b.danger }
                GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), padding = PaddingValues(14.dp), onClick = {
                    when (key) {
                        "reg" -> nav.goFiltered(admin, AdminRoutes.RESELLERS, "pending")
                        "quote", "ready" -> nav.goFiltered(admin, AdminRoutes.ORDERS, "open")
                        "expiring" -> nav.navigate(AdminRoutes.TOOLS)
                        else -> {}
                    }
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon, tint, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        if (key != "returns") Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // tiles
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("Running rentals", "${d.rentals.active}", Icons.Outlined.Timer, b.success, Modifier.weight(1f), hint = "+${d.rentals.today} today",
                        onClick = if (can(Perm.RENTALS)) ({ nav.goFiltered(admin, AdminRoutes.RENTALS, "active") }) else null)
                    MiniStat("Free slots", "${d.slots.free}/${d.slots.total}", Icons.Outlined.Inventory2, b.info, Modifier.weight(1f), hint = "${d.slots.busy} busy",
                        onClick = if (can(Perm.TOOLS)) ({ nav.navigate(AdminRoutes.TOOLS) }) else null)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val sv = d.services
                    if (sv != null) MiniStat("Open orders", "${sv.open}", Icons.Outlined.SupportAgent, b.warning, Modifier.weight(1f), hint = if (sv.inProgress > 0) "${sv.inProgress} working" else null,
                        onClick = { nav.goFiltered(admin, AdminRoutes.ORDERS, "open") })
                    MiniStat("Active resellers", "${d.resellers}", Icons.Outlined.Groups, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f),
                        hint = d.pendingRegistrations?.takeIf { it > 0 }?.let { "$it waiting" },
                        onClick = if (can(Perm.RESELLERS)) ({ nav.goTab(AdminRoutes.RESELLERS) }) else null)
                }
                val s = d.sales
                if (s != null && s.outstanding > 0) {
                    MiniStat("Unpaid invoices", money(s.outstanding, d.currency), Icons.Outlined.AccountBalanceWallet, b.danger, Modifier.fillMaxWidth())
                }
            }
        }

        if (d.queue.isNotEmpty()) {
            item { SectionTitle("Order queue", Modifier.padding(horizontal = 16.dp), "All orders") { nav.goFiltered(admin, AdminRoutes.ORDERS, "open") } }
            items(d.queue, key = { "q" + it.id }) { o -> QueueRow(o, Modifier.padding(horizontal = 16.dp)) { nav.navigate(AdminRoutes.order(o.id)) } }
        }
        if (d.live.isNotEmpty()) {
            item { SectionTitle("Running now", Modifier.padding(horizontal = 16.dp), "All rentals") { nav.goFiltered(admin, AdminRoutes.RENTALS, "active") } }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(d.live, key = { it.id }) { r -> LiveCard(r) { nav.navigate(AdminRoutes.rental(r.id)) } }
                }
            }
        }
        if (d.expiringAccounts.isNotEmpty()) {
            item { SectionTitle("Tool accounts expiring", Modifier.padding(horizontal = 16.dp), "Tools") { nav.navigate(AdminRoutes.TOOLS) } }
            items(d.expiringAccounts, key = { "x" + it.slotId }) { x ->
                GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), padding = PaddingValues(14.dp), onClick = { nav.navigate(AdminRoutes.slots(x.toolId)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.EventBusy, b.danger, 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${x.tool} · ${x.slot}", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Account ends ${shortDate(x.expiresAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (attention.isEmpty() && d.queue.isEmpty() && d.live.isEmpty()) {
            item {
                GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.Build, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("All quiet", style = MaterialTheme.typography.titleSmall)
                            Text("Nothing is waiting for you right now.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(settings: Settings, unread: Int, d: AdminDashboard?, onBell: () -> Unit) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val hello = when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    val name = d?.me?.name?.ifBlank { null } ?: settings.username
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(hello, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                RoleBadge(if (settings.owner) "Owner" else "Staff", if (settings.owner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            }
            Text(name.ifBlank { "Admin" }, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onBell) {
            BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 99) "99+" else "$unread") } }) {
                Icon(Icons.Outlined.Notifications, "Notifications")
            }
        }
    }
}

@Composable
private fun HeroBox(content: @Composable () -> Unit) {
    val b = AB.brand
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(b.gradient)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.10f), radius = size.width * 0.42f, center = Offset(size.width * 0.95f, size.height * 0.02f))
                drawCircle(Color.White.copy(alpha = 0.07f), radius = size.width * 0.30f, center = Offset(size.width * 0.02f, size.height * 1.05f))
            }
            .padding(20.dp),
    ) { content() }
}

@Composable
private fun SalesHero(d: AdminDashboard, today: Double, yesterday: Double, monthRevenue: Double, monthProfit: Double, week: List<Pair<String, Double>>) {
    HeroBox {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Payments, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sales today", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                val diff = today - yesterday
                if (yesterday > 0 || today > 0) {
                    Row(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.TrendingUp, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text((if (diff >= 0) "+" else "−") + money(kotlin.math.abs(diff), d.currency) + " vs yesterday", color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(money(today, d.currency), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text("${d.rentals.today} rental${if (d.rentals.today == 1) "" else "s"} today" + (d.services?.let { " · ${it.today} order${if (it.today == 1) "" else "s"}" } ?: ""), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(14.dp))
            WeekBars(week, d.currency, onBrand = true)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroFigure("This month", money(monthRevenue, d.currency), Modifier.weight(1f))
                HeroFigure("Month profit", money(monthProfit, d.currency), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroFigure(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StaffHero(d: AdminDashboard) {
    HeroBox {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Running rentals", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            Text("${d.rentals.active}", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
            Text("${d.rentals.today} started today · ${d.slots.free} of ${d.slots.total} slots free", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            d.services?.let {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroFigure("Ready to start", "${it.ready}", Modifier.weight(1f))
                    HeroFigure("Need a price", "${it.needQuote}", Modifier.weight(1f))
                    HeroFigure("Working", "${it.inProgress}", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QueueRow(o: AdminOrder, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemAvatar(o.service, o.color, o.image, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(o.service, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(o.reseller, o.device, ago(o.createdAt)).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                val (label, tone) = orderBadge(o)
                StatusPill(label, tone)
                if (o.price > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(money(o.price, o.currency), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** What an order needs from the team, in words. */
fun orderBadge(o: AdminOrder) = when {
    o.status == "pending" && !o.paid -> "Needs price" to com.aamirbuneri.abgsmrental.ui.components.Tone.Warning
    o.status == "pending" -> "Ready" to com.aamirbuneri.abgsmrental.ui.components.Tone.Success
    o.status == "quoted" -> "Price sent" to com.aamirbuneri.abgsmrental.ui.components.Tone.Brand
    else -> o.statusLabel.ifBlank { o.status } to orderTone(o.status)
}

@Composable
private fun LiveCard(r: AdminRental, onClick: () -> Unit) {
    GlassCard(Modifier.width(250.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemAvatar(r.tool, r.color, r.image, 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(r.tool, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${r.renter} · ${r.slot}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CountdownRing(r.endsAtMs, r.minutes, size = 64.dp, stroke = 6.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Ends at", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(timeOnly(r.endsAtMs), style = MaterialTheme.typography.titleMedium)
                Text(r.plan, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** Ask once (Android 13+) so new orders and registrations can alert the phone. */
@Composable
private fun AskNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val prefs = context.container.prefs
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted && !prefs.askedPermission()) {
            prefs.setAskedPermission()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
