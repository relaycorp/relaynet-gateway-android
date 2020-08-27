package tech.relaycorp.gateway.pdc.local

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.gateway.pdc.local.utils.Handshake
import tech.relaycorp.gateway.pdc.local.utils.InvalidHandshakeSignatureException
import tech.relaycorp.gateway.test.FullCertPath
import tech.relaycorp.gateway.test.KeyPairSet
import tech.relaycorp.relaynet.messages.InvalidMessageException
import java.nio.charset.Charset
import kotlin.test.assertNull

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
        private val endpointPrivateKey = KeyPairSet.PRIVATE_ENDPOINT.private
        private val endpointCertificate = FullCertPath.PRIVATE_ENDPOINT

        @Test
        fun `Invalid signatures should be refused`() {
            // Sign with different private key:
            val invalidSignatureSerialized =
                HandshakeTestUtils.sign(nonce, KeyPairSet.PDA_GRANTEE.private, endpointCertificate)

            val exception = assertThrows<InvalidHandshakeSignatureException> {
                Handshake.verifySignature(invalidSignatureSerialized, nonce)
            }

            assertEquals("Invalid signature", exception.message)
            assertTrue(exception.cause is InvalidMessageException)
        }

        @Test
        fun `Verification should fail if signed nonce doesn't match expected plaintext`() {
            val invalidSignatureSerialized = HandshakeTestUtils.sign(
                byteArrayOf(1, *nonce),
                endpointPrivateKey,
                endpointCertificate
            )

            val exception = assertThrows<InvalidHandshakeSignatureException> {
                Handshake.verifySignature(invalidSignatureSerialized, nonce)
            }

            assertEquals("Signed nonce does not match expected one", exception.message)
            assertNull(exception.cause)
        }

        @Test
        fun `Signer's certificate should be output if signed nonce matches expected plaintext`() {
            val signatureSerialized =
                HandshakeTestUtils.sign(nonce, endpointPrivateKey, endpointCertificate)

            val signerCertificate = Handshake.verifySignature(signatureSerialized, nonce)

            assertEquals(endpointCertificate, signerCertificate)
        }
    }
}
