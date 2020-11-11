package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import javax.inject.Inject
import kotlin.time.days

class GenerateCCA
@Inject constructor(
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val localConfig: LocalConfig
) {

    suspend fun generate() =
        CargoCollectionAuthorization(
            recipientAddress = publicGatewayPreferences.getCogRPCAddress(),
            payload = "".toByteArray(),
            senderCertificate = localConfig.getCertificate(),
            ttl = TTL.inSeconds.toInt()
        )

    suspend fun generateByteArray() =
        generate().serialize(localConfig.getKeyPair().private)

    companion object {
        private val TTL = 14.days
    }
}
