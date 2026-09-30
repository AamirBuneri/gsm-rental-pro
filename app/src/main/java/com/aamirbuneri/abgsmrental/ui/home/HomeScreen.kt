package com.aamirbuneri.abgsmrental.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Timer
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
import com.aamirbuneri.abgsmrental.data.Dashboard
import com.aamirbuneri.abgsmrental.data.Order
import com.aamirbuneri.abgsmrental.data.Rental
import com.aamirbuneri.abgsmrental.data.Settings
import com.aamirbuneri.abgsmrental.ui.DataVM
import com.aamirbuneri.abgsmrental.ui.Load
import com.aamirbuneri.abgsmrental.ui.Routes
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
import com.aamirbuneri.abgsmrental.ui.util.openWhatsApp
import com.aamirbuneri.abgsmrental.ui.util.timeOnly
import com.aamirbuneri.abgsmrental.work.Reminders
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavHostController, shell: Shell, settings: Settings) {
    val context = LocalContext.current
    val api = context.container.api
    val vm: DataVM<Dashboard> = viewModel(key = "home") { DataVM { api.dashboard() } }
    NotificationPermission()

    // keep it fresh while open
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            vm.refresh()
        }
    }
    val state = vm.state
    LaunchedEffect(state) {
        if (state is Load.Ok) {
            shell.unread = state.data.unread
            shell.services = state.data.services
            Reminders.sync(context, state.data.activeRentals)
        }
    }

    ScreenBackground {
        PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            when (state) {
                is Load.Loading -> Column(Modifier.statusBarsPadding()) {
                    Header(settings.username, settings.siteName, 0) {}
                    LoadingCards(4, 110.dp)
                }
                is Load.Err -> Column(Modifier.statusBarsPadding()) {
                    Header(settings.username, settings.siteName, shell.unread) { nav.navigate(Routes.NOTIFICATIONS) }
                    ErrorState(state.error.message ?: "", state.error.offline) { vm.refresh() }
                }
                is Load.Ok -> HomeContent(nav, shell, state.data, settings, vm.refreshError)
            }
        }
    }
}

@Composable
private fun HomeContent(nav: NavHostController, shell: Shell, d: Dashboard, settings: Settings, refreshError: String?) {
    val context = LocalContext.current
    val a = d.account
    val whatsapp = shell.info?.support?.whatsapp.orEmpty()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Column(Modifier.statusBarsPadding()) { Header(a.displayName, settings.siteName, d.unread) { nav.navigate(Routes.NOTIFICATIONS) } } }
        if (refreshError != null) item { InlineError(refreshError, Modifier.padding(horizontal = 16.dp)) }
        item {
            BalanceCard(
                balance = money(a.balance, a.currency),
                negative = a.balance < 0,
                sub = when {
                    a.canSpend == null -> "Unlimited credit"
                    a.creditAllowed && (a.creditLimit ?: 0.0) > 0 -> "Can spend ${money(a.canSpend ?: 0.0, a.currency)} · credit ${money(a.creditLimit ?: 0.0, a.currency)}"
                    else -> "Available to spend"
                },
                onTopUp = {
                    if (whatsapp.isNotBlank()) openWhatsApp(context, whatsapp, "Hi, I want to top up my wallet. Username: ${a.username}")
                    else nav.navigate(Routes.WALLET)
                },
                onWallet = { nav.navigate(Routes.WALLET) },
            )
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Rent a tool", Icons.Outlined.Build, AB.brand.gradientColors.first(), Modifier.weight(1f)) { nav.goTab(Routes.RENT) }
                if (d.services) QuickAction("Order service", Icons.Outlined.SupportAgent, AB.brand.info, Modifier.weight(1f)) { nav.goTab(Routes.SERVICES) }
                QuickAction("My rentals", Icons.Outlined.Timer, AB.brand.warning, Modifier.weight(1f)) { nav.goTab(Routes.RENTALS) }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Running", "${d.activeRentals.size}", Icons.Outlined.Timer, AB.brand.success, Modifier.weight(1f))
                StatTile("This month", "${d.stats.monthRentals}", Icons.Outlined.CalendarMonth, AB.brand.info, Modifier.weight(1f))
                StatTile("Spent", money(d.stats.monthSpent, a.currency), Icons.Outlined.Payments, AB.brand.warning, Modifier.weight(1f))
            }
        }
        item { SectionTitle("Running now", Modifier.padding(horizontal = 16.dp), if (d.activeRentals.isNotEmpty()) "See all" else null) { nav.goTab(Routes.RENTALS) } }
        item {
            if (d.activeRentals.isEmpty()) {
                GlassCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), onClick = { nav.goTab(Routes.RENT) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.Add, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("No tool running", style = MaterialTheme.typography.titleSmall)
                            Text("Rent a tool — the login is ready in seconds.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(d.activeRentals, key = { it.id }) { r -> RunningCard(r) { nav.navigate(Routes.rental(r.id)) } }
                }
            }
        }
        if (d.services && d.openOrders.isNotEmpty()) {
            item { SectionTitle("Open orders", Modifier.padding(horizontal = 16.dp), "All orders") { nav.goTab(Routes.SERVICES) } }
            items(d.openOrders, key = { "o" + it.id }) { o -> OrderRow(o, Modifier.padding(horizontal = 16.dp)) { nav.navigate(Routes.order(o.id)) } }
        }
    }
}

@Composable
private fun Header(name: String, siteName: String, unread: Int, onBell: () -> Unit) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val hello = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(hello + if (siteName.isNotBlank()) " · $siteName" else "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(name.ifBlank { "Welcome" }, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onBell) {
            BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 99) "99+" else "$unread") } }) {
                Icon(Icons.Outlined.Notifications, "Notifications")
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: String, negative: Boolean, sub: String, onTopUp: () -> Unit, onWallet: () -> Unit) {
    val b = AB.brand
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(b.gradient)
            .drawBehind {
                // soft rings, like the logo's orbit
                drawCircle(Color.White.copy(alpha = 0.10f), radius = size.width * 0.42f, center = Offset(size.width * 0.95f, size.height * 0.05f))
                drawCircle(Color.White.copy(alpha = 0.07f), radius = size.width * 0.30f, center = Offset(size.width * 0.05f, size.height * 1.05f))
            }
            .padding(22.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccountBalanceWallet, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Wallet balance", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text(balance, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            if (negative) Text("You owe this amount", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(2.dp))
            Text(sub, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Top up", Icons.Outlined.Add, filled = true, onClick = onTopUp)
                PillButton("History", Icons.Outlined.Payments, filled = false, onClick = onWallet)
            }
        }
    }
}

@Composable
private fun PillButton(text: String, icon: ImageVector, filled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = if (filled) 0f else 0.35f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val c = if (filled) Color(0xFF0F55AF) else Color.White
        Icon(icon, null, tint = c, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = c, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier, onClick = onClick, padding = PaddingValues(vertical = 14.dp, horizontal = 10.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(icon, tint, 44.dp)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    GlassCard(modifier, padding = PaddingValues(14.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun RunningCard(r: Rental, onClick: () -> Unit) {
    GlassCard(Modifier.width(250.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemAvatar(r.tool, r.color, null, 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(r.tool, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(r.plan, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CountdownRing(r.endsAtMs, r.minutes, size = 70.dp, stroke = 6.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Ends at", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(timeOnly(r.endsAtMs), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("Show login →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun OrderRow(o: Order, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.SupportAgent, AB.brand.info)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(o.service, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(o.device, o.number, ago(o.createdAt)).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(o.statusLabel.ifBlank { o.status }, orderTone(o.status))
                if (o.price != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(money(o.price, o.currency), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Ask once (Android 13+) so rental reminders and updates can show. */
@Composable
private fun NotificationPermission() {
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
