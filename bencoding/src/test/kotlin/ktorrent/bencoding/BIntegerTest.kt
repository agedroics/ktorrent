package ktorrent.bencoding

import kotlin.test.Test
import kotlin.test.assertEquals

class BIntegerTest {

    @Test
    fun testZero() {
        assertEquals("i0e", BInteger(0).encode().toString(Charsets.UTF_8))
    }

    @Test
    fun testNegative() {
        assertEquals("i-3e", BInteger(-3).encode().toString(Charsets.UTF_8))
    }

    @Test
    fun test64Bit() {
        assertEquals("i9223372036854775807e", BInteger(Long.MAX_VALUE).encode().toString(Charsets.UTF_8))
    }
}
