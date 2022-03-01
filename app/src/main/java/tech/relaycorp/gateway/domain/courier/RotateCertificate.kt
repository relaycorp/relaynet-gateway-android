package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.LocalConfig
import tech.relaycorp.gateway.domain.endpoint.NotifyEndpointsOfRenewCertificate
import tech.relaycorp.relaynet.messages.CertificateRotation
import tech.relaycorp.relaynet.messages.InvalidMessageException
import java.util.logging.Level
import javax.inject.Inject

class RotateCertificate @Inject constructor(
    private val localConfig: LocalConfig,
    private val publicGatewayPreferences: PublicGatewayPreferences,
    private val notifyEndpoints: NotifyEndpointsOfRenewCertificate
) {

    suspend operator fun invoke(certRotationSerialized: ByteArray) {
        val certRotation = try {
            CertificateRotation.deserialize(certRotationSerialized)
        } catch (e: InvalidMessageException) {
            logger.log(Level.WARNING, "Invalid CertificateRotation", e)
            return
        }

        val currentIdCert = localConfig.getIdentityCertificate()
        val newIdCert = certRotation.subjectCertificate

        if (currentIdCert.expiryDate >= newIdCert.expiryDate) return

        localConfig.setIdentityCertificate(newIdCert)
        certRotation.chain.first().let { publicGatewayCert ->
            publicGatewayPreferences.setCertificate(publicGatewayCert)
        }

        notifyEndpoints.notifyAll()
    }
}
