package ktorrent.bencoding

import kotlin.test.Test
import kotlin.test.assertEquals

class BByteStringTest {

    @Test
    fun `Encodes empty string`() {
        assertEquals("0:", BByteString("").encode().toString(Charsets.UTF_8))
    }

    @Test
    fun `Encodes unicode string`() {
        assertEquals("3:aā", BByteString("aā").encode().toString(Charsets.UTF_8))
    }
}
