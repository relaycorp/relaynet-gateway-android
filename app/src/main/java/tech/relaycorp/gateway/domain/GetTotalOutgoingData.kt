package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.model.RecipientLocation
import javax.inject.Inject

class GetTotalOutgoingData
@Inject constructor(
    private val storedParcelDao: StoredParcelDao
) {

    fun get() =
        storedParcelDao.countSizeForRecipientLocation(RecipientLocation.ExternalGateway)
}
