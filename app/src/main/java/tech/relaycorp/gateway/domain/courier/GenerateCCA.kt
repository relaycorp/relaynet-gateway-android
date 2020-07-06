package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import javax.inject.Inject

class GenerateCCA
@Inject constructor() {

    // TODO: implementation
    fun generate() =
        CargoCollectionAuthorization.deserialize(ByteArray(0))

    // TODO: implementation
    fun generateByteArray() = ByteArray(0)
}
