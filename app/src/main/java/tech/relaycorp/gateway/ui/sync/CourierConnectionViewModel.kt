package tech.relaycorp.gateway.ui.sync

import tech.relaycorp.gateway.background.CourierConnectionObserver
import tech.relaycorp.gateway.ui.BaseViewModel
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
