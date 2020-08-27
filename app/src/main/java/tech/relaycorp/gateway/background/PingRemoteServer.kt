package tech.relaycorp.gateway.background

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.relaycorp.gateway.common.Logging.logger
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.logging.Level
import javax.inject.Inject

class PingRemoteServer
@Inject constructor() {
    suspend fun pingAddress(address: String, port: Int) =
        withContext(Dispatchers.IO) {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(address, port), TIMEOUT)
                true
            } catch (ce: ConnectException) {
                logger.log(Level.INFO, "Could not reach $address:$port")
                false
            } catch (ex: Exception) {
                logger.log(Level.INFO, "Could not reach $address:$port", ex)
                false
            } finally {
                socket.close()
            }
        }

    suspend fun pingHostname(hostname: String) =
        withContext(Dispatchers.IO) {
            try {
                URL(hostname)
                    .openConnection()
                    .apply {
                        setRequestProperty("Connection", "close")
                        connectTimeout = TIMEOUT
                        connect()
                    }
                true
            } catch (ex: IOException) {
                logger.log(Level.INFO, "Could not reach $hostname", ex)
                false
            }
        }

    companion object {
        private const val TIMEOUT = 2000
    }
}
