package tech.relaycorp.gateway.data.database

import androidx.room.TypeConverter
import tech.relaycorp.gateway.data.model.*
import java.util.Date

class Converters {
    @TypeConverter
    fun toDate(dateLong: Long) = Date(dateLong)

    @TypeConverter
    fun fromDate(date: Date) = date.time

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

    @TypeConverter
    fun toRecipientLocation(value: String) = RecipientLocation.fromValue(value)

    @TypeConverter
    fun fromRecipientLocation(location: RecipientLocation) = location.value

    @TypeConverter
    fun toStorageSize(value: Long) = StorageSize(value)

    @TypeConverter
    fun fromStorageSize(size: StorageSize) = size.bytes
}
