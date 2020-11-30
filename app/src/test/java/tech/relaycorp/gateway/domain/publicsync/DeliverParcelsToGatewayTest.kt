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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.disk.MessageDataNotFoundException
import tech.relaycorp.gateway.domain.DeleteParcel
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.pdc.PoWebClientBuilder
import tech.relaycorp.gateway.test.CargoDeliveryCertPath
import tech.relaycorp.gateway.test.factory.StoredParcelFactory
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.bindings.pdc.RejectedParcelException
import tech.relaycorp.relaynet.bindings.pdc.ServerConnectionException
import tech.relaycorp.relaynet.testing.KeyPairSet
import java.lang.IllegalArgumentException
import kotlin.test.assertEquals

class DeliverParcelsToGatewayTest {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val diskMessageOperations = mock<DiskMessageOperations>()
    private val poWebClient = mock<PoWebClient>()
    private val poWebClientBuilder = object : PoWebClientBuilder {
        override suspend fun build() = poWebClient
    }
    private val localConfig = mock<LocalConfig>()
    private val deleteParcel = mock<DeleteParcel>()
    private val subject = DeliverParcelsToGateway(
        storedParcelDao, diskMessageOperations, poWebClientBuilder, localConfig, deleteParcel
    )

    @BeforeEach
    internal fun setUp() = testSuspend {
        whenever(localConfig.getCertificate()).thenReturn(CargoDeliveryCertPath.PRIVATE_GW)
        whenever(localConfig.getKeyPair()).thenReturn(KeyPairSet.PRIVATE_GW)
        whenever(diskMessageOperations.readMessage(any(), any()))
            .thenReturn { "".byteInputStream() }
    }

    @Test
    internal fun `when keepAlive is false, only deliver first batch of parcels`() = testSuspend {
        val parcel1 = StoredParcelFactory.build()
        val parcel2 = StoredParcelFactory.build()
        whenever(storedParcelDao.observeForRecipientLocation(any(), any()))
            .thenReturn(flowOf(listOf(parcel1), listOf(parcel2)))

        subject.deliver(false)

        verify(poWebClient, times(1)).deliverParcel(
            any(),
            check {
                assertEquals(CargoDeliveryCertPath.PRIVATE_GW, it.certificate)
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
