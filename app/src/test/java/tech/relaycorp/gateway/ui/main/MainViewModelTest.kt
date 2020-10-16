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
import tech.relaycorp.gateway.data.model.StorageSize
import tech.relaycorp.gateway.data.preference.AppPreferences
import tech.relaycorp.gateway.domain.GetEndpointApplicationsCount
import tech.relaycorp.gateway.domain.GetTotalOutgoingData
import tech.relaycorp.gateway.test.WaitAssertions.waitForAssertEquals

class MainViewModelTest {

    private val appPreferences = mock<AppPreferences>()
    private val connectionStateObserver = mock<ConnectionStateObserver>()
    private val getTotalOutgoingData = mock<GetTotalOutgoingData>()
    private val getEndpointApplicationsCount = mock<GetEndpointApplicationsCount>()

    @BeforeEach
    internal fun setUp() {
        whenever(appPreferences.isOnboardingDone()).thenReturn(flowOf(true))
        whenever(connectionStateObserver.observe())
            .thenReturn(flowOf(ConnectionState.InternetAndPublicGateway))
        whenever(getTotalOutgoingData.get()).thenReturn(flowOf(StorageSize.ZERO))
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
        whenever(getTotalOutgoingData.get()).thenReturn(flowOf(StorageSize.ZERO))

        assertEquals(
            MainViewModel.DataState.Invisible,
            buildViewModel().dataState.first()
        )
    }

    @Test
    internal fun `data to sync visible with outgoing data`() = runBlockingTest {
        whenever(connectionStateObserver.observe()).thenReturn(flowOf(ConnectionState.Disconnected))
        val totalSize = StorageSize(100)
        whenever(getTotalOutgoingData.get()).thenReturn(flowOf(totalSize))

        waitForAssertEquals(
            MainViewModel.DataState.Visible.WithOutgoingData(totalSize),
            buildViewModel().dataState::first
        )
    }

    @Test
    internal fun `data to sync visible without outgoing data`() = runBlockingTest {
        whenever(connectionStateObserver.observe()).thenReturn(flowOf(ConnectionState.Disconnected))
        whenever(getTotalOutgoingData.get()).thenReturn(flowOf(StorageSize.ZERO))

        waitForAssertEquals(
            MainViewModel.DataState.Visible.WithoutOutgoingData,
            buildViewModel().dataState::first
        )
    }

    private fun buildViewModel() =
        MainViewModel(
            appPreferences,
            connectionStateObserver,
            getTotalOutgoingData,
            getEndpointApplicationsCount
        )
}
