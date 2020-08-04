package tech.relaycorp.gateway.pdc.local

import tech.relaycorp.relaynet.crypto.SignedData
import tech.relaycorp.relaynet.crypto.SignedDataException
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
     * Verify that the signature in `cmsSignedData` is valid and its plaintext corresponds to
     * `nonce`.
     */
    @Throws(InvalidHandshakeSignatureException::class)
    internal fun verifySignature(
        signedDataSerialized: ByteArray,
        nonce: ByteArray
    ): SignedData {
        return try {
            SignedData.deserialize(signedDataSerialized).also { it.verify(nonce) }
        } catch (exc: SignedDataException) {
            throw InvalidHandshakeSignatureException("Invalid CMS SignedData value", exc)
        }
    }
}
