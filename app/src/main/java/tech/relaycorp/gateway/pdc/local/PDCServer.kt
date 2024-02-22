package tech.relaycorp.gateway.pdc.local

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.withContext
import tech.relaycorp.gateway.pdc.local.routes.EndpointRegistrationRoute
import tech.relaycorp.gateway.pdc.local.routes.PDCServerRoute
import tech.relaycorp.gateway.pdc.local.routes.ParcelCollectionRoute
import tech.relaycorp.gateway.pdc.local.routes.ParcelDeliveryRoute
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class PDCServer
@Inject constructor(
    private val stateManager: PDCServerStateManager,
    endpointRegistrationRoute: EndpointRegistrationRoute,
    private val parcelCollectionRoute: ParcelCollectionRoute,
    parcelDeliveryRoute: ParcelDeliveryRoute,
    private val backgroundContext: CoroutineContext,
) {

    private val server by lazy {
        embeddedServer(Netty, PORT, "127.0.0.1", watchPaths = emptyList()) {
            PDCServerConfiguration.configure(
                this,
                listOf(
                    endpointRegistrationRoute,
                    parcelCollectionRoute,
                    parcelDeliveryRoute,
                ),
            )
        }
    }

    suspend fun start() {
        withContext(backgroundContext) {
            server.start(false)
        }
        stateManager.set(State.Started)
    }

    suspend fun stop() {
        withContext(backgroundContext) {
            parcelCollectionRoute.stop()
            server.stop(1000, CALL_DEADLINE.inWholeMilliseconds)
        }
        stateManager.set(State.Stopped)
    }

    enum class State {
        Started,
        Stopped,
    }

    companion object {
        const val PORT = 13276
        private val CALL_DEADLINE = 5.seconds
    }
}

object PDCServerConfiguration {
    fun configure(serverApp: Application, routes: List<PDCServerRoute>) {
        with(serverApp) {
            install(WebSockets)

            routing {
                routes.iterator().forEach { it.register(this) }
            }
        }
    }
}
