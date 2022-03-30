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
import tech.relaycorp.gateway.data.doh.PublicAddressResolutionException
import tech.relaycorp.gateway.data.doh.ResolveServiceAddress
import tech.relaycorp.gateway.data.model.ServiceAddress
import tech.relaycorp.relaynet.testing.pki.PDACertPath
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
    private val mockPublicGatewayAddressPreference = mock<Preference<String>>()
    private val emptyStringPreference = mock<Preference<String>> {
        whenever(it.asFlow()).thenReturn(flowOf(""))
        whenever(it.get()).thenReturn("")
    }

    @BeforeEach
    internal fun setUp() {
        runBlockingTest {
            whenever(mockPublicGatewayAddressPreference.get()).thenReturn(publicGatewayAddress)
            whenever(
                mockSharedPreferences
                    .getString("address", PublicGatewayPreferences.DEFAULT_ADDRESS)
            ).thenReturn(mockPublicGatewayAddressPreference)
            whenever(mockSharedPreferences.getString(eq("public_gateway_public_key"), anyOrNull()))
                .thenReturn(emptyStringPreference)
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

    @Nested
    inner class GetPublicKey {
        @Test
        fun `getPublicKey returns certificate public key`() = runBlockingTest {
            whenever(mockReadRawFile.read(any())).thenReturn(PDACertPath.PUBLIC_GW.serialize())

            val publicKey = gwPreferences.getPublicKey()

            assertEquals(PDACertPath.PUBLIC_GW.subjectPublicKey, publicKey)
        }
    }

    @Nested
    inner class GetPrivateAddress {
        @Test
        fun `getPrivateAddress returns certificate private address`() = runBlockingTest {
            whenever(
                mockSharedPreferences.getString(
                    eq("public_gateway_private_address"),
                    anyOrNull()
                )
            )
                .thenReturn(emptyStringPreference)
            whenever(mockReadRawFile.read(any())).thenReturn(PDACertPath.PUBLIC_GW.serialize())

            val address = gwPreferences.getPrivateAddress()

            assertEquals(PDACertPath.PUBLIC_GW.subjectPrivateAddress, address)
        }

        @Test
        fun `getPrivateAddress returns cached private address`() = runBlockingTest {
            val preference = mock<Preference<String>> {
                whenever(it.get()).thenReturn("private_address")
            }
            whenever(
                mockSharedPreferences.getString(eq("public_gateway_private_address"), anyOrNull())
            )
                .thenReturn(preference)

            val address = gwPreferences.getPrivateAddress()

            assertEquals("private_address", address)
        }
    }
}
