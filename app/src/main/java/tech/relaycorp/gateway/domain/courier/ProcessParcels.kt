package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.gateway.domain.DeleteParcel
import tech.relaycorp.gateway.domain.endpoint.DeliverParcelToEndpoint
import java.util.logging.Level
import javax.inject.Inject

class ProcessParcels
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val diskMessageOperations: DiskMessageOperations,
    private val deliverParcelToEndpoint: DeliverParcelToEndpoint,
    private val deleteParcel: DeleteParcel
) {

    suspend fun process() {
        val parcels = storedParcelDao.listForRecipientLocation(RecipientLocation.LocalEndpoint)
        parcels.forEach { storedParcel ->
            val parcelStream =
                diskMessageOperations.readMessage(
                    StoredParcel.STORAGE_FOLDER,
                    storedParcel.storagePath
                )
            try {
                deliverParcelToEndpoint.deliver(storedParcel.recipientAddress, parcelStream())
                deleteParcel.delete(storedParcel)
            } catch (exception: Exception) {
                logger.log(Level.WARNING, "Could not deliver parcel", exception)
            }
        }
    }
}
