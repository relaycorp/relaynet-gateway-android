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
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair

class GenerateCCATest {

    private val publicGatewayPreferences = mock<PublicGatewayPreferences>()
    private val localConfig = mock<LocalConfig>()
    private val generateCCA = GenerateCCA(publicGatewayPreferences, localConfig)

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
            whenever(localConfig.getCertificate()).thenReturn(certificate)
            whenever(publicGatewayPreferences.getCogRPCAddress()).thenReturn(ADDRESS)
        }
    }

    @Test
    internal fun `generate in ByteArray`() = runBlockingTest {
        val ccaBytes = generateCCA.generateByteArray()
        val cca = CargoCollectionAuthorization.deserialize(ccaBytes)

        cca.validate(null)
        assertEquals(ADDRESS, cca.recipientAddress)
        assertEquals(localConfig.getCertificate(), cca.senderCertificate)
    }

    @Test
    internal fun `generate with creation date 5 minutes past if registration is before`() =
        runBlockingTest {
            val certificate = issueGatewayCertificate(
                subjectPublicKey = localConfig.getKeyPair().public,
                issuerPrivateKey = localConfig.getKeyPair().private,
                validityEndDate = nowInUtc().plusMinutes(1),
                validityStartDate = nowInUtc().minusDays(1)
            )
            whenever(localConfig.getCertificate()).thenReturn(certificate)

            val ccaBytes = generateCCA.generateByteArray()
            val cca = CargoCollectionAuthorization.deserialize(ccaBytes)

            assertTrue(
                cca.creationDate.isBefore(nowInUtc().minusMinutes(4)) &&
                    cca.creationDate.isAfter(nowInUtc().minusMinutes(6))
            )
        }

    @Test
    internal fun `generate with creation date equal to registration if more recent`() =
        runBlockingTest {
            val certificate = issueGatewayCertificate(
                subjectPublicKey = localConfig.getKeyPair().public,
                issuerPrivateKey = localConfig.getKeyPair().private,
                validityEndDate = nowInUtc().plusMinutes(1),
                validityStartDate = nowInUtc()
            )
            whenever(localConfig.getCertificate()).thenReturn(certificate)

            val ccaBytes = generateCCA.generateByteArray()
            val cca = CargoCollectionAuthorization.deserialize(ccaBytes)

            assertTrue(
                certificate.startDate.isEqual(cca.creationDate)
            )
        }
}
