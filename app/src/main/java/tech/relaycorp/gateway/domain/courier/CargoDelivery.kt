package tech.relaycorp.gateway.domain.courier

import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.relaynet.CargoDeliveryRequest
import tech.relaycorp.relaynet.cogrpc.client.ChannelBuilderProvider
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.net.ssl.SSLContext

class CargoDelivery
@Inject constructor(
    private val clientBuilder: CogRPCClient.Builder,
    private val connectionStateObserver: ConnectionStateObserver,
    private val generateCargo: GenerateCargo
) {
    private val grpcProvider : ChannelBuilderProvider<OkHttpChannelBuilder> = { address, trustManager ->
        OkHttpChannelBuilder.forAddress(address.hostName, address.port)
            .let {
                if (trustManager != null) {
                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, arrayOf(trustManager), SecureRandom())
                    it.sslSocketFactory(sslContext.socketFactory)
                } else {
                    it
                }
            }
    }

    suspend fun deliver() {
        val client =
            getCourierAddress()?.let { clientBuilder.build(it, grpcProvider) } ?: throw Disconnected()

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
