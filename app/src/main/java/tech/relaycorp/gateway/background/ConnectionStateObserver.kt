package tech.relaycorp.gateway.background

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.gateway.common.interval
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.seconds

@Singleton
class ConnectionStateObserver
@Inject constructor(
    private val connectivityManager: ConnectivityManager,
    private val wifiManager: WifiManager,
    private val pingRemoteServer: PingRemoteServer,
    private val checkInternetAccess: CheckInternetAccess,
    private val checkPublicGatewayAccess: CheckPublicGatewayAccess
) {

    private val state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    private val networkRequest = NetworkRequest.Builder().build()

    private val hotspotSourceIpAddress
        get() = wifiManager.dhcpInfo?.serverAddress?.toIpAddressString()

    private val wifiNetworkName
        get() = wifiManager.connectionInfo.ssid

    init {
        val networkCallback = NetworkCallback()

        networkCallback
            .networkState
            .flatMapLatest { networkState ->
                networkState?.let { network ->
                    // If we have a network, check the state in a regular interval
                    interval(SERVER_PING_INTERVAL)
                        .map { checkNetworkState(network) }
                } ?: flowOf(ConnectionState.Disconnected)
            }
            .onEach { state.value = it }
            .launchIn(CoroutineScope(Dispatchers.IO))

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun observe(): Flow<ConnectionState> = state

    private suspend fun checkNetworkState(network: Network): ConnectionState {
        return if (checkInternetAccess.check()) {
            if (checkPublicGatewayAccess.check()) {
                ConnectionState.InternetAndPublicGateway
            } else {
                ConnectionState.InternetWithoutPublicGateway
            }
        } else if (network.isWifi) {
            val serverAddress = hotspotSourceIpAddress
                ?: return ConnectionState.WiFiWithUnknown(wifiNetworkName)
            if (pingCourierServer(serverAddress)) {
                ConnectionState.WiFiWithCourier(
                    wifiNetworkName,
                    serverAddress.toFullServerAddress()
                )
            } else {
                ConnectionState.WiFiWithUnknown(wifiNetworkName)
            }
        } else {
            ConnectionState.Disconnected
        }
    }

    private suspend fun pingCourierServer(serverAddress: String) =
        pingRemoteServer.pingAddress(serverAddress, CogRPC.PORT)

    private val Network.isWifi
        get() =
            connectivityManager.getNetworkCapabilities(this)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

    private fun Int.toIpAddressString() =
        String.format(
            "%d.%d.%d.%d",
            this and 0xff,
            this shr 8 and 0xff,
            this shr 16 and 0xff,
            this shr 24 and 0xff
        )

    private fun String.toFullServerAddress() = "https://$this:${CogRPC.PORT}"

    private class NetworkCallback : ConnectivityManager.NetworkCallback() {
        val networkState = MutableStateFlow<Network?>(null)

        override fun onAvailable(network: Network) {
            networkState.value = network
        }

        override fun onUnavailable() {
            networkState.value = null
        }

        override fun onLost(network: Network) {
            networkState.value = null
        }
    }

    companion object {
        private val SERVER_PING_INTERVAL = 5.seconds
    }
}
