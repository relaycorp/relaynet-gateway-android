package tech.relaycorp.gateway.domain.publicsync

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.ktor.test.dispatcher.testSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.doh.PublicAddressResolutionException
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.domain.endpoint.NotifyEndpoints
import tech.relaycorp.gateway.pdc.PoWebClientProvider
import tech.relaycorp.gateway.test.CargoDeliveryCertPath
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.bindings.pdc.ClientBindingException
import tech.relaycorp.relaynet.bindings.pdc.NonceSignerException
import tech.relaycorp.relaynet.bindings.pdc.ParcelCollection
import tech.relaycorp.relaynet.bindings.pdc.ServerConnectionException
import tech.relaycorp.relaynet.bindings.pdc.StreamingMode
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import java.lang.Thread.sleep
import kotlin.math.roundToLong
import kotlin.time.milliseconds

class CollectParcelsFromGatewayTest {

    private val storeParcel = mock<StoreParcel>()
    private val poWebClient = mock<PoWebClient>()
    private val poWebClientBuilder = object : PoWebClientProvider {
        override suspend fun get() = poWebClient
    }
    private val localConfig = mock<LocalConfig>()
    private val notifyEndpoints = mock<NotifyEndpoints>()
    private val subject = CollectParcelsFromGateway(
        storeParcel, poWebClientBuilder, localConfig, notifyEndpoints
    )

    @BeforeEach
    internal fun setUp() = testSuspend {
        whenever(localConfig.getCertificate()).thenReturn(CargoDeliveryCertPath.PRIVATE_GW)
        whenever(localConfig.getKeyPair()).thenReturn(KeyPairSet.PRIVATE_GW)
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.Success(mock()))
    }

    @Test
    fun `Failure to resolve PoWeb address should be ignored`() = runBlockingTest {
        val failingPoWebClientProvider = object : PoWebClientProvider {
            override suspend fun get() = throw PublicAddressResolutionException("Whoops")
        }
        val subject = CollectParcelsFromGateway(
            storeParcel, failingPoWebClientProvider, localConfig, notifyEndpoints
        )

        subject.collect(false)

        verify(poWebClient, never()).collectParcels(any(), any())
    }

    @Test
    internal fun `collect parcels with keepAlive false`() = testSuspend {
        val parcelCollection = mock<ParcelCollection>()
        whenever(parcelCollection.parcelSerialized).thenReturn(ByteArray(0))
        whenever(parcelCollection.ack).thenReturn {}
        whenever(poWebClient.collectParcels(any(), any())).thenReturn(flowOf(parcelCollection))

        subject.collect(false)

        verify(poWebClient).collectParcels(
            check { assertEquals(CargoDeliveryCertPath.PRIVATE_GW, it.first().certificate) },
            check { assertEquals(StreamingMode.CloseUponCompletion, it) }
        )
        verify(storeParcel)
            .store(eq(parcelCollection.parcelSerialized), eq(RecipientLocation.LocalEndpoint))
        verify(parcelCollection).ack
        verify(notifyEndpoints).notifyAllPending()
    }

    @Test
    internal fun `collect parcels with keepAlive true`() = testSuspend {
        val parcelCollection = mock<ParcelCollection>()
        whenever(parcelCollection.parcelSerialized).thenReturn(ByteArray(0))
        whenever(parcelCollection.ack).thenReturn {}
        whenever(poWebClient.collectParcels(any(), any()))
            .thenReturn(flowOf(parcelCollection, parcelCollection))
        val parcel = mock<Parcel>()
        val parcelAddress = "1234"
        whenever(parcel.recipientAddress).thenReturn(parcelAddress)
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.Success(parcel))

        subject.collect(true)

        verify(poWebClient).collectParcels(
            check { assertEquals(CargoDeliveryCertPath.PRIVATE_GW, it.first().certificate) },
            check { assertEquals(StreamingMode.KeepAlive, it) }
        )
        verify(storeParcel, times(2))
            .store(eq(parcelCollection.parcelSerialized), eq(RecipientLocation.LocalEndpoint))
        verify(parcelCollection, times(2)).ack
        verify(notifyEndpoints, times(2)).notify(eq(MessageAddress.of(parcelAddress)))
    }

    @Test
    internal fun `collect invalid parcel, with keepAlive true, acks but does not notify`() =
        testSuspend {
            val parcelCollection = mock<ParcelCollection>()
            whenever(parcelCollection.parcelSerialized).thenReturn(ByteArray(0))
            whenever(parcelCollection.ack).thenReturn {}
            whenever(poWebClient.collectParcels(any(), any())).thenReturn(flowOf(parcelCollection))
            whenever(storeParcel.store(any<ByteArray>(), any()))
                .thenReturn(StoreParcel.Result.InvalidParcel(mock(), Exception()))

            subject.collect(true)

            verify(parcelCollection).ack
            verifyNoMoreInteractions(notifyEndpoints)
        }

    @Test
    internal fun `poWebClient client binding issues are handled`() = testSuspend {
        whenever(poWebClient.collectParcels(any(), any())).thenThrow(ClientBindingException(""))
        subject.collect(false)
    }

    @Test
    internal fun `poWebClient signer issues are handled`() = testSuspend {
        whenever(poWebClient.collectParcels(any(), any())).thenThrow(NonceSignerException(""))
        subject.collect(false)
    }

    @Test
    internal fun `poWebClient with keepAlive false, server issues are handled`() = testSuspend {
        whenever(poWebClient.collectParcels(any(), any())).thenThrow(ServerConnectionException(""))
        subject.collect(false)
    }

    @Test
    internal fun `poWebClient with keepAlive true, retries after server issue`() {
        val retryPeriod = 100.milliseconds
        CollectParcelsFromGateway.RETRY_AFTER_PERIOD = retryPeriod
        runBlockingTest {
            var tries = 0
            whenever(poWebClient.collectParcels(any(), any()))
                .thenReturn(
                    flow<ParcelCollection> {
                        throw ServerConnectionException("")
                    }.onCompletion { tries++ }
                )

            CoroutineScope(Dispatchers.Unconfined).launch {
                subject.collect(true)
            }
            sleep((retryPeriod.toLongMilliseconds() * 1.5).roundToLong())
            assertEquals(2, tries)
        }
    }
}
