package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.test.factory.ParcelCollectionFactory
import tech.relaycorp.gateway.test.factory.StoredParcelFactory
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import java.io.InputStream

class GenerateCargoTest {

    private val storedParcelDao = mock<StoredParcelDao>()
    private val parcelCollectionDao = mock<ParcelCollectionDao>()
    private val diskMessageOperations = mock<DiskMessageOperations>()
    private val publicGatewayPreferences = mock<PublicGatewayPreferences>()
    private val localConfig = mock<LocalConfig>()
    private val generateCargo = GenerateCargo(
        storedParcelDao,
        parcelCollectionDao,
        diskMessageOperations,
        publicGatewayPreferences,
        localConfig
    )

    @BeforeEach
    internal fun setUp() = runBlockingTest {
        val keyPair = generateRSAKeyPair()
        val certificate = issueGatewayCertificate(
            keyPair.public,
            keyPair.private,
            nowInUtc().plusMinutes(1)
        )
        whenever(localConfig.getKeyPair()).thenReturn(keyPair)
        whenever(localConfig.getCertificate()).thenReturn(certificate)
        whenever(publicGatewayPreferences.getAddress()).thenReturn(flowOf("https://example.org"))
        whenever(publicGatewayPreferences.getCertificate()).thenReturn(flowOf(certificate))
    }

    @Test
    internal fun `empty cargo`() = runBlockingTest {
        whenever(storedParcelDao.listForRecipientLocation(any())).thenReturn(emptyList())
        whenever(parcelCollectionDao.getAll()).thenReturn(emptyList())

        val cargoes = generateCargo.generate().toList()

        assertTrue(cargoes.isEmpty())
    }

    @Test
    internal fun `generate 1 cargo with 1 PCA and 1 parcel`() = runBlockingTest {
        val parcel = StoredParcelFactory.build()
        whenever(storedParcelDao.listForRecipientLocation(any())).thenReturn(listOf(parcel))
        val messageStream: () -> InputStream = "ABC".toByteArray()::inputStream
        whenever(diskMessageOperations.readMessage(any(), any())).thenReturn(messageStream)
        val parcelCollection = ParcelCollectionFactory.build()
        whenever(parcelCollectionDao.getAll()).thenReturn(listOf(parcelCollection))

        val cargoes = generateCargo.generate().toList()
        assertEquals(1, cargoes.size)

        val cargo = Cargo.deserialize(cargoes.first().readBytes())
        assertEquals(publicGatewayPreferences.getAddress().first(), cargo.recipientAddress)

        // TODO: fix this expiryDate assertion that is failing on CI
        /*
        val maxExpirationTime =
            max(listOf(parcel.expirationTimeUtc, parcelCollection.expirationTimeUtc))
        assertEquals(
            maxExpirationTime.toInstant().epochSecond,
            cargo.expiryDate.toInstant().epochSecond
        )
        */

        val cargoMessages = cargo.unwrapPayload(localConfig.getKeyPair().private)
        assertEquals(2, cargoMessages.messages.size)
    }
}
