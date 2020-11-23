package tech.relaycorp.gateway.domain.endpoint

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import tech.relaycorp.gateway.background.RelaynetAndroid
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import javax.inject.Inject

class NotifyEndpoints
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val endpointDao: LocalEndpointDao,
    private val getEndpointReceiver: GetEndpointReceiver,
    private val context: Context
) {

    suspend fun notifyAllPending() {
        val parcels = storedParcelDao.listForRecipientLocation(RecipientLocation.LocalEndpoint)
        val recipients = parcels.map { it.recipientAddress }.distinct()
        val recipientAppIds = endpointDao.list(recipients).map { it.applicationId }.distinct()
        recipientAppIds.forEach {
            notifyApp(it)
        }
    }

    suspend fun notify(endpointAddress: MessageAddress) {
        val endpoint = endpointDao.get(endpointAddress)
        if (endpoint == null) {
            logger.warning(
                "Can't notify endpoint with unknown address $endpointAddress about incoming parcels"
            )
            return
        }

        notifyApp(endpoint.applicationId)
    }

    private fun notifyApp(applicationId: String) {
        val receiverName = getEndpointReceiver.get(applicationId) ?: run {
            logger.warning(
                "Failed to notify $applicationId about incoming parcels (receiver not found)"
            )
            return@notifyApp
        }

        context.sendBroadcast(
            Intent(RelaynetAndroid.ENDPOINT_NOTIFY_ACTION)
                .setComponent(
                    ComponentName(
                        applicationId,
                        receiverName
                    )
                )
        )
    }
}
