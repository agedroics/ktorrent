package ktorrent.protocol.peer

import ktorrent.utils.*
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.ceil

sealed class Message {

    protected abstract val bytes: ByteArray

    fun write(outputStream: OutputStream) = outputStream.write(bytes)

    companion object {

        fun read(inputStream: InputStream, maxDataLength: Int, pieceCount: Int): Message {
            val length = inputStream.readBytesOrFail(4).toInt()
            if (length == 0) {
                return KeepAlive
            }
            val id = inputStream.readByteOrFail()
            return when (id) {
                0 -> when (length) {
                    1 -> Choke
                    else -> throw IncorrectLengthException("choke", length, 1)
                }
                1 -> when (length) {
                    1 -> Unchoke
                    else -> throw IncorrectLengthException("unchoke", length, 1)
                }
                2 -> when (length) {
                    1 -> Interested
                    else -> throw IncorrectLengthException("interested", length, 1)
                }
                3 -> when (length) {
                    1 -> NotInterested
                    else -> throw IncorrectLengthException("not interested", length, 1)
                }
                4 -> when (length) {
                    5 -> Have(inputStream.readBytesOrFail(4).toInt())
                    else -> throw IncorrectLengthException("have", length, 5)
                }
                5 -> {
                    val expectedLength = 1 + ceil(pieceCount / 8f).toInt()
                    when (length) {
                        expectedLength -> BitField(BitArray.wrap(inputStream.readBytesOrFail(length - 1)))
                        else -> throw IncorrectLengthException("bitfield", length, expectedLength)
                    }
                }
                6 -> when (length) {
                    13 -> Request(
                            pieceIndex = inputStream.readBytesOrFail(4).toInt(),
                            offset = inputStream.readBytesOrFail(4).toInt(),
                            length = inputStream.readBytesOrFail(4).toInt()
                    )
                    else -> throw IncorrectLengthException("request", length, 13)
                }
                7 -> when (length) {
                    in 9..maxDataLength + 9 -> Piece(
                            pieceIndex = inputStream.readBytesOrFail(4).toInt(),
                            offset = inputStream.readBytesOrFail(4).toInt(),
                            data = inputStream.readBytesOrFail(length - 9)
                    )
                    in 0..8 -> throw IncorrectLengthException("piece", length)
                    else -> throw PieceMessageTooLongException(length, maxDataLength + 9)
                }
                8 -> when (length) {
                    13 -> Cancel(
                            pieceIndex = inputStream.readBytesOrFail(4).toInt(),
                            offset = inputStream.readBytesOrFail(4).toInt(),
                            length = inputStream.readBytesOrFail(4).toInt()
                    )
                    else -> throw IncorrectLengthException("cancel", length, 13)
                }
                else -> throw UnrecognizedMessageException(id, length)
            }
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
