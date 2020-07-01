package tech.relaycorp.gateway.ui.sync

import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*
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

    fun stopClicked() = stopClicks.sendBlocking(Click)
    private val stopClicks = PublishFlow<Click>()

    // Outputs

    private val _state = MutableStateFlow(CourierSync.State.Initial)
    val state = courierSync.state()

    private val _finish = PublishFlow<Finish>()
    val finish get() = _finish.asFlow()

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
            .asFlow()
            .onEach {
                syncStateJob.cancel()
                syncJob.cancel()
                _finish.send(Finish)
            }
            .launchIn(ioScope)
    }
}
