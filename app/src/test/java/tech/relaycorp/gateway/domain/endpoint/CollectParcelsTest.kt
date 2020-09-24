package tech.relaycorp.gateway.domain.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.disk.MessageDataNotFoundException
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.domain.DeleteParcel
import tech.relaycorp.gateway.test.factory.StoredParcelFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectParcelsTest {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val diskMessageOperations = mock<DiskMessageOperations>()
    private val deleteParcel = mock<DeleteParcel>()

    @BeforeEach
    internal fun setUp() = runBlockingTest {
        whenever(diskMessageOperations.readMessage(any(), any()))
            .thenReturn(ByteArray(0)::inputStream)
    }

    @Test
    internal fun getParcelsForEndpoint() = runBlockingTest {
        whenever(storedParcelDao.listForRecipients(any(), any(), any()))
            .thenReturn(flowOf(listOf(StoredParcelFactory.build())))

        val result =
            buildSubject().getNewParcelsForEndpoints(listOf(MessageAddress.of("1234"))).first()

        assertEquals(1, result.size)
    }

    @Test
    internal fun `getParcelsToDeliver with parcel data not found`() = runBlockingTest {
        whenever(storedParcelDao.listForRecipients(any(), any(), any()))
            .thenReturn(flowOf(listOf(StoredParcelFactory.build())))
        whenever(diskMessageOperations.readMessage(any(), any()))
            .thenThrow(MessageDataNotFoundException())

        val result =
            buildSubject().getNewParcelsForEndpoints(listOf(MessageAddress.of("1234"))).first()

        assertEquals(0, result.size)
    }

    @Test
    internal fun `getParcelsToDeliver does not send duplicates`() = runBlockingTest {
        val parcel = StoredParcelFactory.build()
        whenever(storedParcelDao.listForRecipients(any(), any(), any()))
            .thenReturn(flowOf(listOf(parcel), listOf(parcel)))

        val result =
            buildSubject().getNewParcelsForEndpoints(listOf(MessageAddress.of("1234"))).toList()

        assertEquals(1, result.flatten().size)
    }

    @Test
    internal fun processParcelAck() = runBlockingTest {
        val parcel = StoredParcelFactory.build()
        whenever(storedParcelDao.listForRecipients(any(), any(), any()))
            .thenReturn(flowOf(listOf(parcel)))

        val subject = buildSubject()

        val deliveries =
            subject.getNewParcelsForEndpoints(listOf(MessageAddress.of("1234"))).first()

        subject.processParcelAck(deliveries.first().first)
        verify(deleteParcel).delete(eq(parcel))
    }

    @Test
    internal fun noParcelsToDeliverOrAck() = runBlockingTest {
        val listState = MutableStateFlow(listOf(StoredParcelFactory.build()))
        whenever(storedParcelDao.listForRecipients(any(), any(), any())).thenReturn(listState)

        val subject = buildSubject()
        subject
            .getNewParcelsForEndpoints(listOf(MessageAddress.of("1234")))
            .launchIn(CoroutineScope(Dispatchers.Unconfined))

        assertTrue(subject.anyParcelsLeftToDeliverOrAck.first())

        listState.value = emptyList()
        assertFalse(subject.anyParcelsLeftToDeliverOrAck.first())
    }

    @Test
    internal fun `processParcelAck with unknown local id`() = runBlockingTest {
        buildSubject().processParcelAck("invalid")
        verify(deleteParcel, never()).delete(any())
    }

    private fun buildSubject() =
        CollectParcels(storedParcelDao, diskMessageOperations, deleteParcel)
}
