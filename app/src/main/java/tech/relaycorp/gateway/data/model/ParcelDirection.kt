package tech.relaycorp.gateway.data.model

enum class ParcelDirection(val value: String) {
    Incoming("incoming"), Outgoing("outgoing");

    companion object {
        fun fromValue(value: String) =
            values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid ParcelDirection value = $value")
    }
}
