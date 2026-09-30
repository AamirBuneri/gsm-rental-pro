package com.aamirbuneri.abgsmrental.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.South
import androidx.compose.material.icons.outlined.North
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.AdminReseller
import com.aamirbuneri.abgsmrental.data.Perm
import com.aamirbuneri.abgsmrental.data.Settings
import com.aamirbuneri.abgsmrental.ui.Load
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.ErrorState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.IconBadge
import com.aamirbuneri.abgsmrental.ui.components.InfoRow
import com.aamirbuneri.abgsmrental.ui.components.InlineError
import com.aamirbuneri.abgsmrental.ui.components.LoadingCards
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.components.SecondaryButton
import com.aamirbuneri.abgsmrental.ui.components.StatusPill
import com.aamirbuneri.abgsmrental.ui.components.Tone
import com.aamirbuneri.abgsmrental.ui.components.rentalLabel
import com.aamirbuneri.abgsmrental.ui.components.rentalTone
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.ago
import com.aamirbuneri.abgsmrental.ui.util.dateTime
import com.aamirbuneri.abgsmrental.ui.util.dial
import com.aamirbuneri.abgsmrental.ui.util.money
import com.aamirbuneri.abgsmrental.ui.util.openWhatsApp
import com.aamirbuneri.abgsmrental.ui.util.shortDate

fun resellerState(state: String): Pair<String, Tone> = when (state) {
    "pending" -> "Waiting" to Tone.Warning
    "email" -> "Email not confirmed" to Tone.Info
    "disabled" -> "Disabled" to Tone.Danger
    else -> "Active" to Tone.Success
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminResellersScreen(nav: NavHostController, admin: AdminShellState) {
    val api = LocalContext.current.container.api
    val vm: PagedVM<AdminReseller> = viewModel(key = "a_resellers") {
        PagedVM(admin.resellerFilter ?: "all", { it.id }) { f, q, p -> api.adminResellers(f, q, p) }
    }
    LaunchedEffect(admin.resellerFilter) {
        admin.resellerFilter?.let { vm.pick(it); admin.resellerFilter = null }
    }
    LaunchedEffect(vm.counts) { vm.counts["pending"]?.let { admin.pendingRegistrations = it } }
    val c = vm.counts
    ScreenBackground {
        PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.load(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(Modifier.statusBarsPadding().padding(bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ScreenTitle("Resellers", c["all"]?.let { "$it accounts · ${c["active"] ?: 0} active" })
                        SearchBox(vm.query, "Name, username, email, phone or shop", { vm.search(it) }, Modifier.padding(horizontal = 16.dp))
                        FilterRow(
                            listOf(
                                FilterOption("all", "All"),
                                FilterOption("pending", "Waiting", c["pending"]),
                                FilterOption("active", "Active"),
                                FilterOption("debt", "In debt", c["debt"]),
                                FilterOption("disabled", "Disabled", c["disabled"]),
                            ),
                            vm.filter, { vm.pick(it) },
                        )
                    }
                }
                val list = vm.items
                val err = vm.error
                when {
                    list == null && err != null -> item { ErrorState(err.message ?: "", err.offline) { vm.load() } }
                    list == null -> item { LoadingCards(5, 72.dp) }
                    list.isEmpty() -> item {
                        EmptyState(
                            if (vm.filter == "pending") Icons.Outlined.HowToReg else Icons.Outlined.Groups,
                            if (vm.filter == "pending") "No one waiting" else "No resellers here",
                            if (vm.query.isNotBlank()) "Nothing matches “${vm.query}”." else if (vm.filter == "pending") "New sign-ups that need approval show up here." else "Resellers appear here when they sign up.",
                        )
                    }
                    else -> {
                        if (err != null) item { InlineError(err.message, Modifier.padding(horizontal = 16.dp)) }
                        items(list, key = { it.id }) { r -> ResellerRow(r, Modifier.padding(horizontal = 16.dp)) { nav.navigate(AdminRoutes.reseller(r.id)) } }
                        if (vm.hasMore) item { LoadMore(vm.loadingMore) { vm.more() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResellerRow(r: AdminReseller, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonAvatar(r.displayName, 46.dp, online = r.online)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf("@${r.username}", r.company).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (r.state == "pending") Text("Signed up ${ago(r.createdAt)}", style = MaterialTheme.typography.labelSmall, color = AB.brand.warning)
                else if (r.activeRentals > 0) Text("${r.activeRentals} running", style = MaterialTheme.typography.labelSmall, color = AB.brand.success)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (r.state != "active") {
                    val (label, tone) = resellerState(r.state)
                    StatusPill(label, tone)
                    Spacer(Modifier.height(4.dp))
                }
                Text(money(r.balance, r.currency), style = MaterialTheme.typography.labelLarge, color = if (r.balance < 0) AB.brand.danger else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private enum class ResellerDialog { NONE, ADD, DEDUCT, TOGGLE, APPROVE, REJECT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminResellerScreen(nav: NavHostController, settings: Settings, id: Int) {
    val api = LocalContext.current.container.api
    val vm: DetailVM<AdminReseller> = viewModel(key = "a_reseller-$id") { DetailVM { api.adminReseller(id) } }
    val toast = rememberToast()
    var dialog by remember { mutableStateOf(ResellerDialog.NONE) }
    val state = vm.state
    val r = (state as? Load.Ok)?.data
    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(r?.displayName ?: "Reseller", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is Load.Loading -> LoadingCards(3, 140.dp)
                    is Load.Err -> ErrorState(state.error.message ?: "", state.error.offline) { vm.refresh() }
                    is Load.Ok -> ResellerDetail(nav, settings, state.data, vm.refreshError, vm.busy, onDialog = { dialog = it }) {
                        vm.act(toast, { x -> x.message }) { api.adminResellerAction(id, "confirm-email") }
                    }
                }
            }
        }
    }
    when (dialog) {
        ResellerDialog.NONE -> {}
        ResellerDialog.ADD, ResellerDialog.DEDUCT -> {
            var add by remember { mutableStateOf(dialog == ResellerDialog.ADD) }
            AmountDialog(
                title = "Wallet · ${r?.displayName.orEmpty()}",
                currency = r?.currency ?: "PKR",
                confirm = if (add) "Add money" else "Deduct",
                note = if (add) "Note (e.g. cash, JazzCash ref)" else "Reason",
                extra = {
                    TwoWay("Add", "Deduct", add, { add = it })
                    Text("Balance now: ${money(r?.balance ?: 0.0, r?.currency ?: "PKR")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                onDismiss = { dialog = ResellerDialog.NONE },
            ) { amount, note ->
                dialog = ResellerDialog.NONE
                vm.act(toast, { it.message }) { api.adminCredit(id, if (add) "add" else "deduct", amount, note) }
            }
        }
        ResellerDialog.TOGGLE -> ConfirmDialog(
            if (r?.active == true) "Disable ${r.displayName}?" else "Enable ${r?.displayName.orEmpty()}?",
            if (r?.active == true) "They are signed out everywhere (website and app) and can’t rent until you enable them again." else "They can sign in and rent again.",
            if (r?.active == true) "Disable" else "Enable",
            danger = r?.active == true,
            onDismiss = { dialog = ResellerDialog.NONE },
        ) { dialog = ResellerDialog.NONE; vm.act(toast, { x -> x.message }) { api.adminResellerAction(id, "toggle") } }
        ResellerDialog.APPROVE -> ConfirmDialog(
            "Approve ${r?.displayName.orEmpty()}?", "They get a notification (and WhatsApp, if set up) and can start renting.", "Approve",
            onDismiss = { dialog = ResellerDialog.NONE },
        ) { dialog = ResellerDialog.NONE; vm.act(toast, { x -> x.message }) { api.adminResellerAction(id, "verify") } }
        ResellerDialog.REJECT -> ConfirmDialog(
            "Reject this sign-up?", "The account is disabled. They see your reason.", "Reject", danger = true, reasonLabel = "Reason (optional)",
            onDismiss = { dialog = ResellerDialog.NONE },
        ) { reason -> dialog = ResellerDialog.NONE; vm.act(toast, { x -> x.message }) { api.adminResellerAction(id, "reject", reason) } }
    }
}

@Composable
private fun ResellerDetail(
    nav: NavHostController,
    settings: Settings,
    r: AdminReseller,
    refreshError: String?,
    busy: Boolean,
    onDialog: (ResellerDialog) -> Unit,
    onConfirmEmail: () -> Unit,
) {
    val context = LocalContext.current
    val b = AB.brand
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InlineError(refreshError)
        // profile + wallet card
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(b.gradient)
                .drawBehind { drawCircle(Color.White.copy(alpha = 0.10f), radius = size.width * 0.4f, center = Offset(size.width * 0.95f, 0f)) }
                .padding(20.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.25f)).padding(2.dp)) { PersonAvatar(r.displayName, 52.dp, online = r.online) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.displayName, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOf("@${r.username}", r.company).filter { it.isNotBlank() }.joinToString(" · "), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    val (label, _) = resellerState(r.state)
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Wallet", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                Text(money(r.balance, r.currency), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(
                    buildString {
                        append("Spent ${money(r.totalSpent, r.currency)}")
                        if (r.creditAllowed) append(" · credit ${if (r.creditLimit == null) "unlimited" else money(r.creditLimit, r.currency)}")
                        if (r.discountPercent > 0) append(" · ${r.discountPercent.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }}% off")
                    },
                    color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (r.state == "pending") {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.HowToReg, b.warning, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Waiting for approval", style = MaterialTheme.typography.titleSmall)
                        Text("Signed up ${ago(r.createdAt)}" + if (!r.emailVerified) " · email not confirmed yet" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton("Approve", { onDialog(ResellerDialog.APPROVE) }, Modifier.weight(1f), icon = Icons.Outlined.CheckCircle, loading = busy, height = 48.dp)
                    SecondaryButton("Reject", { onDialog(ResellerDialog.REJECT) }, Modifier.weight(1f), icon = Icons.Outlined.PersonOff, enabled = !busy)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (r.canMoney) {
                ActionTile("Add money", Icons.Outlined.AddCard, b.success, Modifier.weight(1f), enabled = !busy) { onDialog(ResellerDialog.ADD) }
                ActionTile("Deduct", Icons.Outlined.RemoveCircleOutline, b.warning, Modifier.weight(1f), enabled = !busy) { onDialog(ResellerDialog.DEDUCT) }
            }
            if (r.phone.isNotBlank()) ActionTile("WhatsApp", Icons.AutoMirrored.Outlined.Chat, b.success, Modifier.weight(1f)) { openWhatsApp(context, r.phone, "Hi ${r.displayName}, ") }
            if (r.state != "pending") ActionTile(if (r.active) "Disable" else "Enable", if (r.active) Icons.Outlined.Block else Icons.Outlined.CheckCircle, if (r.active) b.danger else b.success, Modifier.weight(1f), enabled = !busy) { onDialog(ResellerDialog.TOGGLE) }
        }
        if (!r.emailVerified && r.active) {
            SecondaryButton("Mark email as confirmed", onConfirmEmail, Modifier.fillMaxWidth(), icon = Icons.Outlined.MarkEmailRead, enabled = !busy)
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Account", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            InfoRow("Email", r.email + if (!r.emailVerified) " (not confirmed)" else "")
            if (r.phone.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { InfoRow("Phone", r.phone) }
                    IconButton(onClick = { dial(context, r.phone) }) { Icon(Icons.Outlined.Call, "Call", tint = MaterialTheme.colorScheme.secondary) }
                }
            }
            if (r.tier.isNotBlank()) InfoRow("Tier", r.tier.replaceFirstChar { it.uppercase() })
            InfoRow("Currency", r.currency)
            InfoRow("Rentals", "${r.rentalsTotal} total · ${r.activeRentals} running")
            if (r.ordersTotal > 0) InfoRow("Service orders", "${r.ordersTotal}")
            InfoRow("Two-step sign-in", if (r.twofa) "On" else "Off")
            InfoRow("Joined", dateTime(r.createdAt))
            InfoRow("Last sign-in", r.lastLoginAt?.let { ago(it) } ?: "Never")
        }

        if (r.recentRentals.isNotEmpty() && settings.can(Perm.RENTALS)) {
            GlassCard(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 12.dp)) {
                Text("Recent rentals", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                r.recentRentals.forEach { x ->
                    Row(
                        Modifier.fillMaxWidth().clickable { nav.navigate(AdminRoutes.rental(x.id)) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(x.tool, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("${x.plan} · ${shortDate(x.startedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(rentalLabel(if (x.running) "active" else if (x.status == "active") "expired" else x.status), rentalTone(x.status))
                    }
                }
            }
        }

        if (r.ledger.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 12.dp)) {
                Text("Wallet history", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                r.ledger.forEach { e ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(if (e.isCredit) Icons.Outlined.South else Icons.Outlined.North, if (e.isCredit) b.success else b.danger, 34.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.description.ifBlank { e.type }, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(shortDate(e.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text((if (e.isCredit) "+" else "−") + money(e.amount, e.currency), style = MaterialTheme.typography.labelLarge, color = if (e.isCredit) b.success else b.danger)
                            Text("Bal ${money(e.balanceAfter, e.currency)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
