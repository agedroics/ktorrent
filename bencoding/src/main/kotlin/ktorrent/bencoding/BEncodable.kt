package ktorrent.bencoding

import java.io.ByteArrayOutputStream
import java.io.OutputStream

interface BEncodable {

    fun write(outputStream: OutputStream)

    fun encode(): ByteArray = ByteArrayOutputStream().run {
        write(this)
        toByteArray()
    }
}
