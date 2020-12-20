package tech.relaycorp.gateway.test.factory

import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.testing.pki.PDACertPath

object ParcelFactory {

    fun build() = Parcel(
        PDACertPath.PRIVATE_ENDPOINT.subjectPrivateAddress,
        "".toByteArray(),
        PDACertPath.PDA,
        senderCertificateChain = setOf(PDACertPath.PRIVATE_ENDPOINT)
    )

    fun buildSerialized() =
        build().serialize(KeyPairSet.PDA_GRANTEE.private)
}
