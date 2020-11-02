package tech.relaycorp.gateway.background

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object InternetWithoutPublicGateway : ConnectionState()
    object InternetAndPublicGateway : ConnectionState()
    object WiFiWithUnknown : ConnectionState()
    data class WiFiWithCourier(val courierAddress: String) : ConnectionState()
}
