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
import tech.relaycorp.relaynet.nodes.GatewayManager
import java.util.logging.Level
import javax.inject.Inject
import javax.inject.Provider

class ProcessCargo
@Inject constructor(
    private val cargoStorage: CargoStorage,
    private val storeParcel: StoreParcel,
    private val storeParcelCollection: StoreParcelCollection,
    private val deleteParcel: DeleteParcel,
    private val gatewayManager: Provider<GatewayManager>,
    private val rotateCertificate: RotateCertificate
) {

    suspend fun process() {
        val cargoes = cargoStorage.list()
        cargoes.iterator().forEach { cargoStream ->
            val cargo = Cargo.deserialize(cargoStream().readBytesAndClose())
            val messageSet = gatewayManager.get().unwrapMessagePayload(cargo)
            messageSet.classifyMessages().forEach { message -> handleMessage(message) }
        }
        cargoStorage.deleteAll()
    }

    private suspend fun handleMessage(message: CargoMessage) {
        when (message.type) {
            CargoMessage.Type.PARCEL ->
                storeParcelAndParcelCollection(message.messageSerialized)
            CargoMessage.Type.PCA ->
                deserializeAckAndDeleteParcel(message.messageSerialized)
            CargoMessage.Type.CERTIFICATE_ROTATION ->
                rotateCertificate(message.messageSerialized)
            else ->
                logger.log(Level.WARNING, "Unsupported message received")
        }
    }

    private suspend fun storeParcelAndParcelCollection(parcelData: ByteArray) {
        val parcel = when (
            val result = storeParcel.store(parcelData, RecipientLocation.LocalEndpoint)
        ) {
            is StoreParcel.Result.MalformedParcel,
            is StoreParcel.Result.CollectedParcel -> return
            is StoreParcel.Result.InvalidParcel -> {
                logger.log(Level.WARNING, "Invalid parcel received", result.cause)
                result.parcel
            }
            is StoreParcel.Result.Success -> result.parcel
        }
        storeParcelCollection.storeForParcel(parcel)
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
