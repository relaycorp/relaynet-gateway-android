package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.data.disk.SensitiveStore
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.time.ZonedDateTime
import javax.inject.Inject

class LocalConfig
@Inject constructor(
    private val sensitiveStore: SensitiveStore
) {

    suspend fun getKeyPair() =
        sensitiveStore.read(PRIVATE_KEY_FILE_NAME)
            ?.toPrivateKey()
            ?.toKeyPair()
            ?: generateKeyPair().also {
                sensitiveStore.store(PRIVATE_KEY_FILE_NAME, it.private.encoded)
            }

    suspend fun getCertificate() =
        sensitiveStore.read(CERTIFICATE_FILE_NAME)
            ?.let { Certificate.deserialize(it) }
            ?: generateGatewayCertificate(getKeyPair())
                .also {
                    sensitiveStore.store(CERTIFICATE_FILE_NAME, it.serialize())
                }

    private fun generateKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        keyGen.initialize(RSA_KEY_MODULUS)
        return keyGen.generateKeyPair()
    }

    private fun generateGatewayCertificate(keyPair: KeyPair) =
        issueGatewayCertificate(
            keyPair.public,
            keyPair.private,
            ZonedDateTime.now().plusYears(GATEWAY_CERTIFICATE_VALIDITY_YEARS)
        )

    private fun ByteArray.toPrivateKey(): PrivateKey {
        val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(this)
        val generator: KeyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
        return generator.generatePrivate(privateKeySpec)
    }

    private fun PrivateKey.toKeyPair(): KeyPair {
        val publicKeySpec =
            (this as RSAPrivateCrtKey).run { RSAPublicKeySpec(modulus, publicExponent) }
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(publicKeySpec)
        return KeyPair(publicKey, this)
    }

    companion object {
        private const val KEY_ALGORITHM = "RSA"
        private const val RSA_KEY_MODULUS = 2048
        private const val GATEWAY_CERTIFICATE_VALIDITY_YEARS = 3L

        private const val PRIVATE_KEY_FILE_NAME = "local_gateway.key"
        private const val CERTIFICATE_FILE_NAME = "local_gateway.certificate"
    }
}
