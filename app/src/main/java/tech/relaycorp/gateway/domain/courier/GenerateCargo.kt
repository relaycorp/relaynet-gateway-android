package tech.relaycorp.gateway.domain.courier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.disk.MessageDataNotFoundException
import tech.relaycorp.gateway.data.model.ParcelCollection
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.messages.ParcelCollectionAck
import tech.relaycorp.relaynet.messages.payloads.CargoMessageSetWithExpiry
import tech.relaycorp.relaynet.messages.payloads.CargoMessageWithExpiry
import tech.relaycorp.relaynet.messages.payloads.batch
import tech.relaycorp.relaynet.nodes.GatewayManager
import java.io.InputStream
import java.time.Duration
import java.util.logging.Level
import javax.inject.Inject

class GenerateCargo
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val parcelCollectionDao: ParcelCollectionDao,
    private val diskMessageOperations: DiskMessageOperations,
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val localConfig: LocalConfig,
    private val calculateCreationDate: CalculateCRCMessageCreationDate,
    private val gatewayManager: GatewayManager
) {

    suspend fun generate(): Flow<InputStream> =
        (getPCAsMessages() + getParcelMessages())
            .asSequence()
            .batch()
            .asFlow()
            .map { it.toCargo().serialize(localConfig.getKeyPair().private).inputStream() }

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
            expiryDate = expirationTimeUtc
        )

    private suspend fun StoredParcel.toCargoMessageWithExpiry(): CargoMessageWithExpiry? =
        readParcel()?.let { parcelSerialized ->
            CargoMessageWithExpiry(
                cargoMessageSerialized = parcelSerialized,
                expiryDate = expirationTimeUtc
            )
        }

    private suspend fun StoredParcel.readParcel(): ByteArray? =
        try {
            diskMessageOperations.readMessage(
                StoredParcel.STORAGE_FOLDER,
                storagePath
            )().readBytesAndClose()
        } catch (e: MessageDataNotFoundException) {
            logger.log(Level.WARNING, "Read parcel", e)
            null
        }

    private suspend fun CargoMessageSetWithExpiry.toCargo(): Cargo {
        if (nowInUtc() > latestMessageExpiryDate) {
            logger.warning(
                "The latest expiration date $latestMessageExpiryDate has expired already"
            )
        }

        val recipientAddress = publicGatewayPreferences.getCogRPCAddress()
        val creationDate = calculateCreationDate.calculate()

        logger.info("Generating cargo for $recipientAddress")
        val senderCertificate = localConfig.getCertificate()
        val cargoMessageSetCiphertext = gatewayManager.wrapMessagePayload(
            cargoMessageSet,
            publicGatewayPreferences.getCertificate().subjectPrivateAddress,
            senderCertificate.subjectPrivateAddress
        )
        return Cargo(
            recipientAddress = recipientAddress,
            payload = cargoMessageSetCiphertext,
            senderCertificate = senderCertificate,
            creationDate = creationDate,
            ttl = Duration.between(creationDate, latestMessageExpiryDate).seconds.toInt()
        )
    }
}
