package tech.relaycorp.gateway.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Endpoint",
    indices = [Index(value = ["applicationId"], unique = false)],
)
data class LocalEndpoint(
    @PrimaryKey
    val address: MessageAddress,
    val applicationId: String,
)
