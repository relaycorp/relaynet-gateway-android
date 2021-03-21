package tech.relaycorp.gateway.data.doh

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.doh.Answer
import tech.relaycorp.doh.DoHClient
import tech.relaycorp.doh.LookupFailureException

class ResolveServiceAddressTest {
    private val mockDoHClient = mock<DoHClient>()
    private val subject = ResolveServiceAddress(mockDoHClient)

    private val publicGatewayAddress = "example.com"
    private val publicGatewayTargetHost = "poweb.example.com"
    private val publicGatewayTargetPort = 135

    @Nested
    inner class ResolvePoWebAddress {
        @Test
        fun `Target host and port should be returned`() = runBlockingTest {
            whenever(mockDoHClient.lookUp("_awala-gsc._tcp.$publicGatewayAddress", "SRV"))
                .thenReturn(
                    Answer(listOf("0 1 $publicGatewayTargetPort $publicGatewayTargetHost."))
                )
            val address = subject.resolvePoWeb(publicGatewayAddress)

            kotlin.test.assertEquals(publicGatewayTargetHost, address.host)
            kotlin.test.assertEquals(publicGatewayTargetPort, address.port)
        }

        @Test
        fun `SRV data with fewer than four fields should be refused`() = runBlockingTest {
            val malformedSRVData = "0 1 3"
            whenever(mockDoHClient.lookUp("_awala-gsc._tcp.$publicGatewayAddress", "SRV"))
                .thenReturn(Answer(listOf(malformedSRVData)))

            val exception = assertThrows<PublicAddressResolutionException> {
                subject.resolvePoWeb(publicGatewayAddress)
            }

            kotlin.test.assertEquals(
                "Malformed SRV for $publicGatewayAddress ($malformedSRVData)",
                exception.message
            )
            kotlin.test.assertNull(exception.cause)
        }

        @Test
        fun `Lookup errors should be wrapped`() = runBlockingTest {
            val lookupException = LookupFailureException("Whoops")
            whenever(mockDoHClient.lookUp(any(), any())).thenThrow(lookupException)

            val exception = assertThrows<PublicAddressResolutionException> {
                subject.resolvePoWeb(publicGatewayAddress)
            }

            kotlin.test.assertEquals("Failed to resolve DNS for PoWeb address", exception.message)
            kotlin.test.assertTrue(exception.cause is LookupFailureException)
        }
    }
}
