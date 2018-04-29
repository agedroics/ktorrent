package ktorrent.utils

import kotlin.test.*

class BitArrayTest {

    @Test
    fun `Throws exception when accessing out-of-bounds elements`() {
        val bitArray = BitArray(12)
        assertFailsWith(ArrayIndexOutOfBoundsException::class) { bitArray[-1] }
        assertFailsWith(ArrayIndexOutOfBoundsException::class) { bitArray[12] }
        assertFailsWith(ArrayIndexOutOfBoundsException::class) { bitArray[-1] = true }
        assertFailsWith(ArrayIndexOutOfBoundsException::class) { bitArray[12] = true }
    }

    @Test
    fun `Reads edge-case elements`() {
        val bitArray = BitArray(12)
        assertFalse(bitArray[0])
        assertFalse(bitArray[11])
    }

    @Test
    fun `Sets a bit`() {
        val bitArray = BitArray(12)
        bitArray[5] = true
        assertTrue(bitArray[5])
        bitArray[5] = false
        assertFalse(bitArray[5])
    }

    @Test
    fun `Wrapper around ByteArray mutates source`() {
        val byteArray = ByteArray(1)
        val bitArray = BitArray.wrap(byteArray)
        assertEquals(byteArray, bitArray.byteArray)
        bitArray[0] = true
        assertEquals(byteArray, bitArray.byteArray)
        assertEquals(0b1000_0000.toByte(), byteArray[0])
    }

    @Test
    fun `Throws exception when wrappable ByteArray is too small`() {
        assertFailsWith(IllegalArgumentException::class) { BitArray.wrap(ByteArray(2), 17) }
    }

    @Test
    fun `Zero-sized instance is empty`() {
        val bitArray = BitArray(0)
        assertEquals(0, bitArray.byteArray.size)
        assertFalse(ByteArray(6).isEmpty())
    }
}
