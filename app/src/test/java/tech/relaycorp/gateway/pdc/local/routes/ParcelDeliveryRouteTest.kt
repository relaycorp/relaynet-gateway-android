package tech.relaycorp.gateway.pdc.local.routes

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ParcelDeliveryRouteTest {
    @Test
    @Disabled
    fun `Invalid request Content-Type should be refused with an HTTP 415 response`() {
    }

    @Test
    @Disabled
    fun `Invalid parcels should be refused with an HTTP 400 response`() {
    }

    @Test
    @Disabled
    fun `Parcels from unauthorized senders should be refused with an HTTP 403 response`() {
    }

    @Test
    @Disabled
    fun `Valid parcels should result in an HTTP 204 response`() {
    }
}
