package ktorrent.bencoding

import kotlin.test.Test
import kotlin.test.assertEquals

class BListTest {

    @Test
    fun testEmpty() {
        assertEquals("le", BList().encode().toString(Charsets.UTF_8))
    }

    @Test
    fun testWithByteStrings() {
        val list = BList(listOf(BByteString("spam"), BByteString("eggs")))
        assertEquals("l4:spam4:eggse", list.encode().toString(Charsets.UTF_8))
    }
}
