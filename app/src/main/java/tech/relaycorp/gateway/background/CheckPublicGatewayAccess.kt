package tech.relaycorp.gateway.background

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.preference.PublicAddressResolutionException
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import java.util.logging.Level
import javax.inject.Inject

class CheckPublicGatewayAccess
@Inject constructor(
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val pingRemoteServer: PingRemoteServer
) {
    suspend fun check(): Boolean {
        val address = try {
            publicGatewayPreferences.resolvePoWebAddress()
        } catch (exc: PublicAddressResolutionException) {
            logger.log(Level.WARNING, "Failed to resolve PoWeb address", exc)
            return false
        }
        return pingRemoteServer.pingURL("https://${address.host}:${address.port}")
    }
}
