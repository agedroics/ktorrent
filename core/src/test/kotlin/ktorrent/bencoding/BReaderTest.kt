package ktorrent.bencoding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BReaderTest {

    @Test
    fun `Throws exception on empty input`() {
        assertFailsWith(InvalidBEncodingException::class, { BReader("".byteInputStream()).read() })
    }

    @Test
    fun `Throws exception on invalid syntax`() {
        assertFailsWith(InvalidBEncodingException::class) { BReader("a".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader(":".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("e".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("i".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("l".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("d".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("da".byteInputStream()).read() }
    }

    @Test
    fun `Decodes empty ByteString`() {
        assertEquals(BByteString(""), BReader("0:".byteInputStream()).read())
    }

    @Test
    fun `Decodes unicode ByteString`() {
        assertEquals(BByteString("aā"), BReader("3:aā".byteInputStream()).read())
    }

    @Test
    fun `Throws exception on invalid ByteStrings`() {
        assertFailsWith(InvalidBEncodingException::class) { BReader("2:a".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("-1:a".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader(":a".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("1e:a".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("6".byteInputStream()).read() }
    }

    @Test
    fun `Decodes 0`() {
        assertEquals(BInteger(0), BReader("i0e".byteInputStream()).read())
    }

    @Test
    fun `Decodes negative integer`() {
        assertEquals(BInteger(-301), BReader("i-301e".byteInputStream()).read())
    }

    @Test
    fun `Decodes 64 bit signed integers`() {
        assertEquals(BInteger(Long.MIN_VALUE), BReader("i-9223372036854775808e".byteInputStream()).read())
        assertEquals(BInteger(Long.MAX_VALUE), BReader("i9223372036854775807e".byteInputStream()).read())
    }

    @Test
    fun `Throws exception on invalid integers`() {
        assertFailsWith(InvalidBEncodingException::class) { BReader("i00e".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("i01e".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("i-0e".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("i1".byteInputStream()).read() }
        assertFailsWith(InvalidBEncodingException::class) { BReader("ie".byteInputStream()).read() }
    }

    @Test
    fun `Decodes empty list`() {
        assertEquals(BList(), BReader("le".byteInputStream()).read())
    }

    @Test
    fun `Decodes list of ByteStrings`() {
        val list = BList(BByteString("spam"), BByteString("eggs"))
        assertEquals(list, BReader("l4:spam4:eggse".byteInputStream()).read())
    }

    @Test
    fun `Decodes empty dictionary`() {
        assertEquals(BDictionary(), BReader("de".byteInputStream()).read())
    }

    @Test
    fun `Decodes dictionary of ByteString values`() {
        val dictionary = BDictionary(
                "spam" to BByteString("eggs"),
                "cow" to BByteString("moo")
        )
        assertEquals(dictionary, BReader("d3:cow3:moo4:spam4:eggse".byteInputStream()).read())
    }
}
