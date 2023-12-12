package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.test.BaseDataTestCase
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import tech.relaycorp.relaynet.pki.CertificationPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import tech.relaycorp.relaynet.wrappers.nodeId
import java.time.Duration

class GenerateCCATest : BaseDataTestCase() {

    private val internetGatewayPreferences = mock<InternetGatewayPreferences>()
    private val localConfig = LocalConfig(
        privateKeyStoreProvider,
        certificateStoreProvider,
        internetGatewayPreferences,
    )
    private val calculateCreationDate = mock<CalculateCRCMessageCreationDate>()

    private val generateCCA = GenerateCCA(
        internetGatewayPreferences,
        localConfig,
        calculateCreationDate,
        gatewayManagerProvider,
    )

    companion object {
        private const val ADDRESS = "example.org"
    }

    @BeforeEach
    internal fun setUp() {
        runBlocking {
            registerPrivateGatewayIdentity()

            val keyPair = KeyPairSet.PRIVATE_GW
            val cda = issueGatewayCertificate(
                subjectPublicKey = keyPair.public,
                issuerPrivateKey = keyPair.private,
                validityEndDate = nowInUtc().plusMinutes(1),
                validityStartDate = nowInUtc().minusDays(1),
            )
            whenever(certificateStore.retrieveLatest(any(), eq(keyPair.public.nodeId)))
                .thenReturn(CertificationPath(cda, emptyList()))

            whenever(internetGatewayPreferences.getId())
                .thenReturn(PDACertPath.INTERNET_GW.subjectId)
            whenever(internetGatewayPreferences.getAddress()).thenReturn(ADDRESS)
            whenever(internetGatewayPreferences.getPublicKey())
                .thenReturn(KeyPairSet.INTERNET_GW.public)

            registerInternetGatewaySessionKey()
        }
    }

    @Test
    fun `generate in ByteArray`() = runBlockingTest {
        val creationDate = nowInUtc()
        whenever(calculateCreationDate.calculate()).thenReturn(creationDate)

        val ccaBytes = generateCCA.generateSerialized()
        val cca = CargoCollectionAuthorization.deserialize(ccaBytes)

        cca.validate(null)
        assertEquals(ADDRESS, cca.recipient.internetAddress)
        assertArrayEquals(PDACertPath.PRIVATE_GW.serialize(), cca.senderCertificate.serialize())
        assertTrue(Duration.between(creationDate, cca.creationDate).abs().seconds <= 1)

        // Check it was encrypted with the public gateway's session key
        val ccr = cca.unwrapPayload(internetGatewaySessionKeyPair.privateKey).payload
        assertEquals(
            KeyPairSet.INTERNET_GW.public,
            ccr.cargoDeliveryAuthorization.subjectPublicKey,
        )
    }
}
