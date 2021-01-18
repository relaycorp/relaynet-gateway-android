package tech.relaycorp.gateway.background

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.ResponseException
import io.ktor.client.features.UserAgent
import io.ktor.client.request.head
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import tech.relaycorp.gateway.BuildConfig
import tech.relaycorp.gateway.common.Logging.logger
import java.io.IOException
import java.util.logging.Level
import javax.inject.Inject

class PingRemoteServer
@Inject constructor() {

    private val ktorClient by lazy {
        HttpClient(Android) {
            install(UserAgent) {
                agent = "Relaynet Private Gateway/${BuildConfig.VERSION_NAME} (Android)"
            }
        }
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

    suspend fun pingURL(url: String) =
        try {
            ktorClient.head<Unit>(url)
            true
        } catch (e: IOException) {
            logger.log(Level.INFO, "Could not ping $url (${e.message})")
            false
        } catch (e: ResponseException) {
            logger.log(
                Level.INFO,
                "Successfully pinged $url but got a response exception (${e.message})"
            )
            true
        }
}
