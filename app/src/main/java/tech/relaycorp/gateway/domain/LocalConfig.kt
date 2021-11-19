package tech.relaycorp.gateway.domain

import androidx.annotation.VisibleForTesting
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.disk.SensitiveStore
import tech.relaycorp.gateway.domain.courier.CalculateCRCMessageCreationDate
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.wrappers.deserializeRSAKeyPair
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.security.KeyPair
import javax.inject.Inject
import kotlin.time.toJavaDuration

class LocalConfig
@Inject constructor(
    private val sensitiveStore: SensitiveStore
) {
    // Private Gateway Key Pair

    suspend fun getKeyPair() =
        sensitiveStore.read(PRIVATE_KEY_FILE_NAME)
            ?.deserializeRSAKeyPair()
            ?: throw RuntimeException("No key pair was found")

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun generateKeyPair() = generateRSAKeyPair().also { setKeyPair(it) }

    private suspend fun setKeyPair(value: KeyPair) {
        sensitiveStore.store(PRIVATE_KEY_FILE_NAME, value.private.encoded)
    }

    // Private Gateway Certificate

    suspend fun getCertificate() =
        sensitiveStore.read(PDA_CERTIFICATE_FILE_NAME)
            ?.let { Certificate.deserialize(it) }
            ?: generateCertificate()
                .also { setCertificate(it) }

    suspend fun setCertificate(value: Certificate) {
        sensitiveStore.store(PDA_CERTIFICATE_FILE_NAME, value.serialize())
    }

    suspend fun getCargoDeliveryAuth() =
        sensitiveStore.read(CDA_CERTIFICATE_FILE_NAME)
            ?.let { Certificate.deserialize(it) }
            ?: throw RuntimeException("No CDA issuer was found")

    private suspend fun generateCargoDeliveryAuth() = generateCertificate().also {
        sensitiveStore.store(CDA_CERTIFICATE_FILE_NAME, it.serialize())
    }

    @Synchronized
    suspend fun bootstrap() {
        try {
            getKeyPair()
        } catch (_: RuntimeException) {
            generateKeyPair()
        }

        try {
            getCargoDeliveryAuth()
        } catch (_: RuntimeException) {
            generateCargoDeliveryAuth()
        }
    }

    // Helpers

    private suspend fun generateCertificate(): Certificate {
        val keyPair = getKeyPair()
        return issueGatewayCertificate(
            subjectPublicKey = keyPair.public,
            issuerPrivateKey = keyPair.private,
            validityStartDate = nowInUtc()
                .minus(CalculateCRCMessageCreationDate.CLOCK_DRIFT_TOLERANCE.toJavaDuration()),
            validityEndDate = nowInUtc().plusYears(1)
        )
    }

    companion object {
        private const val PRIVATE_KEY_FILE_NAME = "local_gateway.key"

        private const val PDA_CERTIFICATE_FILE_NAME = "local_gateway.certificate"
        private const val CDA_CERTIFICATE_FILE_NAME = "cda_local_gateway.certificate"
    }
}
