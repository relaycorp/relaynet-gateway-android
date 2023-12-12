package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.issueDeliveryAuthorization
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.messages.payloads.CargoCollectionRequest
import tech.relaycorp.relaynet.nodes.GatewayManager
import tech.relaycorp.relaynet.wrappers.nodeId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.Duration.Companion.days

class GenerateCCA
@Inject constructor(
    private val internetGatewayPreferences: InternetGatewayPreferences,
    private val localConfig: LocalConfig,
    private val calculateCreationDate: CalculateCRCMessageCreationDate,
    private val gatewayManager: Provider<GatewayManager>,
) {

    suspend fun generateSerialized(): ByteArray {
        val identityPrivateKey = localConfig.getIdentityKey()
        val cdaIssuer = localConfig.getCargoDeliveryAuth()
        val internetGatewayPublicKey = internetGatewayPreferences.getPublicKey()
        val cda = issueDeliveryAuthorization(
            internetGatewayPublicKey,
            identityPrivateKey,
            ZonedDateTime.now().plusSeconds(TTL.inWholeSeconds),
            cdaIssuer,
        )
        val ccr = CargoCollectionRequest(cda)
        val ccrCiphertext = gatewayManager.get().wrapMessagePayload(
            ccr,
            internetGatewayPublicKey.nodeId,
            cdaIssuer.subjectId,
        )
        val cca = CargoCollectionAuthorization(
            recipient = Recipient(
                internetGatewayPreferences.getId(),
                internetGatewayPreferences.getAddress(),
            ),
            payload = ccrCiphertext,
            senderCertificate = localConfig.getIdentityCertificate(),
            creationDate = calculateCreationDate.calculate(),
            ttl = TTL.inWholeSeconds.toInt(),
        )
        return cca.serialize(identityPrivateKey)
    }

    companion object {
        private val TTL = 14.days
    }
}
