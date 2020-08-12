package tech.relaycorp.gateway.domain.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.disk.MessageDataNotFoundException
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.domain.DeleteParcel
import tech.relaycorp.gateway.test.factory.StoredParcelFactory
import kotlin.test.assertEquals

class ParcelDeliveryTest {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val diskMessageOperations = mock<DiskMessageOperations>()
    private val deleteParcel = mock<DeleteParcel>()

    @Test
    internal fun getParcelsToDeliver() = runBlockingTest {
        whenever(storedParcelDao.listForRecipient(any(), any()))
            .thenReturn(listOf(StoredParcelFactory.build()))
        whenever(diskMessageOperations.readMessage(any(), any()))
            .thenReturn(ByteArray(0)::inputStream)

        val result = buildParcelDelivery().getParcelsToDeliver(MessageAddress.of("1234"))

        assertEquals(1, result.count())
    }

    @Test
    internal fun `getParcelsToDeliver with parcel data not found`() = runBlockingTest {
        whenever(storedParcelDao.listForRecipient(any(), any()))
            .thenReturn(listOf(StoredParcelFactory.build()))
        whenever(diskMessageOperations.readMessage(any(), any()))
            .thenThrow(MessageDataNotFoundException())

        val result = buildParcelDelivery().getParcelsToDeliver(MessageAddress.of("1234"))

        assertEquals(0, result.count())
    }

    @Test
    internal fun processParcelAck() = runBlockingTest {
        val parcel = StoredParcelFactory.build()
        whenever(storedParcelDao.listForRecipient(any(), any()))
            .thenReturn(listOf(parcel))
        whenever(diskMessageOperations.readMessage(any(), any()))
            .thenReturn(ByteArray(0)::inputStream)

        val parcelDelivery = buildParcelDelivery()
        val deliveries = parcelDelivery.getParcelsToDeliver(MessageAddress.of("1234"))
        parcelDelivery.processParcelAck(deliveries.first().localId)

        verify(deleteParcel).delete(eq(parcel))
    }

    @Test
    internal fun `processParcelAck with unknown local id`() = runBlockingTest {
        buildParcelDelivery().processParcelAck("invalid")

        verify(deleteParcel, never()).delete(any())
    }

    private fun buildParcelDelivery() =
        ParcelDelivery(storedParcelDao, diskMessageOperations, deleteParcel)
}
