package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.endpoint.GatewayCertificateChangeNotifier
import tech.relaycorp.relaynet.messages.CertificateRotation
import tech.relaycorp.relaynet.messages.InvalidMessageException
import java.util.logging.Level
import javax.inject.Inject

class RotateCertificate @Inject constructor(
    private val localConfig: LocalConfig,
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val notifyEndpointsChangeNotifier: GatewayCertificateChangeNotifier
) {

    suspend operator fun invoke(certRotationSerialized: ByteArray) {
        val certRotation = try {
            CertificateRotation.deserialize(certRotationSerialized)
        } catch (e: InvalidMessageException) {
            logger.log(Level.WARNING, "Invalid CertificateRotation", e)
            return
        }

        val currentIdCert = localConfig.getIdentityCertificate()
        val newIdCert = certRotation.certificationPath.leafCertificate

        if (currentIdCert.expiryDate >= newIdCert.expiryDate) return

        localConfig.setIdentityCertificate(newIdCert)
        certRotation.certificationPath.certificateAuthorities.first().let { publicGatewayCert ->
            publicGatewayPreferences.setPublicKey(publicGatewayCert.subjectPublicKey)
        }

        notifyEndpointsChangeNotifier.notifyAll()
    }
}
