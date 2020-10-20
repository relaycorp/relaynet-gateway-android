package tech.relaycorp.gateway.pdc.local.utils

import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readBytes
import io.ktor.websocket.DefaultWebSocketServerSession
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.messages.InvalidMessageException
import tech.relaycorp.relaynet.messages.control.HandshakeChallenge
import tech.relaycorp.relaynet.messages.control.HandshakeResponse
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import tech.relaycorp.relaynet.wrappers.x509.CertificateException
import javax.inject.Inject

class ParcelCollectionHandshake
@Inject constructor(
    private val localConfig: LocalConfig
) {

    @Throws(HandshakeUnsuccessful::class)
    suspend fun handshake(session: DefaultWebSocketServerSession): List<Certificate> {
        val nonce = Handshake.generateNonce()
        val challenge = HandshakeChallenge(nonce)
        session.outgoing.send(
            Frame.Binary(true, challenge.serialize())
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

        val certificates = response.nonceSignatures
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

        if (!areCertificatesValid(certificates)) {
            session.closeCannotAccept(
                "Handshake response included invalid certificates"
            )
            throw HandshakeUnsuccessful()
        }

        return certificates
    }

    private suspend fun areCertificatesValid(certificates: List<Certificate>): Boolean {
        val localCertificate = localConfig.getCertificate()
        certificates.forEach { certificate ->
            try {
                certificate.getCertificationPath(emptyList(), listOf(localCertificate))
            } catch (exc: CertificateException) {
                return false
            }
        }
        return true
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
