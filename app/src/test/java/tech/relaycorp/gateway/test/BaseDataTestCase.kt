package tech.relaycorp.gateway.test

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import tech.relaycorp.relaynet.SessionKeyPair
import tech.relaycorp.relaynet.keystores.PrivateKeyStore
import tech.relaycorp.relaynet.nodes.GatewayManager
import tech.relaycorp.relaynet.testing.keystores.MockPrivateKeyStore
import tech.relaycorp.relaynet.testing.keystores.MockSessionPublicKeyStore
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import tech.relaycorp.relaynet.wrappers.privateAddress
import javax.inject.Provider

abstract class BaseDataTestCase {
    protected val privateKeyStore = MockPrivateKeyStore()
    protected val privateKeyStoreProvider = Provider<PrivateKeyStore> { privateKeyStore }

    protected val publicKeyStore = MockSessionPublicKeyStore()

    private val gatewayManager = GatewayManager(privateKeyStore, publicKeyStore)
    protected val gatewayManagerProvider = Provider<GatewayManager> { gatewayManager }

    protected val privateGatewaySessionKeyPair = SessionKeyPair.generate()
    protected val publicGatewaySessionKeyPair = SessionKeyPair.generate()

    @BeforeEach
    @AfterEach
    fun clearKeystores() {
        privateKeyStore.clear()
        publicKeyStore.clear()
    }

    protected suspend fun registerPrivateGatewayIdentityKeyPair() = privateKeyStore.saveIdentityKey(
        KeyPairSet.PRIVATE_GW.private,
        PDACertPath.PRIVATE_GW
    )

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