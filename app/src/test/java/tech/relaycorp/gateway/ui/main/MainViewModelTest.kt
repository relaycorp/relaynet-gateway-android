package tech.relaycorp.gateway.ui.main

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.data.model.StorageSize
import tech.relaycorp.gateway.domain.GetEndpointApplicationsCount
import tech.relaycorp.gateway.domain.GetTotalOutgoingData
import tech.relaycorp.gateway.test.WaitAssertions.waitForAssertEquals

class MainViewModelTest {

    private val connectionStateObserver = mock<ConnectionStateObserver>()
    private val getTotalOutgoingData = mock<GetTotalOutgoingData>()
    private val getEndpointApplicationsCount = mock<GetEndpointApplicationsCount>()

    @Test
    internal fun `data to sync invisible when connected to public gateway`() = runBlockingTest {
        whenever(connectionStateObserver.observe())
            .thenReturn(flowOf(ConnectionState.InternetAndPublicGateway))
        whenever(getTotalOutgoingData.get()).thenReturn(flowOf(StorageSize.ZERO))
        whenever(getEndpointApplicationsCount.get()).thenReturn(flowOf(0))

        assertEquals(
            MainViewModel.DataToSyncState.Invisible,
            buildViewModel().dataToSyncState.first()
        )
    }

    @Test
    internal fun `data to sync visible with applications`() = runBlockingTest {
        whenever(connectionStateObserver.observe()).thenReturn(flowOf(ConnectionState.Disconnected))
        val totalSize = StorageSize(100)
        whenever(getTotalOutgoingData.get()).thenReturn(flowOf(totalSize))
        whenever(getEndpointApplicationsCount.get()).thenReturn(flowOf(1))

        waitForAssertEquals(
            MainViewModel.DataToSyncState.Visible.WithApplications(totalSize),
            buildViewModel().dataToSyncState::first
        )
    }

    @Test
    internal fun `data to sync visible without applications`() = runBlockingTest {
        whenever(connectionStateObserver.observe()).thenReturn(flowOf(ConnectionState.Disconnected))
        whenever(getTotalOutgoingData.get()).thenReturn(flowOf(StorageSize.ZERO))
        whenever(getEndpointApplicationsCount.get()).thenReturn(flowOf(0))

        waitForAssertEquals(
            MainViewModel.DataToSyncState.Visible.WithoutApplications,
            buildViewModel().dataToSyncState::first
        )
    }

    private fun buildViewModel() =
        MainViewModel(connectionStateObserver, getTotalOutgoingData, getEndpointApplicationsCount)
}
