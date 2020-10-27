package tech.relaycorp.gateway.ui.main

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import tech.relaycorp.gateway.background.ConnectionState
import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.data.preference.AppPreferences
import tech.relaycorp.gateway.domain.GetEndpointApplicationsCount
import tech.relaycorp.gateway.domain.GetOutgoingData
import tech.relaycorp.gateway.ui.BaseViewModel
import javax.inject.Inject

class MainViewModel
@Inject constructor(
    appPreferences: AppPreferences,
    private val connectionStateObserver: ConnectionStateObserver,
    private val getOutgoingData: GetOutgoingData,
    private val getEndpointApplicationsCount: GetEndpointApplicationsCount
) : BaseViewModel() {

    // Outputs

    val openOnboarding: Flow<Unit> get() = _openOnboarding.filterNotNull()
    private val _openOnboarding = MutableStateFlow<Unit?>(null)

    val connectionState get() = connectionStateObserver.observe()

    val dataState: Flow<DataState> get() = _dataState
    private val _dataState = MutableStateFlow<DataState>(DataState.Invisible)

    val appsState: Flow<AppsState> get() = _appsState
    private val _appsState = MutableStateFlow<AppsState>(AppsState.None)

    val isCourierSyncVisible
        get() = connectionStateObserver.observe()
            .map { it !is ConnectionState.InternetAndPublicGateway }

    init {
        appPreferences
            .isOnboardingDone()
            .take(1)
            .onEach { isDone -> if (!isDone) _openOnboarding.value = Unit }
            .launchIn(ioScope)

        connectionStateObserver
            .observe()
            .flatMapLatest { connectionState ->
                getOutgoingData
                    .any()
                    .map { anyOutgoingData ->
                        when (connectionState) {
                            is ConnectionState.InternetAndPublicGateway -> DataState.Invisible
                            else ->
                                if (anyOutgoingData) {
                                    DataState.Visible.WithOutgoingData
                                } else {
                                    DataState.Visible.WithoutOutgoingData
                                }
                        }
                    }
            }
            .onEach { _dataState.value = it }
            .launchIn(ioScope)

        getEndpointApplicationsCount.get()
            .map { if (it > 0) AppsState.Some else AppsState.None }
            .onEach { _appsState.value = it }
            .launchIn(ioScope)
    }

    sealed class DataState {
        object Invisible : DataState()
        sealed class Visible : DataState() {
            object WithOutgoingData : Visible()
            object WithoutOutgoingData : Visible()
        }
    }

    enum class AppsState {
        Some, None
    }
}
