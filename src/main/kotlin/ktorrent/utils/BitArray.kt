package ktorrent.utils

import ktorrent.bencoding.BByteString
import ktorrent.bencoding.BDictionary
import ktorrent.bencoding.BEncodable
import ktorrent.bencoding.BInteger
import java.io.OutputStream
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.math.ceil

class BitArray : BEncodable {

    val byteArray: ByteArray
    val size: Int

    constructor(size: Int, fillWithTrues: Boolean = false) {
        this.size = size
        byteArray = ByteArray(ceil(size / 8f).toInt())
        if (fillWithTrues) {
            fillWith(true)
        }
    }

    private constructor(byteArray: ByteArray, size: Int) {
        val requiredLength = ceil(size / 8f).toInt()
        if (requiredLength > byteArray.size) {
            throw IllegalArgumentException("Byte array too small (size: ${byteArray.size}, required: $requiredLength)")
        } else {
            this.size = size
            this.byteArray = byteArray
        }
    }

    fun fillWith(value: Boolean) {
        (0 until byteArray.size).forEach {
            byteArray[it] = when (value) {
                true -> when (it) {
                    byteArray.size - 1 -> (0xFF ushr size % 8).inv().toByte()
                    else -> 0xFF.toByte()
                }
                false -> 0
            }
        }
    }

    operator fun get(index: Int) = when (index) {
        in 0 until size -> byteArray[index / 8] and (0b1000_0000 ushr index % 8).toByte() != 0.toByte()
        else -> throw ArrayIndexOutOfBoundsException(index)
    }

    operator fun set(index: Int, value: Boolean) {
        when (index) {
            in 0 until size -> byteArray[index / 8] = when (value) {
                true -> byteArray[index / 8] or (0b1000_0000 ushr index % 8).toByte()
                false -> byteArray[index / 8] and (0b1000_0000 ushr index % 8).toByte().inv()
            }
            else -> throw ArrayIndexOutOfBoundsException(index)
        }
    }

    override fun write(outputStream: OutputStream) = BDictionary(
            "data" to BByteString(byteArray),
            "size" to BInteger(size.toLong())
    ).write(outputStream)

    companion object {

        fun wrap(byteArray: ByteArray, size: Int = byteArray.size * 8) = BitArray(byteArray, size)

        fun read(dictionary: BDictionary) = BitArray(
                byteArray = (dictionary["data"] as? BByteString)?.value ?: throw MappingException("Failed to read piece map"),
                size = (dictionary["size"] as? BInteger)?.value?.toInt() ?: throw MappingException("Failed to read piece map")
        )
    }
}
