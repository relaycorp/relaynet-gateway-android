package tech.relaycorp.gateway.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.test.BaseDataTestCase
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LocalConfigTest : BaseDataTestCase() {

    private val internetGatewayPreferences = mock<InternetGatewayPreferences>()
    private val localConfig = LocalConfig(
        privateKeyStoreProvider, certificateStoreProvider, internetGatewayPreferences
    )

    @BeforeEach
    fun setUp() {
        runBlocking {
            whenever(internetGatewayPreferences.getId())
                .thenReturn(PDACertPath.INTERNET_GW.subjectId)
        }
    }

    @Nested
    inner class GetKeyPair {
        @Test
        fun `Key pair should be returned if it exists`() = runBlockingTest {
            localConfig.bootstrap()

            val retrievedKeyPair = localConfig.getIdentityKey()

            val storedKeyPair = privateKeyStore.retrieveAllIdentityKeys().first()
            assertEquals(storedKeyPair, retrievedKeyPair)
        }

        @Test
        fun `Exception should be thrown if key pair does not exist`() = runBlockingTest {
            val exception = assertThrows<RuntimeException> {
                localConfig.getIdentityKey()
            }

            assertEquals("No key pair was found", exception.message)
        }
    }

    @Nested
    inner class GetCargoDeliveryAuth {
        @Test
        fun `Certificate should be returned if it exists`() = runBlockingTest {
            localConfig.bootstrap()

            val certificate1 = localConfig.getCargoDeliveryAuth().serialize()
            val certificate2 = localConfig.getCargoDeliveryAuth().serialize()
            assertTrue(certificate1.contentEquals(certificate2))
        }

        @Test
        fun `New certificate is generated if none exists`() = runBlockingTest {
            localConfig.bootstrap()
            certificateStore.clear()

            assertNotNull(localConfig.getCargoDeliveryAuth())
        }
    }

    @Nested
    inner class Bootstrap {
        @Test
        fun `Key pair should be created if it doesn't already exist`() = runBlockingTest {
            localConfig.bootstrap()

            val keyPair = localConfig.getIdentityKey()

            val storedKeyPair = privateKeyStore.retrieveAllIdentityKeys().first()
            assertEquals(keyPair, storedKeyPair)
        }

        @Test
        fun `Key pair should not be created if it already exists`() = runBlockingTest {
            localConfig.bootstrap()
            val originalKeyPair = localConfig.getIdentityKey()

            localConfig.bootstrap()
            val keyPair = localConfig.getIdentityKey()

            assertEquals(originalKeyPair, keyPair)
        }

        @Test
        fun `Correct public gateway id used as issuer in set identity certificate `() =
            runBlockingTest {
                localConfig.bootstrap()

                verify(certificateStore).setCertificate(
                    any(),
                    any(),
                    any(),
                    eq(PDACertPath.INTERNET_GW.subjectId)
                )
            }

        @Test
        fun `CDA issuer should be created if it doesn't already exist`() = runBlockingTest {
            localConfig.bootstrap()

            localConfig.getCargoDeliveryAuth()
        }

        @Test
        fun `CDA issuer should not be created if it already exists`() = runBlockingTest {
            localConfig.bootstrap()
            val originalCDAIssuer = localConfig.getCargoDeliveryAuth()

            localConfig.bootstrap()
            val cdaIssuer = localConfig.getCargoDeliveryAuth()

            assertArrayEquals(originalCDAIssuer.serialize(), cdaIssuer.serialize())
        }
    }

    @Test
    internal fun deleteExpiredCertificates() = runBlockingTest {
        localConfig.deleteExpiredCertificates()

        verify(certificateStore).deleteExpired()
    }
}
