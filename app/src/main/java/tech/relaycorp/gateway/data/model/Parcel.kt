package tech.relaycorp.gateway.data.model

import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.ramf.RAMFMessage

// TODO: Dummy class to be replaced by the Relaynet core library
object Parcel {
    fun deserialize(bytes: ByteArray): RAMFMessage =
        Cargo.Companion.deserialize(bytes)
}
