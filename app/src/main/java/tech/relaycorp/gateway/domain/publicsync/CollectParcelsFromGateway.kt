package tech.relaycorp.gateway.domain.publicsync

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.retry
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.doh.PublicAddressResolutionException
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.domain.endpoint.NotifyEndpoints
import tech.relaycorp.gateway.pdc.PoWebClientProvider
import tech.relaycorp.relaynet.bindings.pdc.ClientBindingException
import tech.relaycorp.relaynet.bindings.pdc.NonceSignerException
import tech.relaycorp.relaynet.bindings.pdc.PDCException
import tech.relaycorp.relaynet.bindings.pdc.ParcelCollection
import tech.relaycorp.relaynet.bindings.pdc.ServerConnectionException
import tech.relaycorp.relaynet.bindings.pdc.ServerException
import tech.relaycorp.relaynet.bindings.pdc.Signer
import tech.relaycorp.relaynet.bindings.pdc.StreamingMode
import java.util.logging.Level
import javax.inject.Inject

class CollectParcelsFromGateway
@Inject constructor(
    private val storeParcel: StoreParcel,
    private val poWebClientProvider: PoWebClientProvider,
    private val localConfig: LocalConfig,
    private val notifyEndpoints: NotifyEndpoints
) {

    suspend fun collect(keepAlive: Boolean) {
        logger.info("Collecting parcels from Public Gateway (keepAlive=$keepAlive)")

        val poWebClient = try {
            poWebClientProvider.get()
        } catch (exc: PublicAddressResolutionException) {
            logger.log(
                Level.WARNING,
                "Failed to collect parcels due to PoWeb address resolution error",
                exc
            )
            return
        }
        val signer = Signer(localConfig.getCertificate(), localConfig.getKeyPair().private)
        val streamingMode =
            if (keepAlive) StreamingMode.KeepAlive else StreamingMode.CloseUponCompletion

        try {
            poWebClient.use {
                poWebClient
                    .collectParcels(arrayOf(signer), streamingMode)
                    .retry(Long.MAX_VALUE) { e ->
                        if (keepAlive && e is ServerConnectionException) {
                            // The culprit is likely to be:
                            // https://github.com/relaycorp/cloud-gateway/issues/53
                            logger.log(
                                Level.WARNING,
                                "Could not collect parcels due to server error, will retry.",
                                e
                            )
                            delay(RETRY_AFTER_SECONDS)
                            true
                        } else false
                    }
                    .collect { collectParcel(it, keepAlive) }
            }
        } catch (e: ServerException) {
            logger.log(Level.WARNING, "Could not collect parcels due to server error", e)
            return
        } catch (e: ClientBindingException) {
            logger.log(Level.SEVERE, "Could not collect parcels due to client error", e)
            return
        } catch (e: NonceSignerException) {
            logger.log(Level.SEVERE, "Could not collect parcels due to signing error", e)
            return
        } catch (e: PDCException) {
            logger.log(Level.SEVERE, "Could not collect parcel due to unexpected error", e)
            return
        } catch (e: CancellationException) {
            logger.log(Level.INFO, "Parcel collection stopped", e)
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

    companion object {
        @VisibleForTesting
        var RETRY_AFTER_SECONDS = 1.toLong()
    }
}
