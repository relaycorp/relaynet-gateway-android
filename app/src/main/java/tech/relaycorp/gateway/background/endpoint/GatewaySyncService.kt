package tech.relaycorp.gateway.background.endpoint

import android.app.Service
import android.content.Intent
import android.os.Binder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import tech.relaycorp.gateway.background.component
import tech.relaycorp.gateway.pdc.local.PDCServer
import javax.inject.Inject

class GatewaySyncService : Service() {

    private val scope = CoroutineScope(SupervisorJob())

    @Inject
    lateinit var pdcServer: PDCServer

    override fun onBind(intent: Intent?): Binder {
        component.inject(this)
        // Wait for the server to start
        runBlocking {
            withContext(scope.coroutineContext) {
                pdcServer.start()
            }
        }
        return Binder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        scope.launch {
            pdcServer.stop()
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
