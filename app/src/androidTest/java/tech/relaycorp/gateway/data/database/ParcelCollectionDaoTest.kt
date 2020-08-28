package tech.relaycorp.gateway.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.relaycorp.gateway.test.factory.ParcelCollectionFactory

class ParcelCollectionDaoTest {

    private val dao =
        Room.inMemoryDatabaseBuilder(getApplicationContext<Context>(), AppDatabase::class.java)
            .build()
            .parcelCollectionDao()

    @Test
    internal fun exists() {
        runBlocking {
            val element = ParcelCollectionFactory.build()
            assertFalse(
                dao.exists(element.recipientAddress, element.senderAddress, element.messageId)
            )

            dao.insert(element)
            assertTrue(
                dao.exists(element.recipientAddress, element.senderAddress, element.messageId)
            )
        }
    }
}
