package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.common.Operation
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.Parcel
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StorageSize
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.ramf.RAMFException
import tech.relaycorp.relaynet.ramf.RAMFMessage
import java.io.InputStream
import java.util.Date
import javax.inject.Inject

class StoreParcel
@Inject constructor(
    private val storedParcelRepository: StoredParcelDao,
    private val diskMessageOperations: DiskMessageOperations
) {

    suspend fun store(parcelStream: InputStream): Operation<StoredParcel> {
        val parcelBytes = parcelStream.readBytesAndClose()
        val parcel = try {
            Parcel.deserialize(parcelBytes)
        } catch (exc: RAMFException) {
            logger.warning("Malformed parcel received: ${exc.message}")
            return Operation.Error(MalformedParcelException())
        }

        try {
            parcel.validate()
        } catch (exc: RAMFException) {
            logger.warning("Invalid parcel received: ${exc.message}")
            return Operation.Error(InvalidParcelException())
        }

        val parcelPath = diskMessageOperations.writeMessage(
            StoredParcel.STORAGE_FOLDER,
            StoredParcel.STORAGE_PREFIX,
            parcelBytes
        )
        val parcelSize = StorageSize(parcelBytes.size.toLong())
        val storedParcel = parcel.toStoredParcel(parcelPath, parcelSize)
        storedParcelRepository.insert(storedParcel)
        return Operation.Success(storedParcel)
    }

    private fun RAMFMessage.toStoredParcel(storagePath: String, dataSize: StorageSize) =
        StoredParcel(
            recipientAddress = MessageAddress.of(recipientAddress),
            senderAddress = MessageAddress.of(senderCertificate.subjectPrivateAddress),
            messageId = MessageId(id),
            recipientLocation = RecipientLocation.LocalEndpoint,
            creationTimeUtc = Date.from(creationDate.toInstant()),
            expirationTimeUtc = Date.from(expiryDate.toInstant()),
            size = dataSize,
            storagePath = storagePath
        )

    class MalformedParcelException() : Exception()
    class InvalidParcelException() : Exception()
}
