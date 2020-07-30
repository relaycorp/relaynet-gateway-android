package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.StoreParcel
import java.io.InputStream
import javax.inject.Inject

class ReceiveParcelFromEndpoint
@Inject constructor(
    private val storeParcel: StoreParcel
) {

    suspend fun receive(parcelStream: InputStream) =
        storeParcel.store(parcelStream, RecipientLocation.ExternalGateway)
}
