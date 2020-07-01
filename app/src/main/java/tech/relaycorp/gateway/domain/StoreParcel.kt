package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.repos.ParcelRepository
import tech.relaycorp.gateway.data.disk.DiskException
import tech.relaycorp.gateway.data.disk.DiskRepository
import tech.relaycorp.gateway.data.model.*
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.ramf.RAMFException
import tech.relaycorp.relaynet.ramf.RAMFMessage
import java.io.InputStream
import java.util.*
import javax.inject.Inject

class StoreParcel
@Inject constructor(
    private val parcelRepository: ParcelRepository,
    private val diskRepository: DiskRepository
) {

    suspend fun storeParcel(parcelInputStream: InputStream): Result {
        val parcelBytes = parcelInputStream.readBytesAndClose()
        val parcel = try {
            Parcel.deserialize(parcelBytes)
        } catch (exc: RAMFException) {
            logger.warning("Malformed Cargo received: ${exc.message}")
            return Result.Error.Malformed
        }

        try {
            parcel.validate()
        } catch (exc: RAMFException) {
            logger.warning("Invalid cargo received: ${exc.message}")
            return Result.Error.Invalid
        }

        return storeMessage(parcel, parcelBytes)
    }

    private suspend fun storeMessage(
        message: RAMFMessage,
        data: ByteArray
    ): Result {
        val dataSize = StorageSize(data.size.toLong())

        val storagePath = try {
            diskRepository.writeParcel(data)
        } catch (exception: DiskException) {
            return Result.Error.CouldNotStore
        }
        val storedParcel = message.toStoredParcel(storagePath, dataSize)
        parcelRepository.insert(storedParcel)
        return Result.Success(storedParcel)
    }

    private fun RAMFMessage.toStoredParcel(
        storagePath: String,
        dataSize: StorageSize
    ): StoredParcel {
        val recipientAddress = PublicMessageAddress(recipientAddress)
        return StoredParcel(
            recipientAddress = recipientAddress,
            senderAddress = PrivateMessageAddress(senderCertificate.subjectPrivateAddress),
            messageId = MessageId(id),
            creationTimeUtc = Date.from(creationDate.toInstant()),
            expirationTimeUtc = Date.from(expiryDate.toInstant()),
            size = dataSize,
            storagePath = storagePath
        )
    }

    sealed class Result {
        data class Success(val parcel: StoredParcel) : Result()
        sealed class Error : Result() {
            object CouldNotStore : Error()
            object Malformed : Error()
            object Invalid : Error()
        }
    }
}
