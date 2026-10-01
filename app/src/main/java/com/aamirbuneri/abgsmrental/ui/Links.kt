package com.aamirbuneri.abgsmrental.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.ui.admin.AdminRoutes
import com.aamirbuneri.abgsmrental.ui.panel.PanelCtx
import com.aamirbuneri.abgsmrental.ui.util.openUrl
import kotlinx.coroutines.delay

/** Opens what a notification (tap or its button) points at, once the screens are ready. */
@Composable
fun OpenLinks(nav: NavHostController, admin: Boolean) {
    val context = LocalContext.current
    val link by context.container.openLink.collectAsState()
    LaunchedEffect(link) {
        val l = link ?: return@LaunchedEffect
        context.container.openLink.value = null
        delay(250) // the NavHost sets up its graph first
        val site = runCatching { context.container.prefs.site() }.getOrDefault("")
        runCatching { openSiteLink(context, nav, l, admin, site) }
    }
}

/**
 * A link from the website ("/reseller/tools/3", "https://site/…/admin/rentals/9", "https://wa.me/…",
 * "notice:12") → the matching app screen, or the browser / WhatsApp / dialer for anything else.
 */
fun openSiteLink(context: Context, nav: NavHostController, link: String, admin: Boolean, siteAddress: String) {
    if (link.startsWith("notice:")) {
        nav.navigate(if (admin) AdminRoutes.NOTIFICATIONS else Routes.NOTIFICATIONS)
        return
    }
    val site = siteAddress.trimEnd('/')
    val path = localPath(link, site)
    if (path == null) {
        openUrl(context, link)
        return
    }
    val route = if (admin) adminRoute(path) else resellerRoute(path)
    when {
        route == null -> openUrl(context, if (link.startsWith("/")) "$site/index.php?r=" + android.net.Uri.encode(path) else link)
        route.startsWith("tab:") -> nav.goTab(route.removePrefix("tab:"))
        else -> nav.navigate(route)
    }
}

/** "/admin/x", "https://site/admin/x", "https://site/index.php?r=/admin/x" → "/admin/x?…"; other sites → null. */
private fun localPath(link: String, site: String): String? {
    if (link.startsWith("/") && !link.startsWith("//")) return link
    if (!link.startsWith("http")) return null
    val base = site.removePrefix("https://").removePrefix("http://")
    val bare = link.removePrefix("https://").removePrefix("http://")
    if (base.isEmpty() || !bare.startsWith(base)) return null
    val rest = bare.removePrefix(base)
    Regex("""[?&]r=([^&#]+)""").find(rest)?.let { return android.net.Uri.decode(it.groupValues[1]) }
    return rest.ifBlank { "/" }
}

private fun num(path: String, re: String): Int? = Regex(re).find(path)?.groupValues?.get(1)?.toIntOrNull()

private fun pg(key: String, id: Int = 0, x: String = "") = PanelCtx.route(key, id, x)

private fun resellerRoute(full: String): String? {
    val p = full.substringBefore('?').substringBefore('#').trimEnd('/')
    return when {
        num(p, "^/reseller/rentals/(\\d+)") != null -> Routes.rental(num(p, "^/reseller/rentals/(\\d+)")!!)
        p.startsWith("/reseller/rentals") || p.startsWith("/reseller/history") -> "tab:" + Routes.RENTALS
        num(p, "^/reseller/services/orders/(\\d+)") != null -> Routes.order(num(p, "^/reseller/services/orders/(\\d+)")!!)
        num(p, "^/reseller/services/(\\d+)") != null -> Routes.orderNew(num(p, "^/reseller/services/(\\d+)")!!)
        p.startsWith("/reseller/services") -> "tab:" + Routes.SERVICES
        p.startsWith("/reseller/tools") -> "tab:" + Routes.RENT
        p.startsWith("/reseller/wallet") -> Routes.WALLET
        num(p, "^/reseller/invoices/(\\d+)") != null -> pg("rinvoice", num(p, "^/reseller/invoices/(\\d+)")!!)
        p.startsWith("/reseller/invoices") -> pg("rinvoices")
        p.startsWith("/reseller/returns") -> pg("myreturns")
        p.startsWith("/reseller/chat") -> pg("chat", 0, "Support team")
        p.startsWith("/reseller/reviews") -> pg("myreviews")
        p.startsWith("/reseller/api") -> pg("myapi")
        p.startsWith("/reseller/notifications") -> Routes.NOTIFICATIONS
        p.startsWith("/profile/2fa") -> pg("twofa")
        p.startsWith("/profile") -> pg("profile")
        p == "/reseller" || p == "" || p == "/" -> "tab:" + Routes.HOME
        else -> null
    }
}

private fun adminRoute(full: String): String? {
    val p = full.substringBefore('?').substringBefore('#').trimEnd('/')
    val with = Regex("[?&]with=(\\d+)").find(full)?.groupValues?.get(1)?.toIntOrNull()
    return when {
        num(p, "^/admin/rentals/(\\d+)") != null -> AdminRoutes.rental(num(p, "^/admin/rentals/(\\d+)")!!)
        p.startsWith("/admin/rentals") -> "tab:" + AdminRoutes.RENTALS
        num(p, "^/admin/services/orders/(\\d+)") != null -> AdminRoutes.order(num(p, "^/admin/services/orders/(\\d+)")!!)
        p.startsWith("/admin/services/orders") -> "tab:" + AdminRoutes.ORDERS
        p.startsWith("/admin/services") -> pg("services")
        num(p, "^/admin/resellers/(\\d+)") != null -> AdminRoutes.reseller(num(p, "^/admin/resellers/(\\d+)")!!)
        p.startsWith("/admin/resellers") || p.startsWith("/admin/registrations") -> "tab:" + AdminRoutes.RESELLERS
        num(p, "^/admin/tools/(\\d+)") != null -> AdminRoutes.slots(num(p, "^/admin/tools/(\\d+)")!!)
        p.startsWith("/admin/tools") -> AdminRoutes.TOOLS
        p.startsWith("/admin/accounts") -> pg("accounts")
        num(p, "^/admin/invoices/(\\d+)") != null -> pg("invoice", num(p, "^/admin/invoices/(\\d+)")!!)
        p.startsWith("/admin/invoices") -> pg("invoices")
        p.startsWith("/admin/returns") -> pg("returns")
        p.startsWith("/admin/chat") -> if (with != null) pg("chat", with, "Chat") else pg("chats")
        p.startsWith("/admin/messages") -> pg("compose")
        p.startsWith("/admin/announcements") -> pg("ann")
        p.startsWith("/admin/website/banners") -> pg("banners")
        p.startsWith("/admin/website") -> pg("website")
        p.startsWith("/admin/reviews") -> pg("reviews")
        p.startsWith("/admin/api-access") -> pg("apiaccess")
        p.startsWith("/admin/staff") -> pg("staff")
        p.startsWith("/admin/settings") -> pg("settings")
        p.startsWith("/admin/system") -> pg("backups")
        p.startsWith("/admin/activity") -> pg("activity")
        p.startsWith("/admin/reports") -> pg("reports")
        p.startsWith("/admin/profit") -> pg("profit")
        p.startsWith("/admin/clients") -> pg("clients")
        p.startsWith("/admin/assign") -> pg("assign")
        p.startsWith("/admin/notifications") -> AdminRoutes.NOTIFICATIONS
        p.startsWith("/profile/2fa") -> pg("twofa")
        p.startsWith("/profile") -> pg("profile")
        p == "/admin" || p == "" || p == "/" -> "tab:" + AdminRoutes.HOME
        else -> null
    }
}
