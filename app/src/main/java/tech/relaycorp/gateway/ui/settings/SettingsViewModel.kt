package tech.relaycorp.gateway.ui.settings

import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.GetOutgoingData
import tech.relaycorp.gateway.domain.publicsync.MigrateGateway
import tech.relaycorp.gateway.ui.BaseViewModel
import tech.relaycorp.gateway.ui.common.Click
import tech.relaycorp.gateway.ui.main.PublishFlow
import javax.inject.Inject

class SettingsViewModel
@Inject constructor(
    getOutgoingData: GetOutgoingData,
    publicGatewayPreferences: PublicGatewayPreferences,
    migrateGateway: MigrateGateway
) : BaseViewModel() {

    val showOutgoingData: Flow<Boolean> get() = _showOutgoingData
    private val _showOutgoingData = MutableStateFlow(false)
    val outgoingData: Flow<GetOutgoingData.Data> get() = _outgoingData
    private val _outgoingData = MutableStateFlow(GetOutgoingData.Data())
    val publicGwAddress: Flow<String> get() = _publicGwAddress
    private val _publicGwAddress = MutableStateFlow("")
    val publicGwAddressEnabled: Flow<Boolean> get() = _publicGwAddressEnabled
    private val _publicGwAddressEnabled = MutableStateFlow(true)
    val publicGwSubmitEnabled: Flow<Boolean> get() = _publicGwSubmitEnabled
    private val _publicGwSubmitEnabled = MutableStateFlow(false)
    val errors: Flow<Error> get() = _errors.asFlow()
    private val _errors = PublishFlow<Error>()
    val messages: Flow<Message> get() = _messages.asFlow()
    private val _messages = PublishFlow<Message>()

    fun publicGwAddressChanged(value: String) {
        _publicGwAddressChanges.value = value
    }

    private val _publicGwAddressChanges = MutableStateFlow("")
    fun publicGwSubmitted() {
        _publicGwSubmits.sendBlocking(Click)
    }

    private val _publicGwSubmits = PublishFlow<Click>()

    init {
        getOutgoingData
            .get()
            .onEach {
                _outgoingData.value = it
                _showOutgoingData.value = !it.isZero
            }
            .launchIn(ioScope)

        ioScope.launch {
            _publicGwAddress.value = publicGatewayPreferences.getAddress()
        }

        _publicGwAddressChanges
            .onEach {
                val submitEnabled = it.isNotBlank() && it != _publicGwAddress.value
                _publicGwSubmitEnabled.value = submitEnabled
            }
            .launchIn(ioScope)

        _publicGwSubmits
            .asFlow()
            .filter { _publicGwSubmitEnabled.value }
            .onEach {
                _publicGwAddressEnabled.value = false
                _publicGwSubmitEnabled.value = false
                val address = _publicGwAddressChanges.value
                when (migrateGateway.migrate(address)) {
                    MigrateGateway.Result.Successful ->
                        _messages.send(Message.MigrationSuccessful)
                    MigrateGateway.Result.FailedToResolve -> {
                        _errors.send(Error.MigrationFailedToResolve)
                        _publicGwSubmitEnabled.value = true
                    }
                    MigrateGateway.Result.FailedToRegister -> {
                        _errors.send(Error.MigrationFailedToRegister)
                        _publicGwSubmitEnabled.value = true
                    }
                }
                _publicGwAddressEnabled.value = true
            }
            .launchIn(ioScope)
    }

    enum class Error {
        MigrationFailedToResolve,
        MigrationFailedToRegister
    }

    enum class Message {
        MigrationSuccessful
    }
}
