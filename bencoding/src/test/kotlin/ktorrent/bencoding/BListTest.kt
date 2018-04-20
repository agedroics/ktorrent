package ktorrent.bencoding

import kotlin.test.Test
import kotlin.test.assertEquals

class BListTest {

    @Test
    fun `Encodes empty list`() {
        assertEquals("le", BList().encode().toString(Charsets.UTF_8))
    }

    @Test
    fun `Encodes list of ByteStrings`() {
        val list = BList(BByteString("spam"), BByteString("eggs"))
        assertEquals("l4:spam4:eggse", list.encode().toString(Charsets.UTF_8))
    }
}
