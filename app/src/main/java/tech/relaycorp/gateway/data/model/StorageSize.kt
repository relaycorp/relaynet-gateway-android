package tech.relaycorp.gateway.data.model

import kotlin.math.min

data class StorageSize(
    val bytes: Long,
) : Comparable<StorageSize> {
    val isZero get() = bytes == 0L

    operator fun plus(size: StorageSize) = StorageSize(bytes + size.bytes)
    operator fun minus(size: StorageSize) = StorageSize(bytes - size.bytes)
    operator fun minus(diff: Long) = StorageSize(bytes - diff)
    operator fun times(size: StorageSize) = StorageSize(bytes * size.bytes)
    operator fun times(times: Int) = StorageSize(bytes * times)
    operator fun div(divisor: StorageSize) = StorageSize(bytes / divisor.bytes)
    operator fun div(divisor: Long) = StorageSize(bytes / divisor)
    operator fun rem(divisor: StorageSize) = StorageSize(bytes % divisor.bytes)
    override fun compareTo(other: StorageSize) = bytes.compareTo(other.bytes)

    companion object {
        val ZERO = StorageSize(0L)

        fun min(size1: StorageSize, size2: StorageSize) = StorageSize(min(size1.bytes, size2.bytes))
    }
}
