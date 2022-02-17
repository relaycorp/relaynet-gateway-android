package tech.relaycorp.gateway.test

import com.nhaarman.mockitokotlin2.spy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import tech.relaycorp.relaynet.SessionKeyPair
import tech.relaycorp.relaynet.keystores.CertificateStore
import tech.relaycorp.relaynet.keystores.PrivateKeyStore
import tech.relaycorp.relaynet.nodes.GatewayManager
import tech.relaycorp.relaynet.testing.keystores.MockCertificateStore
import tech.relaycorp.relaynet.testing.keystores.MockPrivateKeyStore
import tech.relaycorp.relaynet.testing.keystores.MockSessionPublicKeyStore
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import tech.relaycorp.relaynet.wrappers.privateAddress
import javax.inject.Provider

abstract class BaseDataTestCase {
    protected val privateKeyStore = MockPrivateKeyStore()
    protected val certificateStore = spy(MockCertificateStore())
    protected val privateKeyStoreProvider = Provider<PrivateKeyStore> { privateKeyStore }
    protected val certificateStoreProvider = Provider<CertificateStore> { certificateStore }

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

    protected suspend fun registerPrivateGatewayIdentity() {
        privateKeyStore.saveIdentityKey(KeyPairSet.PRIVATE_GW.private)
        certificateStore.save(PDACertPath.PRIVATE_GW)
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
