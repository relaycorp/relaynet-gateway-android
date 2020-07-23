package tech.relaycorp.gateway.data.disk

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.ramf.RAMFException
import java.io.InputStream
import javax.inject.Inject

class CargoStorage
@Inject constructor(
    private val diskMessageOperations: DiskMessageOperations
) {

    @Throws(Exception::class)
    suspend fun store(cargoStream: InputStream): String {
        val cargoBytes = cargoStream.readBytesAndClose()
        val cargo = try {
            Cargo.deserialize(cargoBytes)
        } catch (exc: RAMFException) {
            logger.warning("Malformed cargo received: ${exc.message}")
            throw Exception.MalformedCargo(exc)
        }

        try {
            cargo.validate()
        } catch (exc: RAMFException) {
            logger.warning("Invalid cargo received: ${exc.message}")
            throw Exception.InvalidCargo(exc)
        }

        return diskMessageOperations.writeMessage(FOLDER, PREFIX, cargoBytes)
    }

    suspend fun list() = diskMessageOperations.listMessages(FOLDER)

    suspend fun deleteAll() = diskMessageOperations.deleteAllMessages(FOLDER)

    sealed class Exception(cause: Throwable? = null) : kotlin.Exception(cause) {
        class MalformedCargo(cause: Throwable) : Exception(cause)
        class InvalidCargo(cause: Throwable) : Exception(cause)
    }

    companion object {
        private const val FOLDER = "cargo"
        private const val PREFIX = "cargo_"
    }
}
