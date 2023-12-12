package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
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
import tech.relaycorp.relaynet.messages.CertificateRotation
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.messages.payloads.CargoMessageSet
import tech.relaycorp.relaynet.pki.CertificationPath
import tech.relaycorp.relaynet.testing.pki.CDACertPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath

class ProcessCargoTest : BaseDataTestCase() {

    private val cargoStorage = mock<CargoStorage>()
    private val storeParcel = mock<StoreParcel>()
    private val storeParcelCollection = mock<StoreParcelCollection>()
    private val deleteParcel = mock<DeleteParcel>()
    private val rotateCertificate = mock<RotateCertificate>()

    private val processCargo = ProcessCargo(
        cargoStorage,
        storeParcel,
        storeParcelCollection,
        deleteParcel,
        gatewayManagerProvider,
        rotateCertificate,
    )

    @BeforeEach
    fun setUp() = runBlockingTest { registerPrivateGatewaySessionKey() }

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
            eq(MessageAddress.of(pca.recipientEndpointId)),
            eq(MessageAddress.of(pca.senderEndpointId)),
            eq(MessageId(pca.parcelId)),
        )
    }

    @Test
    fun `store received parcel with collection`() = runBlockingTest {
        val parcel = ParcelFactory.build()
        val cargoSerialized = generateCargoFromMessages(
            listOf(parcel.serialize(KeyPairSet.PDA_GRANTEE.private)),
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
            listOf(parcel.serialize(KeyPairSet.PDA_GRANTEE.private)),
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
            listOf(parcel.serialize(KeyPairSet.PDA_GRANTEE.private)),
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
            listOf(parcel.serialize(KeyPairSet.PDA_GRANTEE.private)),
        )
        whenever(cargoStorage.list()).thenReturn(listOf(cargoSerialized::inputStream))
        whenever(storeParcel.store(any<ByteArray>(), any()))
            .thenReturn(StoreParcel.Result.InvalidParcel(ParcelFactory.build(), Exception()))

        processCargo.process()

        verify(storeParcel).store(any<ByteArray>(), any())
        verify(storeParcelCollection).storeForParcel(any())
    }

    @Test
    internal fun `rotates certificate when certificate rotation is received`() = runBlockingTest {
        val certRotation =
            CertificateRotation(CertificationPath(PDACertPath.PRIVATE_GW, emptyList()))
        val certRotationSerialized = certRotation.serialize()
        val cargoSerialized = generateCargoFromMessages(listOf(certRotationSerialized))
        whenever(cargoStorage.list()).thenReturn(listOf(cargoSerialized::inputStream))

        processCargo.process()

        verify(rotateCertificate).invoke(certRotationSerialized)
    }

    private fun generateCargoFromMessages(messagesSerialized: List<ByteArray>): ByteArray {
        val cargoMessageSet = CargoMessageSet(messagesSerialized.toTypedArray())
        val cargoSerialized = Cargo(
            Recipient(CDACertPath.PRIVATE_GW.subjectId),
            cargoMessageSet.encrypt(
                privateGatewaySessionKeyPair.sessionKey,
                internetGatewaySessionKeyPair,
            ),
            CDACertPath.INTERNET_GW,
        )
        return cargoSerialized.serialize(KeyPairSet.INTERNET_GW.private)
    }
}
