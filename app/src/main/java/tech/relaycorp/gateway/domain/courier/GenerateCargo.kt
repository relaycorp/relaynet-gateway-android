package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.relaynet.messages.Cargo
import java.io.InputStream
import javax.inject.Inject

class GenerateCargo
@Inject constructor() {

    // TODO: implementation
    suspend fun generate(): Iterable<InputStream> = emptyList()

}
