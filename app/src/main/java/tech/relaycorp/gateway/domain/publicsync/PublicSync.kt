package tech.relaycorp.gateway.domain.publicsync

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.background.ForegroundAppMonitor
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.common.interval
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.pdc.local.PDCServer
import tech.relaycorp.gateway.pdc.local.PDCServerStateManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Singleton
class PublicSync
@Inject constructor(
    private val foregroundAppMonitor: ForegroundAppMonitor,
    private val pdcServerStateManager: PDCServerStateManager,
    private val internetGatewayPreferences: InternetGatewayPreferences,
    private val connectionStateObserver: ConnectionStateObserver,
    private val registerGateway: RegisterGateway,
    private val deliverParcelsToGateway: DeliverParcelsToGateway,
    private val collectParcelsFromGateway: CollectParcelsFromGateway,
) {

    private var syncJob: Job? = null

    @VisibleForTesting
    val isSyncing
        get() = syncJob?.isActive == true

    suspend fun sync() {
        combine(
            foregroundAppMonitor.observe(),
            pdcServerStateManager.observe(),
            // Retry registration and sync every minute in case there's a failure
            interval(1.minutes),
        ) { foregroundState, pdcState, _ -> Pair(foregroundState, pdcState) }
            .flatMapLatest { (foregroundState, pdcState) ->
                if (
                    foregroundState == ForegroundAppMonitor.State.Foreground ||
                    pdcState == PDCServer.State.Started
                ) {
                    connectionStateObserver.observe()
                        .map { it is ConnectionState.InternetWithGateway }
                } else {
                    flowOf(false)
                }
            }.collect { syncShouldBeRunning ->
                if (syncShouldBeRunning) {
                    startSync()
                } else {
                    stopSync()
                }
            }
    }

    suspend fun syncOneOff() {
        // If the app is on the foreground, it's already syncing or will be shortly
        // If it's not registered yet, we don't want to sync
        if (isSyncing || isForeground() || !isRegistered()) return

        logger.info("Starting Public Gateway Sync (one-off)")
        deliverParcelsToGateway.deliver(false)
        delay(3.seconds)
        collectParcelsFromGateway.collect(false)
    }

    private suspend fun startSync() {
        if (isSyncing) return
        if (!registerGateway.registerIfNeeded().isSuccessful) return

        logger.info("Starting public sync")
        val syncJob = Job()
        this.syncJob = syncJob
        val syncScope = CoroutineScope(syncJob + Dispatchers.IO)
        syncScope.launch { deliverParcelsToGateway.deliver(true) }
        syncScope.launch { collectParcelsFromGateway.collect(true) }
    }

    private fun stopSync() {
        if (isSyncing) {
            logger.info("Stopping public sync")
            syncJob?.cancel()
        }
    }

    private suspend fun isRegistered() =
        internetGatewayPreferences.getRegistrationState() == RegistrationState.Done

    private suspend fun isForeground() =
        foregroundAppMonitor.observe().first() == ForegroundAppMonitor.State.Foreground
}
