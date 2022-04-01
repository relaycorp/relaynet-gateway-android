package tech.relaycorp.gateway.domain.courier

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import tech.relaycorp.cogrpc.okhttp.OkHTTPChannelBuilderProvider
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.disk.CargoStorage
import tech.relaycorp.gateway.domain.endpoint.IncomingParcelNotifier
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import java.util.logging.Level
import javax.inject.Inject

class CargoCollection
@Inject constructor(
    private val clientBuilder: CogRPCClient.Builder,
    private val connectionStateObserver: ConnectionStateObserver,
    private val notifyEndpoints: IncomingParcelNotifier,
    private val generateCCA: GenerateCCA,
    private val cargoStorage: CargoStorage,
    private val processCargo: ProcessCargo
) {

    @Throws(Disconnected::class)
    suspend fun collect() {
        val client = getCourierAddress()?.let {
            clientBuilder.build(it, OkHTTPChannelBuilderProvider.Companion::makeBuilder)
        } ?: throw Disconnected()

        try {
            client
                .collectCargo { generateCCAInputStream() }
                .collect { cargoStorage.store(it) }
        } catch (e: CogRPCClient.CCARefusedException) {
            logger.log(Level.WARNING, "CCA refused")
            return
        } finally {
            client.close()
        }

        processCargo.process()
        notifyEndpoints.notifyAllPending()
    }

    private fun generateCCAInputStream() = runBlocking {
        generateCCA.generateSerialized().inputStream()
    }

    private suspend fun getCourierAddress() =
        connectionStateObserver
            .observe()
            .map { it as? ConnectionState.WiFiWithCourier }
            .first()
            ?.courierAddress

    class Disconnected : Exception()
}
