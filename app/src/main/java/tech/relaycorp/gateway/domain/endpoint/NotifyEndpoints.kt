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
import java.util.logging.Level
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
        recipients.forEach {
            try {
                notify(it)
            } catch (e: UnreachableEndpointApp) {
                logger.log(Level.WARNING, "Notification Error", e)
            }
        }
    }

    @Throws(UnreachableEndpointApp::class)
    suspend fun notify(endpointAddress: MessageAddress) {
        val endpoint = endpointDao.get(endpointAddress)
        if (endpoint == null) {
            logger.warning("Can't notify endpoint with unknown address $endpointAddress")
            return
        }

        val receiverName = getEndpointReceiver.get(endpoint.applicationId)
            ?: throw UnreachableEndpointApp("Can't notify ${endpoint.applicationId}")

        context.sendBroadcast(
            Intent(RelaynetAndroid.ENDPOINT_NOTIFY_ACTION)
                .setComponent(
                    ComponentName(
                        endpoint.applicationId,
                        receiverName
                    )
                )
        )
    }

    class UnreachableEndpointApp(message: String) : Exception(message)
}
