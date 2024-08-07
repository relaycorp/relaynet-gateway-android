package tech.relaycorp.gateway.domain.publicsync

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.doh.InternetAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.model.ServiceAddress
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
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
import tech.relaycorp.relaynet.wrappers.nodeId
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class RegisterGatewayTest : BaseDataTestCase() {

    private val pgwPreferences = mock<InternetGatewayPreferences>()
    private val localConfig = LocalConfig(
        privateKeyStoreProvider,
        certificateStoreProvider,
        pgwPreferences,
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
        publicKeyStore,
    )

    @BeforeEach
    internal fun setUp() = runTest {
        registerPrivateGatewayParcelDeliveryCertificate()
        whenever(pgwPreferences.getId())
            .thenReturn(PDACertPath.INTERNET_GW.subjectId)
    }

    @Test
    fun `failure to resolve PoWeb address should be ignored`() = runTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        val failingPoWebClientBuilder = object : PoWebClientBuilder {
            override suspend fun build(address: ServiceAddress) =
                throw InternetAddressResolutionException("Whoops")
        }
        val registerGateway = RegisterGateway(
            notifyEndpoints,
            pgwPreferences,
            localConfig,
            failingPoWebClientBuilder,
            resolveServiceAddress,
            publicKeyStore,
        )

        registerGateway.registerIfNeeded()

        verify(poWebClient, never()).collectParcels(any(), any())
    }

    @Test
    internal fun `does not register if already registered and not expiring`() = runTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.Done)
        localConfig.setParcelDeliveryCertificate(
            issueGatewayCertificate(
                KeyPairSet.PRIVATE_GW.public,
                KeyPairSet.INTERNET_GW.private,
                // not expiring soon
                ZonedDateTime.now().plusYears(1),
                validityStartDate = ZonedDateTime.now().minusSeconds(1),
            ),
        )

        registerGateway.registerIfNeeded()

        verifyNoMoreInteractions(poWebClient)
    }

    @Test
    internal fun `registers if needs to renew certificate`() = runTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.Done)
        localConfig.setParcelDeliveryCertificate(
            issueGatewayCertificate(
                KeyPairSet.PRIVATE_GW.public,
                KeyPairSet.INTERNET_GW.private,
                // expiring soon
                ZonedDateTime.now().plusDays(1),
                PDACertPath.INTERNET_GW,
                validityStartDate = ZonedDateTime.now().minusSeconds(1),
            ),
        )
        whenever(poWebClient.preRegisterNode(any()))
            .thenReturn(buildPNRR())
        whenever(poWebClient.registerNode(any()))
            .thenReturn(buildPNR(internetGatewaySessionKeyPair.sessionKey))

        registerGateway.registerIfNeeded()

        verify(poWebClient).preRegisterNode(any())
        verify(poWebClient).registerNode(any())
    }

    @Test
    fun `successful registration stores new values`() = runTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        val pnrr = buildPNRR()
        whenever(poWebClient.preRegisterNode(any())).thenReturn(pnrr)
        val pnr = buildPNR(internetGatewaySessionKeyPair.sessionKey)
        whenever(poWebClient.registerNode(any())).thenReturn(pnr)

        registerGateway.registerIfNeeded()

        verify(pgwPreferences).setPublicKey(eq(pnr.gatewayCertificate.subjectPublicKey))
        verify(pgwPreferences).setRegistrationState(eq(RegistrationState.Done))
        publicKeyStore.retrieve(
            pnr.privateNodeCertificate.subjectId,
            pnr.gatewayCertificate.subjectId,
        )
        assertEquals(pnr.privateNodeCertificate, localConfig.getParcelDeliveryCertificate())
    }

    @Test
    internal fun `unsuccessful registration does not store new values`() = runTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        whenever(poWebClient.preRegisterNode(any())).thenReturn(buildPNRR())
        whenever(poWebClient.registerNode(any())).thenThrow(ClientBindingException("Error"))

        assertEquals(RegisterGateway.Result.FailedToRegister, registerGateway.registerIfNeeded())

        verify(pgwPreferences, never()).setPublicKey(any())
        verify(pgwPreferences, never()).setRegistrationState(any())
        assertEquals(0, publicKeyStore.keys.size)
    }

    @Test
    fun `Registration missing public gateway session key should fail`() = runTest {
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
    fun `new certificate triggers notification`() = runTest {
        whenever(pgwPreferences.getAddress())
            .thenReturn(internetGatewaySessionKeyPair.sessionKey.publicKey.nodeId)
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.Done)
        val pnrr = buildPNRR()
        whenever(poWebClient.preRegisterNode(any())).thenReturn(pnrr)
        val pnr = buildPNR(internetGatewaySessionKeyPair.sessionKey)
        whenever(poWebClient.registerNode(any())).thenReturn(pnr)

        registerGateway.registerIfNeeded()

        verify(notifyEndpoints).notifyAll()
    }

    @Test
    fun `first certificate triggers does not trigger notification`() = runTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        val pnrr = buildPNRR()
        whenever(poWebClient.preRegisterNode(any())).thenReturn(pnrr)
        val pnr = buildPNR(internetGatewaySessionKeyPair.sessionKey)
        whenever(poWebClient.registerNode(any())).thenReturn(pnr)

        registerGateway.registerIfNeeded()

        verify(notifyEndpoints, never()).notifyAll()
    }

    private fun buildPNRR(): PrivateNodeRegistrationRequest {
        val authorization = PrivateNodeRegistrationAuthorization(
            ZonedDateTime.now().plusSeconds(3),
            "1234".toByteArray(),
        )
        return PrivateNodeRegistrationRequest(
            KeyPairSet.PRIVATE_ENDPOINT.public,
            authorization.serialize(KeyPairSet.PRIVATE_GW.private),
        )
    }

    private fun buildPNR(internetGatewaySessionKey: SessionKey?) = PrivateNodeRegistration(
        PDACertPath.PRIVATE_GW,
        PDACertPath.INTERNET_GW,
        "example.org",
        internetGatewaySessionKey,
    )
}
