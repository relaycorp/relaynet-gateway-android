package tech.relaycorp.gateway.domain.endpoint

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import tech.relaycorp.gateway.background.RelaynetAndroid
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.model.MessageAddress
import javax.inject.Inject

class NotifyEndpoint
@Inject constructor(
    private val endpointDao: LocalEndpointDao,
    private val getEndpointReceiver: GetEndpointReceiver,
    private val context: Context
) {

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
