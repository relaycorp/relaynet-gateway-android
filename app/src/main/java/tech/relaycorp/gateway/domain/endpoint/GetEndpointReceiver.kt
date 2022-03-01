package tech.relaycorp.gateway.domain.endpoint

import android.content.Intent
import android.content.pm.PackageManager
import javax.inject.Inject

open class GetEndpointReceiver
@Inject constructor(
    private val packageManager: PackageManager
) {
    open fun get(applicationId: String): String? {
        val intent = Intent(EndpointNotifyAction.ParcelToReceive.action)
            .setPackage(applicationId)

        return packageManager.queryBroadcastReceivers(intent, 0).firstOrNull()?.activityInfo?.name
    }
}
