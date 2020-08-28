package tech.relaycorp.gateway.pdc.local.routes

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.domain.endpoint.EndpointRegistration
import tech.relaycorp.gateway.domain.endpoint.InvalidPNRAException
import tech.relaycorp.gateway.pdc.local.utils.ControlMessageContentType
import tech.relaycorp.gateway.test.FullCertPath
import tech.relaycorp.gateway.test.KeyPairSet
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistration
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationRequest
import kotlin.test.assertEquals

class EndpointRegistrationRouteTest {
    private val plainTextUTF8ContentType = ContentType.Text.Plain.withCharset(Charsets.UTF_8)

    private val endpointRegistration = mock<EndpointRegistration>()
    private val route = EndpointRegistrationRoute(endpointRegistration)

    @Test
    fun `Invalid request content type should be refused`() {
        testPDCServerRoute(route) {
            val call = handleRequest(HttpMethod.Post, "/v1/nodes") {
                addHeader("Content-Type", ContentType.Application.Json.toString())
            }
            with(call) {
                assertEquals(HttpStatusCode.UnsupportedMediaType, response.status())
                assertEquals(plainTextUTF8ContentType, response.contentType())
                assertEquals(
                    "Content type ${ControlMessageContentType.PNRR} is required",
                    response.content
                )
            }
        }
    }

    @Test
    fun `Invalid CRR should be refused`() {
        testPDCServerRoute(route) {
            val call = handleRequest(HttpMethod.Post, "/v1/nodes") {
                addHeader("Content-Type", ControlMessageContentType.PNRR.toString())
                setBody("invalid CRR".toByteArray())
            }
            with(call) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals(plainTextUTF8ContentType, response.contentType())
                assertEquals(
                    "Invalid registration request",
                    response.content
                )
            }
        }
    }

    @Test
    fun `Valid CRR with invalid authorization encapsulated should be refused`() = runBlockingTest {
        whenever(endpointRegistration.register(any()))
            .thenThrow(InvalidPNRAException("Invalid authorization", null))

        testPDCServerRoute(route) {
            val crr = PrivateNodeRegistrationRequest(
                KeyPairSet.PRIVATE_ENDPOINT.public,
                "invalid authorization".toByteArray()
            )
            val call = handleRequest(HttpMethod.Post, "/v1/nodes") {
                addHeader("Content-Type", ControlMessageContentType.PNRR.toString())
                setBody(crr.serialize(KeyPairSet.PRIVATE_ENDPOINT.private))
            }
            with(call) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals(plainTextUTF8ContentType, response.contentType())
                assertEquals(
                    "Invalid authorization encapsulated in registration request",
                    response.content
                )
            }
        }
    }

    @Test
    fun `Valid CRR should complete the registration`() = runBlockingTest {
        val PrivateNodeRegistration =
            PrivateNodeRegistration(FullCertPath.PRIVATE_ENDPOINT, FullCertPath.PRIVATE_GW)
        val PrivateNodeRegistrationSerialized = PrivateNodeRegistration.serialize()
        whenever(endpointRegistration.register(any())).thenReturn(PrivateNodeRegistrationSerialized)

        testPDCServerRoute(route) {
            val crr = PrivateNodeRegistrationRequest(
                KeyPairSet.PRIVATE_ENDPOINT.public,
                "invalid authorization".toByteArray()
            )
            val call = handleRequest(HttpMethod.Post, "/v1/nodes") {
                addHeader("Content-Type", ControlMessageContentType.PNRR.toString())
                setBody(crr.serialize(KeyPairSet.PRIVATE_ENDPOINT.private))
            }
            with(call) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ControlMessageContentType.PNR, response.contentType())
                assertEquals(
                    PrivateNodeRegistrationSerialized.asList(),
                    response.byteContent!!.asList()
                )
            }
        }
    }
}
