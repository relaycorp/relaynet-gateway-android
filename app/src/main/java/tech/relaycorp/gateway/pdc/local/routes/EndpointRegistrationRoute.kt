package tech.relaycorp.gateway.pdc.local.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.contentType
import io.ktor.request.receive
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.post
import tech.relaycorp.gateway.domain.endpoint.EndpointRegistration
import tech.relaycorp.gateway.domain.endpoint.InvalidCRAException
import tech.relaycorp.gateway.pdc.local.utils.ControlMessageContentType
import tech.relaycorp.relaynet.messages.InvalidMessageException
import tech.relaycorp.relaynet.messages.control.ClientRegistrationRequest
import javax.inject.Inject

class EndpointRegistrationRoute
@Inject constructor(
    private val endpointRegistration: EndpointRegistration
) : PDCServerRoute {

    override fun register(routing: Routing) {
        routing.post("/v1/clients") {
            if (call.request.contentType() != ControlMessageContentType.CRR) {
                call.respondText(
                    "Content type ${ControlMessageContentType.CRR} is required",
                    status = HttpStatusCode.UnsupportedMediaType
                )
                return@post
            }

            val crr = try {
                ClientRegistrationRequest.deserialize(call.receive())
            } catch (_: InvalidMessageException) {
                call.respondText(
                    "Invalid client registration request",
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }

            val registrationSerialized = try {
                endpointRegistration.register(crr)
            } catch (_: InvalidCRAException) {
                call.respondText(
                    "Invalid client registration authorization encapsulated in CRR",
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }

            call.respondBytes(registrationSerialized, ControlMessageContentType.CLIENT_REGISTRATION)
        }
    }
}
