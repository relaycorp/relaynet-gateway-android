package tech.relaycorp.gateway.pdc.local.routes

import io.ktor.application.install
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import io.ktor.websocket.WebSockets

fun <R> testPDCServerRoute(route: PDCServerRoute, test: TestApplicationEngine.() -> R) =
    withTestApplication(
        {
            install(WebSockets)
            routing(route::register)
        },
        test
    )
