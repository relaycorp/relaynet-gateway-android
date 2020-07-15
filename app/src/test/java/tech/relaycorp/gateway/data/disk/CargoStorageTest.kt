package tech.relaycorp.gateway.data.disk

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.common.Operation

internal class CargoStorageTest {

    private val diskOperations = mock<DiskMessageOperations>()
    private val cargoStorage = CargoStorage(diskOperations)

    @Test
    internal fun storeMalformed() = runBlockingTest {
        val result = cargoStorage.store(ByteArray(0).inputStream())
        assertTrue(
            (result as Operation.Error<String>)
                .throwable is CargoStorage.MalformedCargoException
        )
    }
}
