package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.messages.CertificateRotation
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import java.time.ZonedDateTime

class RotateCertificateTest {

    private val localConfig = mock<LocalConfig>()
    private val publicGatewayPreferences = mock<PublicGatewayPreferences>()
    private val rotateCertificate = RotateCertificate(
        localConfig, publicGatewayPreferences
    )

    @Test
    fun `rotate successfully`() = runBlockingTest {
        val newIdCertificate = issueGatewayCertificate(
            KeyPairSet.PRIVATE_GW.public,
            KeyPairSet.PUBLIC_GW.private,
            ZonedDateTime.now().plusYears(10),
            validityStartDate = ZonedDateTime.now().minusDays(1)
        )
        val certificateRotation = CertificateRotation(
            newIdCertificate, listOf(PDACertPath.PUBLIC_GW)
        )
        whenever(localConfig.getIdentityCertificate()).thenReturn(PDACertPath.PRIVATE_GW)

        rotateCertificate(certificateRotation.serialize())

        verify(localConfig).setIdentityCertificate(
            check { assertArrayEquals(newIdCertificate.serialize(), it.serialize()) },
            any()
        )
        verify(publicGatewayPreferences).setCertificate(PDACertPath.PUBLIC_GW)
    }

    @Test
    fun `does not save invalid certificate rotation`() = runBlockingTest {
        rotateCertificate("invalid".toByteArray())

        verify(localConfig, never()).setIdentityCertificate(any(), any())
        verify(publicGatewayPreferences, never()).setCertificate(any())
    }

    @Test
    fun `does not save certificate older than current`() = runBlockingTest {
        val newIdCertificate = issueGatewayCertificate(
            KeyPairSet.PRIVATE_GW.public,
            KeyPairSet.PUBLIC_GW.private,
            PDACertPath.PRIVATE_GW.expiryDate.minusSeconds(1),
            PDACertPath.PUBLIC_GW,
            validityStartDate = ZonedDateTime.now().minusDays(1)
        )
        val certificateRotation = CertificateRotation(
            newIdCertificate, listOf(PDACertPath.PUBLIC_GW)
        )
        whenever(localConfig.getIdentityCertificate()).thenReturn(PDACertPath.PRIVATE_GW)

        rotateCertificate(certificateRotation.serialize())

        verify(localConfig, never()).setIdentityCertificate(any(), any())
        verify(publicGatewayPreferences, never()).setCertificate(any())
    }
}
