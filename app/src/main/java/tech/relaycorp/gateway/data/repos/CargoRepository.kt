package tech.relaycorp.gateway.data.repos

import tech.relaycorp.gateway.data.disk.DiskRepository
import java.io.InputStream
import javax.inject.Inject

class CargoRepository
@Inject constructor(
    private val diskRepository: DiskRepository
) {
    // TODO: implementation
    suspend fun store(inputStream: InputStream) {}
}
