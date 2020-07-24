package tech.relaycorp.gateway.test.factory

import tech.relaycorp.gateway.test.FullCertPath
import tech.relaycorp.gateway.test.KeyPairSet
import tech.relaycorp.relaynet.messages.Parcel

object ParcelFactory {

    fun build() = Parcel(
        FullCertPath.PRIVATE_ENDPOINT.subjectPrivateAddress,
        "".toByteArray(),
        FullCertPath.PDA,
        senderCertificateChain = setOf(FullCertPath.PRIVATE_ENDPOINT)
    )

    fun buildSerialized() =
        build().serialize(KeyPairSet.PDA_GRANTEE.private)
}
