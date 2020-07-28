package tech.relaycorp.gateway.pdc.local

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.cms.CMSException
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.CMSTypedData
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.relaynet.issueEndpointCertificate
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import java.io.IOException
import java.nio.charset.Charset

class HandshakeTest {
    @Nested
    inner class GenerateNonce {
        @Test
        fun `Nonce should be a UUID4`() {
            val nonce = Handshake.generateNonce()

            val nonceString = nonce.toString(Charset.forName("UTF8"))
            assertTrue(HandshakeTestUtils.UUID4_REGEX.matches(nonceString))
        }
    }

    @Nested
    inner class VerifySignature {
        private val nonce = "The nonce".toByteArray()
        private val endpointKeyPair = generateRSAKeyPair()
        private val endpointCertificate = issueEndpointCertificate(
            endpointKeyPair.public,
            endpointKeyPair.private,
            nowInUtc().plusDays(1)
        )

        @Test
        fun `Invalid DER values should be refused`() {
            val invalidCMSSignedData = "Not really DER-encoded".toByteArray()

            val exception = assertThrows<InvalidHandshakeSignatureException> {
                Handshake.verifySignature(invalidCMSSignedData, nonce)
            }

            assertEquals("Value is not DER-encoded", exception.message)
        }

        @Test
        fun `ContentInfo wrapper should be required`() {
            val invalidCMSSignedData = ASN1Integer(10).encoded

            val exception = assertThrows<InvalidHandshakeSignatureException> {
                Handshake.verifySignature(invalidCMSSignedData, nonce)
            }

            assertEquals(
                "SignedData value is not wrapped in ContentInfo",
                exception.message
            )
        }

        @Test
        fun `ContentInfo wrapper should contain a valid SignedData value`() {
            val signedDataOid = ASN1ObjectIdentifier("1.2.840.113549.1.7.2")
            val invalidCMSSignedData = ContentInfo(signedDataOid, ASN1Integer(10))

            val exception = assertThrows<InvalidHandshakeSignatureException> {
                Handshake.verifySignature(invalidCMSSignedData.encoded, nonce)
            }

            assertEquals(
                "ContentInfo wraps invalid SignedData value",
                exception.message
            )
        }

        @Test
        fun `Well formed but invalid signatures should be rejected`() {
            // Swap the SignerInfo collection from two different CMS SignedData values

            val cmsSignedDataSerialized1 =
                HandshakeTestUtils.sign(nonce, endpointKeyPair.private, endpointCertificate)
            val cmsSignedData1 = parseCmsSignedData(cmsSignedDataSerialized1)

            val cmsSignedDataSerialized2 = HandshakeTestUtils.sign(
                byteArrayOf(0xde.toByte(), *nonce),
                endpointKeyPair.private,
                endpointCertificate
            )
            val cmsSignedData2 = parseCmsSignedData(cmsSignedDataSerialized2)

            val invalidCmsSignedData = CMSSignedData.replaceSigners(
                cmsSignedData1,
                cmsSignedData2.signerInfos
            )
            val invalidCmsSignedDataSerialized = invalidCmsSignedData.toASN1Structure().encoded

            val exception = assertThrows<InvalidHandshakeSignatureException> {
                Handshake.verifySignature(invalidCmsSignedDataSerialized, nonce)
            }

            assertEquals("Invalid signature", exception.message)
        }

        @Test
        fun `An empty SignerInfo collection should be refused`() {
            val signedDataGenerator = CMSSignedDataGenerator()
            val plaintextCms: CMSTypedData = CMSProcessableByteArray(nonce)
            val cmsSignedData = signedDataGenerator.generate(plaintextCms, true)

            val exception = assertThrows<InvalidHandshakeSignatureException> {
                Handshake.verifySignature(cmsSignedData.encoded, nonce)
            }

            assertEquals(
                "SignedData should contain exactly one SignerInfo (got 0)",
                exception.message
            )
        }

        @Test
        fun `A SignerInfo collection with more than one item should be refused`() {
            val signedDataGenerator = CMSSignedDataGenerator()

            val signerBuilder = JcaContentSignerBuilder("SHA256withRSA")
            val contentSigner: ContentSigner = signerBuilder.build(endpointKeyPair.private)
            val signerInfoGenerator = JcaSignerInfoGeneratorBuilder(
                JcaDigestCalculatorProviderBuilder()
                    .build()
            ).build(contentSigner, endpointCertificate.certificateHolder)
            // Add the same SignerInfo twice
            signedDataGenerator.addSignerInfoGenerator(
                signerInfoGenerator
            )
            signedDataGenerator.addSignerInfoGenerator(
                signerInfoGenerator
            )

            val cmsSignedData = signedDataGenerator.generate(
                CMSProcessableByteArray(nonce),
                true
            )

            val exception = assertThrows<InvalidHandshakeSignatureException> {
                Handshake.verifySignature(cmsSignedData.encoded, nonce)
            }

            assertEquals(
                "SignedData should contain exactly one SignerInfo (got 2)",
                exception.message
            )
        }

        @Test
        fun `Certificate of signer should be required`() {
            val signedDataGenerator = CMSSignedDataGenerator()

            val signerBuilder = JcaContentSignerBuilder("SHA256withRSA")
            val contentSigner: ContentSigner = signerBuilder.build(endpointKeyPair.private)
            val signerInfoGenerator = JcaSignerInfoGeneratorBuilder(
                JcaDigestCalculatorProviderBuilder()
                    .build()
            ).build(contentSigner, endpointCertificate.certificateHolder)
            signedDataGenerator.addSignerInfoGenerator(
                signerInfoGenerator
            )

            val cmsSignedData = signedDataGenerator.generate(
                CMSProcessableByteArray(nonce),
                true
            )

            val exception = assertThrows<InvalidHandshakeSignatureException> {
                Handshake.verifySignature(cmsSignedData.encoded, nonce)
            }

            assertEquals("Certificate of signer should be attached", exception.message)
        }

        @Test
        fun `Verification should fail if signed nonce does not match expected plaintext`() {
            val cmsSignedDataSerialized =
                HandshakeTestUtils.sign(nonce, endpointKeyPair.private, endpointCertificate)

            val exception = assertThrows<InvalidHandshakeSignatureException> {
                Handshake.verifySignature(
                    cmsSignedDataSerialized,
                    byteArrayOf(0xde.toByte(), *nonce)
                )
            }

            assertEquals("Invalid signature", exception.message)
        }

        @Test
        fun `Verification should succeed if signed nonce matches expected plaintext`() {
            val cmsSignedDataSerialized =
                HandshakeTestUtils.sign(nonce, endpointKeyPair.private, endpointCertificate)

            // No exceptions thrown
            Handshake.verifySignature(cmsSignedDataSerialized, nonce)
        }

        @Test
        fun `Signer certificate should be output when verification passes`() {
            val cmsSignedDataSerialized = HandshakeTestUtils.sign(
                nonce,
                endpointKeyPair.private,
                endpointCertificate
            )

            val verificationResult = Handshake.verifySignature(cmsSignedDataSerialized, nonce)

            assertEquals(
                endpointCertificate.certificateHolder,
                verificationResult.signerCertificate.certificateHolder
            )
        }

        @Throws(IOException::class, IllegalArgumentException::class, CMSException::class)
        private fun parseCmsSignedData(cmsSignedDataSerialized: ByteArray): CMSSignedData {
            val asn1Stream = ASN1InputStream(cmsSignedDataSerialized)
            val asn1Sequence = asn1Stream.readObject()
            val contentInfo = ContentInfo.getInstance(asn1Sequence)
            return CMSSignedData(contentInfo)
        }
    }
}
