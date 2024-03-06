package tech.relaycorp.gateway.test

import com.nhaarman.mockitokotlin2.spy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import tech.relaycorp.relaynet.SessionKeyPair
import tech.relaycorp.relaynet.keystores.CertificateStore
import tech.relaycorp.relaynet.keystores.PrivateKeyStore
import tech.relaycorp.relaynet.nodes.GatewayManager
import tech.relaycorp.relaynet.pki.CertificationPath
import tech.relaycorp.relaynet.testing.keystores.MockCertificateStore
import tech.relaycorp.relaynet.testing.keystores.MockPrivateKeyStore
import tech.relaycorp.relaynet.testing.keystores.MockSessionPublicKeyStore
import tech.relaycorp.relaynet.testing.pki.CDACertPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import tech.relaycorp.relaynet.wrappers.nodeId
import javax.inject.Provider

abstract class BaseDataTestCase {
    protected val privateKeyStore = spy(MockPrivateKeyStore())
    protected val certificateStore = spy(MockCertificateStore())
    protected val privateKeyStoreProvider = Provider<PrivateKeyStore> { privateKeyStore }
    protected val certificateStoreProvider = Provider<CertificateStore> { certificateStore }

    protected val publicKeyStore = MockSessionPublicKeyStore()

    private val gatewayManager = GatewayManager(privateKeyStore, publicKeyStore)
    protected val gatewayManagerProvider = Provider<GatewayManager> { gatewayManager }

    protected val privateGatewaySessionKeyPair = SessionKeyPair.generate()
    protected val internetGatewaySessionKeyPair = SessionKeyPair.generate()

    @BeforeEach
    @AfterEach
    fun clearKeystores() {
        privateKeyStore.clear()
        publicKeyStore.clear()
        certificateStore.clear()
    }

    protected suspend fun registerPrivateGatewayParcelDeliveryCertificate() {
        privateKeyStore.saveIdentityKey(KeyPairSet.PRIVATE_GW.private)
        certificateStore.save(
            CertificationPath(PDACertPath.PRIVATE_GW, emptyList()),
            PDACertPath.INTERNET_GW.subjectId,
        )
    }

    protected suspend fun registerPrivateGatewaySessionKey() {
        privateKeyStore.saveSessionKey(
            privateGatewaySessionKeyPair.privateKey,
            privateGatewaySessionKeyPair.sessionKey.keyId,
            KeyPairSet.PRIVATE_GW.public.nodeId,
            KeyPairSet.INTERNET_GW.public.nodeId,
        )
    }

    protected suspend fun registerInternetGatewaySessionKey() {
        publicKeyStore.save(
            internetGatewaySessionKeyPair.sessionKey,
            KeyPairSet.INTERNET_GW.public.nodeId,
        )
    }

    protected fun clearPrivateGatewayParcelDeliveryCertificate() {
        certificateStore.delete(
            PDACertPath.PRIVATE_GW.subjectPublicKey.nodeId,
            PDACertPath.INTERNET_GW.subjectId,
        )
    }

    protected suspend fun bootstrapCargoDeliveryAuth() {
        privateKeyStore.saveIdentityKey(KeyPairSet.PRIVATE_GW.private)
        certificateStore.save(
            CertificationPath(CDACertPath.PRIVATE_GW, emptyList()),
            CDACertPath.PRIVATE_GW.subjectId,
        )
    }
}
