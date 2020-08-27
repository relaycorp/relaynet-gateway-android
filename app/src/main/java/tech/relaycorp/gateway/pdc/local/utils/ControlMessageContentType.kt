package tech.relaycorp.gateway.pdc.local.utils

import io.ktor.http.ContentType

// TODO: Move to PoWeb lib
object ControlMessageContentType {
    val CRR = ContentType("application", "vnd.relaynet.pdc.crr")
    val CLIENT_REGISTRATION = ContentType("application", "vnd.relaynet.pdc.client-registration")
}
