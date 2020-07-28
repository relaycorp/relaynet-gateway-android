package tech.relaycorp.gateway.test

import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair

object KeyPairSet {
    val PUBLIC_GW by lazy { generateRSAKeyPair() }
    val PRIVATE_GW by lazy { generateRSAKeyPair() }
    val PRIVATE_ENDPOINT by lazy { generateRSAKeyPair() }
    val PDA_GRANTEE by lazy { generateRSAKeyPair() }
}
