package tech.relaycorp.gateway.ui.main

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.data.preference.AppPreferences
import tech.relaycorp.gateway.test.WaitAssertions.waitForAssertEquals

class MainViewModelTest {

    private val appPreferences = mock<AppPreferences>()
    private val connectionStateObserver = mock<ConnectionStateObserver>()
    private val onboardingDoneFlow = PublishFlow<Boolean>()
    private val connectionStateObserve = PublishFlow<ConnectionState>()
    private lateinit var viewModel: MainViewModel

    @BeforeEach
    internal fun setUp() {
        whenever(appPreferences.isOnboardingDone())
            .thenReturn(onboardingDoneFlow.asSharedFlow())
        whenever(connectionStateObserver.observe())
            .thenReturn(connectionStateObserve.asSharedFlow())
        viewModel = buildViewModel()
    }

    @Test
    internal fun `open onboarding if not done`() {
        viewModel.ioScope.launch {
            onboardingDoneFlow.emit(false)
            waitForAssertEquals(
                ConnectionState.InternetWithGateway,
                viewModel.openOnboarding::first,
            )
        }
    }

    @Test
    internal fun `connection state is passed through`() {
        viewModel.ioScope.launch {
            connectionStateObserve.emit(ConnectionState.InternetWithGateway)
            waitForAssertEquals(
                ConnectionState.InternetWithGateway,
                viewModel.connectionState::first,
            )
        }
    }

    private fun buildViewModel() = MainViewModel(
        appPreferences,
        connectionStateObserver,
    )
}
