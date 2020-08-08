package tech.relaycorp.gateway.data.model

enum class RecipientLocation(val value: String) {
    LocalEndpoint("local-endpoint"),
    ExternalGateway("external-gateway");

    companion object {
        fun fromValue(value: String): RecipientLocation {
            val recipientLocationType = RecipientLocation::class.simpleName
            return values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid $recipientLocationType value = $value")
        }
    }
}
