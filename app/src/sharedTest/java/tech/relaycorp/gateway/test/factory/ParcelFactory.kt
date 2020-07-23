package tech.relaycorp.gateway.test.factory

import tech.relaycorp.relaynet.issueEndpointCertificate
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import java.time.ZonedDateTime

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
            ZonedDateTime.now().plusDays(1)
        )
    }
}
