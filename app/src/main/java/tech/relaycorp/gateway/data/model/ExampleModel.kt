package tech.relaycorp.gateway.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "Message"
)
data class ExampleModel(
    @PrimaryKey val id: Long
)
