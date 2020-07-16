package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.data.model.MessageAddress
import java.io.InputStream
import javax.inject.Inject

class DeliverParcelToEndpoint
@Inject constructor() {

    // TODO: implementation
    suspend fun deliver(
        recipientAddress: MessageAddress,
        parcelStream: InputStream
    ) {}
}
