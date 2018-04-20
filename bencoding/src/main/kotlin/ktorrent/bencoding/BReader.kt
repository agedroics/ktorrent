package ktorrent.bencoding

import java.io.InputStream

class BReader(private val inputStream: InputStream) {

    private var position = 0

    fun read() = read(readByte())

    private fun read(firstByte: Char) = when (firstByte) {
        in '0'..'9' -> readByteString(firstByte - '0')
        'i' -> readInteger()
        'l' -> readList()
        'd' -> readDictionary()
        else -> throw InvalidBEncodingException("Unexpected symbol '$firstByte' at ${position - 1}")
    }

    private tailrec fun readByteString(length: Int): BByteString {
        val readResult = readByte()
        return when {
            readResult == ':' -> BByteString(readBytes(length))
            readResult in '0'..'9' && length != 0 -> readByteString(length * 10 + (readResult - '0'))
            else -> throw InvalidBEncodingException("Unexpected symbol '$readResult' at ${position - 1}")
        }
    }

    private tailrec fun readInteger(value: Long = 0, first: Boolean = true, negative: Boolean = false): BInteger {
        val readResult = readByte()
        return when {
            readResult == 'e' && !first -> BInteger(if (negative) -value else value)
            readResult in '0'..'9' && (first && !negative || negative && !first || negative && readResult != '0' || value != 0L) ->
                readInteger(value * 10 + (readResult - '0'), false, negative)
            readResult == '-' && first && !negative -> readInteger(value, true, true)
            else -> throw InvalidBEncodingException("Unexpected symbol '$readResult' at ${position - 1}")
        }
    }

    private fun readList(): BList = BList().apply {
        while (readByte().takeUnless { it == 'e' }?.let { add(read(it)) } != null) {}
    }

    private tailrec fun readDictionary(dictionary: BDictionary = BDictionary()): BDictionary {
        val readResult = readByte()
        return when (readResult) {
            'e' -> dictionary
            in '0'..'9' -> {
                dictionary[readByteString(readResult - '0').string()] = read()
                readDictionary(dictionary)
            }
            else -> throw InvalidBEncodingException("Unexpected symbol '$readResult' at ${position - 1}")
        }
    }

    private fun readByte(): Char {
        ++position
        val readResult = inputStream.read()
        when (readResult) {
            -1 -> throw InvalidBEncodingException("Unexpected end of stream")
            else -> return readResult.toChar()
        }
    }

    private fun readBytes(n: Int): ByteArray {
        val bytes = ByteArray(n)
        var bytesRead = 0
        while (bytesRead < n) {
            val readResult = inputStream.read(bytes, bytesRead, n - bytesRead)
            when (readResult) {
                -1 -> throw InvalidBEncodingException("Unexpected end of stream")
                else -> bytesRead += readResult
            }
        }
        position += n
        return bytes
    }
}
