package tech.relaycorp.gateway.domain.courier

import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.disk.CargoStorage
import tech.relaycorp.relaynet.cogrpc.client.ChannelBuilderProvider
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import java.security.SecureRandom
import java.util.logging.Level
import javax.inject.Inject
import javax.net.ssl.SSLContext

class CargoCollection
@Inject constructor(
    private val clientBuilder: CogRPCClient.Builder,
    private val connectionStateObserver: ConnectionStateObserver,
    private val generateCCA: GenerateCCA,
    private val cargoStorage: CargoStorage,
    private val processCargo: ProcessCargo,
    private val processParcels: ProcessParcels
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

    @Throws(Disconnected::class)
    suspend fun collect() {
        val client =
            getCourierAddress()?.let { clientBuilder.build(it, grpcProvider) } ?: throw Disconnected()

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
        processParcels.process()
    }

    private fun generateCCAInputStream() = runBlocking {
        generateCCA.generateByteArray().inputStream()
    }

    private suspend fun getCourierAddress() =
        connectionStateObserver
            .observe()
            .map { it as? ConnectionState.WiFiWithCourier }
            .first()
            ?.courierAddress

    class Disconnected : Exception()
}
