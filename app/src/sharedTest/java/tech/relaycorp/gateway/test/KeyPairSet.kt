package tech.relaycorp.gateway.test

import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair

object KeyPairSet {
    val PUBLIC_GW = generateRSAKeyPair()
    val PRIVATE_GW = generateRSAKeyPair()
    val PRIVATE_ENDPOINT = generateRSAKeyPair()
    val PDA_GRANTEE = generateRSAKeyPair()
}
