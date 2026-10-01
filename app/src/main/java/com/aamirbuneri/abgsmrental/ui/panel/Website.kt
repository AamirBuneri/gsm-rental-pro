package com.aamirbuneri.abgsmrental.ui.panel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewCarousel
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.FilePart
import com.aamirbuneri.abgsmrental.data.bool
import com.aamirbuneri.abgsmrental.data.form
import com.aamirbuneri.abgsmrental.data.int
import com.aamirbuneri.abgsmrental.data.list
import com.aamirbuneri.abgsmrental.data.obj
import com.aamirbuneri.abgsmrental.data.str
import com.aamirbuneri.abgsmrental.ui.admin.FilterOption
import com.aamirbuneri.abgsmrental.ui.admin.FilterRow
import com.aamirbuneri.abgsmrental.ui.admin.rememberToast
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.Tone
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.ago
import com.aamirbuneri.abgsmrental.ui.util.parseIso
import kotlinx.serialization.json.JsonElement
import java.text.SimpleDateFormat
import java.util.Locale

// ── website posts ──────────────────────────────────────────────────────────

@Composable
fun WebsitePanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_site") { PageVM(api, "/admin/website") }
    val toast = rememberToast()
    LaunchedEffect(Unit) { if (vm.data != null) vm.load() }
    Panel("Website", ctx.nav, vm, actions = {
        IconButton(onClick = { ctx.go("post", 0) }) { Icon(Icons.Outlined.Add, "New post") }
    }) { d ->
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassCard(Modifier.weight(1f), onClick = { ctx.go("banners") }) {
                    Icon(Icons.Outlined.ViewCarousel, null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Banners", style = MaterialTheme.typography.titleSmall)
                    Text("Home page slider", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                GlassCard(Modifier.weight(1f), onClick = { ctx.go("reviews") }) {
                    Icon(Icons.Outlined.Star, null, tint = AB.brand.warning)
                    Text("Reviews", style = MaterialTheme.typography.titleSmall)
                    Text("What shows on the site", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (d.int("notPosted") > 0) item {
            Text("${d.int("notPosted")} tools / services aren’t on the website yet — tap + and link one.", style = MaterialTheme.typography.bodySmall, color = AB.brand.info)
        }
        val posts = d.list("posts")
        if (posts.isEmpty()) item { EmptyState(Icons.Outlined.Language, "No posts", "Cards on your public website. Tap + to add one.") }
        posts.groupBy { it.str("section") }.forEach { (section, rows) ->
            item { Section(if (section == "services") "Services" else "Tools") }
            items(rows, key = { "post" + it.int("id") }) { p ->
                val on = p.bool("is_active")
                Line(
                    p.str("title"),
                    p.str("warning").ifBlank { null } ?: listOfNotNull(
                        p.str("category").ifBlank { null },
                        when (p.str("link_type")) { "tool" -> "→ rent " + p.obj("linked").str("name"); "service" -> "→ order " + p.obj("linked").str("name"); "url" -> "→ " + p.str("link_url"); else -> null },
                        when (p.str("price_mode")) { "custom" -> p.str("price_text"); "auto" -> "live price"; else -> null },
                    ).joinToString(" · "),
                    leading = {
                        val img = siteFile(ctx.site, p.str("image"))
                        if (img != null) AsyncImage(img, null, Modifier.height(40.dp).aspectRatio(1f).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        else Dot(p.str("title"), p.str("color"))
                    },
                    pill = if (p.str("warning").isNotBlank()) "Check" to Tone.Warning else if (on) "Live" to Tone.Success else "Hidden" to Tone.Neutral,
                    onClick = { ctx.go("post", p.int("id")) },
                    menu = listOf<Pair<String, () -> Unit>>(
                        "Edit" to { ctx.go("post", p.int("id")) },
                        (if (on) "Hide" else "Show") to { vm.submit("/admin/website/${p.int("id")}/toggle", form(), toast) },
                        "Delete" to { vm.submit("/admin/website/${p.int("id")}/delete", form(), toast) },
                    ),
                )
            }
        }
    }
}

@Composable
fun PostFormPanel(ctx: PanelCtx, id: Int) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_post$id") { PageVM(api, if (id > 0) "/admin/website/$id/edit" else "/admin/website/new") }
    val toast = rememberToast()
    val f = remember { FormState() }
    var filled by remember { mutableStateOf(false) }
    var picture by remember { mutableStateOf<FilePart?>(null) }
    val d = vm.data
    LaunchedEffect(d) {
        if (d != null && !filled) {
            filled = true
            val p = d.obj("post") ?: d.obj("prefill")
            f["section"] = p.str("section", "tools"); f["title"] = p.str("title"); f["category"] = p.str("category"); f["description"] = p.str("description")
            f["color"] = p.str("color", "#22d3ee"); f["badge"] = p.str("badge"); f["link_type"] = p.str("link_type", "none")
            f["link_tool"] = if (p.str("link_type") == "tool") p.str("link_id") else ""
            f["link_service"] = if (p.str("link_type") == "service") p.str("link_id") else ""
            f["link_url"] = p.str("link_url"); f["button_label"] = p.str("button_label"); f["price_mode"] = p.str("price_mode", "hide"); f["price_text"] = p.str("price_text")
            f["show_status"] = if (p == null || p.bool("show_status")) "1" else "0"
            f["is_active"] = if (p == null || d.obj("post") == null || p.bool("is_active")) "1" else "0"
            f["sort_order"] = p.str("sort_order", "0")
        }
    }
    fun save() {
        vm.submitFiles(if (id > 0) "/admin/website/$id/edit" else "/admin/website/new", f.pairs(), listOfNotNull(picture), toast, reload = false) { ctx.nav.popBackStack() }
    }
    Panel(if (id > 0) "Edit post" else "New post", ctx.nav, vm, bottom = {
        GradientButton(if (id > 0) "Save post" else "Publish post", { save() }, Modifier.fillMaxWidth(), loading = vm.busy)
    }) { data ->
        val links = data.obj("links")
        item {
            Group("Card") {
                ChoiceIn(f, "section", "Section", listOf("tools" to "Tools", "services" to "Services"))
                TextIn(f, "title", "Title")
                TextIn(f, "category", "Category (optional)")
                TextIn(f, "description", "Description", lines = 4)
                TextIn(f, "badge", "Badge (optional)", hint = "e.g. HOT, NEW, -20%")
                ColorIn(f, "color", TOOL_COLORS)
                ImageIn("Picture", picture, siteFile(ctx.site, data.obj("post").str("image"))) { picture = it }
                if (data.obj("post").str("image").isNotBlank()) SwitchIn(f, "remove_image", "Remove the current picture")
            }
        }
        item {
            Group("Button") {
                ChoiceIn(f, "link_type", null, listOf("tool" to "Rent a tool", "service" to "Order a service", "url" to "Open a link", "none" to "No button"))
                when (f["link_type"]) {
                    "tool" -> {
                        PickIn(f, "link_tool", "Tool", links.list("tools").map { it.str("id") to it.str("name") })
                        SwitchIn(f, "show_status", "Show free / busy", "Live availability on the card")
                    }
                    "service" -> PickIn(f, "link_service", "Service", links.list("services").map { it.str("id") to it.str("name") })
                    "url" -> TextIn(f, "link_url", "Link", hint = "https://wa.me/923001234567, tel:…, /register")
                }
                if (f["link_type"] != "none") TextIn(f, "button_label", "Button text", hint = "Rent now")
            }
        }
        item {
            Group("Price") {
                ChoiceIn(f, "price_mode", null, listOf("hide" to "Hide", "custom" to "My text", "auto" to "Live price"))
                if (f["price_mode"] == "custom") TextIn(f, "price_text", "Price text", hint = "Rs 450 / day")
            }
        }
        item {
            Group {
                SwitchIn(f, "is_active", "Show on the website")
                TextIn(f, "sort_order", "Order", number = true, hint = "Smaller numbers come first")
            }
        }
    }
}

// ── banners ────────────────────────────────────────────────────────────────

private val LOCAL_DT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)

@Composable
fun BannersPanel(ctx: PanelCtx) {
    val context = LocalContext.current
    val api = context.container.api
    val vm: PageVM = viewModel(key = "p_banners") { PageVM(api, "/admin/website/banners") }
    val toast = rememberToast()
    var editing by remember { mutableStateOf<JsonElement?>(null) }
    var picture by remember { mutableStateOf<FilePart?>(null) }
    var phonePicture by remember { mutableStateOf<FilePart?>(null) }
    var settings by remember { mutableStateOf(false) }
    val f = remember { FormState() }
    val s = remember { FormState() }
    val upload = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        val files = uris.mapNotNull { readPicture(context, it, "images[]") }
        if (files.isNotEmpty()) vm.submitFiles("/admin/website/banners", emptyList(), files, toast)
        else if (uris.isNotEmpty()) toast("Pictures must be under 5 MB.")
    }
    Panel("Banners", ctx.nav, vm, actions = {
        IconButton(onClick = {
            s.values.clear(); vm.data.obj("settings")?.let { st -> st.keys.forEach { k -> s[k] = st.str(k) } }; settings = true
        }) { Icon(Icons.Outlined.Tune, "Slider settings") }
        IconButton(onClick = { upload.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Outlined.AddPhotoAlternate, "Add banners") }
    }) { d ->
        val st = d.obj("settings")
        item {
            Text(if (st.str("banner_enabled") == "1") "The slider is ON on your home page. Wide pictures (e.g. 1920×600) look best." else "The slider is OFF — turn it on in ⚙ settings.",
                style = MaterialTheme.typography.bodySmall, color = if (st.str("banner_enabled") == "1") AB.brand.success else AB.brand.warning)
        }
        val rows = d.list("banners")
        if (rows.isEmpty()) item { EmptyState(Icons.Outlined.ViewCarousel, "No banners", "Tap the picture button to upload one or many.") }
        items(rows, key = { "bn" + it.int("id") }) { b ->
            val on = b.bool("is_active")
            GlassCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                AsyncImage(siteFile(ctx.site, b.str("image")), null, Modifier.fillMaxWidth().aspectRatio(3.2f).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)), contentScale = ContentScale.Crop)
                Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(b.str("title").ifBlank { "Banner #${b.int("id")}" }, style = MaterialTheme.typography.titleSmall)
                        Text(listOfNotNull(if (on) "Live" else "Hidden", b.str("link_url").ifBlank { null }, b.str("ends_at").ifBlank { null }?.let { "ends " + com.aamirbuneri.abgsmrental.ui.util.shortDate(it) },
                            if (b.str("image_mobile").isNotBlank()) "phone picture" else null).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = if (on) AB.brand.success else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MoreMenu(listOf<Pair<String, () -> Unit>>(
                        "Edit" to {
                            f.values.clear(); picture = null; phonePicture = null
                            f["title"] = b.str("title"); f["link_url"] = b.str("link_url"); f["new_tab"] = if (b.bool("new_tab")) "1" else "0"; f["is_active"] = if (on) "1" else "0"
                            f["starts_at"] = parseIso(b.str("starts_at"))?.let { LOCAL_DT.format(it) } ?: ""
                            f["ends_at"] = parseIso(b.str("ends_at"))?.let { LOCAL_DT.format(it) } ?: ""
                            editing = b
                        },
                        (if (on) "Hide" else "Show") to { vm.submit("/admin/website/banners/${b.int("id")}/toggle", form(), toast) },
                        "Move up" to { vm.submit("/admin/website/banners/${b.int("id")}/move", form("dir" to "up"), toast) },
                        "Move down" to { vm.submit("/admin/website/banners/${b.int("id")}/move", form("dir" to "down"), toast) },
                        "Delete" to { vm.submit("/admin/website/banners/${b.int("id")}/delete", form(), toast) },
                    ))
                }
            }
        }
    }
    val e = editing
    Sheet(e != null, "Edit banner", { editing = null }, action = "Save", busy = vm.busy, onAction = {
        if (e != null) {
            val out = f.pairs().map { (k, v) ->
                k to when { k == "starts_at" && v.length == 10 -> "${v}T00:00"; k == "ends_at" && v.length == 10 -> "${v}T23:59"; else -> v }
            }
            vm.submitFiles("/admin/website/banners/${e.int("id")}/edit", out, listOfNotNull(picture, phonePicture?.let { FilePart("image_mobile", it.fileName, it.mime, it.bytes) }), toast) { if (it.ok) editing = null }
        }
    }) {
        TextIn(f, "title", "Name (for you)")
        TextIn(f, "link_url", "Link when tapped (optional)", hint = "https://… or /register")
        SwitchIn(f, "new_tab", "Open the link in a new tab")
        SwitchIn(f, "is_active", "Show on the website")
        DateIn(f, "starts_at", "Start (optional)")
        DateIn(f, "ends_at", "End (optional)")
        ImageIn("Replace picture", picture, siteFile(ctx.site, e?.str("image"))) { picture = it }
        ImageIn("Phone picture (optional)", phonePicture, siteFile(ctx.site, e?.str("image_mobile"))) { phonePicture = it }
        if (e?.str("image_mobile")?.isNotBlank() == true) SwitchIn(f, "remove_mobile", "Remove the phone picture")
    }
    Sheet(settings, "Slider settings", { settings = false }, action = "Save", busy = vm.busy, onAction = {
        vm.submit("/admin/website/banners/settings", s.json(), toast) { if (it.ok) settings = false }
    }) {
        val d = vm.data
        SwitchIn(s, "banner_enabled", "Show the banner slider", "On your website’s home page")
        ChoiceIn(s, "banner_effect", "Effect", d.obj("effects")?.let { m -> m.keys.map { it to m.str(it) } } ?: listOf("slide" to "Slide", "fade" to "Fade", "zoom" to "Zoom"))
        ChoiceIn(s, "banner_position", "Position", d.obj("positions")?.let { m -> m.keys.map { it to m.str(it) } } ?: listOf("above" to "Above", "below" to "Below", "replace" to "Banner only"))
        ChoiceIn(s, "banner_width", "Width", listOf("contained" to "Inside the page", "full" to "Full width"))
        ChoiceIn(s, "banner_nav_style", "Indicators", listOf("bars" to "Progress lines", "dots" to "Dots"))
        TextIn(s, "banner_interval", "Seconds per banner", number = true)
        SwitchIn(s, "banner_arrows", "Arrows")
        SwitchIn(s, "banner_dots", "Indicators")
        SwitchIn(s, "banner_glow", "Glow behind the slider")
    }
}

// ── reviews ────────────────────────────────────────────────────────────────

@Composable
fun ReviewsPanel(ctx: PanelCtx) {
    val api = LocalContext.current.container.api
    val vm: PageVM = viewModel(key = "p_reviews") { PageVM(api, "/admin/reviews") }
    val toast = rememberToast()
    var replying by remember { mutableStateOf<JsonElement?>(null) }
    var settings by remember { mutableStateOf(false) }
    val f = remember { FormState() }
    val s = remember { FormState() }
    Panel("Reviews", ctx.nav, vm, actions = {
        IconButton(onClick = { s.values.clear(); vm.data.obj("settings")?.let { st -> st.keys.forEach { k -> s[k] = st.str(k) } }; settings = true }) { Icon(Icons.Outlined.Tune, "Review settings") }
    }) { d ->
        val sum = d.obj("summary")
        val c = d.obj("counts")
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(sum.str("avg", "0"), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = AB.brand.warning)
                    Spacer(Modifier.padding(6.dp))
                    Column {
                        Text("★".repeat(sum.str("avg", "0").toDoubleOrNull()?.let { Math.round(it).toInt() } ?: 0).padEnd(5, '☆'), color = AB.brand.warning, style = MaterialTheme.typography.titleMedium)
                        Text("${sum.int("count")} reviews on the website", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            FilterRow(listOf(FilterOption("", "All"), FilterOption("pending", "Waiting", c.int("pending")), FilterOption("approved", "On website", c.int("approved")), FilterOption("hidden", "Hidden", c.int("hidden"))),
                vm.query["status"] ?: "", { vm.set("status", it) }, Modifier.padding(horizontal = 0.dp))
        }
        val rows = d.list("rows")
        if (rows.isEmpty()) item { EmptyState(Icons.Outlined.Star, "No reviews", "Resellers rate finished rentals and services.") }
        items(rows, key = { "rv" + it.int("id") }) { r ->
            val st = r.str("status")
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("★".repeat(r.int("rating")).padEnd(5, '☆'), color = AB.brand.warning)
                        Text("${r.str("full_name").ifBlank { r.str("username") }} · ${if (r.str("kind") == "service") "service" else "rental"} · ${ago(r.str("created_at"))}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    com.aamirbuneri.abgsmrental.ui.components.StatusPill(when (st) { "approved" -> "On website"; "hidden" -> "Hidden"; else -> "Waiting" }, when (st) { "approved" -> Tone.Success; "hidden" -> Tone.Neutral; else -> Tone.Warning })
                    MoreMenu(buildList<Pair<String, () -> Unit>> {
                        if (st != "approved") add("Show on website" to { vm.submit("/admin/reviews/${r.int("id")}/status", form("status" to "approved"), toast) })
                        if (st != "hidden") add("Hide" to { vm.submit("/admin/reviews/${r.int("id")}/status", form("status" to "hidden"), toast) })
                        add("Reply" to { f.values.clear(); f["reply"] = r.str("reply"); replying = r })
                        add("Delete" to { vm.submit("/admin/reviews/${r.int("id")}/delete", form(), toast) })
                    })
                }
                if (r.str("comment").isNotBlank()) { Spacer(Modifier.height(6.dp)); Text("“${r.str("comment")}”", style = MaterialTheme.typography.bodyMedium) }
                if (r.str("reply").isNotBlank()) { Spacer(Modifier.height(6.dp)); Text("You: ${r.str("reply")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
            }
        }
        pager(d, vm)
    }
    val rp = replying
    Sheet(rp != null, "Reply", { replying = null }, action = "Save", busy = vm.busy, onAction = {
        if (rp != null) vm.submit("/admin/reviews/${rp.int("id")}/reply", f.json(), toast) { replying = null }
    }) {
        Text("Shown under the review on the website. Leave empty to remove your reply.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextIn(f, "reply", "Reply", lines = 4)
    }
    Sheet(settings, "Review settings", { settings = false }, action = "Save", busy = vm.busy, onAction = {
        vm.submit("/admin/reviews/settings", s.json(), toast) { if (it.ok) settings = false }
    }) {
        SwitchIn(s, "reviews_enabled", "Ask resellers for reviews", "A “Rate” button on finished rentals and services")
        SwitchIn(s, "reviews_on_site", "Show reviews on the website")
        ChoiceIn(s, "reviews_auto", "Publish automatically", listOf("0" to "Never (I check each)", "4" to "4★ and up", "5" to "Only 5★"))
    }
}
