package tech.relaycorp.gateway.domain.publicsync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.disk.MessageDataNotFoundException
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.gateway.data.doh.PublicAddressResolutionException
import tech.relaycorp.gateway.domain.DeleteParcel
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.pdc.PoWebClientProvider
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.bindings.pdc.ClientBindingException
import tech.relaycorp.relaynet.bindings.pdc.PDCException
import tech.relaycorp.relaynet.bindings.pdc.RejectedParcelException
import tech.relaycorp.relaynet.bindings.pdc.ServerConnectionException
import tech.relaycorp.relaynet.bindings.pdc.ServerException
import tech.relaycorp.relaynet.bindings.pdc.Signer
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import java.util.logging.Level
import javax.inject.Inject

@JvmSuppressWildcards
class DeliverParcelsToGateway
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val diskMessageOperations: DiskMessageOperations,
    private val poWebClientProvider: PoWebClientProvider,
    private val localConfig: LocalConfig,
    private val deleteParcel: DeleteParcel
) {

    suspend fun deliver(keepAlive: Boolean) {
        try {
            logger.info("Delivering parcels to Public Gateway (keepAlive=$keepAlive)")

            val poWebClient = try {
                poWebClientProvider.get()
            } catch (exc: PublicAddressResolutionException) {
                logger.log(
                    Level.WARNING,
                    "Failed to deliver parcels due to PoWeb address resolution error",
                    exc
                )
                return
            }
            val parcelsQuery =
                storedParcelDao.observeForRecipientLocation(RecipientLocation.ExternalGateway)
            val parcelsFlow =
                if (keepAlive) {
                    // one at a time
                    parcelsQuery.filter { it.any() }.map { it.first() }
                } else {
                    // just one batch, but one at a time
                    parcelsQuery.take(1).flatMapLatest { it.asFlow() }
                }

            poWebClient.use {
                parcelsFlow.collect { parcel ->
                    try {
                        deliverParcel(poWebClient, parcel)
                    } catch (e: ClientBindingException) {
                        logger.log(Level.SEVERE, "Could not deliver parcel due to client error", e)
                    } catch (e: ServerConnectionException) {
                        logger.log(Level.INFO, "Could not deliver parcel due to server error", e)
                    } catch (e: PDCException) {
                        logger.log(
                            Level.SEVERE,
                            "Could not deliver parcel due to unexpected error",
                            e
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            logger.log(Level.INFO, "Parcel delivery stopped", e)
        }
    }

    @Throws(ServerException::class)
    private suspend fun deliverParcel(poWebClient: PoWebClient, parcel: StoredParcel) {
        val parcelStream = parcel.getInputStream() ?: return

        try {
            logger.info("Delivering parcel to Gateway ${parcel.messageId.value}")
            poWebClient.deliverParcel(parcelStream.readBytesAndClose(), getSigner())
            logger.info("Delivered")
        } catch (e: RejectedParcelException) {
            logger.log(Level.WARNING, "Could not deliver rejected parcel (will be deleted)", e)
        }

        deleteParcel.delete(parcel)
    }

    private suspend fun StoredParcel.getInputStream() =
        try {
            diskMessageOperations.readMessage(StoredParcel.STORAGE_FOLDER, storagePath)()
        } catch (e: MessageDataNotFoundException) {
            logger.log(Level.WARNING, "Could not read parcel", e)
            deleteParcel.delete(this)
            null
        }

    private lateinit var _signer: Signer
    private suspend fun getSigner() =
        if (this::_signer.isInitialized) {
            _signer
        } else {
            val certificate = localConfig.getCertificate()
            logger.info("GW cert: " + certificate.subjectPrivateAddress)
            Signer(certificate, localConfig.getKeyPair().private).also {
                _signer = it
            }
        }
}
