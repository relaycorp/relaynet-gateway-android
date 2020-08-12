package tech.relaycorp.gateway.background

import kotlinx.coroutines.flow.first
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import javax.inject.Inject

class CheckPublicGatewayAccess
@Inject constructor(
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val pingRemoteServer: PingRemoteServer
) {
    suspend fun check() =
        pingRemoteServer.pingHostname(publicGatewayPreferences.getAddress().first())
}
