package tech.relaycorp.gateway.pdc.local.utils

import tech.relaycorp.poweb.PoWebContentType
import io.ktor.http.ContentType as KtorContentType

object ContentType {
    val PARCEL = KtorContentType.parse(PoWebContentType.PARCEL.value)
    val REGISTRATION_REQUEST = KtorContentType.parse(PoWebContentType.REGISTRATION_REQUEST.value)
    val REGISTRATION = KtorContentType.parse(PoWebContentType.REGISTRATION.value)
}
