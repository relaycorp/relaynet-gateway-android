package tech.relaycorp.gateway.domain.publicsync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import tech.relaycorp.gateway.background.ForegroundAppMonitor
import tech.relaycorp.gateway.common.Logging.logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.seconds

@Singleton
class PublicSync
@Inject constructor(
    private val foregroundAppMonitor: ForegroundAppMonitor,
    private val deliverPublicParcels: DeliverPublicParcels,
    private val collectPublicParcels: CollectPublicParcels
) {

    private var syncJob: Job? = null
    private val isSyncing get() = syncJob?.isActive == true

    suspend fun syncOnAppForeground() {
        foregroundAppMonitor
            .observe()
            .collect { state ->
                when (state) {
                    ForegroundAppMonitor.State.Foreground -> startSync()
                    ForegroundAppMonitor.State.Background -> stopSync()
                }
            }
    }

    suspend fun syncOneOff() {
        if (isSyncing) return // If the app is on the foreground it's already syncing

        logger.info("Starting Public Gateway Sync (one-off)")
        deliverPublicParcels.deliver(false)
        delay(3.seconds)
        collectPublicParcels.collect(false)
    }

    private fun startSync() {
        logger.info("App on foreground, starting public sync")
        val syncJob = Job()
        this.syncJob = syncJob
        val syncScope = CoroutineScope(syncJob + Dispatchers.IO)
        syncScope.launch { deliverPublicParcels.deliver(true) }
        syncScope.launch { collectPublicParcels.collect(true) }
    }

    private fun stopSync() {
        logger.info("App on background, stopping public sync")
        syncJob?.cancel()
    }
}
