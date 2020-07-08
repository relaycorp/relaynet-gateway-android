package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.common.Operation
import tech.relaycorp.gateway.data.model.MessageAddress
import java.io.InputStream
import javax.inject.Inject

class DeliverParcel
@Inject constructor() {

    // TODO: implementation
    suspend fun deliver(
        recipientAddress: MessageAddress,
        parcelStream: InputStream
    ): Operation<Unit> =
        Operation.Success(Unit)
}
