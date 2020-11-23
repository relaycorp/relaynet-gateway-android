package tech.relaycorp.gateway.pdc.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PDCServerStateManager
@Inject constructor() {

    private val stateFlow = MutableStateFlow(PDCServer.State.Stopped)

    fun set(state: PDCServer.State) {
        stateFlow.value = state
    }

    fun observe(): Flow<PDCServer.State> = stateFlow.asSharedFlow()
}
