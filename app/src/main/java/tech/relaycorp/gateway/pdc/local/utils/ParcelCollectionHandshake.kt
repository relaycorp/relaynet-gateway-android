package tech.relaycorp.gateway.pdc.local.utils

import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readBytes
import io.ktor.websocket.DefaultWebSocketServerSession
import tech.relaycorp.poweb.handshake.Challenge
import tech.relaycorp.poweb.handshake.InvalidMessageException
import tech.relaycorp.poweb.handshake.Response
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import javax.inject.Inject

class ParcelCollectionHandshake
@Inject constructor() {

    @Throws(HandshakeUnsuccessful::class)
    suspend fun handshake(session: DefaultWebSocketServerSession): List<Certificate> {
        val nonce = Handshake.generateNonce()
        val challenge = Challenge(nonce)
        session.outgoing.send(
            Frame.Binary(true, challenge.serialize())
        )

        val responseRaw = session.incoming.receive()
        val response = try {
            Response.deserialize(responseRaw.readBytes())
        } catch (_: InvalidMessageException) {
            session.closeCannotAccept("Invalid handshake response")
            throw HandshakeUnsuccessful()
        }

        if (response.nonceSignatures.isEmpty()) {
            session.closeCannotAccept("Handshake response did not include any nonce signatures")
            throw HandshakeUnsuccessful()
        }

        return response.nonceSignatures
            .map { nonceSignature ->
                try {
                    Handshake.verifySignature(
                        nonceSignature,
                        nonce
                    )
                } catch (_: InvalidHandshakeSignatureException) {
                    session.closeCannotAccept(
                        "Handshake response included invalid nonce signatures"
                    )
                    throw HandshakeUnsuccessful()
                }
            }
    }

    private suspend fun DefaultWebSocketServerSession.closeCannotAccept(reason: String) {
        close(
            CloseReason(
                CloseReason.Codes.CANNOT_ACCEPT,
                reason
            )
        )
    }

    class HandshakeUnsuccessful : Exception()
}
