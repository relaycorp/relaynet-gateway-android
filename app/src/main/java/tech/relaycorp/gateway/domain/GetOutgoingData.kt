package tech.relaycorp.gateway.domain

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StorageSize
import javax.inject.Inject

class GetOutgoingData
@Inject constructor(
    private val storedParcelDao: StoredParcelDao
) {

    fun get() =
        combine(
            storedParcelDao.countSizeForRecipientLocation(RecipientLocation.ExternalGateway),
            storedParcelDao.countSizeForRecipientLocationAndInTransit(
                RecipientLocation.ExternalGateway,
                true
            )
        ) { total, inTransit -> Data(total, inTransit) }

    fun any() =
        storedParcelDao.countSizeForRecipientLocation(RecipientLocation.ExternalGateway)
            .map { !it.isZero }

    data class Data(
        val total: StorageSize = StorageSize.ZERO,
        val inTransit: StorageSize = StorageSize.ZERO
    ) {
        val percentage
            get() = if (total.isZero) 0 else (inTransit.bytes / total.bytes).toInt()
        val isZero get() = total.isZero
    }
}
