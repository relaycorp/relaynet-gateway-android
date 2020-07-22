package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.common.Logging.logger
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
import java.util.Date
import javax.inject.Inject

class StoreParcel
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val diskMessageOperations: DiskMessageOperations
) {

    @Throws(Exception.MalformedParcel::class, Exception.InvalidParcel::class)
    suspend fun store(
        parcelStream: InputStream,
        recipientLocation: RecipientLocation
    ) = store(parcelStream.readBytesAndClose(), recipientLocation)

    @Throws(Exception.MalformedParcel::class, Exception.InvalidParcel::class)
    suspend fun store(
        parcelData: ByteArray,
        recipientLocation: RecipientLocation
    ): StoredParcel {
        val parcel = try {
            Parcel.deserialize(parcelData)
        } catch (exc: RAMFException) {
            logger.warning("Malformed parcel received: ${exc.message}")
            throw Exception.MalformedParcel()
        }

        if (recipientLocation == RecipientLocation.LocalEndpoint && !parcel.isRecipientAddressPrivate) {
            throw Exception.InvalidPublicLocalRecipient()
        }

        try {
            when (recipientLocation) {
                RecipientLocation.LocalEndpoint -> parcel.validate() // TODO: validate with private local endpoint
                RecipientLocation.ExternalGateway -> parcel.validate()
            }
        } catch (exc: RAMFException) {
            logger.warning("Invalid parcel received: ${exc.message}")
            throw Exception.InvalidParcel()
        }

        val parcelPath = diskMessageOperations.writeMessage(
            StoredParcel.STORAGE_FOLDER,
            StoredParcel.STORAGE_PREFIX,
            parcelData
        )
        val parcelSize = StorageSize(parcelData.size.toLong())
        val storedParcel = parcel.toStoredParcel(parcelPath, parcelSize, recipientLocation)
        storedParcelDao.insert(storedParcel)
        return storedParcel
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
            creationTimeUtc = Date.from(creationDate.toInstant()),
            expirationTimeUtc = Date.from(expiryDate.toInstant()),
            size = dataSize,
            storagePath = storagePath
        )

    sealed class Exception : kotlin.Exception() {
        class MalformedParcel() : Exception()
        class InvalidParcel() : Exception()
        class InvalidPublicLocalRecipient() : Exception()
    }
}
