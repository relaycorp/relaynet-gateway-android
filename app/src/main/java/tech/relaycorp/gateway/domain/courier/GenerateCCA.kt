package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import java.util.Collections.max
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
            creationDate = calculateCreationDate(),
            ttl = TTL.inSeconds.toInt()
        )

    suspend fun generateByteArray() =
        generate().serialize(localConfig.getKeyPair().private)

    private suspend fun calculateCreationDate() =
        max(
            listOf(
                nowInUtc().minusMinutes(5), // Allow for some clock-drift
                localConfig.getCertificate().startDate // Never before the GW registration
            )
        )

    companion object {
        private val TTL = 14.days
    }
}
