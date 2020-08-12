package tech.relaycorp.gateway.test

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

    private const val TIMEOUT = 15_000L
    private const val INTERVAL = TIMEOUT / 20
}
