package tech.relaycorp.gateway.test.factory

import tech.relaycorp.gateway.test.CargoDeliveryCertPath
import tech.relaycorp.gateway.test.KeyPairSet
import tech.relaycorp.relaynet.messages.Cargo

object CargoFactory {

    /**
     * Build a cargo bound for a private gateway.
     */
    fun build() = Cargo(
        recipientAddress = CargoDeliveryCertPath.PRIVATE_GW.subjectPrivateAddress,
        senderCertificate = CargoDeliveryCertPath.PUBLIC_GW,
        payload = "".toByteArray()
    )

    fun buildSerialized() =
        build().serialize(KeyPairSet.PRIVATE_GW.private)
}
