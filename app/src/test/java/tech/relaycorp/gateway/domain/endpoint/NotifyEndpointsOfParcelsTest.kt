package tech.relaycorp.gateway.domain.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.never
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.database.LocalEndpointDao
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.model.LocalEndpoint
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.test.factory.LocalEndpointFactory
import tech.relaycorp.gateway.test.factory.StoredParcelFactory
import kotlin.test.assertEquals

class NotifyEndpointsOfParcelsTest {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val localEndpointDao = mock<LocalEndpointDao>()
    private val notifyEndpoints = mock<NotifyEndpoints>()

    @Test
    internal fun `only notify parcels with unique addresses`() = runBlockingTest {
        // Arrange
        val useCase = build()

        val parcel1 = StoredParcelFactory.build()
            .copy(recipientLocation = RecipientLocation.LocalEndpoint)
        val parcel2 = StoredParcelFactory.build()
            .copy(recipientLocation = RecipientLocation.LocalEndpoint)
        val parcel3 = StoredParcelFactory.build().copy(
            recipientLocation = RecipientLocation.LocalEndpoint,
            recipientAddress = parcel2.recipientAddress
        )

        val endpoint1 = LocalEndpointFactory.build().copy(address = parcel1.recipientAddress)
        val endpoint2 = LocalEndpointFactory.build().copy(address = parcel2.recipientAddress)

        whenever(
            storedParcelDao.listForRecipientLocation(any(), any())
        ).thenReturn(listOf(parcel1, parcel2, parcel3))

        whenever(localEndpointDao.list(any())).thenReturn(listOf(endpoint1, endpoint2))

        // Act
        useCase.notifyAllPending()

        // Assert
        verify(localEndpointDao).list(
            check {
                assertEquals(2, it.size)
            }
        )
        verify(notifyEndpoints).notify(
            listOf(endpoint1, endpoint2),
            EndpointNotifyAction.ParcelToReceive
        )
    }

    @Test
    fun `notify by message address`() = runBlockingTest {
        // Arrange
        val useCase = build()
        val endpoint1 = LocalEndpointFactory.build()
        whenever(localEndpointDao.get(endpoint1.address)).thenReturn(endpoint1)

        // Act
        useCase.notify(endpoint1.address)

        // Assert
        verify(localEndpointDao).get(
            check {
                assertEquals(endpoint1.address, it)
            }
        )
        verify(notifyEndpoints).notify(
            endpoint1,
            EndpointNotifyAction.ParcelToReceive
        )
    }

    @Test
    fun `don't notify when theres no local address`() = runBlockingTest {
        // Arrange
        val useCase = build()
        whenever(localEndpointDao.get(any())).thenReturn(null)

        // Act
        useCase.notify(LocalEndpointFactory.build().address)

        // Assert
        verify(notifyEndpoints, never()).notify(any<LocalEndpoint>(), any())
    }

    fun build() = NotifyEndpointsOfParcels(
        notifyEndpoints,
        storedParcelDao,
        localEndpointDao
    )
}
