package tech.relaycorp.gateway.ui.settings

import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.publicsync.MigrateGateway
import tech.relaycorp.gateway.ui.BaseViewModel
import tech.relaycorp.gateway.ui.common.Click
import tech.relaycorp.gateway.ui.common.Finish
import tech.relaycorp.gateway.ui.main.PublishFlow
import javax.inject.Inject
import javax.inject.Named

class MigrateGatewayViewModel
@Inject constructor(
    internetGatewayPreferences: InternetGatewayPreferences,
    migrateGateway: MigrateGateway,
    @Named("validator_hostname")
    hostnameValidator: (@JvmSuppressWildcards String) -> @JvmSuppressWildcards Boolean
) : BaseViewModel() {

    val state: Flow<State> get() = _state
    private val _state = MutableStateFlow<State>(State.Insert)
    val finishSuccessfully get() = _finishSuccessfully.asFlow()
    private val _finishSuccessfully = PublishFlow<Finish>()

    fun addressChanged(value: String) {
        _addressChanges.value = value
    }

    private val _addressChanges = MutableStateFlow("")

    fun submitted() {
        _submits.trySendBlocking(Click)
    }

    private val _submits = PublishFlow<Click>()

    init {
        _addressChanges
            .onEach {
                _state.value = when {
                    it.isBlank() -> State.Insert
                    it == internetGatewayPreferences.getAddress() -> State.Error.SameAddress
                    hostnameValidator(it) -> State.AddressValid
                    else -> State.Error.AddressInvalid
                }
            }
            .launchIn(ioScope)

        _submits
            .asFlow()
            .filter { _state.value == State.AddressValid }
            .onEach {
                _state.value = State.Submitting
                val address = _addressChanges.value
                when (migrateGateway.migrate(address)) {
                    MigrateGateway.Result.Successful ->
                        _finishSuccessfully.send(Finish)
                    MigrateGateway.Result.FailedToResolve ->
                        _state.value = State.Error.FailedToResolve
                    MigrateGateway.Result.FailedToRegister ->
                        _state.value = State.Error.FailedToRegister
                }
            }
            .launchIn(ioScope)
    }

    sealed class State {
        object Insert : State()
        object AddressValid : State()
        object Submitting : State()
        sealed class Error : State() {
            object AddressInvalid : Error()
            object SameAddress : Error()
            object FailedToResolve : Error()
            object FailedToRegister : Error()
        }
    }
}
