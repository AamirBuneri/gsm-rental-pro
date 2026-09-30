@file:OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)

package com.aamirbuneri.abgsmrental

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

/** A fake GSM Rental Pro site: answers /index.php?r=/api/v1/… like the real one (3.3.0). */
class FakeSite : Dispatcher() {
    val calls = CopyOnWriteArrayList<String>()
    private var rented = false

    override fun dispatch(request: RecordedRequest): MockResponse {
        val r = request.requestUrl?.queryParameter("r").orEmpty()
        val m = request.method.orEmpty()
        calls += "$m $r"
        val authed = request.getHeader("X-API-Key")?.startsWith("gma_") == true
        fun ok(data: String) = MockResponse().setHeader("Content-Type", "application/json").setBody("""{"ok":true,"data":$data}""")
        if (r != "/api/v1/app/info" && !r.startsWith("/api/v1/auth/") && !authed) {
            return MockResponse().setResponseCode(401).setBody("""{"ok":false,"error":"Your session ended. Please sign in again."}""")
        }
        return when {
            r == "/api/v1/app/info" -> ok("""{"app":"gsm-rental-pro","api_version":1,"version":"3.3.0","name":"AB Tools","tagline":"GSM tool rental","currency":"PKR","app_enabled":true,"maintenance":false,"services":true,"registration":true,"website":"http://x/","register_url":"http://x/register","forgot_url":"http://x/forgot","support":{"whatsapp":"923001234567","phone":"+92 300 1234567","email":"help@abtools.pk"}}""")
            r == "/api/v1/auth/login" -> ok("""{"need_2fa":false,"token":"gma_${"a1".repeat(24)}","user":{"username":"ali","name":"Ali Khan","email":"ali@shop.pk"}}""")
            r == "/api/v1/dashboard" -> ok("""{"account":{"username":"ali","name":"Ali Khan","email":"ali@shop.pk","balance":4650,"currency":"PKR","credit_allowed":true,"credit_limit":2000,"can_spend":6650,"discount_percent":5},
                "active_rentals":[$RUNNING],"open_orders":[$ORDER_OPEN],"unread":3,
                "stats":{"rentals_total":42,"month_rentals":12,"month_spent":8400},"services":true,"server_time":"2026-10-01T10:00:00Z"}""")
            r == "/api/v1/tools" -> ok("""[
                {"id":1,"name":"UnlockTool","description":"All-in-one tool for Qualcomm, MTK and Samsung — FRP, flash, IMEI-free repairs.","color":"#22d3ee","available":true,"free_slots":3,"plans":[{"id":1,"label":"2 Hours","minutes":120,"price":150,"currency":"PKR"},{"id":2,"label":"6 Hours","minutes":360,"price":350,"currency":"PKR"},{"id":3,"label":"24 Hours","minutes":1440,"price":900,"currency":"PKR"}]},
                {"id":2,"name":"Chimera Tool","description":"Samsung, Huawei, LG and Xiaomi service tool.","color":"#a855f7","available":false,"next_free_at":"2026-10-01T16:30:00Z","plans":[{"id":4,"label":"4 Hours","minutes":240,"price":500,"currency":"PKR"}]},
                {"id":3,"name":"Griffin Unlocker","description":"iPhone and Samsung unlocking.","color":"#f59e0b","available":true,"free_slots":1,"plans":[{"id":5,"label":"1 Day","minutes":1440,"price":1200,"currency":"PKR"}]}]""")
            r == "/api/v1/rentals" && m == "POST" -> { rented = true; ok(RUNNING.dropLast(1) + ",\"balance\":4300}") }
            r == "/api/v1/rentals" -> ok("""[$RUNNING,{"id":7,"number":"RNT-260930-X1","tool":"Chimera Tool","plan":"4 Hours","minutes":240,"price":500,"currency":"PKR","status":"expired","started_at":"2026-09-30T08:00:00Z","expires_at":"2026-09-30T12:00:00Z","seconds_left":0,"tool_id":2,"color":"#a855f7"},{"id":6,"number":"RNT-260929-Q2","tool":"UnlockTool","plan":"2 Hours","minutes":120,"price":150,"currency":"PKR","status":"returned","started_at":"2026-09-29T08:00:00Z","expires_at":"2026-09-29T10:00:00Z","seconds_left":0,"tool_id":1,"color":"#22d3ee"}]""")
            r.startsWith("/api/v1/rentals/") -> ok(RUNNING)
            r == "/api/v1/services" -> ok("""[
                {"id":1,"name":"Xiaomi FRP Remove","category":"Xiaomi","description":"Google account lock removal for all Xiaomi / Redmi / Poco.","color":"#f97316","price":500,"currency":"PKR","quote":false,"eta_minutes":20,"needs_remote":true,"fields":[{"label":"Model","required":true},{"label":"Android version","required":false}]},
                {"id":2,"name":"Samsung Network Unlock","category":"Samsung","description":"Carrier unlock.","color":"#3b82f6","price":null,"currency":"PKR","quote":true,"eta_minutes":60,"needs_remote":false,"fields":[]},
                {"id":3,"name":"iCloud Info Check","category":"Apple","description":"","color":"#64748b","price":150,"currency":"PKR","quote":false,"eta_minutes":5,"needs_remote":false,"fields":[{"label":"IMEI / Serial","required":true}]}]""")
            r == "/api/v1/orders" -> ok("""[$ORDER_OPEN,$ORDER_DONE]""")
            r.startsWith("/api/v1/orders/") -> ok(ORDER_DONE)
            r == "/api/v1/notifications" -> ok("""{"unread":2,"items":[
                {"id":9,"title":"Tool ready: UnlockTool","message":"Your rental RNT-261001-AB12 is active until 4:00 PM.","type":"success","read":false,"rental_id":1,"link":"/reseller/rentals","created_at":"2026-10-01T09:58:00Z"},
                {"id":8,"title":"Order completed","message":"Xiaomi FRP Remove — done.","type":"info","read":false,"rental_id":null,"link":"/reseller/services/orders/5","created_at":"2026-09-30T18:00:00Z"},
                {"id":7,"title":"Wallet topped up","message":"Rs 5,000 added to your wallet.","type":"success","read":true,"rental_id":null,"link":"/reseller/wallet","created_at":"2026-09-29T10:00:00Z"}]}""")
            r == "/api/v1/notifications/read" -> ok("""{"unread":0}""")
            r == "/api/v1/wallet" -> ok("""{"balance":4650,"currency":"PKR","credit_allowed":true,"credit_limit":2000,"can_spend":6650,"topup_whatsapp":"923001234567","items":[
                {"id":30,"type":"debit","amount":350,"balance_after":4650,"currency":"PKR","description":"Rental UnlockTool (6 Hours)","reference_type":"rental","reference_id":1,"created_at":"2026-10-01T10:00:00Z"},
                {"id":29,"type":"credit","amount":5000,"balance_after":5000,"currency":"PKR","description":"Wallet top-up","reference_type":"topup","reference_id":null,"created_at":"2026-09-29T10:00:00Z"},
                {"id":28,"type":"credit","amount":500,"balance_after":0,"currency":"PKR","description":"Refund: SRV-260928-A1 (Xiaomi FRP Remove)","reference_type":"service","reference_id":4,"created_at":"2026-09-28T10:00:00Z"}]}""")
            r == "/api/v1/auth/logout" -> ok("""{"signed_out":true}""")
            else -> MockResponse().setResponseCode(404).setBody("""{"ok":false,"error":"Unknown endpoint $r"}""")
        }
    }

    companion object {
        const val RUNNING = """{"id":1,"number":"RNT-261001-AB12","tool":"UnlockTool","plan":"6 Hours","minutes":360,"price":350,"currency":"PKR","status":"active","started_at":"2026-10-01T10:00:00Z","expires_at":"2026-10-01T16:00:00Z","seconds_left":15125,"login":{"username":"shop_user_07","email":"","password":"Unl0ck!2026"},"tool_id":1,"color":"#22d3ee"}"""
        const val ORDER_OPEN = """{"id":4,"number":"SRV-261001-K9","service":"Xiaomi FRP Remove","status":"in_progress","status_label":"In progress","price":500,"currency":"PKR","paid":true,"device":"Redmi Note 12","result":null,"created_at":"2026-10-01T09:40:00Z","completed_at":null,"service_id":1,"quote":false,"awaiting_acceptance":false,"can_cancel":false,"note":"","remote_app":"anydesk","details":{"Model":"23021RAAEG"},"started_at":"2026-10-01T09:45:00Z","quoted_at":null}"""
        const val ORDER_DONE = """{"id":5,"number":"SRV-260930-P3","service":"Xiaomi FRP Remove","status":"completed","status_label":"Completed","price":500,"currency":"PKR","paid":true,"device":"Poco X5","result":"FRP removed. Phone set up with a new Google account — restart once.","created_at":"2026-09-30T17:40:00Z","completed_at":"2026-09-30T18:00:00Z","service_id":1,"quote":false,"awaiting_acceptance":false,"can_cancel":false,"note":"Customer waiting","remote_app":"anydesk","details":{"Model":"22111317PG","Android version":"13"},"started_at":"2026-09-30T17:45:00Z","quoted_at":null}"""
    }
}


@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class AppFlowBase(private val theme: String) {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val server = MockWebServer()
    private val site = FakeSite()

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        runCatching {
            WorkManagerTestInitHelper.initializeTestWorkManager(ctx, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
        }
        server.dispatcher = site
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun shot(name: String) {
        rule.mainClock.advanceTimeBy(700)
        rule.waitForIdle()
        captureScreenRoboImage("build/screens/$theme/$name.png")
    }

    /** Move the app's clock forward in small steps (animations, splash) while the fake site answers. */
    private fun waitText(text: String, timeoutMs: Long = 25_000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            rule.mainClock.advanceTimeBy(100)
            if (rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) return
            Thread.sleep(30)
        }
        throw AssertionError("Timed out waiting for \"$text\". Calls: ${site.calls}")
    }

    private fun back() {
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.mainClock.advanceTimeBy(600)
    }

    @Test
    fun fullFlow() {
        // the loading shimmer and the countdown tick forever: drive the clock by hand
        rule.mainClock.autoAdvance = false
        // brand splash
        rule.mainClock.advanceTimeBy(300)
        shot("00-splash")
        // splash → setup
        waitText("Connect to your panel")
        shot("01-setup")

        rule.onNodeWithTag("site").performTextReplacement(server.url("/").toString().trimEnd('/'))
        rule.onNodeWithText("Continue").performClick()
        waitText("Welcome back")
        shot("02-login")

        rule.onNodeWithTag("username").performTextReplacement("ali")
        rule.onNodeWithTag("password").performTextReplacement("Reseller@123")
        rule.onNodeWithText("Sign in").performClick()
        waitText("Wallet balance")
        shot("03-home")

        // Rent tab + plan sheet + rent
        rule.onNode(hasText("Rent") and hasClickLabelOrTab()).performClick()
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

        // Rentals tab + detail
        rule.onNode(hasText("Rentals") and hasClickLabelOrTab()).performClick()
        waitText("Show login")
        shot("07-rentals")
        rule.onNodeWithText("Show login").performClick()
        waitText("Tool login")
        shot("08-rental-detail")
        back()

        // Services + order form + my orders + order detail
        rule.onNode(hasText("Services") and hasClickLabelOrTab()).performClick()
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

        // Account, wallet, notifications
        rule.onNode(hasText("Account") and hasClickLabelOrTab()).performClick()
        waitText("Appearance")
        shot("13-account")
        rule.onNodeWithText("Wallet & history").performClick()
        waitText("Wallet top-up")
        shot("14-wallet")
        back()
        rule.onNodeWithText("Notifications").performClick()
        waitText("Tool ready: UnlockTool")
        shot("15-notifications")

        assertTrue("app talked to the site: ${site.calls}", site.calls.any { it.contains("/api/v1/dashboard") })
    }
}

/** Bottom-bar items (not headings with the same word). */
private fun hasClickLabelOrTab() = androidx.compose.ui.test.hasClickAction()

@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppFlowLightTest : AppFlowBase("light")

@Config(sdk = [34], qualifiers = "w411dp-h891dp-night-xxhdpi")
class AppFlowDarkTest : AppFlowBase("dark")
