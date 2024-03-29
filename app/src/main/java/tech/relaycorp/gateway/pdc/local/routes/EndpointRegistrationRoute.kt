package tech.relaycorp.gateway.pdc.local.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import tech.relaycorp.gateway.domain.endpoint.EndpointRegistration
import tech.relaycorp.gateway.domain.endpoint.GatewayNotRegisteredException
import tech.relaycorp.gateway.domain.endpoint.InvalidPNRAException
import tech.relaycorp.gateway.pdc.local.utils.ContentType
import tech.relaycorp.relaynet.messages.InvalidMessageException
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationRequest
import javax.inject.Inject

class EndpointRegistrationRoute
@Inject constructor(
    private val endpointRegistration: EndpointRegistration,
) : PDCServerRoute {

    override fun register(routing: Routing) {
        routing.post("/v1/nodes") {
            if (call.request.contentType() != ContentType.REGISTRATION_REQUEST) {
                call.respondText(
                    "Content type ${ContentType.REGISTRATION_REQUEST} is required",
                    status = HttpStatusCode.UnsupportedMediaType,
                )
                return@post
            }

            val crr = try {
                PrivateNodeRegistrationRequest.deserialize(call.receive())
            } catch (_: InvalidMessageException) {
                call.respondText(
                    "Invalid registration request",
                    status = HttpStatusCode.BadRequest,
                )
                return@post
            }

            val registrationSerialized = try {
                endpointRegistration.register(crr)
            } catch (_: InvalidPNRAException) {
                call.respondText(
                    "Invalid authorization encapsulated in registration request",
                    status = HttpStatusCode.BadRequest,
                )
                return@post
            } catch (_: GatewayNotRegisteredException) {
                call.respondText(
                    "Gateway not registered",
                    status = HttpStatusCode.BadRequest,
                )
                return@post
            }

            call.respondBytes(registrationSerialized, ContentType.REGISTRATION)
        }
    }
}
