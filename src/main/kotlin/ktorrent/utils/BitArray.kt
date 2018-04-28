package ktorrent.utils

import java.util.*
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.math.ceil

class BitArray : Collection<Boolean> {

    override val size: Int

    val byteArray: ByteArray

    constructor(size: Int, fillWithTrues: Boolean = false) {
        this.size = size
        byteArray = ByteArray(ceil(size / 8f).toInt())
        if (fillWithTrues) {
            fillWith(true)
        }
    }

    fun fillWith(value: Boolean) {
        (0 until byteArray.size).forEach {
            byteArray[it] = when (value) {
                true -> when (it) {
                    byteArray.size - 1 -> (0xFF ushr byteArray.size % 8).inv().toByte()
                    else -> 0xFF.toByte()
                }
                false -> 0
            }
        }
    }

    private constructor(byteArray: ByteArray, size: Int) {
        val requiredLength = ceil(size / 8f).toInt()
        if (requiredLength > byteArray.size) {
            throw IllegalArgumentException("Byte array too small (size: ${byteArray.size}, required: $requiredLength)")
        } else {
            this.byteArray = byteArray
            this.size = size
        }
    }

    operator fun get(index: Int) = when (index) {
        in 0 until size -> byteArray[index / 8] and (0b1000_0000 ushr index % 8).toByte() != 0.toByte()
        else -> throw ArrayIndexOutOfBoundsException(index)
    }

    operator fun set(index: Int, set: Boolean) {
        when (index) {
            in 0 until size -> byteArray[index / 8] = when (set) {
                true -> byteArray[index / 8] or (0b1000_0000 ushr index % 8).toByte()
                false -> byteArray[index / 8] and (0b1000_0000 ushr index % 8).toByte().inv()
            }
            else -> throw ArrayIndexOutOfBoundsException(index)
        }
    }

    override fun contains(element: Boolean): Boolean {
        if (size % 8 == 0) {
            return byteArray.any { it != if (element) 0.toByte() else 0b1111_1111.toByte() }
        }
        for (i in 0 until byteArray.size - 1) {
            when (element) {
                true -> if (byteArray[i] != 0.toByte()) return true
                false -> if (byteArray[i] != 0b1111_1111.toByte()) return true
            }
        }
        when (element) {
            true -> if (byteArray.last() != 0.toByte()) return true
            false -> if (byteArray.last() != (0b1111_1111 ushr size % 8).inv().toByte()) return true
        }
        return false
    }

    override fun containsAll(elements: Collection<Boolean>): Boolean {
        if (elements.isEmpty()) {
            return false
        }
        val containsTrue = contains(true)
        val containsFalse = contains(false)
        if (containsTrue && containsFalse) {
            return true
        }
        return elements.all { if (it) containsTrue else containsFalse }
    }

    override fun isEmpty() = size == 0

    override fun iterator() = BitArrayIterator(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BitArray

        if (size != other.size) return false
        if (!Arrays.equals(byteArray, other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + Arrays.hashCode(byteArray)
        return result
    }

    companion object {

        fun wrap(byteArray: ByteArray, size: Int = byteArray.size * 8): BitArray {
            return BitArray(byteArray, size)
        }
    }

    class BitArrayIterator(private val bitArray: BitArray) : Iterator<Boolean> {

        private var position = 0

        override fun hasNext() = position < bitArray.size

        override fun next() = bitArray[position++]
    }
}
