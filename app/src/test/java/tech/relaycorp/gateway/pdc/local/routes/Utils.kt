package tech.relaycorp.gateway.pdc.local.routes

import com.nhaarman.mockitokotlin2.mock
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import tech.relaycorp.gateway.domain.endpoint.EndpointRegistration
import tech.relaycorp.gateway.pdc.local.configure

fun <R> testPDCServer(test: TestApplicationEngine.() -> R): R {
    val mockEndpointRegistration = mock<EndpointRegistration>()
    return testPDCServer(mockEndpointRegistration, test)
}

fun <R> testPDCServer(
    mockEndpointRegistration: EndpointRegistration,
    test: TestApplicationEngine.() -> R
): R {
    return withTestApplication({ configure(mockEndpointRegistration) }, test)
}
