package ktorrent.protocol.peer

import ktorrent.protocol.utils.BitArray
import ktorrent.protocol.utils.toByteArray
import ktorrent.protocol.utils.toInt
import ktorrent.protocol.utils.toShort
import java.io.InputStream
import java.io.OutputStream

sealed class Message {

    protected abstract val bytes: ByteArray

    fun write(outputStream: OutputStream) = outputStream.write(bytes)

    fun read(inputStream: InputStream): Message {
        val length = readBytes(inputStream, 4).toInt()
        if (length == 0) {
            return KeepAlive
        }
        val bytes = readBytes(inputStream, length)
        return when (bytes[0].toInt()) {
            0 -> Choke
            1 -> Unchoke
            2 -> Interested
            3 -> NotInterested
            4 -> when (length) {
                5 -> Have(bytes.sliceArray(1..4).toInt())
                else -> throw PeerProtocolException("Incorrect length for 'have' message")
            }
            5 -> BitField(BitArray.wrap(bytes.sliceArray(1 until bytes.size)))
            6 -> when (length) {
                13 -> Request(
                        pieceIndex = bytes.sliceArray(1..4).toInt(),
                        offset = bytes.sliceArray(5..8).toInt(),
                        length = bytes.sliceArray(9..12).toInt()
                )
                else -> throw PeerProtocolException("Incorrect length for 'request' message")
            }
            7 -> when {
                length >= 9 -> Piece(
                        pieceIndex = bytes.sliceArray(1..4).toInt(),
                        offset = bytes.sliceArray(5..8).toInt(),
                        data = bytes.sliceArray(9 until bytes.size)
                )
                else -> throw PeerProtocolException("'piece' message too short")
            }
            8 -> when (length) {
                13 -> Cancel(
                        pieceIndex = bytes.sliceArray(1..4).toInt(),
                        offset = bytes.sliceArray(5..8).toInt(),
                        length = bytes.sliceArray(9..12).toInt()
                )
                else -> throw PeerProtocolException("Incorrect length for 'cancel' message")
            }
            9 -> when (length) {
                3 -> Port(bytes.sliceArray(1..2).toShort())
                else -> throw PeerProtocolException("Incorrect length for 'port' message")
            }
            else -> throw PeerProtocolException("Unrecognized message of length $length with id ${bytes[0].toInt()}")
        }
    }
}

object KeepAlive : Message() {

    override val bytes = ByteArray(4)
}

object Choke : Message() {

    override val bytes = ByteArray(5).apply {
        System.arraycopy(1.toByteArray(), 0, this, 0, 4)
    }
}

object Unchoke : Message() {

    override val bytes = ByteArray(5).apply {
        System.arraycopy(1.toByteArray(), 0, this, 0, 4)
        this[4] = 1
    }
}

object Interested : Message() {

    override val bytes = ByteArray(5).apply {
        System.arraycopy(1.toByteArray(), 0, this, 0, 4)
        this[4] = 2
    }
}

object NotInterested : Message() {

    override val bytes = ByteArray(5).apply {
        System.arraycopy(1.toByteArray(), 0, this, 0, 4)
        this[4] = 3
    }
}

class Have(val pieceIndex: Int) : Message() {

    override val bytes = ByteArray(9).apply {
        System.arraycopy(5.toByteArray(), 0, this, 0, 4)
        this[4] = 4
        System.arraycopy(pieceIndex.toByteArray(), 0, this, 5, 4)
    }
}

class BitField(val bitArray: BitArray) : Message() {

    override val bytes: ByteArray

    get() = bitArray.byteArray.let {
        ByteArray(5 + it.size).apply {
            System.arraycopy((1 + it.size).toByteArray(), 0, this, 0, 4)
            this[4] = 5
            System.arraycopy(it, 0, this, 5, it.size)
        }
    }
}

class Request(val pieceIndex: Int, val offset: Int, length: Int) : Message() {

    override val bytes = ByteArray(17).apply {
        System.arraycopy(13.toByteArray(), 0, this, 0, 4)
        this[4] = 6
        System.arraycopy(pieceIndex.toByteArray(), 0, this, 5, 4)
        System.arraycopy(offset.toByteArray(), 0, this, 9, 4)
        System.arraycopy(length.toByteArray(), 0, this, 13, 4)
    }
}

class Piece(val pieceIndex: Int, val offset: Int, val data: ByteArray) : Message() {

    override val bytes: ByteArray

    get() = ByteArray(13 + data.size).apply {
        System.arraycopy((9 + data.size).toByteArray(), 0, this, 0, 4)
        this[4] = 7
        System.arraycopy(pieceIndex.toByteArray(), 0, this, 5, 4)
        System.arraycopy(offset.toByteArray(), 0, this, 9, 4)
        System.arraycopy(data, 0, this, 13, data.size)
    }
}

class Cancel(val pieceIndex: Int, val offset: Int, val length: Int) : Message() {

    override val bytes = ByteArray(17).apply {
        System.arraycopy(13.toByteArray(), 0, this, 0, 4)
        this[4] = 8
        System.arraycopy(pieceIndex.toByteArray(), 0, this, 5, 4)
        System.arraycopy(offset.toByteArray(), 0, this, 9, 4)
        System.arraycopy(length.toByteArray(), 0, this, 13, 4)
    }
}

class Port(val port: Short) : Message() {

    override val bytes = ByteArray(7).apply {
        System.arraycopy(3.toByteArray(), 0, this, 0, 4)
        this[4] = 9
        System.arraycopy(port.toByteArray(), 0, this, 5, 4)
    }
}

fun readBytes(inputStream: InputStream, n: Int): ByteArray {
    val bytes = ByteArray(n)
    var bytesRead = 0
    while (bytesRead < n) {
        val readResult = inputStream.read(bytes, bytesRead, n - bytesRead)
        when (readResult) {
            -1 -> throw PeerProtocolException("Unexpected end of stream")
            else -> bytesRead += readResult
        }
    }
    return bytes
}
