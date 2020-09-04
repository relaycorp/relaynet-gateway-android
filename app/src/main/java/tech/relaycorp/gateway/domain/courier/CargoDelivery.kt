package tech.relaycorp.gateway.domain.courier

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import tech.relaycorp.cogrpc.okhttp.OkHTTPChannelBuilderProvider
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.relaynet.CargoDeliveryRequest
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import java.util.UUID
import javax.inject.Inject

class CargoDelivery
@Inject constructor(
    private val clientBuilder: CogRPCClient.Builder,
    private val connectionStateObserver: ConnectionStateObserver,
    private val generateCargo: GenerateCargo
) {

    suspend fun deliver() {
        val client =
            getCourierAddress()?.let {
                clientBuilder.build(it, OkHTTPChannelBuilderProvider.Companion::makeBuilder)
            } ?: throw Disconnected()

        try {
            client
                .deliverCargo(generateCargoDeliveries().toList())
                .collect()
        } finally {
            client.close()
        }
    }

    private suspend fun generateCargoDeliveries() =
        generateCargo.generate()
            .map { CargoDeliveryRequest(UUID.randomUUID().toString()) { it } }

    private suspend fun getCourierAddress() =
        connectionStateObserver
            .observe()
            .map { it as? ConnectionState.WiFiWithCourier }
            .first()
            ?.courierAddress

    class Disconnected : Exception()
}
