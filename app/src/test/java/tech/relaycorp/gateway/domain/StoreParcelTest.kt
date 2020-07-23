package tech.relaycorp.gateway.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.test.factory.ParcelFactory
import tech.relaycorp.relaynet.messages.Parcel

internal class StoreParcelTest {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val diskOperations = mock<DiskMessageOperations>()
    private val storeParcel = StoreParcel(storedParcelDao, diskOperations)

    @Test
    internal fun `store malformed parcel`() = runBlockingTest {
        val result = storeParcel.store(ByteArray(0).inputStream(), RecipientLocation.LocalEndpoint)
        assertTrue(result is StoreParcel.Result.MalformedParcel)
    }

    @Test
    internal fun `store invalid parcel`() = runBlockingTest {
        val parcel = Parcel(
            recipientAddress = "__invalid__",
            senderCertificate = ParcelFactory.certificate,
            payload = ByteArray(0)
        ).serialize(ParcelFactory.keyPair.private)

        val result = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)
        assertTrue(result is StoreParcel.Result.InvalidParcel)
    }

    @Test
    internal fun `store parcel with public address for local endpoint`() = runBlockingTest {
        val parcel = Parcel(
            recipientAddress = "http://example.org",
            senderCertificate = ParcelFactory.certificate,
            payload = ByteArray(0)
        ).serialize(ParcelFactory.keyPair.private)

        val result = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)
        assertTrue(result is StoreParcel.Result.InvalidPublicLocalRecipient)
    }

    @Test
    internal fun `store parcel successfully`() = runBlockingTest {
        whenever(diskOperations.writeMessage(any(), any(), any())).thenReturn("")
        val parcel = Parcel(
            recipientAddress = "012345",
            senderCertificate = ParcelFactory.certificate,
            payload = ByteArray(0)
        ).serialize(ParcelFactory.keyPair.private)

        val result = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)

        assertTrue(result is StoreParcel.Result.Success)
        verify(diskOperations).writeMessage(any(), any(), any())
        verify(storedParcelDao).insert(any())
    }
}
