package ktorrent.bencoding

import kotlin.test.Test
import kotlin.test.assertEquals

class BIntegerTest {

    @Test
    fun `Encodes 0`() {
        assertEquals("i0e", BInteger(0).encode().toString(Charsets.UTF_8))
    }

    @Test
    fun `Encodes negative integer`() {
        assertEquals("i-3e", BInteger(-3).encode().toString(Charsets.UTF_8))
    }

    @Test
    fun `Encodes 64 bit signed integers`() {
        assertEquals("i-9223372036854775808e", BInteger(Long.MIN_VALUE).encode().toString(Charsets.UTF_8))
        assertEquals("i9223372036854775807e", BInteger(Long.MAX_VALUE).encode().toString(Charsets.UTF_8))
    }
}
