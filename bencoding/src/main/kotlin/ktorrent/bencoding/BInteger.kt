package ktorrent.bencoding

import java.io.OutputStream

data class BInteger(val value: Long) : BEncodable {

    override fun write(outputStream: OutputStream) = with(outputStream) {
        write('i'.toInt())
        write(value.toString().toByteArray(Charsets.ISO_8859_1))
        write('e'.toInt())
    }
}
