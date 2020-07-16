package tech.relaycorp.gateway.background.endpoint

import android.app.Service
import android.content.Intent
import android.os.Binder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import tech.relaycorp.gateway.background.component
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.domain.endpoint.EndpointServer
import javax.inject.Inject

class GatewaySyncService : Service() {

    private val scope get() = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Inject
    lateinit var endpointServer: EndpointServer

    override fun onBind(intent: Intent?): Binder {
        component.inject(this)
        logger.info("GatewaySyncService onBind")
        scope.launch {
            endpointServer.start()
        }
        return Binder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        scope.launch {
            endpointServer.stop()
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
