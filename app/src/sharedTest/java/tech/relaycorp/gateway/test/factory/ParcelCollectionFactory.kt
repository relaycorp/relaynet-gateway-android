package tech.relaycorp.gateway.test.factory

import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.ParcelCollection
import tech.relaycorp.gateway.data.model.PrivateMessageAddress
import kotlin.random.Random

object ParcelCollectionFactory {

    fun build() = ParcelCollection(
        senderAddress = PrivateMessageAddress(Random.nextInt().toString()),
        recipientAddress = MessageAddress.of(Random.nextInt().toString()),
        messageId = MessageId(Random.nextInt().toString()),
        creationTimeUtc = nowInUtc(),
        expirationTimeUtc = nowInUtc(),
    )
}
