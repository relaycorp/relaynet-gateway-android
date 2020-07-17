package tech.relaycorp.gateway.data.model

import androidx.room.Entity

@Entity(
    tableName = "Endpoint",
    primaryKeys = ["address", "applicationId"]
)
data class LocalEndpoint(
    val applicationId: String,
    val address: MessageAddress
)
