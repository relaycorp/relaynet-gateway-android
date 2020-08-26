package tech.relaycorp.gateway.pdc.local.routes

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.FrameType
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readReason
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.domain.endpoint.CollectParcels
import tech.relaycorp.gateway.pdc.local.HandshakeTestUtils
import tech.relaycorp.gateway.pdc.local.utils.ParcelCollectionHandshake
import tech.relaycorp.poweb.handshake.Challenge
import tech.relaycorp.poweb.handshake.Response
import tech.relaycorp.relaynet.issueEndpointCertificate
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import java.nio.charset.Charset
import javax.inject.Provider
import kotlin.test.assertEquals

class ParcelCollectionHandshakeTest {
    private val endpointKeyPair = generateRSAKeyPair()
    private val endpointCertificate = issueEndpointCertificate(
        endpointKeyPair.public,
        endpointKeyPair.private,
        nowInUtc().plusDays(1)
    )
    private val collectParcels = mock<CollectParcels>()
    private val route =
        ParcelCollectionRoute(ParcelCollectionHandshake(), Provider { collectParcels })

    @Test
    fun `Requests with Origin header should be refused`() {
        testPDCServerRoute(route) {

            handleWebSocketConversation(
                ParcelCollectionRoute.URL_PATH,
                { addHeader(ParcelCollectionRoute.HEADER_ORIGIN, "http://example.com") }
            ) { incoming, _ ->
                val closingFrameRaw = incoming.receive()
                assertEquals(FrameType.CLOSE, closingFrameRaw.frameType)

                val closingFrame = closingFrameRaw as Frame.Close
                assertEquals(
                    CloseReason.Codes.VIOLATED_POLICY,
                    closingFrame.readReason()!!.knownReason
                )
                assertEquals(
                    "Web browser requests are disabled for security reasons",
                    closingFrame.readReason()!!.message
                )
            }
        }
    }

    @Nested
    inner class Handshake {
        @Test
        fun `Challenge should be sent as soon as client connects`() {
            testPDCServerRoute(route) {
                handleWebSocketConversation(ParcelCollectionRoute.URL_PATH) { incoming, _ ->
                    val challengeRaw = incoming.receive()
                    assertEquals(FrameType.BINARY, challengeRaw.frameType)

                    val challenge = Challenge.deserialize(challengeRaw.readBytes())
                    val nonceString = challenge.nonce.toString(Charset.forName("UTF8"))
                    Assertions.assertTrue(HandshakeTestUtils.UUID4_REGEX.matches(nonceString))
                }
            }
        }

        @Test
        fun `Connection should error out if response is invalid`() {
            testPDCServerRoute(route) {
                handleWebSocketConversation(ParcelCollectionRoute.URL_PATH) { incoming, outgoing ->
                    // Ignore the challenge
                    incoming.receive()

                    // Send an invalid response
                    outgoing.send(Frame.Text("invalid response"))

                    val closingFrameRaw = incoming.receive()
                    assertEquals(FrameType.CLOSE, closingFrameRaw.frameType)

                    val closingFrame = closingFrameRaw as Frame.Close
                    assertEquals(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        closingFrame.readReason()!!.knownReason
                    )
                    assertEquals(
                        "Invalid handshake response",
                        closingFrame.readReason()!!.message
                    )
                }
            }
        }

        @Test
        fun `Connection should error out if response contains zero signatures`() {
            testPDCServerRoute(route) {
                handleWebSocketConversation(ParcelCollectionRoute.URL_PATH) { incoming, outgoing ->
                    // Ignore the challenge because we're not signing its nonce
                    incoming.receive()

                    val response = Response(emptyArray())
                    outgoing.send(Frame.Binary(true, response.serialize()))

                    val closingFrameRaw = incoming.receive()
                    assertEquals(FrameType.CLOSE, closingFrameRaw.frameType)

                    val closingFrame = closingFrameRaw as Frame.Close
                    assertEquals(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        closingFrame.readReason()!!.knownReason
                    )
                    assertEquals(
                        "Handshake response did not include any nonce signatures",
                        closingFrame.readReason()!!.message
                    )
                }
            }
        }

        @Test
        fun `Connection should error out if response contains at least one invalid signature`() {
            testPDCServerRoute(route) {
                handleWebSocketConversation(ParcelCollectionRoute.URL_PATH) { incoming, outgoing ->
                    val challenge = Challenge.deserialize(incoming.receive().readBytes())

                    val validSignature = HandshakeTestUtils.sign(
                        challenge.nonce,
                        endpointKeyPair.private,
                        endpointCertificate
                    )
                    val invalidSignature = "not really a signature".toByteArray()
                    val response = Response(arrayOf(validSignature, invalidSignature))
                    outgoing.send(Frame.Binary(true, response.serialize()))

                    val closingFrameRaw = incoming.receive()
                    assertEquals(FrameType.CLOSE, closingFrameRaw.frameType)

                    val closingFrame = closingFrameRaw as Frame.Close
                    assertEquals(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        closingFrame.readReason()!!.knownReason
                    )
                    assertEquals(
                        "Handshake response included invalid nonce signatures",
                        closingFrame.readReason()!!.message
                    )
                }
            }
        }

        @Test
        fun `Handshake should complete successfully if all signatures are valid`() =
            runBlockingTest {
                whenever(collectParcels.getNewParcelsForEndpoints(any()))
                    .thenReturn(flowOf(emptyList()))
                whenever(collectParcels.anyParcelsLeftToDeliverOrAck)
                    .thenReturn(flowOf(true))

                testPDCServerRoute(route) {
                    handleWebSocketConversation(
                        ParcelCollectionRoute.URL_PATH,
                        setup = { addHeader(ParcelCollectionRoute.HEADER_KEEP_ALIVE, "off") }
                    ) { incoming, outgoing ->
                        val challenge = Challenge.deserialize(incoming.receive().readBytes())

                        val validSignature = HandshakeTestUtils.sign(
                            challenge.nonce,
                            endpointKeyPair.private,
                            endpointCertificate
                        )
                        val response = Response(arrayOf(validSignature))
                        outgoing.send(Frame.Binary(true, response.serialize()))

                        val closingFrameRaw = incoming.receive()
                        assertEquals(FrameType.CLOSE, closingFrameRaw.frameType)

                        val closingFrame = closingFrameRaw as Frame.Close
                        assertEquals(
                            CloseReason.Codes.NORMAL,
                            closingFrame.readReason()!!.knownReason
                        )
                    }
                }
            }
    }
}
