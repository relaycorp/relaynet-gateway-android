package tech.relaycorp.gateway.domain

import androidx.annotation.VisibleForTesting
import tech.relaycorp.gateway.common.CryptoUtils.BC_PROVIDER
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.disk.SensitiveStore
import tech.relaycorp.gateway.domain.courier.CalculateCRCMessageCreationDate
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import javax.inject.Inject
import kotlin.time.toJavaDuration

class LocalConfig
@Inject constructor(
    private val sensitiveStore: SensitiveStore
) {
    // Private Gateway Key Pair

    suspend fun getKeyPair() =
        sensitiveStore.read(PRIVATE_KEY_FILE_NAME)
            ?.toPrivateKey()
            ?.toKeyPair()
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

    suspend fun deleteCertificate() {
        sensitiveStore.delete(PDA_CERTIFICATE_FILE_NAME)
    }

    suspend fun getCargoDeliveryAuth() =
        sensitiveStore.read(CDA_CERTIFICATE_FILE_NAME)
            ?.let { Certificate.deserialize(it) }
            ?: throw RuntimeException("No CDA issuer was found")

    private suspend fun generateCargoDeliveryAuth() = generateCertificate().also {
        sensitiveStore.store(CDA_CERTIFICATE_FILE_NAME, it.serialize())
    }

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

    private fun ByteArray.toPrivateKey(): PrivateKey {
        val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(this)
        val generator: KeyFactory = KeyFactory.getInstance(KEY_ALGORITHM, BC_PROVIDER)
        return generator.generatePrivate(privateKeySpec)
    }

    private fun PrivateKey.toKeyPair(): KeyPair {
        val publicKeySpec =
            (this as RSAPrivateCrtKey).run { RSAPublicKeySpec(modulus, publicExponent) }
        val keyFactory = KeyFactory.getInstance("RSA", BC_PROVIDER)
        val publicKey = keyFactory.generatePublic(publicKeySpec)
        return KeyPair(publicKey, this)
    }

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
        private const val KEY_ALGORITHM = "RSA"

        private const val PRIVATE_KEY_FILE_NAME = "local_gateway.key"

        private const val PDA_CERTIFICATE_FILE_NAME = "local_gateway.certificate"
        private const val CDA_CERTIFICATE_FILE_NAME = "cda_local_gateway.certificate"
    }
}
