package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.disk.MessageDataNotFoundException
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.gateway.domain.DeleteParcel
import java.io.InputStream
import java.util.UUID
import java.util.logging.Level
import javax.inject.Inject

class ParcelDelivery
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val diskMessageOperations: DiskMessageOperations,
    private val deleteParcel: DeleteParcel
) {

    private val parcelsSentForDelivery = mutableMapOf<String, StoredParcel>()

    suspend fun getParcelsToDeliver(endpointAddress: MessageAddress): Iterable<ParcelDeliveryRequest> {
        val parcels =
            storedParcelDao.listForRecipient(endpointAddress, RecipientLocation.LocalEndpoint)
                .map { Pair(generateLocalId(), it) }
                .toMap()

        parcelsSentForDelivery.putAll(parcels)

        return parcels
            .mapNotNull { entry ->
                entry.value.getInputStream()?.let { ParcelDeliveryRequest(entry.key, it) }
            }
    }

    suspend fun processParcelAck(localId: String) {
        deleteParcel.delete(parcelsSentForDelivery[localId] ?: return)
    }

    private fun generateLocalId() = UUID.randomUUID().toString()

    private suspend fun StoredParcel.getInputStream() =
        try {
            diskMessageOperations.readMessage(StoredParcel.STORAGE_FOLDER, storagePath)()
        } catch (e: MessageDataNotFoundException) {
            logger.log(Level.WARNING, "Read parcel", e)
            null
        }
}

data class ParcelDeliveryRequest(
    val localId: String,
    val parcelStream: InputStream
)
