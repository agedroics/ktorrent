package ktorrent

import kotlin.test.*

class BitArrayTest {

    @Test
    fun `Does not find set bits in a new instance`() {
        assertFalse(BitArray(8).contains(true))
        assertFalse(BitArray(12).contains(true))
    }

    @Test
    fun `Does not find unset bits in a fully set instance`() {
        val bitArray8 = BitArray(8)
        for (i in 0 until bitArray8.size) {
            bitArray8[i] = true
        }
        assertFalse(bitArray8.contains(false))

        val bitArray12 = BitArray(12)
        for (i in 0 until bitArray12.size) {
            bitArray12[i] = true
        }
        assertFalse(bitArray12.contains(false))
    }

    @Test
    fun `Finds an unset bit in a new instance`() {
        assertTrue(BitArray(8).contains(false))
        assertTrue(BitArray(12).contains(false))
    }

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
    fun `Equality checks contents and length`() {
        assertEquals(BitArray(6), BitArray(6))
        assertNotEquals(BitArray(6), BitArray(8))
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
    fun `containsAll works as expected`() {
        val bitArray = BitArray(6)
        assertFalse(bitArray.containsAll(emptyList()))
        assertFalse(bitArray.containsAll(listOf(true, false)))
        assertTrue(bitArray.containsAll(listOf(false, false)))

        bitArray[2] = true
        assertFalse(bitArray.containsAll(emptyList()))
        assertTrue(bitArray.containsAll(listOf(true, false)))
    }

    @Test
    fun `Zero-sized instance is empty`() {
        val bitArray = BitArray(0)
        assertTrue(bitArray.isEmpty())
        assertEquals(0, bitArray.byteArray.size)
        assertFalse(ByteArray(6).isEmpty())
    }
}
