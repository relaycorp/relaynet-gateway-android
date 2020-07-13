package tech.relaycorp.gateway.domain.courier

import kotlinx.coroutines.flow.first
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import javax.inject.Inject

class GenerateCCA
@Inject constructor(
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val localConfig: LocalConfig
) {

    suspend fun generate() =
        CargoCollectionAuthorization(
            recipientAddress = publicGatewayPreferences.getAddress().first(),
            payload = "".toByteArray(),
            senderCertificate = localConfig.getCertificate()
        )

    suspend fun generateByteArray() =
        generate().serialize(localConfig.getKeyPair().private)
}
