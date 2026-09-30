package com.aamirbuneri.abgsmrental.ui.admin

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.SupportAgent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.AdminOrder
import com.aamirbuneri.abgsmrental.data.RemoteLogin
import com.aamirbuneri.abgsmrental.ui.Load
import com.aamirbuneri.abgsmrental.ui.components.CredentialRow
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.ErrorState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.InfoRow
import com.aamirbuneri.abgsmrental.ui.components.InlineError
import com.aamirbuneri.abgsmrental.ui.components.ItemAvatar
import com.aamirbuneri.abgsmrental.ui.components.LoadingCards
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.components.SecondaryButton
import com.aamirbuneri.abgsmrental.ui.components.StatusPill
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.dateTime
import com.aamirbuneri.abgsmrental.ui.util.money
import com.aamirbuneri.abgsmrental.ui.util.openWhatsApp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(nav: NavHostController, admin: AdminShellState) {
    val api = LocalContext.current.container.api
    val vm: PagedVM<AdminOrder> = viewModel(key = "a_orders") {
        PagedVM("open", { it.id }) { f, q, p -> api.adminOrders(f, q, p) }
    }
    LaunchedEffect(admin.orderFilter) {
        admin.orderFilter?.let { vm.pick(it); admin.orderFilter = null }
    }
    LaunchedEffect(Unit) { while (true) { delay(30_000); vm.load() } }
    LaunchedEffect(vm.items) {
        vm.items?.takeIf { vm.filter == "open" && vm.query.isBlank() }?.let { list -> admin.openOrders = list.count { it.status == "pending" } }
    }
    val c = vm.counts
    ScreenBackground {
        PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.load(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(Modifier.statusBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ScreenTitle("Service orders", if (vm.items != null) "${vm.total} ${if (vm.filter == "open") "open" else "shown"}" else null)
                        SearchBox(vm.query, "Order, service, device or reseller", { vm.search(it) }, Modifier.padding(horizontal = 16.dp))
                        FilterRow(
                            listOf(
                                FilterOption("open", "Open", c["open"]),
                                FilterOption("completed", "Completed", c["completed"]),
                                FilterOption("cancelled", "Cancelled", c["cancelled"]),
                                FilterOption("failed", "Failed", c["failed"]),
                                FilterOption("all", "All"),
                            ),
                            vm.filter, { vm.pick(it) },
                        )
                    }
                }
                val list = vm.items
                val err = vm.error
                when {
                    list == null && err != null -> item { ErrorState(err.message ?: "", err.offline) { vm.load() } }
                    list == null -> item { LoadingCards(4, 86.dp) }
                    list.isEmpty() -> item {
                        EmptyState(Icons.Outlined.SupportAgent, if (vm.filter == "open") "No open orders" else "No orders here", if (vm.query.isNotBlank()) "Nothing matches “${vm.query}”." else "New service orders show up here.")
                    }
                    else -> {
                        if (err != null) item { InlineError(err.message, Modifier.padding(horizontal = 16.dp)) }
                        items(list, key = { it.id }) { o -> QueueRow(o, Modifier.padding(horizontal = 16.dp)) { nav.navigate(AdminRoutes.order(o.id)) } }
                        if (vm.hasMore) item { LoadMore(vm.loadingMore) { vm.more() } }
                    }
                }
            }
        }
    }
}

private enum class OrderDialog { NONE, QUOTE, COMPLETE, CLOSE, START }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderScreen(nav: NavHostController, id: Int) {
    val api = LocalContext.current.container.api
    val vm: DetailVM<AdminOrder> = viewModel(key = "a_order-$id") { DetailVM { api.adminOrder(id) } }
    val toast = rememberToast()
    var dialog by remember { mutableStateOf(OrderDialog.NONE) }
    var remote by remember { mutableStateOf<RemoteLogin?>(null) }
    val state = vm.state
    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text((state as? Load.Ok)?.data?.number ?: "Order", maxLines = 1) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is Load.Loading -> LoadingCards(3, 140.dp)
                    is Load.Err -> ErrorState(state.error.message ?: "", state.error.offline) { vm.refresh() }
                    is Load.Ok -> OrderDetail(
                        nav, state.data, vm.refreshError, vm.busy, remote,
                        onShowRemote = { vm.side(toast, { api.adminOrderSecret(id) }) { remote = it } },
                        onDialog = { dialog = it },
                    )
                }
            }
        }
    }
    val o = (state as? Load.Ok)?.data
    when (dialog) {
        OrderDialog.NONE -> {}
        OrderDialog.START -> ConfirmDialog(
            "Start work?", "The reseller is told a technician is connecting now. The remote login stays available until you complete the order.", "Start",
            onDismiss = { dialog = OrderDialog.NONE },
        ) { dialog = OrderDialog.NONE; vm.act(toast, { it.message }) { api.adminOrderStart(id) } }
        OrderDialog.QUOTE -> AmountDialog(
            "Send a price", o?.currency ?: "PKR", "Send", note = "Message to the reseller (optional)",
            initial = o?.price?.takeIf { it > 0 }?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "",
            onDismiss = { dialog = OrderDialog.NONE },
        ) { amount, note -> dialog = OrderDialog.NONE; vm.act(toast, { it.message }) { api.adminOrderQuote(id, amount, note) } }
        OrderDialog.COMPLETE -> TextDialog(
            "Complete order", "Result for the reseller", "Complete",
            hint = "What was done — the reseller sees this. The remote login is erased.",
            onDismiss = { dialog = OrderDialog.NONE },
        ) { result -> dialog = OrderDialog.NONE; remote = null; vm.act(toast, { it.message }) { api.adminOrderComplete(id, result) } }
        OrderDialog.CLOSE -> CloseDialog(o, onDismiss = { dialog = OrderDialog.NONE }) { status, refund, reason ->
            dialog = OrderDialog.NONE; remote = null
            vm.act(toast, { it.message }) { api.adminOrderClose(id, status, refund, reason) }
        }
    }
}

@Composable
private fun CloseDialog(o: AdminOrder?, onDismiss: () -> Unit, onClose: (String, Double, String) -> Unit) {
    var cancelled by remember { mutableStateOf(true) }
    val paid = o?.paid == true && (o.price) > 0
    AmountDialog(
        title = "Cancel or fail order",
        currency = o?.currency ?: "PKR",
        confirm = "Close order",
        note = "Reason for the reseller",
        initial = if (paid) o!!.price.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } else "0",
        extra = {
            TwoWay("Cancelled", "Failed", cancelled, { cancelled = it })
            Text(if (paid) "Refund to the reseller’s wallet (0 = no refund):" else "Not paid yet — nothing to refund.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        onDismiss = onDismiss,
    ) { amount, note -> onClose(if (cancelled) "cancelled" else "failed", if (paid) amount else 0.0, note) }
}

@Composable
private fun OrderDetail(
    nav: NavHostController,
    o: AdminOrder,
    refreshError: String?,
    busy: Boolean,
    remote: RemoteLogin?,
    onShowRemote: () -> Unit,
    onDialog: (OrderDialog) -> Unit,
) {
    val context = LocalContext.current
    val b = AB.brand
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InlineError(refreshError)
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemAvatar(o.service, o.color, o.image, 56.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(o.service, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOf(o.device, o.number).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    val (label, tone) = orderBadge(o)
                    StatusPill(label, tone)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (o.price > 0) money(o.price, o.currency) else "—", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (o.paid) "Paid" else if (o.quote) "Price on request" else "Not paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // the next step, big
        when {
            o.canStart -> GradientButton("Start work", { onDialog(OrderDialog.START) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.PlayArrow, loading = busy)
            o.status == "in_progress" -> GradientButton("Complete order", { onDialog(OrderDialog.COMPLETE) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.CheckCircle, loading = busy)
            o.needsQuote -> GradientButton("Send a price", { onDialog(OrderDialog.QUOTE) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.RequestQuote, loading = busy)
        }
        if (o.open) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (o.canQuote && !o.needsQuote) ActionTile("Change price", Icons.Outlined.RequestQuote, b.warning, Modifier.weight(1f), enabled = !busy) { onDialog(OrderDialog.QUOTE) }
                if (o.canComplete && o.status != "in_progress") ActionTile("Complete", Icons.Outlined.CheckCircle, b.success, Modifier.weight(1f), enabled = !busy) { onDialog(OrderDialog.COMPLETE) }
                if (o.canClose) ActionTile("Cancel / fail", Icons.Outlined.Cancel, b.danger, Modifier.weight(1f), enabled = !busy) { onDialog(OrderDialog.CLOSE) }
                if (o.resellerPhone.isNotBlank()) ActionTile("WhatsApp", Icons.AutoMirrored.Outlined.Chat, b.success, Modifier.weight(1f)) {
                    openWhatsApp(context, o.resellerPhone, "Hi ${o.reseller}, about your order ${o.number} (${o.service}):")
                }
            }
        }
        if (o.status == "quoted") {
            Text("Waiting for the reseller to accept the price and pay from their wallet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (o.hasRemote) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Remote access", style = MaterialTheme.typography.titleMedium)
                Text(o.remoteApp.ifBlank { "Remote app" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                if (remote == null) {
                    SecondaryButton("Show remote login", onShowRemote, Modifier.fillMaxWidth(), icon = Icons.Outlined.Key, enabled = !busy)
                    Spacer(Modifier.height(6.dp))
                    Text("Viewing it is recorded in the activity log.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (remote.id.isNotBlank()) CredentialRow("${remote.app} ID", remote.id)
                        if (remote.password.isNotBlank()) CredentialRow("Password", remote.password, secret = true)
                    }
                }
            }
        }

        if (!o.result.isNullOrBlank()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Result", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(o.result, style = MaterialTheme.typography.bodyMedium)
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Details", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            InfoRow("Reseller", listOf(o.reseller, if (o.resellerUsername.isNotBlank()) "@${o.resellerUsername}" else "").filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" })
            if (o.device.isNotBlank()) InfoRow("Device", o.device)
            o.detailPairs.forEach { (k, v) -> InfoRow(k, v) }
            if (o.etaMinutes > 0) InfoRow("Usual time", "${o.etaMinutes} min")
            InfoRow("Placed", dateTime(o.createdAt))
            o.quotedAt?.let { InfoRow("Price sent", dateTime(it)) }
            o.startedAt?.let { InfoRow("Started", dateTime(it)) }
            o.completedAt?.let { InfoRow("Finished", dateTime(it)) }
        }
        if (o.note.isNotBlank()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Reseller’s note", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(o.note, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (o.resellerId != null) {
            SecondaryButton("Open reseller", { nav.navigate(AdminRoutes.reseller(o.resellerId)) }, Modifier.fillMaxWidth())
        }
    }
}
