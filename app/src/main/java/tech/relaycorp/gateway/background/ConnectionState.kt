package tech.relaycorp.gateway.background

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object InternetWithoutInternetGateway : ConnectionState()
    object InternetAndGateway : ConnectionState()
    object WiFiWithUnknown : ConnectionState()
    data class WiFiWithCourier(val courierAddress: String) : ConnectionState()
}
