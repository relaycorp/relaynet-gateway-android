package tech.relaycorp.gateway.background

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object InternetWithoutPublicGateway : ConnectionState()
    object InternetAndPublicGateway : ConnectionState()

    data class WiFiWithUnknown(
        val networkName: String
    ) : ConnectionState()

    data class WiFiWithCourier(
        val networkName: String,
        val courierAddress: String
    ) : ConnectionState()
}
