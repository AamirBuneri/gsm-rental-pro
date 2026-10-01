package com.aamirbuneri.abgsmrental.ui.panel

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/** Every panel page under one route: "pg/{key}/{id}?x={x}". */
fun NavGraphBuilder.panelPages(ctx: () -> PanelCtx) {
    composable(
        PanelCtx.ROUTE,
        arguments = listOf(
            navArgument("key") { type = NavType.StringType },
            navArgument("id") { type = NavType.IntType; defaultValue = 0 },
            navArgument("x") { type = NavType.StringType; nullable = true; defaultValue = null },
        ),
    ) { entry ->
        val key = entry.arguments?.getString("key").orEmpty()
        val id = entry.arguments?.getInt("id") ?: 0
        val x = entry.arguments?.getString("x").orEmpty()
        PanelPage(ctx(), key, id, x)
    }
}

@androidx.compose.runtime.Composable
fun PanelPage(ctx: PanelCtx, key: String, id: Int, x: String) {
    when (key) {
        // catalog
        "tools" -> ToolsPanel(ctx)
        "tool" -> ToolFormPanel(ctx, id)
        "slots" -> SlotsPanel(ctx, id)
        "accounts" -> AccountsPanel(ctx)
        "services" -> ServicesPanel(ctx)
        "service" -> ServiceFormPanel(ctx, id)
        // operations & money
        "assign" -> AssignPanel(ctx, id)
        "clients" -> ClientsPanel(ctx)
        "returns" -> ReturnsPanel(ctx)
        "invoices" -> InvoicesPanel(ctx)
        "invoice" -> InvoicePanel(ctx, id)
        "reports" -> ReportsPanel(ctx)
        "profit" -> ProfitPanel(ctx)
        // communication
        "chats" -> ChatListPanel(ctx)
        "chat" -> ChatPanel(ctx, id, x)
        "orderchat" -> ChatPanel(ctx, 0, x, orderId = id)
        "compose" -> ComposerPanel(ctx)
        "ann" -> AnnouncementsPanel(ctx)
        // website
        "website" -> WebsitePanel(ctx)
        "post" -> PostFormPanel(ctx, id)
        "banners" -> BannersPanel(ctx)
        "reviews" -> ReviewsPanel(ctx)
        // resellers & system
        "rform" -> ResellerFormPanel(ctx, id)
        "rdelete" -> ResellerDeletePanel(ctx, id)
        "apiaccess" -> ApiAccessPanel(ctx)
        "staff" -> StaffPanel(ctx)
        "settings" -> SettingsPanel(ctx)
        "setting" -> SettingsSectionPanel(ctx, x)
        "activity" -> ActivityPanel(ctx)
        "backups" -> BackupsPanel(ctx)
        // reseller side + everyone
        "rinvoices" -> InvoicesPanel(ctx, reseller = true)
        "rinvoice" -> InvoicePanel(ctx, id, reseller = true)
        "myreturns" -> MyReturnsPanel(ctx)
        "returnreq" -> RequestReturnPanel(ctx, id, x)
        "myapi" -> MyApiPanel(ctx)
        "myreviews" -> MyReviewsPanel(ctx)
        "profile" -> ProfilePanel(ctx)
        "twofa" -> TwoFaPanel(ctx)
        else -> androidx.compose.runtime.LaunchedEffect(key) { ctx.nav.popBackStack() }
    }
}
