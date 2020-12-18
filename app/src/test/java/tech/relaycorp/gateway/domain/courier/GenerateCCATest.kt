package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import tech.relaycorp.relaynet.testing.CertificationPath
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import java.time.Duration

class GenerateCCATest {

    private val publicGatewayPreferences = mock<PublicGatewayPreferences>()
    private val localConfig = mock<LocalConfig>()
    private val calculateCreationDate = mock<CalculateCRCMessageCreationDate>()
    private val generateCCA =
        GenerateCCA(publicGatewayPreferences, localConfig, calculateCreationDate)

    companion object {
        private const val ADDRESS = "http://example.org"
    }

    @BeforeEach
    internal fun setUp() {
        runBlocking {
            val keyPair = generateRSAKeyPair()
            val certificate = issueGatewayCertificate(
                subjectPublicKey = keyPair.public,
                issuerPrivateKey = keyPair.private,
                validityEndDate = nowInUtc().plusMinutes(1),
                validityStartDate = nowInUtc().minusDays(1)
            )

            whenever(localConfig.getKeyPair()).thenReturn(keyPair)
            whenever(localConfig.getCargoDeliveryAuth()).thenReturn(certificate)
            whenever(publicGatewayPreferences.getCogRPCAddress()).thenReturn(ADDRESS)
            whenever(publicGatewayPreferences.getCertificate())
                .thenReturn(CertificationPath.PUBLIC_GW)
        }
    }

    @Test
    internal fun `generate in ByteArray`() = runBlockingTest {
        val creationDate = nowInUtc()
        whenever(calculateCreationDate.calculate()).thenReturn(creationDate)

        val ccaBytes = generateCCA.generateByteArray()
        val cca = CargoCollectionAuthorization.deserialize(ccaBytes)

        cca.validate(null)
        assertEquals(ADDRESS, cca.recipientAddress)
        assertEquals(localConfig.getCargoDeliveryAuth(), cca.senderCertificate)
        assertTrue(Duration.between(creationDate, cca.creationDate).abs().seconds <= 1)
    }
}
