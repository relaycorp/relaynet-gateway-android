package tech.relaycorp.gateway.pdc.local.utils

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.bindings.pdc.DetachedSignatureType
import tech.relaycorp.relaynet.bindings.pdc.InvalidSignatureException
import tech.relaycorp.relaynet.messages.InvalidMessageException
import tech.relaycorp.relaynet.messages.control.HandshakeChallenge
import tech.relaycorp.relaynet.messages.control.HandshakeResponse
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import javax.inject.Inject

class ParcelCollectionHandshake
@Inject constructor(
    private val localConfig: LocalConfig,
) {

    @Throws(HandshakeUnsuccessful::class)
    suspend fun handshake(session: DefaultWebSocketServerSession): List<Certificate> {
        val nonce = Handshake.generateNonce()
        val challenge = HandshakeChallenge(nonce)
        session.outgoing.send(
            Frame.Binary(true, challenge.serialize()),
        )

        val responseRaw = session.incoming.receive()
        val response = try {
            HandshakeResponse.deserialize(responseRaw.readBytes())
        } catch (_: InvalidMessageException) {
            session.closeCannotAccept("Invalid handshake response")
            throw HandshakeUnsuccessful()
        }

        if (response.nonceSignatures.isEmpty()) {
            session.closeCannotAccept("Handshake response did not include any nonce signatures")
            throw HandshakeUnsuccessful()
        }

        val trustedCertificates = localConfig.getAllValidParcelDeliveryCertificates()

        return response.nonceSignatures
            .map { nonceSignature ->
                try {
                    DetachedSignatureType.NONCE.verify(
                        nonceSignature,
                        nonce,
                        trustedCertificates,
                    )
                } catch (_: InvalidSignatureException) {
                    session.closeCannotAccept(
                        "Handshake response included invalid nonce signatures or untrusted signers",
                    )
                    throw HandshakeUnsuccessful()
                }
            }
    }

    private suspend fun DefaultWebSocketServerSession.closeCannotAccept(reason: String) {
        close(
            CloseReason(
                CloseReason.Codes.CANNOT_ACCEPT,
                reason,
            ),
        )
    }

    class HandshakeUnsuccessful : Exception()
}
