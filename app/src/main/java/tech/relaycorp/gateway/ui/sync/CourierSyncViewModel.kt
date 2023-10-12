package tech.relaycorp.gateway.ui.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.relaycorp.gateway.domain.courier.CourierSync
import tech.relaycorp.gateway.ui.BaseViewModel
import tech.relaycorp.gateway.ui.common.Click
import tech.relaycorp.gateway.ui.common.Finish
import tech.relaycorp.gateway.ui.main.PublishFlow
import javax.inject.Inject

class CourierSyncViewModel
@Inject constructor(
    private val courierSync: CourierSync
) : BaseViewModel() {

    // Inputs

    fun stopClicked() = stopClicks.tryEmit(Click)
    private val stopClicks = PublishFlow<Click>()

    // Outputs

    private val _state = MutableStateFlow(CourierSync.State.Initial)
    val state: Flow<CourierSync.State> = _state

    private val _finish = PublishFlow<Finish>()
    val finish get() = _finish.asSharedFlow()

    init {
        val syncJob = ioScope.launch {
            courierSync.sync()
        }

        val syncStateJob =
            courierSync
                .state()
                .onEach { _state.value = it }
                .launchIn(ioScope)

        stopClicks
            .asSharedFlow()
            .onEach {
                syncStateJob.cancel()
                syncJob.cancel()
                _finish.emit(Finish)
            }
            .launchIn(ioScope)
    }
}
