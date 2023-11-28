package tech.relaycorp.gateway.background

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.ResponseException
import io.ktor.client.features.UserAgent
import io.ktor.client.request.head
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import tech.relaycorp.gateway.BuildConfig
import tech.relaycorp.gateway.common.Logging.logger
import java.io.IOException
import java.util.logging.Level
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class PingRemoteServer
@Inject constructor() {

    private val ktorClient by lazy {
        HttpClient(Android) {
            install(UserAgent) {
                agent = "Awala Private Gateway/${BuildConfig.VERSION_NAME} (Android)"
            }
        }
    }

    suspend fun pingSocket(address: String, port: Int) = try {
        aSocket(ActorSelectorManager(Dispatchers.IO))
            .tcp()
            .connect(address, port) {
                socketTimeout = TIMEOUT.inWholeMilliseconds
            }
            .use { true }
    } catch (e: IOException) {
        logger.log(Level.INFO, "Could not ping $address:$port")
        false
    }

    suspend fun pingURL(url: String) = try {
        withTimeout(TIMEOUT) {
            ktorClient.head<Unit>(url)
            true
        }
    } catch (e: IOException) {
        logger.log(Level.INFO, "Could not ping $url (${e.message})")
        false
    } catch (e: TimeoutCancellationException) {
        logger.log(Level.INFO, "Could not ping $url (${e.message})")
        false
    } catch (e: ResponseException) {
        logger.log(
            Level.INFO,
            "Successfully pinged $url but got a response exception (${e.message})",
        )
        true
    }

    companion object {
        private val TIMEOUT = 5.seconds
    }
}
