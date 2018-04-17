package ktorrent.bencoding

import java.io.InputStream

class BReader(private val inputStream: InputStream) {

    private var position = 0

    fun read() = read(readByte())

    private fun read(firstByte: Char) = when (firstByte) {
        in '0'..'9' -> readByteString(firstByte)
        'i' -> readInteger()
        'l' -> readList()
        'd' -> readDictionary()
        (-1).toChar() -> throw BEncodingException("Unexpected end of stream")
        else -> throw BEncodingException("Unexpected symbol '$firstByte' at ${position - 1}")
    }

    private fun readByteString(firstByte: Char): BByteString {
        var length = firstByte - '0'
        while (true) {
            val readResult = readByte()
            when {
                readResult == ':' -> return BByteString(readBytes(length))
                readResult in '0'..'9' && firstByte != '0' -> length = length * 10 + (readResult - '0')
                readResult == (-1).toChar() -> throw BEncodingException("Unexpected end of stream")
                else -> throw BEncodingException("Unexpected symbol '$readResult' at ${position - 1}")
            }
        }
    }

    private fun readInteger(): BInteger {
        var value = 0L
        var first = true
        var zero = false
        var negative = false
        while (true) {
            val readResult = readByte()
            when {
                readResult == 'e' && !first -> return BInteger(if (negative) -value else value)
                readResult in '0'..'9' && !zero && (!negative || !first || readResult != '0') -> {
                    value = value * 10 + (readResult - '0')
                    if (first) {
                        first = false
                        zero = readResult == '0'
                    }
                }
                readResult == '-' && first && !negative -> negative = true
                readResult == (-1).toChar() -> throw BEncodingException("Unexpected end of stream")
                else -> throw BEncodingException("Unexpected symbol '$readResult' at ${position - 1}")
            }
        }
    }

    private fun readList(): BList = BList().apply {
        while (readByte().takeUnless { it == 'e' }?.let { this.add(read(it)) } != null) {}
    }

    private fun readDictionary(): BDictionary = BDictionary().apply {
        while (true) {
            val readResult = readByte()
            when (readResult) {
                'e' -> return this
                in '0'..'9' -> this[readByteString(readResult).string()] = read()
                (-1).toChar() -> throw BEncodingException("Unexpected end of stream")
                else -> throw BEncodingException("Unexpected symbol '$readResult' at ${position - 1}")
            }
        }
    }

    private fun readByte(): Char {
        ++position
        return inputStream.read().toChar()
    }

    private fun readBytes(n: Int): ByteArray {
        val bytes = ByteArray(n)
        var bytesRead = 0
        while (bytesRead < n) {
            val readResult = inputStream.read(bytes, bytesRead, n - bytesRead)
            when (readResult) {
                -1 -> throw BEncodingException("Unexpected end of stream")
                else -> bytesRead += readResult
            }
        }
        position += n
        return bytes
    }
}
