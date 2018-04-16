package ktorrent.protocol

import java.security.MessageDigest

fun ByteArray.split(chunkSize: Int) = (0 until size / chunkSize).map {
    sliceArray(chunkSize * it until kotlin.math.min(chunkSize * (it + 1), size))
}

fun ByteArray.sha1(): ByteArray {
    val digest = MessageDigest.getInstance("SHA-1")
    digest.update(this)
    return digest.digest()
}

fun ByteArray.toHexString(): String {
    val stringBuilder = StringBuilder(size * 2)
    forEach {
        val value = it.toInt() % 0xFF
        stringBuilder.append(HEX_CHARS[(value ushr 4) and 0x0F]).append(HEX_CHARS[value and 0x0F])
    }
    return stringBuilder.toString()
}

private val HEX_CHARS = "0123456789ABCDEF"
