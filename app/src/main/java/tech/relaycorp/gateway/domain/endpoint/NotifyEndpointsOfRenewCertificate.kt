package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.database.LocalEndpointDao
import javax.inject.Inject

class NotifyEndpointsOfRenewCertificate @Inject constructor(
    private val notifyEndpoints: NotifyEndpoints,
    private val endpointDao: LocalEndpointDao
) {
    suspend fun notifyAll() {
        val registeredEndPoints = endpointDao.list()
        if (registeredEndPoints.isEmpty()) {
            logger.warning("No endpoint to notify about Certificate Renew")
            return
        }

        notifyEndpoints.notifyApp(registeredEndPoints, EndpointNotifyAction.CertificateRenew)
    }
}
