package tech.relaycorp.gateway.background

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object InternetWithoutGateway : ConnectionState()
    object InternetWithGateway : ConnectionState()
    object WiFiWithUnknown : ConnectionState()
    data class WiFiWithCourier(val courierAddress: String) : ConnectionState()
}
