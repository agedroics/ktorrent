package ktorrent.bencoding

import java.io.OutputStream

data class BInteger(val value: Long) : BEncodable {

    private val bytes = ("i" + value.toString() + "e").toByteArray(Charsets.ISO_8859_1)

    override fun write(outputStream: OutputStream) = outputStream.write(bytes)
}
