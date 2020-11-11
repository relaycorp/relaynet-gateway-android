package tech.relaycorp.gateway.common

import java.security.MessageDigest
import java.security.PublicKey

fun PublicKey.getHexDigest(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(encoded)
    return hash.toHexString()
}

fun ByteArray.toHexString() =
    joinToString("") { String.format("%02X", it) }
