package tech.relaycorp.gateway.test.factory

import tech.relaycorp.relaynet.messages.ParcelCollectionAck
import kotlin.random.Random

object ParcelCollectionAckFactory {

    fun build() = ParcelCollectionAck(
        senderEndpointPrivateAddress = Random.nextInt().toString(),
        recipientEndpointAddress = Random.nextInt().toString(),
        parcelId = Random.nextInt().toString()
    )
}
