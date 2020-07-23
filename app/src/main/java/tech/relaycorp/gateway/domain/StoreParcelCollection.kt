package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.common.toDate
import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.ParcelCollection
import tech.relaycorp.gateway.data.model.PrivateMessageAddress
import tech.relaycorp.relaynet.messages.Parcel
import javax.inject.Inject

class StoreParcelCollection
@Inject constructor(
    private val parcelCollectionDao: ParcelCollectionDao
) {

    suspend fun storeForParcel(parcel: Parcel) {
        parcelCollectionDao.insert(
            ParcelCollection(
                MessageAddress.of(parcel.recipientAddress),
                PrivateMessageAddress(parcel.senderCertificate.subjectPrivateAddress),
                MessageId(parcel.id),
                parcel.creationDate.toDate(),
                parcel.expiryDate.toDate()
            )
        )
    }
}
