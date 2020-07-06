package tech.relaycorp.gateway.domain.courier

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tech.relaycorp.gateway.background.CourierConnectionObserver
import tech.relaycorp.gateway.background.CourierConnectionState
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.repos.CargoRepository
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import java.util.logging.Level
import javax.inject.Inject

class CargoCollection
@Inject constructor(
    private val clientBuilder: CogRPCClient.Builder,
    private val courierConnectionObserver: CourierConnectionObserver,
    private val generateCCA: GenerateCCA,
    private val cargoRepository: CargoRepository,
    private val processCargo: ProcessCargo
) {

    @Throws(Disconnected::class)
    suspend fun collect() {
        val client =
            getCourierAddress()?.let { clientBuilder.build(it) } ?: throw Disconnected()

        try {
            client
                .collectCargo { generateCCAInputStream() }
                .collect { cargoRepository.store(it) }
        } catch (e: CogRPCClient.CCARefusedException) {
            logger.log(Level.WARNING, "CCA refused")
            return
        } finally {
            client.close()
        }

        processCargo.process()
    }

    private fun generateCCAInputStream() = generateCCA.generateByteArray().inputStream()

    private suspend fun getCourierAddress() =
        courierConnectionObserver
            .observe()
            .map { it as? CourierConnectionState.ConnectedWithCourier }
            .first()
            ?.address

    class Disconnected : Exception()
}
