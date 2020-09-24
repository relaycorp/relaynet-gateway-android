package tech.relaycorp.gateway.domain.endpoint

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.relaycorp.gateway.App
import tech.relaycorp.gateway.data.database.AppDatabase
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.test.factory.LocalEndpointFactory

class NotifyEndpointTest {

    private val localEndpointDao by lazy {
        Room.inMemoryDatabaseBuilder(getApplicationContext<App>(), AppDatabase::class.java)
            .build()
            .localEndpointDao()
    }
    private val context by lazy { spy(getApplicationContext<App>()) }
    private val getEndpointReceiver = mock<GetEndpointReceiver>()
    private val notifyEndpoint by lazy {
        NotifyEndpoint(localEndpointDao, getEndpointReceiver, context)
    }

    @Test
    fun notify_withKnownAddressAndReceiver() {
        runBlocking {
            val endpoint = LocalEndpointFactory.build()
            localEndpointDao.insert(endpoint)

            val receiverName = "${endpoint.applicationId}.Receiver"
            whenever(getEndpointReceiver.get(any())).thenReturn(receiverName)

            notifyEndpoint.notify(endpoint.address)

            verify(context).sendBroadcast(
                check {
                    assertEquals(endpoint.applicationId, it.component?.packageName)
                    assertEquals(receiverName, it.component?.className)
                }
            )
        }
    }

    @Test(expected = NotifyEndpoint.UnreachableEndpointApp::class)
    fun notify_withKnownAddressButWithoutReceiver() {
        runBlocking {
            val endpoint = LocalEndpointFactory.build()
            localEndpointDao.insert(endpoint)

            whenever(getEndpointReceiver.get(any())).thenReturn(null)

            notifyEndpoint.notify(endpoint.address)
        }
    }

    @Test
    fun notify_withUnknownAddress() {
        runBlocking {
            notifyEndpoint.notify(MessageAddress.of("1234"))

            verify(context, never()).sendBroadcast(any(), any())
        }
    }
}
