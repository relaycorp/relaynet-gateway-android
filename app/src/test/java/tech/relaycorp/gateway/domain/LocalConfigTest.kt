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
import tech.relaycorp.gateway.data.disk.FileStore
import tech.relaycorp.gateway.test.BaseDataTestCase
import kotlin.test.assertEquals

class LocalConfigTest : BaseDataTestCase() {

    private val fileStore = mock<FileStore>()
    private val localConfig = LocalConfig(fileStore, privateKeyStoreProvider)

    @BeforeEach
    fun setUp() {
        runBlocking {
            val memoryStore = mutableMapOf<String, ByteArray>()
            whenever(fileStore.store(any(), any())).then {
                val key = it.getArgument<String>(0)
                val value = it.getArgument(1) as ByteArray
                memoryStore[key] = value
                Unit
            }
            whenever(fileStore.read(any())).thenAnswer {
                val key = it.getArgument<String>(0)
                memoryStore[key]
            }
        }
    }

    @Nested
    inner class GetKeyPair {
        @Test
        fun `Key pair should be returned if it exists`() = runBlockingTest {
            localConfig.bootstrap()

            val retrievedKeyPair = localConfig.getIdentityKeyPair()

            val storedKeyPair = privateKeyStore.retrieveAllIdentityKeys().first()
            assertEquals(storedKeyPair, retrievedKeyPair)
        }

        @Test
        fun `Exception should be thrown if key pair does not exist`() = runBlockingTest {
            val exception = assertThrows<RuntimeException> {
                localConfig.getIdentityKeyPair()
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

            val keyPair = localConfig.getIdentityKeyPair()

            val storedKeyPair = privateKeyStore.retrieveAllIdentityKeys().first()
            assertEquals(keyPair, storedKeyPair)
        }

        @Test
        fun `Key pair should not be created if it already exists`() = runBlockingTest {
            localConfig.bootstrap()
            val originalKeyPair = localConfig.getIdentityKeyPair()

            localConfig.bootstrap()
            val keyPair = localConfig.getIdentityKeyPair()

            assertEquals(originalKeyPair, keyPair)
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
