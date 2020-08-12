package tech.relaycorp.gateway.pdc.local

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import tech.relaycorp.gateway.domain.endpoint.EndpointRegistration
import tech.relaycorp.gateway.pdc.local.routes.registerEndpointRegistration
import tech.relaycorp.gateway.pdc.local.routes.registerParcelCollection

fun Application.configure(endpointRegistration: EndpointRegistration) {
    install(WebSockets)

    routing {
        registerEndpointRegistration(endpointRegistration)
        registerParcelCollection()
    }
}
