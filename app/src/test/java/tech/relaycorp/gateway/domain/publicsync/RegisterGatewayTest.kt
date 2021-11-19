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
import tech.relaycorp.gateway.data.disk.SensitiveStore
import tech.relaycorp.gateway.data.doh.PublicAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.model.ServiceAddress
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.pdc.PoWebClientBuilder
import tech.relaycorp.gateway.test.BaseDataTestCase
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.SessionKey
import tech.relaycorp.relaynet.bindings.pdc.ClientBindingException
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistration
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationAuthorization
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationRequest
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class RegisterGatewayTest : BaseDataTestCase() {

    private val pgwPreferences = mock<PublicGatewayPreferences>()
    private val mockSensitiveKeyStore = mock<SensitiveStore>()
    private val localConfig = LocalConfig(mockSensitiveKeyStore, privateKeyStore)
    private val poWebClient = mock<PoWebClient>()
    private val poWebClientBuilder = object : PoWebClientBuilder {
        override suspend fun build(address: ServiceAddress) = poWebClient
    }
    private val resolveServiceAddress = mock<ResolveServiceAddress>()
    private val registerGateway = RegisterGateway(
        pgwPreferences,
        localConfig,
        poWebClientBuilder,
        resolveServiceAddress,
        publicKeyStore
    )

    @BeforeEach
    internal fun setUp() = runBlockingTest {
        registerPrivateGatewayIdentityKeyPair()
    }

    @Test
    fun `Failure to resolve PoWeb address should be ignored`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        val failingPoWebClientBuilder = object : PoWebClientBuilder {
            override suspend fun build(address: ServiceAddress) =
                throw PublicAddressResolutionException("Whoops")
        }
        val registerGateway = RegisterGateway(
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
    internal fun `does not register if not needed`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.Done)

        registerGateway.registerIfNeeded()

        verifyNoMoreInteractions(poWebClient)
    }

    @Test
    fun `successful registration stores new values`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        val pnrr = buildPNRR()
        whenever(poWebClient.preRegisterNode(any())).thenReturn(pnrr)
        val pnr = buildPNR(publicGatewaySessionKeyPair.sessionKey)
        whenever(poWebClient.registerNode(any())).thenReturn(pnr)

        registerGateway.registerIfNeeded()

        verify(pgwPreferences).setCertificate(eq(pnr.gatewayCertificate))
        verify(pgwPreferences).setRegistrationState(eq(RegistrationState.Done))
        publicKeyStore.retrieve(pnr.gatewayCertificate.subjectPrivateAddress)
        assertEquals(pnr.privateNodeCertificate, localConfig.getIdentityKeyPair().certificate)
    }

    @Test
    internal fun `unsuccessful registration does not store new values`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        whenever(poWebClient.preRegisterNode(any())).thenReturn(buildPNRR())
        whenever(poWebClient.registerNode(any())).thenThrow(ClientBindingException("Error"))

        assertEquals(RegisterGateway.Result.FailedToRegister, registerGateway.registerIfNeeded())

        verify(pgwPreferences, never()).setCertificate(any())
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

        verify(pgwPreferences, never()).setCertificate(any())
        verify(pgwPreferences, never()).setRegistrationState(any())
        assertEquals(0, publicKeyStore.keys.size)
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
