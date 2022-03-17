package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.disk.FileStore
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.test.BaseDataTestCase
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import tech.relaycorp.relaynet.testing.pki.CDACertPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import java.time.Duration

class GenerateCCATest : BaseDataTestCase() {

    private val publicGatewayPreferences = mock<PublicGatewayPreferences>()
    private val mockFileStore = mock<FileStore>()
    private val localConfig = LocalConfig(
        mockFileStore, privateKeyStoreProvider, certificateStoreProvider, publicGatewayPreferences
    )
    private val calculateCreationDate = mock<CalculateCRCMessageCreationDate>()

    private val generateCCA = GenerateCCA(
        publicGatewayPreferences,
        localConfig,
        calculateCreationDate,
        gatewayManagerProvider
    )

    companion object {
        private const val ADDRESS = "http://example.org"
    }

    @BeforeEach
    internal fun setUp() {
        runBlocking {
            registerPrivateGatewayIdentity()

            val keyPair = KeyPairSet.PRIVATE_GW
            val certificate = issueGatewayCertificate(
                subjectPublicKey = keyPair.public,
                issuerPrivateKey = keyPair.private,
                validityEndDate = nowInUtc().plusMinutes(1),
                validityStartDate = nowInUtc().minusDays(1)
            )
            whenever(mockFileStore.read(eq(LocalConfig.CDA_CERTIFICATE_FILE_NAME)))
                .thenReturn(certificate.serialize())

            whenever(publicGatewayPreferences.getPrivateAddress())
                .thenReturn(PDACertPath.PUBLIC_GW.subjectPrivateAddress)
            whenever(publicGatewayPreferences.getCogRPCAddress()).thenReturn(ADDRESS)
            whenever(publicGatewayPreferences.getCertificate())
                .thenReturn(CDACertPath.PUBLIC_GW)

            registerPublicGatewaySessionKey()
        }
    }

    @Test
    fun `generate in ByteArray`() = runBlockingTest {
        val creationDate = nowInUtc()
        whenever(calculateCreationDate.calculate()).thenReturn(creationDate)

        val ccaBytes = generateCCA.generateSerialized()
        val cca = CargoCollectionAuthorization.deserialize(ccaBytes)

        cca.validate(null)
        assertEquals(ADDRESS, cca.recipientAddress)
        assertEquals(PDACertPath.PRIVATE_GW, cca.senderCertificate)
        assertTrue(Duration.between(creationDate, cca.creationDate).abs().seconds <= 1)

        // Check it was encrypted with the public gateway's session key
        val ccr = cca.unwrapPayload(publicGatewaySessionKeyPair.privateKey).payload
        assertEquals(
            KeyPairSet.PUBLIC_GW.public,
            ccr.cargoDeliveryAuthorization.subjectPublicKey
        )
    }
}
