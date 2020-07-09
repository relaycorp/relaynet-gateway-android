package tech.relaycorp.gateway.pdc.local

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ParcelCollectionWebSocketsTest {
    @Test
    @Disabled
    fun `Requests with Origin header should be refused`() {
    }

    @Nested
    inner class Handshake {
        @Test
        @Disabled
        fun `Challenge should be sent as soon as client connects`() {
        }

        @Test
        @Disabled
        fun `Connection should error out if challenge is not responded to`() {
        }

        @Test
        @Disabled
        fun `Connection should error out if response contains zero signatures`() {
        }

        @Test
        @Disabled
        fun `Connection should error out if response contains at least one invalid signature`() {
        }

        @Test
        @Disabled
        fun `Handshake should complete successfully if all signatures are valid`() {
        }
    }
}
