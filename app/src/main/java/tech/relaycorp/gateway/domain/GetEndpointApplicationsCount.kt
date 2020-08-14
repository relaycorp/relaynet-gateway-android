package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.data.database.LocalEndpointDao
import javax.inject.Inject

class GetEndpointApplicationsCount
@Inject constructor(
    private val localEndpointDao: LocalEndpointDao
) {

    fun get() = localEndpointDao.countApplicationIds()
}
