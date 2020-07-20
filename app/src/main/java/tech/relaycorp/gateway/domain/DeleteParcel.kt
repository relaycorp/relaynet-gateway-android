package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.StoredParcel
import javax.inject.Inject

class DeleteParcel
@Inject constructor(
    private val parcelCollectionDao: ParcelCollectionDao,
    private val diskMessageOperations: DiskMessageOperations
) {
    suspend fun delete(
        recipientAddress: MessageAddress,
        senderAddress: MessageAddress,
        messageId: MessageId
    ) {
        parcelCollectionDao.get(recipientAddress, senderAddress, messageId)
            ?.let { delete(it) }
    }

    suspend fun delete(storedParcel: StoredParcel) {
        parcelCollectionDao.delete(storedParcel)
        diskMessageOperations.deleteMessage(StoredParcel.STORAGE_FOLDER, storedParcel.storagePath)
    }
}
