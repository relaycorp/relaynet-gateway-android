package tech.relaycorp.gateway.ui.main

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.data.preference.AppPreferences
import tech.relaycorp.gateway.test.WaitAssertions.waitForAssertEquals

class MainViewModelTest {

    private val appPreferences = mock<AppPreferences>()
    private val connectionStateObserver = mock<ConnectionStateObserver>()

    @BeforeEach
    internal fun setUp() {
        whenever(appPreferences.isOnboardingDone()).thenReturn(flowOf(true))
        whenever(connectionStateObserver.observe())
            .thenReturn(flowOf(ConnectionState.InternetAndPublicGateway))
    }

    @Test
    internal fun `open onboarding if not done`() = runBlockingTest {
        whenever(appPreferences.isOnboardingDone()).thenReturn(flowOf(false))

        waitForAssertEquals(
            Unit,
            buildViewModel().openOnboarding::first
        )
    }

    @Test
    internal fun `connection state is passed through`() = runBlockingTest {
        whenever(connectionStateObserver.observe())
            .thenReturn(flowOf(ConnectionState.InternetAndPublicGateway))

        waitForAssertEquals(
            ConnectionState.InternetAndPublicGateway,
            buildViewModel().connectionState::first
        )
    }

    private fun buildViewModel() =
        MainViewModel(
            appPreferences,
            connectionStateObserver
        )
}
