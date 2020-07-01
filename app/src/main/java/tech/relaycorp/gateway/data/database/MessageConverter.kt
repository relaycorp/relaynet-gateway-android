package tech.relaycorp.gateway.data.database

import androidx.room.TypeConverter
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.PrivateMessageAddress
import tech.relaycorp.gateway.data.model.PublicMessageAddress

class MessageConverter {
    @TypeConverter
    fun toAddress(value: String) = MessageAddress.of(value)

    @TypeConverter
    fun fromAddress(address: MessageAddress) = address.value

    @TypeConverter
    fun toPrivateAddress(value: String) = PrivateMessageAddress(value)

    @TypeConverter
    fun fromPrivateAddress(address: PrivateMessageAddress) = address.value

    @TypeConverter
    fun toPublicAddress(value: String) = PublicMessageAddress(value)

    @TypeConverter
    fun fromPublicAddress(address: PublicMessageAddress) = address.value

    @TypeConverter
    fun toId(value: String) = MessageId(value)

    @TypeConverter
    fun fromId(id: MessageId) = id.value
}
