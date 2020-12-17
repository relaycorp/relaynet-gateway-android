package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.issueDeliveryAuthorization
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import tech.relaycorp.relaynet.messages.payloads.CargoCollectionRequest
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.time.days

class GenerateCCA
@Inject constructor(
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val localConfig: LocalConfig,
    private val calculateCreationDate: CalculateCRCMessageCreationDate
) {

    suspend fun generate(): CargoCollectionAuthorization {
        val senderCertificate = localConfig.getCRCCertificate()
        val cda = issueDeliveryAuthorization(
            publicGatewayPreferences.getCertificate().subjectPublicKey,
            localConfig.getKeyPair().private,
            ZonedDateTime.now().plusSeconds(TTL.inSeconds.toLong()),
            senderCertificate
        )
        val ccr = CargoCollectionRequest(cda)
        return CargoCollectionAuthorization(
            recipientAddress = publicGatewayPreferences.getCogRPCAddress(),
            payload = ccr.encrypt(publicGatewayPreferences.getCertificate()),
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
