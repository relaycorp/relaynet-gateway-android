package tech.relaycorp.gateway.pdc.local

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import tech.relaycorp.gateway.pdc.local.routes.parcelCollection

fun Application.main() {
    install(WebSockets)

    routing {
        parcelCollection()
    }
}
