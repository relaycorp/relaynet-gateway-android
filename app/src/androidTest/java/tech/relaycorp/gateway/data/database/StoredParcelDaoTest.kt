package tech.relaycorp.gateway.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.test.factory.StoredParcelFactory

class StoredParcelDaoTest {

    private val dao =
        Room.inMemoryDatabaseBuilder(getApplicationContext<Context>(), AppDatabase::class.java)
            .build()
            .storedParcelDao()

    @Test
    internal fun countSizeForRecipientLocation() {
        runBlocking {
            val parcels =
                (1..3).map {
                    StoredParcelFactory.build()
                        .copy(recipientLocation = RecipientLocation.ExternalGateway)
                        .also { dao.insert(it) }
                }
            val totalSize = parcels.map { it.size }.reduce { acc, size -> size + acc }

            val result =
                dao.countSizeForRecipientLocation(RecipientLocation.ExternalGateway).first()
            assertEquals(totalSize, result)
        }
    }

    @Test
    internal fun listForRecipientLocation_skipsExpired() {
        runBlocking {
            // expired
            StoredParcelFactory.build()
                .copy(
                    recipientLocation = RecipientLocation.ExternalGateway,
                    expirationTimeUtc = nowInUtc().minusMinutes(5),
                )
                .also { dao.insert(it) }
            val parcelUnexpired = StoredParcelFactory.build()
                .copy(
                    recipientLocation = RecipientLocation.ExternalGateway,
                    expirationTimeUtc = nowInUtc().plusMinutes(5),
                )
                .also { dao.insert(it) }

            val result = dao.listForRecipientLocation(RecipientLocation.ExternalGateway)

            assertEquals(1, result.size)
            assertEquals(parcelUnexpired, result.first())
        }
    }
}
