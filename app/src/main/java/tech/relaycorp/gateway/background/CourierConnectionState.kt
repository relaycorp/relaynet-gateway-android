package tech.relaycorp.gateway.background

sealed class CourierConnectionState {
    data class ConnectedWithCourier(val address: String) : CourierConnectionState()
    object ConnectedWithUnknown : CourierConnectionState()
    object Disconnected : CourierConnectionState()
}
