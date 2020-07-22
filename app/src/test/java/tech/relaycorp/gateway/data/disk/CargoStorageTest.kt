package tech.relaycorp.gateway.data.disk

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CargoStorageTest {

    private val diskOperations = mock<DiskMessageOperations>()
    private val cargoStorage = CargoStorage(diskOperations)

    @Test
    internal fun storeMalformed() {
        assertThrows<CargoStorage.Exception.MalformedCargo> {
            runBlocking {
                cargoStorage.store(ByteArray(0).inputStream())
            }
        }
    }
}
