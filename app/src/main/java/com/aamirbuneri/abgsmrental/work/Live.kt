package com.aamirbuneri.abgsmrental.work

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.aamirbuneri.abgsmrental.R
import com.aamirbuneri.abgsmrental.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps listening for new alerts while the app is closed (no Firebase needed): checks the site every
 * ~25 s while the screen is on and ~60 s when it is off. Android requires a small ongoing notification for this.
 * If the phone deep-sleeps, the wake-up alarm ([AlertAlarmReceiver]) still checks about every minute
 * (Android may stretch that to several minutes on a phone left idle for a long time).
 */
class LiveService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = ongoing(this)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(this, ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(ID, n)
            }
        }.onFailure { stopSelf(); return START_NOT_STICKY }
        if (loop?.isActive != true) {
            loop = scope.launch {
                val power = getSystemService(PowerManager::class.java)
                while (isActive) {
                    val lock = power?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "abgsm:alerts")?.apply { setReferenceCounted(false); acquire(30_000L) }
                    val keepGoing = try { AlertCheck.run(this@LiveService) } finally { runCatching { lock?.release() } }
                    if (!keepGoing) { stopSelf(); break }
                    LiveAlerts.scheduleAlarm(this@LiveService)
                    delay(if (power?.isInteractive == true) 25_000L else 60_000L)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ID = 7100

        fun ongoing(context: Context): Notification =
            NotificationCompat.Builder(context, Notifier.CH_LIVE)
                .setSmallIcon(R.drawable.ic_stat_ab)
                .setColor(ContextCompat.getColor(context, R.color.notification_accent))
                .setContentTitle("Live alerts on")
                .setContentText("You’ll get new orders, messages and offers even when the app is closed.")
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(Notifier.openIntent(context, ID))
                .build()
    }
}

/** Wakes the phone about once a minute to check, even in deep sleep; schedules the next one. */
class AlertAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        app.container.scope.launch {
            val power = app.getSystemService(PowerManager::class.java)
            val lock = power?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "abgsm:alarm")?.apply { setReferenceCounted(false); acquire(25_000L) }
            try {
                val s = app.container.prefs.snapshot()
                if (s.signedIn && s.notifications && s.liveAlerts) {
                    AlertCheck.run(app)
                    LiveAlerts.scheduleAlarm(app)
                }
            } finally {
                runCatching { lock?.release() }
                pending.finish()
            }
        }
    }
}

/** After a restart or an app update: start listening again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        val app = context.applicationContext
        app.container.scope.launch {
            try { LiveAlerts.ensure(app) } finally { pending.finish() }
        }
    }
}
