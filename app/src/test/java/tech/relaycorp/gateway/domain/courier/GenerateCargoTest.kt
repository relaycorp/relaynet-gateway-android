package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.test.factory.ParcelCollectionFactory
import tech.relaycorp.gateway.test.factory.StoredParcelFactory

class GenerateCargoTest {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val parcelCollectionDao = mock<ParcelCollectionDao>()
    private val diskMessageOperations = mock<DiskMessageOperations>()
    private val generateCargo =
        GenerateCargo(storedParcelDao, parcelCollectionDao, diskMessageOperations)

    @Test
    internal fun `empty cargo`() = runBlockingTest {
        whenever(storedParcelDao.listForRecipientLocation(any())).thenReturn(emptyList())
        whenever(parcelCollectionDao.getAll()).thenReturn(emptyList())

        val cargo = generateCargo.generate()

        assertTrue(cargo.none())
    }

    @Test
    internal fun `generate 1 cargo with 1 PCA and 1 parcel`() = runBlockingTest {
        whenever(storedParcelDao.listForRecipientLocation(any()))
            .thenReturn(listOf(StoredParcelFactory.build()))
        whenever(diskMessageOperations.readMessage(any(), any()))
            .thenReturn("ABC".toByteArray()::inputStream)
        whenever(parcelCollectionDao.getAll())
            .thenReturn(listOf(ParcelCollectionFactory.build()))

        val cargo = generateCargo.generate()

        assertEquals(1, cargo.count())
    }
}
