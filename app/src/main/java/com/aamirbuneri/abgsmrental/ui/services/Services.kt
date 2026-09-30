package com.aamirbuneri.abgsmrental.ui.services

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.automirrored.outlined.ScreenShare
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.Api
import com.aamirbuneri.abgsmrental.data.ApiException
import com.aamirbuneri.abgsmrental.data.Order
import com.aamirbuneri.abgsmrental.data.Service
import com.aamirbuneri.abgsmrental.ui.DataVM
import com.aamirbuneri.abgsmrental.ui.Load
import com.aamirbuneri.abgsmrental.ui.Routes
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
import com.aamirbuneri.abgsmrental.ui.components.orderTone
import com.aamirbuneri.abgsmrental.ui.home.OrderRow
import com.aamirbuneri.abgsmrental.ui.rentals.Segmented
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.copy
import com.aamirbuneri.abgsmrental.ui.util.dateTime
import com.aamirbuneri.abgsmrental.ui.util.duration
import com.aamirbuneri.abgsmrental.ui.util.money
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// ── Services tab: catalogue + my orders ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(nav: NavHostController) {
    val api = LocalContext.current.container.api
    val services: DataVM<List<Service>> = viewModel(key = "services") { DataVM { api.services() } }
    val orders: DataVM<List<Order>> = viewModel(key = "orders") { DataVM { api.orders(100) } }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(tab) { if (tab == 1) orders.refresh() }

    ScreenBackground {
        PullToRefreshBox(
            isRefreshing = if (tab == 0) services.refreshing else orders.refreshing,
            onRefresh = { if (tab == 0) services.refresh(pull = true) else orders.refresh(pull = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                        Text("Remote services", style = MaterialTheme.typography.headlineMedium)
                        Text("Order a service — we connect to the phone and do the work.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(14.dp))
                        Segmented(listOf("Order a service", "My orders"), tab) { tab = it }
                    }
                }
                if (tab == 0) {
                    when (val s = services.state) {
                        is Load.Loading -> item { LoadingCards(5, 86.dp) }
                        is Load.Err -> item { ErrorState(s.error.message ?: "", s.error.offline) { services.refresh() } }
                        is Load.Ok -> {
                            val cats = s.data.map { it.category }.filter { it.isNotBlank() }.distinct()
                            if (cats.size > 1) item {
                                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item { Chip("All", category == null) { category = null } }
                                    items(cats) { c -> Chip(c, category == c) { category = c } }
                                }
                            }
                            val shown = s.data.filter { category == null || it.category == category }
                            if (shown.isEmpty()) item { EmptyState(Icons.Outlined.SupportAgent, "No services", "No remote services are offered right now.") }
                            items(shown, key = { it.id }) { svc -> ServiceCard(svc, Modifier.padding(horizontal = 16.dp)) { nav.navigate(Routes.orderNew(svc.id)) } }
                        }
                    }
                } else {
                    when (val o = orders.state) {
                        is Load.Loading -> item { LoadingCards(5, 80.dp) }
                        is Load.Err -> item { ErrorState(o.error.message ?: "", o.error.offline) { orders.refresh() } }
                        is Load.Ok -> {
                            if (o.data.isEmpty()) item { EmptyState(Icons.Outlined.Inbox, "No orders yet", "Your service orders and their results show up here.", action = "Order a service") { tab = 0 } }
                            items(o.data, key = { it.id }) { ord -> OrderRow(ord, Modifier.padding(horizontal = 16.dp)) { nav.navigate(Routes.order(ord.id)) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun ServiceCard(s: Service, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemAvatar(s.name, s.color, s.image, 50.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (s.category.isNotBlank()) {
                        Text(s.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (s.etaMinutes > 0) {
                        Icon(Icons.Outlined.Schedule, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(3.dp))
                        Text("~" + duration(s.etaMinutes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (s.quote || s.price == null) "Price on request" else money(s.price, s.currency),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (s.quote) AB.brand.info else MaterialTheme.colorScheme.primary,
                )
            }
            if (s.needsRemote) {
                Icon(Icons.AutoMirrored.Outlined.ScreenShare, "Needs remote access", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── New order ───────────────────────────────────────────────────────────────

class OrderAction(private val api: Api) : ViewModel() {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    fun place(service: Service, device: String, fields: Map<String, String>, remoteApp: String, remoteId: String, remotePass: String, note: String, onDone: (Order) -> Unit) {
        if (busy) return
        val missing = service.fields.firstOrNull { it.required && fields[it.label].isNullOrBlank() }
        error = when {
            device.isBlank() -> "Enter the phone model."
            missing != null -> "Fill in “${missing.label}”."
            service.needsRemote && remoteId.isBlank() -> "Enter the ${if (remoteApp == "ultraviewer") "UltraViewer" else "AnyDesk"} ID."
            else -> null
        }
        if (error != null) return
        viewModelScope.launch {
            busy = true
            try {
                val o = api.placeOrder(buildJsonObject {
                    put("service_id", service.id)
                    put("device", device.trim())
                    put("note", note.trim())
                    if (service.needsRemote) {
                        put("remote_app", remoteApp)
                        put("remote_id", remoteId.trim())
                        put("remote_pass", remotePass)
                    }
                    putJsonObject("fields") { fields.forEach { (k, v) -> put(k, v.trim()) } }
                })
                onDone(o)
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(nav: NavHostController, serviceId: Int) {
    val api = LocalContext.current.container.api
    val services: DataVM<List<Service>> = viewModel(key = "svc-form") { DataVM { api.services() } }
    val action: OrderAction = viewModel(key = "order-action") { OrderAction(api) }
    val service = (services.state as? Load.Ok)?.data?.firstOrNull { it.id == serviceId }

    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(service?.name ?: "New order") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            when (val s = services.state) {
                is Load.Loading -> LoadingCards(4, 70.dp)
                is Load.Err -> ErrorState(s.error.message ?: "", s.error.offline) { services.refresh() }
                is Load.Ok -> if (service == null) {
                    EmptyState(Icons.Outlined.SupportAgent, "Service not available", "This service was removed or switched off.")
                } else OrderForm(service, action) { o ->
                    nav.popBackStack()
                    nav.navigate(Routes.order(o.id))
                }
            }
        }
    }
}

@Composable
private fun OrderForm(service: Service, action: OrderAction, onPlaced: (Order) -> Unit) {
    var device by rememberSaveable { mutableStateOf("") }
    var remoteApp by rememberSaveable { mutableStateOf("anydesk") }
    var remoteId by rememberSaveable { mutableStateOf("") }
    var remotePass by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val fields = remember { mutableStateMapOf<String, String>() }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemAvatar(service.name, service.color, service.image, 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(service.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(service.category.takeIf { it.isNotBlank() }, if (service.etaMinutes > 0) "about ${duration(service.etaMinutes)}" else null).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (service.quote || service.price == null) "Quote" else money(service.price, service.currency),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                )
            }
            if (service.description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(service.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Phone", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            FormField(device, { device = it }, "Phone model *", placeholder = "e.g. Redmi Note 12")
            service.fields.forEach { f ->
                Spacer(Modifier.height(10.dp))
                FormField(fields[f.label].orEmpty(), { fields[f.label] = it }, f.label + if (f.required) " *" else "")
            }
        }

        if (service.needsRemote) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Remote access", style = MaterialTheme.typography.titleSmall)
                Text("Connect the phone to a PC and open the remote app there.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Segmented(listOf("AnyDesk", "UltraViewer"), if (remoteApp == "ultraviewer") 1 else 0) { remoteApp = if (it == 1) "ultraviewer" else "anydesk" }
                Spacer(Modifier.height(12.dp))
                FormField(remoteId, { remoteId = it }, "${if (remoteApp == "ultraviewer") "UltraViewer" else "AnyDesk"} ID *", keyboard = KeyboardType.Number)
                Spacer(Modifier.height(10.dp))
                FormField(remotePass, { remotePass = it }, "Password (if set)")
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            FormField(note, { note = it }, "Note for the technician (optional)", singleLine = false)
        }

        InlineError(action.error)
        GradientButton(
            text = if (service.quote || service.price == null) "Send order (price on request)" else "Order · ${money(service.price, service.currency)}",
            onClick = { action.place(service, device, fields.toMap(), remoteApp, remoteId, remotePass, note, onPlaced) },
            modifier = Modifier.fillMaxWidth(),
            loading = action.busy,
        )
        Text(
            if (service.quote) "The admin sends you a price first — you accept it before anything is paid."
            else "Paid from your wallet. If it can’t be done, you get the money back.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FormField(value: String, onChange: (String) -> Unit, label: String, placeholder: String? = null, keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ── Order detail ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(nav: NavHostController, id: Int) {
    val api = LocalContext.current.container.api
    val vm: DataVM<Order> = viewModel(key = "order-$id") { DataVM { api.order(id) } }
    val actions: OrderActionsVM = viewModel(key = "order-actions-$id") { OrderActionsVM(api) }
    val state = vm.state
    LaunchedEffect(state) {
        // while it's being worked on, check every 20 s
        val o = (state as? Load.Ok)?.data
        if (o != null && o.open) { delay(20_000); vm.refresh() }
    }
    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text((state as? Load.Ok)?.data?.number?.ifBlank { null } ?: "Order") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is Load.Loading -> LoadingCards(3, 120.dp)
                    is Load.Err -> ErrorState(state.error.message ?: "", state.error.offline) { vm.refresh() }
                    is Load.Ok -> OrderDetail(state.data, vm.refreshError, actions, onUpdated = { vm.set(it) }, onFailed = { vm.refresh() })
                }
            }
        }
    }
}

class OrderActionsVM(private val api: Api) : ViewModel() {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    fun accept(o: Order, onOk: (Order) -> Unit, onFail: () -> Unit) = act({ api.acceptQuote(o.id, o.price) }, onOk, onFail)
    fun cancel(o: Order, reason: String, onOk: (Order) -> Unit, onFail: () -> Unit) =
        act({ api.cancelOrder(o.id, reason.ifBlank { "Cancelled in the app" }) }, onOk, onFail)

    private fun act(block: suspend () -> Order, onOk: (Order) -> Unit, onFail: () -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true; error = null
            try {
                onOk(block())
            } catch (e: ApiException) {
                error = e.message
                onFail()
            } finally {
                busy = false
            }
        }
    }
}

@Composable
private fun OrderDetail(o: Order, refreshError: String?, actions: OrderActionsVM, onUpdated: (Order) -> Unit, onFailed: () -> Unit) {
    val context = LocalContext.current
    var confirmAccept by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InlineError(refreshError ?: actions.error)
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(o.service, style = MaterialTheme.typography.titleLarge)
                    Text(o.device, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(o.statusLabel.ifBlank { o.status }, orderTone(o.status))
            }
            Spacer(Modifier.height(16.dp))
            Timeline(o)
        }

        if (o.awaitingAcceptance) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Price offered", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (o.price != null) money(o.price, o.currency) else "—", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text("Accept to pay from your wallet and start the work.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                GradientButton("Accept & pay", { confirmAccept = true }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Check, loading = actions.busy)
            }
        }

        val result = o.result
        if (!result.isNullOrBlank()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Result", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { copy(context, "Result", result) }) { Icon(Icons.Outlined.ContentCopy, "Copy result", tint = MaterialTheme.colorScheme.secondary) }
                }
                SelectionContainer {
                    Text(result, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Details", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            InfoRow("Order", o.number)
            InfoRow("Price", when {
                o.price != null -> money(o.price, o.currency) + if (o.paid) " · paid" else ""
                else -> "Waiting for a quote"
            })
            InfoRow("Placed", dateTime(o.createdAt))
            if (o.startedAt != null) InfoRow("Started", dateTime(o.startedAt))
            if (o.completedAt != null) InfoRow("Finished", dateTime(o.completedAt))
            if (o.remoteApp.isNotBlank()) InfoRow("Remote app", if (o.remoteApp == "ultraviewer") "UltraViewer" else "AnyDesk")
            o.detailPairs.forEach { (k, v) -> InfoRow(k, v) }
            if (o.note.isNotBlank()) InfoRow("Note", o.note)
        }

        if (o.canCancel) {
            SecondaryButton("Cancel order", { confirmCancel = true }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Cancel, enabled = !actions.busy)
        }
    }

    if (confirmAccept) {
        AlertDialog(
            onDismissRequest = { confirmAccept = false },
            title = { Text("Accept the price?") },
            text = { Text("${if (o.price != null) money(o.price, o.currency) else ""} will be paid from your wallet and the work starts.") },
            confirmButton = { TextButton(onClick = { confirmAccept = false; actions.accept(o, onUpdated, onFailed) }) { Text("Accept & pay") } },
            dismissButton = { TextButton(onClick = { confirmAccept = false }) { Text("Not now") } },
        )
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel this order?") },
            text = {
                Column {
                    Text(if (o.paid) "The amount paid goes back to your wallet." else "Nothing has been paid for it yet.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(reason, { reason = it }, label = { Text("Reason (optional)") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = { TextButton(onClick = { confirmCancel = false; actions.cancel(o, reason, onUpdated, onFailed) }) { Text("Cancel order", color = AB.brand.danger) } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep it") } },
        )
    }
}

@Composable
private fun Timeline(o: Order) {
    data class Step(val label: String, val done: Boolean, val current: Boolean)
    val failed = o.status == "failed" || o.status == "cancelled"
    val steps = buildList {
        add(Step("Placed", true, o.status == "pending" && !o.quote))
        if (o.quote) add(Step(if (o.awaitingAcceptance) "Price offered" else "Price agreed", o.quotedAt != null || o.paid, o.status == "quoted"))
        add(Step("In progress", o.status in setOf("in_progress", "completed") || o.startedAt != null, o.status == "in_progress"))
        add(Step(if (failed) (if (o.status == "failed") "Failed" else "Cancelled") else "Done", o.status in setOf("completed", "failed", "cancelled"), false))
    }
    val b = AB.brand
    Column {
        steps.forEachIndexed { i, s ->
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val last = i == steps.lastIndex
                    val color = when {
                        last && failed -> b.danger
                        s.done -> b.success
                        s.current -> b.info
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Box(
                        Modifier.size(22.dp).clip(CircleShape)
                            .background(if (s.done) color else Color.Transparent)
                            .border(2.dp, color, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (s.done) Icon(if (last && failed) Icons.Outlined.Cancel else Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    if (!last) Box(Modifier.width(2.dp).height(22.dp).background(if (steps[i + 1].done) b.success else MaterialTheme.colorScheme.outlineVariant))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    s.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (s.current) FontWeight.Bold else FontWeight.Normal,
                    color = if (s.done || s.current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}
