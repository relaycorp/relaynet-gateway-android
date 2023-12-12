package tech.relaycorp.gateway.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.time.ZonedDateTime

@Entity(
    tableName = "ParcelCollection",
    primaryKeys = ["recipientAddress", "senderAddress", "messageId"],
)
data class ParcelCollection(
    val recipientAddress: MessageAddress,
    val senderAddress: PrivateMessageAddress,
    val messageId: MessageId,
    @ColumnInfo(index = true)
    val creationTimeUtc: ZonedDateTime,
    @ColumnInfo(index = true)
    val expirationTimeUtc: ZonedDateTime,
)
