package tech.relaycorp.gateway.background

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.head
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import tech.relaycorp.gateway.common.Logging.logger
import java.io.IOException
import java.util.logging.Level
import javax.inject.Inject

class PingRemoteServer
@Inject constructor() {

    private val ktorClient by lazy {
        HttpClient(Android)
    }

    suspend fun pingSocket(address: String, port: Int) =
        try {
            aSocket(ActorSelectorManager(Dispatchers.IO))
                .tcp()
                .connect(address, port)
                .use { true }
        } catch (e: IOException) {
            logger.log(Level.INFO, "Could not ping $address:$port")
            false
        }

    suspend fun pingHostname(hostname: String) =
        try {
            ktorClient.head<Unit>(hostname)
            true
        } catch (e: IOException) {
            logger.log(Level.INFO, "Could not ping $hostname", e)
            false
        }
}
