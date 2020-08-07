package tech.relaycorp.gateway.pdc.local.routes

import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readBytes
import io.ktor.request.header
import io.ktor.routing.Routing
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.webSocket
import tech.relaycorp.gateway.pdc.local.Handshake
import tech.relaycorp.gateway.pdc.local.InvalidHandshakeSignatureException
import tech.relaycorp.poweb.handshake.Challenge
import tech.relaycorp.poweb.handshake.InvalidMessageException
import tech.relaycorp.poweb.handshake.Response

fun Routing.parcelCollection() {
    webSocket("/v1/parcel-collection") {
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

        handshake()

        // The actual sending of parcels is part of
        // https://github.com/relaycorp/relaynet-gateway-android/issues/16
    }
}

private suspend fun DefaultWebSocketServerSession.handshake() {
    val nonce = Handshake.generateNonce()
    val challenge = Challenge(nonce)
    outgoing.send(
        Frame.Binary(
            true,
            challenge.serialize()
        )
    )

    val responseRaw = incoming.receive()
    val response = try {
        Response.deserialize(responseRaw.readBytes())
    } catch (_: InvalidMessageException) {
        close(
            CloseReason(
                CloseReason.Codes.CANNOT_ACCEPT,
                "Invalid handshake response"
            )
        )
        return
    }

    if (response.nonceSignatures.isEmpty()) {
        close(
            CloseReason(
                CloseReason.Codes.CANNOT_ACCEPT,
                "Handshake response did not include any nonce signatures"
            )
        )
        return
    }

    for (nonceSignature in response.nonceSignatures) {
        try {
            Handshake.verifySignature(
                nonceSignature,
                nonce
            )
        } catch (_: InvalidHandshakeSignatureException) {
            close(
                CloseReason(
                    CloseReason.Codes.CANNOT_ACCEPT,
                    "Handshake response included invalid nonce signatures"
                )
            )
            return
        }
    }

    // TODO: Output private endpoint addresses in the second half of
    // https://github.com/relaycorp/relaynet-gateway-android/issues/16. I didn't do it now because
    // I'd have no way to test this properly and I didn't want to change the visibility of this
    // method just for that. (The private addresses will be extracted from each certificate
    // output by Handshake.verifySignature())
}
