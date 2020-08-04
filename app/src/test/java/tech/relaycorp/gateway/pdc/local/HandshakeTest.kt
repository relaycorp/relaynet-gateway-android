package tech.relaycorp.gateway.pdc.local

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.cms.CMSException
import org.bouncycastle.cms.CMSSignedData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.relaynet.crypto.SignedDataException
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

            assertEquals("Invalid CMS SignedData value", exception.message)
            assertTrue(exception.cause is SignedDataException)
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

            assertEquals("Invalid CMS SignedData value", exception.message)
            assertTrue(exception.cause is SignedDataException)
        }

        @Test
        fun `Verification should succeed if signed nonce matches expected plaintext`() {
            val cmsSignedDataSerialized =
                HandshakeTestUtils.sign(nonce, endpointKeyPair.private, endpointCertificate)

            // No exceptions thrown
            Handshake.verifySignature(cmsSignedDataSerialized, nonce)
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
