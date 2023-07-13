package tech.relaycorp.gateway.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

fun interval(duration: Duration) = flow {
    while (true) {
        emit(Unit)
        delay(duration.inWholeMilliseconds)
    }
}
