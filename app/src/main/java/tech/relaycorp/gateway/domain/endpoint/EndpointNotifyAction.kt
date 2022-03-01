package tech.relaycorp.gateway.domain.endpoint

enum class EndpointNotifyAction(val action: String) {
    ParcelToReceive("tech.relaycorp.endpoint.NEW_PARCEL"),
    CertificateRenew("tech.relaycorp.endpoint.CERT_RENEW")
}
