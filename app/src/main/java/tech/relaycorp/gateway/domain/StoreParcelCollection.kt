package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.gateway.data.model.ParcelCollection
import javax.inject.Inject

class StoreParcelCollection
@Inject constructor(
    private val parcelCollectionDao: ParcelCollectionDao
) {

    suspend fun storeForParcel(parcel: StoredParcel) {
        parcelCollectionDao.insert(
            ParcelCollection(
                parcel.recipientAddress,
                parcel.senderAddress,
                parcel.messageId,
                parcel.creationTimeUtc,
                parcel.expirationTimeUtc
            )
        )
    }
}
