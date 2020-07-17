package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.domain.endpoint.NotifyEndpoint
import tech.relaycorp.gateway.test.factory.StoredParcelFactory

class ProcessParcelsTest {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val notifyEndpoint = mock<NotifyEndpoint>()
    private val processParcels = ProcessParcels(storedParcelDao, notifyEndpoint)

    @Test
    fun process() = runBlockingTest {
        val parcel1 = StoredParcelFactory.build()
        val parcel2 = StoredParcelFactory.build()
        val parcel3 = StoredParcelFactory.build().copy(recipientAddress = parcel2.recipientAddress)

        val parcels = listOf(parcel1, parcel2, parcel3)
        whenever(storedParcelDao.listForRecipientLocation(any())).thenReturn(parcels)

        processParcels.process()

        verify(notifyEndpoint, times(1)).notify(eq(parcel1.recipientAddress))
        verify(notifyEndpoint, times(1)).notify(eq(parcel2.recipientAddress))
    }
}
