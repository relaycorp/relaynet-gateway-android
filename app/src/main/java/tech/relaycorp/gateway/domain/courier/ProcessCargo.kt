package tech.relaycorp.gateway.domain.courier

import tech.relaycorp.gateway.data.disk.CargoStorage
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.relaynet.cogrpc.readBytesAndClose
import tech.relaycorp.relaynet.messages.Cargo
import javax.inject.Inject

class ProcessCargo
@Inject constructor(
    private val cargoStorage: CargoStorage,
    private val readParcelsFromCargo: ReadParcelsFromCargo,
    private val storeParcel: StoreParcel
) {

    suspend fun process() {
        val cargoes = cargoStorage.list()
        cargoes.forEach { cargoStream ->
            val cargo = Cargo.deserialize(cargoStream().readBytesAndClose())
            readParcelsFromCargo
                .read(cargo)
                .forEach { storeParcel.store(it) }
        }
        cargoStorage.deleteAll()
    }
}
