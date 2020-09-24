package tech.relaycorp.gateway.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.disk.ReadRawFile
import tech.relaycorp.gateway.data.disk.SensitiveStore
import tech.relaycorp.relaynet.testing.CertificationPath
import tech.relaycorp.relaynet.testing.KeyPairSet

internal class LocalConfigTest {

    private val sensitiveStore = mock<SensitiveStore>()
    private val readRawFile = mock<ReadRawFile>()
    private val localConfig = LocalConfig(sensitiveStore, readRawFile)

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
        val keyPair = KeyPairSet.PRIVATE_GW
        whenever(readRawFile.read(any())).thenReturn(keyPair.private.encoded)

        val keyPair1 = localConfig.getKeyPair()
        assertTrue(keyPair1.private.encoded!!.contentEquals(keyPair.private.encoded))
        assertTrue(keyPair1.public.encoded!!.contentEquals(keyPair.public.encoded))

        val keyPair2 = localConfig.getKeyPair()
        assertTrue(keyPair2.private.encoded!!.contentEquals(keyPair.private.encoded))
        assertTrue(keyPair2.public.encoded!!.contentEquals(keyPair.public.encoded))
    }

    @Test
    internal fun `get certificate stores and recovers the same certificate`() = runBlockingTest {
        val certificate = CertificationPath.PRIVATE_GW
        whenever(readRawFile.read(any())).thenReturn(certificate.serialize())
        assertTrue(
            certificate.serialize().contentEquals(localConfig.getCertificate().serialize())
        )
        assertTrue(
            certificate.serialize().contentEquals(localConfig.getCertificate().serialize())
        )
    }
}
