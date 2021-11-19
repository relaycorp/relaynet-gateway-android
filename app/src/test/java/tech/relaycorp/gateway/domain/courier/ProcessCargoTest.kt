package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.disk.CargoStorage
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.DeleteParcel
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.domain.StoreParcelCollection
import tech.relaycorp.gateway.test.BaseDataTestCase
import tech.relaycorp.gateway.test.factory.ParcelCollectionAckFactory
import tech.relaycorp.gateway.test.factory.ParcelFactory
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.messages.payloads.CargoMessageSet
import tech.relaycorp.relaynet.testing.pki.CDACertPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet

class ProcessCargoTest : BaseDataTestCase() {

    private val cargoStorage = mock<CargoStorage>()
    private val storeParcel = mock<StoreParcel>()
    private val storeParcelCollection = mock<StoreParcelCollection>()
    private val deleteParcel = mock<DeleteParcel>()

    private val processCargo = ProcessCargo(
        cargoStorage,
        storeParcel,
        storeParcelCollection,
        deleteParcel,
        gatewayManager
    )

    @Test
    internal fun `deletes all cargo at the end`() = runBlockingTest {
        whenever(cargoStorage.list()).thenReturn(emptyList())
        processCargo.process()
        verify(cargoStorage).deleteAll()
    }

    @Test
    internal fun `deletes parcel when its ack is received`() = runBlockingTest {
        val pca = ParcelCollectionAckFactory.build()
        val cargoSerialized = generateCargoFromMessages(listOf(pca.serialize()))
        whenever(cargoStorage.list()).thenReturn(listOf(cargoSerialized::inputStream))

        processCargo.process()

        verify(deleteParcel).delete(
            eq(MessageAddress.of(pca.recipientEndpointAddress)),
            eq(MessageAddress.of(pca.senderEndpointPrivateAddress)),
            eq(MessageId(pca.parcelId))
        )
    }

    @Test
    fun `store received parcel with collection`() = runBlockingTest {
        val parcel = ParcelFactory.build()
        val cargoSerialized = generateCargoFromMessages(
            listOf(parcel.serialize(KeyPairSet.PDA_GRANTEE.private))
        )
        whenever(cargoStorage.list()).thenReturn(listOf(cargoSerialized::inputStream))
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.Success(parcel))

        processCargo.process()

        verify(storeParcel).store(any<ByteArray>(), eq(RecipientLocation.LocalEndpoint))
        verify(storeParcelCollection).storeForParcel(eq(parcel))
    }

    @Test
    fun `received malformed parcel that does not get stored`() = runBlockingTest {
        val parcel = ParcelFactory.build()
        val cargoSerialized = generateCargoFromMessages(
            listOf(parcel.serialize(KeyPairSet.PDA_GRANTEE.private))
        )
        whenever(cargoStorage.list()).thenReturn(listOf(cargoSerialized::inputStream))
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.MalformedParcel(Exception()))

        processCargo.process()

        verify(storeParcel).store(any<ByteArray>(), any())
        verify(storeParcelCollection, never()).storeForParcel(any())
    }

    @Test
    fun `received duplicated parcel that does not get stored`() = runBlockingTest {
        val parcel = ParcelFactory.build()
        val cargoSerialized = generateCargoFromMessages(
            listOf(parcel.serialize(KeyPairSet.PDA_GRANTEE.private))
        )
        whenever(cargoStorage.list()).thenReturn(listOf(cargoSerialized::inputStream))
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.CollectedParcel(ParcelFactory.build()))

        processCargo.process()

        verify(storeParcel).store(any<ByteArray>(), any())
        verify(storeParcelCollection, never()).storeForParcel(any())
    }

    @Test
    fun `received invalid parcel but collection is stored`() = runBlockingTest {
        val parcel = ParcelFactory.build()
        val cargoSerialized = generateCargoFromMessages(
            listOf(parcel.serialize(KeyPairSet.PDA_GRANTEE.private))
        )
        whenever(cargoStorage.list()).thenReturn(listOf(cargoSerialized::inputStream))
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.InvalidParcel(ParcelFactory.build(), Exception()))

        processCargo.process()

        verify(storeParcel).store(any<ByteArray>(), any())
        verify(storeParcelCollection).storeForParcel(any())
    }

    private fun generateCargoFromMessages(messagesSerialized: List<ByteArray>): ByteArray {
        val cargoMessageSet = CargoMessageSet(messagesSerialized.toTypedArray())
        val cargoSerialized = Cargo(
            CDACertPath.PRIVATE_GW.subjectPrivateAddress,
            cargoMessageSet.encrypt(
                privateGatewaySessionKeyPair.sessionKey,
                publicGatewaySessionKeyPair
            ),
            CDACertPath.PUBLIC_GW
        )
        return cargoSerialized.serialize(KeyPairSet.PUBLIC_GW.private)
    }
}
