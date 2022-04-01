package tech.relaycorp.gateway.domain.publicsync

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.disk.FileStore
import tech.relaycorp.gateway.data.doh.PublicAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.model.ServiceAddress
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.endpoint.GatewayCertificateChangeNotifier
import tech.relaycorp.gateway.pdc.PoWebClientBuilder
import tech.relaycorp.gateway.test.BaseDataTestCase
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.SessionKey
import tech.relaycorp.relaynet.bindings.pdc.ClientBindingException
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistration
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationAuthorization
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationRequest
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class RegisterGatewayTest : BaseDataTestCase() {

    private val pgwPreferences = mock<PublicGatewayPreferences>()
    private val mockFileStore = mock<FileStore>()
    private val localConfig = LocalConfig(
        mockFileStore, privateKeyStoreProvider, certificateStoreProvider, pgwPreferences
    )
    private val poWebClient = mock<PoWebClient>()
    private val poWebClientBuilder = object : PoWebClientBuilder {
        override suspend fun build(address: ServiceAddress) = poWebClient
    }
    private val resolveServiceAddress = mock<ResolveServiceAddress>()
    private val notifyEndpoints = mock<GatewayCertificateChangeNotifier>()
    private val registerGateway = RegisterGateway(
        notifyEndpoints,
        pgwPreferences,
        localConfig,
        poWebClientBuilder,
        resolveServiceAddress,
        publicKeyStore
    )

    @BeforeEach
    internal fun setUp() = runBlockingTest {
        registerPrivateGatewayIdentity()
        whenever(pgwPreferences.getPrivateAddress())
            .thenReturn(PDACertPath.PUBLIC_GW.subjectPrivateAddress)
    }

    @Test
    fun `failure to resolve PoWeb address should be ignored`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        val failingPoWebClientBuilder = object : PoWebClientBuilder {
            override suspend fun build(address: ServiceAddress) =
                throw PublicAddressResolutionException("Whoops")
        }
        val registerGateway = RegisterGateway(
            notifyEndpoints,
            pgwPreferences,
            localConfig,
            failingPoWebClientBuilder,
            resolveServiceAddress,
            publicKeyStore
        )

        registerGateway.registerIfNeeded()

        verify(poWebClient, never()).collectParcels(any(), any())
    }

    @Test
    internal fun `does not register if already registered and not expiring`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.Done)
        localConfig.setIdentityCertificate(
            issueGatewayCertificate(
                KeyPairSet.PRIVATE_GW.public,
                KeyPairSet.PUBLIC_GW.private,
                ZonedDateTime.now().plusYears(1), // not expiring soon
                validityStartDate = ZonedDateTime.now().minusSeconds(1)
            )
        )

        registerGateway.registerIfNeeded()

        verifyNoMoreInteractions(poWebClient)
    }

    @Test
    internal fun `registers if needs to renew certificate`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.Done)
        localConfig.setIdentityCertificate(
            issueGatewayCertificate(
                KeyPairSet.PRIVATE_GW.public,
                KeyPairSet.PUBLIC_GW.private,
                ZonedDateTime.now().plusDays(1), // expiring soon
                PDACertPath.PUBLIC_GW,
                validityStartDate = ZonedDateTime.now().minusSeconds(1)
            )
        )
        whenever(poWebClient.preRegisterNode(any()))
            .thenReturn(buildPNRR())
        whenever(poWebClient.registerNode(any()))
            .thenReturn(buildPNR(publicGatewaySessionKeyPair.sessionKey))

        registerGateway.registerIfNeeded()

        verify(poWebClient).preRegisterNode(any())
        verify(poWebClient).registerNode(any())
    }

    @Test
    fun `successful registration stores new values`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        val pnrr = buildPNRR()
        whenever(poWebClient.preRegisterNode(any())).thenReturn(pnrr)
        val pnr = buildPNR(publicGatewaySessionKeyPair.sessionKey)
        whenever(poWebClient.registerNode(any())).thenReturn(pnr)

        registerGateway.registerIfNeeded()

        verify(pgwPreferences).setPublicKey(eq(pnr.gatewayCertificate.subjectPublicKey))
        verify(pgwPreferences).setRegistrationState(eq(RegistrationState.Done))
        publicKeyStore.retrieve(pnr.gatewayCertificate.subjectPrivateAddress)
        assertEquals(pnr.privateNodeCertificate, localConfig.getIdentityCertificate())
    }

    @Test
    internal fun `unsuccessful registration does not store new values`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        whenever(poWebClient.preRegisterNode(any())).thenReturn(buildPNRR())
        whenever(poWebClient.registerNode(any())).thenThrow(ClientBindingException("Error"))

        assertEquals(RegisterGateway.Result.FailedToRegister, registerGateway.registerIfNeeded())

        verify(pgwPreferences, never()).setPublicKey(any())
        verify(pgwPreferences, never()).setRegistrationState(any())
        assertEquals(0, publicKeyStore.keys.size)
    }

    @Test
    fun `Registration missing public gateway session key should fail`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        val pnrr = buildPNRR()
        whenever(poWebClient.preRegisterNode(any())).thenReturn(pnrr)
        val pnr = buildPNR(null)
        whenever(poWebClient.registerNode(any())).thenReturn(pnr)

        assertEquals(RegisterGateway.Result.FailedToRegister, registerGateway.registerIfNeeded())

        verify(pgwPreferences, never()).setPublicKey(any())
        verify(pgwPreferences, never()).setRegistrationState(any())
        assertEquals(0, publicKeyStore.keys.size)
    }

    @Test
    fun `new certificate triggers notification`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.Done)
        val pnrr = buildPNRR()
        whenever(poWebClient.preRegisterNode(any())).thenReturn(pnrr)
        val pnr = buildPNR(publicGatewaySessionKeyPair.sessionKey)
        whenever(poWebClient.registerNode(any())).thenReturn(pnr)

        registerGateway.registerIfNeeded()

        verify(notifyEndpoints).notifyAll()
    }

    @Test
    fun `first certificate triggers does not trigger notification`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        val pnrr = buildPNRR()
        whenever(poWebClient.preRegisterNode(any())).thenReturn(pnrr)
        val pnr = buildPNR(publicGatewaySessionKeyPair.sessionKey)
        whenever(poWebClient.registerNode(any())).thenReturn(pnr)

        registerGateway.registerIfNeeded()

        verify(notifyEndpoints, never()).notifyAll()
    }

    private fun buildPNRR(): PrivateNodeRegistrationRequest {
        val authorization = PrivateNodeRegistrationAuthorization(
            ZonedDateTime.now().plusSeconds(3),
            "1234".toByteArray()
        )
        return PrivateNodeRegistrationRequest(
            KeyPairSet.PRIVATE_ENDPOINT.public,
            authorization.serialize(KeyPairSet.PRIVATE_GW.private)
        )
    }

    private fun buildPNR(publicGatewaySessionKey: SessionKey?) = PrivateNodeRegistration(
        PDACertPath.PRIVATE_GW,
        PDACertPath.PUBLIC_GW,
        publicGatewaySessionKey
    )
}
