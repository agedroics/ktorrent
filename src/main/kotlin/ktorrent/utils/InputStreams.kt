package ktorrent.utils

import java.io.InputStream

object EndOfStreamException : RuntimeException("Unexpected end of stream")

fun InputStream.readByteOrFail(): Int {
    val readResult = read()
    return when (readResult) {
        -1 -> throw EndOfStreamException
        else -> readResult
    }
}

fun InputStream.readBytesOrFail(n: Int): ByteArray {
    val bytes = ByteArray(n)
    var bytesRead = 0
    while (bytesRead < n) {
        val readResult = read(bytes, bytesRead, n - bytesRead)
        when (readResult) {
            -1 -> throw EndOfStreamException
            else -> bytesRead += readResult
        }
    }
    return bytes
}
