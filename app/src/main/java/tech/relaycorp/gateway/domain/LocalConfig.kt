package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.common.CryptoUtils.BC_PROVIDER
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.disk.SensitiveStore
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

class LocalConfig
@Inject constructor(
    private val sensitiveStore: SensitiveStore
) {

    // Private Gateway Key Pair

    suspend fun getKeyPair() =
        sensitiveStore.read(PRIVATE_KEY_FILE_NAME)
            ?.toPrivateKey()
            ?.toKeyPair()
            ?: generateKeyPair()
                .also { setKeyPair(it) }

    private suspend fun setKeyPair(value: KeyPair) {
        sensitiveStore.store(PRIVATE_KEY_FILE_NAME, value.private.encoded)
    }

    // Private Gateway Certificate

    suspend fun getCertificate() =
        sensitiveStore.read(CERTIFICATE_FILE_NAME)
            ?.let { Certificate.deserialize(it) }
            ?: generateCertificate()
                .also { setCertificate(it) }

    suspend fun setCertificate(value: Certificate) {
        sensitiveStore.store(CERTIFICATE_FILE_NAME, value.serialize())
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

    private fun generateKeyPair() = generateRSAKeyPair()

    private suspend fun generateCertificate(): Certificate {
        val keyPair = getKeyPair()
        return issueGatewayCertificate(
            subjectPublicKey = keyPair.public,
            issuerPrivateKey = keyPair.private,
            validityEndDate = nowInUtc().plusYears(1)
        )
    }

    companion object {
        private const val KEY_ALGORITHM = "RSA"

        private const val PRIVATE_KEY_FILE_NAME = "local_gateway.key"
        private const val CERTIFICATE_FILE_NAME = "local_gateway.certificate"
    }
}
