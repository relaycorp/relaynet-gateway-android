package tech.relaycorp.gateway.pdc.local

import tech.relaycorp.relaynet.messages.control.NonceSignature
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.security.PrivateKey

object HandshakeTestUtils {
    val UUID4_REGEX =
        """^[-0-9a-f]{36}$""".toRegex()

    fun sign(
        nonce: ByteArray,
        signerPrivateKey: PrivateKey,
        signerCertificate: Certificate
    ): ByteArray {
        val signature = NonceSignature(nonce, signerCertificate)
        return signature.serialize(signerPrivateKey)
    }
}
