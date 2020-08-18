package tech.relaycorp.gateway.pdc.local.routes

import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.close
import io.ktor.request.header
import io.ktor.routing.Routing
import io.ktor.websocket.webSocket
import tech.relaycorp.gateway.data.model.PrivateMessageAddress
import tech.relaycorp.gateway.pdc.local.utils.ParcelCollectionHandshake
import javax.inject.Inject

class ParcelCollectionRoute
@Inject constructor(
    private val parcelCollectionHandshake: ParcelCollectionHandshake
) : PDCServerRoute {
    override fun register(routing: Routing) {
        routing.webSocket("/v1/parcel-collection") {
            if (call.request.header("Origin") != null) {
                // The client is most likely a (malicious) web page
                close(
                    CloseReason(
                        CloseReason.Codes.VIOLATED_POLICY,
                        "Web browser requests are disabled for security reasons"
                    )
                )
                return@webSocket
            }

            val certificates = try {
                parcelCollectionHandshake.handshake(this)
            } catch (e: ParcelCollectionHandshake.HandshakeUnsuccessful) {
                return@webSocket
            }

            // TODO: Call cert.getCertificartePath() and validate certificate chain

            val endpointAddresses =
                certificates.map { PrivateMessageAddress(it.subjectPrivateAddress) }

            // TODO: The actual sending of parcels is part of
            // https://github.com/relaycorp/relaynet-gateway-android/issues/16
        }
    }
}
