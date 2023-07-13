package tech.relaycorp.gateway.domain.endpoint

enum class NotificationType(val action: String) {
    IncomingParcel("tech.relaycorp.endpoint.INCOMING_PARCEL"),
    GatewayCertificateChange("tech.relaycorp.endpoint.GATEWAY_CERT_CHANGE")
}
