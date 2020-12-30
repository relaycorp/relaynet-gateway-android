package tech.relaycorp.gateway.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.disk.SensitiveStore

internal class LocalConfigTest {

    private val sensitiveStore = mock<SensitiveStore>()
    private val localConfig = LocalConfig(sensitiveStore)

    @BeforeEach
    internal fun setUp() {
        runBlocking {
            var stored: ByteArray? = null
            whenever(sensitiveStore.store(any(), any())).then {
                stored = it.getArgument(1) as ByteArray
                Unit
            }
            whenever(sensitiveStore.read(any())).thenAnswer { stored }
        }
    }

    @Test
    internal fun `get key pair stores and recovers the same key pair`() = runBlockingTest {
        val keyPair1 = localConfig.getKeyPair()
        val keyPair2 = localConfig.getKeyPair()
        assertTrue(keyPair2.private.encoded!!.contentEquals(keyPair1.private.encoded))
        assertTrue(keyPair2.public.encoded!!.contentEquals(keyPair1.public.encoded))
    }

    @Test
    internal fun `get certificate stores and recovers the same certificate`() = runBlockingTest {
        val certificate1 = localConfig.getCertificate().serialize()
        val certificate2 = localConfig.getCertificate().serialize()
        assertTrue(certificate1.contentEquals(certificate2))
    }

    @Test
    internal fun `get CDA stores and recovers the same certificate`() = runBlockingTest {
        val certificate1 = localConfig.getCargoDeliveryAuth().serialize()
        val certificate2 = localConfig.getCargoDeliveryAuth().serialize()
        assertTrue(certificate1.contentEquals(certificate2))
    }
}
