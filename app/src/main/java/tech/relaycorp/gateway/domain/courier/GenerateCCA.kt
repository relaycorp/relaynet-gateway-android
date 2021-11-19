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

    suspend fun generate(): CargoCollectionAuthorization {
        val senderCertificate = localConfig.getCargoDeliveryAuth()
        val publicGatewayPublicKey = publicGatewayPreferences.getCertificate().subjectPublicKey
        val cda = issueDeliveryAuthorization(
            publicGatewayPublicKey,
            localConfig.getKeyPair().private,
            ZonedDateTime.now().plusSeconds(TTL.inSeconds.toLong()),
            senderCertificate
        )
        val ccr = CargoCollectionRequest(cda)
        val ccrCiphertext = gatewayManager.wrapMessagePayload(
            ccr,
            publicGatewayPublicKey.privateAddress,
            senderCertificate.subjectPrivateAddress
        )
        return CargoCollectionAuthorization(
            recipientAddress = publicGatewayPreferences.getCogRPCAddress(),
            payload = ccrCiphertext,
            senderCertificate = senderCertificate,
            creationDate = calculateCreationDate.calculate(),
            ttl = TTL.inSeconds.toInt()
        )
    }

    suspend fun generateByteArray() =
        generate().serialize(localConfig.getKeyPair().private)

    companion object {
        private val TTL = 14.days
    }
}
