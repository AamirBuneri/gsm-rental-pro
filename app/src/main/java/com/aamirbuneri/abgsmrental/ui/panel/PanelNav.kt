package com.aamirbuneri.abgsmrental.ui.panel

import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.data.Settings

/** What every panel page needs: navigation, the signed-in account, the site's address and base currency. */
class PanelCtx(val nav: NavHostController, val settings: Settings, val currency: String) {
    val site: String get() = settings.site

    fun go(key: String, id: Int = 0, extra: String = "") = nav.navigate(route(key, id, extra))

    /** Open [key] instead of the current page (e.g. new tool → its slots). */
    fun replace(key: String, id: Int = 0) {
        nav.popBackStack()
        nav.navigate(route(key, id))
    }

    companion object {
        const val ROUTE = "pg/{key}/{id}?x={x}"
        fun route(key: String, id: Int = 0, extra: String = "") = "pg/$key/$id" + if (extra.isNotBlank()) "?x=" + android.net.Uri.encode(extra) else ""
    }
}
