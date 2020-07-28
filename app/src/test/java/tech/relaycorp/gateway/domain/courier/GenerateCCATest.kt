package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    internal fun generateByteArray() = runBlockingTest {
        val keyPair = generateRSAKeyPair()
        val certificate = issueGatewayCertificate(
            subjectPublicKey = keyPair.public,
            issuerPrivateKey = keyPair.private,
            validityEndDate = nowInUtc().plusMinutes(1),
            validityStartDate = nowInUtc().minusMinutes(1)
        )
        val address = "http://example.org"
        whenever(localConfig.getKeyPair()).thenReturn(keyPair)
        whenever(localConfig.getCertificate()).thenReturn(certificate)
        whenever(publicGatewayPreferences.getAddress()).thenReturn(flowOf(address))

        val ccaBytes = generateCCA.generateByteArray()
        val cca = CargoCollectionAuthorization.deserialize(ccaBytes)

        cca.validate()
        assertEquals(address, cca.recipientAddress)
        assertEquals(certificate, cca.senderCertificate)
    }
}
