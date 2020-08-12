package tech.relaycorp.gateway.domain.endpoint

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
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
    private val notifyEndpoint by lazy { NotifyEndpoint(localEndpointDao, context) }

    @Test
    fun notify_withKnownAddress() {
        runBlocking {
            val endpoint = LocalEndpointFactory.build()
            localEndpointDao.insert(endpoint)

            notifyEndpoint.notify(endpoint.address)

            verify(context).sendBroadcast(
                check {
                    assertEquals(endpoint.applicationId, it.`package`)
                },
                any()
            )
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
