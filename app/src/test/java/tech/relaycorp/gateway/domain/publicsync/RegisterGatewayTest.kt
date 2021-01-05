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
import tech.relaycorp.gateway.data.doh.PublicAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.model.ServiceAddress
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.pdc.PoWebClientBuilder
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.bindings.pdc.ClientBindingException
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistration
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationAuthorization
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationRequest
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import java.time.ZonedDateTime

class RegisterGatewayTest {

    private val pgwPreferences = mock<PublicGatewayPreferences>()
    private val localConfig = mock<LocalConfig>()
    private val poWebClient = mock<PoWebClient>()
    private val poWebClientBuilder = object : PoWebClientBuilder {
        override suspend fun build(address: ServiceAddress) = poWebClient
    }
    private val resolveServiceAddress = mock<ResolveServiceAddress>()
    private val registerGateway =
        RegisterGateway(pgwPreferences, localConfig, poWebClientBuilder, resolveServiceAddress)

    @BeforeEach
    internal fun setUp() = runBlockingTest {
        whenever(localConfig.getKeyPair()).thenReturn(generateRSAKeyPair())
    }

    @Test
    fun `Failure to resolve PoWeb address should be ignored`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        val failingPoWebClientBuilder = object : PoWebClientBuilder {
            override suspend fun build(address: ServiceAddress) =
                throw PublicAddressResolutionException("Whoops")
        }
        val registerGateway = RegisterGateway(
            pgwPreferences, localConfig, failingPoWebClientBuilder, resolveServiceAddress
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
    internal fun `successful registration stores new values`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        val pnrr = buildPNRR()
        whenever(poWebClient.preRegisterNode(any())).thenReturn(pnrr)
        val pnr = buildPNR()
        whenever(poWebClient.registerNode(any())).thenReturn(pnr)

        registerGateway.registerIfNeeded()

        verify(localConfig).setCertificate(eq(pnr.privateNodeCertificate))
        verify(pgwPreferences).setCertificate(eq(pnr.gatewayCertificate))
        verify(pgwPreferences).setRegistrationState(eq(RegistrationState.Done))
    }

    @Test
    internal fun `unsuccessful registration does not store new values`() = runBlockingTest {
        whenever(pgwPreferences.getRegistrationState()).thenReturn(RegistrationState.ToDo)
        whenever(poWebClient.preRegisterNode(any())).thenReturn(buildPNRR())
        whenever(poWebClient.registerNode(any())).thenThrow(ClientBindingException("Error"))

        registerGateway.registerIfNeeded()

        verify(localConfig, never()).setCertificate(any())
        verify(pgwPreferences, never()).setCertificate(any())
        verify(pgwPreferences, never()).setRegistrationState(any())
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

    private fun buildPNR() = PrivateNodeRegistration(
        PDACertPath.PRIVATE_ENDPOINT,
        PDACertPath.PRIVATE_GW
    )
}
