package com.aamirbuneri.abgsmrental.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.ApiException
import com.aamirbuneri.abgsmrental.data.FilePart
import com.aamirbuneri.abgsmrental.data.bool
import com.aamirbuneri.abgsmrental.data.form
import com.aamirbuneri.abgsmrental.data.int
import com.aamirbuneri.abgsmrental.data.items
import com.aamirbuneri.abgsmrental.data.list
import com.aamirbuneri.abgsmrental.data.obj
import com.aamirbuneri.abgsmrental.data.str
import com.aamirbuneri.abgsmrental.ui.admin.PersonAvatar
import com.aamirbuneri.abgsmrental.ui.admin.SearchBox
import com.aamirbuneri.abgsmrental.ui.admin.rememberToast
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.RichNotice
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.components.Tone
import com.aamirbuneri.abgsmrental.ui.components.noticeStyle
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.ago
import com.aamirbuneri.abgsmrental.ui.util.shortDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

// ── chat with resellers ────────────────────────────────────────────────────

@Composable
fun ChatListPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_chats") { PageVM(api, "/api/chat/threads") }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { while (true) { delay(8000); vm.load() } }
    Panel("Chat", ctx.nav, vm) { d ->
        val all = d.items()
        item { SearchBox(q, "Find a reseller", { q = it }) }
        val rows = all.filter { q.isBlank() || it.str("name").contains(q, true) || it.str("username").contains(q, true) }
        if (rows.isEmpty()) item { EmptyState(Icons.AutoMirrored.Outlined.Chat, "No chats", "Active resellers show up here.") }
        items(rows, key = { it.int("id") }) { t ->
            val unread = t.int("unread")
            Line(
                t.str("name"),
                t.str("last").ifBlank { "@" + t.str("username") },
                leading = { PersonAvatar(t.str("name"), 42.dp, online = t.bool("online")) },
                value = t.str("last_time").ifBlank { null },
                pill = if (unread > 0) "$unread new" to Tone.Info else null,
                onClick = { ctx.go("chat", t.int("id"), t.str("name")) },
            )
        }
    }
}

private class ChatMsg(val id: Int, val mine: Boolean, val body: String, val time: String, val day: String, val image: String?)

private fun JsonElement.toMsg() = ChatMsg(int("id"), bool("mine"), str("body"), str("time"), str("day"), str("image").ifBlank { null })

/**
 * One conversation. [withId] = the reseller (team side) or 0 (reseller ↔ support team).
 * [orderId] > 0 → the chat of a remote-service order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPanel(ctx: PanelCtx, withId: Int, name: String, orderId: Int = 0) {
    val api = LocalContext.current.container.api
    val toast = rememberToast()
    val scope = rememberCoroutineScope()
    val msgs = remember { mutableStateListOf<ChatMsg>() }
    var peer by remember { mutableStateOf<JsonElement?>(null) }
    var seenUpto by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val path = if (orderId > 0) "/api/services/orders/$orderId/messages" else "/api/chat/messages"

    suspend fun pull() {
        val q = buildMap {
            put("after", "${msgs.lastOrNull()?.id ?: 0}")
            if (withId > 0 && orderId == 0) put("with", "$withId")
        }
        try {
            val j = api.page(path, q).json
            val fresh = j.list("messages").map { it.toMsg() }.filter { m -> msgs.none { it.id == m.id } }
            if (fresh.isNotEmpty()) { msgs.addAll(fresh); list.animateScrollToItem(maxOf(0, msgs.size - 1)) }
            j.obj("peer")?.let { peer = it }
            seenUpto = j.int("seen_upto", seenUpto)
            error = null
        } catch (e: ApiException) {
            if (!loaded) error = e.message
        }
        loaded = true
    }

    LaunchedEffect(withId, orderId) { while (true) { pull(); delay(4000) } }

    fun send() {
        val body = text.trim()
        if (body.isEmpty() || sending) return
        sending = true
        scope.launch {
            try {
                val data = if (orderId > 0) form("body" to body) else form("body" to body, "to" to if (withId > 0) "$withId" else null)
                api.submit(if (orderId > 0) path else "/api/chat/send", data)
                text = ""
                pull()
            } catch (e: ApiException) {
                toast(e.message ?: "Couldn’t send.")
            } finally {
                sending = false
            }
        }
    }

    ScreenBackground {
        Column(Modifier.fillMaxSize().imePadding()) {
            TopAppBar(
                title = {
                    Column {
                        Text(name.ifBlank { if (orderId > 0) "Order chat" else "Support team" }, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                        val p = peer
                        if (p != null) Text(if (p.bool("online")) "Online" else "Last seen ${p.str("last_seen")}", style = MaterialTheme.typography.labelSmall,
                            color = if (p.bool("online")) AB.brand.success else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = { ctx.nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    error != null && msgs.isEmpty() -> EmptyState(Icons.AutoMirrored.Outlined.Chat, "Can’t open the chat", error ?: "")
                    loaded && msgs.isEmpty() -> EmptyState(Icons.AutoMirrored.Outlined.Chat, "No messages yet", "Say hello 👋")
                    else -> LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(msgs.size, key = { msgs[it].id }) { i ->
                            val m = msgs[i]
                            if (i == 0 || msgs[i - 1].day != m.day) {
                                Text(m.day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            Bubble(m, seen = m.mine && orderId == 0 && m.id <= seenUpto)
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).navigationBarsPadding().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, placeholder = { Text("Message") },
                    maxLines = 5, shape = RoundedCornerShape(22.dp), modifier = Modifier.weight(1f).testTag("chat_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(AB.brand.gradient).clickable(enabled = text.isNotBlank() && !sending) { send() }.testTag("chat_send"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Outlined.Send, "Send", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMsg, seen: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start) {
        val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (m.mine) 18.dp else 4.dp, bottomEnd = if (m.mine) 4.dp else 18.dp)
        Column(
            Modifier.widthIn(max = 300.dp).clip(shape)
                .background(if (m.mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            val fg = if (m.mine) Color.White else MaterialTheme.colorScheme.onSurface
            if (m.image != null) {
                AsyncImage(m.image, null, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.height(6.dp))
            }
            if (m.body.isNotBlank()) Text(m.body, color = fg, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text(m.time, color = fg.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                if (m.mine) {
                    Spacer(Modifier.width(4.dp))
                    Icon(if (seen) Icons.Outlined.DoneAll else Icons.Outlined.Check, null, Modifier.size(14.dp), tint = fg.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// ── announcements (banner at the top of every reseller page) ───────────────

@Composable
fun AnnouncementsPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_ann") { PageVM(api, "/admin/announcements") }
    val toast = rememberToast()
    var open by remember { mutableStateOf(false) }
    val f = remember { FormState(mapOf("type" to "info")) }
    Panel("Announcements", ctx.nav, vm, actions = {
        IconButton(onClick = { f.values.clear(); f["type"] = "info"; open = true }) { Icon(Icons.Outlined.Add, "New announcement") }
    }) { d ->
        item {
            Text("A banner at the top of every reseller page and the app’s home screen until it expires or you turn it off.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val rows = d.list("rows")
        if (rows.isEmpty()) item { EmptyState(Icons.Outlined.Campaign, "No announcements", "Tap + to post one.") }
        items(rows, key = { it.int("id") }) { a ->
            val on = a.bool("is_active")
            val expired = a.str("expires_at").isNotBlank() && (com.aamirbuneri.abgsmrental.ui.util.parseIso(a.str("expires_at"))?.time ?: Long.MAX_VALUE) < System.currentTimeMillis()
            Line(
                a.str("title"),
                a.str("body").take(140) + "\n" + listOfNotNull("by " + a.str("username").ifBlank { "—" }, ago(a.str("created_at")),
                    a.str("expires_at").ifBlank { null }?.let { "until " + shortDate(it) }).joinToString(" · "),
                leading = { val st = noticeStyle(a.str("type")); Box(Modifier.size(36.dp).clip(CircleShape).background(st.color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Icon(st.icon, null, tint = st.color) } },
                pill = when { expired -> "Expired" to Tone.Neutral; on -> "Showing" to Tone.Success; else -> "Off" to Tone.Neutral },
                menu = listOf<Pair<String, () -> Unit>>(
                    (if (on) "Turn off" else "Turn on") to { vm.submit("/admin/announcements/${a.int("id")}/toggle", form(), toast) },
                    "Delete" to { vm.submit("/admin/announcements/${a.int("id")}/delete", form(), toast) },
                ),
            )
        }
    }
    Sheet(open, "New announcement", { open = false }, action = "Publish", busy = vm.busy, onAction = {
        vm.submit("/admin/announcements", f.json(extra = if (f["expires_at"].length == 10) mapOf("expires_at" to f["expires_at"] + " 23:59") else emptyMap()), toast) { if (it.ok) open = false }
    }) {
        ChoiceIn(f, "type", "Style", listOf("info" to "Info", "success" to "Good news", "warning" to "Warning", "danger" to "Urgent"))
        TextIn(f, "title", "Title")
        TextIn(f, "body", "Text", lines = 4)
        DateIn(f, "expires_at", "Show until (optional)")
    }
}

// ── send a message (notification with picture, formatting and a button) ────

private val AUDIENCES = listOf("resellers" to "All resellers", "selected" to "Chosen", "team" to "Staff & owner", "everyone" to "Everyone")
private val STYLES = listOf("info" to "Info", "success" to "Good news", "promo" to "Offer", "warning" to "Warning", "danger" to "Urgent")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposerPanel(ctx: PanelCtx) {
    val context = LocalContext.current
    val api = context.container.api
    val vm: PageVM = viewModel(key = "p_compose") { PageVM(api, "/admin/messages") }
    val toast = rememberToast()
    val f = remember { FormState(mapOf("audience" to "resellers", "type" to "info", "whatsapp" to "0")) }
    var body by remember { mutableStateOf(TextFieldValue("")) }
    var picture by remember { mutableStateOf<FilePart?>(null) }
    val chosen = remember { mutableStateListOf<Int>() }
    var picking by remember { mutableStateOf(false) }
    var who by remember { mutableStateOf("") }
    var recalling by remember { mutableStateOf<JsonElement?>(null) }

    fun wrap(mark: String) {
        val s = body.selection
        val t = body.text
        val inner = t.substring(s.min, s.max).ifEmpty { "text" }
        val out = t.substring(0, s.min) + mark + inner + mark + t.substring(s.max)
        body = TextFieldValue(out, TextRange(s.min + mark.length, s.min + mark.length + inner.length))
    }

    fun send() {
        val fields = buildList {
            listOf("audience", "type", "title", "action_label", "action_url").forEach { add(it to f[it]) }
            add("message" to body.text)
            if (f.on("whatsapp")) add("whatsapp" to "1")
            if (f["audience"] == "selected") chosen.forEach { add("ids[]" to "$it") }
        }
        vm.submitFiles("/admin/messages", fields, listOfNotNull(picture), toast) { r ->
            if (r.ok) {
                f["title"] = ""; f["action_label"] = ""; f["action_url"] = ""; body = TextFieldValue(""); picture = null; chosen.clear()
            }
        }
    }

    Panel("Send a message", ctx.nav, vm, bottom = {
        GradientButton(if (vm.busy) "Sending…" else "Send message", { send() }, Modifier.fillMaxWidth().testTag("compose_send"), enabled = !vm.busy && f["title"].isNotBlank() && body.text.isNotBlank())
    }) { d ->
        val resellers = d.list("resellers")
        item {
            Text("Shows in the bell and the app — even when the app is closed — with your picture and button. WhatsApp copy is optional.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Group("To") {
                ChoiceIn(f, "audience", null, AUDIENCES)
                if (f["audience"] == "selected") {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (chosen.isEmpty()) "Nobody chosen yet" else "${chosen.size} chosen", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        androidx.compose.material3.TextButton(onClick = { picking = true }) { Text("Choose resellers") }
                    }
                }
                if (f["audience"] == "team" || f["audience"] == "everyone") {
                    Text("Staff see it in their admin bell (${d.int("teamCount")} people).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Group("Message") {
                ChoiceIn(f, "type", "Style", STYLES)
                TextIn(f, "title", "Title", hint = "e.g. 🎉 UnlockTool is back — 20% off today")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { wrap("**") }) { Icon(Icons.Outlined.FormatBold, "Bold") }
                    IconButton(onClick = { wrap("_") }) { Icon(Icons.Outlined.FormatItalic, "Italic") }
                    Text("**bold** · _italic_ · links work · emoji 👍", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = body, onValueChange = { body = it }, label = { Text("Message") }, minLines = 5,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().testTag("compose_body"),
                )
                ImageIn("Picture (optional)", picture, null) { picture = it }
            }
        }
        item {
            Group("Button (optional)") {
                TextIn(f, "action_label", "Button text", hint = "Rent now")
                TextIn(f, "action_url", "Button link", hint = "/reseller/tools, https://…, tel:…")
                val tools = d.list("tools").take(8)
                val services = d.list("services").take(6)
                val wa = d.str("whatsapp_number")
                val chips = buildList {
                    add(Triple("/reseller/tools", "Rent a tool", "Rent a tool"))
                    add(Triple("/reseller/services", "Remote services", "Remote services"))
                    add(Triple("/reseller/wallet", "Wallet", "Top up"))
                    add(Triple("/reseller/rentals", "My rentals", "My rentals"))
                    tools.forEach { add(Triple("/reseller/tools/${it.int("id")}", it.str("name"), "Rent ${it.str("name")}")) }
                    services.forEach { add(Triple("/reseller/services/${it.int("id")}", it.str("name"), "Order now")) }
                    if (wa.isNotBlank()) add(Triple("https://wa.me/$wa", "WhatsApp us", "Chat on WhatsApp"))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    chips.forEach { (url, label, button) ->
                        FilterChip(selected = f["action_url"] == url, onClick = { f["action_url"] = url; if (f["action_label"].isBlank()) f["action_label"] = button }, label = { Text(label, maxLines = 1) })
                    }
                }
            }
        }
        item { SwitchIn(f, "whatsapp", "Also send on WhatsApp", "Uses your WhatsApp gateway (text and link only)") }
        item {
            Section("Preview")
            RichNotice(f["title"], body.text, f["type"], image = picture?.bytes?.let { java.nio.ByteBuffer.wrap(it) }, actionLabel = f["action_label"].ifBlank { if (f["action_url"].isNotBlank()) "Open" else null }, time = "now", unread = true)
        }
        val recent = d.list("recent")
        if (recent.isNotEmpty()) item { Section("Sent") }
        items(recent, key = { "b" + it.int("id") }) { b ->
            val recalled = b.str("recalled_at").isNotBlank()
            Line(
                b.str("title"),
                listOfNotNull(
                    AUDIENCES.firstOrNull { it.first == b.str("audience") }?.second,
                    "${b.int("recipients")} people · ${b.int("read_count")} read",
                    if (b.int("whatsapp") > 0) "WhatsApp ${b.int("whatsapp")}" else null,
                    if (b.str("via") == "app") "from the app" else null,
                    ago(b.str("created_at")),
                ).joinToString(" · "),
                leading = { val st = noticeStyle(b.str("type")); Box(Modifier.size(36.dp).clip(CircleShape).background(st.color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Icon(st.icon, null, tint = st.color) } },
                pill = if (recalled) "Recalled" to Tone.Neutral else null,
                menu = if (recalled) emptyList() else listOf<Pair<String, () -> Unit>>("Recall" to { recalling = b }),
            )
        }
    }

    // choose resellers
    Sheet(picking, "Choose resellers", { picking = false }, action = "Done", onAction = { picking = false }) {
        SearchBox(who, "Name, username, phone", { who = it })
        val all = vm.data.list("resellers")
        Row {
            androidx.compose.material3.TextButton(onClick = { chosen.clear(); chosen.addAll(all.map { it.int("id") }) }) { Text("All") }
            androidx.compose.material3.TextButton(onClick = { chosen.clear() }) { Text("None") }
        }
        all.filter { r -> who.isBlank() || listOf("username", "full_name", "phone", "company_name").any { r.str(it).contains(who, true) } }.forEach { r ->
            val id = r.int("id")
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { if (id in chosen) chosen.remove(id) else chosen.add(id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = id in chosen, onCheckedChange = { if (it) chosen.add(id) else chosen.remove(id) })
                Column(Modifier.weight(1f)) {
                    Text(r.str("full_name").ifBlank { r.str("username") }, style = MaterialTheme.typography.titleSmall)
                    Text(listOfNotNull("@" + r.str("username"), r.str("company_name").ifBlank { null }, r.str("phone").ifBlank { null }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    val rc = recalling
    Sheet(rc != null, "Recall message", { recalling = null }, action = "Recall", busy = vm.busy, onAction = {
        if (rc != null) vm.submit("/admin/messages/${rc.int("id")}/recall", form(), toast) { recalling = null }
    }) {
        Text("“${rc?.str("title")}” disappears from every bell and app that still has it. WhatsApp copies can’t be taken back.", style = MaterialTheme.typography.bodyMedium)
    }
}
