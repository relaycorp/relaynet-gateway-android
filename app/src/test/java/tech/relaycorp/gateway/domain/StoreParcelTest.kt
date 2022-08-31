package tech.relaycorp.gateway.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.relaynet.issueEndpointCertificate
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import java.time.ZonedDateTime

internal class StoreParcelTest {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val parcelCollectionDao = mock<ParcelCollectionDao>()
    private val diskOperations = mock<DiskMessageOperations>()
    private val mockLocalConfig = mock<LocalConfig>()
    private val storeParcel =
        StoreParcel(storedParcelDao, parcelCollectionDao, diskOperations, mockLocalConfig)

    private val publicEndpointAddress = "example.org"

    @BeforeEach
    fun setUp() = runBlockingTest {
        whenever(mockLocalConfig.getAllValidIdentityCertificates())
            .thenReturn(listOf(PDACertPath.PRIVATE_GW))
        whenever(parcelCollectionDao.exists(any(), any(), any())).thenReturn(false)
    }

    @Test
    internal fun `store malformed parcel`() = runBlockingTest {
        val result = storeParcel.store(ByteArray(0).inputStream(), RecipientLocation.LocalEndpoint)
        assertTrue(result is StoreParcel.Result.MalformedParcel)
    }

    @Test
    internal fun `store invalid parcel bound for local endpoint`() = runBlockingTest {
        val parcel = Parcel(
            Recipient(PDACertPath.PRIVATE_ENDPOINT.subjectId),
            ByteArray(0),
            PDACertPath.INTERNET_GW // Unauthorized sender
        ).serialize(KeyPairSet.INTERNET_GW.private)

        val result = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)
        assertTrue(result is StoreParcel.Result.InvalidParcel)
    }

    @Test
    internal fun `store invalid parcel bound for external gateway`() = runBlockingTest {
        val parcel = Parcel(
            Recipient("0deadbeef", publicEndpointAddress),
            ByteArray(0),
            PDACertPath.PRIVATE_ENDPOINT,
            "",
            ZonedDateTime.now().plusMinutes(2)
        ).serialize(KeyPairSet.PRIVATE_ENDPOINT.private)

        val result = storeParcel.store(parcel, RecipientLocation.ExternalGateway)
        assertTrue(result is StoreParcel.Result.InvalidParcel)
    }

    @Test
    internal fun `store parcel with public address for local endpoint`() = runBlockingTest {
        // The sender is authorized by one of the local endpoints but the recipient is a public
        // address
        val parcel = Parcel(
            Recipient("0deadbeef", publicEndpointAddress),
            ByteArray(0),
            PDACertPath.PDA
        ).serialize(KeyPairSet.PDA_GRANTEE.private)

        val result = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)
        assertTrue(result is StoreParcel.Result.InvalidParcel)
    }

    @Test
    fun `store parcel bound for local endpoint successfully`() = runBlockingTest {
        whenever(diskOperations.writeMessage(any(), any(), any())).thenReturn("")
        val parcel = Parcel(
            Recipient(PDACertPath.PRIVATE_ENDPOINT.subjectId),
            ByteArray(0),
            PDACertPath.PDA,
            senderCertificateChain = setOf(PDACertPath.PRIVATE_ENDPOINT)
        ).serialize(KeyPairSet.PDA_GRANTEE.private)

        val result = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)

        assertTrue(result is StoreParcel.Result.Success)
        verify(diskOperations).writeMessage(any(), any(), any())
        verify(storedParcelDao).insert(any())
    }

    @Test
    fun `store parcel bound for external gateway successfully`() = runBlockingTest {
        whenever(diskOperations.writeMessage(any(), any(), any())).thenReturn("")
        val untrustedKeyPair = generateRSAKeyPair()
        val untrustedCertificate = issueEndpointCertificate(
            untrustedKeyPair.public,
            untrustedKeyPair.private,
            ZonedDateTime.now().plusMinutes(2)
        )
        val parcel = Parcel(
            Recipient("0deadbeef", publicEndpointAddress),
            ByteArray(0),
            untrustedCertificate
        ).serialize(untrustedKeyPair.private)

        val result = storeParcel.store(parcel, RecipientLocation.ExternalGateway)

        assertTrue(result is StoreParcel.Result.Success)
        verify(diskOperations).writeMessage(any(), any(), any())
        verify(storedParcelDao).insert(any())
    }

    @Test
    internal fun `store duplicated parcel`() = runBlockingTest {
        whenever(parcelCollectionDao.exists(any(), any(), any())).thenReturn(true)

        val parcel = Parcel(
            Recipient(PDACertPath.PRIVATE_ENDPOINT.subjectId),
            ByteArray(0),
            PDACertPath.PDA,
            senderCertificateChain = setOf(PDACertPath.PRIVATE_ENDPOINT)
        ).serialize(KeyPairSet.PDA_GRANTEE.private)

        val result = storeParcel.store(parcel, RecipientLocation.LocalEndpoint)
        assertTrue(result is StoreParcel.Result.CollectedParcel)
    }
}
