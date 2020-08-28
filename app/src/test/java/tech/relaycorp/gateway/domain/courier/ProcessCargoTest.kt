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
import tech.relaycorp.gateway.test.factory.CargoFactory
import tech.relaycorp.gateway.test.factory.ParcelCollectionAckFactory
import tech.relaycorp.gateway.test.factory.ParcelFactory
import tech.relaycorp.relaynet.messages.payloads.CargoMessage

class ProcessCargoTest {

    private val cargoStorage = mock<CargoStorage>()
    private val readMessagesFromCargo = mock<ReadMessagesFromCargo>()
    private val storeParcel = mock<StoreParcel>()
    private val storeParcelCollection = mock<StoreParcelCollection>()
    private val deleteParcel = mock<DeleteParcel>()
    private val processCargo = ProcessCargo(
        cargoStorage, readMessagesFromCargo, storeParcel, storeParcelCollection, deleteParcel
    )

    @Test
    internal fun `deletes all cargo at the end`() = runBlockingTest {
        whenever(cargoStorage.list()).thenReturn(emptyList())
        processCargo.process()
        verify(cargoStorage).deleteAll()
    }

    @Test
    internal fun `deletes parcel when its ack is received`() = runBlockingTest {
        val cargoBytes = CargoFactory.buildSerialized()
        whenever(cargoStorage.list()).thenReturn(listOf(cargoBytes::inputStream))
        val pca = ParcelCollectionAckFactory.build()
        val cargoMessage = mockCargoMessage(CargoMessage.Type.PCA, pca.serialize())
        whenever(readMessagesFromCargo.read(any())).thenReturn(sequenceOf(cargoMessage))

        processCargo.process()

        verify(deleteParcel).delete(
            eq(MessageAddress.of(pca.recipientEndpointAddress)),
            eq(MessageAddress.of(pca.senderEndpointPrivateAddress)),
            eq(MessageId(pca.parcelId))
        )
    }

    @Test
    fun `store received parcel with collection`() = runBlockingTest {
        whenever(cargoStorage.list())
            .thenReturn(listOf(CargoFactory.buildSerialized()::inputStream))
        val parcel = ParcelFactory.build()
        val cargoMessage = mockCargoMessage(CargoMessage.Type.PARCEL)
        whenever(readMessagesFromCargo.read(any())).thenReturn(sequenceOf(cargoMessage))
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.Success(parcel))

        processCargo.process()

        verify(storeParcel).store(any<ByteArray>(), eq(RecipientLocation.LocalEndpoint))
        verify(storeParcelCollection).storeForParcel(eq(parcel))
    }

    @Test
    fun `received malformed parcel that does not get stored`() = runBlockingTest {
        whenever(cargoStorage.list())
            .thenReturn(listOf(CargoFactory.buildSerialized()::inputStream))
        val cargoMessage = mockCargoMessage(CargoMessage.Type.PARCEL)
        whenever(readMessagesFromCargo.read(any())).thenReturn(sequenceOf(cargoMessage))
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.MalformedParcel(Exception()))

        processCargo.process()

        verify(storeParcel).store(any<ByteArray>(), any())
        verify(storeParcelCollection, never()).storeForParcel(any())
    }

    @Test
    fun `received duplicated parcel that does not get stored`() = runBlockingTest {
        whenever(cargoStorage.list())
            .thenReturn(listOf(CargoFactory.buildSerialized()::inputStream))
        val cargoMessage = mockCargoMessage(CargoMessage.Type.PARCEL)
        whenever(readMessagesFromCargo.read(any())).thenReturn(sequenceOf(cargoMessage))
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.DuplicatedParcel(ParcelFactory.build()))

        processCargo.process()

        verify(storeParcel).store(any<ByteArray>(), any())
        verify(storeParcelCollection, never()).storeForParcel(any())
    }

    @Test
    fun `received invalid parcel but collection is stored`() = runBlockingTest {
        whenever(cargoStorage.list())
            .thenReturn(listOf(CargoFactory.buildSerialized()::inputStream))
        val cargoMessage = mockCargoMessage(CargoMessage.Type.PARCEL)
        whenever(readMessagesFromCargo.read(any())).thenReturn(sequenceOf(cargoMessage))
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.InvalidParcel(ParcelFactory.build(), Exception()))

        processCargo.process()

        verify(storeParcel).store(any<ByteArray>(), any())
        verify(storeParcelCollection).storeForParcel(any())
    }

    private fun mockCargoMessage(
        type: CargoMessage.Type,
        messageSerialized: ByteArray = ByteArray(0)
    ): CargoMessage {
        val cargoMessage = mock<CargoMessage>()
        whenever(cargoMessage.type).thenReturn(type)
        whenever(cargoMessage.messageSerialized).thenReturn(messageSerialized)
        return cargoMessage
    }
}
