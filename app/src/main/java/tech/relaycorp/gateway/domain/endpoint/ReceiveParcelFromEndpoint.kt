package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.domain.publicsync.PublicSync
import java.io.InputStream
import javax.inject.Inject

class ReceiveParcelFromEndpoint
@Inject constructor(
    private val storeParcel: StoreParcel,
    private val PublicSync: PublicSync
) {

    suspend fun receive(parcels: Iterable<InputStream>) {
        parcels.forEach { parcelStream ->
            storeParcel.store(parcelStream, RecipientLocation.ExternalGateway)
        }
        PublicSync.sync()
    }
}
