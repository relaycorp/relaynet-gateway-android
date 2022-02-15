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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.endpoint.CollectParcels
import tech.relaycorp.gateway.pdc.local.HandshakeTestUtils
import tech.relaycorp.gateway.pdc.local.utils.ParcelCollectionHandshake
import tech.relaycorp.relaynet.bindings.pdc.DetachedSignatureType
import tech.relaycorp.relaynet.bindings.pdc.Signer
import tech.relaycorp.relaynet.bindings.pdc.StreamingMode
import tech.relaycorp.relaynet.messages.control.HandshakeChallenge
import tech.relaycorp.relaynet.messages.control.HandshakeResponse
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import java.nio.charset.Charset
import javax.inject.Provider
import kotlin.test.assertEquals

class ParcelCollectionHandshakeTest {
    private val collectParcels = mock<CollectParcels>()
    private val localConfig = mock<LocalConfig>()
    private val route =
        ParcelCollectionRoute(ParcelCollectionHandshake(localConfig), Provider { collectParcels })

    @BeforeEach
    internal fun setUp() = runBlockingTest {
        whenever(localConfig.getAllValidIdentityCertificates())
            .thenReturn(listOf(PDACertPath.PRIVATE_GW))
    }

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
        private val endpointSigner =
            Signer(PDACertPath.PRIVATE_ENDPOINT, KeyPairSet.PRIVATE_ENDPOINT.private)

        @Test
        fun `Challenge should be sent as soon as client connects`() {
            testPDCServerRoute(route) {
                handleWebSocketConversation(ParcelCollectionRoute.URL_PATH) { incoming, _ ->
                    val challengeRaw = incoming.receive()
                    assertEquals(FrameType.BINARY, challengeRaw.frameType)

                    val challenge = HandshakeChallenge.deserialize(challengeRaw.readBytes())
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

                    val response = HandshakeResponse(emptyList())
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
                    val challenge = HandshakeChallenge.deserialize(incoming.receive().readBytes())

                    val validSignature = endpointSigner.sign(
                        challenge.nonce,
                        DetachedSignatureType.NONCE
                    )
                    val invalidSignature = "not really a signature".toByteArray()
                    val response = HandshakeResponse(listOf(validSignature, invalidSignature))
                    outgoing.send(Frame.Binary(true, response.serialize()))

                    val closingFrameRaw = incoming.receive()
                    assertEquals(FrameType.CLOSE, closingFrameRaw.frameType)

                    val closingFrame = closingFrameRaw as Frame.Close
                    assertEquals(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        closingFrame.readReason()!!.knownReason
                    )
                    assertEquals(
                        "Handshake response included invalid nonce signatures or untrusted " +
                            "signers",
                        closingFrame.readReason()!!.message
                    )
                }
            }
        }

        @Test
        fun `Connection should error out if response contains an untrusted certificate`() =
            runBlockingTest {
                testPDCServerRoute(route) {
                    handleWebSocketConversation(
                        ParcelCollectionRoute.URL_PATH,
                        setup = {
                            addHeader(
                                StreamingMode.HEADER_NAME,
                                StreamingMode.CloseUponCompletion.headerValue
                            )
                        }
                    ) { incoming, outgoing ->
                        val challenge =
                            HandshakeChallenge.deserialize(incoming.receive().readBytes())

                        val untrustedSigner =
                            Signer(PDACertPath.PDA, KeyPairSet.PDA_GRANTEE.private)
                        val signature =
                            untrustedSigner.sign(challenge.nonce, DetachedSignatureType.NONCE)
                        val response = HandshakeResponse(listOf(signature))
                        outgoing.send(Frame.Binary(true, response.serialize()))

                        val closingFrameRaw = incoming.receive()
                        assertEquals(FrameType.CLOSE, closingFrameRaw.frameType)

                        val closingFrame = closingFrameRaw as Frame.Close
                        assertEquals(
                            CloseReason.Codes.CANNOT_ACCEPT,
                            closingFrame.readReason()!!.knownReason
                        )
                        assertEquals(
                            "Handshake response included invalid nonce signatures or untrusted " +
                                "signers",
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
                    .thenReturn(flowOf(false))

                testPDCServerRoute(route) {
                    handleWebSocketConversation(
                        ParcelCollectionRoute.URL_PATH,
                        setup = {
                            addHeader(
                                StreamingMode.HEADER_NAME,
                                StreamingMode.CloseUponCompletion.headerValue
                            )
                        }
                    ) { incoming, outgoing ->
                        val challenge =
                            HandshakeChallenge.deserialize(incoming.receive().readBytes())

                        val validSignature = endpointSigner.sign(
                            challenge.nonce,
                            DetachedSignatureType.NONCE
                        )
                        val response = HandshakeResponse(listOf(validSignature))
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
