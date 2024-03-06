package tech.relaycorp.gateway.domain.courier

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.endpoint.GatewayCertificateChangeNotifier
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.messages.CertificateRotation
import tech.relaycorp.relaynet.pki.CertificationPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import java.time.ZonedDateTime

class RotateCertificateTest {

    private val localConfig = mock<LocalConfig>()
    private val internetGatewayPreferences = mock<InternetGatewayPreferences>()
    private val notifyEndpoints = mock<GatewayCertificateChangeNotifier>()

    private val rotateCertificate = RotateCertificate(
        localConfig,
        internetGatewayPreferences,
        notifyEndpoints,
    )

    @Test
    fun `rotate successfully`() = runTest {
        val newIdCertificate = issueGatewayCertificate(
            KeyPairSet.PRIVATE_GW.public,
            KeyPairSet.INTERNET_GW.private,
            ZonedDateTime.now().plusYears(10),
            validityStartDate = ZonedDateTime.now().minusDays(1),
        )
        val certificateRotation = CertificateRotation(
            CertificationPath(newIdCertificate, listOf(PDACertPath.INTERNET_GW)),
        )
        whenever(localConfig.getParcelDeliveryCertificate()).thenReturn(PDACertPath.PRIVATE_GW)

        rotateCertificate(certificateRotation.serialize())

        verify(localConfig).setParcelDeliveryCertificate(
            check { assertArrayEquals(newIdCertificate.serialize(), it.serialize()) },
            any(),
        )
        verify(internetGatewayPreferences).setPublicKey(PDACertPath.INTERNET_GW.subjectPublicKey)
    }

    @Test
    fun `does not save invalid certificate rotation`() = runBlockingTest {
        rotateCertificate("invalid".toByteArray())

        verify(localConfig, never()).setParcelDeliveryCertificate(any(), any())
        verify(internetGatewayPreferences, never()).setPublicKey(any())
        verify(notifyEndpoints, never()).notifyAll()
    }

    @Test
    fun `does not save certificate older than current`() = runBlockingTest {
        val newIdCertificate = issueGatewayCertificate(
            KeyPairSet.PRIVATE_GW.public,
            KeyPairSet.INTERNET_GW.private,
            PDACertPath.PRIVATE_GW.expiryDate.minusSeconds(1),
            PDACertPath.INTERNET_GW,
            validityStartDate = ZonedDateTime.now().minusDays(1),
        )
        val certificateRotation = CertificateRotation(
            CertificationPath(newIdCertificate, listOf(PDACertPath.INTERNET_GW)),
        )
        whenever(localConfig.getParcelDeliveryCertificate()).thenReturn(PDACertPath.PRIVATE_GW)

        rotateCertificate(certificateRotation.serialize())

        verify(localConfig, never()).setParcelDeliveryCertificate(any(), any())
        verify(internetGatewayPreferences, never()).setPublicKey(any())
        verify(notifyEndpoints, never()).notifyAll()
    }

    @Test
    fun `new certificate triggers notification`() = runBlockingTest {
        val oldCertificate = issueGatewayCertificate(
            KeyPairSet.PRIVATE_GW.public,
            KeyPairSet.INTERNET_GW.private,
            PDACertPath.PRIVATE_GW.expiryDate.minusSeconds(1),
            PDACertPath.INTERNET_GW,
            validityStartDate = ZonedDateTime.now().minusDays(1),
        )
        val certificateRotation = CertificateRotation(
            CertificationPath(PDACertPath.PRIVATE_GW, listOf(PDACertPath.INTERNET_GW)),
        )
        whenever(localConfig.getParcelDeliveryCertificate()).thenReturn(oldCertificate)

        rotateCertificate(certificateRotation.serialize())

        verify(notifyEndpoints, times(1)).notifyAll()
    }
}
