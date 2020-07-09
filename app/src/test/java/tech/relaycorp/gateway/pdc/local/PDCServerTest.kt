package tech.relaycorp.gateway.pdc.local

import io.ktor.application.Application
import io.ktor.http.cio.websocket.FrameType
import io.ktor.http.cio.websocket.readBytes
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.relaycorp.poweb.handshake.Challenge
import java.nio.charset.Charset
import kotlin.test.assertEquals

class ParcelCollectionWebSocketsTest {
    @Test
    @Disabled
    fun `Requests with Origin header should be refused`() {
    }

    @Nested
    inner class Handshake {
        @Test
        fun `Challenge should be sent as soon as client connects`() {
            withTestApplication(Application::main) {
                handleWebSocketConversation("/v1/parcel-collection") { incoming, _ ->
                    val challengeRaw = incoming.receive()
                    assertEquals(FrameType.BINARY, challengeRaw.frameType)
                    val challenge = Challenge.deserialize(challengeRaw.readBytes())
                    val nonceString = challenge.nonce.toString(Charset.forName("UTF8"))
                    Assertions.assertTrue(TestUtils.UUID4_REGEX.matches(nonceString))
                }
            }
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
