package tech.relaycorp.gateway.background

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.doh.InternetAddressResolutionException
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import java.util.logging.Level
import javax.inject.Inject

class CheckInternetGatewayAccess
@Inject constructor(
    private val internetGatewayPreferences: InternetGatewayPreferences,
    private val pingRemoteServer: PingRemoteServer,
) {
    suspend fun check(): Boolean {
        val address = try {
            internetGatewayPreferences.getPoWebAddress()
        } catch (exc: InternetAddressResolutionException) {
            logger.log(Level.WARNING, "Failed to resolve PoWeb address (${exc.message})")
            return false
        }
        return pingRemoteServer.pingURL("https://${address.host}:${address.port}")
    }
}
