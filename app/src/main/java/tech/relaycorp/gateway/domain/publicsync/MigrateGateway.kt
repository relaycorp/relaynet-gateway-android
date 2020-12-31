package tech.relaycorp.gateway.domain.publicsync

import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import javax.inject.Inject

class MigrateGateway
@Inject constructor(
    private val localConfig: LocalConfig,
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val localEndpointDao: LocalEndpointDao,
    private val parcelCollectionDao: ParcelCollectionDao,
    private val storedParcelDao: StoredParcelDao,
    private val diskMessageOperations: DiskMessageOperations,
    private val registerGateway: RegisterGateway
) {

    suspend fun migrate(address: String): Result {
        deleteAllData()
        publicGatewayPreferences.setAddress(address)
        return when (registerGateway.registerIfNeeded()) {
            is RegisterGateway.Result.Registered,
            RegisterGateway.Result.AlreadyRegistered -> Result.Successful
            RegisterGateway.Result.FailedToRegister -> Result.FailedToRegister
            RegisterGateway.Result.FailedToResolve -> Result.FailedToResolve
        }
    }

    private suspend fun deleteAllData() {
        localConfig.clear()
        publicGatewayPreferences.clear()
        localEndpointDao.deleteAll()
        parcelCollectionDao.deleteAll()
        storedParcelDao.deleteAll()
        diskMessageOperations.deleteAllMessages(StoredParcel.STORAGE_FOLDER)
    }

    enum class Result {
        Successful, FailedToResolve, FailedToRegister
    }
}
