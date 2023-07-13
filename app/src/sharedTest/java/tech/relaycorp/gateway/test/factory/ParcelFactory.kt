package tech.relaycorp.gateway.test.factory

import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath

object ParcelFactory {

    fun build(internetAddress: String? = null) = Parcel(
        Recipient(PDACertPath.PRIVATE_ENDPOINT.subjectId, internetAddress),
        "".toByteArray(),
        PDACertPath.PDA,
        senderCertificateChain = setOf(PDACertPath.PRIVATE_ENDPOINT)
    )

    fun buildSerialized(internetAddress: String? = null) =
        build(internetAddress).serialize(KeyPairSet.PDA_GRANTEE.private)
}
