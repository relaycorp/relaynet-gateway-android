package tech.relaycorp.gateway.pdc.local.utils

import io.ktor.http.ContentType

// TODO: Move to PoWeb lib
object ControlMessageContentType {
    // Private Node Registration Request (PNRR)
    val PNRR = ContentType("application", "vnd.relaynet.pdc.pnrr")

    // Private Node Registration (PNR)
    val PNR = ContentType("application", "vnd.relaynet.pdc.pnr")
}
