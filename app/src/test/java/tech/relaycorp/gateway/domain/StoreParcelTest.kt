package tech.relaycorp.gateway.domain

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.RecipientLocation

internal class StoreParcelTest {

    private val storedParcelRepository = mock<StoredParcelDao>()
    private val diskOperations = mock<DiskMessageOperations>()
    private val storeParcel = StoreParcel(storedParcelRepository, diskOperations)

    @Test
    internal fun storeMalformed() {
        assertThrows<StoreParcel.MalformedParcelException> {
            runBlocking {
                storeParcel.store(ByteArray(0).inputStream(), RecipientLocation.LocalEndpoint)
            }
        }
    }
}
