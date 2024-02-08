package tech.relaycorp.gateway.pdc.local.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.pdc.local.utils.ContentType
import javax.inject.Inject

class ParcelDeliveryRoute
@Inject constructor(private val storeParcel: StoreParcel) : PDCServerRoute {
    override fun register(routing: Routing) {
        routing.post("/v1/parcels") {
            if (call.request.contentType() != ContentType.PARCEL) {
                call.respondText(
                    "Content type ${ContentType.PARCEL} is required",
                    status = HttpStatusCode.UnsupportedMediaType,
                )
                return@post
            }

            val storeResult =
                storeParcel.store(call.receive<ByteArray>(), RecipientLocation.ExternalGateway)
            when (storeResult) {
                is StoreParcel.Result.MalformedParcel -> call.respondText(
                    "Parcel is malformed",
                    status = HttpStatusCode.BadRequest,
                )
                is StoreParcel.Result.InvalidParcel -> call.respondText(
                    "Parcel is invalid",
                    status = HttpStatusCode.UnprocessableEntity,
                )
                else -> call.respond(HttpStatusCode.Accepted)
            }
        }
    }
}
