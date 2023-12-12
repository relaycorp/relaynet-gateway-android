package tech.relaycorp.gateway.test.factory

import tech.relaycorp.gateway.data.model.LocalEndpoint
import tech.relaycorp.gateway.data.model.PrivateMessageAddress
import java.util.UUID
import kotlin.random.Random

object LocalEndpointFactory {

    fun build() = LocalEndpoint(
        address = PrivateMessageAddress(Random.nextInt().toString()),
        applicationId = UUID.randomUUID().toString(),
    )
}
