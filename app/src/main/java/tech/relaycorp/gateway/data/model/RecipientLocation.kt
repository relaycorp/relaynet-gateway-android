package tech.relaycorp.gateway.data.model

enum class RecipientLocation(val value: String) {
    LocalEndpoint("local-endpoint"),
    ExternalGateway("external-gateway");

    companion object {
        fun fromValue(value: String) =
            values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid ${RecipientLocation::class.simpleName} value = $value")
    }
}
