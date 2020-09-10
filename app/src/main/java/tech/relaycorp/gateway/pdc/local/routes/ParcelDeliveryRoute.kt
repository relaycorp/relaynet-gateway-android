package tech.relaycorp.gateway.pdc.local.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.request.contentType
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.post
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.pdc.local.utils.ContentType
import javax.inject.Inject
import io.ktor.http.ContentType as KtorContentType

class ParcelDeliveryRoute
@Inject constructor(private val storeParcel: StoreParcel) : PDCServerRoute {
    override fun register(routing: Routing) {
        routing.post("/v1/parcels") {
            if (call.request.contentType() != ContentType.PARCEL) {
                call.respondText(
                    "Content type ${ContentType.PARCEL} is required",
                    status = HttpStatusCode.UnsupportedMediaType
                )
                return@post
            }

            val storeResult =
                storeParcel.store(call.receive<ByteArray>(), RecipientLocation.ExternalGateway)
            call.respond(
                when (storeResult) {
                    is StoreParcel.Result.MalformedParcel -> TextContent(
                        "Parcel is malformed",
                        KtorContentType.Text.Plain,
                        HttpStatusCode.BadRequest
                    )
                    is StoreParcel.Result.InvalidParcel -> TextContent(
                        "Parcel is invalid",
                        KtorContentType.Text.Plain,
                        HttpStatusCode.Forbidden
                    )
                    else -> HttpStatusCode.Accepted
                }
            )
        }
    }
}
