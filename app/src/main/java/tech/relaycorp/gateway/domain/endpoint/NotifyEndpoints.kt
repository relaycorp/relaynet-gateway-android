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
    private val context: Context,
) {

    fun notify(localEndpoints: List<LocalEndpoint>, notificationType: NotificationType) =
        localEndpoints
            .distinct() // mapper and use strings
            .forEach { notify(it, notificationType) }

    fun notify(localEndpoint: LocalEndpoint, notificationType: NotificationType) {
        val receiverName =
            getEndpointReceiver.get(localEndpoint.applicationId, notificationType) ?: run {
                logger.warning(
                    "Failed to notify ${localEndpoint.applicationId} " +
                        "about ${notificationType.name} (receiver not found)",
                )
                return@notify
            }

        context.sendBroadcast(
            Intent(notificationType.action)
                .setComponent(
                    ComponentName(
                        localEndpoint.applicationId,
                        receiverName,
                    ),
                ),
        )
    }
}
