package tech.relaycorp.gateway.pdc.local

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.pdc.local.utils.Handshake
import java.nio.charset.Charset

@Suppress("RedundantInnerClassModifier")
class HandshakeTest {
    @Nested
    inner class GenerateNonce {
        @Test
        fun `Nonce should be a UUID4`() {
            val nonce = Handshake.generateNonce()

            val nonceString = nonce.toString(Charset.forName("UTF8"))
            assertTrue(HandshakeTestUtils.UUID4_REGEX.matches(nonceString))
        }
    }
}
