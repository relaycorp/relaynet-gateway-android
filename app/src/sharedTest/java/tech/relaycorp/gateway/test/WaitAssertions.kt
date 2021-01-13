package tech.relaycorp.gateway.test

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

object WaitAssertions {

    fun waitFor(check: () -> Unit) {
        val initialTime = System.currentTimeMillis()
        var lastError: Throwable?
        do {
            try {
                check.invoke()
                return
            } catch (throwable: Throwable) {
                lastError = throwable
            }
            Thread.sleep(INTERVAL)
        } while (System.currentTimeMillis() - initialTime < TIMEOUT)
        throw AssertionError("Timeout waiting", lastError)
    }

    suspend fun suspendWaitFor(check: suspend () -> Unit) =
        waitFor { runBlocking { check.invoke() } }

    suspend fun waitForAssertEquals(expected: Any, actualCheck: suspend () -> Any) {
        val initialTime = System.currentTimeMillis()
        var value = actualCheck.invoke()

        while (expected != value) {
            delay(INTERVAL)
            if (System.currentTimeMillis() - initialTime > TIMEOUT) {
                throw AssertionError("Timeout waiting for $value to become $expected")
            }
            value = actualCheck.invoke()
        }
    }

    private const val TIMEOUT = 10_000L
    private const val INTERVAL = TIMEOUT / 20
}
