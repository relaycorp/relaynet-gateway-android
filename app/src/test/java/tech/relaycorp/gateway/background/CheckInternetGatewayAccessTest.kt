package tech.relaycorp.gateway.background

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.doh.InternetAddressResolutionException
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.data.model.ServiceAddress
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckInternetGatewayAccessTest {
    private val mockGatewayPreferences = mock<InternetGatewayPreferences>()
    private val mockPingRemoteServer = mock<PingRemoteServer>()
    private val checkAccess = CheckInternetGatewayAccess(mockGatewayPreferences, mockPingRemoteServer)

    private val powebAddress = ServiceAddress("poweb.example.com", 443)
    private val powebURL = "https://${powebAddress.host}:${powebAddress.port}"

    @Test
    fun `False should be returned if the address could not be resolved`() = runBlockingTest {
        whenever(mockGatewayPreferences.getPoWebAddress())
            .thenThrow(InternetAddressResolutionException("Whoops"))

        assertFalse(checkAccess.check())
        verify(mockPingRemoteServer, never()).pingURL(any())
    }

    @Test
    fun `False should be returned if the ping failed`() = runBlockingTest {
        whenever(mockGatewayPreferences.getPoWebAddress()).thenReturn(powebAddress)
        whenever(mockPingRemoteServer.pingURL(powebURL)).thenReturn(false)

        assertFalse(checkAccess.check())
    }

    @Test
    fun `True should be returned if the ping succeeded`() = runBlockingTest {
        whenever(mockGatewayPreferences.getPoWebAddress()).thenReturn(powebAddress)
        whenever(mockPingRemoteServer.pingURL(powebURL)).thenReturn(true)

        assertTrue(checkAccess.check())
    }
}
