package com.aamirbuneri.abgsmrental

import android.app.Application
import android.content.Context
import com.aamirbuneri.abgsmrental.data.Api
import com.aamirbuneri.abgsmrental.data.Prefs
import com.aamirbuneri.abgsmrental.work.Notifier
import com.aamirbuneri.abgsmrental.work.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class App : Application() {
    lateinit var container: Container
        private set

    override fun onCreate() {
        super.onCreate()
        container = Container(this)
        Notifier.createChannels(this)
        container.scope.launch {
            if (container.prefs.snapshot().signedIn) SyncWorker.schedule(this@App)
        }
    }
}

/** Everything the screens share. */
class Container(val context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val prefs = Prefs(context)

    private val _signedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Unread notifications (updated by the background checks too). */
    val unread = MutableStateFlow(0)

    /** Where a tapped notification wants to go: a site path ("/reseller/tools/3"), a web link, or "notice:<id>". */
    val openLink = MutableStateFlow<String?>(null)

    /** Fires when the site rejects the saved token (password changed, admin signed the phone out …). */
    val signedOut: SharedFlow<Unit> = _signedOut

    val api = Api(prefs) {
        if (prefs.snapshot().signedIn) {
            prefs.signOut()
            SyncWorker.cancel(context)
            _signedOut.tryEmit(Unit)
        }
    }

    suspend fun signOut() {
        api.logout()
        prefs.signOut()
        SyncWorker.cancel(context)
        Notifier.cancelAll(context)
    }
}

val Context.container: Container get() = (applicationContext as App).container
