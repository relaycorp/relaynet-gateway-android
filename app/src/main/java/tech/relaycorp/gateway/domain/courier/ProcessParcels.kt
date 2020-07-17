package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.endpoint.NotifyEndpoint
import javax.inject.Inject

class ProcessParcels
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val notifyEndpoint: NotifyEndpoint
) {

    suspend fun process() {
        val parcels = storedParcelDao.listForRecipientLocation(RecipientLocation.LocalEndpoint)
        val recipients = parcels.map { it.recipientAddress }.distinct()
        recipients.forEach { notifyEndpoint.notify(it) }
    }
}
