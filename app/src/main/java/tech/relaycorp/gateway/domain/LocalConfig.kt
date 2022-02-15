package tech.relaycorp.gateway.domain

import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.gateway.common.toPublicKey
import tech.relaycorp.gateway.data.disk.FileStore
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
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.toJavaDuration

class LocalConfig
@Inject constructor(
    private val fileStore: FileStore,
    private val privateKeyStore: Provider<PrivateKeyStore>,
    private val certificateStore: Provider<CertificateStore>
) {
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
        certificateStore.get().retrieveLatest(it.privateAddress)
            ?: CertificationPath(generateIdentityCertificate(it), emptyList())
    }

    suspend fun setIdentityCertificate(
        leafCertificate: Certificate,
        certificateChain: List<Certificate> = emptyList()
    ) {
        certificateStore.get().save(leafCertificate, certificateChain)
    }

    private suspend fun generateIdentityCertificate(privateKey: PrivateKey): Certificate {
        val certificate = selfIssueCargoDeliveryAuth(privateKey, privateKey.toPublicKey())
        setIdentityCertificate(certificate)
        return certificate
    }

    @Synchronized
    suspend fun bootstrap() {
        try {
            getIdentityKey()
        } catch (_: RuntimeException) {
            val keyPair = generateIdentityKeyPair()
            generateIdentityCertificate(keyPair.private)
        }

        try {
            getCargoDeliveryAuth()
        } catch (_: RuntimeException) {
            generateCargoDeliveryAuth()
        }
    }

    suspend fun getCargoDeliveryAuth() =
        fileStore.read(CDA_CERTIFICATE_FILE_NAME)
            ?.let { Certificate.deserialize(it) }
            ?: throw RuntimeException("No CDA issuer was found")

    private fun selfIssueCargoDeliveryAuth(
        privateKey: PrivateKey,
        publicKey: PublicKey
    ): Certificate {
        return issueGatewayCertificate(
            subjectPublicKey = publicKey,
            issuerPrivateKey = privateKey,
            validityStartDate = nowInUtc()
                .minus(CalculateCRCMessageCreationDate.CLOCK_DRIFT_TOLERANCE.toJavaDuration()),
            validityEndDate = nowInUtc().plusYears(1)
        )
    }

    private suspend fun generateCargoDeliveryAuth() {
        val key = getIdentityKey()
        val certificate = getIdentityCertificate()
        val cda = selfIssueCargoDeliveryAuth(key, certificate.subjectPublicKey)
        fileStore.store(CDA_CERTIFICATE_FILE_NAME, cda.serialize())
    }

    // Helpers

    companion object {
        internal const val CDA_CERTIFICATE_FILE_NAME = "cda_local_gateway.certificate"
    }
}
