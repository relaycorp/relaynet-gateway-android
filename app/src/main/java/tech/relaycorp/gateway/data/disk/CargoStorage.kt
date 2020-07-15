package tech.relaycorp.gateway.data.disk

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.common.Operation
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.ramf.RAMFException
import java.io.InputStream
import javax.inject.Inject

class CargoStorage
@Inject constructor(
    private val diskMessageOperations: DiskMessageOperations
) {

    suspend fun store(cargoStream: InputStream): Operation<String> {
        val cargoBytes = cargoStream.readBytesAndClose()
        val cargo = try {
            Cargo.deserialize(cargoBytes)
        } catch (exc: RAMFException) {
            logger.warning("Malformed cargo received: ${exc.message}")
            return Operation.Error(MalformedCargoException())
        }

        try {
            cargo.validate()
        } catch (exc: RAMFException) {
            logger.warning("Invalid cargo received: ${exc.message}")
            return Operation.Error(InvalidCargoException())
        }

        return Operation.Success(
            diskMessageOperations.writeMessage(
                FOLDER,
                PREFIX, cargoBytes
            )
        )
    }

    suspend fun list() = diskMessageOperations.listMessages(FOLDER)

    suspend fun deleteAll() = diskMessageOperations.deleteAllMessages(FOLDER)

    class MalformedCargoException() : Exception()
    class InvalidCargoException() : Exception()

    companion object {
        private const val FOLDER = "cargo"
        private const val PREFIX = "cargo_"
    }
}
