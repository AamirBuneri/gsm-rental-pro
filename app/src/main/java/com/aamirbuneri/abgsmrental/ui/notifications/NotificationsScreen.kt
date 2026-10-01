package com.aamirbuneri.abgsmrental.ui.notifications

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.Api
import com.aamirbuneri.abgsmrental.data.ApiException
import com.aamirbuneri.abgsmrental.data.Notice
import com.aamirbuneri.abgsmrental.ui.Routes
import com.aamirbuneri.abgsmrental.ui.Shell
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.ErrorState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.IconBadge
import com.aamirbuneri.abgsmrental.ui.components.InlineError
import com.aamirbuneri.abgsmrental.ui.components.LoadingCards
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.ago
import kotlinx.coroutines.launch

class NoticesVM(private val api: Api, private val admin: Boolean = false) : ViewModel() {
    var items by mutableStateOf<List<Notice>?>(null)
        private set
    var unread by mutableStateOf(0)
        private set
    var error by mutableStateOf<ApiException?>(null)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var end by mutableStateOf(false)
        private set

    init { refresh() }

    fun refresh(pull: Boolean = false) {
        viewModelScope.launch {
            if (pull) refreshing = true
            try {
                val p = if (admin) api.adminNotifications(limit = 30) else api.notifications(limit = 30)
                items = p.items; unread = p.unread; end = p.items.size < 30; error = null
            } catch (e: ApiException) {
                error = e
            } finally {
                refreshing = false
            }
        }
    }

    fun more() {
        val last = items?.lastOrNull()?.id ?: return
        if (loadingMore || end) return
        viewModelScope.launch {
            loadingMore = true
            try {
                val p = if (admin) api.adminNotifications(beforeId = last, limit = 30) else api.notifications(beforeId = last, limit = 30)
                items = items.orEmpty() + p.items; end = p.items.size < 30
            } catch (e: ApiException) {
                error = e
            } finally {
                loadingMore = false
            }
        }
    }

    fun read(n: Notice) {
        if (n.read) return
        items = items?.map { if (it.id == n.id) it.copy(read = true) else it }
        unread = (unread - 1).coerceAtLeast(0)
        viewModelScope.launch { runCatching { unread = (if (admin) api.adminMarkRead(n.id) else api.markRead(n.id)).unread } }
    }

    fun readAll() {
        items = items?.map { it.copy(read = true) }
        unread = 0
        viewModelScope.launch { runCatching { if (admin) api.adminMarkRead(null) else api.markRead(null) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(nav: NavHostController, shell: Shell, admin: Boolean = false) {
    val context = LocalContext.current
    val api = context.container.api
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val vm: NoticesVM = viewModel(key = if (admin) "a_notices" else "notices") { NoticesVM(api, admin) }
    LaunchedEffect(vm.unread) { shell.unread = vm.unread }
    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = {
                    if (vm.unread > 0) TextButton(onClick = { vm.readAll() }) {
                        Icon(Icons.Outlined.DoneAll, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark all read")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
                val list = vm.items
                val err = vm.error
                when {
                    list == null && err != null -> ErrorState(err.message ?: "", err.offline) { vm.refresh() }
                    list == null -> LoadingCards(6, 72.dp)
                    list.isEmpty() -> EmptyState(Icons.Outlined.NotificationsNone, "All caught up", if (admin) "New sign-ups, orders and alerts will show up here." else "Rental, order and wallet updates will show up here.")
                    else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (err != null) item { InlineError(err.message) }
                        items(list, key = { it.id }) { n ->
                            fun open(link: String) {
                                scope.launch {
                                    val site = context.container.prefs.site()
                                    runCatching { com.aamirbuneri.abgsmrental.ui.openSiteLink(context, nav, link, admin, site) }
                                }
                            }
                            val tap = {
                                vm.read(n)
                                when {
                                    !admin && n.rentalId != null -> nav.navigate(Routes.rental(n.rentalId))
                                    admin && n.rentalId != null -> nav.navigate(com.aamirbuneri.abgsmrental.ui.admin.AdminRoutes.rental(n.rentalId))
                                    n.link.isNotBlank() -> open(n.link)
                                    !n.actionUrl.isNullOrBlank() -> open(n.actionUrl)
                                    else -> {}
                                }
                            }
                            if (n.broadcast || !n.image.isNullOrBlank() || !n.actionLabel.isNullOrBlank()) {
                                com.aamirbuneri.abgsmrental.ui.components.RichNotice(
                                    n.title, n.message, n.type, image = n.image?.ifBlank { null }, actionLabel = n.actionLabel?.ifBlank { null },
                                    time = ago(n.createdAt), unread = !n.read,
                                    onAction = { vm.read(n); n.actionUrl?.takeIf { it.isNotBlank() }?.let { open(it) } },
                                    onClick = { tap() },
                                )
                            } else NoticeRow(n) { tap() }
                        }
                        if (!vm.end) item {
                            TextButton(onClick = { vm.more() }, enabled = !vm.loadingMore, modifier = Modifier.fillMaxWidth()) {
                                Text(if (vm.loadingMore) "Loading…" else "Load older")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeRow(n: Notice, onClick: () -> Unit) {
    val b = AB.brand
    val (icon, tint) = when (n.type) {
        "success" -> Icons.Outlined.CheckCircle to b.success
        "warning" -> Icons.Outlined.WarningAmber to b.warning
        "danger", "error" -> Icons.Outlined.ErrorOutline to b.danger
        else -> Icons.Outlined.Info to b.info
    }
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(icon, tint, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.title, style = MaterialTheme.typography.titleSmall, fontWeight = if (n.read) FontWeight.Medium else FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (!n.read) Box(Modifier.size(9.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                }
                if (n.message.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(n.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text(ago(n.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
        }
    }
}
