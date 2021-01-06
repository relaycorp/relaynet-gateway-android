package tech.relaycorp.gateway.ui.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.GetOutgoingData
import tech.relaycorp.gateway.ui.BaseViewModel
import javax.inject.Inject

class SettingsViewModel
@Inject constructor(
    getOutgoingData: GetOutgoingData,
    publicGatewayPreferences: PublicGatewayPreferences
) : BaseViewModel() {

    val showOutgoingData: Flow<Boolean> get() = _showOutgoingData
    private val _showOutgoingData = MutableStateFlow(false)
    val outgoingData: Flow<GetOutgoingData.Data> get() = _outgoingData
    private val _outgoingData = MutableStateFlow(GetOutgoingData.Data())
    val publicGwAddress: Flow<String> get() = _publicGwAddress
    private val _publicGwAddress = MutableStateFlow("")

    init {
        getOutgoingData
            .get()
            .onEach {
                _outgoingData.value = it
                _showOutgoingData.value = !it.isZero
            }
            .launchIn(ioScope)

        publicGatewayPreferences
            .observeAddress()
            .onEach {
                _publicGwAddress.value = publicGatewayPreferences.getAddress()
            }
            .launchIn(ioScope)
    }
}
