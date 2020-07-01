package tech.relaycorp.gateway.background

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourierConnectionObserver
@Inject constructor(
    connectivityManager: ConnectivityManager,
    internal val wifiManager: WifiManager
) {

    internal val state =
        MutableStateFlow<CourierConnectionState>(CourierConnectionState.Disconnected)

    private val networkRequest =
        NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // TODO: check if there's an actual courier server listening at the right port
            val hotspotSourceIpAddress =
                wifiManager.dhcpInfo?.serverAddress?.toIpAddressString()

            state.value =
                hotspotSourceIpAddress
                    ?.let { CourierConnectionState.ConnectedWithCourier("https://$it:21473") }
                    ?: CourierConnectionState.ConnectedWithUnknown
        }

        override fun onUnavailable() {
            state.value = CourierConnectionState.Disconnected
        }

        override fun onLost(network: Network) {
            state.value = CourierConnectionState.Disconnected
        }
    }

    init {
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun observe(): Flow<CourierConnectionState> = state

    private fun Int.toIpAddressString() =
        String.format(
            "%d.%d.%d.%d",
            this and 0xff,
            this shr 8 and 0xff,
            this shr 16 and 0xff,
            this shr 24 and 0xff
        )
}
