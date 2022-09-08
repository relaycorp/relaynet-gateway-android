package tech.relaycorp.gateway.test.factory

import tech.relaycorp.relaynet.messages.ParcelCollectionAck
import kotlin.random.Random

object ParcelCollectionAckFactory {

    fun build() = ParcelCollectionAck(
        senderEndpointId = Random.nextInt().toString(),
        recipientEndpointId = Random.nextInt().toString(),
        parcelId = Random.nextInt().toString()
    )
}
