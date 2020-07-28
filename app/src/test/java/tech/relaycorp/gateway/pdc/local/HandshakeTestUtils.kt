package tech.relaycorp.gateway.pdc.local

import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.CMSTypedData
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import tech.relaycorp.gateway.common.CryptoUtils
import tech.relaycorp.relaynet.wrappers.x509.Certificate
import java.security.PrivateKey

object HandshakeTestUtils {
    val UUID4_REGEX =
        """^[-0-9a-f]{36}$""".toRegex()

    fun sign(
        plaintext: ByteArray,
        signerPrivateKey: PrivateKey,
        signerCertificate: Certificate,
        encapsulatePlaintext: Boolean = false
    ): ByteArray {
        val signedDataGenerator = CMSSignedDataGenerator()

        val signerBuilder = JcaContentSignerBuilder("SHA256WITHRSAANDMGF1")
            .setProvider(CryptoUtils.BC_PROVIDER)
        val contentSigner: ContentSigner = signerBuilder.build(signerPrivateKey)
        val signerInfoGenerator = JcaSignerInfoGeneratorBuilder(
            JcaDigestCalculatorProviderBuilder()
                .build()
        ).build(contentSigner, signerCertificate.certificateHolder)
        signedDataGenerator.addSignerInfoGenerator(
            signerInfoGenerator
        )

        signedDataGenerator.addCertificate(signerCertificate.certificateHolder)

        val plaintextCms: CMSTypedData = CMSProcessableByteArray(plaintext)
        val cmsSignedData = signedDataGenerator.generate(plaintextCms, encapsulatePlaintext)
        return cmsSignedData.encoded
    }
}
