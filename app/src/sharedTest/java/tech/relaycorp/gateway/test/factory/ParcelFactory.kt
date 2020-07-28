package tech.relaycorp.gateway.test.factory

import tech.relaycorp.gateway.common.nowInUtc
import tech.relaycorp.relaynet.issueEndpointCertificate
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair

object ParcelFactory {

    fun build() = Parcel(
        recipientAddress = "012345",
        senderCertificate = certificate,
        payload = "".toByteArray()
    )

    fun buildSerialized() =
        build().serialize(keyPair.private)

    val keyPair by lazy {
        generateRSAKeyPair()
    }

    val certificate by lazy {
        issueEndpointCertificate(
            keyPair.public,
            keyPair.private,
            nowInUtc().plusDays(1)
        )
    }
}
