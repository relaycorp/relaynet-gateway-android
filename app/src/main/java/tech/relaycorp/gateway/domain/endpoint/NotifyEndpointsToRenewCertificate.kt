package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.database.LocalEndpointDao
import javax.inject.Inject

class NotifyEndpointsToRenewCertificate @Inject constructor(
    private val notifyEndpoints: NotifyEndpoints,
    private val endpointDao: LocalEndpointDao
) {
    suspend fun notifyAll() {
        val registeredEndPoints = endpointDao.list()
        if (registeredEndPoints.isEmpty()) {
            logger.info("No endpoint to notify about Certificate Renew")
            return
        }

        notifyEndpoints.notify(registeredEndPoints, EndpointNotifyAction.CertificateRenew)
    }
}
