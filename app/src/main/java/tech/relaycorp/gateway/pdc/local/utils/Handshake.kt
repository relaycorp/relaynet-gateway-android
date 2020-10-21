package tech.relaycorp.gateway.pdc.local.utils

import java.util.UUID

internal object Handshake {
    /**
     * Generate handshake nonce.
     */
    internal fun generateNonce(): ByteArray {
        val uuid = UUID.randomUUID()
        return uuid.toString().toByteArray()
    }
}
