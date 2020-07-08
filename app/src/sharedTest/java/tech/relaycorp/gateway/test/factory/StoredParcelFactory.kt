package tech.relaycorp.gateway.test.factory

import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StoredParcel
import java.util.Date
import java.util.Random

object StoredParcelFactory {

    fun build() = StoredParcel(
        recipientAddress = MessageAddress.of(Random().nextInt().toString()),
        senderAddress = MessageAddress.of(Random().nextInt().toString()),
        messageId = MessageId(Random().nextInt().toString()),
        recipientLocation = RecipientLocation.values()[Random().nextInt(RecipientLocation.values().size)],
        creationTimeUtc = Date(),
        expirationTimeUtc = Date(),
        storagePath = "",
        size = StorageSizeFactory.build()
    )
}
