package tech.relaycorp.gateway.background

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.relaycorp.gateway.common.Logging.logger
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.logging.Level
import javax.inject.Inject

class PingRemoteServer
@Inject constructor() {
    suspend fun ping(hostname: String, port: Int) =
        withContext(Dispatchers.IO) {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(hostname, port), 2000)
                true
            } catch (ce: ConnectException) {
                logger.log(Level.INFO, "Could not reach $hostname:$port")
                false
            } catch (ex: Exception) {
                logger.log(Level.INFO, "Could not reach $hostname:$port", ex)
                false
            } finally {
                socket.close()
            }
        }
}
