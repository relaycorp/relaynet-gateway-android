package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.messages.Cargo
import javax.inject.Inject

class ReadMessagesFromCargo
@Inject constructor(
    private val localConfig: LocalConfig
) {

    suspend fun read(cargo: Cargo) =
        cargo
            .unwrapPayload(localConfig.getKeyPair().private)
            .classifyMessages()
}
