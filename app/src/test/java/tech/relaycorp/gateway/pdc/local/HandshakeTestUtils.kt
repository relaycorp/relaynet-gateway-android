package tech.relaycorp.gateway.pdc.local

import tech.relaycorp.relaynet.crypto.SignedData
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.security.PrivateKey

object HandshakeTestUtils {
    val UUID4_REGEX =
        """^[-0-9a-f]{36}$""".toRegex()

    fun sign(
        plaintext: ByteArray,
        signerPrivateKey: PrivateKey,
        signerCertificate: Certificate,
        encapsulatePlaintext: Boolean = false
    ): ByteArray {
        val signedData = SignedData.sign(
            plaintext,
            signerPrivateKey,
            signerCertificate,
            encapsulatePlaintext = encapsulatePlaintext
        )
        return signedData.serialize()
    }
}
