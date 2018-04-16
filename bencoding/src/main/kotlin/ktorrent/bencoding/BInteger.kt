package ktorrent.bencoding

import java.io.OutputStream

data class BInteger(val value: Long) : BEncodable {

    override fun write(outputStream: OutputStream) {
        outputStream.write('i'.toInt())
        outputStream.write(value.toString().toByteArray(Charsets.ISO_8859_1))
        outputStream.write('e'.toInt())
    }
}
