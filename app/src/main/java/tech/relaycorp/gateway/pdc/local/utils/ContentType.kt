package tech.relaycorp.gateway.pdc.local.utils

import tech.relaycorp.relaynet.bindings.ContentTypes
import io.ktor.http.ContentType as KtorContentType

object ContentType {
    val PARCEL = KtorContentType.parse(ContentTypes.PARCEL.value)
    val REGISTRATION_REQUEST = KtorContentType.parse(ContentTypes.NODE_REGISTRATION_REQUEST.value)
    val REGISTRATION = KtorContentType.parse(ContentTypes.NODE_REGISTRATION.value)
}
