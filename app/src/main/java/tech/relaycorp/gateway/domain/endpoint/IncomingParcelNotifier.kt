package tech.relaycorp.gateway.domain.endpoint

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import javax.inject.Inject

class IncomingParcelNotifier @Inject constructor(
    private val notifyEndpoints: NotifyEndpoints,
    private val storedParcelDao: StoredParcelDao,
    private val endpointDao: LocalEndpointDao,
) {

    suspend fun notifyAllPending() {
        val parcels = storedParcelDao.listForRecipientLocation(RecipientLocation.LocalEndpoint)
        val recipients = parcels.map { it.recipientAddress }.distinct()
        val localEndpointsForParcels = endpointDao.list(recipients)
        notifyEndpoints.notify(localEndpointsForParcels, NotificationType.IncomingParcel)
    }

    suspend fun notify(endpointAddress: MessageAddress) {
        val endpoint = endpointDao.get(endpointAddress)
        if (endpoint == null) {
            logger.warning(
                "Can't notify endpoint with unknown address " +
                    "$endpointAddress about incoming parcels",
            )
            return
        }

        notifyEndpoints.notify(endpoint, NotificationType.IncomingParcel)
    }
}
