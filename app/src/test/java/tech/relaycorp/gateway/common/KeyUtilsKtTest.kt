package tech.relaycorp.gateway.common

import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class KeyUtilsKtTest {
    @Test
    internal fun `ByteArray#toHexString`() {
        val byteArray = byteArrayOf(10, 2, 15, 11)
        assertEquals(
            "0A020F0B",
            byteArray.toHexString()
        )
    }
}
