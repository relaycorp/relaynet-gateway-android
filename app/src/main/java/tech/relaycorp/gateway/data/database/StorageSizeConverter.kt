package tech.relaycorp.gateway.data.database

import androidx.room.TypeConverter
import tech.relaycorp.gateway.data.model.StorageSize

class StorageSizeConverter {
    @TypeConverter
    fun toStorageSize(value: Long) = StorageSize(value)

    @TypeConverter
    fun fromStorageSize(size: StorageSize) = size.bytes
}
