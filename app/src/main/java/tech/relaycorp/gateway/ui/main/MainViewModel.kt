package tech.relaycorp.gateway.ui.main

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.data.model.StorageSize
import tech.relaycorp.gateway.domain.GetEndpointApplicationsCount
import tech.relaycorp.gateway.domain.GetTotalOutgoingData
import tech.relaycorp.gateway.ui.BaseViewModel
import javax.inject.Inject

class MainViewModel
@Inject constructor(
    private val connectionStateObserver: ConnectionStateObserver,
    private val getTotalOutgoingData: GetTotalOutgoingData,
    private val getEndpointApplicationsCount: GetEndpointApplicationsCount
) : BaseViewModel() {

    // Outputs

    val connectionState get() = connectionStateObserver.observe()

    val dataToSyncState: Flow<DataToSyncState> get() = _dataToSyncState
    private val _dataToSyncState = MutableStateFlow<DataToSyncState>(DataToSyncState.Invisible)

    val isCourierSyncVisible
        get() = connectionStateObserver.observe()
            .map { it !is ConnectionState.InternetAndPublicGateway }

    init {
        connectionStateObserver
            .observe()
            .flatMapLatest { connectionState ->
                combine(
                    getTotalOutgoingData.get(),
                    getEndpointApplicationsCount.get()
                ) { outgoingData, appCount ->
                    when (connectionState) {
                        is ConnectionState.InternetAndPublicGateway -> DataToSyncState.Invisible
                        else ->
                            if (appCount > 0) {
                                DataToSyncState.Visible.WithApplications(outgoingData)
                            } else {
                                DataToSyncState.Visible.WithoutApplications
                            }
                    }
                }
            }
            .onEach { _dataToSyncState.value = it }
            .launchIn(ioScope)
    }

    sealed class DataToSyncState {
        object Invisible : DataToSyncState()
        sealed class Visible : DataToSyncState() {
            data class WithApplications(val dataWaitingToSync: StorageSize) : Visible()
            object WithoutApplications : Visible()
        }
    }
}
