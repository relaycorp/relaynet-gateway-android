package tech.relaycorp.gateway.domain.endpoint

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ParcelDeliveryTest {
    @Test
    @Disabled
    fun `Malformed parcel should be refused`() {
    }

    @Test
    @Disabled
    fun `Well-formed yet invalid parcel should be refused`() {
    }

    @Test
    @Disabled
    fun `Parcel sent by an unregistered local endpoint should be refused`() {
    }

    @Test
    @Disabled
    fun `Parcel bound for public endpoint should be accepted`() {
    }

    @Test
    @Disabled
    fun `Authorized parcel bound for private endpoint should be accepted`() {
    }

    @Test
    @Disabled
    fun `Unauthorized parcel bound for private endpoint should be accepted`() {
    }
}
