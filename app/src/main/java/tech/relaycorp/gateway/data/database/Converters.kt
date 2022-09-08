package tech.relaycorp.gateway.data.database

import androidx.room.TypeConverter
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.PrivateMessageAddress
import tech.relaycorp.gateway.data.model.PublicMessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StorageSize
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class Converters {
    @TypeConverter
    fun toZonedDateTime(value: Long): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.of("UTC"))

    @TypeConverter
    fun fromZonedDateTime(value: ZonedDateTime) = value.toInstant().toEpochMilli()

    @TypeConverter
    fun toAddress(value: String) = MessageAddress.of(value)

    @TypeConverter
    fun fromAddress(address: MessageAddress) = address.value

    @TypeConverter
    fun toNodeId(value: String) = PrivateMessageAddress(value)

    @TypeConverter
    fun fromNodeId(address: PrivateMessageAddress) = address.value

    @TypeConverter
    fun toInternetAddress(value: String) = PublicMessageAddress(value)

    @TypeConverter
    fun fromInternetAddress(address: PublicMessageAddress) = address.value

    @TypeConverter
    fun toId(value: String) = MessageId(value)

    @TypeConverter
    fun fromId(id: MessageId) = id.value

    @TypeConverter
    fun toRecipientLocation(value: String) = RecipientLocation.fromValue(value)

    @TypeConverter
    fun fromRecipientLocation(location: RecipientLocation) = location.value

    @TypeConverter
    fun toStorageSize(value: Long) = StorageSize(value)

    @TypeConverter
    fun fromStorageSize(size: StorageSize) = size.bytes
}
