package tech.relaycorp.gateway.data.preference

import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.fredporciuncula.flow.preferences.Preference
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.gateway.data.disk.ReadRawFile
import tech.relaycorp.gateway.data.doh.InternetAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.ServiceAddress
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import javax.inject.Provider
import kotlin.test.assertEquals

class InternetGatewayPreferencesTest {
    private val mockSharedPreferences = mock<FlowSharedPreferences>()
    private val mockReadRawFile = mock<ReadRawFile>()
    private val mockResolveServiceAddress = mock<ResolveServiceAddress>()
    private val gwPreferences = InternetGatewayPreferences(
        Provider { mockSharedPreferences },
        mockReadRawFile,
        mockResolveServiceAddress
    )

    private val internetGatewayAddress = "example.com"
    private val internetGatewayTargetHost = "poweb.example.com"
    private val internetGatewayTargetPort = 135
    private val mockInternetGatewayAddressPreference = mock<Preference<String>>()
    private val emptyStringPreference = mock<Preference<String>> {
        whenever(it.asFlow()).thenReturn(flowOf(""))
        whenever(it.get()).thenReturn("")
    }

    @BeforeEach
    internal fun setUp() {
        runBlockingTest {
            whenever(mockInternetGatewayAddressPreference.get()).thenReturn(internetGatewayAddress)
            whenever(
                mockSharedPreferences
                    .getString("address", InternetGatewayPreferences.DEFAULT_ADDRESS)
            ).thenReturn(mockInternetGatewayAddressPreference)
            whenever(mockSharedPreferences.getString(eq("public_gateway_public_key"), anyOrNull()))
                .thenReturn(emptyStringPreference)
        }
    }

    @Nested
    inner class GetPoWebURL {
        @Test
        fun `PoWebAddress should be resolved and returned`() = runBlockingTest {
            whenever(mockResolveServiceAddress.resolvePoWeb(any()))
                .thenReturn(ServiceAddress(internetGatewayTargetHost, internetGatewayTargetPort))

            val address = gwPreferences.getPoWebAddress()

            assertEquals(internetGatewayTargetHost, address.host)
            assertEquals(internetGatewayTargetPort, address.port)
        }

        @Test
        fun `PoWebAddress exception should be thrown as well`() = runBlockingTest {
            whenever(mockResolveServiceAddress.resolvePoWeb(any()))
                .thenThrow(InternetAddressResolutionException(""))

            assertThrows<InternetAddressResolutionException> {
                gwPreferences.getPoWebAddress()
            }
        }
    }

    @Nested
    inner class GetPublicKey {
        @Test
        fun `getPublicKey returns certificate public key`() = runBlockingTest {
            whenever(mockReadRawFile.read(any())).thenReturn(PDACertPath.INTERNET_GW.serialize())

            val publicKey = gwPreferences.getPublicKey()

            assertEquals(PDACertPath.INTERNET_GW.subjectPublicKey, publicKey)
        }
    }

    @Nested
    inner class GetId {
        @Test
        fun `getId returns certificate node id`() = runBlockingTest {
            whenever(
                mockSharedPreferences.getString(
                    eq("public_gateway_id"),
                    anyOrNull()
                )
            )
                .thenReturn(emptyStringPreference)
            whenever(mockReadRawFile.read(any())).thenReturn(PDACertPath.INTERNET_GW.serialize())

            val address = gwPreferences.getId()

            assertEquals(PDACertPath.INTERNET_GW.subjectId, address)
        }

        @Test
        fun `getId returns cached node id`() = runBlockingTest {
            val preference = mock<Preference<String>> {
                whenever(it.get()).thenReturn("private_address")
            }
            whenever(
                mockSharedPreferences.getString(eq("public_gateway_id"), anyOrNull())
            )
                .thenReturn(preference)

            val address = gwPreferences.getId()

            assertEquals("private_address", address)
        }
    }
}
