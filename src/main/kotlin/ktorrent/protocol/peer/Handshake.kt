package ktorrent.protocol.peer

import ktorrent.utils.EndOfStreamException
import ktorrent.utils.readByteOrFail
import ktorrent.utils.readBytesOrFail
import java.io.InputStream
import java.io.OutputStream

class Handshake(val infoHash: ByteArray, val peerId: ByteArray) {

    private val prefix = ByteArray(28).apply {
        this[0] = 19.toByte()
        System.arraycopy("BitTorrent protocol".toByteArray(Charsets.UTF_8), 0, this, 1, 19)
    }

    fun write(outputStream: OutputStream) = with(outputStream) {
        write(prefix)
        write(infoHash)
        write(peerId)
    }

    companion object {

        fun read(inputStream: InputStream): Handshake {
            val length = inputStream.readByteOrFail()
            val protocol = String(inputStream.readBytesOrFail(length), Charsets.UTF_8)
            if (protocol != "BitTorrent protocol") {
                throw IncompatibleProtocolException(protocol)
            }
            if (inputStream.skip(8) < 8) {
                throw EndOfStreamException
            }
            return Handshake(inputStream.readBytesOrFail(20), inputStream.readBytesOrFail(20))
        }
    }
}
