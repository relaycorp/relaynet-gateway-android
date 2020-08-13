package tech.relaycorp.gateway.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.relaycorp.gateway.test.factory.LocalEndpointFactory

class LocalEndpointDaoTest {

    private val dao =
        Room.inMemoryDatabaseBuilder(getApplicationContext<Context>(), AppDatabase::class.java)
            .build()
            .localEndpointDao()

    @Test
    internal fun countApplicationIds() {
        runBlocking {
            val endpoint1 = LocalEndpointFactory.build()
            val endpoint2 = LocalEndpointFactory.build()
            val endpoint3 =
                LocalEndpointFactory.build().copy(applicationId = endpoint2.applicationId)

            listOf(endpoint1, endpoint2, endpoint3).forEach { dao.insert(it) }

            val result = dao.countApplicationIds().first()
            assertEquals(2, result)
        }
    }
}
