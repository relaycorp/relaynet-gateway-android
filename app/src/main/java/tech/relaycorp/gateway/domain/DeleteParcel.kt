package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.StoredParcel
import javax.inject.Inject

class DeleteParcel
@Inject constructor(
    private val storedParcelDao: StoredParcelDao,
    private val diskMessageOperations: DiskMessageOperations
) {
    suspend fun delete(storedParcel: StoredParcel) {
        storedParcelDao.delete(storedParcel)
        diskMessageOperations.deleteMessage(StoredParcel.STORAGE_FOLDER, storedParcel.storagePath)
    }
}
