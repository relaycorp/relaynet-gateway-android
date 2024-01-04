package tech.relaycorp.gateway.domain.courier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.disk.MessageDataNotFoundException
import tech.relaycorp.gateway.data.model.ParcelCollection
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.messages.ParcelCollectionAck
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.messages.payloads.CargoMessageSetWithExpiry
import tech.relaycorp.relaynet.messages.payloads.CargoMessageWithExpiry
import tech.relaycorp.relaynet.messages.payloads.batch
import tech.relaycorp.relaynet.nodes.GatewayManager
import java.io.InputStream
import java.time.Duration
import java.util.logging.Level
import javax.inject.Inject
import javax.inject.Provider

class GenerateCargo
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val parcelCollectionDao: ParcelCollectionDao,
    private val diskMessageOperations: DiskMessageOperations,
    private val internetGatewayPreferences: InternetGatewayPreferences,
    private val localConfig: LocalConfig,
    private val calculateCreationDate: CalculateCRCMessageCreationDate,
    private val gatewayManager: Provider<GatewayManager>,
) {

    suspend fun generate(): Flow<InputStream> = (getPCAsMessages() + getParcelMessages())
        .asSequence()
        .batch()
        .asFlow()
        .mapNotNull { it.toCargoSerialized()?.inputStream() }

    private suspend fun getPCAsMessages() =
        parcelCollectionDao.getAll().map { it.toCargoMessageWithExpiry() }

    private suspend fun getParcelMessages() =
        storedParcelDao.listForRecipientLocation(RecipientLocation.ExternalGateway)
            .mapNotNull { it.toCargoMessageWithExpiry() }

    private fun ParcelCollection.toCargoMessageWithExpiry() = CargoMessageWithExpiry(
        cargoMessageSerialized = ParcelCollectionAck(
            senderEndpointId = senderAddress.value,
            recipientEndpointId = recipientAddress.value,
            parcelId = messageId.value,
        ).serialize(),
        expiryDate = expirationTimeUtc,
    )

    private suspend fun StoredParcel.toCargoMessageWithExpiry(): CargoMessageWithExpiry? =
        readParcel()?.let { parcelSerialized ->
            CargoMessageWithExpiry(
                cargoMessageSerialized = parcelSerialized,
                expiryDate = expirationTimeUtc,
            )
        }

    private suspend fun StoredParcel.readParcel(): ByteArray? = try {
        diskMessageOperations.readMessage(
            StoredParcel.STORAGE_FOLDER,
            storagePath,
        )().readBytesAndClose()
    } catch (e: MessageDataNotFoundException) {
        logger.log(Level.WARNING, "Read parcel", e)
        null
    }

    private suspend fun CargoMessageSetWithExpiry.toCargoSerialized(): ByteArray? {
        if (nowInUtc() > latestMessageExpiryDate) {
            logger.warning(
                "The latest expiration date $latestMessageExpiryDate has expired already",
            )
        }

        val identityKey = localConfig.getIdentityKey()
        val identityCert = localConfig.getIdentityCertificate()

        val recipientAddress = internetGatewayPreferences.getAddress()
        val recipientId = internetGatewayPreferences.getId() ?: run {
            logger.warning(
                "Failed to generate cargo because the internet gateway is not registered",
            )
            return null
        }
        val creationDate = calculateCreationDate.calculate()

        logger.info("Generating cargo for $recipientAddress")
        val cargoMessageSetCiphertext = gatewayManager.get().wrapMessagePayload(
            cargoMessageSet,
            recipientId,
            identityCert.subjectId,
        )
        val cargo = Cargo(
            recipient = Recipient(recipientId, recipientAddress),
            payload = cargoMessageSetCiphertext,
            senderCertificate = identityCert,
            creationDate = creationDate,
            ttl = Duration.between(creationDate, latestMessageExpiryDate).seconds.toInt(),
        )
        return cargo.serialize(identityKey)
    }
}
