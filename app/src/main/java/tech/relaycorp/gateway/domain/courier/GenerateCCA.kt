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
import javax.inject.Provider
import kotlin.time.days

class GenerateCCA
@Inject constructor(
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val localConfig: LocalConfig,
    private val calculateCreationDate: CalculateCRCMessageCreationDate,
    private val gatewayManager: Provider<GatewayManager>
) {

    suspend fun generateSerialized(): ByteArray {
        val identityPrivateKey = localConfig.getIdentityKey()
        val cdaIssuer = localConfig.getCargoDeliveryAuth()
        val publicGatewayPublicKey = publicGatewayPreferences.getCertificate().subjectPublicKey
        val cda = issueDeliveryAuthorization(
            publicGatewayPublicKey,
            identityPrivateKey,
            ZonedDateTime.now().plusSeconds(TTL.inSeconds.toLong()),
            cdaIssuer
        )
        val ccr = CargoCollectionRequest(cda)
        val ccrCiphertext = gatewayManager.get().wrapMessagePayload(
            ccr,
            publicGatewayPublicKey.privateAddress,
            cdaIssuer.subjectPrivateAddress
        )
        val cca = CargoCollectionAuthorization(
            recipientAddress = publicGatewayPreferences.getCogRPCAddress(),
            payload = ccrCiphertext,
            senderCertificate = localConfig.getIdentityCertificate(),
            creationDate = calculateCreationDate.calculate(),
            ttl = TTL.inSeconds.toInt()
        )
        return cca.serialize(identityPrivateKey)
    }

    companion object {
        private val TTL = 14.days
    }
}
