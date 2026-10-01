@file:OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)

package com.aamirbuneri.abgsmrental

import android.content.Context
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.github.takahirom.roborazzi.captureScreenRoboImage
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.CopyOnWriteArrayList

/** A fake GSM Rental Pro site: answers /index.php?r=/api/v1/… like the real one (3.4.0). */
class FakeSite : Dispatcher() {
    val calls = CopyOnWriteArrayList<String>()
    val bodies = CopyOnWriteArrayList<String>()

    /** The admin approved the new sign-up (the waiting screen's "Check again" then signs in). */
    @Volatile var approved = false

    override fun dispatch(request: RecordedRequest): MockResponse {
        val r = request.requestUrl?.queryParameter("r").orEmpty()
        val m = request.method.orEmpty()
        val body = request.body.readUtf8()
        calls += "$m $r"
        if (body.isNotBlank()) bodies += "$r $body"
        val key = request.getHeader("X-API-Key").orEmpty()
        val authed = key.startsWith("gma_")
        val admin = key == ADMIN_TOKEN
        fun ok(data: String, code: Int = 200) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody("""{"ok":true,"data":$data}""")
        fun fail(code: Int, error: String, state: String? = null) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json")
            .setBody("""{"ok":false,"error":"$error"${if (state != null) ""","state":"$state"""" else ""}}""")
        if (r != "/api/v1/app/info" && !r.startsWith("/api/v1/auth/") && !authed) return fail(401, "Your session ended. Please sign in again.")
        if (r.startsWith("/api/v1/admin/") && !admin) return fail(403, "This needs an admin or staff account.")
        return when {
            r == "/api/v1/app/info" -> ok("""{"app":"gsm-rental-pro","api_version":1,"version":"3.4.0","name":"AB Tools","tagline":"GSM tool rental","currency":"PKR","app_enabled":true,"maintenance":false,"services":true,"registration":true,
                "app_signup":true,"password_reset":true,"team_app":true,
                "currencies":[{"code":"PKR","name":"Pakistani Rupee","symbol":"Rs"},{"code":"USD","name":"US Dollar","symbol":"$"},{"code":"EUR","name":"Euro","symbol":"€"},{"code":"GBP","name":"British Pound","symbol":"£"},{"code":"AED","name":"UAE Dirham","symbol":"AED"}],
                "website":"http://x/","register_url":"http://x/register","forgot_url":"http://x/forgot","support":{"whatsapp":"923001234567","phone":"+92 300 1234567","email":"help@abtools.pk"}}""")
            r == "/api/v1/auth/login" && body.contains("\"admin\"") -> ok("""{"need_2fa":false,"token":"$ADMIN_TOKEN","user":$ADMIN_USER}""")
            r == "/api/v1/auth/login" && body.contains("\"newshop\"") ->
                if (approved) ok("""{"need_2fa":false,"token":"gma_${"c3".repeat(24)}","user":{"username":"newshop","name":"Bilal Ahmed","email":"bilal@shop.pk","role":"reseller","team":false,"owner":false,"perms":[]}}""")
                else fail(403, "Your account is waiting for admin approval. You can sign in as soon as it’s approved.", "pending")
            r == "/api/v1/auth/login" -> ok("""{"need_2fa":false,"token":"gma_${"a1".repeat(24)}","user":{"username":"ali","name":"Ali Khan","email":"ali@shop.pk","role":"reseller","team":false,"owner":false,"perms":[]}}""")
            r == "/api/v1/auth/register" -> ok("""{"state":"pending","token":null,"message":"Your account is waiting for admin approval. You can sign in as soon as it’s approved.","user":{"username":"newshop","name":"Bilal Ahmed","email":"bilal@shop.pk","role":"reseller"}}""", 201)
            r == "/api/v1/auth/forgot" -> ok("""{"message":"If an account uses bilal@shop.pk, a reset link is on its way. Open it on this phone, choose a new password, then sign in here."}""")
            r == "/api/v1/auth/me" -> ok(if (admin) ADMIN_USER else """{"username":"ali","name":"Ali Khan","email":"ali@shop.pk","role":"reseller","team":false,"owner":false,"perms":[]}""")
            r == "/api/v1/auth/logout" -> ok("""{"signed_out":true}""")
            // ── reseller ──
            r == "/api/v1/dashboard" -> ok("""{"account":{"username":"ali","name":"Ali Khan","email":"ali@shop.pk","balance":4650,"currency":"PKR","credit_allowed":true,"credit_limit":2000,"can_spend":6650,"discount_percent":5},
                "active_rentals":[$RUNNING],"open_orders":[$ORDER_OPEN],"unread":3,
                "stats":{"rentals_total":42,"month_rentals":12,"month_spent":8400},"services":true,"server_time":"2026-10-01T10:00:00Z"}""")
            r == "/api/v1/tools" -> ok("""[
                {"id":1,"name":"UnlockTool","description":"All-in-one tool for Qualcomm, MTK and Samsung — FRP, flash, IMEI-free repairs.","color":"#22d3ee","available":true,"free_slots":3,"plans":[{"id":1,"label":"2 Hours","minutes":120,"price":150,"currency":"PKR"},{"id":2,"label":"6 Hours","minutes":360,"price":350,"currency":"PKR"},{"id":3,"label":"24 Hours","minutes":1440,"price":900,"currency":"PKR"}]},
                {"id":2,"name":"Chimera Tool","description":"Samsung, Huawei, LG and Xiaomi service tool.","color":"#a855f7","available":false,"next_free_at":"2026-10-01T16:30:00Z","plans":[{"id":4,"label":"4 Hours","minutes":240,"price":500,"currency":"PKR"}]},
                {"id":3,"name":"Griffin Unlocker","description":"iPhone and Samsung unlocking.","color":"#f59e0b","available":true,"free_slots":1,"plans":[{"id":5,"label":"1 Day","minutes":1440,"price":1200,"currency":"PKR"}]}]""")
            r == "/api/v1/rentals" && m == "POST" -> ok(RUNNING.dropLast(1) + ",\"balance\":4300}")
            r == "/api/v1/rentals" -> ok("""[$RUNNING,{"id":7,"number":"RNT-260930-X1","tool":"Chimera Tool","plan":"4 Hours","minutes":240,"price":500,"currency":"PKR","status":"expired","started_at":"2026-09-30T08:00:00Z","expires_at":"2026-09-30T12:00:00Z","seconds_left":0,"tool_id":2,"color":"#a855f7"},{"id":6,"number":"RNT-260929-Q2","tool":"UnlockTool","plan":"2 Hours","minutes":120,"price":150,"currency":"PKR","status":"returned","started_at":"2026-09-29T08:00:00Z","expires_at":"2026-09-29T10:00:00Z","seconds_left":0,"tool_id":1,"color":"#22d3ee"}]""")
            r.startsWith("/api/v1/rentals/") -> ok(RUNNING)
            r == "/api/v1/services" -> ok("""[
                {"id":1,"name":"Xiaomi FRP Remove","category":"Xiaomi","description":"Google account lock removal for all Xiaomi / Redmi / Poco.","color":"#f97316","price":500,"currency":"PKR","quote":false,"eta_minutes":20,"needs_remote":true,"fields":[{"label":"Model","required":true},{"label":"Android version","required":false}]},
                {"id":2,"name":"Samsung Network Unlock","category":"Samsung","description":"Carrier unlock.","color":"#3b82f6","price":null,"currency":"PKR","quote":true,"eta_minutes":60,"needs_remote":false,"fields":[]},
                {"id":3,"name":"iCloud Info Check","category":"Apple","description":"","color":"#64748b","price":150,"currency":"PKR","quote":false,"eta_minutes":5,"needs_remote":false,"fields":[{"label":"IMEI / Serial","required":true}]}]""")
            r == "/api/v1/orders" -> ok("""[$ORDER_OPEN,$ORDER_DONE]""")
            r.startsWith("/api/v1/orders/") -> ok(ORDER_DONE)
            r == "/api/v1/notifications" -> ok("""{"unread":3,"items":[
                {"id":10,"title":"🎉 UnlockTool 20% off today","message":"Rent **6 hours** for the price of 5 — _today only_. Details: https://abtools.pk/offer","type":"promo","read":false,"rental_id":null,"link":"","created_at":"2026-10-01T09:59:00Z","broadcast":true,"action_label":"Rent now","action_url":"/reseller/tools"},
                {"id":9,"title":"Tool ready: UnlockTool","message":"Your rental RNT-261001-AB12 is active until 4:00 PM.","type":"success","read":false,"rental_id":1,"link":"/reseller/rentals","created_at":"2026-10-01T09:58:00Z"},
                {"id":8,"title":"Order completed","message":"Xiaomi FRP Remove — done.","type":"info","read":false,"rental_id":null,"link":"/reseller/services/orders/5","created_at":"2026-09-30T18:00:00Z"},
                {"id":7,"title":"Wallet topped up","message":"Rs 5,000 added to your wallet.","type":"success","read":true,"rental_id":null,"link":"/reseller/wallet","created_at":"2026-09-29T10:00:00Z"}]}""")
            r == "/api/v1/notifications/read" -> ok("""{"unread":0}""")
            r == "/api/v1/wallet" -> ok("""{"balance":4650,"currency":"PKR","credit_allowed":true,"credit_limit":2000,"can_spend":6650,"topup_whatsapp":"923001234567","items":[
                {"id":30,"type":"debit","amount":350,"balance_after":4650,"currency":"PKR","description":"Rental UnlockTool (6 Hours)","reference_type":"rental","reference_id":1,"created_at":"2026-10-01T10:00:00Z"},
                {"id":29,"type":"credit","amount":5000,"balance_after":5000,"currency":"PKR","description":"Wallet top-up","reference_type":"topup","reference_id":null,"created_at":"2026-09-29T10:00:00Z"},
                {"id":28,"type":"credit","amount":500,"balance_after":0,"currency":"PKR","description":"Refund: SRV-260928-A1 (Xiaomi FRP Remove)","reference_type":"service","reference_id":4,"created_at":"2026-09-28T10:00:00Z"}]}""")
            // ── owner / staff ──
            r == "/api/v1/admin/dashboard" -> ok("""{"me":$ADMIN_USER,"currency":"PKR","slots":{"free":5,"busy":7,"total":12},"rentals":{"active":7,"today":14},"resellers":38,"unread":4,"services_enabled":true,
                "sales":{"today":18450,"yesterday":15200,"month_revenue":312800,"month_profit":201400,"outstanding":6500,"week":[{"label":"Fri 25","value":12400,"count":9},{"label":"Sat 26","value":16800,"count":12},{"label":"Sun 27","value":9100,"count":6},{"label":"Mon 28","value":14300,"count":10},{"label":"Tue 29","value":17900,"count":13},{"label":"Wed 30","value":15200,"count":11},{"label":"Thu 01","value":18450,"count":14}]},
                "services":{"ready":2,"need_quote":1,"quoted":1,"in_progress":1,"today":6},"pending_registrations":2,"pending_returns":0,
                "live":[$A_RENTAL,${A_RENTAL.replace("\"id\":11", "\"id\":12").replace("Ali Khan", "Usman Mobiles").replace("\"seconds_left\":9000", "\"seconds_left\":1500").replace("Slot A", "Slot C")}],
                "expiring_accounts":[{"slot_id":3,"slot":"Slot C","tool":"Chimera Tool","tool_id":2,"expires_at":"2026-10-04T12:00:00Z"}],
                "queue":[$A_ORDER_READY,$A_ORDER_QUOTE],"server_time":"2026-10-01T10:00:00Z"}""")
            r == "/api/v1/admin/notifications" -> ok("""{"unread":2,"items":[
                {"id":40,"title":"New registration: newshop","message":"Bilal Ahmed (Bilal Mobiles) signed up in the Android app and is waiting for verification.","type":"info","read":false,"rental_id":null,"link":"/admin/registrations","created_at":"2026-10-01T09:50:00Z"},
                {"id":39,"title":"New service order","message":"Xiaomi FRP Remove from Ali Khan — paid Rs 500.","type":"info","read":false,"rental_id":null,"link":"/admin/services/orders/21","created_at":"2026-10-01T09:40:00Z"},
                {"id":38,"title":"Tool account expiring","message":"Chimera Tool · Slot C ends in 3 days.","type":"warning","read":true,"rental_id":null,"link":"/admin/tools/2/slots","created_at":"2026-09-30T20:00:00Z"}]}""")
            r == "/api/v1/admin/notifications/read" -> ok("""{"unread":0}""")
            r == "/api/v1/admin/rentals" -> ok("""{"items":[$A_RENTAL,${A_RENTAL.replace("\"id\":11", "\"id\":12").replace("Ali Khan", "Usman Mobiles").replace("\"seconds_left\":9000", "\"seconds_left\":1500").replace("Slot A", "Slot C")}],"page":1,"pages":1,"total":2,"counts":{"all":318,"active":7,"expired":290,"closed":12,"returned":9}}""")
            r.startsWith("/api/v1/admin/rentals/") -> ok(A_RENTAL.dropLast(1) + ""","login":{"username":"shop_user_07","email":"","password":"Unl0ck!2026"},"notes":"","close_reason":"","history":[{"body":"Customer asked for 1 more hour","by":"Aamir","at":"2026-10-01T10:30:00Z"}]}""")
            r == "/api/v1/admin/orders" -> ok("""{"items":[$A_ORDER_READY,$A_ORDER_QUOTE,${A_ORDER_READY.replace("\"id\":21", "\"id\":22").replace("\"status\":\"pending\"", "\"status\":\"in_progress\"").replace("Redmi Note 12", "Poco X5")}],"page":1,"pages":1,"total":3,"counts":{"open":3,"completed":212,"cancelled":6,"failed":3}}""")
            r.startsWith("/api/v1/admin/orders/") -> ok(A_ORDER_READY.dropLast(1) + ""","details":{"Model":"23021RAAEG","Android version":"13"},"note":"Customer waiting in shop","result":null,"remote_app":"AnyDesk","has_remote":true,"reseller_phone":"923001112233","eta_minutes":20,"quoted_at":null,"can_start":true,"can_quote":false,"can_complete":true,"can_close":true}""")
            r == "/api/v1/admin/resellers" -> ok("""{"items":[$A_PENDING,$A_RESELLER,
                {"id":5,"username":"usman","name":"Usman Mobiles","email":"usman@x.pk","phone":"923009990000","company":"Usman Mobiles","balance":-1200,"currency":"PKR","tier":"gold","credit_allowed":true,"active":true,"verified":true,"email_verified":true,"state":"active","active_rentals":1,"online":false,"created_at":"2026-06-02T10:00:00Z"},
                {"id":6,"username":"zain","name":"Zain Telecom","email":"zain@x.pk","phone":"","company":"","balance":250,"currency":"USD","tier":"","credit_allowed":false,"active":false,"verified":true,"email_verified":true,"state":"disabled","active_rentals":0,"online":false,"created_at":"2026-05-02T10:00:00Z"}],
                "page":1,"pages":1,"total":4,"counts":{"all":41,"active":38,"pending":2,"disabled":1,"debt":3}}""")
            r.startsWith("/api/v1/admin/resellers/") -> ok(A_RESELLER.dropLast(1) + ""","total_spent":84200,"discount_percent":5,"credit_limit":2000,"last_login_at":"2026-10-01T09:30:00Z","rentals_total":42,"orders_total":9,"twofa":true,"can_money":true,
                "recent_rentals":[$A_RENTAL],"ledger":[{"id":30,"type":"debit","amount":350,"balance_after":4650,"currency":"PKR","description":"Rental UnlockTool (6 Hours)","created_at":"2026-10-01T10:00:00Z"},{"id":29,"type":"credit","amount":5000,"balance_after":5000,"currency":"PKR","description":"Wallet top-up — cash","created_at":"2026-09-29T10:00:00Z"}]}""")
            r == "/api/v1/admin/tools" -> ok("""[{"id":1,"name":"UnlockTool","color":"#22d3ee","image":null,"active":true,"total":6,"busy":4,"free":2,"disabled":0,"expiring":0},
                {"id":2,"name":"Chimera Tool","color":"#a855f7","image":null,"active":true,"total":4,"busy":3,"free":0,"disabled":1,"expiring":1},
                {"id":3,"name":"Griffin Unlocker","color":"#f59e0b","image":null,"active":true,"total":2,"busy":0,"free":2,"disabled":0,"expiring":0}]""")
            r.startsWith("/api/v1/admin/tools/") -> ok("""{"tool":{"id":1,"name":"UnlockTool","color":"#22d3ee","image":null,"active":true},"slots":[
                {"id":1,"name":"Slot A","tool":"UnlockTool","tool_id":1,"status":"busy","username":"shop_user_07","email":"","account_expires_at":"2026-12-30T00:00:00Z","renter":"Ali Khan","rental_id":11,"rental_number":"RNT-261001-AB12","rental_ends_at":"2026-10-01T16:00:00Z","seconds_left":9000,"notes":""},
                {"id":2,"name":"Slot B","tool":"UnlockTool","tool_id":1,"status":"available","username":"shop_user_08","email":"","account_expires_at":"2026-10-05T00:00:00Z","renter":null,"rental_id":null,"seconds_left":0,"notes":""},
                {"id":4,"name":"Slot D","tool":"UnlockTool","tool_id":1,"status":"disabled","username":"shop_user_10","email":"","account_expires_at":null,"renter":null,"rental_id":null,"seconds_left":0,"notes":""}]}""")
            r.startsWith("/api/v1/admin/slots/") && r.endsWith("/secret") -> ok("""{"username":"shop_user_08","email":"","password":"B8-new!Pass"}""")
            r == "/api/v1/app/ping" -> ok("""{"latest_id":${if (admin) 40 else 10},"unread":${if (admin) 2 else 3},"items":[]}""")
            r == "/api/v1/bridge" -> bridge(Regex("\"path\":\"([^\"]*)\"").find(body)?.groupValues?.get(1).orEmpty(), admin)
            else -> MockResponse().setResponseCode(404).setBody("""{"error":"Not found"}""")
        }
    }

    /** The website's own pages, as /api/v1/bridge answers them (3.5). */
    private fun bridge(path: String, admin: Boolean): MockResponse {
        fun page(view: String, data: String) = MockResponse().setHeader("Content-Type", "application/json").setBody("""{"ok":true,"kind":"view","status":200,"view":"$view","title":"","data":$data,"flash":[]}""")
        fun json(data: String) = MockResponse().setHeader("Content-Type", "application/json").setBody("""{"ok":true,"kind":"json","status":200,"json":$data,"flash":[]}""")
        return when (path) {
            "/admin/tools" -> page("admin/tools", """{"tools":[
                {"id":1,"name":"UnlockTool","color":"#22d3ee","is_active":1,"free_slots":2,"busy_slots":4,"total_slots":6,"dur_count":3,"min_price":"150.00","rental_count":"812"},
                {"id":2,"name":"Chimera Tool","color":"#a855f7","is_active":1,"free_slots":0,"busy_slots":3,"total_slots":4,"dur_count":1,"min_price":"500.00","rental_count":"301"},
                {"id":3,"name":"Griffin Unlocker","color":"#f59e0b","is_active":0,"free_slots":2,"busy_slots":0,"total_slots":2,"dur_count":1,"min_price":"1200.00","rental_count":"44"}]}""")
            "/admin/tools/1/slots" -> page("admin/slots", """{"tool":{"id":1,"name":"UnlockTool","color":"#22d3ee"},"slots":[
                {"id":1,"slot_name":"Slot A","username":"shop_user_07","status":"busy","renter_name":"Ali Khan","rental_id":11,"expiry_time":"2026-10-01T16:00:00Z","account_expires_at":"2026-12-30 00:00:00","total_paid":"4500","total_earned":"18450","renew_cost":"4500","renew_days":"30"},
                {"id":2,"slot_name":"Slot B","username":"shop_user_08","status":"available","account_expires_at":"2026-10-05 00:00:00","total_paid":"4500","total_earned":"9100"},
                {"id":4,"slot_name":"Slot D","username":"shop_user_10","status":"disabled","total_paid":"0","total_earned":"0"}]}""")
            "/admin/messages" -> page("admin/messages", """{"resellers":[{"id":2,"username":"ali","full_name":"Ali Khan","phone":"923001112233","company_name":"Ali Mobile Zone"},{"id":5,"username":"usman","full_name":"Usman Mobiles","phone":"","company_name":""}],
                "recent":[{"id":3,"title":"🎉 UnlockTool 20% off today","message":"Rent **6 hours** for the price of 5.","type":"promo","audience":"resellers","recipients":38,"read_count":"21","whatsapp":0,"via":"app","created_at":"2026-10-01T08:00:00Z","recalled_at":null}],
                "tools":[{"id":1,"name":"UnlockTool"},{"id":2,"name":"Chimera Tool"}],"services":[{"id":1,"name":"Xiaomi FRP Remove"}],"teamCount":2,"whatsapp_number":"923001234567"}""")
            "/api/chat/threads" -> json("""[{"id":2,"name":"Ali Khan","username":"ali","online":true,"unread":2,"last":"Slot A login not working, please check","last_time":"10:05 AM"},
                {"id":5,"name":"Usman Mobiles","username":"usman","online":false,"unread":0,"last":"You: Done, try again","last_time":"30 Sep"}]""")
            "/api/chat/messages" -> json("""{"messages":[{"id":1,"mine":false,"body":"Hello, Slot A login not working, please check","time":"10:04 AM","day":"Thu, 01 Oct 2026"},
                {"id":2,"mine":true,"body":"Checking now — one minute.","time":"10:05 AM","day":"Thu, 01 Oct 2026"},
                {"id":3,"mine":false,"body":"Thanks 👍","time":"10:06 AM","day":"Thu, 01 Oct 2026"}],"seen_upto":2,"peer":{"name":"${if (admin) "Ali Khan" else "Support team"}","online":true,"last_seen":"now"}}""")
            "/admin/settings" -> page("admin/settings", """{"info":{"url":"https://abtools.pk","php":"8.3.12","db":"MySQL 10.11","last":1790000000},
                "sections":{"general":[{"key":"system_name","type":"str","value":"AB Tools"},{"key":"timezone","type":"tz","value":"Asia/Karachi"},{"key":"base_currency","type":"cur","value":"PKR"},{"key":"support_whatsapp","type":"str","value":"923001234567"}],
                "rentals":[{"key":"max_active_rentals","type":"int","value":"3"},{"key":"show_slot_count","type":"bool","value":"1"},{"key":"refund_mode","type":"refund","value":"prorata"}]}}""")
            "/admin/website" -> page("admin/site_posts", """{"notPosted":1,"posts":[{"id":1,"section":"tools","title":"UnlockTool","category":"Multi-brand","color":"#22d3ee","link_type":"tool","linked":{"name":"UnlockTool"},"price_mode":"auto","is_active":1},
                {"id":2,"section":"services","title":"Xiaomi FRP Remove","color":"#f97316","link_type":"url","link_url":"https://wa.me/923001234567","price_mode":"custom","price_text":"Rs 500","is_active":1}]}""")
            "/reseller/invoices" -> page("reseller/invoices", """{"rows":[{"id":31,"invoice_number":"INV-261001-0031","tool_name":"UnlockTool","created_at":"2026-10-01T10:00:00Z","payment_status":"paid","total_amount":"350.00","currency":"PKR"},
                {"id":30,"invoice_number":"INV-260930-0030","tool_name":"Chimera Tool","created_at":"2026-09-30T08:00:00Z","payment_status":"paid","total_amount":"500.00","currency":"PKR"}]}""")
            "/profile" -> page("auth/profile", """{"me":{"username":"${if (admin) "admin" else "ali"}","full_name":"${if (admin) "Aamir Buneri" else "Ali Khan"}","email":"ali@shop.pk","phone":"923001112233","company_name":"Ali Mobile Zone","role":"${if (admin) "admin" else "reseller"}","totp_enabled":false,"email_verified":true,"created_at":"2026-03-12T10:00:00Z"},
                "logins":[{"id":1,"success":1,"ip_address":"39.45.10.2","user_agent":"ABGsmRental-Android/1.2.0","created_at":"2026-10-01T09:30:00Z"}]}""")
            else -> MockResponse().setHeader("Content-Type", "application/json").setBody("""{"ok":false,"kind":"abort","status":404,"error":"Not in the fake site: $path","flash":[]}""")
        }
    }

    companion object {
        val ADMIN_TOKEN = "gma_" + "b2".repeat(24)
        const val ADMIN_USER = """{"username":"admin","name":"Aamir Buneri","email":"a@abtools.pk","role":"admin","team":true,"owner":true,"perms":["rentals","tools","services","catalog","resellers","money","chat","website"]}"""
        const val RUNNING = """{"id":1,"number":"RNT-261001-AB12","tool":"UnlockTool","plan":"6 Hours","minutes":360,"price":350,"currency":"PKR","status":"active","started_at":"2026-10-01T10:00:00Z","expires_at":"2026-10-01T16:00:00Z","seconds_left":15125,"login":{"username":"shop_user_07","email":"","password":"Unl0ck!2026"},"tool_id":1,"color":"#22d3ee"}"""
        const val ORDER_OPEN = """{"id":4,"number":"SRV-261001-K9","service":"Xiaomi FRP Remove","status":"in_progress","status_label":"In progress","price":500,"currency":"PKR","paid":true,"device":"Redmi Note 12","result":null,"created_at":"2026-10-01T09:40:00Z","completed_at":null,"service_id":1,"quote":false,"awaiting_acceptance":false,"can_cancel":false,"note":"","remote_app":"anydesk","details":{"Model":"23021RAAEG"},"started_at":"2026-10-01T09:45:00Z","quoted_at":null}"""
        const val ORDER_DONE = """{"id":5,"number":"SRV-260930-P3","service":"Xiaomi FRP Remove","status":"completed","status_label":"Completed","price":500,"currency":"PKR","paid":true,"device":"Poco X5","result":"FRP removed. Phone set up with a new Google account — restart once.","created_at":"2026-09-30T17:40:00Z","completed_at":"2026-09-30T18:00:00Z","service_id":1,"quote":false,"awaiting_acceptance":false,"can_cancel":false,"note":"Customer waiting","remote_app":"anydesk","details":{"Model":"22111317PG","Android version":"13"},"started_at":"2026-09-30T17:45:00Z","quoted_at":null}"""
        const val A_RENTAL = """{"id":11,"number":"RNT-261001-AB12","tool":"UnlockTool","tool_id":1,"slot":"Slot A","plan":"6 Hours","minutes":360,"price":350,"currency":"PKR","status":"active","started_at":"2026-10-01T10:00:00Z","expires_at":"2026-10-01T16:00:00Z","seconds_left":9000,"renter":"Ali Khan","reseller":"ali","reseller_id":2,"walk_in":false,"color":"#22d3ee","image":null}"""
        const val A_ORDER_READY = """{"id":21,"number":"SRV-261001-K9","service":"Xiaomi FRP Remove","status":"pending","status_label":"Pending","price":500,"currency":"PKR","paid":true,"quote":false,"needs_quote":false,"device":"Redmi Note 12","reseller":"Ali Khan","reseller_username":"ali","reseller_id":2,"created_at":"2026-10-01T09:40:00Z","started_at":null,"completed_at":null,"color":"#f97316","image":null,"unread":0}"""
        const val A_ORDER_QUOTE = """{"id":23,"number":"SRV-261001-Q4","service":"Samsung Network Unlock","status":"pending","status_label":"Pending","price":0,"currency":"PKR","paid":false,"quote":true,"needs_quote":true,"device":"Galaxy S23","reseller":"Usman Mobiles","reseller_username":"usman","reseller_id":5,"created_at":"2026-10-01T09:20:00Z","started_at":null,"completed_at":null,"color":"#3b82f6","image":null,"unread":0}"""
        const val A_RESELLER = """{"id":2,"username":"ali","name":"Ali Khan","email":"ali@shop.pk","phone":"923001112233","company":"Ali Mobile Zone","balance":4650,"currency":"PKR","tier":"silver","credit_allowed":true,"active":true,"verified":true,"email_verified":true,"state":"active","active_rentals":1,"online":true,"created_at":"2026-03-12T10:00:00Z"}"""
        const val A_PENDING = """{"id":9,"username":"newshop","name":"Bilal Ahmed","email":"bilal@shop.pk","phone":"923009998877","company":"Bilal Mobiles","balance":0,"currency":"PKR","tier":"","credit_allowed":false,"active":true,"verified":false,"email_verified":true,"state":"pending","active_rentals":0,"online":false,"created_at":"2026-10-01T09:50:00Z"}"""
    }
}

/** Real app against [FakeSite] on the JVM, with screenshots. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class FlowHarness(private val theme: String) {
    protected val themeName: String get() = theme
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val server = MockWebServer()
    protected val site = FakeSite()

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        runCatching {
            WorkManagerTestInitHelper.initializeTestWorkManager(ctx, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
        }
        server.dispatcher = site
        server.start()
        // if a step hangs, print where every thread is (shows up in the CI test log)
        watchdog = Thread {
            try {
                Thread.sleep(150_000)
                while (true) {
                    val dump = buildString {
                        appendLine("=== WATCHDOG ${javaClass.simpleName}: last step '$lastStep', calls ${site.calls.takeLast(8)}")
                        Thread.getAllStackTraces().forEach { (t, st) ->
                            if (st.any { it.className.startsWith("androidx") || it.className.startsWith("com.aamirbuneri") || it.className.startsWith("org.robolectric") }) {
                                appendLine("--- ${t.name} (${t.state})")
                                st.take(45).forEach { appendLine("    at $it") }
                            }
                        }
                    }
                    println(dump)
                    runCatching { java.io.File("build/screens/$theme").mkdirs(); java.io.File("build/screens/$theme/HANG-${javaClass.simpleName}.txt").writeText(dump) }
                    Thread.sleep(60_000)
                }
            } catch (_: InterruptedException) {}
        }.apply { isDaemon = true; start() }
    }

    private var watchdog: Thread? = null
    @Volatile protected var lastStep = "start"

    @After
    fun tearDown() {
        watchdog?.interrupt()
        server.shutdown()
    }

    protected fun shot(name: String) {
        lastStep = "shot $name"
        println("STEP shot $name")
        rule.mainClock.advanceTimeBy(700)
        rule.waitForIdle()
        captureScreenRoboImage("build/screens/$theme/$name.png")
    }

    /** Move the app's clock forward in small steps (animations, splash) while the fake site answers. */
    protected fun waitText(text: String, timeoutMs: Long = 25_000) {
        lastStep = "wait $text"
        println("STEP wait $text")
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            rule.mainClock.advanceTimeBy(100)
            if (rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) return
            Thread.sleep(30)
        }
        throw AssertionError("Timed out waiting for \"$text\". Calls: ${site.calls}")
    }

    protected fun back() {
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.mainClock.advanceTimeBy(600)
    }

    /** First node with this text / tag — logs how many matched instead of a slow full-tree dump on failure. */
    protected fun byText(text: String, substring: Boolean = false): androidx.compose.ui.test.SemanticsNodeInteraction {
        val all = rule.onAllNodesWithText(text, substring = substring)
        val n = all.fetchSemanticsNodes().size
        println("STEP find text '$text' → $n")
        if (n == 0) throw AssertionError("No node with text \"$text\" (last step $lastStep)")
        return all[0]
    }

    protected fun byTag(tag: String): androidx.compose.ui.test.SemanticsNodeInteraction {
        val all = rule.onAllNodesWithTag(tag)
        val n = all.fetchSemanticsNodes().size
        println("STEP find tag '$tag' → $n")
        if (n == 0) throw AssertionError("No node tagged \"$tag\" (last step $lastStep)")
        return all[0]
    }

    /** Tap by calling the node's click action (no gesture, no scrolling — works below the fold). */
    protected fun tapTag(tag: String) = act(rule.onAllNodesWithTag(tag).fetchSemanticsNodes(), "tag $tag")
    protected fun tapText(text: String, substring: Boolean = false) = act(rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes(), "text $text")

    private fun act(nodes: List<androidx.compose.ui.semantics.SemanticsNode>, what: String) {
        lastStep = "tap $what"
        println("STEP tap $what → ${nodes.size}")
        val node = nodes.firstOrNull { it.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick) }
            ?: throw AssertionError("Nothing clickable for $what (${nodes.size} nodes)")
        rule.runOnUiThread { node.config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action?.invoke() }
        rule.mainClock.advanceTimeBy(200)
    }

    protected fun typeTag(tag: String, text: String) {
        lastStep = "type $tag"
        val node = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull { it.config.contains(androidx.compose.ui.semantics.SemanticsActions.SetText) }
            ?: throw AssertionError("No text field tagged $tag")
        rule.runOnUiThread { node.config[androidx.compose.ui.semantics.SemanticsActions.SetText].action?.invoke(androidx.compose.ui.text.AnnotatedString(text)) }
        rule.mainClock.advanceTimeBy(100)
    }

    protected fun tab(label: String) = rule.onNode(hasText(label) and hasClickAction()).performClick()

    /** Splash → site address → sign-in screen. */
    protected fun connect(shots: Boolean) {
        // the loading shimmer and the countdown tick forever: drive the clock by hand
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(300)
        if (shots) shot("00-splash")
        waitText("Connect to your panel")
        if (shots) shot("01-setup")
        rule.onNodeWithTag("site").performTextReplacement(server.url("/").toString().trimEnd('/'))
        rule.onNodeWithText("Continue").performClick()
        waitText("Welcome back")
        waitText("Create account") // app/info arrived
    }

    protected fun signIn(user: String, pass: String) {
        rule.onNodeWithTag("username").performTextReplacement(user)
        rule.onNodeWithTag("password").performTextReplacement(pass)
        rule.onNode(hasText("Sign in") and hasClickAction()).performClick()
    }
}

abstract class ResellerFlow(theme: String) : FlowHarness(theme) {
    @Test
    fun fullFlow() {
        connect(shots = true)
        shot("02-login")
        signIn("ali", "Reseller@123")
        waitText("Wallet balance")
        shot("03-home")

        tab("Rent")
        waitText("Griffin Unlocker")
        shot("04-rent")
        rule.onNodeWithText("UnlockTool").performClick()
        waitText("Choose a plan")
        rule.onNodeWithText("6 Hours").performClick()
        shot("05-rent-sheet")
        rule.onNodeWithText("Rent now", substring = true).performClick()
        waitText("is ready")
        shot("06-rent-done")
        rule.onNodeWithText("Done").performClick()

        tab("Rentals")
        waitText("Show login")
        shot("07-rentals")
        rule.onNodeWithText("Show login").performClick()
        waitText("Tool login")
        shot("08-rental-detail")
        back()

        tab("Services")
        waitText("Xiaomi FRP Remove")
        shot("09-services")
        rule.onNodeWithText("Xiaomi FRP Remove").performClick()
        waitText("Remote access")
        shot("10-order-form")
        back()
        rule.onNodeWithText("My orders").performClick()
        waitText("Poco X5")
        shot("11-my-orders")
        rule.onNodeWithText("Poco X5", substring = true).performClick()
        waitText("FRP removed")
        shot("12-order-detail")
        back()

        tab("Account")
        waitText("Theme")
        shot("13-account")
        rule.onNodeWithText("Wallet & history").performClick()
        waitText("Wallet top-up")
        shot("14-wallet")
        back()
        rule.onNodeWithText("Notifications").performClick()
        waitText("Tool ready: UnlockTool")
        shot("15-notifications")
        back()
        tapText("Invoices")
        waitText("INV-261001-0031")
        shot("16-invoices")
        back()
        tapText("Chat with support")
        waitText("Checking now")
        shot("17-chat")
        back()
        tapText("Profile & security")
        waitText("Two-step sign-in")
        shot("18-profile")

        assertTrue("app talked to the site: ${site.calls}", site.calls.any { it.contains("/api/v1/dashboard") })
    }
}

abstract class SignUpFlow(theme: String) : FlowHarness(theme) {
    @Test
    fun signUp() {
        connect(shots = false)
        // forgot password, inside the app
        rule.onNodeWithTag("forgot").performClick()
        waitText("Forgot your password?")
        rule.onNodeWithTag("forgot_email").performTextReplacement("bilal@shop.pk")
        rule.onNode(hasText("Send reset link") and hasClickAction()).performClick()
        waitText("reset link is on its way")
        shot("20-forgot-sent")
        rule.onNode(hasText("Back to sign in") and hasClickAction()).performClick()
        waitText("Welcome back")

        // create an account
        rule.onNodeWithTag("register").performClick()
        waitText("Create your account")
        rule.onNodeWithTag("reg_name").performTextReplacement("Bilal Ahmed")
        rule.onNodeWithTag("reg_username").performTextReplacement("newshop")
        rule.onNodeWithTag("reg_email").performTextReplacement("bilal@shop.pk")
        rule.onNodeWithTag("reg_phone").performTextReplacement("923009998877")
        rule.onNodeWithTag("reg_company").performTextReplacement("Bilal Mobiles")
        shot("21-register")
        tapTag("cur_USD")
        typeTag("reg_password", "Secret@123")
        typeTag("reg_password2", "Secret@123")
        shot("22-register-filled")
        tapTag("reg_submit")
        waitText("Waiting for approval")
        shot("23-waiting-approval")
        assertTrue("sent the form: ${site.bodies}", site.bodies.any { it.startsWith("/api/v1/auth/register") && it.contains("\"currency\":\"USD\"") && it.contains("\"username\":\"newshop\"") && it.contains("\"company\":\"Bilal Mobiles\"") })

        // not yet → still waiting; approved → straight in, no password again
        tapTag("check_again")
        waitText("Not approved yet")
        site.approved = true
        tapTag("check_again")
        waitText("Wallet balance")
        shot("24-signed-in-after-approval")
    }
}

abstract class AdminFlow(theme: String) : FlowHarness(theme) {
    @Test
    fun admin() {
        connect(shots = false)
        signIn("admin", "Admin@12345")
        waitText("Sales today")
        waitText("Needs attention")
        shot("30-admin-home")

        tab("Orders")
        waitText("Samsung Network Unlock")
        shot("31-admin-orders")
        tapText("Redmi Note 12", substring = true)
        waitText("Start work")
        shot("32-admin-order")
        back()

        tab("Resellers")
        waitText("Bilal Ahmed")
        shot("33-admin-resellers")
        tapText("Ali Khan")
        waitText("Wallet history")
        shot("34-admin-reseller")
        // dialogs don't settle with the hand-driven clock in Robolectric: let it run while one is open
        rule.mainClock.autoAdvance = true
        try {
            tapText("Add money")
            waitText("Balance now")
            rule.waitForIdle()
            captureScreenRoboImage("build/screens/$themeName/35-admin-topup.png")
        } catch (e: Throwable) {
            println("STEP dialog screenshot skipped: $e")
        } finally {
            runCatching { tapText("Cancel") }
            rule.mainClock.autoAdvance = false
        }
        back()

        tab("Rentals")
        waitText("Usman Mobiles")
        shot("36-admin-rentals")
        tapText("Ali Khan", substring = true)
        waitText("Renter’s login")
        shot("37-admin-rental")
        back()

        tab("More")
        waitText("Tools, plans & slots")
        shot("38-admin-more")
        tapText("Tools, plans & slots")
        waitText("Griffin Unlocker")
        shot("39-admin-tools")
        tapText("UnlockTool")
        waitText("Slot B")
        shot("40-admin-slots")
        back()
        back()
        waitText("Notifications")
        tapText("Notifications")
        waitText("New registration")
        shot("41-admin-notifications")
        back()
        waitText("Send a message")
        tapText("Send a message")
        waitText("Preview")
        shot("42-admin-compose")
        back()
        tapText("Chat with resellers")
        waitText("Slot A login not working")
        shot("43-admin-chats")
        tapText("Ali Khan")
        waitText("Checking now")
        shot("44-admin-chat")
        back()
        back()
        tapText("Settings")
        waitText("Run the expiry check now")
        shot("45-admin-settings")
        tapText("General")
        waitText("System name")
        shot("46-admin-settings-general")
        back()
        back()
        tapText("Website posts")
        waitText("Xiaomi FRP Remove")
        shot("47-admin-website")

        assertTrue("admin endpoints used: ${site.calls}", site.calls.any { it.contains("/api/v1/admin/dashboard") } && site.calls.none { it == "GET /api/v1/dashboard" })
    }
}

@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppFlowLightTest : ResellerFlow("light")

@Config(sdk = [34], qualifiers = "w411dp-h891dp-night-xxhdpi")
class AppFlowDarkTest : ResellerFlow("dark")

@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SignUpLightTest : SignUpFlow("light")

@Config(sdk = [34], qualifiers = "w411dp-h891dp-night-xxhdpi")
class SignUpDarkTest : SignUpFlow("dark")

@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AdminLightTest : AdminFlow("light")

@Config(sdk = [34], qualifiers = "w411dp-h891dp-night-xxhdpi")
class AdminDarkTest : AdminFlow("dark")
