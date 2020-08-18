package tech.relaycorp.gateway.pdc.local.utils

import tech.relaycorp.relaynet.messages.InvalidMessageException
import tech.relaycorp.relaynet.messages.control.NonceSignature
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.util.UUID

internal object Handshake {
    /**
     * Generate handshake nonce.
     */
    internal fun generateNonce(): ByteArray {
        val uuid = UUID.randomUUID()
        return uuid.toString().toByteArray()
    }

    /**
     * Verify that the signature in `signatureSerialized` is valid and its plaintext corresponds to
     * `nonce`, and return the signer's certificate if the signature is valid.
     */
    @Throws(InvalidHandshakeSignatureException::class)
    internal fun verifySignature(
        signatureSerialized: ByteArray,
        nonce: ByteArray
    ): Certificate {
        val signature = try {
            NonceSignature.deserialize(signatureSerialized)
        } catch (exc: InvalidMessageException) {
            throw InvalidHandshakeSignatureException("Invalid signature", exc)
        }
        if (nonce.asList() != signature.nonce.asList()) {
            throw InvalidHandshakeSignatureException("Signed nonce does not match expected one")
        }
        return signature.signerCertificate
    }
}
