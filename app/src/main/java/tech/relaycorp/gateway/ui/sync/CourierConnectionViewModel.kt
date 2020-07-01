package tech.relaycorp.gateway.ui.sync

import kotlinx.coroutines.channels.sendBlocking
import tech.relaycorp.gateway.background.CourierConnectionObserver
import tech.relaycorp.gateway.ui.BaseViewModel
import tech.relaycorp.gateway.ui.common.Click
import tech.relaycorp.gateway.ui.main.PublishFlow
import javax.inject.Inject

class CourierConnectionViewModel
@Inject constructor(
    private val connectionObserver: CourierConnectionObserver
) : BaseViewModel() {

    // Outputs

    val state get() = connectionObserver.observe()

    init {

    }
}
