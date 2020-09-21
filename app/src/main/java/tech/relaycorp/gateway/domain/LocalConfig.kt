package tech.relaycorp.gateway.domain

import android.content.res.Resources
import org.bouncycastle.jce.provider.BouncyCastleProvider
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.issueGatewayCertificate
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
//    private val sensitiveStore: SensitiveStore,
    private val resources: Resources
) {
    suspend fun getKeyPair() =
        resources.openRawResource(R.raw.priv_gateway_key).readBytesAndClose()
            .toPrivateKey()
            .toKeyPair()
//            ?: generateRSAKeyPair().also {
//                sensitiveStore.store(PRIVATE_KEY_FILE_NAME, it.private.encoded)
//            }

    suspend fun getCertificate() =
        resources.openRawResource(R.raw.priv_gateway_cert).readBytesAndClose()
            ?.let { Certificate.deserialize(it) }
//            ?: generateGatewayCertificate(getKeyPair())
//                .also {
//                    sensitiveStore.store(CERTIFICATE_FILE_NAME, it.serialize())
//                }

    private fun generateGatewayCertificate(keyPair: KeyPair) =
        issueGatewayCertificate(
            keyPair.public,
            keyPair.private,
            nowInUtc().plusYears(GATEWAY_CERTIFICATE_VALIDITY_YEARS)
        )

    private fun ByteArray.toPrivateKey(): PrivateKey {
        val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(this)
        val generator: KeyFactory = KeyFactory.getInstance(KEY_ALGORITHM, bouncyCastleProvider)
        return generator.generatePrivate(privateKeySpec)
    }

    private fun PrivateKey.toKeyPair(): KeyPair {
        val publicKeySpec =
            (this as RSAPrivateCrtKey).run { RSAPublicKeySpec(modulus, publicExponent) }
        val keyFactory = KeyFactory.getInstance("RSA", bouncyCastleProvider)
        val publicKey = keyFactory.generatePublic(publicKeySpec)
        return KeyPair(publicKey, this)
    }

    companion object {
        private const val KEY_ALGORITHM = "RSA"
        private const val GATEWAY_CERTIFICATE_VALIDITY_YEARS = 3L

//        private const val PRIVATE_KEY_FILE_NAME = "local_gateway.key"
//        private const val CERTIFICATE_FILE_NAME = "local_gateway.certificate"

        private val bouncyCastleProvider = BouncyCastleProvider()
    }
}
