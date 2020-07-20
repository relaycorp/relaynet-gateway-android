package tech.relaycorp.gateway.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import tech.relaycorp.gateway.data.model.LocalEndpoint
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.gateway.data.model.ParcelCollection

@Database(
    entities = [
        LocalEndpoint::class,
        StoredParcel::class,
        ParcelCollection::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storedParcelDao(): StoredParcelDao
    abstract fun parcelCollectionDao(): ParcelCollectionDao
    abstract fun localEndpointDao(): LocalEndpointDao
}
