package tech.relaycorp.gateway.domain.publicsync

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.ktor.test.dispatcher.testSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.background.ForegroundAppMonitor
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.pdc.local.PDCServer
import tech.relaycorp.gateway.pdc.local.PDCServerStateManager
import tech.relaycorp.gateway.test.WaitAssertions.waitForAssertEquals

class PublicSyncTest {

    private val foregroundAppMonitor = mock<ForegroundAppMonitor>()
    private val pdcServerStateManager = mock<PDCServerStateManager>()
    private val internetGatewayPreferences = mock<InternetGatewayPreferences>()
    private val connectionStateObserver = mock<ConnectionStateObserver>()
    private val registerGateway = mock<RegisterGateway>()
    private val deliverParcelsToGateway = mock<DeliverParcelsToGateway>()
    private val collectParcelsFromGateway = mock<CollectParcelsFromGateway>()
    private val publicSync = PublicSync(
        foregroundAppMonitor,
        pdcServerStateManager,
        internetGatewayPreferences,
        connectionStateObserver,
        registerGateway,
        deliverParcelsToGateway,
        collectParcelsFromGateway
    )

    @Test
    internal fun `does not sync if gateway is failed to register`() = testSuspend {
        setState(
            ForegroundAppMonitor.State.Foreground,
            PDCServer.State.Started,
            ConnectionState.InternetWithGateway,
            RegisterGateway.Result.FailedToRegister
        )

        sync()

        assertFalse(publicSync.isSyncing)
    }

    @Test
    internal fun `does not sync if there is no internet`() = testSuspend {
        setState(
            ForegroundAppMonitor.State.Foreground,
            PDCServer.State.Started,
            ConnectionState.Disconnected,
            RegisterGateway.Result.AlreadyRegisteredAndNotExpiring
        )

        sync()

        assertFalse(publicSync.isSyncing)
    }

    @Test
    internal fun `does not sync if app is on background and PDC server stopped`() = testSuspend {
        setState(
            ForegroundAppMonitor.State.Background,
            PDCServer.State.Stopped,
            ConnectionState.InternetWithGateway,
            RegisterGateway.Result.AlreadyRegisteredAndNotExpiring
        )

        sync()

        assertFalse(publicSync.isSyncing)
    }

    @Test
    internal fun `syncs if app is on foreground`() = testSuspend {
        setState(
            ForegroundAppMonitor.State.Foreground,
            PDCServer.State.Stopped,
            ConnectionState.InternetWithGateway,
            RegisterGateway.Result.AlreadyRegisteredAndNotExpiring
        )

        sync()

        assertTrue(publicSync.isSyncing)
    }

    @Test
    internal fun `syncs if PDC server is started`() = testSuspend {
        setState(
            ForegroundAppMonitor.State.Background,
            PDCServer.State.Started,
            ConnectionState.InternetWithGateway,
            RegisterGateway.Result.AlreadyRegisteredAndNotExpiring
        )

        sync()

        assertTrue(publicSync.isSyncing)
    }

    @Test
    internal fun `sync stops if app goes to background`() = testSuspend {
        setState(
            ForegroundAppMonitor.State.Background,
            PDCServer.State.Stopped,
            ConnectionState.InternetWithGateway,
            RegisterGateway.Result.AlreadyRegisteredAndNotExpiring
        )
        val appStateFlow = MutableStateFlow(ForegroundAppMonitor.State.Background)
        whenever(foregroundAppMonitor.observe()).thenReturn(appStateFlow.asSharedFlow())

        sync()

        waitForAssertEquals(false) { publicSync.isSyncing }
        appStateFlow.value = ForegroundAppMonitor.State.Foreground
        waitForAssertEquals(true) { publicSync.isSyncing }
        appStateFlow.value = ForegroundAppMonitor.State.Background
        waitForAssertEquals(false) { publicSync.isSyncing }
    }

    private suspend fun setState(
        appState: ForegroundAppMonitor.State,
        pdcState: PDCServer.State,
        connectionState: ConnectionState,
        registrationResult: RegisterGateway.Result
    ) {
        whenever(foregroundAppMonitor.observe())
            .thenReturn(flowOf(appState))
        whenever(pdcServerStateManager.observe())
            .thenReturn(flowOf(pdcState))
        whenever(connectionStateObserver.observe())
            .thenReturn(flowOf(connectionState))
        whenever(registerGateway.registerIfNeeded())
            .thenReturn(registrationResult)
    }

    private fun sync() {
        CoroutineScope(Dispatchers.Unconfined).launch {
            publicSync.sync()
        }
    }
}
