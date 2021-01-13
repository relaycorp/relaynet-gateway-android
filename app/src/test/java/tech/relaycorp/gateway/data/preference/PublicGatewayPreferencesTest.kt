package tech.relaycorp.gateway.data.preference

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.tfcporciuncula.flow.FlowSharedPreferences
import com.tfcporciuncula.flow.StringPreference
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.gateway.data.disk.ReadRawFile
import tech.relaycorp.gateway.data.doh.PublicAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.ServiceAddress
import javax.inject.Provider
import kotlin.test.assertEquals

class PublicGatewayPreferencesTest {
    private val mockSharedPreferences = mock<FlowSharedPreferences>()
    private val mockReadRawFile = mock<ReadRawFile>()
    private val mockResolveServiceAddress = mock<ResolveServiceAddress>()
    private val gwPreferences = PublicGatewayPreferences(
        Provider { mockSharedPreferences },
        mockReadRawFile,
        mockResolveServiceAddress
    )

    private val publicGatewayAddress = "example.com"
    private val publicGatewayTargetHost = "poweb.example.com"
    private val publicGatewayTargetPort = 135
    private val mockPublicGatewayAddressPreference = mock<StringPreference>()

    @BeforeEach
    internal fun setUp() {
        runBlockingTest {
            whenever(mockPublicGatewayAddressPreference.get()).thenReturn(publicGatewayAddress)
            whenever(
                mockSharedPreferences
                    .getString("address", PublicGatewayPreferences.DEFAULT_ADDRESS)
            ).thenReturn(mockPublicGatewayAddressPreference)
        }
    }

    @Nested
    inner class GetPoWebURL {
        @Test
        fun `PoWebAddress should be resolved and returned`() = runBlockingTest {
            whenever(mockResolveServiceAddress.resolvePoWeb(any()))
                .thenReturn(ServiceAddress(publicGatewayTargetHost, publicGatewayTargetPort))

            val address = gwPreferences.getPoWebAddress()

            assertEquals(publicGatewayTargetHost, address.host)
            assertEquals(publicGatewayTargetPort, address.port)
        }

        @Test
        fun `PoWebAddress exception should be thrown as well`() = runBlockingTest {
            whenever(mockResolveServiceAddress.resolvePoWeb(any()))
                .thenThrow(PublicAddressResolutionException(""))

            assertThrows<PublicAddressResolutionException> {
                gwPreferences.getPoWebAddress()
            }
        }
    }
}
