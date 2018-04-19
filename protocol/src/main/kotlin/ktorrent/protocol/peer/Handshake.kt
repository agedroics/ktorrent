package ktorrent.protocol.peer

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
            val length = inputStream.read()
            if (length == -1) {
                throw PeerProtocolException("Unexpected end of stream")
            }
            val protocol = String(readBytes(inputStream, length), Charsets.UTF_8)
            if (protocol != "BitTorrent protocol") {
                throw PeerProtocolException("Incompatible protocol '$protocol'")
            }
            readBytes(inputStream, 8)
            return Handshake(readBytes(inputStream, 20), readBytes(inputStream, 20))
        }
    }
}
