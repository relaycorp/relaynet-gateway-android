package tech.relaycorp.gateway.domain.publicsync

import kotlinx.coroutines.flow.collect
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.domain.endpoint.NotifyEndpoints
import tech.relaycorp.gateway.pdc.PoWebClientBuilder
import tech.relaycorp.relaynet.bindings.pdc.ClientBindingException
import tech.relaycorp.relaynet.bindings.pdc.NonceSignerException
import tech.relaycorp.relaynet.bindings.pdc.ParcelCollection
import tech.relaycorp.relaynet.bindings.pdc.ServerException
import tech.relaycorp.relaynet.bindings.pdc.Signer
import tech.relaycorp.relaynet.bindings.pdc.StreamingMode
import java.util.logging.Level
import javax.inject.Inject

class CollectParcelsFromGateway
@Inject constructor(
    private val storeParcel: StoreParcel,
    private val poWebClientBuilder: PoWebClientBuilder,
    private val localConfig: LocalConfig,
    private val notifyEndpoints: NotifyEndpoints
) {

    suspend fun collect(keepAlive: Boolean) {
        logger.info("Collecting parcels from Public Gateway (keepAlive=$keepAlive)")

        val poWebClient = poWebClientBuilder.build()
        val signer = Signer(localConfig.getCertificate(), localConfig.getKeyPair().private)
        val streamingMode =
            if (keepAlive) StreamingMode.KeepAlive else StreamingMode.CloseUponCompletion

        try {
            poWebClient.use {
                poWebClient
                    .collectParcels(arrayOf(signer), streamingMode)
                    .collect { collectParcel(it, keepAlive) }
            }
        } catch (e: ServerException) {
            logger.log(Level.INFO, "Could not collect parcels due to server error", e)
            return
        } catch (e: ClientBindingException) {
            logger.log(Level.SEVERE, "Could not collect parcels due to client error", e)
            return
        } catch (e: NonceSignerException) {
            logger.log(Level.SEVERE, "Could not collect parcels due to signing error", e)
            return
        }

        if (!keepAlive) {
            notifyEndpoints.notifyAllPending()
        }
    }

    private suspend fun collectParcel(parcelCollection: ParcelCollection, keepAlive: Boolean) {
        val storeResult =
            storeParcel.store(parcelCollection.parcelSerialized, RecipientLocation.LocalEndpoint)
        when (storeResult) {
            is StoreParcel.Result.MalformedParcel ->
                logger.info("Malformed parcel received")
            is StoreParcel.Result.InvalidParcel ->
                logger.info("Invalid parcel received")
            is StoreParcel.Result.CollectedParcel ->
                logger.info("Parcel already received")
            is StoreParcel.Result.Success -> {
                logger.info("Collected parcel from Gateway ${storeResult.parcel.id}")
                if (keepAlive) {
                    notifyEndpoints.notify(MessageAddress.of(storeResult.parcel.recipientAddress))
                }
            }
        }

        parcelCollection.ack()
    }
}
