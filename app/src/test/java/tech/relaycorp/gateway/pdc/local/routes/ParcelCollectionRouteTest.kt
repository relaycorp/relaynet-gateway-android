package tech.relaycorp.gateway.pdc.local.routes

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.FrameType
import io.ktor.http.cio.websocket.readReason
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.domain.endpoint.CollectParcels
import tech.relaycorp.gateway.pdc.local.utils.ParcelCollectionHandshake
import tech.relaycorp.gateway.test.WaitAssertions.suspendWaitFor
import tech.relaycorp.relaynet.messages.control.ParcelDelivery
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import javax.inject.Provider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.seconds

class ParcelCollectionRouteTest {

    private val parcelCollectionHandshake = mock<ParcelCollectionHandshake>()
    private val collectParcels = mock<CollectParcels>()
    private val route =
        ParcelCollectionRoute(parcelCollectionHandshake, Provider { collectParcels })

    private val endpointAddress = MessageAddress.of("1234")
    private val certificate = mock<Certificate>()
        .also { whenever(it.subjectPrivateAddress).thenReturn(endpointAddress.value) }

    @BeforeEach
    internal fun setUp() = runBlockingTest {
        whenever(parcelCollectionHandshake.handshake(any())).thenReturn(listOf(certificate))
        whenever(collectParcels.getNewParcelsForEndpoints(any())).thenReturn(emptyFlow())
        whenever(collectParcels.anyParcelsLeftToDeliverOrAck).thenReturn(flowOf(true))
    }

    @Test
    fun `Server should close if there is not Keep-Alive and no parcel to receive`() =
        runBlockingTest {
            whenever(collectParcels.anyParcelsLeftToDeliverOrAck).thenReturn(flowOf(false))

            testPDCServerRoute(route) {
                handleWebSocketConversation(
                    ParcelCollectionRoute.URL_PATH,
                    { addHeader(ParcelCollectionRoute.HEADER_KEEP_ALIVE, "off") }
                ) { incoming, _ ->
                    val closingFrameRaw = incoming.receive()
                    assertEquals(FrameType.CLOSE, closingFrameRaw.frameType)

                    val closingFrame = closingFrameRaw as Frame.Close
                    assertEquals(
                        CloseReason.Codes.NORMAL,
                        closingFrame.readReason()!!.knownReason
                    )
                    assertEquals(
                        "All available parcels delivered",
                        closingFrame.readReason()!!.message
                    )
                }
            }
        }

    @Test
    fun `Server should keep connection if Keep-Alive is on`() =
        runBlockingTest {
            whenever(collectParcels.anyParcelsLeftToDeliverOrAck).thenReturn(flowOf(true))

            testPDCServerRoute(route) {
                handleWebSocketConversation(
                    ParcelCollectionRoute.URL_PATH,
                    { addHeader(ParcelCollectionRoute.HEADER_KEEP_ALIVE, "on") }
                ) { incoming, _ ->
                    delay(3_000) // wait to see if the connection is kept alive
                    assertFalse(incoming.isClosedForReceive)
                }
            }
        }

    @Test
    fun `Server should send parcel to deliver`() =
        runBlockingTest {
            val parcel = Pair("abc", ByteArray(0).inputStream())
            whenever(collectParcels.getNewParcelsForEndpoints(any()))
                .thenReturn(flowOf(listOf(parcel)))

            testPDCServerRoute(route) {
                handleWebSocketConversation(
                    ParcelCollectionRoute.URL_PATH,
                    { addHeader(ParcelCollectionRoute.HEADER_KEEP_ALIVE, "off") }
                ) { incoming, _ ->
                    val parcelFrame = incoming.receive() as Frame.Binary
                    val parcelDelivery = ParcelDelivery.deserialize(parcelFrame.data)
                    assertEquals(parcel.first, parcelDelivery.deliveryId)
                }
            }
        }

    @Test
    fun `Server should process client parcel acks`() =
        runBlockingTest {
            testPDCServerRoute(route) {
                handleWebSocketConversation(
                    ParcelCollectionRoute.URL_PATH,
                    { addHeader(ParcelCollectionRoute.HEADER_KEEP_ALIVE, "off") }
                ) { _, outgoing ->
                    val localId = "abc"

                    outgoing.send(Frame.Text(localId))

                    suspendWaitFor {
                        verify(collectParcels).processParcelAck(eq(localId))
                    }
                }
            }
        }
}
