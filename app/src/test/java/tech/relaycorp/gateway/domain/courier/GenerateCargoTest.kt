package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.database.ParcelCollectionDao
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.disk.DiskMessageOperations
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.test.BaseDataTestCase
import tech.relaycorp.gateway.test.factory.ParcelCollectionFactory
import tech.relaycorp.gateway.test.factory.StoredParcelFactory
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import java.io.InputStream
import java.time.Duration

class GenerateCargoTest : BaseDataTestCase() {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val parcelCollectionDao = mock<ParcelCollectionDao>()
    private val diskMessageOperations = mock<DiskMessageOperations>()
    private val internetGatewayPreferences = mock<InternetGatewayPreferences>()
    private val localConfig = LocalConfig(
        privateKeyStoreProvider, certificateStoreProvider, internetGatewayPreferences
    )
    private val calculateCRCMessageCreationDate = mock<CalculateCRCMessageCreationDate>()
    private val generateCargo = GenerateCargo(
        storedParcelDao,
        parcelCollectionDao,
        diskMessageOperations,
        internetGatewayPreferences,
        localConfig,
        calculateCRCMessageCreationDate,
        gatewayManagerProvider
    )

    @BeforeEach
    internal fun setUp() = runBlockingTest {
        registerPrivateGatewayIdentity()
        whenever(internetGatewayPreferences.getId())
            .thenReturn(PDACertPath.INTERNET_GW.subjectId)
        whenever(internetGatewayPreferences.getAddress())
            .thenReturn("example.org")
        whenever(internetGatewayPreferences.getPublicKey())
            .thenReturn(KeyPairSet.INTERNET_GW.public)
        whenever(calculateCRCMessageCreationDate.calculate())
            .thenReturn(nowInUtc())

        val messageStream: () -> InputStream = "ABC".toByteArray()::inputStream
        whenever(diskMessageOperations.readMessage(any(), any())).thenReturn(messageStream)

        registerInternetGatewaySessionKey()
    }

    @Test
    internal fun `empty cargo`() = runBlockingTest {
        whenever(storedParcelDao.listForRecipientLocation(any(), any())).thenReturn(emptyList())
        whenever(parcelCollectionDao.getAll()).thenReturn(emptyList())

        val cargoes = generateCargo.generate().toList()

        assertTrue(cargoes.isEmpty())
    }

    @Test
    fun `generate 1 cargo with 1 PCA and 1 parcel`() = runBlockingTest {
        val parcel = StoredParcelFactory.build()
            .copy(expirationTimeUtc = nowInUtc().plusDays(1))
        whenever(storedParcelDao.listForRecipientLocation(any(), any())).thenReturn(listOf(parcel))
        val parcelCollection = ParcelCollectionFactory.build()
            .copy(expirationTimeUtc = nowInUtc())
        whenever(parcelCollectionDao.getAll()).thenReturn(listOf(parcelCollection))
        val creationDate = nowInUtc()
        whenever(calculateCRCMessageCreationDate.calculate()).thenReturn(creationDate)

        val cargoes = generateCargo.generate().toList()
        assertEquals(1, cargoes.size)

        val cargo = Cargo.deserialize(cargoes.first().readBytes())
        assertEquals(
            internetGatewayPreferences.getAddress(),
            cargo.recipient.internetAddress
        )
        assertTrue(Duration.between(parcel.expirationTimeUtc, cargo.expiryDate).abs().seconds <= 2)

        val cargoMessages = cargo.unwrapPayload(internetGatewaySessionKeyPair.privateKey).payload
        assertEquals(2, cargoMessages.messages.size)
        assertTrue(Duration.between(creationDate, cargo.creationDate).abs().seconds <= 1)
    }
}
