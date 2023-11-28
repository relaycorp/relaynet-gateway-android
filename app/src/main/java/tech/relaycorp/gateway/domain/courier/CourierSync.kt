package tech.relaycorp.gateway.domain.courier

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import tech.relaycorp.gateway.common.Logging.logger
import java.util.logging.Level
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class CourierSync
@Inject constructor(
    private val cargoDelivery: CargoDelivery,
    private val cargoCollection: CargoCollection,
) {

    private val state = MutableStateFlow(State.Initial)
    fun state(): Flow<State> = state

    suspend fun sync() {
        try {
            syncUnhandled()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Courier sync error", e)
            state.value = State.Error
        }
    }

    private suspend fun syncUnhandled() {
        state.value = State.CollectingCargo
        cargoCollection.collect()

        state.value = State.Waiting
        delay(WAIT_PERIOD)

        state.value = State.DeliveringCargo
        cargoDelivery.deliver()

        state.value = State.Finished
    }

    enum class State {
        DeliveringCargo,
        Waiting,
        CollectingCargo,
        Finished,
        Error,
        ;

        companion object {
            val Initial = DeliveringCargo
        }
    }

    companion object {
        private val WAIT_PERIOD = 2.seconds
    }
}
