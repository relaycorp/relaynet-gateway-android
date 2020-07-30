package tech.relaycorp.gateway.background

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object InternetWithoutRelaynet : ConnectionState()
    object InternetAndRelaynet : ConnectionState()
    data class WiFiWithUnknown(val networkName: String) : ConnectionState()
    data class WiFiWithCourier(val networkName: String, val courierAddress: String) : ConnectionState()
}
