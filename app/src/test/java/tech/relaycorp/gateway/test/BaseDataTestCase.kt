package tech.relaycorp.gateway.test

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import tech.relaycorp.relaynet.SessionKeyPair
import tech.relaycorp.relaynet.nodes.GatewayManager
import tech.relaycorp.relaynet.testing.keystores.MockPrivateKeyStore
import tech.relaycorp.relaynet.testing.keystores.MockSessionPublicKeyStore
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.wrappers.privateAddress

abstract class BaseDataTestCase {
    private val privateKeyStore = MockPrivateKeyStore()
    protected val publicKeyStore = MockSessionPublicKeyStore()
    protected val gatewayManager = GatewayManager(privateKeyStore, publicKeyStore)

    protected val privateGatewaySessionKeyPair = SessionKeyPair.generate()
    protected val publicGatewaySessionKeyPair = SessionKeyPair.generate()

    @BeforeEach
    @AfterEach
    fun clearKeystores() {
        privateKeyStore.clear()
        publicKeyStore.clear()
    }

    protected suspend fun registerPrivateGatewaySessionKey() {
        privateKeyStore.saveSessionKey(
            privateGatewaySessionKeyPair.privateKey,
            privateGatewaySessionKeyPair.sessionKey.keyId,
            KeyPairSet.PRIVATE_GW.public.privateAddress,
            KeyPairSet.PUBLIC_GW.public.privateAddress
        )
    }

    protected suspend fun registerPublicGatewaySessionKey() {
        publicKeyStore.save(
            publicGatewaySessionKeyPair.sessionKey,
            KeyPairSet.PUBLIC_GW.public.privateAddress
        )
    }
}
