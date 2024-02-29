package tech.relaycorp.gateway.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.relaycorp.gateway.common.Logging.logger
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.common.toPublicKey
import tech.relaycorp.gateway.data.preference.InternetGatewayPreferences
import tech.relaycorp.gateway.domain.courier.CalculateCRCMessageCreationDate
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.keystores.CertificateStore
import tech.relaycorp.relaynet.keystores.PrivateKeyStore
import tech.relaycorp.relaynet.pki.CertificationPath
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import tech.relaycorp.relaynet.wrappers.nodeId
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

class LocalConfig
@Inject constructor(
    private val privateKeyStore: Provider<PrivateKeyStore>,
    private val certificateStore: Provider<CertificateStore>,
    private val internetGatewayPreferences: InternetGatewayPreferences,
) {
    private val mutex = Mutex()

    // Private Gateway Key Pair

    suspend fun getIdentityKey(): PrivateKey =
        privateKeyStore.get().retrieveAllIdentityKeys().firstOrNull()
            ?: throw RuntimeException("No key pair was found")

    private suspend fun generateIdentityKeyPair(): KeyPair {
        val keyPair = generateRSAKeyPair()
        privateKeyStore.get().saveIdentityKey(keyPair.private)
        return keyPair
    }

    // Private Gateway Certificate

    suspend fun getIdentityCertificate(): Certificate =
        getIdentityCertificationPath().leafCertificate

    private suspend fun getIdentityCertificationPath(): CertificationPath = getIdentityKey().let {
        getInternetGatewayId()?.let { internetGatewayId ->
            certificateStore.get()
                .retrieveLatest(it.nodeId, internetGatewayId)
        }
            ?: CertificationPath(generateIdentityCertificate(it), emptyList())
    }

    suspend fun getAllValidIdentityCertificates(): List<Certificate> =
        getAllValidIdentityCertificationPaths().map { it.leafCertificate }

    private suspend fun getAllValidIdentityCertificationPaths(): List<CertificationPath> =
        getInternetGatewayId()?.let { internetGatewayId ->
            certificateStore.get()
                .retrieveAll(getIdentityKey().nodeId, internetGatewayId)
        }.orEmpty()

    suspend fun setIdentityCertificate(
        leafCertificate: Certificate,
        certificateChain: List<Certificate> = emptyList(),
    ) {
        getInternetGatewayId()?.let { internetGatewayId ->
            certificateStore.get()
                .save(
                    CertificationPath(leafCertificate, certificateChain),
                    internetGatewayId,
                )
        } ?: logger.severe(
            "Will not save identity certificate because the internet gateway is not registered yet",
        )
    }

    private suspend fun generateIdentityCertificate(privateKey: PrivateKey): Certificate {
        val certificate = selfIssueCargoDeliveryAuth(privateKey, privateKey.toPublicKey())
        setIdentityCertificate(certificate)
        return certificate
    }

    suspend fun bootstrap() {
        mutex.withLock {
            try {
                getIdentityKey()
            } catch (_: RuntimeException) {
                val keyPair = generateIdentityKeyPair()
                generateIdentityCertificate(keyPair.private)
            }

            getCargoDeliveryAuth() // Generates new CDA if non-existent
        }
    }

    suspend fun getCargoDeliveryAuth() = certificateStore.get()
        .retrieveLatest(
            getIdentityKey().nodeId,
            getIdentityCertificate().subjectId,
        )
        ?.leafCertificate
        .let { storedCertificate ->
            if (storedCertificate?.isExpiringSoon() == false) {
                storedCertificate
            } else {
                generateCargoDeliveryAuth()
            }
        }

    suspend fun getAllValidCargoDeliveryAuth() = certificateStore.get()
        .retrieveAll(
            getIdentityKey().nodeId,
            getIdentityCertificate().subjectId,
        )
        .map { it.leafCertificate }

    private fun selfIssueCargoDeliveryAuth(
        privateKey: PrivateKey,
        publicKey: PublicKey,
    ): Certificate {
        return issueGatewayCertificate(
            subjectPublicKey = publicKey,
            issuerPrivateKey = privateKey,
            validityStartDate = nowInUtc().minus(
                CalculateCRCMessageCreationDate.CLOCK_DRIFT_TOLERANCE.toJavaDuration(),
            ),
            validityEndDate = nowInUtc().plusMonths(6),
        )
    }

    private suspend fun generateCargoDeliveryAuth(): Certificate {
        val key = getIdentityKey()
        val certificate = getIdentityCertificate()
        val cda = selfIssueCargoDeliveryAuth(key, certificate.subjectPublicKey)
        certificateStore.get()
            .save(CertificationPath(cda, emptyList()), certificate.subjectId)
        return cda
    }

    suspend fun deleteExpiredCertificates() {
        certificateStore.get().deleteExpired()
    }

    suspend fun getInternetGatewayAddress() = internetGatewayPreferences.getAddress()

    private suspend fun getInternetGatewayId() = internetGatewayPreferences.getId()

    private fun Certificate.isExpiringSoon() =
        expiryDate < (nowInUtc().plusNanos(CERTIFICATE_EXPIRING_THRESHOLD.inWholeNanoseconds))

    companion object {
        private val CERTIFICATE_EXPIRING_THRESHOLD = 90.days
    }
}
