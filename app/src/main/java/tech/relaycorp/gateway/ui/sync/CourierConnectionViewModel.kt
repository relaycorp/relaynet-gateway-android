package tech.relaycorp.gateway.ui.sync

import tech.relaycorp.gateway.background.ConnectionStateObserver
import tech.relaycorp.gateway.ui.BaseViewModel
import javax.inject.Inject

class CourierConnectionViewModel
@Inject constructor(
    private val connectionStateObserver: ConnectionStateObserver,
) : BaseViewModel() {

    // Outputs

    val state get() = connectionStateObserver.observe()
}
