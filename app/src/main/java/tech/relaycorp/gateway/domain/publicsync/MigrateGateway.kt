package tech.relaycorp.gateway.domain.publicsync

import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import javax.inject.Inject

class MigrateGateway
@Inject constructor(
    private val parcelCollectionDao: ParcelCollectionDao,
    private val registerGateway: RegisterGateway
) {

    suspend fun migrate(address: String) =
        when (registerGateway.registerNewAddress(address)) {
            RegisterGateway.Result.FailedToRegister -> Result.FailedToRegister
            RegisterGateway.Result.FailedToResolve -> Result.FailedToResolve
            RegisterGateway.Result.AlreadyRegistered -> Result.Successful
            is RegisterGateway.Result.Registered -> {
                deleteInvalidatedData()
                Result.Successful
            }
        }

    private suspend fun deleteInvalidatedData() {
        parcelCollectionDao.deleteAll()
    }

    enum class Result {
        Successful, FailedToResolve, FailedToRegister
    }
}
