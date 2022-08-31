package tech.relaycorp.gateway.domain.endpoint

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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

/*
    This class keeps the ParcelCollection state for one connection.
    It should not be re-used between ParcelCollection connections.
 */
class CollectParcels
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val diskMessageOperations: DiskMessageOperations,
    private val deleteParcel: DeleteParcel
) {

    private val parcelsSent = mutableListOf<StoredParcel>()
    private val parcelsWaitingAck = mutableMapOf<String, StoredParcel>()

    private val _anyParcelsLeft = MutableStateFlow(true)
    val anyParcelsLeftToDeliverOrAck: Flow<Boolean> = _anyParcelsLeft

    suspend fun getNewParcelsForEndpoints(
        endpointsAddresses: List<MessageAddress>
    ): Flow<List<Pair<String, InputStream>>> {
        return storedParcelDao
            .listForRecipients(endpointsAddresses, RecipientLocation.LocalEndpoint)
            .onEach { _anyParcelsLeft.value = it.any() }
            .onEach { logger.log(Level.INFO, "getNewParcelsForEndpoints was called")}
            // Filter only parcels we haven't sent before
            .map { list -> list.filter { !parcelsSent.contains(it) } }
            // Associate local ID
            .map { list -> list.map { Pair(generateLocalId(), it) } }
            .onEach { storeSentParcels(it) }
            .map { mapToParcelInputStreams(it) }
    }

    suspend fun processParcelAck(localId: String) {
        deleteParcel.delete(parcelsWaitingAck.remove(localId) ?: return)
    }

    private fun generateLocalId() = UUID.randomUUID().toString()

    private fun storeSentParcels(parcels: List<Pair<String, StoredParcel>>) {
        parcelsSent.addAll(parcels.map { it.second })
        parcelsWaitingAck.putAll(parcels)
    }

    private suspend fun mapToParcelInputStreams(parcels: List<Pair<String, StoredParcel>>) =
        parcels.mapNotNull { (localId, storedParcel) ->
            storedParcel.getInputStream()?.let {
                Pair(localId, it)
            }
        }

    private suspend fun StoredParcel.getInputStream() =
        try {
            diskMessageOperations.readMessage(StoredParcel.STORAGE_FOLDER, storagePath)()
        } catch (e: MessageDataNotFoundException) {
            logger.log(Level.WARNING, "Read parcel", e)
            null
        }
}
