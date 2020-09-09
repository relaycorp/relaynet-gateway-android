package tech.relaycorp.gateway.test.factory

import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.testing.CertificationPath
import tech.relaycorp.relaynet.testing.KeyPairSet

object ParcelFactory {

    fun build() = Parcel(
        CertificationPath.PRIVATE_ENDPOINT.subjectPrivateAddress,
        "".toByteArray(),
        CertificationPath.PDA,
        senderCertificateChain = setOf(CertificationPath.PRIVATE_ENDPOINT)
    )

    fun buildSerialized() =
        build().serialize(KeyPairSet.PDA_GRANTEE.private)
}
