package tech.relaycorp.gateway.domain

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.StoredParcel
import tech.relaycorp.gateway.test.factory.StoredParcelFactory

internal class DeleteParcelTest {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val diskOperations = mock<DiskMessageOperations>()
    private val deleteParcel = DeleteParcel(storedParcelDao, diskOperations)

    @Test
    internal fun delete() = runBlockingTest {
        val storedParcel = StoredParcelFactory.build()
        deleteParcel.delete(storedParcel)
        verify(storedParcelDao).delete(eq(storedParcel))
        verify(diskOperations).deleteMessage(
            eq(StoredParcel.STORAGE_FOLDER),
            eq(storedParcel.storagePath),
        )
    }
}
