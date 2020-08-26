package tech.relaycorp.gateway.pdc.local.routes

import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import tech.relaycorp.gateway.pdc.local.PDCServerConfiguration

fun <R> testPDCServerRoute(route: PDCServerRoute, test: TestApplicationEngine.() -> R) =
    withTestApplication(
        {
            PDCServerConfiguration.configure(this, listOf(route))
        },
        test
    )
