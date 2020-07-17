package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.model.LocalEndpoint
import tech.relaycorp.gateway.data.model.MessageAddress
import javax.inject.Inject

class RegisterEndpoint
@Inject constructor(
    private val localEndpointDao: LocalEndpointDao
) {

    suspend fun register(applicationId: String, endpointAddress: MessageAddress) {
        localEndpointDao.insert(LocalEndpoint(applicationId, endpointAddress))
    }
}
