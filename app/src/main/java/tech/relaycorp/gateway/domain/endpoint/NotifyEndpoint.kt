package tech.relaycorp.gateway.domain.endpoint

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
    private val context: Context
) {

    suspend fun notify(endpointAddress: MessageAddress) {
        endpointDao.get(endpointAddress)
            ?.let { endpoint ->
                context.sendBroadcast(
                    Intent(RelaynetAndroid.ENDPOINT_NOTIFY_ACTION)
                        .setPackage(endpoint.applicationId),
                    RelaynetAndroid.GATEWAY_SYNC_PERMISSION
                )
            }
            ?: logger.warning("Can't notify endpoint with unknown address $endpointAddress")
    }
}
