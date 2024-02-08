package tech.relaycorp.gateway.domain.endpoint

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.test.factory.LocalEndpointFactory
import tech.relaycorp.gateway.test.factory.StoredParcelFactory

class NotifyEndpointsTest {

    private val context by lazy { spy(getApplicationContext<App>()) }
    private val getEndpointReceiver = mock<GetEndpointReceiver>()
    private val notifyEndpoints by lazy {
        NotifyEndpoints(getEndpointReceiver, context)
    }

    @Test
    fun notifyAllPending_oncePerEndpoint() = runTest {
        val parcel1 = StoredParcelFactory.build()
            .copy(recipientLocation = RecipientLocation.LocalEndpoint)
        val parcel2 = StoredParcelFactory.build()
            .copy(recipientLocation = RecipientLocation.LocalEndpoint)

        val endpoint1 = LocalEndpointFactory.build().copy(address = parcel1.recipientAddress)
        val endpoint2 = LocalEndpointFactory.build().copy(address = parcel2.recipientAddress)

        whenever(getEndpointReceiver.get(any(), any())).thenReturn(".Receiver")

        notifyEndpoints.notify(
            listOf(endpoint1, endpoint2),
            NotificationType.IncomingParcel,
        )

        verify(context, times(2)).sendBroadcast(
            check {
                assertTrue(
                    listOf(endpoint1.applicationId, endpoint2.applicationId)
                        .contains(it.component?.packageName),
                )
                assertEquals(".Receiver", it.component?.className)
            },
        )
        verifyNoMoreInteractions(context)
    }

    @Test
    fun notifyAllPending_oncePerApplicationId() = runTest {
        val appId = "123"
        val endpoint = LocalEndpointFactory.build()
            .copy(applicationId = "123")

        whenever(getEndpointReceiver.get(any(), any())).thenReturn(".Receiver")

        notifyEndpoints.notify(
            listOf(endpoint, endpoint),
            NotificationType.IncomingParcel,
        )

        verify(context, times(1)).sendBroadcast(
            check {
                assertEquals(appId, it.component?.packageName)
                assertEquals(".Receiver", it.component?.className)
            },
        )
        verifyNoMoreInteractions(context)
    }

    @Test
    fun notify_withKnownAddressAndReceiver() = runTest {
        val endpoint = LocalEndpointFactory.build()
        val receiverName = "${endpoint.applicationId}.Receiver"
        whenever(getEndpointReceiver.get(any(), any())).thenReturn(receiverName)

        notifyEndpoints.notify(endpoint, NotificationType.IncomingParcel)

        verify(context).sendBroadcast(
            check {
                assertEquals(endpoint.applicationId, it.component?.packageName)
                assertEquals(receiverName, it.component?.className)
            },
        )
    }

    @Test
    fun notify_withKnownAddressButWithoutReceiver() = runTest {
        whenever(getEndpointReceiver.get(any(), any())).thenReturn(null)
        verifyNoMoreInteractions(context)
    }

    @Test
    fun notify_withUnknownAddress() = runTest {
        whenever(getEndpointReceiver.get(any(), any())).thenReturn(null)
        notifyEndpoints.notify(
            LocalEndpointFactory.build(),
            NotificationType.IncomingParcel,
        )
        verify(context, never()).sendBroadcast(any(), any())
    }
}
