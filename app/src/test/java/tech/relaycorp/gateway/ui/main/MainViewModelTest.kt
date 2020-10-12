package tech.relaycorp.gateway.ui.main

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.data.preference.AppPreferences
import tech.relaycorp.gateway.domain.GetEndpointApplicationsCount
import tech.relaycorp.gateway.domain.GetOutgoingData
import tech.relaycorp.gateway.test.WaitAssertions.waitForAssertEquals

class MainViewModelTest {

    private val appPreferences = mock<AppPreferences>()
    private val connectionStateObserver = mock<ConnectionStateObserver>()
    private val getOutgoingData = mock<GetOutgoingData>()
    private val getEndpointApplicationsCount = mock<GetEndpointApplicationsCount>()

    @BeforeEach
    internal fun setUp() {
        whenever(appPreferences.isOnboardingDone()).thenReturn(flowOf(true))
        whenever(connectionStateObserver.observe())
            .thenReturn(flowOf(ConnectionState.InternetAndPublicGateway))
        whenever(getEndpointApplicationsCount.get()).thenReturn(flowOf(0))
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
    internal fun `data to sync invisible when connected to public gateway`() = runBlockingTest {
        whenever(connectionStateObserver.observe())
            .thenReturn(flowOf(ConnectionState.InternetAndPublicGateway))
        whenever(getOutgoingData.any()).thenReturn(flowOf(false))

        assertEquals(
            MainViewModel.DataState.Invisible,
            buildViewModel().dataState.first()
        )
    }

    @Test
    internal fun `data to sync visible with outgoing data`() = runBlockingTest {
        whenever(connectionStateObserver.observe()).thenReturn(flowOf(ConnectionState.Disconnected))
        whenever(getOutgoingData.any()).thenReturn(flowOf(true))

        waitForAssertEquals(
            MainViewModel.DataState.Visible.WithOutgoingData,
            buildViewModel().dataState::first
        )
    }

    @Test
    internal fun `data to sync visible without outgoing data`() = runBlockingTest {
        whenever(connectionStateObserver.observe()).thenReturn(flowOf(ConnectionState.Disconnected))
        whenever(getOutgoingData.any()).thenReturn(flowOf(false))

        waitForAssertEquals(
            MainViewModel.DataState.Visible.WithoutOutgoingData,
            buildViewModel().dataState::first
        )
    }

    private fun buildViewModel() =
        MainViewModel(
            appPreferences,
            connectionStateObserver,
            getOutgoingData,
            getEndpointApplicationsCount
        )
}
