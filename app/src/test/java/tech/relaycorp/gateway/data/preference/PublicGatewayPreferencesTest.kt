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
import tech.relaycorp.doh.Answer
import tech.relaycorp.doh.DoHClient
import tech.relaycorp.doh.LookupFailureException
import tech.relaycorp.gateway.data.disk.ReadRawFile
import javax.inject.Provider
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PublicGatewayPreferencesTest {
    private val mockSharedPreferences = mock<FlowSharedPreferences>()
    private val mockReadRawFile = mock<ReadRawFile>()
    private val mockDoHClient = mock<DoHClient>()
    private val gwPreferences = PublicGatewayPreferences(
        Provider { mockSharedPreferences },
        mockReadRawFile,
        mockDoHClient
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
                mockSharedPreferences.getString("address", PublicGatewayPreferences.DEFAULT_ADDRESS)
            ).thenReturn(mockPublicGatewayAddressPreference)

            whenever(mockDoHClient.lookUp("_rgsc._tcp.$publicGatewayAddress", "SRV"))
                .thenReturn(
                    Answer(listOf("0 1 $publicGatewayTargetPort $publicGatewayTargetHost."))
                )
        }
    }

    @Nested
    inner class ResolvePoWebURL {
        @Test
        fun `Target host and port should be returned`() = runBlockingTest {
            val address = gwPreferences.resolvePoWebAddress()

            assertEquals(publicGatewayTargetHost, address.host)
            assertEquals(publicGatewayTargetPort, address.port)
        }

        @Test
        fun `SRV data with fewer than four fields should be refused`() = runBlockingTest {
            val malformedSRVData = "0 1 3"
            whenever(mockDoHClient.lookUp("_rgsc._tcp.$publicGatewayAddress", "SRV"))
                .thenReturn(Answer(listOf(malformedSRVData)))

            val exception = assertThrows<PublicAddressResolutionException> {
                gwPreferences.resolvePoWebAddress()
            }

            assertEquals(
                "Malformed SRV for $publicGatewayAddress ($malformedSRVData)",
                exception.message
            )
            assertNull(exception.cause)
        }

        @Test
        fun `Lookup errors should be wrapped`() = runBlockingTest {
            val lookupException = LookupFailureException("Whoops")
            whenever(mockDoHClient.lookUp(any(), any())).thenThrow(lookupException)

            val exception = assertThrows<PublicAddressResolutionException> {
                gwPreferences.resolvePoWebAddress()
            }

            assertEquals("Failed to resolve DNS for PoWeb address", exception.message)
            assertTrue(exception.cause is LookupFailureException)
        }
    }
}
