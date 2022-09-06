package tech.relaycorp.gateway.domain.publicsync

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.background.ForegroundAppMonitor
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.model.RegistrationState
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.pdc.local.PDCServer
import tech.relaycorp.gateway.pdc.local.PDCServerStateManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.seconds

@Singleton
class PublicSync
@Inject constructor(
    private val foregroundAppMonitor: ForegroundAppMonitor,
    private val pdcServerStateManager: PDCServerStateManager,
    private val internetGatewayPreferences: InternetGatewayPreferences,
    private val connectionStateObserver: ConnectionStateObserver,
    private val deliverParcelsToGateway: DeliverParcelsToGateway,
    private val collectParcelsFromGateway: CollectParcelsFromGateway
) {

    private var syncJob: Job? = null

    @VisibleForTesting
    val isSyncing
        get() = syncJob?.isActive == true

    suspend fun sync() {
        combine(
            foregroundAppMonitor.observe(),
            pdcServerStateManager.observe(),
            internetGatewayPreferences.observeRegistrationState(),
            connectionStateObserver.observe()
        ) { foregroundState, pdcState, registrationState, connectionState ->
            if (
                registrationState == RegistrationState.Done &&
                connectionState is ConnectionState.InternetWithGateway && (
                    foregroundState == ForegroundAppMonitor.State.Foreground ||
                        pdcState == PDCServer.State.Started
                    )
            ) {
                startSync()
            } else {
                stopSync()
            }
        }
            .collect()
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

    private fun startSync() {
        if (isSyncing) return

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
