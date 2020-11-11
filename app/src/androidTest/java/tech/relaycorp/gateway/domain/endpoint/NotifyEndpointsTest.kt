package tech.relaycorp.gateway.domain.endpoint

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.data.database.AppDatabase
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.test.factory.LocalEndpointFactory
import tech.relaycorp.gateway.test.factory.StoredParcelFactory

class NotifyEndpointsTest {

    private val storedParcelDao by lazy {
        Room.inMemoryDatabaseBuilder(getApplicationContext<App>(), AppDatabase::class.java)
            .build()
            .storedParcelDao()
    }
    private val localEndpointDao by lazy {
        Room.inMemoryDatabaseBuilder(getApplicationContext<App>(), AppDatabase::class.java)
            .build()
            .localEndpointDao()
    }
    private val context by lazy { spy(getApplicationContext<App>()) }
    private val getEndpointReceiver = mock<GetEndpointReceiver>()
    private val notifyEndpoints by lazy {
        NotifyEndpoints(storedParcelDao, localEndpointDao, getEndpointReceiver, context)
    }

    @Test
    fun notifyAllPending() {
        runBlocking {
            val parcel1 = StoredParcelFactory.build()
                .copy(recipientLocation = RecipientLocation.LocalEndpoint)
            val parcel2 = StoredParcelFactory.build()
                .copy(recipientLocation = RecipientLocation.LocalEndpoint)
            val parcel3 = StoredParcelFactory.build().copy(
                recipientLocation = RecipientLocation.LocalEndpoint,
                recipientAddress = parcel2.recipientAddress
            )
            storedParcelDao.insert(parcel1)
            storedParcelDao.insert(parcel2)
            storedParcelDao.insert(parcel3)

            val endpoint1 = LocalEndpointFactory.build().copy(address = parcel1.recipientAddress)
            localEndpointDao.insert(endpoint1)
            val endpoint2 = LocalEndpointFactory.build().copy(address = parcel2.recipientAddress)
            localEndpointDao.insert(endpoint2)

            whenever(getEndpointReceiver.get(any())).thenReturn(".Receiver")

            notifyEndpoints.notifyAllPending()

            verify(context, times(2)).sendBroadcast(
                check {
                    assertTrue(
                        listOf(endpoint1.applicationId, endpoint2.applicationId)
                            .contains(it.component?.packageName)
                    )
                    assertEquals(".Receiver", it.component?.className)
                }
            )
            verifyNoMoreInteractions(context)
        }
    }

    @Test
    fun notify_withKnownAddressAndReceiver() {
        runBlocking {
            val endpoint = LocalEndpointFactory.build()
            localEndpointDao.insert(endpoint)

            val receiverName = "${endpoint.applicationId}.Receiver"
            whenever(getEndpointReceiver.get(any())).thenReturn(receiverName)

            notifyEndpoints.notify(endpoint.address)

            verify(context).sendBroadcast(
                check {
                    assertEquals(endpoint.applicationId, it.component?.packageName)
                    assertEquals(receiverName, it.component?.className)
                }
            )
        }
    }

    @Test(expected = NotifyEndpoints.UnreachableEndpointApp::class)
    fun notify_withKnownAddressButWithoutReceiver() {
        runBlocking {
            val endpoint = LocalEndpointFactory.build()
            localEndpointDao.insert(endpoint)

            whenever(getEndpointReceiver.get(any())).thenReturn(null)

            notifyEndpoints.notify(endpoint.address)
        }
    }

    @Test
    fun notify_withUnknownAddress() {
        runBlocking {
            notifyEndpoints.notify(MessageAddress.of("1234"))

            verify(context, never()).sendBroadcast(any(), any())
        }
    }
}
