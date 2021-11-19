package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.disk.SensitiveStore
import tech.relaycorp.gateway.domain.courier.CalculateCRCMessageCreationDate
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.keystores.IdentityKeyPair
import tech.relaycorp.relaynet.keystores.PrivateKeyStore
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.security.PrivateKey
import java.security.PublicKey
import javax.inject.Inject
import kotlin.time.toJavaDuration

class LocalConfig
@Inject constructor(
    private val sensitiveStore: SensitiveStore,
    private val privateKeyStore: PrivateKeyStore
) {
    // Private Gateway Key Pair

    suspend fun getIdentityKeyPair(): IdentityKeyPair =
        privateKeyStore.retrieveAllIdentityKeys().firstOrNull()
            ?: throw RuntimeException("No key pair was found")

    private suspend fun generateKeyPair(): IdentityKeyPair {
        val keyPair = generateRSAKeyPair()
        val certificate = selfIssueCargoDeliveryAuth(keyPair.private, keyPair.public)
        privateKeyStore.saveIdentityKey(keyPair.private, certificate)
        return IdentityKeyPair(keyPair.private, certificate)
    }

    // Private Gateway Certificate

    suspend fun getIdentityCertificate(): Certificate = getIdentityKeyPair().certificate

    suspend fun setIdentityCertificate(value: Certificate) {
        val privateKey = getIdentityKeyPair().privateKey
        privateKeyStore.saveIdentityKey(privateKey, value)
    }

    @Synchronized
    suspend fun bootstrap() {
        val idKeyPair = try {
            getIdentityKeyPair()
        } catch (_: RuntimeException) {
            generateKeyPair()
        }

        try {
            getCargoDeliveryAuth()
        } catch (_: RuntimeException) {
            generateCargoDeliveryAuth(idKeyPair)
        }
    }

    suspend fun getCargoDeliveryAuth() =
        sensitiveStore.read(CDA_CERTIFICATE_FILE_NAME)
            ?.let { Certificate.deserialize(it) }
            ?: throw RuntimeException("No CDA issuer was found")

    private fun selfIssueCargoDeliveryAuth(
        privateKey: PrivateKey,
        publicKey: PublicKey
    ): Certificate {
        return issueGatewayCertificate(
            subjectPublicKey = publicKey,
            issuerPrivateKey = privateKey,
            validityStartDate = nowInUtc()
                .minus(CalculateCRCMessageCreationDate.CLOCK_DRIFT_TOLERANCE.toJavaDuration()),
            validityEndDate = nowInUtc().plusYears(1)
        )
    }

    private suspend fun generateCargoDeliveryAuth(idKeyPair: IdentityKeyPair) {
        val cda = selfIssueCargoDeliveryAuth(
            idKeyPair.privateKey,
            idKeyPair.certificate.subjectPublicKey
        )
        sensitiveStore.store(CDA_CERTIFICATE_FILE_NAME, cda.serialize())
    }

    // Helpers

    companion object {
        internal const val CDA_CERTIFICATE_FILE_NAME = "cda_local_gateway.certificate"
    }
}
