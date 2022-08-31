package tech.relaycorp.gateway.ui.main

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.asFlow
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
        whenever(appPreferences.isOnboardingDone()).thenReturn(onboardingDoneFlow.asFlow())
        whenever(connectionStateObserver.observe()).thenReturn(connectionStateObserve.asFlow())
        viewModel = buildViewModel()
    }

    @Test
    internal fun `open onboarding if not done`() {
        viewModel.ioScope.launch {
            onboardingDoneFlow.send(false)
            waitForAssertEquals(
                ConnectionState.InternetAndGateway,
                viewModel.openOnboarding::first
            )
        }
    }

    @Test
    internal fun `connection state is passed through`() {
        viewModel.ioScope.launch {
            connectionStateObserve.send(ConnectionState.InternetAndGateway)
            waitForAssertEquals(
                ConnectionState.InternetAndGateway,
                viewModel.connectionState::first
            )
        }
    }

    private fun buildViewModel() =
        MainViewModel(
            appPreferences,
            connectionStateObserver
        )
}
