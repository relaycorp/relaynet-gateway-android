package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.common.toZonedDateTime
import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.disk.MessageDataNotFoundException
import tech.relaycorp.gateway.data.model.ParcelCollection
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.messages.ParcelCollectionAck
import tech.relaycorp.relaynet.messages.payloads.CargoMessageWithExpiry
import tech.relaycorp.relaynet.messages.payloads.batch
import java.io.InputStream
import java.util.logging.Level
import javax.inject.Inject

class GenerateCargo
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val parcelCollectionDao: ParcelCollectionDao,
    private val diskMessageOperations: DiskMessageOperations
) {

    suspend fun generate(): Iterable<() -> InputStream> =
        (getPCAsMessages() + getParcelMessages())
            .asSequence()
            .batch()
            .map { { it.cargoMessageSet.serializePlaintext().inputStream() } }
            .asIterable()

    private suspend fun getPCAsMessages() =
        parcelCollectionDao.getAll().map { it.toCargoMessageWithExpiry() }

    private suspend fun getParcelMessages() =
        storedParcelDao.listForRecipientLocation(RecipientLocation.ExternalGateway)
            .mapNotNull { it.toCargoMessageWithExpiry() }

    private fun ParcelCollection.toCargoMessageWithExpiry() =
        CargoMessageWithExpiry(
            cargoMessageSerialized = ParcelCollectionAck(
                senderEndpointPrivateAddress = senderAddress.value,
                recipientEndpointAddress = recipientAddress.value,
                parcelId = messageId.value
            ).serialize(),
            expiryDate = expirationTimeUtc.toZonedDateTime()
        )

    private suspend fun StoredParcel.toCargoMessageWithExpiry() =
        readParcel()?.let { parcelSerialized ->
            CargoMessageWithExpiry(
                cargoMessageSerialized = parcelSerialized,
                expiryDate = expirationTimeUtc.toZonedDateTime()
            )
        }

    private suspend fun StoredParcel.readParcel() =
        try {
            diskMessageOperations.readMessage(
                StoredParcel.STORAGE_FOLDER,
                storagePath
            )().readBytesAndClose()
        } catch (e: MessageDataNotFoundException) {
            logger.log(Level.WARNING, "Read parcel", e)
            null
        }
}
