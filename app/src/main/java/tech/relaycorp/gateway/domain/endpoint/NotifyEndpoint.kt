package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.data.model.MessageAddress
import javax.inject.Inject

class NotifyEndpoint
@Inject constructor() {

    // TODO: implementation
    suspend fun notify(endpointAddress: MessageAddress) {}
}
