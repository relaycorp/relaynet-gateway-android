package tech.relaycorp.gateway.common

import tech.relaycorp.relaynet.wrappers.deserializeRSAKeyPair
import java.security.PrivateKey
import java.security.PublicKey

fun PrivateKey.toPublicKey(): PublicKey =
    encoded.deserializeRSAKeyPair().public
