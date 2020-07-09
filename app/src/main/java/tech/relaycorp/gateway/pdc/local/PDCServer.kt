package tech.relaycorp.gateway.pdc.local

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.http.cio.websocket.Frame
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import tech.relaycorp.poweb.handshake.Challenge

fun Application.main() {
    install(WebSockets)

    routing {
        webSocket("/v1/parcel-collection") {
            val challenge = Challenge(Handshake.generateNonce())
            outgoing.send(Frame.Binary(true, challenge.serialize()))
        }
    }
}
