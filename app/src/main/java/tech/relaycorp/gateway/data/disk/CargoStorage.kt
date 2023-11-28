package tech.relaycorp.gateway.data.disk

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.RelaynetException
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.ramf.RAMFException
import java.io.InputStream
import javax.inject.Inject

class CargoStorage
@Inject constructor(
    private val diskMessageOperations: DiskMessageOperations,
    private val localConfig: LocalConfig,
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
            cargo.validate(localConfig.getAllValidCargoDeliveryAuth())
        } catch (exc: RelaynetException) {
            logger.warning("Invalid cargo received: ${exc.message}")
            throw Exception.InvalidCargo(null, exc)
        }

        return diskMessageOperations.writeMessage(FOLDER, PREFIX, cargoBytes)
    }

    suspend fun list() = diskMessageOperations.listMessages(FOLDER)

    suspend fun deleteAll() = diskMessageOperations.deleteAllMessages(FOLDER)

    sealed class Exception(message: String?, cause: Throwable? = null) :
        kotlin.Exception(message, cause) {
        class MalformedCargo(cause: Throwable) :
            Exception(null, cause)
        class InvalidCargo(message: String?, cause: Throwable? = null) :
            Exception(message, cause)
    }

    companion object {
        internal const val FOLDER = "cargo"
        internal const val PREFIX = "cargo_"
    }
}
