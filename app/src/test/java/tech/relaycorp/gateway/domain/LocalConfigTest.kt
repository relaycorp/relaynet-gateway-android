package tech.relaycorp.gateway.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.data.disk.SensitiveStore
import java.nio.charset.Charset

internal class LocalConfigTest {

    private val sensitiveStore = mock<SensitiveStore>()
    private val localConfig = LocalConfig(App().resources)

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
        assertEquals(
            keyPair1.private.encoded.toString(Charset.defaultCharset()),
            keyPair2.private.encoded.toString(Charset.defaultCharset())
        )
        assertEquals(
            keyPair1.public.encoded.toString(Charset.defaultCharset()),
            keyPair2.public.encoded.toString(Charset.defaultCharset())
        )
    }

    @Test
    internal fun `get certificate stores and recovers the same certificate`() = runBlockingTest {
        assertEquals(
            localConfig.getCertificate().serialize().toString(Charset.defaultCharset()),
            localConfig.getCertificate().serialize().toString(Charset.defaultCharset())
        )
    }
}
