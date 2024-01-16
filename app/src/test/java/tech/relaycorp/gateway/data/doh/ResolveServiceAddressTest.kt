package tech.relaycorp.gateway.data.doh

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.doh.Answer
import tech.relaycorp.doh.DoHClient
import tech.relaycorp.doh.LookupFailureException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResolveServiceAddressTest {
    private val mockDoHClient = mock<DoHClient>()
    private val subject = ResolveServiceAddress(mockDoHClient)

    private val internetGatewayAddress = "example.com"
    private val internetGatewayTargetHost = "poweb.example.com"
    private val internetGatewayTargetPort = 135

    @Nested
    inner class ResolvePoWebAddress {
        @Test
        fun `Target host and port should be returned`() = runTest {
            whenever(mockDoHClient.lookUp("_awala-gsc._tcp.$internetGatewayAddress", "SRV"))
                .thenReturn(
                    Answer(listOf("0 1 $internetGatewayTargetPort $internetGatewayTargetHost.")),
                )
            val address = subject.resolvePoWeb(internetGatewayAddress)

            assertEquals(internetGatewayTargetHost, address.host)
            assertEquals(internetGatewayTargetPort, address.port)
        }

        @Test
        fun `SRV data with fewer than four fields should be refused`() = runTest {
            val malformedSRVData = "0 1 3"
            whenever(mockDoHClient.lookUp("_awala-gsc._tcp.$internetGatewayAddress", "SRV"))
                .thenReturn(Answer(listOf(malformedSRVData)))

            val exception = assertThrows<InternetAddressResolutionException> {
                subject.resolvePoWeb(internetGatewayAddress)
            }

            assertEquals(
                "Malformed SRV for $internetGatewayAddress ($malformedSRVData)",
                exception.message,
            )
            assertNull(exception.cause)
        }

        @Test
        fun `Lookup errors should be wrapped`() = runTest {
            val lookupException = LookupFailureException("Whoops")
            whenever(mockDoHClient.lookUp(any(), any())).thenThrow(lookupException)

            val exception = assertThrows<InternetAddressResolutionException> {
                subject.resolvePoWeb(internetGatewayAddress)
            }

            assertEquals("Failed to resolve DNS for PoWeb address", exception.message)
            assertTrue(exception.cause is LookupFailureException)
        }
    }
}
