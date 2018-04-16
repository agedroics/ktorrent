package ktorrent.bencoding

import java.io.OutputStream
import java.util.*

data class BByteString(val value: ByteArray) : BEncodable {

    val string = value.toString(Charsets.UTF_8)

    constructor(string: String): this(string.toByteArray())

    override fun write(outputStream: OutputStream) {
        outputStream.write(value.size.toString().toByteArray(Charsets.ISO_8859_1))
        outputStream.write(':'.toInt())
        outputStream.write(value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BByteString

        if (!Arrays.equals(value, other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(value)
    }
}
