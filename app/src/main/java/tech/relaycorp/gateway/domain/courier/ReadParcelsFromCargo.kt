package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.relaynet.messages.Cargo
import java.io.InputStream
import javax.inject.Inject

class ReadParcelsFromCargo
@Inject constructor() {

    // TODO: implement me
    suspend fun read(cargo: Cargo): Iterable<InputStream> = emptyList()
}
