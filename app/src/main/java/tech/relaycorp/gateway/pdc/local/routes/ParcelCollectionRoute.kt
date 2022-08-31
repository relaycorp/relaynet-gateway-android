package tech.relaycorp.gateway.pdc.local.routes

import androidx.annotation.VisibleForTesting
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.request.header
import io.ktor.routing.Routing
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.webSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.domain.endpoint.CollectParcels
import tech.relaycorp.gateway.pdc.local.utils.ParcelCollectionHandshake
import tech.relaycorp.relaynet.bindings.pdc.StreamingMode
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.messages.control.ParcelDelivery
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Provider

class ParcelCollectionRoute
@Inject constructor(
    private val parcelCollectionHandshake: ParcelCollectionHandshake,
    private val collectParcelsProvider: Provider<CollectParcels>
) : PDCServerRoute {

    @VisibleForTesting
    var logger: Logger = Logger.getLogger(javaClass.name)

    private val asyncJob = SupervisorJob()
    private val asyncScope = CoroutineScope(asyncJob)

    override fun register(routing: Routing) {
        routing.webSocket(URL_PATH) {
            try {
                handle()
            } catch (exc: ClosedReceiveChannelException) {
                val reason = closeReason.await()
                if (reason == null) {
                    logger.log(Level.WARNING, "TCP connection closed abruptly", exc)
                } else if (reason.knownReason != CloseReason.Codes.NORMAL) {
                    logger.log(Level.WARNING, "TCP connection closed due to $reason", exc)
                }
            } catch (exc: PoWebException) {
                logger.log(Level.WARNING, "Connection closed without a normal reason", exc)
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.handle() {
        if (call.request.header(HEADER_ORIGIN) != null) {
            // The client is most likely a (malicious) web page
            close(
                CloseReason(
                    CloseReason.Codes.VIOLATED_POLICY,
                    "Web browser requests are disabled for security reasons"
                )
            )
            return
        }

        val certificates = try {
            parcelCollectionHandshake.handshake(this)
        } catch (e: ParcelCollectionHandshake.HandshakeUnsuccessful) {
            return
        }

        val collectParcels = collectParcelsProvider.get()
        val sendJob = sendParcels(collectParcels, certificates.toAddresses())
        val receiveJob = receiveAcks(collectParcels)

        val keepAlive =
            call.request.header(StreamingMode.HEADER_NAME) == StreamingMode.KeepAlive.headerValue
        if (!keepAlive) {
            collectParcels
                .anyParcelsLeftToDeliverOrAck
                .onEach { anyParcelsLeft ->
                    if (!anyParcelsLeft) {
                        close(
                            CloseReason(
                                CloseReason.Codes.NORMAL,
                                "All available parcels delivered"
                            )
                        )
                        asyncJob.complete()
                    }
                }
                .launchIn(asyncScope)
        }

        sendJob.join()
        receiveJob.join()

        // The server must've closed the connection for us to get here, since we're consuming
        // all incoming messages indefinitely.
        val reason = closeReason.await()
        val closeCode = reason?.code // Freeze reason.code value as it can change
        if (closeCode != CloseReason.Codes.NORMAL.code) {
            throw ServerConnectionException(
                "Server closed the connection unexpectedly " +
                    "(code: $closeCode, reason: ${reason?.message})"
            )
        }
    }

    private fun List<Certificate>.toAddresses() =
        map { MessageAddress.of(it.subjectId) }

    private suspend fun DefaultWebSocketServerSession.sendParcels(
        collectParcels: CollectParcels,
        addresses: List<MessageAddress>
    ) =
        collectParcels.getNewParcelsForEndpoints(addresses)
            .onEach { parcels ->
                parcels.iterator().forEach { (localId, parcelStream) ->
                    val parcelDelivery =
                        ParcelDelivery(localId, parcelStream.readBytesAndClose())
                    outgoing.send(
                        Frame.Binary(true, parcelDelivery.serialize())
                    )
                }
            }
            .launchIn(asyncScope)

    private suspend fun DefaultWebSocketServerSession.receiveAcks(collectParcels: CollectParcels) =
        incoming.receiveAsFlow()
            .onEach { frame ->
                if (frame !is Frame.Text) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid ack"))
                    asyncJob.complete()
                    return@onEach
                }

                val localId = frame.readText()
                collectParcels.processParcelAck(localId)
            }
            .launchIn(asyncScope)

    companion object {
        const val URL_PATH = "/v1/parcel-collection"
        const val HEADER_ORIGIN = "Origin"
    }
}
