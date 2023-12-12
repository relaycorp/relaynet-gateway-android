package tech.relaycorp.gateway.data.model

sealed class MessageAddress {

    val type
        get() = when (this) {
            is PublicMessageAddress -> Type.Public
            is PrivateMessageAddress -> Type.Private
        }
    val value
        get() = when (this) {
            is PublicMessageAddress -> publicValue
            is PrivateMessageAddress -> privateValue
        }

    companion object {
        fun of(value: String) = if (value.contains(":")) {
            PublicMessageAddress(value)
        } else {
            PrivateMessageAddress(value)
        }
    }

    enum class Type(val value: String) {
        Public("public"),
        Private("private"),
        ;

        companion object {
            fun fromValue(value: String) = values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid MessageAddress.Type value = $value")
        }
    }
}

data class PublicMessageAddress(val publicValue: String) : MessageAddress()
data class PrivateMessageAddress(val privateValue: String) : MessageAddress()
