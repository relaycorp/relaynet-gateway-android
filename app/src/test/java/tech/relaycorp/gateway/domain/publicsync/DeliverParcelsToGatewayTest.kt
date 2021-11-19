package tech.relaycorp.gateway.domain.publicsync

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.ktor.test.dispatcher.testSuspend
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.disk.MessageDataNotFoundException
import tech.relaycorp.gateway.data.disk.FileStore
import tech.relaycorp.gateway.data.doh.PublicAddressResolutionException
import tech.relaycorp.gateway.domain.DeleteParcel
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.pdc.PoWebClientProvider
import tech.relaycorp.gateway.test.BaseDataTestCase
import tech.relaycorp.gateway.test.factory.StoredParcelFactory
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.bindings.pdc.RejectedParcelException
import tech.relaycorp.relaynet.bindings.pdc.ServerConnectionException
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import kotlin.test.assertEquals

class DeliverParcelsToGatewayTest : BaseDataTestCase() {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val diskMessageOperations = mock<DiskMessageOperations>()
    private val poWebClient = mock<PoWebClient>()
    private val poWebClientProvider = object : PoWebClientProvider {
        override suspend fun get() = poWebClient
    }
    private val mockFileStore = mock<FileStore>()
    private val localConfig = LocalConfig(mockFileStore, privateKeyStore)
    private val deleteParcel = mock<DeleteParcel>()
    private val subject = DeliverParcelsToGateway(
        storedParcelDao, diskMessageOperations, poWebClientProvider, localConfig, deleteParcel
    )

    @BeforeEach
    internal fun setUp() = testSuspend {
        registerPrivateGatewayIdentityKeyPair()
        whenever(diskMessageOperations.readMessage(any(), any()))
            .thenReturn { "".byteInputStream() }
    }

    @Test
    fun `Failure to resolve PoWeb address should be ignored`() = runBlockingTest {
        val failingPoWebClientProvider = object : PoWebClientProvider {
            override suspend fun get() = throw PublicAddressResolutionException("Whoops")
        }
        val subject = DeliverParcelsToGateway(
            storedParcelDao,
            diskMessageOperations,
            failingPoWebClientProvider,
            localConfig,
            deleteParcel
        )

        subject.deliver(false)

        verify(poWebClient, never()).deliverParcel(any(), any())
    }

    @Test
    fun `when keepAlive is false, only deliver first batch of parcels`() = testSuspend {
        val parcel1 = StoredParcelFactory.build()
        val parcel2 = StoredParcelFactory.build()
        whenever(storedParcelDao.observeForRecipientLocation(any(), any()))
            .thenReturn(flowOf(listOf(parcel1), listOf(parcel2)))

        subject.deliver(false)

        verify(poWebClient, times(1)).deliverParcel(
            any(),
            check {
                assertEquals(PDACertPath.PRIVATE_GW, it.certificate)
            }
        )
        verify(deleteParcel).delete(eq(parcel1))
    }

    @Test
    internal fun `when keepAlive is true, all parcel batches are delivered`() = testSuspend {
        val parcel1 = StoredParcelFactory.build()
        val parcel2 = StoredParcelFactory.build()
        val parcel3 = StoredParcelFactory.build()
        whenever(storedParcelDao.observeForRecipientLocation(any(), any()))
            .thenReturn(
                flowOf(
                    listOf(parcel1, parcel2, parcel3),
                    listOf(parcel2, parcel3),
                    listOf(parcel3)
                )
            )

        subject.deliver(true)

        verify(poWebClient, times(3)).deliverParcel(any(), any())
        verify(deleteParcel).delete(eq(parcel1))
        verify(deleteParcel).delete(eq(parcel2))
        verify(deleteParcel).delete(eq(parcel3))
    }

    @Test
    internal fun `when parcel is rejected, it is deleted`() = testSuspend {
        val parcel = StoredParcelFactory.build()
        whenever(storedParcelDao.observeForRecipientLocation(any(), any()))
            .thenReturn(flowOf(listOf(parcel)))

        whenever(poWebClient.deliverParcel(any(), any())).thenThrow(RejectedParcelException(""))

        subject.deliver(false)

        verify(poWebClient, times(1)).deliverParcel(any(), any())
        verify(deleteParcel).delete(eq(parcel))
    }

    @Test
    internal fun `when could not read parcel, it is deleted`() = testSuspend {
        val parcel = StoredParcelFactory.build()
        whenever(storedParcelDao.observeForRecipientLocation(any(), any()))
            .thenReturn(flowOf(listOf(parcel)))
        whenever(diskMessageOperations.readMessage(any(), any()))
            .thenThrow(MessageDataNotFoundException())

        subject.deliver(false)

        verify(poWebClient, never()).deliverParcel(any(), any())
        verify(deleteParcel).delete(eq(parcel))
    }

    @Test
    internal fun `server issues are handled`() = testSuspend {
        val parcel = StoredParcelFactory.build()
        whenever(storedParcelDao.observeForRecipientLocation(any(), any()))
            .thenReturn(flowOf(listOf(parcel)))
        whenever(poWebClient.deliverParcel(any(), any())).thenThrow(ServerConnectionException(""))

        subject.deliver(false)
    }

    @Test
    internal fun `unexpected non-PDC exceptions are not handled`() = testSuspend {
        val parcel = StoredParcelFactory.build()
        whenever(storedParcelDao.observeForRecipientLocation(any(), any()))
            .thenReturn(flowOf(listOf(parcel)))
        whenever(poWebClient.deliverParcel(any(), any())).thenThrow(IllegalArgumentException(""))

        assertThrows<IllegalArgumentException> {
            subject.deliver(false)
        }
    }
}
