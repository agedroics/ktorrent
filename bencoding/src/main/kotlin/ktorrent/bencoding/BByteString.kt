package ktorrent.bencoding

import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

data class BByteString(val value: ByteArray) : BEncodable {

    constructor(string: String, charset: Charset = Charsets.UTF_8) : this(string.toByteArray(charset))

    fun string(charset: Charset = Charsets.UTF_8) = String(value, charset)

    override fun write(outputStream: OutputStream) = outputStream.run {
        write(value.size.toString().toByteArray(Charsets.ISO_8859_1))
        write(':'.toInt())
        write(value)
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
