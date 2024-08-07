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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.doh.InternetAddressResolutionException
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.domain.endpoint.IncomingParcelNotifier
import tech.relaycorp.gateway.pdc.PoWebClientProvider
import tech.relaycorp.gateway.test.BaseDataTestCase
import tech.relaycorp.gateway.test.TestLogHandler
import tech.relaycorp.poweb.PoWebClient
import tech.relaycorp.relaynet.bindings.pdc.ClientBindingException
import tech.relaycorp.relaynet.bindings.pdc.NonceSignerException
import tech.relaycorp.relaynet.bindings.pdc.ParcelCollection
import tech.relaycorp.relaynet.bindings.pdc.ServerConnectionException
import tech.relaycorp.relaynet.bindings.pdc.StreamingMode
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import java.util.logging.Level
import java.util.logging.Logger

class CollectParcelsFromGatewayTest : BaseDataTestCase() {

    private val storeParcel = mock<StoreParcel>()
    private val poWebClient = mock<PoWebClient>()
    private val poWebClientBuilder = object : PoWebClientProvider {
        override suspend fun get() = poWebClient
    }
    private val mockInternetGatewayPreferences = mock<InternetGatewayPreferences>()
    private val mockLocalConfig = LocalConfig(
        privateKeyStoreProvider,
        certificateStoreProvider,
        mockInternetGatewayPreferences,
    )
    private val notifyEndpoints = mock<IncomingParcelNotifier>()
    private val subject = CollectParcelsFromGateway(
        storeParcel,
        poWebClientBuilder,
        notifyEndpoints,
        mockLocalConfig,
    )
    private val testLogHandler = TestLogHandler()

    @BeforeEach
    fun setUp() = testSuspend {
        registerPrivateGatewayParcelDeliveryCertificate()
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.Success(mock()))
        whenever(mockInternetGatewayPreferences.getId())
            .thenReturn(PDACertPath.INTERNET_GW.subjectId)

        Logger.getLogger(CollectParcelsFromGateway::class.java.name).addHandler(testLogHandler)
    }

    @AfterEach
    fun tearDown() {
        testLogHandler.close()
        Logger.getLogger(CollectParcelsFromGateway::class.java.name).removeHandler(testLogHandler)
    }

    @Test
    fun `Failure to resolve PoWeb address should be ignored`() = runTest {
        val failingPoWebClientProvider = object : PoWebClientProvider {
            override suspend fun get() = throw InternetAddressResolutionException("Whoops")
        }
        val subject = CollectParcelsFromGateway(
            storeParcel,
            failingPoWebClientProvider,
            notifyEndpoints,
            mockLocalConfig,
        )

        subject.collect(false)

        verify(poWebClient, never()).collectParcels(any(), any())
    }

    @Test
    fun `With missing parcel delivery certificate should not collect parcels`() = runTest {
        clearPrivateGatewayParcelDeliveryCertificate()

        subject.collect(false)

        verify(poWebClient, never()).collectParcels(any(), any())
    }

    @Test
    fun `collect parcels with keepAlive false`() = testSuspend {
        val parcelCollection = mock<ParcelCollection>()
        whenever(parcelCollection.parcelSerialized).thenReturn(ByteArray(0))
        whenever(parcelCollection.ack).thenReturn {}
        whenever(poWebClient.collectParcels(any(), any())).thenReturn(flowOf(parcelCollection))

        subject.collect(false)

        verify(poWebClient).collectParcels(
            check { assertEquals(PDACertPath.PRIVATE_GW, it.first().certificate) },
            check { assertEquals(StreamingMode.CloseUponCompletion, it) },
        )
        verify(storeParcel)
            .store(eq(parcelCollection.parcelSerialized), eq(RecipientLocation.LocalEndpoint))
        verify(parcelCollection).ack
        verify(notifyEndpoints).notifyAllPending()
    }

    @Test
    fun `collect parcels with keepAlive true`() = testSuspend {
        val parcelCollection = mock<ParcelCollection>()
        whenever(parcelCollection.parcelSerialized).thenReturn(ByteArray(0))
        whenever(parcelCollection.ack).thenReturn {}
        whenever(poWebClient.collectParcels(any(), any()))
            .thenReturn(flowOf(parcelCollection, parcelCollection))
        val parcel = mock<Parcel>()
        val parcelAddress = "1234"
        val mockRecipient = Recipient("0deadbeef", parcelAddress)
        whenever(parcel.recipient).thenReturn(mockRecipient)
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.Success(parcel))

        subject.collect(true)

        verify(poWebClient).collectParcels(
            check { assertEquals(PDACertPath.PRIVATE_GW, it.first().certificate) },
            check { assertEquals(StreamingMode.KeepAlive, it) },
        )
        verify(storeParcel, times(2))
            .store(eq(parcelCollection.parcelSerialized), eq(RecipientLocation.LocalEndpoint))
        verify(parcelCollection, times(2)).ack
        verify(notifyEndpoints, times(2)).notify(
            eq(MessageAddress.of(mockRecipient.id)),
        )
    }

    @Test
    fun `collect invalid parcel, with keepAlive true, acks but does not notify`() = testSuspend {
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
    fun `poWebClient client binding issues are handled`() = testSuspend {
        whenever(poWebClient.collectParcels(any(), any())).thenReturn(
            flow {
                throw ClientBindingException("Message")
            },
        )

        subject.collect(false)

        val logRecord = testLogHandler.filterLogs(
            Level.SEVERE,
            "Could not collect parcels due to client error",
        )
            .singleOrNull()
        assert(logRecord != null)
        assert(logRecord?.thrown is ClientBindingException)
        verifyNoMoreInteractions(storeParcel, notifyEndpoints)
    }

    @Test
    fun `poWebClient signer issues are handled`() = testSuspend {
        whenever(poWebClient.collectParcels(any(), any())).thenReturn(
            flow {
                throw NonceSignerException("Message")
            },
        )

        subject.collect(false)

        val logRecord = testLogHandler.filterLogs(
            Level.SEVERE,
            "Could not collect parcels due to signing error",
        )
            .singleOrNull()
        assert(logRecord != null)
        assert(logRecord?.thrown is NonceSignerException)
        verifyNoMoreInteractions(storeParcel, notifyEndpoints)
    }

    @Test
    fun `poWebClient with keepAlive false, server issues are handled`() = testSuspend {
        whenever(poWebClient.collectParcels(any(), any())).thenReturn(
            flow {
                throw ServerConnectionException("Message")
            },
        )

        subject.collect(false)

        val logRecord = testLogHandler.filterLogs(
            Level.WARNING,
            "Could not collect parcels due to server error",
        )
            .singleOrNull()
        assert(logRecord != null)
        assert(logRecord?.thrown is ServerConnectionException)
        verifyNoMoreInteractions(storeParcel, notifyEndpoints)
    }
}
