package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StorageSize
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.ramf.RAMFException
import java.io.InputStream
import javax.inject.Inject

class StoreParcel
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val diskMessageOperations: DiskMessageOperations
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

        if (recipientLocation == RecipientLocation.LocalEndpoint && !parcel.isRecipientAddressPrivate) {
            return Result.InvalidPublicLocalRecipient(parcel)
        }

        try {
            when (recipientLocation) {
                RecipientLocation.LocalEndpoint -> parcel.validate() // TODO: validate with private local endpoint
                RecipientLocation.ExternalGateway -> parcel.validate()
            }
        } catch (exc: RAMFException) {
            return Result.InvalidParcel(parcel, exc)
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

    private fun Parcel.toStoredParcel(
        storagePath: String,
        dataSize: StorageSize,
        recipientLocation: RecipientLocation
    ) =
        StoredParcel(
            recipientAddress = MessageAddress.of(recipientAddress),
            senderAddress = MessageAddress.of(senderCertificate.subjectPrivateAddress),
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
        data class InvalidPublicLocalRecipient(val parcel: Parcel) : Result()
        data class Success(val parcel: Parcel) : Result()
    }
}
