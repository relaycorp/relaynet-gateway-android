package tech.relaycorp.gateway.pdc.local

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.selector.X509CertificateHolderSelector
import org.bouncycastle.cms.CMSException
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.util.Selector
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.io.IOException

internal data class SignatureVerification(
    val signerCertificate: Certificate
)

internal object Handshake {
    @Throws(InvalidHandshakeSignatureException::class)
    internal fun verifySignature(
        cmsSignedData: ByteArray,
        expectedPlaintext: ByteArray
    ): SignatureVerification {
        val signedData = parseCmsSignedData(cmsSignedData, expectedPlaintext)

        val signerInfo = getSignerInfoFromSignedData(signedData)

        // We shouldn't have to force this type cast but this is the only way I could get the code to work and, based on
        // what I found online, that's what others have had to do as well
        @Suppress("UNCHECKED_CAST") val signerCertSelector = X509CertificateHolderSelector(
            signerInfo.sid.issuer,
            signerInfo.sid.serialNumber
        ) as Selector<X509CertificateHolder>

        val signerCertMatches = signedData.certificates.getMatches(signerCertSelector)
        val signerCertificateHolder = try {
            signerCertMatches.first()
        } catch (_: NoSuchElementException) {
            throw InvalidHandshakeSignatureException(
                "Certificate of signer should be attached"
            )
        }
        val verifier = JcaSimpleSignerInfoVerifierBuilder().build(signerCertificateHolder)
        try {
            signerInfo.verify(verifier)
        } catch (_: CMSException) {
            throw InvalidHandshakeSignatureException("Invalid signature")
        }

        return SignatureVerification(Certificate(signerCertificateHolder))
    }

    private fun getSignerInfoFromSignedData(signedData: CMSSignedData): SignerInformation {
        val signersCount = signedData.signerInfos.size()
        if (signersCount != 1) {
            throw InvalidHandshakeSignatureException(
                "SignedData should contain exactly one SignerInfo (got $signersCount)"
            )
        }
        return signedData.signerInfos.first()
    }

    @Throws(InvalidHandshakeSignatureException::class)
    private fun parseCmsSignedData(
        cmsSignedDataSerialized: ByteArray,
        expectedPlaintext: ByteArray
    ): CMSSignedData {
        val asn1Stream = ASN1InputStream(cmsSignedDataSerialized)
        val asn1Sequence = try {
            asn1Stream.readObject()
        } catch (_: IOException) {
            throw InvalidHandshakeSignatureException("Value is not DER-encoded")
        }
        val contentInfo = try {
            ContentInfo.getInstance(asn1Sequence)
        } catch (_: IllegalArgumentException) {
            throw InvalidHandshakeSignatureException("SignedData value is not wrapped in ContentInfo")
        }
        return try {
            CMSSignedData(CMSProcessableByteArray(expectedPlaintext), contentInfo)
        } catch (_: CMSException) {
            throw InvalidHandshakeSignatureException("ContentInfo wraps invalid SignedData value")
        }
    }
}
