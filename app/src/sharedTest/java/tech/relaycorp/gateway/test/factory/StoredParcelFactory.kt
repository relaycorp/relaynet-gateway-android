package tech.relaycorp.gateway.test.factory

import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StoredParcel
import java.util.Random

object StoredParcelFactory {

    fun build(): StoredParcel {
        val recipientLocations = RecipientLocation.values()
        return StoredParcel(
            recipientAddress = MessageAddress.of(Random().nextInt().toString()),
            senderAddress = MessageAddress.of(Random().nextInt().toString()),
            messageId = MessageId(Random().nextInt().toString()),
            recipientLocation = recipientLocations[Random().nextInt(recipientLocations.size)],
            creationTimeUtc = nowInUtc(),
            expirationTimeUtc = nowInUtc().plusDays(1),
            storagePath = "file",
            size = StorageSizeFactory.build()
        )
    }
}
