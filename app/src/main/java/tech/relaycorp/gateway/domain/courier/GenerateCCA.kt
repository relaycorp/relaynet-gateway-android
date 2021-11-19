package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.issueDeliveryAuthorization
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import tech.relaycorp.relaynet.messages.payloads.CargoCollectionRequest
import tech.relaycorp.relaynet.nodes.GatewayManager
import tech.relaycorp.relaynet.wrappers.privateAddress
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.time.days

class GenerateCCA
@Inject constructor(
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val localConfig: LocalConfig,
    private val calculateCreationDate: CalculateCRCMessageCreationDate,
    private val gatewayManager: GatewayManager
) {

    suspend fun generateSerialized(): ByteArray {
        val identityPrivateKey = localConfig.getIdentityKeyPair().privateKey
        val senderCertificate = localConfig.getCargoDeliveryAuth()
        val publicGatewayPublicKey = publicGatewayPreferences.getCertificate().subjectPublicKey
        val cda = issueDeliveryAuthorization(
            publicGatewayPublicKey,
            identityPrivateKey,
            ZonedDateTime.now().plusSeconds(TTL.inSeconds.toLong()),
            senderCertificate
        )
        val ccr = CargoCollectionRequest(cda)
        val ccrCiphertext = gatewayManager.wrapMessagePayload(
            ccr,
            publicGatewayPublicKey.privateAddress,
            senderCertificate.subjectPrivateAddress
        )
        val cca = CargoCollectionAuthorization(
            recipientAddress = publicGatewayPreferences.getCogRPCAddress(),
            payload = ccrCiphertext,
            senderCertificate = senderCertificate,
            creationDate = calculateCreationDate.calculate(),
            ttl = TTL.inSeconds.toInt()
        )
        return cca.serialize(identityPrivateKey)
    }

    companion object {
        private val TTL = 14.days
    }
}
