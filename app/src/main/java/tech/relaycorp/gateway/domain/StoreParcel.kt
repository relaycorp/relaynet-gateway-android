package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.PrivateMessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StorageSize
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.relaynet.RelaynetException
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.ramf.RAMFException
import java.io.InputStream
import java.util.logging.Level
import javax.inject.Inject

class StoreParcel
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val parcelCollectionDao: ParcelCollectionDao,
    private val diskMessageOperations: DiskMessageOperations,
    private val localConfig: LocalConfig
) {

    suspend fun store(
        parcelStream: InputStream,
        recipientLocation: RecipientLocation
    ) = store(parcelStream.readBytesAndClose(), recipientLocation)

    suspend fun store(
        parcelData: ByteArray,
        recipientLocation: RecipientLocation
    ): Result {
        val parcel = try {
            Parcel.deserialize(parcelData)
        } catch (exc: RAMFException) {
            return Result.MalformedParcel(exc)
        }

        val requiredCertificateAuthorities =
            if (recipientLocation == RecipientLocation.ExternalGateway) {
                null
            } else {
                localConfig.getAllValidIdentityCertificates()
            }
        try {
            parcel.validate(requiredCertificateAuthorities)
        } catch (exc: RelaynetException) {
            return Result.InvalidParcel(parcel, exc)
        }

        if (isParcelAlreadyCollected(parcel)) {
            return Result.CollectedParcel(parcel)
        }

        val parcelPath = diskMessageOperations.writeMessage(
            StoredParcel.STORAGE_FOLDER,
            StoredParcel.STORAGE_PREFIX,
            parcelData
        )

        val parcelSize = StorageSize(parcelData.size.toLong())
        val storedParcel = parcel.toStoredParcel(parcelPath, parcelSize, recipientLocation)
        storedParcelDao.insert(storedParcel)
        return Result.Success(parcel)
    }

    private suspend fun isParcelAlreadyCollected(parcel: Parcel) =
        parcelCollectionDao.exists(
            MessageAddress.of(parcel.recipient.id),
            PrivateMessageAddress(parcel.senderCertificate.subjectId),
            MessageId(parcel.id)
        )

    private fun Parcel.toStoredParcel(
        storagePath: String,
        dataSize: StorageSize,
        recipientLocation: RecipientLocation
    ) =
        StoredParcel(
            recipientAddress = MessageAddress.of(recipient.id),
            senderAddress = MessageAddress.of(senderCertificate.subjectId),
            messageId = MessageId(id),
            recipientLocation = recipientLocation,
            creationTimeUtc = creationDate,
            expirationTimeUtc = expiryDate,
            size = dataSize,
            storagePath = storagePath
        )

    sealed class Result {
        data class MalformedParcel(val cause: Throwable) : Result()
        data class InvalidParcel(val parcel: Parcel, val cause: Throwable) : Result()
        data class CollectedParcel(val parcel: Parcel) : Result()
        data class Success(val parcel: Parcel) : Result()
    }
}
