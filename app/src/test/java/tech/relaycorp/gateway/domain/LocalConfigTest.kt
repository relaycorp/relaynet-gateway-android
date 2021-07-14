package tech.relaycorp.gateway.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.gateway.data.disk.SensitiveStore
import kotlin.test.assertEquals

class LocalConfigTest {

    private val sensitiveStore = mock<SensitiveStore>()
    private val localConfig = LocalConfig(sensitiveStore)

    @BeforeEach
    fun setUp() {
        runBlocking {
            val memoryStore = mutableMapOf<String, ByteArray>()
            whenever(sensitiveStore.store(any(), any())).then {
                val key = it.getArgument<String>(0)
                val value = it.getArgument(1) as ByteArray
                memoryStore[key] = value
                Unit
            }
            whenever(sensitiveStore.read(any())).thenAnswer {
                val key = it.getArgument<String>(0)
                memoryStore[key]
            }
        }
    }

    @Nested
    inner class GetKeyPair {
        @Test
        fun `Key pair should be returned if it exists`() = runBlockingTest {
            val keyPair = localConfig.generateKeyPair()

            val retrievedKeyPair = localConfig.getKeyPair()

            assertEquals(
                keyPair.private.encoded.asList(),
                retrievedKeyPair.private.encoded.asList()
            )
        }

        @Test
        fun `Exception should be thrown if key pair does not exist`() = runBlockingTest {
            val exception = assertThrows<RuntimeException> {
                localConfig.getKeyPair()
            }

            assertEquals("No key pair was found", exception.message)
        }
    }

    @Test
    fun `get certificate stores and recovers the same certificate`() = runBlockingTest {
        localConfig.generateKeyPair()

        val certificate1 = localConfig.getCertificate().serialize()
        val certificate2 = localConfig.getCertificate().serialize()
        assertTrue(certificate1.contentEquals(certificate2))
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
        fun `Exception should be thrown if certificate does not exist yet`() = runBlockingTest {
            val exception = assertThrows<RuntimeException> {
                localConfig.getCargoDeliveryAuth()
            }

            assertEquals("No CDA issuer was found", exception.message)
        }
    }

    @Nested
    inner class Bootstrap {
        @Test
        fun `Key pair should be created if it doesn't already exist`() = runBlockingTest {
            localConfig.bootstrap()

            localConfig.getKeyPair()
        }

        @Test
        fun `Key pair should not be created if it already exists`() = runBlockingTest {
            localConfig.bootstrap()
            val originalKeyPair = localConfig.getKeyPair()

            localConfig.bootstrap()
            val keyPair = localConfig.getKeyPair()

            assertEquals(originalKeyPair.private.encoded.asList(), keyPair.private.encoded.asList())
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

            assertEquals(originalCDAIssuer, cdaIssuer)
        }
    }
}
