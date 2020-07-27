package tech.relaycorp.gateway.test.factory

import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StoredParcel
import java.util.Random

object StoredParcelFactory {

    fun build() = StoredParcel(
        recipientAddress = MessageAddress.of(Random().nextInt().toString()),
        senderAddress = MessageAddress.of(Random().nextInt().toString()),
        messageId = MessageId(Random().nextInt().toString()),
        recipientLocation = RecipientLocation.values()[Random().nextInt(RecipientLocation.values().size)],
        creationTimeUtc = nowInUtc(),
        expirationTimeUtc = nowInUtc(),
        storagePath = "",
        size = StorageSizeFactory.build()
    )
}
