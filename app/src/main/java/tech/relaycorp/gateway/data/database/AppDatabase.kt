package tech.relaycorp.gateway.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import tech.relaycorp.gateway.data.model.ExampleModel

@Database(
    entities = [ExampleModel::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase()
