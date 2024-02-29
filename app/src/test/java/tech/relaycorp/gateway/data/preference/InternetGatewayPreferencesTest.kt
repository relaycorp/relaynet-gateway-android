package tech.relaycorp.gateway.data.preference

import android.util.Base64
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.fredporciuncula.flow.preferences.Preference
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.gateway.data.doh.InternetAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.ServiceAddress
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.wrappers.nodeId
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InternetGatewayPreferencesTest {
    private val mockSharedPreferences = mock<FlowSharedPreferences>()
    private val mockResolveServiceAddress = mock<ResolveServiceAddress>()
    private val gwPreferences = InternetGatewayPreferences(
        { mockSharedPreferences },
        mockResolveServiceAddress,
    )

    private val internetGatewayAddress = "example.com"
    private val internetGatewayTargetHost = "poweb.example.com"
    private val internetGatewayTargetPort = 135
    private val mockInternetGatewayAddressPreference = mockStringPreference(internetGatewayAddress)
    private val emptyStringPreference = mockStringPreference("")

    @BeforeEach
    internal fun setUp() {
        runTest {
            whenever(
                mockSharedPreferences
                    .getString("address", InternetGatewayPreferences.DEFAULT_ADDRESS),
            ).thenReturn(mockInternetGatewayAddressPreference)
            whenever(mockSharedPreferences.getString(eq("public_gateway_public_key"), anyOrNull()))
                .thenReturn(emptyStringPreference)
            whenever(mockSharedPreferences.getString(eq("public_gateway_id"), anyOrNull()))
                .thenReturn(emptyStringPreference)
        }
    }

    @Nested
    inner class GetPoWebURL {
        @Test
        fun `PoWebAddress should be resolved and returned`() = runTest {
            whenever(mockResolveServiceAddress.resolvePoWeb(any()))
                .thenReturn(ServiceAddress(internetGatewayTargetHost, internetGatewayTargetPort))

            val address = gwPreferences.getPoWebAddress()

            assertEquals(internetGatewayTargetHost, address.host)
            assertEquals(internetGatewayTargetPort, address.port)
        }

        @Test
        fun `PoWebAddress exception should be thrown as well`() = runTest {
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
        fun `getPublicKey returns null when public is not set`() = runTest {
            assertNull(gwPreferences.getPublicKey())
        }

        @Test
        fun `getPublicKey returns shared preferences public key when set`() = runTest {
            val publicKey = KeyPairSet.INTERNET_GW.public
            val publicKeyPreference = mockStringPreference(
                Base64.encodeToString(publicKey.encoded, Base64.DEFAULT),
            )
            whenever(mockSharedPreferences.getString(eq("public_gateway_public_key"), anyOrNull()))
                .thenReturn(publicKeyPreference)

            val result = gwPreferences.getPublicKey()

            assertEquals(publicKey, result)
        }
    }

    @Nested
    inner class GetId {
        @Test
        fun `getId returns null when public key not set`() = runTest {
            assertNull(gwPreferences.getId())
        }

        @Test
        fun `getId returns public key node id`() = runTest {
            val publicKey = KeyPairSet.INTERNET_GW.public
            val publicKeyPreference = mockStringPreference(
                Base64.encodeToString(publicKey.encoded, Base64.DEFAULT),
            )
            whenever(mockSharedPreferences.getString(eq("public_gateway_public_key"), anyOrNull()))
                .thenReturn(publicKeyPreference)

            val address = gwPreferences.getId()

            assertEquals(publicKey.nodeId, address)
        }

        @Test
        fun `getId returns cached node id`() = runTest {
            val mockedPreference = mockStringPreference("private_address")
            whenever(
                mockSharedPreferences.getString(eq("public_gateway_id"), anyOrNull()),
            )
                .thenReturn(mockedPreference)

            val address = gwPreferences.getId()

            assertEquals("private_address", address)
        }
    }

    private fun mockStringPreference(value: String) = mock<Preference<String>> {
        whenever(it.asFlow()).thenReturn(flowOf(value))
        whenever(it.get()).thenReturn(value)
    }
}
