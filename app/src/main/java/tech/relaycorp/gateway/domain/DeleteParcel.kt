package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.data.repos.ParcelRepository
import tech.relaycorp.gateway.data.disk.DiskRepository
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.PrivateMessageAddress
import javax.inject.Inject

class DeleteParcel
@Inject constructor(
    private val storesParcelRepository: ParcelRepository,
    private val diskRepository: DiskRepository
) {
    suspend fun delete(senderAddress: PrivateMessageAddress, messageId: MessageId) {
        val parcel = storesParcelRepository.get(senderAddress, messageId)
        storesParcelRepository.delete(parcel)
        diskRepository.deleteMessage(parcel.storagePath)
    }
}
