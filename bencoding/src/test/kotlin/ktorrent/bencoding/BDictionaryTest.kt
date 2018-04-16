package ktorrent.bencoding

import kotlin.test.Test
import kotlin.test.assertEquals

class BDictionaryTest {

    @Test
    fun testEmpty() {
        assertEquals("de", BDictionary().encode().toString(Charsets.UTF_8))
    }

    @Test
    fun testOrder() {
        val dictionary = BDictionary(mapOf(
                "spam" to BByteString("eggs"),
                "cow" to BByteString("moo")
        ))
        assertEquals("d3:cow3:moo4:spam4:eggse", dictionary.encode().toString(Charsets.UTF_8))
    }
}
