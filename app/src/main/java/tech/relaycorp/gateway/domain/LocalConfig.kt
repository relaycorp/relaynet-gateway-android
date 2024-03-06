package tech.relaycorp.gateway.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    // Bootstrap

    suspend fun bootstrap() {
        mutex.withLock {
            try {
                getIdentityKey()
            } catch (_: RuntimeException) {
                generateIdentityKeyPair()
            }

            getCargoDeliveryAuth() // Generates new CDA if non-existent
        }
    }

    // Private Gateway Key Pair

    suspend fun getIdentityKey(): PrivateKey =
        privateKeyStore.get().retrieveAllIdentityKeys().firstOrNull()
            ?: throw RuntimeException("No key pair was found")

    private suspend fun generateIdentityKeyPair(): KeyPair {
        val keyPair = generateRSAKeyPair()
        privateKeyStore.get().saveIdentityKey(keyPair.private)
        return keyPair
    }

    // Parcel Delivery

    suspend fun getParcelDeliveryCertificate(): Certificate? =
        getParcelDeliveryCertificationPath()?.leafCertificate

    private suspend fun getParcelDeliveryCertificationPath(): CertificationPath? =
        getIdentityKey().let {
            certificateStore.get()
                .retrieveLatest(it.nodeId, getInternetGatewayId())
        }

    suspend fun getAllValidParcelDeliveryCertificates(): List<Certificate> =
        getAllValidParcelDeliveryCertificationPaths().map { it.leafCertificate }

    private suspend fun getAllValidParcelDeliveryCertificationPaths(): List<CertificationPath> =
        certificateStore.get()
            .retrieveAll(getIdentityKey().nodeId, getInternetGatewayId())

    suspend fun setParcelDeliveryCertificate(
        leafCertificate: Certificate,
        certificateChain: List<Certificate> = emptyList(),
    ) {
        certificateStore.get()
            .save(
                CertificationPath(leafCertificate, certificateChain),
                getInternetGatewayId(),
            )
    }

    // Cargo Delivery

    suspend fun getCargoDeliveryAuth() = getIdentityKey().nodeId.let { nodeId ->
        certificateStore.get().retrieveLatest(nodeId, nodeId)
    }
        ?.leafCertificate
        .let { storedCertificate ->
            if (storedCertificate?.isExpiringSoon() == false) {
                storedCertificate
            } else {
                generateCargoDeliveryAuth()
            }
        }

    suspend fun getAllValidCargoDeliveryAuth() = getIdentityKey().nodeId.let { nodeId ->
        certificateStore.get()
            .retrieveAll(nodeId, nodeId)
            .map { it.leafCertificate }
    }

    private suspend fun generateCargoDeliveryAuth(): Certificate {
        val privateKey = getIdentityKey()
        val publicKey = privateKey.toPublicKey()
        val cda = selfIssueCargoDeliveryAuth(privateKey, publicKey)
        certificateStore.get()
            .save(CertificationPath(cda, emptyList()), publicKey.nodeId)
        return cda
    }

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

    // Maintenance

    suspend fun deleteExpiredCertificates() {
        certificateStore.get().deleteExpired()
    }

    private fun Certificate.isExpiringSoon() =
        expiryDate < (nowInUtc().plusNanos(CERTIFICATE_EXPIRING_THRESHOLD.inWholeNanoseconds))

    // Helpers

    suspend fun getInternetGatewayAddress() = internetGatewayPreferences.getAddress()

    private suspend fun getInternetGatewayId() = internetGatewayPreferences.getId()

    companion object {
        private val CERTIFICATE_EXPIRING_THRESHOLD = 90.days
    }
}
