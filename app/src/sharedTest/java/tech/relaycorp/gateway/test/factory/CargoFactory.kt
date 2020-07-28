package tech.relaycorp.gateway.test.factory

import tech.relaycorp.gateway.test.CargoDeliveryCertPath
import tech.relaycorp.gateway.test.KeyPairSet
import tech.relaycorp.relaynet.messages.Cargo

object CargoFactory {

    /**
     * Build a cargo bound for a private gateway.
     */
    fun build() = Cargo(
        CargoDeliveryCertPath.PRIVATE_GW.subjectPrivateAddress,
        "".toByteArray(),
        CargoDeliveryCertPath.PUBLIC_GW
    )

    fun buildSerialized() =
        build().serialize(KeyPairSet.PUBLIC_GW.private)
}
