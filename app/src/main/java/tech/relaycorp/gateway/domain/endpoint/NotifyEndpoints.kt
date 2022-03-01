package tech.relaycorp.gateway.domain.endpoint

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.model.LocalEndpoint
import javax.inject.Inject

class NotifyEndpoints
@Inject constructor(
    private val getEndpointReceiver: GetEndpointReceiver,
    private val context: Context
) {

    fun notify(localEndpoints: List<LocalEndpoint>, endpointNotifyAction: EndpointNotifyAction) =
        localEndpoints
            .distinct() // mapper and use strings
            .forEach { notify(it, endpointNotifyAction) }

    fun notify(localEndpoint: LocalEndpoint, endpointNotifyAction: EndpointNotifyAction) {
        val receiverName = getEndpointReceiver.get(localEndpoint.applicationId, endpointNotifyAction) ?: run {
            logger.warning(
                "Failed to notify ${localEndpoint.applicationId} " +
                    "about ${endpointNotifyAction.name} (receiver not found)"
            )
            return@notify
        }

        context.sendBroadcast(
            Intent(endpointNotifyAction.action)
                .setComponent(
                    ComponentName(
                        localEndpoint.applicationId,
                        receiverName
                    )
                )
        )
    }
}
