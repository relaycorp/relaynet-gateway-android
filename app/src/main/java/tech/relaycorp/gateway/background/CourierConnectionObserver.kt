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
class CourierConnectionObserver
@Inject constructor(
    connectivityManager: ConnectivityManager,
    private val wifiManager: WifiManager,
    private val pingRemoteServer: PingRemoteServer
) {

    private val state =
        MutableStateFlow<CourierConnectionState>(CourierConnectionState.Disconnected)

    private val networkRequest =
        NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

    private val hotspotSourceIpAddress
        get() =
            wifiManager.dhcpInfo?.serverAddress?.toIpAddressString()

    init {
        val networkCallback = NetworkCallback()

        networkCallback
            .isWifiConnected
            .flatMapLatest { isWifiConnected ->
                if (isWifiConnected) {
                    interval(SERVER_PING_INTERVAL)
                        .map { checkConnectedState() }
                } else {
                    flowOf(CourierConnectionState.Disconnected)
                }
            }
            .onEach { state.value = it }
            .launchIn(CoroutineScope(Dispatchers.IO))

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun observe(): Flow<CourierConnectionState> = state

    private suspend fun checkConnectedState(): CourierConnectionState {
        val serverAddress = hotspotSourceIpAddress
            ?: return CourierConnectionState.ConnectedWithUnknown
        return if (pingCourierServer(serverAddress)) {
            CourierConnectionState.ConnectedWithCourier(serverAddress.toFullServerAddress())
        } else {
            CourierConnectionState.ConnectedWithUnknown
        }
    }

    private suspend fun pingCourierServer(serverAddress: String) =
        pingRemoteServer.ping(serverAddress, CogRPC.PORT)

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
        val isWifiConnected = MutableStateFlow(false)

        override fun onAvailable(network: Network) {
            isWifiConnected.value = true
        }

        override fun onUnavailable() {
            isWifiConnected.value = false
        }

        override fun onLost(network: Network) {
            isWifiConnected.value = false
        }
    }

    companion object {
        private val SERVER_PING_INTERVAL = 5.seconds
    }
}
