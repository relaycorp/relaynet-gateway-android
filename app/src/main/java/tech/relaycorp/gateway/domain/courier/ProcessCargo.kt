package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.disk.CargoStorage
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.DeleteParcel
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.domain.StoreParcelCollection
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.messages.ParcelCollectionAck
import tech.relaycorp.relaynet.messages.payloads.CargoMessage
import java.util.logging.Level
import javax.inject.Inject

class ProcessCargo
@Inject constructor(
    private val cargoStorage: CargoStorage,
    private val readMessagesFromCargo: ReadMessagesFromCargo,
    private val storeParcel: StoreParcel,
    private val storeParcelCollection: StoreParcelCollection,
    private val deleteParcel: DeleteParcel
) {

    suspend fun process() {
        val cargoes = cargoStorage.list()
        cargoes.forEach { cargoStream ->
            val cargo = Cargo.deserialize(cargoStream().readBytesAndClose())
            readMessagesFromCargo.read(cargo)
                .forEach { message -> handleMessage(message) }
        }
        cargoStorage.deleteAll()
    }

    private suspend fun handleMessage(message: CargoMessage) {
        when (message.type) {
            CargoMessage.Type.PARCEL ->
                storeParcelAndParcelCollection(message.messageSerialized)
            CargoMessage.Type.PCA ->
                deserializeAckAndDeleteParcel(message.messageSerialized)
            else ->
                logger.log(Level.WARNING, "Unsupported message received")
        }
    }

    private suspend fun storeParcelAndParcelCollection(parcelData: ByteArray) {
        try {
            val parcel = storeParcel.store(parcelData, RecipientLocation.LocalEndpoint)
            storeParcelCollection.storeForParcel(parcel)
        } catch (e: StoreParcel.MalformedParcelException) {
            logger.log(Level.WARNING, "Malformed parcel", e)
        } catch (e: StoreParcel.InvalidParcelException) {
            logger.log(Level.WARNING, "Invalid parcel", e)
        }
    }

    private suspend fun deserializeAckAndDeleteParcel(parcelAckData: ByteArray) {
        val pca = ParcelCollectionAck.deserialize(parcelAckData)
        deleteParcel.delete(
            recipientAddress = MessageAddress.of(pca.recipientEndpointAddress),
            senderAddress = MessageAddress.of(pca.senderEndpointPrivateAddress),
            messageId = MessageId(pca.parcelId)
        )
    }
}
