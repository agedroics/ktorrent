package ktorrent.bencoding

import java.io.InputStream

class BReader(private val inputStream: InputStream) {

    private var position = 0

    fun read(): BEncodable {
        val readResult = inputStream.read()
        ++position
        when (readResult) {
            -1 -> throw BEncodingException("Unexpected end of stream")
            else -> return read(readResult.toChar())
        }
    }

    private fun read(firstByte: Char) = when (firstByte) {
        in '0'..'9' -> readByteString(firstByte)
        'i' -> readInteger()
        'l' -> readList()
        'd' -> readDictionary()
        else -> throw BEncodingException("Unexpected symbol '$firstByte' at ${position - 1}")
    }

    private fun readByteString(firstByte: Char): BByteString {
        var length = firstByte - '0'
        while (true) {
            val readResult = inputStream.read().toChar()
            ++position
            when {
                readResult in '0'..'9' && firstByte != '0' -> length = length * 10 + (readResult - '0')
                else -> when (readResult) {
                    ':' -> {
                        val byteString = BByteString(read(length))
                        position += byteString.string.length
                        return byteString
                    }
                    (-1).toChar() -> throw BEncodingException("Unexpected end of stream")
                    else -> throw BEncodingException("Unexpected symbol '$readResult' at ${position - 1}")
                }
            }
        }
    }

    private fun readInteger(): BInteger {
        var value = 0L
        var first = true
        var zero = false
        var negative = false
        while (true) {
            val readResult = inputStream.read().toChar()
            ++position
            when {
                readResult in '0'..'9' && !zero && (!negative || !first || readResult != '0') -> {
                    value = value * 10 + (readResult - '0')
                    if (first) {
                        first = false
                        zero = readResult == '0'
                    }
                }
                readResult == '-' && first && !negative -> negative = true
                readResult == 'e' && !first -> return BInteger(if (negative) -value else value)
                readResult.toInt() == -1 -> throw BEncodingException("Unexpected end of stream")
                else -> throw BEncodingException("Unexpected symbol '$readResult' at ${position - 1}")
            }
        }
    }

    private fun readList(): BList {
        val list = BList()
        while (true) {
            val readResult = inputStream.read().toChar()
            ++position
            when {
                readResult == 'e' -> return list
                readResult.toInt() == -1 -> throw BEncodingException("Unexpected end of stream")
                else -> list.add(read(readResult))
            }
        }
    }

    private fun readDictionary(): BDictionary {
        val dictionary = BDictionary()
        while (true) {
            val readResult = inputStream.read().toChar()
            ++position
            when (readResult) {
                in '0'..'9' -> {
                    dictionary[readByteString(readResult).string] = read()
                }
                'e' -> return dictionary
                (-1).toChar() -> throw BEncodingException("Unexpected end of stream")
                else -> throw BEncodingException("Unexpected symbol '$readResult' at ${position - 1}")
            }
        }
    }

    private fun read(n: Int): ByteArray {
        val bytes = ByteArray(n)
        var bytesRead = 0
        while (bytesRead < n) {
            val readResult = inputStream.read(bytes, bytesRead, n - bytesRead)
            when (readResult) {
                -1 -> throw BEncodingException("Unexpected end of stream")
                else -> bytesRead += readResult
            }
        }
        return bytes
    }
}
