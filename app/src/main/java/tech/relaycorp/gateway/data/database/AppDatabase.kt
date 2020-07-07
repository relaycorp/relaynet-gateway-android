package tech.relaycorp.gateway.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import tech.relaycorp.gateway.data.model.StoredParcel

@Database(
    entities = [StoredParcel::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parcelRepository(): StoredParcelDao
}
