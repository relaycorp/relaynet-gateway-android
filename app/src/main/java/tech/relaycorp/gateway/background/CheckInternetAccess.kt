package tech.relaycorp.gateway.background

import javax.inject.Inject

class CheckInternetAccess
@Inject constructor(
    private val pingRemoteServer: PingRemoteServer
) {
    suspend fun check() =
        pingRemoteServer.pingHostname(INTERNET_TEST_WEBSITE)

    companion object {
        private const val INTERNET_TEST_WEBSITE = "https://google.com"
    }
}
