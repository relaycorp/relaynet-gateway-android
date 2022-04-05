package tech.relaycorp.gateway.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.common.toPublicKey
import tech.relaycorp.gateway.data.preference.PublicGatewayPreferences
import tech.relaycorp.gateway.domain.courier.CalculateCRCMessageCreationDate
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.keystores.CertificateStore
import tech.relaycorp.relaynet.keystores.CertificationPath
import tech.relaycorp.relaynet.keystores.PrivateKeyStore
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import tech.relaycorp.relaynet.wrappers.privateAddress
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.days
import kotlin.time.toJavaDuration

class LocalConfig
@Inject constructor(
    private val privateKeyStore: Provider<PrivateKeyStore>,
    private val certificateStore: Provider<CertificateStore>,
    private val publicGatewayPreferences: PublicGatewayPreferences
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
        certificateStore.get()
            .retrieveLatest(it.privateAddress, getPublicGatewayPrivateAddress())
            ?: CertificationPath(generateIdentityCertificate(it), emptyList())
    }

    suspend fun getAllValidIdentityCertificates(): List<Certificate> =
        getAllValidIdentityCertificationPaths().map { it.leafCertificate }

    private suspend fun getAllValidIdentityCertificationPaths(): List<CertificationPath> =
        certificateStore.get()
            .retrieveAll(getIdentityKey().privateAddress, getPublicGatewayPrivateAddress())

    suspend fun setIdentityCertificate(
        leafCertificate: Certificate,
        certificateChain: List<Certificate> = emptyList()
    ) {
        certificateStore.get()
            .save(leafCertificate, certificateChain, getPublicGatewayPrivateAddress())
    }

    private suspend fun generateIdentityCertificate(privateKey: PrivateKey): Certificate {
        val certificate = selfIssueCargoDeliveryAuth(
            privateKey, privateKey.toPublicKey(), nowInUtc().plusYears(1)
        )
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

    suspend fun getCargoDeliveryAuth() =
        certificateStore.get()
            .retrieveLatest(
                getIdentityKey().privateAddress,
                getIdentityCertificate().subjectPrivateAddress
            )
            ?.leafCertificate
            .let { storedCertificate ->
                if (storedCertificate?.isExpiringSoon() == false) {
                    storedCertificate
                } else {
                    generateCargoDeliveryAuth()
                }
            }

    suspend fun getAllValidCargoDeliveryAuth() =
        certificateStore.get()
            .retrieveAll(
                getIdentityKey().privateAddress,
                getIdentityCertificate().subjectPrivateAddress
            )
            .map { it.leafCertificate }

    private fun selfIssueCargoDeliveryAuth(
        privateKey: PrivateKey,
        publicKey: PublicKey,
        expiryDate: ZonedDateTime
    ): Certificate {
        return issueGatewayCertificate(
            subjectPublicKey = publicKey,
            issuerPrivateKey = privateKey,
            validityStartDate = nowInUtc()
                .minus(CalculateCRCMessageCreationDate.CLOCK_DRIFT_TOLERANCE.toJavaDuration()),
            validityEndDate = expiryDate
        )
    }

    private suspend fun generateCargoDeliveryAuth(): Certificate {
        val key = getIdentityKey()
        val certificate = getIdentityCertificate()
        val cda = selfIssueCargoDeliveryAuth(
            key, certificate.subjectPublicKey, certificate.expiryDate
        )
        certificateStore.get().save(cda, emptyList(), certificate.subjectPrivateAddress)
        return cda
    }

    suspend fun deleteExpiredCertificates() {
        certificateStore.get().deleteExpired()
    }

    private suspend fun getPublicGatewayPrivateAddress() =
        publicGatewayPreferences.getPrivateAddress()

    private fun Certificate.isExpiringSoon() =
        expiryDate < (nowInUtc().plusNanos(CERTIFICATE_EXPIRING_THRESHOLD.inWholeNanoseconds))

    companion object {
        private val CERTIFICATE_EXPIRING_THRESHOLD = 1.days
    }
}
