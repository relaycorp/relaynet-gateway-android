package tech.relaycorp.gateway.domain

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.common.Operation
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.StoredParcel

internal class StoreParcelTest {

    private val storedParcelRepository = mock<StoredParcelDao>()
    private val diskOperations = mock<DiskMessageOperations>()
    private val storeParcel = StoreParcel(storedParcelRepository, diskOperations)

    @Test
    internal fun storeMalformed() = runBlockingTest {
        val result = storeParcel.store(ByteArray(0).inputStream())
        assertTrue(
            (result as Operation.Error<StoredParcel>)
                .throwable is StoreParcel.MalformedParcelException
        )
    }
}
