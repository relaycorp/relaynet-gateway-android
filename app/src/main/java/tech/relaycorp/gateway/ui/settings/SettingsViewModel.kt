package tech.relaycorp.gateway.ui.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.domain.GetOutgoingData
import tech.relaycorp.gateway.ui.BaseViewModel
import javax.inject.Inject

class SettingsViewModel
@Inject constructor(
    getOutgoingData: GetOutgoingData
) : BaseViewModel() {

    val showOutgoingData: Flow<Boolean> get() = _showOutgoingData
    private val _showOutgoingData = MutableStateFlow(false)
    val outgoingData: Flow<GetOutgoingData.Data> get() = _outgoingData
    private val _outgoingData = MutableStateFlow<GetOutgoingData.Data>(GetOutgoingData.Data())

    init {
        getOutgoingData
            .get()
            .onEach {
                _outgoingData.value = it
                _showOutgoingData.value = !it.isZero
            }
            .launchIn(ioScope)
    }
}
