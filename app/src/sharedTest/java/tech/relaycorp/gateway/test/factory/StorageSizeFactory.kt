package tech.relaycorp.gateway.test.factory

import tech.relaycorp.gateway.data.model.StorageSize
import java.util.Random

object StorageSizeFactory {
    fun build() = StorageSize(Random().nextInt(1000).toLong() + 1)
}
